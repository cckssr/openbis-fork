package ch.openbis.drive.settings;

import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.db.SyncJobEventDAO;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.notifications.NotificationManager;
import ch.openbis.drive.util.OpenBISDriveUtil;
import ch.openbis.drive.util.StartAtLoginUtil;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class SettingsManager {
    public static final String DEFAULT_SETTINGS_FILE = "settings.json";
    public static final String DEFAULT_BACKUP_SETTINGS_FILE = "settings.json.bk";
    public static final HashSet<String> supportedLangauges = new HashSet<>(Set.of("en", "de", "fr", "it", "es"));

    private final Configuration configuration;
    private final Path settingsPath;
    private final Path backupSettingsPath;
    private final SyncJobEventDAO syncJobEventDAO;
    private final NotificationManager notificationManager;


    public SettingsManager(@NonNull Configuration configuration, @NonNull SyncJobEventDAO syncJobEventDAO, @NonNull NotificationManager notificationManager) {
        this.configuration = configuration;
        this.settingsPath = Path.of(configuration.getLocalAppDirectory().toString(), DEFAULT_SETTINGS_FILE);
        this.backupSettingsPath = Path.of(configuration.getLocalAppDirectory().toString(), DEFAULT_BACKUP_SETTINGS_FILE);
        this.syncJobEventDAO = syncJobEventDAO;
        this.notificationManager = notificationManager;
    }

    @SneakyThrows
    synchronized public void setSettings(@NonNull Settings settings) {
        validateSettings(settings);

        Optional<Settings> currentSettings;
        try {
            currentSettings = readSettingsFromPath(settingsPath);
        } catch (Exception e) {
            currentSettings = Optional.empty();
        }
        if(currentSettings.isPresent()) {
            Files.copy(settingsPath, backupSettingsPath, StandardCopyOption.REPLACE_EXISTING);
        }

        if(!Files.exists(settingsPath)) {
            Files.createFile(settingsPath);
        }

        cleanSyncJobApplicationFiles(settings.getJobs());

        if(currentSettings.isEmpty() || currentSettings.get().isStartAtLogin() != settings.isStartAtLogin()) {
            if(settings.isStartAtLogin()) {
                addStartAtLogin();
            } else {
                removeStartAtLogin();
            }
        }

        try (FileChannel fileChannel = FileChannel.open(settingsPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] bytes = JacksonObjectMapper.getInstance().writeValue(settings);
            int writtenBytes = fileChannel.write(ByteBuffer.wrap(bytes));
            if (writtenBytes != bytes.length) {
                throw new IllegalStateException("Settings could not be properly written");
            }
        } catch (Exception e){
            if(currentSettings.isPresent()) {
                Files.copy(backupSettingsPath, settingsPath, StandardCopyOption.REPLACE_EXISTING);
            }
            throw e;
        }
    }

    @SneakyThrows
    synchronized public Settings getSettings() {
        Optional<Settings> settings;
        try {
            settings = readSettingsFromPath(settingsPath);
        } catch (Exception e) {
            settings = readSettingsFromPath(backupSettingsPath);
            if(settings.isPresent()) {
                Files.copy(backupSettingsPath, settingsPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return settings.orElse(null);
    }

    synchronized public @NonNull List<@NonNull SyncJob> getSyncJobs() {
        return Optional.ofNullable(getSettings())
                .map( settings -> (List<SyncJob>) settings.getJobs() )
                .orElse(Collections.emptyList());
    }

    synchronized public void addSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        Settings currentSettings = getSettings();
        if (currentSettings == null) {
            currentSettings = Settings.defaultSettings();
        }
        if (currentSettings.getJobs() == null) { currentSettings.setJobs(new ArrayList<>()); }
        currentSettings.getJobs().addAll(syncJobs);

        setSettings(currentSettings);
    }

    synchronized public void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        Settings currentSettings = getSettings();
        if (currentSettings == null) {
            currentSettings = Settings.defaultSettings();
        }
        if (currentSettings.getJobs() == null) { currentSettings.setJobs(new ArrayList<>()); }

        HashSet<SyncJob> syncJobsAsHashSet = new HashSet<>(syncJobs);
        currentSettings.getJobs().removeIf(syncJobsAsHashSet::contains);

        setSettings(currentSettings);
    }

    synchronized private Optional<Settings> readSettingsFromPath(@NonNull Path path) throws Exception {
        if ( Files.exists(path) ) {
            byte[] bytes = Files.readAllBytes(path);
            if(bytes.length > 0) {
                return Optional.of(JacksonObjectMapper.getInstance().readValue(new ByteArrayInputStream(bytes), Settings.class));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /*
     * Validates that local directories are not overlapping
     */
    void validateSettings(@NonNull Settings settings) {
        if (settings.getJobs() != null) {

            ArrayList<Path> localDirValues = settings.getJobs().stream()
                    .map( SyncJob::getLocalDirectoryRoot).map(Path::of).map(Path::toAbsolutePath).collect(Collectors.toCollection(ArrayList::new));

            for(int i=0; i<localDirValues.size(); i++) {
                for(int j=i+1; j<localDirValues.size(); j++) {
                    if(localDirValues.get(i).startsWith(localDirValues.get(j)) || localDirValues.get(j).startsWith(localDirValues.get(i))) {
                        throw new IllegalArgumentException("Overlapping local-directories among sync-jobs");
                    }
                }
            }
        }
        if(settings.getLanguage() == null || !supportedLangauges.contains(settings.getLanguage())) {
            throw new IllegalArgumentException(String.format("Language must be one of %s", String.join(",", supportedLangauges)));
        }
    }

    synchronized void cleanSyncJobApplicationFiles(@NonNull List<@NonNull SyncJob> newSyncJobs) throws Exception {
        List<SyncJob> currentJobs = Optional.ofNullable(getSettings()).map(Settings::getJobs).orElse(new ArrayList<>());
        for(SyncJob currentJob : currentJobs) {
            if(newSyncJobs.stream().noneMatch( newSyncJob -> areEqualExceptEnabled(newSyncJob, currentJob))) {
                cleanSyncJobApplicationFiles(currentJob);
            }
        }
    }

    public static boolean areEqualExceptEnabled(@NonNull SyncJob syncJob1, @NonNull SyncJob syncJob2) {
        return syncJob1.getType() == syncJob2.getType() &&
                syncJob1.getLocalDirectoryRoot().equals(syncJob2.getLocalDirectoryRoot()) &&
                syncJob1.getRemoteDirectoryRoot().equals(syncJob2.getRemoteDirectoryRoot()) &&
                syncJob1.getOpenBisUrl().equals(syncJob2.getOpenBisUrl()) &&
                syncJob1.getEntityPermId().equals(syncJob2.getEntityPermId()) &&
                syncJob1.getOpenBisPersonalAccessToken().equals(syncJob2.getOpenBisPersonalAccessToken());
    }

    synchronized void cleanSyncJobApplicationFiles(@NonNull SyncJob removedSyncJob) throws Exception {
        syncJobEventDAO.removeByLocalDirectoryRoot(removedSyncJob.getLocalDirectoryRoot());
        notificationManager.clearNotificationsForSyncJob(removedSyncJob);
    }

    synchronized void addStartAtLogin() throws Exception {
        StartAtLoginUtil.addStartAtLogin(configuration);
    }

    synchronized void removeStartAtLogin() throws Exception {
        StartAtLoginUtil.removeStartAtLogin(configuration);
    }
}
