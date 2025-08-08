package ch.openbis.drive.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Settings {
    private boolean startAtLogin;
    private String language;
    private int syncInterval; //Seconds
    private ArrayList<@NonNull SyncJob> jobs;
    //TODO private Proxy proxy;

    public static Settings defaultSettings() {
        return new Settings(false, "en", 2 * 60, new ArrayList<>());
    }

    public ArrayList<@NonNull SyncJob> getJobs() {
        if ( jobs == null ) {
            jobs = new ArrayList<>();
        }
        return jobs;
    }
}
