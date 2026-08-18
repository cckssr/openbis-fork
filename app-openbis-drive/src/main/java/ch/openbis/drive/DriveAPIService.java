package ch.openbis.drive;

import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.gui.i18n.I18n;
import ch.openbis.drive.logging.Logging;
import ch.openbis.drive.protobuf.DriveAPIGrpcImpl;
import ch.openbis.drive.util.OsDetectionUtil;
import ch.openbis.drive.util.SystemTrayUtil;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;
import lombok.SneakyThrows;

import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public class DriveAPIService {
    public static final String LOCK_FILE_NAME = ".openbis-drive.lock";
    public static final String SOCKET_FILE_NAME = ".openbis-drive.socket";

    static Logger logger;

    public static void main(String[] args) throws Exception {
        Logging.initializeBackgroundServiceLogging();
        logger = LogManager.getLogger(DriveAPIService.class);

        DriveAPIService driveAPIService;
        try {
            driveAPIService = new DriveAPIService();
        } catch (Exception e) {
            logger.catching(e);
            return;
        }

        logger.info("STARTING BACKGROUND SERVICE");

        try {
            driveAPIService.start();

            Runtime.getRuntime().addShutdownHook( new Thread(
                    new Runnable() {
                        @Override
                        @SneakyThrows
                        public void run() {
                            driveAPIService.server.shutdownNow();
                        }
                    }
            ));

            driveAPIService.awaitTermination();
        } catch (Error e) {
            logger.catching(e);
            throw e;
        }
    }

    final Configuration configuration;
    final FileLock applicationFileLock;
    final DriveAPIServerImpl driveAPIServer;
    final SystemTrayUtil systemTrayUtil;
    Server server;

    boolean started;

    public DriveAPIService() throws Exception {
        this.configuration = new Configuration();
        configuration.readOpenbisDriveProperties();
        applicationFileLock = FileChannel.open(configuration.getLocalAppStateDirectory().resolve(LOCK_FILE_NAME), StandardOpenOption.CREATE, StandardOpenOption.WRITE).tryLock();
        systemTrayUtil = new SystemTrayUtil(
                new I18n(Locale.getDefault().getLanguage()),
                (_void) -> { this.stop(); return null; }
        );
        driveAPIServer = new DriveAPIServerImpl(configuration, systemTrayUtil);

        if(applicationFileLock == null) {
            throw new IllegalStateException("DriveAPIService instance already running");
        }
    }

    public synchronized void start() throws Exception {
        if (!started) {
            driveAPIServer.start();

            systemTrayUtil.addAppToTray();

            try {

                switch (OsDetectionUtil.detectOS()) {

                    case Linux -> {
                        Files.deleteIfExists(configuration.getLocalAppStateDirectory().resolve(SOCKET_FILE_NAME));
                        server = NettyServerBuilder.forAddress(new DomainSocketAddress(configuration.getLocalAppStateDirectory().resolve(DriveAPIService.SOCKET_FILE_NAME).toString()))
                            .channelType(EpollServerDomainSocketChannel.class)
                            .workerEventLoopGroup(new EpollEventLoopGroup())
                            .bossEventLoopGroup(new EpollEventLoopGroup())
                            .addService(new DriveAPIGrpcImpl(driveAPIServer, configuration))
                            .build()
                            .start();
                    }

                    case Windows, Mac -> {
                        server = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", configuration.getOpenbisDrivePort()))
                            .addService(new DriveAPIGrpcImpl(driveAPIServer, configuration))
                            .build()
                            .start();
                    }

                    case Unknown -> throw new IllegalStateException("Unknown operating-system");
                }
            } catch ( Exception e ) {
                logger.catching(e);
            }
            started = true;
        }
    }

    public synchronized void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

    public void stop() {
        try {
            if (server != null) {
                this.server.shutdownNow();
                this.server.awaitTermination();
            }
            System.exit(0);
        } catch (Exception e) {
            logger.catching(e);
            System.exit(1);
        }
    }

}
