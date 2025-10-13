package ch.openbis.drive.protobuf.client;

import ch.openbis.drive.DriveAPI;
import ch.openbis.drive.DriveAPIService;
import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.protobuf.*;
import ch.openbis.drive.protobuf.converters.ProtobufConversionUtil;
import ch.openbis.drive.util.OsDetectionUtil;
import io.grpc.*;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;
import lombok.NonNull;
import lombok.SneakyThrows;


import java.net.InetSocketAddress;
import java.util.List;

public class DriveAPIClientProtobufImpl implements DriveAPI, AutoCloseable {

    final ManagedChannel grpcManagedChannel;
    final EpollEventLoopGroup eventLoopGroup;

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    public DriveAPIClientProtobufImpl(Configuration configuration) throws Exception {
        switch (OsDetectionUtil.detectOS()) {
            case Linux, Mac -> {
                eventLoopGroup = new EpollEventLoopGroup();
                grpcManagedChannel = getGrpcChannel(configuration);
            }
            case Windows -> {
                eventLoopGroup = null;
                grpcManagedChannel = NettyChannelBuilder.forAddress(new InetSocketAddress("localhost", configuration.getOpenbisDrivePort()))
                        .withOption(ChannelOption.SO_KEEPALIVE, null)
                        .usePlaintext()
                        .build();
            }
            default -> throw new IllegalStateException("Unknown operating-system");
        }
    }

    private ManagedChannel getGrpcChannel(Configuration configuration) {
        return NettyChannelBuilder.forAddress(new DomainSocketAddress(configuration.getLocalAppStateDirectory().resolve(DriveAPIService.SOCKET_FILE_NAME).toString()))
                .withOption(ChannelOption.SO_KEEPALIVE, null)
                .usePlaintext()
                .channelType(EpollDomainSocketChannel.class)
                .eventLoopGroup(eventLoopGroup)
                .build();
    }

    @SneakyThrows
    synchronized public void setSettings(@NonNull Settings settings) {
        DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).setSettings(ProtobufConversionUtil.toProtobufSettings(settings));
    }

    @SneakyThrows
    synchronized public @NonNull Settings getSettings() {
        return ProtobufConversionUtil.fromProtobufSettings(DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).getSettings(DriveApiService.Empty.newBuilder().build()));
    }

    @SneakyThrows
    synchronized public @NonNull List<@NonNull SyncJob> getSyncJobs() {
        return ProtobufConversionUtil.fromProtobufSyncJobs(DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).getSyncJobs(DriveApiService.Empty.newBuilder().build()));
    }

    @SneakyThrows
    synchronized public void addSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).addSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs));
    }

    @SneakyThrows
    synchronized public void removeSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).removeSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs));
    }

    @SneakyThrows
    synchronized public void startSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).startSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs));
    }

    @SneakyThrows
    synchronized public void stopSyncJobs(@NonNull List<@NonNull SyncJob> syncJobs) {
        DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).stopSyncJobs(ProtobufConversionUtil.toProtobufSyncJobs(syncJobs));
    }

    @SneakyThrows
    synchronized public @NonNull List<? extends Event> getEvents(@NonNull Integer limit) {
        return ProtobufConversionUtil.fromProtobufEvents(DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).getEvents(DriveApiService.Limit.newBuilder().setLimit(limit).build()));
    }

    @SneakyThrows
    synchronized @NonNull public List<Notification> getNotifications(@NonNull Integer limit) {
        return ProtobufConversionUtil.fromProtobufNotifications(DriveAPIServiceGrpc.newBlockingStub(grpcManagedChannel).getNotifications(DriveApiService.Limit.newBuilder().setLimit(limit).build()));
    }

    @Override
    public void close() throws Exception {
        grpcManagedChannel.shutdownNow();
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
