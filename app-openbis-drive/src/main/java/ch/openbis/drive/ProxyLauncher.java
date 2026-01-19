package ch.openbis.drive;

import ch.openbis.drive.gui.Launcher;
import ch.openbis.drive.util.OpenBISDriveUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ProxyLauncher {
    public static void main(String[] args) throws Exception{
        if (args.length == 1 && "background-process".equals(args[0])) {
            DriveAPIService.main(new String[0]);
        } else if (args.length >= 1) {
                switch (args[0]) {
                    default -> DriveAPICmdLineApp.main(args);
                }
        } else {
            Launcher.main(new String[0]);
        }
    }
}
