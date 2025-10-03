package ch.openbis.drive.util;

import java.util.Optional;

public class OsDetectionUtil {
    public enum OS { Linux, Windows, Mac, Unknown }
    public static OS detectOS() {
        String osName = Optional.ofNullable(System.getProperty("os.name")).map(String::toLowerCase).orElse(null);
        if (osName == null) {
            return OS.Unknown;
        } else {
            if (osName.contains("linux")) {
                return OS.Linux;
            } else if (osName.contains("win")) {
                return OS.Windows;
            } else if (osName.contains("mac")) {
                return OS.Mac;
            } else {
                return OS.Unknown;
            }
        }
    }
}
