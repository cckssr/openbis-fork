/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.common.filesystem;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import ch.systemsx.cisd.base.utilities.OSUtilities;
import ch.systemsx.cisd.common.exceptions.Status;
import ch.systemsx.cisd.common.filesystem.rsync.RsyncBasedRecursiveHardLinkMaker;
import ch.systemsx.cisd.common.test.RetryTen;
import ch.systemsx.cisd.common.test.TestReportCleaner;
import ch.systemsx.cisd.common.time.TimingParameters;

/**
 * Test cases for the {@link FastRecursiveHardLinkMaker}.
 * <p>
 * More or less a duplicate of {@link RecursiveHardLinkMakerTest}.
 * 
 * @author Chandrasekhar Ramakrishnan
 */
@Listeners(TestReportCleaner.class)
public class FastRecursiveHardLinkMakerTest extends AbstractHardlinkMakerTest
{
    private static final long TIMEOUT_SECONDS = 10;

    @Override
    protected TestBigStructureCreator createBigStructureCreator(File root)
    {
        int[] numberOfFolders =
                { 100, 10 };
        int[] numberOfFiles =
                { 1, 10, 10 };
        return new TestBigStructureCreator(root, numberOfFolders, numberOfFiles);
    }

    @Override
    protected IImmutableCopier createHardLinkCopier()
    {
        IImmutableCopier copier = FastRecursiveHardLinkMaker.tryCreate(null);
        assert copier != null;
        return copier;
    }

    @Override
    @Test(groups =
    { "requires_unix" }, retryAnalyzer = RetryTen.class)
    public void testDeleteWhileCopying() throws IOException
    {
        final TestBigStructureCreator creator =
                createBigStructureCreator(new File(workingDirectory, "big-structure"));
        final File src = creator.createBigStructure();
        assertTrue(creator.verifyStructure());

        final BlockingDirectoryImmutableCopier blockingDirectoryCopier =
                new BlockingDirectoryImmutableCopier(createRsyncDirectoryCopier());
        final IImmutableCopier copier = new AssertionCatchingImmutableCopierWrapper(
                createFastCopier(blockingDirectoryCopier));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            final Future<Status> copyFuture = executor.submit(() -> copier.copyImmutably(src, outputDir, null));

            blockingDirectoryCopier.awaitUntilBlocked();
            FileUtilities.deleteRecursively(src);
            final Status status = unblockAndAwait(copyFuture, blockingDirectoryCopier);

            assertFalse(status.isOK());
            final File dest = new File(outputDir, src.getName());

            final TestBigStructureCreator structureCopy = new TestBigStructureCreator(dest);
            assertFalse("Big structure was partially copied", structureCopy.verifyStructure());
            assertFalse("Original was not partially deleted", creator.verifyStructure());
        } finally
        {
            blockingDirectoryCopier.unblock();
            executor.shutdownNow();
        }
    }

    private IImmutableCopier createFastCopier(final BlockingDirectoryImmutableCopier blockingDirectoryCopier)
    {
        final TimingParameters timingParameters = TimingParameters.getDefaultParameters();
        final IFileImmutableCopier fastFileCopier = FastHardLinkMaker.tryCreate(timingParameters);
        final File lnExecutable = OSUtilities.findExecutable("ln");
        assert fastFileCopier != null || lnExecutable != null;
        final IImmutableCopier fallbackCopier = (fastFileCopier == null)
                        ? RecursiveHardLinkMaker.tryCreate(HardLinkMaker.create(lnExecutable, timingParameters))
                        : RecursiveHardLinkMaker.tryCreate(fastFileCopier);
        return new FastRecursiveHardLinkMaker(fastFileCopier, blockingDirectoryCopier,
                fallbackCopier, timingParameters);
    }

    private IDirectoryImmutableCopier createRsyncDirectoryCopier()
    {
        final File rsyncExecutable = OSUtilities.findExecutable("rsync");
        assert rsyncExecutable != null;
        return new RsyncBasedRecursiveHardLinkMaker(rsyncExecutable,
                TimingParameters.getDefaultParameters(), 3, null);
    }

    private Status unblockAndAwait(final Future<Status> copyFuture,
            final BlockingDirectoryImmutableCopier blockingDirectoryCopier)
    {
        try
        {
            blockingDirectoryCopier.unblock();
            return copyFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e)
        {
            throw new AssertionError("Failed to finish fast copy after deleting the source.", e);
        }
    }

    private static final class BlockingDirectoryImmutableCopier implements IDirectoryImmutableCopier
    {
        private final IDirectoryImmutableCopier delegate;

        private final CountDownLatch reachedDirectoryCopy = new CountDownLatch(1);

        private final CountDownLatch continueCopy = new CountDownLatch(1);

        private BlockingDirectoryImmutableCopier(final IDirectoryImmutableCopier delegate)
        {
            assert delegate != null;
            this.delegate = delegate;
        }

        private void awaitUntilBlocked()
        {
            try
            {
                assertTrue(
                        "Timed out waiting for the fast recursive copy to reach directory copying.",
                        reachedDirectoryCopy.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for the fast copy to block.", e);
            }
        }

        private void unblock()
        {
            continueCopy.countDown();
        }

        @Override
        public Status copyDirectoryImmutably(final File sourceDirectory, final File destinationDirectory,
                final String targetNameOrNull)
        {
            return copyDirectoryImmutably(sourceDirectory, destinationDirectory, targetNameOrNull,
                    CopyModeExisting.ERROR);
        }

        @Override
        public Status copyDirectoryImmutably(final File sourceDirectory, final File destinationDirectory,
                final String targetNameOrNull, final CopyModeExisting mode)
        {
            reachedDirectoryCopy.countDown();
            try
            {
                assertTrue("Timed out waiting for deletion to finish before continuing fast copy.",
                        continueCopy.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting to continue fast copying.", e);
            }
            return delegate.copyDirectoryImmutably(sourceDirectory, destinationDirectory,
                    targetNameOrNull, mode);
        }
    }
}
