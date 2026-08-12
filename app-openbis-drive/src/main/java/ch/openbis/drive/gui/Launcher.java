package ch.openbis.drive.gui;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.logging.Logging;
import javafx.application.Application;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

public class Launcher {
    public static final String GUI_LOCK_FILE_NAME = ".openbis-drive-gui.lock";

    static Logger logger;

    public static void main(String[] args) throws Exception {
        Logging.initializeGraphicalInterfaceLogging();
        logger = LogManager.getLogger(Launcher.class);
        logger.info("STARTING GUI");

        try ( FileLock applicationFileLock =
                      FileChannel.open(new Configuration().getLocalAppStateDirectory().resolve(GUI_LOCK_FILE_NAME),
                              StandardOpenOption.CREATE, StandardOpenOption.WRITE).tryLock() ) {
            if (applicationFileLock != null) {
                System.setProperty("javafx.preloader", Preloader.class.getName());
                Application.launch(OpenDriveApplication.class, args);
            } else {
                throw new IllegalStateException("openBISDrive GUI instance already running");
            }
        } catch (Exception e) {
            logger.catching(e);
            throw e;
        }
    }
}
