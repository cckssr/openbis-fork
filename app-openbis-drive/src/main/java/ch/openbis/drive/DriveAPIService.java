package ch.openbis.drive;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.protobuf.DriveAPIGrpcImpl;
import ch.openbis.drive.util.OsDetectionUtil;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public class DriveAPIService {
    public static final String LOCK_FILE_NAME = ".openbis-drive.lock";
    public static final String SOCKET_FILE_NAME = ".openbis-drive.socket";

    public static void main(String[] args) throws Exception {
        DriveAPIService driveAPIService = new DriveAPIService();
        driveAPIService.start();

        Runtime.getRuntime().addShutdownHook( new Thread(
                new Runnable() {
                    @Override
                    @SneakyThrows
                    public void run() {
                        driveAPIService.stop();
                    }
                }
        ));

        driveAPIService.awaitTermination();
    }

    final Configuration configuration;
    final FileLock applicationFileLock;
    final DriveAPIServerImpl driveAPIServer;
    Server server;

    boolean started;

    public DriveAPIService() throws Exception {
        this.configuration = new Configuration();
        applicationFileLock = FileChannel.open(configuration.getLocalAppStateDirectory().resolve(LOCK_FILE_NAME), StandardOpenOption.CREATE, StandardOpenOption.WRITE).tryLock();
        driveAPIServer = new DriveAPIServerImpl(configuration);

        if(applicationFileLock == null) {
            throw new IllegalStateException("DriveAPIService instance already running");
        }
    }

    public synchronized void start() throws Exception {
        if (!started) {
            driveAPIServer.setSettings(Optional.ofNullable(driveAPIServer.getSettings()).orElse(Settings.defaultSettings()));

            try {

                switch (OsDetectionUtil.detectOS()) {

                    case Linux -> {
                        Files.deleteIfExists(configuration.getLocalAppStateDirectory().resolve(SOCKET_FILE_NAME));
                        server = NettyServerBuilder.forAddress(new DomainSocketAddress(configuration.getLocalAppStateDirectory().resolve(DriveAPIService.SOCKET_FILE_NAME).toString()))
                            .channelType(EpollServerDomainSocketChannel.class)
                            .workerEventLoopGroup(new EpollEventLoopGroup())
                            .bossEventLoopGroup(new EpollEventLoopGroup())
                            .addService(new DriveAPIGrpcImpl(driveAPIServer))
                            .build()
                            .start();
                    }

                    case Windows, Mac -> {
                        server = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", configuration.getOpenbisDrivePort()))
                            .addService(new DriveAPIGrpcImpl(driveAPIServer))
                            .build()
                            .start();
                    }

                    case Unknown -> throw new IllegalStateException("Unknown operating-system");
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            started = true;
        }
    }

    public synchronized void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

    public synchronized void stop() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

}
