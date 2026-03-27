/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.shared.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoolTest
{

    private static final int NUMBER_OF_THREADS = 4;

    private IntegerFactory factory;

    private Pool<Void, Integer> pool;

    @Before
    public void beforeMethod() throws Exception
    {
        factory = new IntegerFactory();
        pool = new Pool<>(NUMBER_OF_THREADS, null, factory);
    }

    @After
    public void afterMethod() throws Exception
    {
        pool.shutdown();
    }

    @Test
    public void checkOut_singleThread_returnsNonNullElement() throws Exception
    {
        Integer element = pool.checkOut();
        assertNotNull(element);
        pool.checkIn(element);
    }

    @Test
    public void checkOut_singleThread_elementIsReusedAfterCheckIn() throws Exception
    {
        Integer first = pool.checkOut();
        pool.checkIn(first);
        Integer second = pool.checkOut();
        assertEquals(first, second);
        pool.checkIn(second);
    }

    /**
     * All threads check out simultaneously. Each must receive a non-null element.
     * With the buggy implementation one thread may steal another thread's newly created element,
     * leaving the other with null, causing a NullPointerException inside checkOut().
     */
    @Test
    public void checkOut_concurrent_allThreadsReceiveNonNullElement() throws Exception
    {
        int threadCount = NUMBER_OF_THREADS;
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount))
        {
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            List<Future<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++)
            {
                futures.add(executor.submit(() ->
                {
                    barrier.await();
                    Integer element = pool.checkOut();
                    Thread.sleep(10);
                    pool.checkIn(element);
                    return element;
                }));
            }

            for (Future<Integer> future : futures)
            {
                assertNotNull("checkOut() returned null for a thread", future.get(5, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * All threads hold their elements at the same time.
     * No element may be shared between two threads simultaneously.
     */
    @Test
    public void checkOut_concurrent_noElementIsSharedBetweenThreads() throws Exception
    {
        int threadCount = NUMBER_OF_THREADS;
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount))
        {
            CyclicBarrier startBarrier = new CyclicBarrier(threadCount);
            CyclicBarrier holdBarrier = new CyclicBarrier(threadCount);
            Set<Integer> heldConcurrently = Collections.newSetFromMap(new ConcurrentHashMap<>());
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++)
            {
                futures.add(executor.submit(() ->
                {
                    startBarrier.await();
                    Integer element = pool.checkOut();
                    boolean unique = heldConcurrently.add(element);
                    holdBarrier.await(); // all threads hold their element at the same time
                    pool.checkIn(element);
                    return unique;
                }));
            }

            for (Future<Boolean> future : futures)
            {
                assertTrue("Same element was checked out by two threads simultaneously",
                        future.get(5, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * More threads than NUMBER_OF_THREADS run concurrently. The total number of elements ever created
     * must not exceed NUMBER_OF_THREADS regardless of how many threads contend.
     */
    @Test
    public void checkOut_concurrent_elementCountNeverExceedsMaxSize() throws Exception
    {
        int threadCount = NUMBER_OF_THREADS * 3;
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount))
        {
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            List<Future<Void>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++)
            {
                futures.add(executor.submit(() ->
                {
                    barrier.await();
                    Integer element = pool.checkOut();
                    Thread.sleep(10);
                    pool.checkIn(element);
                    return null;
                }));
            }

            for (Future<Void> future : futures)
            {
                future.get(10, TimeUnit.SECONDS);
            }

            assertTrue("Pool created " + factory.createdCount() + " elements, but NUMBER_OF_THREADS is " + NUMBER_OF_THREADS,
                    factory.createdCount() <= NUMBER_OF_THREADS);
        }
    }

    /**
     * When all elements are checked out, a new thread must block until one is returned.
     */
    @Test(timeout = 5000)
    public void checkOut_blocksWhenPoolExhausted_unblocksAfterCheckIn() throws Exception
    {
        List<Integer> checkedOut = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_THREADS; i++)
        {
            checkedOut.add(pool.checkOut());
        }

        CountDownLatch threadStarted = new CountDownLatch(1);
        CountDownLatch elementReceived = new CountDownLatch(1);

        Thread waitingThread = new Thread(() ->
        {
            try
            {
                threadStarted.countDown();
                Integer element = pool.checkOut(); // must block here
                pool.checkIn(element);
                elementReceived.countDown();
            } catch (Exception e)
            {
                Thread.currentThread().interrupt();
            }
        });
        waitingThread.start();

        threadStarted.await();
        Thread.sleep(50); // give the thread time to reach acquire()
        assertTrue("Thread should be blocked on checkOut()", waitingThread.isAlive());

        pool.checkIn(checkedOut.removeFirst());

        assertTrue("Thread should have unblocked after checkIn()",
                elementReceived.await(2, TimeUnit.SECONDS));

        for (Integer element : checkedOut)
        {
            pool.checkIn(element);
        }
    }

    @Test
    public void shutdown_destroysAllAvailableElements() throws Exception
    {
        Integer e1 = pool.checkOut();
        Integer e2 = pool.checkOut();
        pool.checkIn(e1);
        pool.checkIn(e2);

        pool.shutdown();

        assertEquals(2, factory.destroyedElements().size());
        assertTrue(factory.destroyedElements().contains(e1));
        assertTrue(factory.destroyedElements().contains(e2));
    }

    @Test
    public void shutdown_destroysElementsStillCheckedOut() throws Exception
    {
        Integer e1 = pool.checkOut();
        Integer e2 = pool.checkOut();
        pool.checkIn(e1);
        // e2 intentionally not checked in

        pool.shutdown();

        assertEquals(2, factory.destroyedElements().size());
        assertTrue(factory.destroyedElements().contains(e1));
        assertTrue(factory.destroyedElements().contains(e2));
    }

    private static class IntegerFactory implements Factory<Void, Void, Integer>
    {
        private final AtomicInteger counter = new AtomicInteger(0);

        private final Set<Integer> destroyed = Collections.newSetFromMap(new ConcurrentHashMap<>());

        @Override
        public void init(Void configurationParameters)
        {
        }

        @Override
        public Integer create(Void factoryParameters)
        {
            return counter.incrementAndGet();
        }

        @Override
        public void destroy(Integer element)
        {
            destroyed.add(element);
        }

        int createdCount()
        {
            return counter.get();
        }

        Set<Integer> destroyedElements()
        {
            return destroyed;
        }
    }

}
