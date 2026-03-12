package ch.openbis.drive;

import ch.openbis.drive.gui.Launcher;

public class ProxyLauncher {
    public static void main(String[] args) throws Exception{
        if (args.length == 1 && "background-process".equals(args[0])) {
            DriveAPIService.main(new String[0]);
        } else if (args.length >= 1) {
            switch (args[0]) {
                case "gui" -> Launcher.main(args);
                default -> DriveAPICmdLineApp.main(args);
            }
        } else {
            Launcher.main(new String[0]);
        }
    }
}
