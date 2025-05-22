package ch.systemsx.cisd.common.logging;

import ch.systemsx.cisd.common.logging.ext.DailyRollingFileHandler;
import ch.systemsx.cisd.common.logging.ext.LoggingUtils;
import ch.systemsx.cisd.common.logging.ext.NullHandler;
import ch.systemsx.cisd.common.logging.ext.PatternFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Initializes the logging system. The {@link #init()} method needs to be called once at system startup.
 * Detailed sysout tracing added to surface file paths, URLs, and property loading steps.
 *
 * @author Bernd Rinn (modified)
 */
public class LogInitializer
{

    private static final String LOG_DIRECTORY = "etc";
    private static final String LOG_FILENAME = "logging.properties";
    private static final String FILE_URL_PREFIX = "file:";
    private static final String GLOBAL_LEVEL_KEY = ".global.level";
    public static final String GLOBAL_HANDLERS = ".global.handlerAliases";
    public static final String INTERNAL_HANDLERS = ".handlerAliases";
    private static final String LEVEL_SUFFIX = ".level";



    static
    {
        System.setProperty("log4j.defaultInitOverride", "true");
        LogLog.info("[LogInitializer] Static init: log4j.defaultInitOverride=true");
        // Make Hibernate’s JBoss-Logging use JUL
        System.setProperty("org.jboss.logging.provider", "jdk");

    }

    private static boolean initialized = false;

    private static URL createURL(final String configurationOrNull)
    {
        LogLog.info("[LogInitializer] createURL(config=" + configurationOrNull + ")");
        if (configurationOrNull != null)
        {
            try
            {
                LogLog.info("[LogInitializer] Trying URL string: " + configurationOrNull);
                return new URL(configurationOrNull);
            } catch (final MalformedURLException ex)
            {
                System.err.println("[LogInitializer] Malformed URL '" + configurationOrNull + "': " + ex.getMessage());
            }
        }
        URL resource = LogInitializer.class.getResource("/" + LOG_DIRECTORY + "/" + LOG_FILENAME);
        LogLog.info("[LogInitializer] Classpath resource URL: " + resource);
        return resource;
    }

    private static String tryFindConfigurationInSystemProperties()
    {
        String config = System.getProperty("log4j.configuration");
        LogLog.info("[LogInitializer] System property log4j.configuration=" + config);
        if (config != null)
        {
            String trimmed = config.trim();
            if (!trimmed.isEmpty())
            {
                LogLog.info("[LogInitializer] Using system property config: " + trimmed);
                return trimmed;
            }
        }
        return null;
    }

    private static File getEffectiveLogFile(final String configurationOrNull)
    {
        File file;
        if (configurationOrNull == null)
        {
            file = new File(LOG_DIRECTORY, LOG_FILENAME);
        } else
        {
            file = new File(configurationOrNull);
        }
        LogLog.info("[LogInitializer] getEffectiveLogFile -> " + file.getAbsolutePath());
        return file;
    }

    public static synchronized void init()
    {
        LogLog.info("[LogInitializer] init() called. alreadyInitialized=" + initialized);
        if (initialized)
        {
            LogLog.info("[LogInitializer] Already initialized. Skipping.");
            return;
        }
        initialize();
    }

    private static void initialize()
    {
        LogLog.info("[LogInitializer] Starting initialize()");
        String config = tryFindConfigurationInSystemProperties();
        boolean fileTried = false;

        if (config == null || !config.startsWith(FILE_URL_PREFIX))
        {
            File logFile = getEffectiveLogFile(config);
            fileTried = true;
            if (logFile.exists() && logFile.isFile())
            {
                LogLog.info("[LogInitializer] Found log file: " + logFile.getAbsolutePath());
                configureFromFile(logFile);
                initialized = true;
                LogLog.info("[LogInitializer] Initialized from file.");
                return;
            }
            LogLog.info("[LogInitializer] Log file not found: " + logFile.getAbsolutePath());
        }

        LogLog.info("[LogInitializer] Trying URL-based config (config=" + config + ")");
        URL url = createURL(config);
        if (url != null)
        {
            configureFromURL(url);
            initialized = true;
            LogLog.info("[LogInitializer] Initialized from URL.");
            return;
        }

        initialized = true;
        LogLog.info("[LogInitializer] No external config found. Initialization complete with defaults.");
    }

    public static void configureFromURL(final URL url)
    {
        LogLog.info("[LogInitializer] configureFromURL(" + url + ")");
        try
        {
            File file = new File(url.toURI());
            LogLog.info("[LogInitializer] URL to URI path: " + file.getAbsolutePath());
            if (file.exists())
            {
                LogLog.info("[LogInitializer] File exists at URL path. Delegating to configureFromFile.");
                configureFromFile(file);
                return;
            }
        } catch (URISyntaxException e)
        {
            System.err.println("[LogInitializer] URI syntax error: " + e.getMessage());
        }
        System.out.printf("[LogInitializer] Configured from URL '%s' (no watching)%n", url);
    }

    public static void configureFromFile(File logFile)
    {
        if (!logFile.getName().endsWith(".properties")) {
            String msg = String.format(
                    "[LogInitializer] Unsupported configuration file: '%s'.%n" +
                            "Please supply a '.properties' file for logging setup.",
                    logFile.getAbsolutePath()
            );
            LogLog.info(msg);
            new IllegalArgumentException("Invalid file type").printStackTrace(System.out);

            // Bail out
            return;
        }

        LogLog.info("[LogInitializer] configureFromFile(logFile=" + logFile.getAbsolutePath() + ")");
//        LogManager logMgr = LogManager.getLogManager();
//        logMgr.reset();

//        Logger root = Logger.getLogger("");
//        for (Handler h : root.getHandlers())
//        {
//            LogLog.info("[LogInitializer] Removing handler: " + h);
//            root.removeHandler(h);
//        }
//
//        try (FileInputStream fis = new FileInputStream(logFile))
//        {
//            LogLog.info("[LogInitializer] Loading properties from file: " + logFile.getAbsolutePath());
//            logMgr.readConfiguration(fis);
//            LogLog.info("[LogInitializer] readConfiguration successful.");
//        } catch (IOException e)
//        {
//            System.err.println("[LogInitializer] Error reading config file: " + e.getMessage());
//            e.printStackTrace();
//        }

        configureFromFile(logFile, null);
    }

    public static void configureFromFile(File logFileParam, List<String> loggersToInitialize)
    {
        LogLog.info("[LogInitializer] configureFromFile(loggers override) called.");
        File logFile = logFileParam;
        if (logFileParam == null || logFileParam.getPath().trim().isEmpty())
        {
            System.err.println("[LogInitializer] No log file defined; using default logging.properties in cwd.");
            logFile = new File("logging.properties");
        }

        Properties props = loadProperties(logFile);
        if (props == null)
        {
            System.err.println("[LogInitializer] Properties load failed; aborting.");
            return;
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Bootstrap snippet: load JUL configuration (handlers, formatters, global &
        // package-specific levels) from your logging.properties file *before*
        //  - and honors any “<package>.level” overrides (e.g. org.hibernate.orm.deprecation=SEVERE).
        // ─────────────────────────────────────────────────────────────────────────────
        try (InputStream in = new FileInputStream(logFile)) {
            LogManager.getLogManager().readConfiguration(in);
            LogLog.info("[LogInitializer][Bootstrap] Loaded JUL config from " + logFile);
        }
        catch (IOException ioe) {
            LogLog.info("[LogInitializer][Bootstrap] Couldn't to load JUL config from "
                    + logFile + ": " + ioe.getMessage());
        }



        String handlersList = props.getProperty(GLOBAL_HANDLERS);
        if (handlersList == null)
        {
            System.err.println("[LogInitializer] No 'handlers' property defined.");
            configureGlobalLoggingLevel(props);
            return;
        }
        LogLog.info("[LogInitializer] Handlers to configure: " + handlersList);
        String[] splitHandlerAliases = handlersList.split("\\s*,\\s*");
        Set<String> handlerAliases = Arrays.stream(splitHandlerAliases)
                .map(String::trim)
                .collect(Collectors.toSet());

        removeAllHandlersBeforeInitialization();
        configureGlobalLoggingLevel(props);
        for (String alias : handlerAliases)
        {
            LogLog.info("[LogInitializer] Initializing handler: " + alias);
            try
            {
                Handler handler = createHandler(alias, props);
                configureCommonProperties(handler, alias, props);
                configureSpecialProperties(handler, alias, props);
                Logger.getLogger("").addHandler(handler);
                LogLog.info("[LogInitializer] Handler added: " + handler);
            } catch (Exception e)
            {
                System.err.println("[LogInitializer] Error initializing handler '" + alias + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        // --- attach per‐logger handlers (AUTH, TRACKING, ACCESS, etc.) ---
        for (String key : props.stringPropertyNames()) {
            // look for keys like "AUTH.handlers", but skip the global "handlers"
            if (key.endsWith(INTERNAL_HANDLERS) && !key.equals(GLOBAL_HANDLERS)) {
                String loggerName = key.substring(0, key.length() - INTERNAL_HANDLERS.length());
                String[] aliases2 = props.getProperty(key).split("\\s*,\\s*");
                Logger logger = null;
                try
                {
                    System.err.println(logFile.getAbsolutePath() + " : " + loggerName);
                    logger = Logger.getLogger(loggerName);
                } catch (Throwable e)
                {
                    System.out.println(logFile.getAbsolutePath() + e);
                    throw new RuntimeException(logFile.getAbsolutePath(), e);
                }
                for (Handler h : logger.getHandlers()) {
                    logger.removeHandler(h);
                }
                for (String alias2 : aliases2) {
                    try {
                        Handler handler = createHandler(alias2, props);
                        configureCommonProperties(handler, alias2, props);
                        configureSpecialProperties(handler, alias2, props);
                        logger.addHandler(handler);
                        LogLog.info(
                                "[LogInitializer] Attached handler '" + alias2 +
                                        "' to logger '" + loggerName + "'");
                    } catch (Exception e) {
                        System.err.println(
                                "[LogInitializer] Failed to attach handler '" + alias2 +
                                        "' to logger '" + loggerName + "': " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        // --- end per‐logger handlers ---

        configureSpecificLoggerLevels(props, handlerAliases);

//        watchLoggerLevel("org.hibernate.orm.deprecation", 1_000L);
    }


    public static Thread watchLoggerLevel(final String loggerName, long pollIntervalMillis)
    {
        Logger logger1 = Logger.getLogger(loggerName);
        final Level[] previousLevel = { logger1.getLevel() };  // use array for mutability in lambda
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Thread watcher = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted())
            {
                Logger logger = Logger.getLogger(loggerName);
                Level current = logger.getLevel();
                if (current != previousLevel[0])
                {
                    String time = LocalDateTime.now().format(fmt);
                    System.out.printf("[%s] Logger '%s' level changed from %s to %s%n",
                            time, loggerName,
                            previousLevel[0], current);
                    previousLevel[0] = current;
                }
                try
                {
                    Thread.sleep(pollIntervalMillis);
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }, "LogLevelWatcher-" + loggerName);

        watcher.setDaemon(true);
        watcher.start();
        return watcher;
    }


    private static void removeAllHandlersBeforeInitialization()
    {
        LogLog.info("[LogInitializer] removeAllHandlersBeforeInitialization()");
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers())
        {
            LogLog.info("[LogInitializer] Removing handler: " + h);
            root.removeHandler(h);
        }

        LogLog.info("[LogInitializer] also reset : LogManager.getLogManager().reset()");
        //LogManager.getLogManager().reset();
    }

    private static Properties loadProperties(File configurationFile)
    {
        LogLog.info("[LogInitializer] loadProperties(file=" + configurationFile.getPath() + ")");
        Properties props = new Properties();
        InputStream is = null;
        boolean loaded = false;

        // 1. Try filesystem
        if (configurationFile.exists() && configurationFile.isFile())
        {
            try
            {
                LogLog.info("[LogInitializer] Loading properties from filesystem: " + configurationFile.getAbsolutePath());
                is = new FileInputStream(configurationFile);
            } catch (IOException e)
            {
                System.err.println("[LogInitializer] IOException opening file: " + e.getMessage());
            }
        } else
        {
            LogLog.info("[LogInitializer] File not found on filesystem; will try classpath lookup.");
        }

        // 2. Fallback to classpath
        if (is == null)
        {
            String resourcePath = configurationFile.getPath().replace(File.separatorChar, '/');
            if (resourcePath.startsWith("/"))
            {
                resourcePath = resourcePath.substring(1);
            }
            LogLog.info("[LogInitializer] Trying classpath resource: " + resourcePath);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null)
            {
                is = cl.getResourceAsStream(resourcePath);
                LogLog.info("[LogInitializer] ContextClassLoader resourceStream=" + (is != null));
            }
            if (is == null)
            {
                is = LogInitializer.class.getResourceAsStream("/" + resourcePath);
                LogLog.info("[LogInitializer] Class.getResourceAsStream(/" + resourcePath + ")=" + (is != null));
                if (is == null)
                {
                    is = LogInitializer.class.getResourceAsStream(resourcePath);
                    LogLog.info("[LogInitializer] Class.getResourceAsStream(" + resourcePath + ")=" + (is != null));
                }
            }
        }

        if (is != null)
        {
            try (InputStream in = is)
            {
                props.load(in);
                loaded = true;
                LogLog.info("[LogInitializer] Properties loaded successfully.");
            } catch (IOException e)
            {
                System.err.println("[LogInitializer] Error loading properties: " + e.getMessage());
            }
        } else
        {
            System.err.println("[LogInitializer] Could not find configuration resource: " + configurationFile.getPath());
        }
        return loaded ? props : null;
    }

    private static void configureGlobalLoggingLevel(Properties props)
    {
        LogLog.info("[LogInitializer] configureGlobalLoggingLevel");
        String global = props.getProperty(GLOBAL_LEVEL_KEY);
        LogLog.info("[LogInitializer] .global.level=" + global);
        if (global != null && !global.isEmpty())
        {
            Level lvl = tryGetLogLevel(global);
            if (lvl != null)
            {
                Logger.getLogger("").setLevel(lvl);
                LogLog.info("[LogInitializer] Root logger level set to " + lvl);
            }
        }
    }

    private static Level tryGetLogLevel(String levelStr)
    {
        LogLog.info("[LogInitializer] tryGetLogLevel(" + levelStr + ")");
        if (levelStr == null || levelStr.isEmpty())
        {
            return null;
        }
        try
        {
            org.apache.log4j.Level log4jLevel = org.apache.log4j.Level.parse(levelStr);
            Level jul = LoggingUtils.mapToJULLevel(log4jLevel);
            LogLog.info("[LogInitializer] Parsed log4j level " + log4jLevel + " -> JUL level " + jul);
            return jul;
        } catch (Exception e)
        {
            try
            {
                Level jul = Level.parse(levelStr);
                LogLog.info("[LogInitializer] Parsed JUL level " + jul);
                return jul;
            } catch (Exception ex)
            {
                System.err.println("[LogInitializer] Failed to parse level: " + levelStr);
                return null;
            }
        }
    }

    private static Handler createHandler(String alias, Properties props) throws Exception
    {
        LogLog.info("[LogInitializer] createHandler(alias=" + alias + ")");
        String className = props.getProperty(alias + ".class");
        LogLog.info("[LogInitializer] Handler class name=" + className);
        if (className == null)
        {
            throw new IllegalArgumentException("No class defined for handler alias: \"" + alias + "\"");
        }
        Class<?> hc = Class.forName(className);
        Handler handler;
        if (DailyRollingFileHandler.class.isAssignableFrom(hc))
        {
            String pattern = props.getProperty(alias + ".logFileNamePattern");
            int limit = Integer.parseInt(props.getProperty(alias + ".maxLogFileSize", "0"));
            boolean append = Boolean.parseBoolean(props.getProperty(alias + ".append", "false"));
            LogLog.info("[LogInitializer] Using file-handler constructor: pattern=" + pattern + ", limit=" + limit + ", append=" + append);
            handler = (Handler) hc.getConstructor(String.class, int.class, boolean.class)
                    .newInstance(pattern, limit, append);

        } else if (ConsoleHandler.class.isAssignableFrom(hc)
                    || NullHandler.class.isAssignableFrom(hc)
                    || Handler.class.isAssignableFrom(hc))
        {
            LogLog.info("[LogInitializer] Using no-arg constructor for handler");
            handler = (Handler) hc.getDeclaredConstructor().newInstance();
        } else{
            throw new IllegalArgumentException("Handler not found : " + hc);
        }

        return handler;
    }

    /* Set common properties (level, formatter, encoding) for a handler */
    private static void configureCommonProperties(Handler handler, String alias, Properties props) throws Exception {
        // Set logging level.
        String levelStr = props.getProperty(alias + ".level");
        if (levelStr != null) {
            handler.setLevel(tryGetLogLevel(levelStr));
        }

        // Set formatter either by class or by message pattern.
        String formatterClassName = props.getProperty(alias + ".formatter");
        if (formatterClassName != null) {
            Class<?> formatterClass = Class.forName(formatterClassName);
            Formatter formatter = (Formatter) formatterClass.getDeclaredConstructor().newInstance();
            handler.setFormatter(formatter);
        } else {
            String messagePattern = props.getProperty(alias + ".messagePattern");
            if (messagePattern != null) {
                PatternFormatter patternFormatter = new PatternFormatter(messagePattern);
                handler.setFormatter(patternFormatter);
            }
        }

        // Set encoding if specified.
        String encoding = props.getProperty(alias + ".encoding");
        if (encoding != null) {
            handler.setEncoding(encoding);
        }
    }

    /* Configure specialized properties for handlers like SocketHandler and MemoryHandler */
    private static void configureSpecialProperties(Handler handler, String alias, Properties props) throws Exception {

        if (handler instanceof java.util.logging.SocketHandler) {
            configureSocketHandler((java.util.logging.SocketHandler) handler, alias, props);
        }
        if (handler instanceof java.util.logging.MemoryHandler) {
            configureMemoryHandler((java.util.logging.MemoryHandler) handler, alias, props);
        }
    }

    /* Configure extra properties for a SocketHandler */
    private static void configureSocketHandler(java.util.logging.SocketHandler handler, String alias, Properties props) {
        String host = props.getProperty(alias + ".host");
        if (host != null) {
            try {
                Method setHost = handler.getClass().getMethod("setHost", String.class);
                setHost.invoke(handler, host);
            } catch (NoSuchMethodException nsme) {
                // Fallback via reflection on private field.
                try {
                    Field hostField = handler.getClass().getDeclaredField("host");
                    hostField.setAccessible(true);
                    hostField.set(handler, host);
                } catch (Exception ex) {
                    System.err.println("Could not set host for SocketHandler for alias: " + alias);
                }
            } catch (Exception e) {
                System.err.println("Error setting host for SocketHandler for alias: " + alias);
            }
        }

        String portStr = props.getProperty(alias + ".port");
        if (portStr != null) {
            int port = Integer.parseInt(portStr);
            try {
                Method setPort = handler.getClass().getMethod("setPort", int.class);
                setPort.invoke(handler, port);
            } catch (NoSuchMethodException nsme) {
                try {
                    Field portField = handler.getClass().getDeclaredField("port");
                    portField.setAccessible(true);
                    portField.set(handler, port);
                } catch (Exception ex) {
                    System.err.println("Could not set port for SocketHandler for alias: " + alias);
                }
            } catch (Exception e) {
                System.err.println("Error setting port for SocketHandler for alias: " + alias);
            }
        }
    }

    /* Configure extra properties for a MemoryHandler */
    private static void configureMemoryHandler(java.util.logging.MemoryHandler handler, String alias, Properties props) {
        // Set the buffer size.
        String sizeStr = props.getProperty(alias + ".size");
        if (sizeStr != null) {
            int size = Integer.parseInt(sizeStr);
            try {
                Field sizeField = handler.getClass().getDeclaredField("size");
                sizeField.setAccessible(true);
                sizeField.set(handler, size);
            } catch (Exception e) {
                System.err.println("Could not set size for MemoryHandler for alias: " + alias);
            }
        }

        // Set the push level.
        String pushLevelStr = props.getProperty(alias + ".push");
        if (pushLevelStr != null) {
            Level pushLevel = tryGetLogLevel(pushLevelStr);
            try {
                Method setPushLevel = handler.getClass().getMethod("setPushLevel", Level.class);
                setPushLevel.invoke(handler, pushLevel);
            } catch (Exception e) {
                System.err.println("Could not set push level for MemoryHandler for alias: " + alias);
            }
        }

        // Set the target handler.
        String targetAlias = props.getProperty(alias + ".target");
        if (targetAlias != null) {
            String targetClassName = props.getProperty(targetAlias + ".class");
            if (targetClassName != null) {
                try {
                    Class<?> targetClass = Class.forName(targetClassName);
                    Handler targetHandler;
                    if (targetClass.equals(java.util.logging.ConsoleHandler.class) ||
                            targetClass.equals(java.util.logging.StreamHandler.class) ||
                            targetClass.equals(java.util.logging.SocketHandler.class) ||
                            targetClass.equals(java.util.logging.MemoryHandler.class)) {
                        targetHandler = (Handler) targetClass.getDeclaredConstructor().newInstance();
                    } else {
                        String pattern = props.getProperty(targetAlias + ".logFileNamePattern");
                        int limit = Integer.parseInt(props.getProperty(targetAlias + ".maxLogFileSize", "0"));
                        int count = Integer.parseInt(props.getProperty(targetAlias + ".maxBackupFiles", "1"));
                        boolean append = Boolean.parseBoolean(props.getProperty(targetAlias + ".append", "false"));
                        targetHandler = (Handler) targetClass.getConstructor(String.class, int.class, int.class, boolean.class)
                                .newInstance(pattern, limit, count, append);
                    }
                    Field targetField = handler.getClass().getDeclaredField("target");
                    targetField.setAccessible(true);
                    targetField.set(handler, targetHandler);
                } catch (Exception e) {
                    System.err.println("Could not set target for MemoryHandler for alias: " + alias);
                }
            }
        }
    }

    /**
     * Configures levels for specific loggers defined in the properties.
     * Example: org.hibernate.level = WARN
     */
    private static void configureSpecificLoggerLevels(Properties props, Set<String> handlerAliases) {
        LogLog.info("Configuring specific logger levels...");
        for (String key : props.stringPropertyNames()) {
            if (key.endsWith(LEVEL_SUFFIX) && !key.equals(GLOBAL_LEVEL_KEY)) {
                // Extract the potential logger name or handler alias part
                String namePart = key.substring(0, key.length() - LEVEL_SUFFIX.length());

                // IMPORTANT: Check if this key belongs to a handler configuration
                if (handlerAliases.contains(namePart)) {
                    // This is a handler's level (e.g., myConsoleHandler.level), already processed. Skip it.
                    // LogLog.info("Skipping handler level key: " + key); // Optional debug log
                    continue;
                }

                // If it's not global and not a handler level, assume it's a specific logger level
                String loggerName = namePart;
                String levelStr = props.getProperty(key);
                Level level = tryGetLogLevel(levelStr);

                if (level != null) {
                    try {
                        Logger logger = Logger.getLogger(loggerName);
                        logger.setLevel(level);
                        LogLog.info("Set level " + level.getName() + " for logger '" + loggerName + "'");
                    } catch (Exception e) {
                        // Catch potential issues getting the logger (though unlikely)
                        System.err.println("ERROR: Could not set level for logger '" + loggerName + "' from property '" + key + "': " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                } else {
                    System.err.println("WARNING: Could not parse level '" + levelStr + "' for logger property '" + key + "'. Level not set.");
                }
            }
        }
        LogLog.info("Finished configuring specific logger levels.");
    }


}
