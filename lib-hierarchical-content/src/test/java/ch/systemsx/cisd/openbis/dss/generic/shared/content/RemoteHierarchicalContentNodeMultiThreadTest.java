/*
 * Copyright ETH 2013 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.dss.generic.shared.content;

import java.io.File;
import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.internal.NamedSequence;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import ch.systemsx.cisd.common.concurrent.MessageChannel;
import ch.systemsx.cisd.common.concurrent.MessageChannelBuilder;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.ethz.sis.shared.log.classic.impl.ConsoleLogger;
import ch.systemsx.cisd.common.test.ProxyAction;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContentNode;
import ch.systemsx.cisd.openbis.dss.generic.shared.dto.DataSetPathInfo;

/**
 * @author Franz-Josef Elmer
 */
public class RemoteHierarchicalContentNodeMultiThreadTest extends AbstractRemoteHierarchicalContentTestCase
{
    @Test
    public void testGetTwoDifferentFilesInSequence() throws Exception
    {
        ContentCache cache = createCache();
        final DataSetPathInfo pathInfo1 = new DataSetPathInfo();
        pathInfo1.setRelativePath(remoteFile1.getName());
        pathInfo1.setDirectory(false);
        IHierarchicalContentNode node1 = createRemoteNode(pathInfo1, cache);
        context.checking(new Expectations()
            {
                {
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo1.getRelativePath());
                    will(returnValue(remoteFile1.toURI().toURL().toString()));
                }
            });
        final DataSetPathInfo pathInfo2 = new DataSetPathInfo();
        pathInfo2.setRelativePath(remoteFile2.getName());
        pathInfo2.setDirectory(false);
        IHierarchicalContentNode node2 = createRemoteNode(pathInfo2, cache);
        context.checking(new Expectations()
            {
                {
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo2.getRelativePath());
                    will(returnValue(remoteFile2.toURI().toURL().toString()));
                }
            });
        prepareRequestPersistence(2);

        File file1 = node1.getFile();

        AssertJUnit.assertEquals(1000, file1.getParentFile().lastModified());
        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER
                + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/" + remoteFile1.getName()).getAbsolutePath(),
                file1.getAbsolutePath());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE1_CONTENT, FileUtilities.loadToString(file1).trim());
        AssertJUnit.assertEquals(
                "[]",
                Arrays.asList(
                        new File(workSpace, ContentCache.DOWNLOADING_FOLDER).list()).toString());

        File file2 = node2.getFile();

        AssertJUnit.assertEquals(61000, file2.getParentFile().lastModified());
        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER
                + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/" + remoteFile2.getName()).getAbsolutePath(),
                file2.getAbsolutePath());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE2_CONTENT, FileUtilities.loadToString(file2).trim());
        context.assertIsSatisfied();
    }

    @Test
    public void testGetSameFileInSequenceFirstTryFailing() throws Exception
    {
        ContentCache cache = createCache();
        final DataSetPathInfo pathInfo = new DataSetPathInfo();
        pathInfo.setRelativePath(remoteFile1.getName());
        pathInfo.setDirectory(false);
        IHierarchicalContentNode node = createRemoteNode(pathInfo, cache);
        context.checking(new Expectations()
            {
                {
                    NamedSequence sequence = new NamedSequence("s1");
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo.getRelativePath());
                    will(throwException(new RuntimeException("error")));
                    inSequence(sequence);

                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo.getRelativePath());
                    will(returnValue(remoteFile1.toURI().toURL().toString()));
                    inSequence(sequence);
                }
            });
        prepareRequestPersistence(1);

        try
        {
            node.getFile();
            AssertJUnit.fail("RuntimeException expected");
        } catch (RuntimeException ex)
        {
            AssertJUnit.assertEquals("error", ex.getMessage());
        }
        File file = node.getFile();

        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/"
                + remoteFile1.getName()).getAbsolutePath(), file.getAbsolutePath());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE1_CONTENT, FileUtilities.loadToString(file).trim());
        context.assertIsSatisfied();
    }

    @Test
    public void testGetTwoDifferentFilesInTwoThreads() throws Exception
    {
        ContentCache cache = createCache();
        final DataSetPathInfo pathInfo1 = new DataSetPathInfo();
        pathInfo1.setRelativePath(remoteFile1.getName());
        pathInfo1.setDirectory(false);
        IHierarchicalContentNode node1 = createRemoteNode(pathInfo1, cache);
        final MessageChannel channel1 = new MessageChannel(10000);
        GetFileRunnable fileRunnable1 = new GetFileRunnable(node1, channel1)
            {
                @Override
                public void run()
                {
                    channel1.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    super.run();
                }
            };
        Thread thread1 = new Thread(fileRunnable1);
        final DataSetPathInfo pathInfo2 = new DataSetPathInfo();
        pathInfo2.setRelativePath(remoteFile2.getName());
        pathInfo2.setDirectory(false);
        IHierarchicalContentNode node2 = createRemoteNode(pathInfo2, cache);
        final MessageChannel channel2 = new MessageChannel(10000);
        GetFileRunnable fileRunnable2 = new GetFileRunnable(node2, channel2)
            {
                @Override
                public void run()
                {
                    channel1.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    channel2.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    super.run();
                }
            };
        Thread thread2 = new Thread(fileRunnable2);
        final MessageChannel channel3 = new MessageChannel(10000);
        context.checking(new Expectations()
            {
                {
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo1.getRelativePath());
                    will(new ProxyAction(returnValue(remoteFile1.toURI().toURL().toString()))
                        {
                            @Override
                            protected void doBeforeReturn()
                            {
                                channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                            }
                        });
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo2.getRelativePath());
                    will(new ProxyAction(returnValue(remoteFile2.toURI().toURL().toString()))
                        {
                            @Override
                            protected void doBeforeReturn()
                            {
                                channel1.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
                                channel3.send(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
                            }
                        });
                }
            });
        prepareRequestPersistence(2);

        thread1.start();
        thread2.start();
        channel3.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        File file1 = fileRunnable1.tryGetResult();
        File file2 = fileRunnable2.tryGetResult();

        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/"
                + remoteFile1.getName()).getAbsolutePath(), file1.getAbsolutePath());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE1_CONTENT, FileUtilities.loadToString(file1).trim());
        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/"
                + remoteFile2.getName()).getAbsolutePath(), file2.getAbsolutePath());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE2_CONTENT, FileUtilities.loadToString(file2).trim());
        context.assertIsSatisfied();
    }

    @Test
    public void testGetSameFileInTwoThreads() throws Exception
    {
        ContentCache cache = createCache();
        final DataSetPathInfo pathInfo = new DataSetPathInfo();
        pathInfo.setRelativePath(remoteFile1.getName());
        pathInfo.setDirectory(false);
        final IHierarchicalContentNode node1 = createRemoteNode(pathInfo, cache);
        ConsoleLogger logger = new ConsoleLogger();
        final MessageChannel channel1 =
                new MessageChannelBuilder(10000).name("1").logger(logger).getChannel();
        final IHierarchicalContentNode node2 = createRemoteNode(pathInfo, cache);
        final MessageChannel channel2 =
                new MessageChannelBuilder(10000).name("2").logger(logger).getChannel();
        final MessageChannel channel3 =
                new MessageChannelBuilder(10000).name("3").logger(logger).getChannel();
        GetFileRunnable fileRunnable1 = new GetFileRunnable(node1, channel1);
        GetFileRunnable fileRunnable2 = new GetFileRunnable(node2, channel2)
            {
                @Override
                public void run()
                {
                    channel1.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    channel2.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    super.run();
                }
            };
        final Thread thread1 = new Thread(fileRunnable1, "thread1");
        final Thread thread2 = new Thread(fileRunnable2, "thread2");
        context.checking(new Expectations()
            {
                {
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo.getRelativePath());
                    will(new ProxyAction(returnValue(remoteFile1.toURI().toURL().toString()))
                        {
                            @Override
                            protected void doBeforeReturn()
                            {
                                channel1.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                                channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                                channel3.send(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
                            }
                        });
                }
            });
        prepareRequestPersistence(2);

        thread1.start();
        thread2.start();
        channel3.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        channel1.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);

        File file1 = fileRunnable1.tryGetResult();
        File file2 = fileRunnable2.tryGetResult();
        AssertJUnit.assertEquals(61000, file1.getParentFile().lastModified());
        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/"
                + remoteFile1.getName()).getAbsolutePath(), file1.getAbsolutePath());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE1_CONTENT, FileUtilities.loadToString(file1).trim());
        AssertJUnit.assertEquals(file1, file2);
        context.assertIsSatisfied();
    }

    @Test
    public void testGetSameFileInThreeThreads() throws Exception
    {
        ContentCache cache = createCache();
        final DataSetPathInfo pathInfo = new DataSetPathInfo();
        pathInfo.setRelativePath(remoteFile1.getName());
        pathInfo.setDirectory(false);
        final IHierarchicalContentNode node1 = createRemoteNode(pathInfo, cache);
        ConsoleLogger logger = new ConsoleLogger();
        final MessageChannel channel1 =
                new MessageChannelBuilder(10000).name("1").logger(logger).getChannel();
        final IHierarchicalContentNode node2 = createRemoteNode(pathInfo, cache);
        final MessageChannel channel2 =
                new MessageChannelBuilder(10000).name("2").logger(logger).getChannel();
        final IHierarchicalContentNode node3 = createRemoteNode(pathInfo, cache);
        final MessageChannel channel3 =
                new MessageChannelBuilder(10000).name("3").logger(logger).getChannel();
        final MessageChannel channel4 =
                new MessageChannelBuilder(10000).name("4").logger(logger).getChannel();
        GetFileRunnable fileRunnable1 = new GetFileRunnable(node1, channel1);
        GetFileRunnable fileRunnable2 = new GetFileRunnable(node2, channel2)
            {
                @Override
                public void run()
                {
                    channel1.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    channel2.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    super.run();
                }
            };
        GetFileRunnable fileRunnable3 = new GetFileRunnable(node3, channel3)
            {
                @Override
                public void run()
                {
                    channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    channel3.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    super.run();
                }
            };
        final Thread thread1 = new Thread(fileRunnable1, "thread1");
        final Thread thread2 = new Thread(fileRunnable2, "thread2");
        final Thread thread3 = new Thread(fileRunnable3, "thread3");
        context.checking(new Expectations()
            {
                {
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo.getRelativePath());
                    will(new ProxyAction(returnValue(remoteFile1.toURI().toURL().toString()))
                        {
                            @Override
                            protected void doBeforeReturn()
                            {
                                channel1.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                                channel3.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                                channel4.send(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
                            }
                        });
                }
            });
        prepareRequestPersistence(3);

        thread1.start();
        thread2.start();
        thread3.start();
        channel4.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        channel1.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        channel3.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);

        File file1 = fileRunnable1.tryGetResult();
        File file2 = fileRunnable2.tryGetResult();
        File file3 = fileRunnable3.tryGetResult();
        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/"
                + remoteFile1.getName()).getAbsolutePath(), file1.getAbsolutePath());
        AssertJUnit.assertEquals(121000, file1.getParentFile().lastModified());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE1_CONTENT, FileUtilities.loadToString(file1).trim());
        AssertJUnit.assertEquals(file1, file2);
        AssertJUnit.assertEquals(file1, file3);
        context.assertIsSatisfied();
    }

    @Test
    public void testGetSameFileInTwoThreadsFirstDownloadFails() throws Exception
    {
        ContentCache cache = createCache();
        final DataSetPathInfo pathInfo = new DataSetPathInfo();
        pathInfo.setRelativePath(remoteFile1.getName());
        pathInfo.setDirectory(false);
        final IHierarchicalContentNode node1 = createRemoteNode(pathInfo, cache);
        ConsoleLogger logger = new ConsoleLogger();
        final MessageChannel channel1 =
                new MessageChannelBuilder(10000).name("1").logger(logger).getChannel();
        final IHierarchicalContentNode node2 = createRemoteNode(pathInfo, cache);
        final MessageChannel channel2 =
                new MessageChannelBuilder(10000).name("2").logger(logger).getChannel();
        final MessageChannel channel3 =
                new MessageChannelBuilder(10000).name("3").logger(logger).getChannel();
        GetFileRunnable fileRunnable1 = new GetFileRunnable(node1, channel1);
        GetFileRunnable fileRunnable2 = new GetFileRunnable(node2, channel2)
            {
                @Override
                public void run()
                {
                    channel1.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    channel2.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                    super.run();
                }
            };
        final Thread thread1 = new Thread(fileRunnable1, "thread1");
        final Thread thread2 = new Thread(fileRunnable2, "thread2");
        final RuntimeException exception = new RuntimeException("error");
        context.checking(new Expectations()
            {
                {
                    NamedSequence sequence = new NamedSequence("s");
                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo.getRelativePath());
                    will(new ProxyAction(throwException(exception))
                        {
                            @Override
                            protected void doBeforeReturn()
                            {
                                channel1.send(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                                channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.STARTED_MESSAGE);
                            }
                        });
                    inSequence(sequence);

                    one(remoteDss).getDownloadUrlForFileForDataSet(AbstractRemoteHierarchicalContentTestCase.SESSION_TOKEN, AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE,
                            pathInfo.getRelativePath());
                    will(new ProxyAction(returnValue(remoteFile1.toURI().toURL().toString()))
                        {
                            @Override
                            protected void doBeforeReturn()
                            {
                                channel3.send(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
                            }
                        });
                    inSequence(sequence);
                }
            });
        prepareRequestPersistence(1);

        thread1.start();
        thread2.start();
        channel3.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);
        channel1.assertNextMessage(exception);
        channel2.assertNextMessage(AbstractRemoteHierarchicalContentTestCase.FINISHED_MESSAGE);

        File file1 = fileRunnable1.tryGetResult();
        AssertJUnit.assertEquals(null, file1);
        File file2 = fileRunnable2.tryGetResult();
        AssertJUnit.assertEquals(new File(workSpace, ContentCache.CACHE_FOLDER + "/" + AbstractRemoteHierarchicalContentTestCase.DATA_SET_CODE + "/"
                + remoteFile1.getName()).getAbsolutePath(), file2.getAbsolutePath());
        AssertJUnit.assertEquals(AbstractRemoteHierarchicalContentTestCase.FILE1_CONTENT, FileUtilities.loadToString(file2).trim());
        context.assertIsSatisfied();
    }

    private IHierarchicalContentNode createRemoteNode(DataSetPathInfo pathInfo, ContentCache cache)
    {
        return new RemoteHierarchicalContentNode(AbstractRemoteHierarchicalContentTestCase.DATA_SET_LOCATION, pathInfo, pathInfoProvider,
                serviceFactory, sessionHolder, cache);
    }

}
