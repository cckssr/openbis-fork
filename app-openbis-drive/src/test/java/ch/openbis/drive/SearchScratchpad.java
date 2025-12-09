package ch.openbis.drive;


import ch.ethz.sis.openbis.generic.OpenBIS;
import lombok.SneakyThrows;

//TODO Scratchpad to be removed
public class SearchScratchpad {
    private static final int HTTP_SERVER_PORT = 8085;
    private static final String HTTP_SERVER_PATH = "/afs-server";
    private static final String TEST_OWNER = "a7bc2fbd-49af-4e2d-86cc-ea316028b793";

    public static void searchExample() {
        OpenBIS openbis = new OpenBIS("https://openbis-sis-ci-sprint.ethz.ch");
//        openbis.setSessionToken();
        String sessionToken = openbis.login("admin", "changeit");
        System.out.println(sessionToken);
        openbis.logout();
    }
    public static void main(String[] args) throws Exception {
        DriveAPIService driveAPIService = new DriveAPIService();
        driveAPIService.start();

//        DriveAPIClientProtobufImpl driveAPIImpl = new DriveAPIClientProtobufImpl(new Configuration());
//        Settings settings = Settings.defaultSettings();
//        settings.setSyncInterval(2);
//        driveAPIImpl.setSettings(settings);
//        driveAPIImpl.removeSyncJobs(driveAPIImpl.getSyncJobs());
//
//        AfsClient afsClient;
//        afsClient = new AfsClient(
//                new URI("http", null, "localhost", HTTP_SERVER_PORT,
//                        HTTP_SERVER_PATH, null, null), 10485760, 30000);
//        String sessionToken = afsClient.login("user", "pwd");
//
//        if(AfsClientUploadHelper.getServerFilePresence(afsClient, TEST_OWNER, "/remotedir").isEmpty()) {
//            afsClient.create(TEST_OWNER, "/remotedir", true);
//        }
//
//        SyncJob mySyncJob = new SyncJob(SyncJob.Type.Bidirectional,
//                "http://localhost:8085/afs-server",
//                sessionToken,
//                TEST_OWNER,
//                "/remotedir",
//                Path.of(System.getProperty("user.home")).resolve("openbis_sync_test").toAbsolutePath().toString(),
//                true);
//
//
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            @Override
//            public void run() {
//                driveAPIImpl.removeSyncJobs(List.of(mySyncJob));
//            }
//        }));
//
//        driveAPIImpl.addSyncJobs( List.of(mySyncJob));

        new Thread(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                while (true) {
                    //DriveAPI driveAPIRef = driveAPIImpl;
                    try {
                        //System.out.println(Arrays.toString(afsClient.list(TEST_OWNER, "/remotedir", false)));
                        //In debug session: put a thread-only blocking breakpoint on 'sleep' and
                        //evaluate expressions with 'openBISSyncClientRef' to monitor and test different cases
                        Thread.sleep(5000);
                    } catch (Exception e) {}
                }
            }
        }).start();
    }
}
