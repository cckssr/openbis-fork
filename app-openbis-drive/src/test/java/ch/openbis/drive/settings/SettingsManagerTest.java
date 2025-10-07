package ch.openbis.drive.settings;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.notifications.NotificationManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class SettingsManagerTest {

    private final Configuration configuration;
    private final SettingsManager settingsManager;

    public SettingsManagerTest() throws Exception {
        configuration = new Configuration(Path.of(this.getClass().getClassLoader().getResource("placeholder.txt").getPath()).getParent().resolve("settings-manager-test"));
        Files.createDirectories(configuration.getLocalAppStateDirectory());
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);
        SyncJobEventDAO syncJobEventDAO = Mockito.mock(SyncJobEventDAO.class);
        settingsManager = Mockito.spy(new SettingsManager(configuration, syncJobEventDAO, notificationManager));
        Mockito.doNothing().when(settingsManager).addStartAtLogin();
        Mockito.doNothing().when(settingsManager).removeStartAtLogin();
    }

    @Before
    synchronized public void before() throws Exception {
        settingsManager.setSettings(Settings.defaultSettings());
        Mockito.clearInvocations(settingsManager);
    }
    

    @Test
    synchronized public void setAndGetSettingsTest() throws Exception {
        Settings settings1 = Settings.defaultSettings();

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "localdir1", false);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "localdir2", true);
        Settings settings2 = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        settingsManager.setSettings(settings1);
        Assert.assertEquals(settings1, settingsManager.getSettings());

        settingsManager.setSettings(settings2);
        Assert.assertEquals(settings2, settingsManager.getSettings());
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(syncJob1);
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(syncJob2);

        settingsManager.setSettings(settings1);
        Assert.assertEquals(settings1, settingsManager.getSettings());
        Mockito.verify(settingsManager, Mockito.times(1)).cleanSyncJobApplicationFiles(syncJob1);
        Mockito.verify(settingsManager, Mockito.times(1)).cleanSyncJobApplicationFiles(syncJob2);
    }

    @Test
    synchronized public void addAndRemoveStartAtLoginTest() throws Exception {
        Settings settings = Settings.defaultSettings();
        settings.setStartAtLogin(false);
        settingsManager.setSettings(settings);
        Mockito.clearInvocations(settingsManager);

        settings.setLanguage("en");
        settings.setStartAtLogin(true);
        settingsManager.setSettings(settings);
        Mockito.verify(settingsManager, Mockito.times(1)).addStartAtLogin();
        Mockito.verify(settingsManager, Mockito.times(0)).removeStartAtLogin();
        Mockito.clearInvocations(settingsManager);

        settings.setLanguage("it");
        settings.setStartAtLogin(false);
        settingsManager.setSettings(settings);
        Mockito.verify(settingsManager, Mockito.times(0)).addStartAtLogin();
        Mockito.verify(settingsManager, Mockito.times(1)).removeStartAtLogin();
        Mockito.clearInvocations(settingsManager);

        settings.setLanguage("en");
        settingsManager.setSettings(settings);
        Mockito.verify(settingsManager, Mockito.times(0)).addStartAtLogin();
        Mockito.verify(settingsManager, Mockito.times(0)).removeStartAtLogin();
        Mockito.clearInvocations(settingsManager);

        settings.setLanguage("it");
        settings.setStartAtLogin(true);
        settingsManager.setSettings(settings);
        Mockito.verify(settingsManager, Mockito.times(1)).addStartAtLogin();
        Mockito.verify(settingsManager, Mockito.times(0)).removeStartAtLogin();
        Mockito.clearInvocations(settingsManager);

        settings.setLanguage("en");
        settings.setStartAtLogin(true);
        settingsManager.setSettings(settings);
        Mockito.verify(settingsManager, Mockito.times(0)).addStartAtLogin();
        Mockito.verify(settingsManager, Mockito.times(0)).removeStartAtLogin();
        Mockito.clearInvocations(settingsManager);

        settings.setLanguage("it");
        settings.setStartAtLogin(false);
        settingsManager.setSettings(settings);
        Mockito.verify(settingsManager, Mockito.times(0)).addStartAtLogin();
        Mockito.verify(settingsManager, Mockito.times(1)).removeStartAtLogin();
        Mockito.clearInvocations(settingsManager);
    }

    @Test
    synchronized public void getSyncJobsTest() throws Exception {
        Settings settings1 = Settings.defaultSettings();

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "localdir1", false);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "localdir2", true);
        Settings settings2 = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        settingsManager.setSettings(settings1);
        Assert.assertEquals(Collections.emptyList(), settingsManager.getSyncJobs());
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(syncJob1);
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(syncJob2);

        settingsManager.setSettings(settings2);
        Assert.assertEquals(List.of(syncJob1, syncJob2), settingsManager.getSyncJobs());
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(syncJob1);
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(syncJob2);
    }

    @Test(expected = IllegalArgumentException.class)
    synchronized public void setInvalidSettingsForOverlappingLocalDirTest() {

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "localdir1/subdir", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "localdir1", false);
        Settings settings = new Settings(true, "it", 15, new ArrayList<>(List.of(syncJob1, syncJob2)));

        settingsManager.setSettings(settings);
    }

    @Test
    synchronized public void addSyncJobsTest() throws Exception {
        settingsManager.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "localdir1", true);
        settingsManager.addSyncJobs(Collections.singletonList(syncJob1));
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(Mockito.any(SyncJob.class));

        Assert.assertEquals(List.of(syncJob1), settingsManager.getSettings().getJobs());

        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "localdir2", true);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "remotedir3", "localdir3", false);
        settingsManager.addSyncJobs(List.of(syncJob2, syncJob3));
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(Mockito.any(SyncJob.class));
        Mockito.verify(settingsManager, Mockito.times(0)).cleanSyncJobApplicationFiles(Mockito.any(SyncJob.class));

        Assert.assertEquals(List.of(syncJob1, syncJob2, syncJob3), settingsManager.getSettings().getJobs());
    }

    @Test
    synchronized public void removeSyncJobsTest() throws Exception {
        settingsManager.setSettings(Settings.defaultSettings());

        SyncJob syncJob1 = new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "localdir1", true);
        SyncJob syncJob2 = new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "localdir2", false);
        SyncJob syncJob3 = new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "remotedir3", "localdir3", false);
        settingsManager.addSyncJobs(List.of(syncJob1, syncJob2, syncJob3));

        Assert.assertEquals(List.of(syncJob1, syncJob2, syncJob3), settingsManager.getSettings().getJobs());

        settingsManager.removeSyncJobs(Collections.singletonList(new SyncJob(SyncJob.Type.Bidirectional, "url2", "token2", "id2", "remotedir2", "localdir2", false)));
        Assert.assertEquals(List.of(syncJob1, syncJob3), settingsManager.getSettings().getJobs());
        Mockito.verify(settingsManager).cleanSyncJobApplicationFiles(syncJob2);

        settingsManager.removeSyncJobs(
                List.of(new SyncJob(SyncJob.Type.Bidirectional, "url1", "token1", "id1", "remotedir1", "localdir1", true),
                        new SyncJob(SyncJob.Type.Bidirectional, "url3", "token3", "id3", "remotedir3", "localdir3", false)));
        Assert.assertEquals(Collections.emptyList(), settingsManager.getSettings().getJobs());
        Mockito.verify(settingsManager).cleanSyncJobApplicationFiles(syncJob1);
        Mockito.verify(settingsManager).cleanSyncJobApplicationFiles(syncJob3);
    }
}
