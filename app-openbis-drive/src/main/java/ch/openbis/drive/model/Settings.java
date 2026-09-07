package ch.openbis.drive.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Settings {
    public static final int DEFAULT_EXPIRING_SESSION_WARNING_DAYS = 7;

    private boolean startAtLogin;
    private String language;
    private int syncInterval; //Seconds
    private ArrayList<@NonNull SyncJob> jobs;
    private ArrayList<String> ignoredPathPatterns;
    private Integer expiringSessionWarningDays;
    //TODO private Proxy proxy;

    public static Settings defaultSettings() {
        return new Settings(
                false,
                "en",
                2 * 60,
                new ArrayList<>(),
                new ArrayList<>(SyncJob.getDefaultIgnoredPathPatterns()),
                DEFAULT_EXPIRING_SESSION_WARNING_DAYS
        );
    }

    public ArrayList<@NonNull SyncJob> getJobs() {
        if ( jobs == null ) {
            jobs = new ArrayList<>();
        }
        return jobs;
    }

    @NonNull
    public Integer getExpiringSessionWarningDays() {
        return Optional.ofNullable(expiringSessionWarningDays)
                .filter(value -> value > 0)
                .orElse(DEFAULT_EXPIRING_SESSION_WARNING_DAYS);
    }
}
