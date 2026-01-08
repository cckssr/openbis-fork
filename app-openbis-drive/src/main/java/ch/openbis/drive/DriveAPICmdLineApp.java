package ch.openbis.drive;

import ch.openbis.drive.conf.Configuration;
import ch.openbis.drive.model.Event;
import ch.openbis.drive.model.Notification;
import ch.openbis.drive.model.Settings;
import ch.openbis.drive.model.SyncJob;
import ch.openbis.drive.protobuf.client.DriveAPIClientProtobufImpl;
import ch.openbis.drive.util.GlobUtil;
import ch.openbis.drive.util.OpenBISDriveUtil;
import com.sun.istack.NotNull;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.NonNull;
import org.apache.commons.cli.*;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static ch.ethz.sis.afsclient.client.AfsClientUploadHelper.toServerPathString;

public class DriveAPICmdLineApp {

    /**
     * ./drive-app help
     * <p>
     * ./drive-app start
     * ./drive-app stop
     * ./drive-app status
     * <p>
     * Prints config on the standard output, in one line, fields separated by tabs
     * ./drive-app config
     * ./drive-app config -startAtLogin=true|false -language=en|fr|de|it|es syncInterval=120 (-> seconds) (defaults: false, 'en', 120 seconds = 2 minutes)
     * ./drive-app config ignored-path-patterns -> shows the active global default list of ignored file patterns (glob syntax)
     * ./drive-app config ignored-path-patterns -showFactoryDefault -> shows the factory-default (not necessarily active) global default list of ignored file patterns (glob syntax)
     * ./drive-app config ignored-path-patterns -reset -> resets the active global default list of ignored file patterns to factory-default values
     * ./drive-app config ignored-path-patterns -set -> sets new ignored path-patterns from console-input
     * ./drive-app config ignored-path-patterns -setFromFile -> sets new ignored path-patterns
     *     from UTF-8 multiline text-file with a glob expression on each line
     * <p>
     * Prints jobs on the standard output, one per line, fields separated by tabs
     * ./drive-app jobs
     * ./drive-app jobs add -title='Description...' -type='Bidirectional|Upload|Download' -dir='./dir-a/dir-b' -openBISurl='https://...' -entityPermId='123-abc-...' -personalAccessToken='098abc...' -remDir='/remote/dir/absolute-path/' -enabled=true|false  ( optional: -ignoreFiles=GlobalDefault|SpecificList|None with default-value: GlobalDefault )
     * ./drive-app jobs remove -dir='./dir-a/dir-b'
     * ./drive-app jobs start -dir='./dir-a/dir-b'
     * ./drive-app jobs stop -dir='./dir-a/dir-b'
     * ./drive-app jobs ignored-path-patterns -dir='./dir-a/dir-b' (shows the ignored path-patterns for the job related to this local directory)
     * ./drive-app jobs ignored-path-patterns -dir='./dir-a/dir-b' -set -> sets new ignored path-patterns from console-input (glob syntax)
     * ./drive-app jobs ignored-path-patterns -dir='./dir-a/dir-b' -setFromFile=./documents/new-patterns.txt -> sets new ignored path-patterns
     *     from UTF-8 multiline text-file with a glob expression on each line
     * <p>
     * Prints notifications on the standard output, one per line, fields separated by tabs
     * ./drive-app notifications -limit=100 (default: 100)
     * <p>
     * Prints events on the standard output, one per line, fields separated by tabs
     * ./drive-app events -limit=100 (default: 100)
     */
    public static void main(String[] args) throws Exception {
        DriveAPICmdLineApp driveAPICmdLineApp = new DriveAPICmdLineApp();

        if(args.length >= 1) {
            switch (args[0]) {
                case "help" -> driveAPICmdLineApp.printHelp();
                case "start" -> driveAPICmdLineApp.handleStartCommand(args);
                case "stop" -> driveAPICmdLineApp.handleStopCommand(args);
                case "status" -> driveAPICmdLineApp.handleStatusCommand(args);
                case "config" -> driveAPICmdLineApp.handleConfigCommand(args);
                case "jobs" -> driveAPICmdLineApp.handleJobsCommand(args);
                case "notifications" -> driveAPICmdLineApp.handleNotificationsCommand(args);
                case "events" -> driveAPICmdLineApp.handleEventsCommand(args);
                default -> driveAPICmdLineApp.printHelp();
            }
        } else {
            driveAPICmdLineApp.printHelp();
        }
    }

