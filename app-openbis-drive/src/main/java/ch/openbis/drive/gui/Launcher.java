package ch.openbis.drive.gui;

import ch.openbis.drive.conf.Configuration;
import javafx.application.Application;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

public class Launcher {
    public static final String GUI_LOCK_FILE_NAME = ".openbis-drive-gui.lock";

    public static void main(String[] args) throws Exception{
        try ( FileLock applicationFileLock =
                      FileChannel.open(new Configuration().getLocalAppStateDirectory().resolve(GUI_LOCK_FILE_NAME),
                              StandardOpenOption.CREATE, StandardOpenOption.WRITE).tryLock() ) {
            if (applicationFileLock != null) {
                System.setProperty("javafx.preloader", Preloader.class.getName());
                Application.launch(OpenDriveApplication.class, args);
            } else {
                throw new IllegalStateException("openBISDrive GUI instance already running");
            }
        }
    }
}
