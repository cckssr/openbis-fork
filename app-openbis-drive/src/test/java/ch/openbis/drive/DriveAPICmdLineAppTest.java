package ch.openbis.drive;

import ch.openbis.drive.model.*;
import ch.openbis.drive.protobuf.client.DriveAPIClientProtobufImpl;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class DriveAPICmdLineAppTest {

    final DriveAPICmdLineApp driveAPICmdLineApp = Mockito.spy(DriveAPICmdLineApp.class);
    final DriveAPIClientProtobufImpl driveAPIClient = Mockito.mock(DriveAPIClientProtobufImpl.class);

    @Before
    public void before() throws Exception {
        Mockito.reset(driveAPICmdLineApp);
        Mockito.reset(driveAPIClient);
        Mockito.doReturn(driveAPIClient).when(driveAPICmdLineApp).getNewDriveAPIClient();
    }

    public void testMain() {
    }

    @Test
    public void testHandleEventsCommand() throws Exception {
        driveAPICmdLineApp.handleEventsCommand( new String[] { "events", "-limit", "5" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvents(5);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleEventsCommand( new String[] { "events", "--limit", "6" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvents(6);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleEventsCommand( new String[] { "events", "-limit=7" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvents(7);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleEventsCommand( new String[] { "events", "--limit=9" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvents(9);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleEventsCommand( new String[] { "events" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvents(100);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleEventsCommand( new String[] { "events" , "other"} );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvents(100);
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testHandleNotificationsCommand() throws Exception {
        driveAPICmdLineApp.handleNotificationsCommand( new String[] { "notifications", "-limit", "5" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotifications(5);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleNotificationsCommand( new String[] { "notifications", "--limit", "6" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotifications(6);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleNotificationsCommand( new String[] { "notifications", "-limit=7" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotifications(7);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleNotificationsCommand( new String[] { "notifications", "--limit=9" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotifications(9);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleNotificationsCommand( new String[] { "notifications" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotifications(100);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleNotificationsCommand( new String[] { "notifications" , "other"} );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotifications(100);
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testHandleJobsCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printJobs();
        Mockito.doNothing().when(driveAPICmdLineApp).handleAddJobCommand(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).handleRemoveJobCommand(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).handleStartJobCommand(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).handleStopJobCommand(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).printHelp();

        driveAPICmdLineApp.handleJobsCommand( new String[] { "jobs" } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printJobs();
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleJobsCommand( new String[] { "jobs" , "add", "..."} );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).handleAddJobCommand(new String[] { "jobs" , "add", "..."});
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleJobsCommand( new String[] { "jobs", "remove", "..." } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).handleRemoveJobCommand(new String[] { "jobs" , "remove", "..."});
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleJobsCommand( new String[] { "jobs", "start", "..." } );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).handleStartJobCommand(new String[] { "jobs" , "start", "..."});
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleJobsCommand( new String[] { "jobs", "stop", "..."} );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).handleStopJobCommand(new String[] { "jobs" , "stop", "..."});
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleJobsCommand( new String[] { "jobs", "hidden-path-patterns", "..."} );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).handleHiddenPathPatternsJobCommand(new String[] { "jobs" , "hidden-path-patterns", "..."});
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleJobsCommand( new String[] { "jobs", "other", "..."} );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
        Mockito.clearInvocations(driveAPICmdLineApp);

    }

    @Test
    public void testHandleAddJobCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).addJob(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        driveAPICmdLineApp.handleAddJobCommand(new String[] { "jobs" , "add",
                "-type=Bidirectional",
                "-dir=/local-dir",
                "-remDir=/remote-dir",
                "-openBISurl=http://url",
                "-entityPermId=1234-abcd",
                "-personalAccessToken=tkntkntkn",
                "-enabled=true",
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).addJob(
                SyncJob.Type.Bidirectional,
                "/local-dir",
                "http://url",
                "1234-abcd",
                "tkntkntkn",
                "/remote-dir",
                true,
                null
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleAddJobCommand(new String[] { "jobs" , "add",
                "-type=Bidirectional",
                "-dir=/local-dir",
                "-remDirWRONGOPTION=/remote-dir",
                "-openBISurl=http://url",
                "-entityPermId=1234-abcd",
                "-personalAccessToken=tkntkntkn",
                "-enabled=true",
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).addJob(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any()
        );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testHandleRemoveJobCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).removeJob(Mockito.any());

        driveAPICmdLineApp.handleRemoveJobCommand(new String[] { "jobs" , "remove",
                "-dir=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).removeJob(
                "/local-dir"
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleRemoveJobCommand(new String[] { "jobs" , "remove",
                "-dirWRONGOPTION=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).removeJob(
                Mockito.any()
        );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testHandleStartJobCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).startJob(Mockito.any());

        driveAPICmdLineApp.handleStartJobCommand(new String[] { "jobs" , "start",
                "-dir=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).startJob(
                "/local-dir"
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleStartJobCommand(new String[] { "jobs" , "start",
                "-dirWRONGOPTION=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).startJob(
                Mockito.any()
        );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testHandleStopJobCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).stopJob(Mockito.any());

        driveAPICmdLineApp.handleStopJobCommand(new String[] { "jobs" , "stop",
                "-dir=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).stopJob(
                "/local-dir"
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleStopJobCommand(new String[] { "jobs" , "stop",
                "-dirWRONGOPTION=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).stopJob(
                Mockito.any()
        );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testHandleHiddenPathPatternsJobCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).stopJob(Mockito.any());

        //Show default hidden-patterns
        driveAPICmdLineApp.handleHiddenPathPatternsJobCommand(new String[] { "jobs" , "hidden-path-patterns" });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).showDefaultHiddenPathPatterns();
        Mockito.clearInvocations(driveAPICmdLineApp);

        //Show hidden path-patterns
        driveAPICmdLineApp.handleHiddenPathPatternsJobCommand(new String[] { "jobs" , "hidden-path-patterns",
                "-dir=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).showHiddenPathPatterns(
                "/local-dir"
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        //Reset hidden path-patterns
        driveAPICmdLineApp.handleHiddenPathPatternsJobCommand(new String[] { "jobs" , "hidden-path-patterns",
                "-dir=/local-dir", "-reset"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).resetHiddenPathPatternsToDefault(
                "/local-dir"
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        //Set hidden path-patterns from console
        System.setIn(new ByteArrayInputStream(String.format("^/bin/?%n   ^/root/?%n%n").getBytes(StandardCharsets.UTF_8)));
        driveAPICmdLineApp.handleHiddenPathPatternsJobCommand(new String[] { "jobs" , "hidden-path-patterns",
                "-dir=/local-dir", "-set"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).setHiddenPathPatterns(
                "/local-dir", String.format("^/bin/?%n^/root/?%n")
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        //Set hidden path-patterns from file
        Path newPatterns = Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("new-patterns.txt");
        Files.deleteIfExists(newPatterns);
        Files.write(newPatterns, String.format("^/bin/?%n   ^/root/?%n%n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        driveAPICmdLineApp.handleHiddenPathPatternsJobCommand(new String[] { "jobs" , "hidden-path-patterns",
                "-dir=/local-dir", String.format("-setFromFile=%s", newPatterns)
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).setHiddenPathPatterns(
                "/local-dir", String.format("^/bin/?%n   ^/root/?%n%n")
        );
        Mockito.clearInvocations(driveAPICmdLineApp);

        //Wrong option
        driveAPICmdLineApp.handleHiddenPathPatternsJobCommand(new String[] { "jobs" , "stop",
                "-dirWRONGOPTION=/local-dir"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).showHiddenPathPatterns(
                Mockito.any()
        );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).resetHiddenPathPatternsToDefault(
                Mockito.any()
        );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).setHiddenPathPatterns(
                Mockito.any(), Mockito.anyString()
        );
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testHandleConfigCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printConfig();
        Mockito.doNothing().when(driveAPICmdLineApp).setConfig(Mockito.any(), Mockito.any(), Mockito.any());

        driveAPICmdLineApp.handleConfigCommand(new String[] { "config"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printConfig();
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleConfigCommand(new String[] { "config", "-startAtLogin=true"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).setConfig(true, null, null);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleConfigCommand(new String[] { "config", "-language=it"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).setConfig(null, "it", null);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleConfigCommand(new String[] { "config", "-syncInterval=40"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).setConfig(null, null, 40);
        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleConfigCommand(new String[] { "config", "-startAtLogin=false", "-language=fr", "-syncInterval=45"
        });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).setConfig(false, "fr", 45);
        Mockito.clearInvocations(driveAPICmdLineApp);
    }

    @Test
    public void testPrintHelp() throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));
        driveAPICmdLineApp.printHelp();
        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());
        Assert.assertTrue(allWrittenLines.contains("help"));
        Assert.assertTrue(allWrittenLines.contains("start"));
        Assert.assertTrue(allWrittenLines.contains("stop"));
        Assert.assertTrue(allWrittenLines.contains("status"));
        Assert.assertTrue(allWrittenLines.contains("config"));
        Assert.assertTrue(allWrittenLines.contains("jobs"));
        Assert.assertTrue(allWrittenLines.contains("notifications"));
        Assert.assertTrue(allWrittenLines.contains("events"));
    }

    @Test
    public void testPrintConfig() throws Exception {
        Settings settings = new Settings();
        settings.setStartAtLogin(true);
        settings.setLanguage("es");
        settings.setSyncInterval(37);
        SyncJob syncJob = new SyncJob(SyncJob.Type.Upload, "http://url", "tkn", "1234", "/remDir", "/dir", true);
        settings.setJobs(new ArrayList<>(List.of(
                syncJob
        )));
        Mockito.doReturn(settings).when(driveAPIClient).getSettings();

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.printConfig();

        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());

        Assert.assertTrue(allWrittenLines.contains("true"));
        Assert.assertTrue(allWrittenLines.contains("es"));
        Assert.assertTrue(allWrittenLines.contains("37"));

        Mockito.verify(driveAPIClient, Mockito.times(1)).getSettings();
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printSyncJob(syncJob);
    }

    @Test
    public void testSetConfig() throws Exception {

        for (Boolean bool : new Boolean[] { true, null } ) {
            for(String language : new String[] {"it" , null}) {
                for(Integer syncInt : new Integer[] {98, null}) {

                    Settings settings = new Settings();
                    settings.setStartAtLogin(false);
                    settings.setLanguage("es");
                    settings.setSyncInterval(37);
                    SyncJob syncJob = new SyncJob(SyncJob.Type.Upload, "http://url", "tkn", "1234", "/remDir", "/dir", true);
                    settings.setJobs(new ArrayList<>(List.of(
                            syncJob
                    )));
                    Mockito.doReturn(settings).when(driveAPIClient).getSettings();
                    Mockito.doNothing().when(driveAPIClient).setSettings(Mockito.any());
                    Mockito.doNothing().when(driveAPICmdLineApp).printConfig();

                    driveAPICmdLineApp.setConfig(bool, language, syncInt);
                    Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).getNewDriveAPIClient();

                    ArgumentCaptor<Settings> settingsArgumentCaptor = ArgumentCaptor.forClass(Settings.class);
                    Mockito.verify(driveAPIClient, Mockito.times(1)).setSettings(settingsArgumentCaptor.capture());
                    Assert.assertEquals(bool != null ? bool : false, settingsArgumentCaptor.getValue().isStartAtLogin());
                    Assert.assertEquals(language != null ? language : "es", settingsArgumentCaptor.getValue().getLanguage());
                    Assert.assertEquals(syncInt != null ? syncInt : 37, settingsArgumentCaptor.getValue().getSyncInterval());
                    Assert.assertEquals(List.of(
                            syncJob
                    ), settingsArgumentCaptor.getValue().getJobs());
                    Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printConfig();

                    Mockito.clearInvocations(driveAPICmdLineApp);
                    Mockito.clearInvocations(driveAPIClient);
                }
            }
        }

    }

    @Test
    public void testAddJob() throws Exception {
        Mockito.doNothing().when(driveAPIClient).addSyncJobs(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).printJobs();

        driveAPICmdLineApp.addJob(SyncJob.Type.Download, "/loc-dir", "http://URL", "abcd-1234", "tkn", "/remDIR", true, null);

        Mockito.verify(driveAPIClient, Mockito.times(1)).addSyncJobs(List.of(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true)
        ));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printJobs();
    }

    @Test
    public void testRemoveJob() throws Exception {
        Mockito.doNothing().when(driveAPIClient).removeSyncJobs(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).printJobs();
        Mockito.doReturn(
                List.of(
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true),
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
                )
        ).when(driveAPIClient).getSyncJobs();

        driveAPICmdLineApp.removeJob("/loc-dir2");

        Mockito.verify(driveAPIClient, Mockito.times(1)).removeSyncJobs(Collections.singletonList(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true)
                ));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printJobs();
    }

    @Test
    public void testStartJob() throws Exception {
        Mockito.doNothing().when(driveAPIClient).startSyncJobs(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).printJobs();
        Mockito.doReturn(
                List.of(
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true),
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
                )
        ).when(driveAPIClient).getSyncJobs();

        driveAPICmdLineApp.startJob("/loc-dir2");

        Mockito.verify(driveAPIClient, Mockito.times(1)).startSyncJobs(Collections.singletonList(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true)
        ));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printJobs();
    }

    @Test
    public void testStopJob() throws Exception {
        Mockito.doNothing().when(driveAPIClient).stopSyncJobs(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).printJobs();
        Mockito.doReturn(
                List.of(
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true),
                        new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
                )
        ).when(driveAPIClient).getSyncJobs();

        driveAPICmdLineApp.stopJob("/loc-dir2");

        Mockito.verify(driveAPIClient, Mockito.times(1)).stopSyncJobs(Collections.singletonList(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true)
        ));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printJobs();
    }

    @Test
    public void testPrintJobs() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());
        Settings settings = new Settings();
        settings.setStartAtLogin(true);
        settings.setLanguage("es");
        settings.setSyncInterval(37);
        List<SyncJob> toBeReturnedSyncJobs = List.of(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
        );
        settings.setJobs(new ArrayList<>(toBeReturnedSyncJobs));
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());

        Mockito.doReturn(
                settings
        ).when(driveAPIClient).getSettings();

        driveAPICmdLineApp.printJobs();

        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printSyncJob(toBeReturnedSyncJobs.get(0));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printSyncJob(toBeReturnedSyncJobs.get(1));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printSyncJob(toBeReturnedSyncJobs.get(2));
    }

    @Test
    public void testPrintNotifications() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printNotification(Mockito.any());
        List<Notification> notifications = List.of(Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc").remoteFile("/rem").message("MSG").timestamp(123354L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc").remoteFile("/rem").message("MSG").timestamp(123356L).build(),
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc").remoteFile("/rem").message("MSG").timestamp(123357L).build());
        Mockito.doReturn(
                notifications
        ).when(driveAPIClient).getNotifications(3);

        driveAPICmdLineApp.printNotifications(3);

        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotification(notifications.get(0));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotification(notifications.get(1));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printNotification(notifications.get(2));
    }

    @Test
    public void testPrintNotification() throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.printNotification(
                Notification.builder().type(Notification.Type.Conflict).localDirectory("/dir").localFile("/loc").remoteFile("/rem").message("MSG").timestamp(123357L).build());

        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());
        Assert.assertTrue(allWrittenLines.contains("Conflict"));
        Assert.assertTrue(allWrittenLines.contains("/dir"));
        Assert.assertTrue(allWrittenLines.contains("/loc"));
        Assert.assertTrue(allWrittenLines.contains("/rem"));
        Assert.assertTrue(allWrittenLines.contains("MSG"));
        Assert.assertTrue(allWrittenLines.contains(Instant.ofEpochMilli(123357L).toString()));
    }

    @Test
    public void testPrintEvents() throws Exception{
        Mockito.doNothing().when(driveAPICmdLineApp).printEvent(Mockito.any());
        List<? extends Event> events = List.of(
                SyncJobEvent.builder().syncDirection(Event.SyncDirection.UP).entityPermId("1234").localDirectoryRoot("/dir").localFile("/loc").remoteFile("/rem").timestamp(123354L).sourceTimestamp(123254L).build(),
                SyncJobEvent.builder().syncDirection(Event.SyncDirection.DOWN).entityPermId("1234").localDirectoryRoot("/dir").localFile("/loc").remoteFile("/rem").timestamp(123356L).sourceTimestamp(85673445L).build());
        Mockito.doReturn(
                events
        ).when(driveAPIClient).getEvents(2);

        driveAPICmdLineApp.printEvents(2);

        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvent(events.get(0));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printEvent(events.get(1));
    }

    @Test
    public void testPrintEvent() throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.printEvent(
                SyncJobEvent.builder().syncDirection(Event.SyncDirection.UP).entityPermId("1234").localDirectoryRoot("/dir").localFile("/loc").remoteFile("/rem").timestamp(123354L).sourceTimestamp(123254L).build());

        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());
        Assert.assertTrue(allWrittenLines.contains("UP"));
        Assert.assertTrue(allWrittenLines.contains("/dir"));
        Assert.assertTrue(allWrittenLines.contains("/loc"));
        Assert.assertTrue(allWrittenLines.contains("/rem"));
        Assert.assertTrue(allWrittenLines.contains(Instant.ofEpochMilli(123354L).toString()));
    }

    @Test
    public void testPrintSyncJob() throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.printSyncJob(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true));
        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());
        Assert.assertTrue(allWrittenLines.contains("Download"));
        Assert.assertTrue(allWrittenLines.contains("http://URL"));
        Assert.assertTrue(allWrittenLines.contains("tkn"));
        Assert.assertTrue(allWrittenLines.contains("/remDIR"));
        Assert.assertTrue(allWrittenLines.contains("/loc-dir"));
        Assert.assertTrue(allWrittenLines.contains("true"));
    }

    @Test
    public void testHandleStartCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).startService();
        Mockito.doNothing().when(driveAPICmdLineApp).printHelp();

        driveAPICmdLineApp.handleStartCommand(new String[] { "start" });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).startService();
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printHelp();

        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleStartCommand(new String[] { "start", "other"});
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).startService();
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
    }

    @Test
    public void testHandleStopCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).stopService();
        Mockito.doNothing().when(driveAPICmdLineApp).printHelp();

        driveAPICmdLineApp.handleStopCommand(new String[] { "stop" });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).stopService();
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printHelp();

        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleStopCommand(new String[] { "stop", "other"});
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).stopService();
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
    }

    @Test
    public void testHandleStatusCommand() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).checkServiceStatus();
        Mockito.doNothing().when(driveAPICmdLineApp).printHelp();

        driveAPICmdLineApp.handleStatusCommand(new String[] { "status" });
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).checkServiceStatus();
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printHelp();

        Mockito.clearInvocations(driveAPICmdLineApp);

        driveAPICmdLineApp.handleStatusCommand(new String[] { "status", "other"});
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).checkServiceStatus();
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printHelp();
    }

    @Test
    public void testCheckServiceStatus() throws Exception {
        //STATUS ACTIVE
        Mockito.doReturn(Settings.defaultSettings()).when(driveAPIClient).getSettings();

        driveAPICmdLineApp.checkServiceStatus();

        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printConfig();

        Mockito.clearInvocations(driveAPICmdLineApp);

        //STATUS NOT RUNNING
        Mockito.doThrow(new StatusRuntimeException(Status.UNAVAILABLE)).when(driveAPIClient).getSettings();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.checkServiceStatus();

        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());
        Assert.assertTrue(allWrittenLines.contains("not running"));
    }

    @Test
    public void testShowDefaultHiddenPathPatterns() throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.showDefaultHiddenPathPatterns();
        bo.flush();

        String allWrittenLines = new String(bo.toByteArray());
        for ( String pattern : SyncJob.getDefaultHiddenPathPatterns() ) {
            Assert.assertTrue(allWrittenLines.contains(pattern));
        }
    }

    @Test
    public void testShowHiddenPathPatterns() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());
        List<SyncJob> toBeReturnedSyncJobs = List.of(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true, true, new ArrayList<>(List.of("^/hidden/?", "hidden2\\.txt$"))),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
        );
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());

        Mockito.doReturn(
                toBeReturnedSyncJobs
        ).when(driveAPIClient).getSyncJobs();

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.showHiddenPathPatterns("/loc-dir2");
        bo.flush();

        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printSyncJob(toBeReturnedSyncJobs.get(0));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).printSyncJob(toBeReturnedSyncJobs.get(1));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printSyncJob(toBeReturnedSyncJobs.get(2));

        String allWrittenLines = new String(bo.toByteArray());
        Assert.assertTrue(allWrittenLines.contains(String.format("^/hidden/?%nhidden2\\.txt$")));
    }

    @Test
    public void testShowHiddenPathPatternsNotFound() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());
        List<SyncJob> toBeReturnedSyncJobs = List.of(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true, true, new ArrayList<>(List.of("^/hidden/?", "hidden2\\.txt$"))),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
        );
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());

        Mockito.doReturn(
                toBeReturnedSyncJobs
        ).when(driveAPIClient).getSyncJobs();

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        driveAPICmdLineApp.showHiddenPathPatterns("/loc-dir4");
        bo.flush();

        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printSyncJob(toBeReturnedSyncJobs.get(0));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printSyncJob(toBeReturnedSyncJobs.get(1));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(0)).printSyncJob(toBeReturnedSyncJobs.get(2));

        String allWrittenLines = new String(bo.toByteArray());
        Assert.assertTrue(allWrittenLines.contains(String.format("not found")));
    }

    @Test
    public void testResetHiddenPathPatterns() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());
        List<SyncJob> toBeReturnedSyncJobs = List.of(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true, true, new ArrayList<>(List.of("^/hidden/?", "hidden2\\.txt$"))),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
        );
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).showHiddenPathPatterns(Mockito.any());

        Mockito.doReturn(
                toBeReturnedSyncJobs
        ).when(driveAPIClient).getSyncJobs();

        driveAPICmdLineApp.resetHiddenPathPatternsToDefault("/loc-dir2");

        ArgumentCaptor<List<SyncJob>> syncJobArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(driveAPIClient, Mockito.times(1)).removeSyncJobs(syncJobArgumentCaptor.capture());
        Assert.assertEquals("/loc-dir2", syncJobArgumentCaptor.getValue().get(0).getLocalDirectoryRoot());
        Mockito.verify(driveAPIClient, Mockito.times(1)).addSyncJobs(Collections.singletonList(new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true, true, new ArrayList<>(SyncJob.getDefaultHiddenPathPatterns()))));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).showHiddenPathPatterns("/loc-dir2");
    }

    @Test
    public void testSetHiddenPathPatterns() throws Exception {
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());
        List<SyncJob> toBeReturnedSyncJobs = List.of(
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir", true),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true, true, new ArrayList<>(List.of("^/hidden/?", "hidden2\\.txt$"))),
                new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir3", true)
        );
        Mockito.doNothing().when(driveAPICmdLineApp).printSyncJob(Mockito.any());
        Mockito.doNothing().when(driveAPICmdLineApp).showHiddenPathPatterns(Mockito.any());

        Mockito.doReturn(
                toBeReturnedSyncJobs
        ).when(driveAPIClient).getSyncJobs();

        driveAPICmdLineApp.setHiddenPathPatterns("/loc-dir2", String.format("^/root/?%n^/boot/?%n\\.exe$"));

        ArgumentCaptor<List<SyncJob>> syncJobArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(driveAPIClient, Mockito.times(1)).removeSyncJobs(syncJobArgumentCaptor.capture());
        Assert.assertEquals("/loc-dir2", syncJobArgumentCaptor.getValue().get(0).getLocalDirectoryRoot());
        Mockito.verify(driveAPIClient, Mockito.times(1)).addSyncJobs(Collections.singletonList(new SyncJob(SyncJob.Type.Download, "http://URL", "tkn", "abcd-1234", "/remDIR", "/loc-dir2", true, true, new ArrayList<>(List.of("^/root/?", "^/boot/?", "\\.exe$")))));
        Mockito.verify(driveAPICmdLineApp, Mockito.times(1)).showHiddenPathPatterns("/loc-dir2");
    }
}