    DriveAPIClientProtobufImpl getNewDriveAPIClient() throws Exception {
        return new DriveAPIClientProtobufImpl(new Configuration());
    }

    void handleEventsCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder().option("limit").longOpt("limit").required(false).hasArg(true).desc("Limit of printed items (default: 100)").get());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }
        int limit = 100;
        if (commandLine.hasOption("limit")) {
            try {
                limit = Integer.parseInt(commandLine.getOptionValue("limit"));
            } catch (Exception e) {
                System.out.println("Wrong '-limit=' option\n");
                printHelp();
                return;
            }
        }
        printEvents(limit);
    }

    void handleNotificationsCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder().option("limit").longOpt("limit").required(false).hasArg(true).desc("Limit of printed items (default: 100)").get());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }
        int limit = 100;
        if (commandLine.hasOption("limit")) {
            try {
                limit = Integer.parseInt(commandLine.getOptionValue("limit"));
            } catch (Exception e) {
                System.out.println("Wrong '-limit=' option\n");
                printHelp();
                return;
            }
        }
        printNotifications(limit);
    }

    void handleJobsCommand(String[] args) throws Exception {
        if (args.length == 1) {
            printJobs();
        } else {
            switch (args[1]) {
                case "add" -> handleAddJobCommand(args);
                case "remove" -> handleRemoveJobCommand(args);
                case "start" -> handleStartJobCommand(args);
                case "stop" -> handleStopJobCommand(args);
                case "ignored-path-patterns" -> handleIgnoredPathPatternsJobCommand(args);
                default -> {
                    System.out.printf("Unknown 'jobs' subcommand %s\n", args[1]);
                    printHelp();
                }
            }
        }
    }

    void handleAddJobCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder()
                .option("type").longOpt("type").required(true).hasArg(true).desc("Sync-job type (Upload|Download|Bidirectional): required").get())
            .addOption(Option.builder()
                .option("dir").longOpt("dir").required(true).hasArg(true).desc("Local synchronized directory: required").get())
            .addOption(Option.builder()
                .option("openBISurl").longOpt("openBISurl").required(true).hasArg(true).desc("openBIS URL: required").get())
            .addOption(Option.builder()
                .option("entityPermId").longOpt("entityPermId").required(true).hasArg(true).desc("openBIS entityPermId: required").get())
            .addOption(Option.builder()
                .option("title").longOpt("title").required(true).hasArg(true).desc("Sync-job descriptive title: required").get())
            .addOption(Option.builder()
                .option("personalAccessToken").longOpt("personalAccessToken").required(true).hasArg(true).desc("openBIS personal access token: required").get())
            .addOption(Option.builder()
                .option("remDir").longOpt("remDir").required(true).hasArg(true).desc("openBIS remote directory: required").get())
            .addOption(Option.builder()
                .option("enabled").longOpt("enabled").required(true).hasArg(true).desc("Enable sync-job or not: required").get())
            .addOption(Option.builder()
                .option("ignoreFiles").longOpt("ignoreFiles").required(false).hasArg(true).desc("Mode of ignoring files: optional (default value: GlobalDefault)").get());

            CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }
        SyncJob.Type syncJobType;
        try {
            syncJobType = SyncJob.Type.valueOf(commandLine.getOptionValue("type"));
        } catch (Exception e) {
            System.out.println("Wrong '-type=' option\n");
            printHelp();
            return;
        }
        String localDirectory;
        try {
            localDirectory = commandLine.getOptionValue("dir");
        } catch (Exception e) {
            System.out.println("Wrong '-dir=' option\n");
            printHelp();
            return;
        }
        String openBISurl;
        try {
            openBISurl = commandLine.getOptionValue("openBISurl");

            if (!validateOpenBISUrl(openBISurl)) {
                System.out.println("Bad '-openBISurl=' value: it must be an http:// or https:// URL without path\n");
                return;
            }

        } catch (Exception e) {
            System.out.println("Wrong '-openBISurl=' option\n");
            printHelp();
            return;
        }
        String entityPermId;
        try {
            entityPermId = commandLine.getOptionValue("entityPermId");
        } catch (Exception e) {
            System.out.println("Wrong '-entityPermId=' option\n");
            printHelp();
            return;
        }
        String title;
        try {
            title = commandLine.getOptionValue("title");
        } catch (Exception e) {
            System.out.println("Wrong '-title=' option\n");
            printHelp();
            return;
        }
        String personalAccessToken;
        try {
            personalAccessToken = commandLine.getOptionValue("personalAccessToken");
        } catch (Exception e) {
            System.out.println("Wrong '-personalAccessToken=' option\n");
            printHelp();
            return;
        }
        String remoteDirectory;
        try {
            remoteDirectory = commandLine.getOptionValue("remDir");
            if ( !Path.of(remoteDirectory).startsWith(File.separator) ) {
                throw new IllegalArgumentException("'-remDir=' option must be absolute path");
            }
        } catch (Exception e) {
            System.out.println("Wrong '-remDir=' option (must be absolute path)\n");
            printHelp();
            return;
        }
        boolean enabled;
        try {
            enabled = Boolean.parseBoolean(commandLine.getOptionValue("enabled"));
        } catch (Exception e) {
            System.out.println("Wrong '-enabled=' option\n");
            printHelp();
            return;
        }
        SyncJob.IgnoredFilesMode ignoreFiles = SyncJob.IgnoredFilesMode.GlobalDefault;
        if (commandLine.hasOption("ignoreFiles")) {
            try {
                ignoreFiles = SyncJob.IgnoredFilesMode.valueOf(commandLine.getOptionValue("ignoreFiles"));
            } catch (Exception e) {
                System.out.println("Wrong '-ignoreFiles=' option\n");
                printHelp();
                return;
            }
        }

        addJob(title, syncJobType, localDirectory, openBISurl, entityPermId, personalAccessToken, toServerPathString(Path.of(remoteDirectory)), enabled, ignoreFiles);
    }

    boolean validateOpenBISUrl(@NotNull String url) {
        URI uri = null;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            return false;
        }
        if (uri != null &&
                List.of("http", "https").contains(uri.getScheme()) &&
                Optional.ofNullable(uri.getPath()).orElse("").isBlank()) {
            return true;
        } else {
            return false;
        }
    }

    void handleRemoveJobCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder()
                        .option("dir").longOpt("dir").required(true).hasArg(true).desc("Local synchronized directory: required").get());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }
        String localDirectory;
        try {
            localDirectory = commandLine.getOptionValue("dir");
        } catch (Exception e) {
            System.out.println("Wrong '-dir=' option\n");
            printHelp();
            return;
        }
        removeJob(localDirectory);
    }

    void handleStartJobCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder()
                .option("dir").longOpt("dir").required(true).hasArg(true).desc("Local synchronized directory: required").get());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }
        String localDirectory;
        try {
            localDirectory = commandLine.getOptionValue("dir");
        } catch (Exception e) {
            System.out.println("Wrong '-dir=' option\n");
            printHelp();
            return;
        }
        startJob(localDirectory);
    }

    void handleStopJobCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder()
                .option("dir").longOpt("dir").required(true).hasArg(true).desc("Local synchronized directory: required").get());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }
        String localDirectory;
        try {
            localDirectory = commandLine.getOptionValue("dir");
        } catch (Exception e) {
            System.out.println("Wrong '-dir=' option\n");
            printHelp();
            return;
        }
        stopJob(localDirectory);
    }

    void handleIgnoredPathPatternsJobCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder()
                .option("dir").longOpt("dir").required(true).hasArg(true).desc("Local synchronized directory: required").get());
        options.addOption(Option.builder()
                .option("set").longOpt("set").required(false).hasArg(false).desc("Set new ignored path-patterns from console-input: optional").get());
        options.addOption(Option.builder()
                .option("setFromFile").longOpt("setFromFile").required(false).hasArg(true).desc("Set new ignored path-patterns from file: optional").get());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }

        String localDirectory;
        try {
            localDirectory = commandLine.getOptionValue("dir");
        } catch (Exception e) {
            System.out.println("Wrong '-dir=' option\n");
            printHelp();
            return;
        }

        String setNewMultiLineValue = null;
        if ( commandLine.hasOption("set") ) {
            try {
                System.out.println("Please, enter new glob expressions one per line. End with empty line:");
                Scanner scanner = new Scanner(System.in);
                StringBuilder multilineValueBuilder = new StringBuilder();

                while (true) {
                    String input = scanner.nextLine();

                    if (input.isBlank()) {
                        setNewMultiLineValue = multilineValueBuilder.toString();
                        break;
                    } else {
                        multilineValueBuilder.append(input.trim()).append(System.lineSeparator());
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading new ignored path-patterns\n");
                printHelp();
                return;
            }
        } else if ( commandLine.hasOption("setFromFile") ) {
            try {
                setNewMultiLineValue = new String(
                        Files.readAllBytes(Path.of(commandLine.getOptionValue("setFromFile"))),
                        StandardCharsets.UTF_8
                );
            } catch (Exception e) {
                System.out.println("Error reading new ignored path-patterns from file\n");
                printHelp();
                return;
            }
        }

        if ( setNewMultiLineValue != null ) {
            setIgnoredPathPatterns(localDirectory, setNewMultiLineValue);
        } else {
            showSpecificIgnoredPathPattern(localDirectory);
        }
    }

    void handleConfigCommand(String[] args) throws Exception {
        if (args.length == 1) {
            printConfig();
        } else {
            switch (args[1]) {
                case "ignored-path-patterns" -> handleIgnoredPathPatternsConfigCommand(args);
                default -> {
                    Options options = new Options();
                    options.addOption(Option.builder()
                                    .option("startAtLogin").longOpt("startAtLogin").required(false).hasArg(true).desc("Start application at system-login (defaults to: false)").get())
                            .addOption(Option.builder()
                                    .option("language").longOpt("language").required(false).hasArg(true).desc("Language (defaults to: en)").get())
                            .addOption(Option.builder()
                                    .option("syncInterval").longOpt("syncInterval").required(false).hasArg(true).desc("Synchronization interval in seconds (defaults to 120 seconds = 2 minutes)").get());
                    CommandLineParser commandLineParser = new DefaultParser();
                    CommandLine commandLine;
                    try {
                        commandLine = commandLineParser.parse(options, args);
                    } catch (Exception e) {
                        System.out.println("Wrong options\n");
                        printHelp();
                        return;
                    }
                    Boolean startAtLogin = null;
                    if (commandLine.hasOption("startAtLogin")) {
                        try {
                            startAtLogin = Boolean.parseBoolean(commandLine.getOptionValue("startAtLogin"));
                        } catch (Exception e) {
                            System.out.println("Wrong '-startAtLogin=' option\n");
                            printHelp();
                            return;
                        }
                    }
                    String language = null;
                    if (commandLine.hasOption("language")) {
                        try {
                            language = commandLine.getOptionValue("language");
                        } catch (Exception e) {
                            System.out.println("Wrong '-language=' option\n");
                            printHelp();
                            return;
                        }
                    }
                    Integer syncInterval = null;
                    if (commandLine.hasOption("syncInterval")) {
                        try {
                            syncInterval = Integer.parseInt(commandLine.getOptionValue("syncInterval"));
                        } catch (Exception e) {
                            System.out.println("Wrong '-syncInterval=' option\n");
                            printHelp();
                            return;
                        }
                    }
                    setConfig(startAtLogin, language, syncInterval);
                }
            }
        }
    }

    void handleIgnoredPathPatternsConfigCommand(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder()
                .option("showFactoryDefault").longOpt("showFactoryDefault").required(false).hasArg(false).desc("Show factory-default ignored path-patterns: optional").get());
        options.addOption(Option.builder()
                .option("reset").longOpt("reset").required(false).hasArg(false).desc("Reset global default ignored path-patterns to factory-default values: optional").get());
        options.addOption(Option.builder()
                .option("set").longOpt("set").required(false).hasArg(false).desc("Set new global default ignored path-patterns from console-input: optional").get());
        options.addOption(Option.builder()
                .option("setFromFile").longOpt("setFromFile").required(false).hasArg(true).desc("Set new global default ignored path-patterns from file: optional").get());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (Exception e) {
            System.out.println("Wrong options\n");
            printHelp();
            return;
        }

        boolean showFactoryDefault = commandLine.hasOption("showFactoryDefault");
        boolean reset = commandLine.hasOption("reset");

        String setNewMultiLineValue = null;
        if ( commandLine.hasOption("set") ) {
            try {
                System.out.println("Please, enter new glob expressions one per line. End with empty line:");
                Scanner scanner = new Scanner(System.in);
                StringBuilder multilineValueBuilder = new StringBuilder();

                while (true) {
                    String input = scanner.nextLine();

                    if (input.isBlank()) {
                        setNewMultiLineValue = multilineValueBuilder.toString();
                        break;
                    } else {
                        multilineValueBuilder.append(input.trim()).append(System.lineSeparator());
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading new ignored path-patterns\n");
                printHelp();
                return;
            }
        } else if ( commandLine.hasOption("setFromFile") ) {{
            try {
                setNewMultiLineValue = new String(
                        Files.readAllBytes(Path.of(commandLine.getOptionValue("setFromFile"))),
                        StandardCharsets.UTF_8
                );
            } catch (Exception e) {
                System.out.println("Error reading new ignored path-patterns from file\n");
                printHelp();
                return;
            }
        }
        }

        if (showFactoryDefault) {
            showFactoryDefaultIgnoredPathPatterns();
        } else if ( setNewMultiLineValue != null ) {
            setGlobalIgnoredPathPatterns(setNewMultiLineValue);
        } else if (reset) {
            resetGlobalIgnoredPathPatterns();
        } else {
            showGlobalIgnoredPathPatterns();
        }
    }

    void printHelp() {
        System.out.println("""
            Use 'help' command to print this message.
            Supported commands:
                start   -> starts the background service
                stop    -> stops the background service
                status  -> prints the status of the background service
                
                config  -> prints the configuration with which the background service is running
                config -startAtLogin=true|false -language=en|fr|de|it|es -syncInterval=120   -> sets configuration parameters: two-letter ISO-code for language, synchronization-interval in seconds (defaults: false, 'en', 120 seconds = 2 minutes)
                config ignored-path-patterns -> shows the active global default list of ignored file patterns (glob syntax)
                config ignored-path-patterns -showFactoryDefault -> shows the showFactoryDefault (not necessarily active) global default list of ignored file patterns (glob syntax)
                config ignored-path-patterns -reset -> resets the active global default list of ignored file patterns to factory-default values
                config ignored-path-patterns -set -> sets new ignored path-patterns from console-input
                config ignored-path-patterns -setFromFile -> sets new ignored path-patterns
                    from UTF-8 multiline text-file with a glob expression on each line
                
                jobs    -> prints the currently registered synchronization-jobs
                jobs add -title='Description...' -type='Bidirectional|Upload|Download' -dir='./dir-a/dir-b' -openBISurl='https://...' -entityPermId='123-abc-...' -personalAccessToken='098abc...' -remDir='/remote/dir/absolute-path/' -enabled=true|false  ( optional: -ignoreFiles=GlobalDefault|SpecificList|None with default-value: GlobalDefault )
                jobs remove -dir='./dir-a/dir-b'
                jobs start -dir='./dir-a/dir-b'
                jobs stop -dir='./dir-a/dir-b'
                
                jobs ignored-path-patterns -dir='./dir-a/dir-b' (shows the ignored path-patterns for the job related to this local directory)
                jobs ignored-path-patterns -dir='./dir-a/dir-b' -set -> sets new ignored path-patterns from console-input (glob syntax)
                jobs ignored-path-patterns -dir='./dir-a/dir-b' -setFromFile=./documents/new-patterns.txt -> sets new ignored path-patterns
                    from UTF-8 multiline text-file with a glob expression on each line

                notifications -limit=100   (default: 100)  -> prints the last limit-number of notifications
                events -limit=100   (default: 100)  -> prints the last limit-number of events""");
    }

    void printConfig() throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            Settings settings = driveAPIClient.getSettings();
            System.out.println("Start-at-login: " + settings.isStartAtLogin());
            System.out.println("Language: " + settings.getLanguage());
            System.out.println(String.format("Sync-interval: %s seconds", settings.getSyncInterval()));
            System.out.println("Synchronization-jobs: ");
            for (SyncJob syncJob : settings.getJobs()) {
                System.out.println("----------");
                printSyncJob(syncJob);
            }
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void setConfig(Boolean startAtLogin, String language, Integer syncInterval) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            Settings settings = driveAPIClient.getSettings();
            if(startAtLogin != null) {
                settings.setStartAtLogin(startAtLogin);
            }
            if(language != null) {
                settings.setLanguage(language);
            }
            if(syncInterval != null) {
                settings.setSyncInterval(syncInterval);
            }
            driveAPIClient.setSettings(settings);
            printConfig();
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void addJob(@NonNull String title, @NonNull SyncJob.Type type, @NonNull String localDirectory, @NonNull String openBISurl, @NonNull String entityPermId, @NonNull String personalAccessToken, @NonNull String remoteDirectory, boolean enabled, @NonNull SyncJob.IgnoredFilesMode ignoredFilesMode) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            SyncJob newSyncJob = new SyncJob();
            newSyncJob.setTitle(title);
            newSyncJob.setType(type);
            newSyncJob.setLocalDirectoryRoot(localDirectory);
            newSyncJob.setOpenBisUrl(openBISurl);
            newSyncJob.setEntityPermId(entityPermId);
            newSyncJob.setOpenBisPersonalAccessToken(personalAccessToken);
            newSyncJob.setRemoteDirectoryRoot(remoteDirectory);
            newSyncJob.setEnabled(enabled);
            newSyncJob.setIgnoreFiles(ignoredFilesMode);
            driveAPIClient.addSyncJobs(Collections.singletonList(newSyncJob));
            printJobs();
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void removeJob(@NonNull String localDirectory) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            String localDirectoryAbsolutePath = Path.of(localDirectory).toAbsolutePath().toString();

            List<SyncJob> currentJobs = driveAPIClient.getSyncJobs();

            for (SyncJob syncJob : currentJobs) {
                if (Path.of(syncJob.getLocalDirectoryRoot()).toAbsolutePath().toString().equals(localDirectoryAbsolutePath)) {
                    driveAPIClient.removeSyncJobs(Collections.singletonList(syncJob));
                }
            }

            printJobs();
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void startJob(@NonNull String localDirectory) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            String localDirectoryAbsolutePath = Path.of(localDirectory).toAbsolutePath().toString();

            List<SyncJob> currentJobs = driveAPIClient.getSyncJobs();

            for (SyncJob syncJob : currentJobs) {
                if (Path.of(syncJob.getLocalDirectoryRoot()).toAbsolutePath().toString().equals(localDirectoryAbsolutePath)) {
                    driveAPIClient.startSyncJobs(Collections.singletonList(syncJob));
                }
            }

            printJobs();
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void stopJob(@NonNull String localDirectory) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            String localDirectoryAbsolutePath = Path.of(localDirectory).toAbsolutePath().toString();

            List<SyncJob> currentJobs = driveAPIClient.getSyncJobs();

            for (SyncJob syncJob : currentJobs) {
                if (Path.of(syncJob.getLocalDirectoryRoot()).toAbsolutePath().toString().equals(localDirectoryAbsolutePath)) {
                    driveAPIClient.stopSyncJobs(Collections.singletonList(syncJob));
                }
            }

            printJobs();
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void showFactoryDefaultIgnoredPathPatterns() throws Exception {
        System.out.println("Factory-default ignored path-patterns:");
        System.out.println("----------");
        SyncJob.getDefaultIgnoredPathPatterns().forEach(System.out::println);
        System.out.println("----------");
    }

    void showGlobalIgnoredPathPatterns() throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {

            System.out.println("Global default ignored path-patterns:");
            System.out.println("----------");
            driveAPIClient.getSettings().getIgnoredPathPatterns().forEach(System.out::println);
            System.out.println("----------");

        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }

    }

    void showSpecificIgnoredPathPattern(@NonNull String localDirectory) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            String localDirectoryAbsolutePath = Path.of(localDirectory).toAbsolutePath().toString();

            Optional<SyncJob> syncJob = driveAPIClient.getSyncJobs().stream().filter(
                    item -> Path.of(item.getLocalDirectoryRoot()).toAbsolutePath().toString().equals(localDirectoryAbsolutePath)
            ).findFirst();

            if ( syncJob.isPresent() ) {
                printSyncJob(syncJob.get());
                System.out.println("Synchronization-job-specific ignored path-patterns:");
                System.out.println("----------");
                syncJob.get().getIgnoredPathPatterns().forEach(System.out::println);
                System.out.println("----------");
            } else {
                System.out.println(String.format("Synchronization-job not found for local directory: %s", localDirectory));
            }

        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void setIgnoredPathPatterns(@NonNull String localDirectory, @NonNull String multiLineIgnoredPathPatterns) throws Exception {
        if ( validateMultiLineIgnoredPathPatterns(multiLineIgnoredPathPatterns) ) {
            try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
                String localDirectoryAbsolutePath = Path.of(localDirectory).toAbsolutePath().toString();

                Optional<SyncJob> syncJob = driveAPIClient.getSyncJobs().stream().filter(
                        item -> Path.of(item.getLocalDirectoryRoot()).toAbsolutePath().toString().equals(localDirectoryAbsolutePath)
                ).findFirst();

                if ( syncJob.isPresent() ) {

                    driveAPIClient.removeSyncJobs(Collections.singletonList(syncJob.get()));
                    syncJob.get().setIgnoredPathPatterns(
                            new ArrayList<>(Arrays.stream(multiLineIgnoredPathPatterns.split("[\\r\\n]+"))
                            .filter(str -> !str.isBlank())
                            .map(String::trim)
                            .toList()));
                    driveAPIClient.addSyncJobs(Collections.singletonList(syncJob.get()));

                    showSpecificIgnoredPathPattern(localDirectory);
                } else {
                    System.out.println(String.format("Synchronization-job not found for local directory: %s", localDirectory));
                }

            } catch (Exception e) {
                if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                    System.out.println("OpenBIS Drive Service is not running.");
                } else {
                    System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
                }
            }
        } else {
            System.out.println("Bad ignored path-patterns");
        }
    }

    void setGlobalIgnoredPathPatterns(@NonNull String multiLineIgnoredPathPatterns) throws Exception {
        if ( validateMultiLineIgnoredPathPatterns(multiLineIgnoredPathPatterns) ) {
            try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {

                Settings settings = driveAPIClient.getSettings();
                settings.setIgnoredPathPatterns(
                        new ArrayList<>(Arrays.stream(multiLineIgnoredPathPatterns.split("[\\r\\n]+"))
                        .filter(str -> !str.isBlank())
                        .map(String::trim)
                        .toList()));
                driveAPIClient.setSettings(settings);

                showGlobalIgnoredPathPatterns();

            } catch (Exception e) {
                if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                    System.out.println("OpenBIS Drive Service is not running.");
                } else {
                    System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
                }
            }
        } else {
            System.out.println("Bad ignored path-patterns");
        }
    }

    void resetGlobalIgnoredPathPatterns() throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {

            Settings settings = driveAPIClient.getSettings();
            settings.setIgnoredPathPatterns(new ArrayList<>(SyncJob.getDefaultIgnoredPathPatterns()));
            driveAPIClient.setSettings(settings);

            showGlobalIgnoredPathPatterns();

        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    boolean validateMultiLineIgnoredPathPatterns(@NonNull String multiLineIgnoredPathPatterns) {
        if(!multiLineIgnoredPathPatterns.isBlank()) {
            for(String ignoredPathPattern : multiLineIgnoredPathPatterns.split("[\\r\\n]+")) {
                if (!ignoredPathPattern.isBlank()) {
                    try {
                        String trimmed = ignoredPathPattern.trim();
                        GlobUtil.compileIgnoredPathGlob(trimmed);
                    } catch (Exception e) {
                        System.out.println(String.format("Wrong glob expression: %s", ignoredPathPattern));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    void printJobs() throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            Settings settings = driveAPIClient.getSettings();
            System.out.println("Synchronization-jobs: ");
            for (SyncJob syncJob : settings.getJobs()) {
                System.out.println("----------");
                printSyncJob(syncJob);
            }
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void printNotifications(int limit) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            List<Notification> notifications = driveAPIClient.getNotifications(limit);
            System.out.println("Notifications: ");
            for (Notification notification : notifications) {
                System.out.println("----------");
                printNotification(notification);
            }
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void printNotification(@NonNull Notification notification) {
        System.out.println("Type: " + notification.getType());
        System.out.println("Local directory: " + notification.getLocalDirectory());
        if(notification.getLocalFile() != null) {
            System.out.println("Local file: " + notification.getLocalFile());
        }
        if(notification.getRemoteFile() != null) {
            System.out.println("Remote file: " + notification.getRemoteFile());
        }
        System.out.println("Message: " + notification.getMessage());
        System.out.println("Timestamp: " + Instant.ofEpochMilli(notification.getTimestamp()));
    }

    void printEvents(int limit) throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            List<? extends Event> events = driveAPIClient.getEvents(limit);
            System.out.println("Events: ");
            for (Event event : events) {
                System.out.println("----------");
                printEvent(event);
            }
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    void printEvent(@NonNull Event event) {
        System.out.println("Synchronization-direction: " + event.getSyncDirection());
        System.out.println("Local directory: " + event.getLocalDirectoryRoot());
        System.out.println("Local file: " + event.getLocalFile());
        System.out.println("Remote file: " + event.getRemoteFile());
        System.out.println("Directory: " + event.isDirectory());
        System.out.println("Source file deleted: " + event.isSourceDeleted());
        System.out.println("Timestamp: " + Instant.ofEpochMilli(event.getTimestamp()));
    }

    void printSyncJob(@NonNull SyncJob syncJob) {
        System.out.println("Title: " + syncJob.getTitle());
        System.out.println("Type: " + syncJob.getType());
        System.out.println("Local directory: " + syncJob.getLocalDirectoryRoot());
        System.out.println("openBIS url: " + syncJob.getOpenBisUrl());
        System.out.println("Entity-perm-id: " + syncJob.getEntityPermId());
        System.out.println("Remote directory: " + syncJob.getRemoteDirectoryRoot());
        System.out.println("Personal access token: " + syncJob.getOpenBisPersonalAccessToken());
        System.out.println("Enabled: " + syncJob.isEnabled());
        System.out.println("Ignored files: " + syncJob.getIgnoreFiles());
    }

    void handleStartCommand(String[] args) throws Exception {
        if (args.length == 1) {
            startService();
        } else {
            System.out.println("Unexpected arguments for start command");
            printHelp();
        }
    }

    void handleStopCommand(String[] args) throws Exception {
        if (args.length == 1) {
            stopService();
        } else {
            System.out.println("Unexpected arguments for stop command");
            printHelp();
        }
    }

    void handleStatusCommand(String[] args) throws Exception {
        if (args.length == 1) {
            checkServiceStatus();
        } else {
            System.out.println("Unexpected arguments for status command");
            printHelp();
        }
    }

    void startService() throws Exception {
        OpenBISDriveUtil.startServiceBackgroundProcess();
    }

    void stopService() throws Exception {
        OpenBISDriveUtil.stopServiceBackgroundProcess();
    }

    void checkServiceStatus() throws Exception {
        try ( DriveAPIClientProtobufImpl driveAPIClient = getNewDriveAPIClient() ) {
            driveAPIClient.getSettings();
            System.out.println("OpenBIS Drive Service is running with this configuration:");
            printConfig();
        } catch (Exception e) {
            if (e instanceof StatusRuntimeException && Status.UNAVAILABLE.getCode() == ((StatusRuntimeException) e).getStatus().getCode()) {
                System.out.println("OpenBIS Drive Service is not running.");
            } else {
                System.out.println(String.format("Error: %s, %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }
}
