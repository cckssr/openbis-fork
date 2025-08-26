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
import java.util.Arrays;
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

    public static final String LOG_FILE_NAME = ".logFileName";

    public static final String MAX_LOG_FILE_SIZE = ".maxLogFileSize";

    public static final String MESSAGE_PATTERN = ".messagePattern";

    public static final String ENCODING = ".encoding";

    public static final String MAX_LOG_ROTATIONS = ".maxLogRotations";

    public static final String APPEND = ".append";

    public static final String USE_PARENT_HANDLERS = ".useParentHandlers";

    static
    {
        System.setProperty("log4j.defaultInitOverride", "true");
        LoggerDiagnostics.debug("Static init: log4j.defaultInitOverride=true");
        // Make Hibernate’s JBoss-Logging use JUL
        System.setProperty("org.jboss.logging.provider", "jdk");

    }

    private static boolean initialized = false;

    private static URL createURL(final String configurationOrNull)
    {
        LoggerDiagnostics.info("createURL(config=" + configurationOrNull + ")");
        if (configurationOrNull != null)
        {
            try
            {
                LoggerDiagnostics.info("Trying URL string: " + configurationOrNull);
                return new URL(configurationOrNull);
            } catch (final MalformedURLException ex)
            {
                LoggerDiagnostics.error("Malformed URL '" + configurationOrNull + "': " + ex.getMessage());
            }
        }
        URL resource = LogInitializer.class.getResource("/" + LOG_DIRECTORY + "/" + LOG_FILENAME);
        LoggerDiagnostics.info("Classpath resource URL: " + resource);
        return resource;
    }

    private static String tryFindConfigurationInSystemProperties()
    {
        String config = System.getProperty("log.configuration");
        LoggerDiagnostics.info("System property log.configuration=" + config);
        if (config != null)
        {
            String trimmed = config.trim();
            if (!trimmed.isEmpty())
            {
                LoggerDiagnostics.info("Using system property config: " + trimmed);
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
        LoggerDiagnostics.info("getEffectiveLogFile -> " + file.getAbsolutePath());
        return file;
    }

    public static synchronized void init()
    {
        LoggerDiagnostics.info("init() called. alreadyInitialized=" + initialized);
        if (initialized)
        {
            LoggerDiagnostics.info("Already initialized. Skipping.");
            return;
        }
        initialize();
    }

    private static void initialize()
    {
        LoggerDiagnostics.info("Starting initialize()");
        String config = tryFindConfigurationInSystemProperties();

        if (config == null || !config.startsWith(FILE_URL_PREFIX))
        {
            File logFile = getEffectiveLogFile(config);
            if (logFile.exists() && logFile.isFile())
            {
                LoggerDiagnostics.info("Found log file: " + logFile.getAbsolutePath());
                configureFromFile(logFile);
                initialized = true;
                LoggerDiagnostics.info("Initialized from file.");
                return;
            }
            LoggerDiagnostics.info("Log file not found: " + logFile.getAbsolutePath());
        }

        LoggerDiagnostics.info("Trying URL-based config (config=" + config + ")");
        URL url = createURL(config);
        if (url != null)
        {
            configureFromURL(url);
            initialized = true;
            LoggerDiagnostics.info("Initialized from URL.");
            return;
        }

        initialized = true;
        LoggerDiagnostics.info("No external config found. Initialization complete with defaults.");
    }

    public static void configureFromURL(final URL url)
    {
        LoggerDiagnostics.info("configureFromURL(" + url + ")");
        try
        {
            File file = new File(url.toURI());
            LoggerDiagnostics.info("URL to URI path: " + file.getAbsolutePath());
            if (file.exists())
            {
                LoggerDiagnostics.info("File exists at URL path. Delegating to configureFromFile.");
                configureFromFile(file);
                return;
            }
        } catch (URISyntaxException e)
        {
            LoggerDiagnostics.error("URI syntax error: " + e.getMessage());
        }
        System.out.printf("Configured from URL '%s' (no watching)%n", url);
    }

    public static void configureFromFile(File logFile)
    {
        if (!logFile.getName().endsWith(".properties")) {
            String msg = String.format(
                    "Unsupported configuration file: '%s'.%n" +
                            "Please supply a '.properties' file for logging setup.",
                    logFile.getAbsolutePath()
            );
            LoggerDiagnostics.info(msg);
            new IllegalArgumentException("Invalid file type").printStackTrace(System.out);

            // Bail out
            return;
        }

        LoggerDiagnostics.info("configureFromFile(logFile=" + logFile.getAbsolutePath() + ")");
        configureFromFile(logFile, null);
    }

    public static void configureFromFile(File logFileParam, List<String> loggersToInitialize)
    {
        LoggerDiagnostics.info("configureFromFile(loggers override) called.");
        File logFile = logFileParam;
        if (logFileParam == null || logFileParam.getPath().trim().isEmpty())
        {
            LoggerDiagnostics.error("No log file defined; using default logging.properties in cwd.");
            logFile = new File("logging.properties");
        }

        Properties props = loadProperties(logFile);
        if (props == null)
        {
            LoggerDiagnostics.error("Properties load failed; aborting.");
            return;
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Bootstrap snippet: load JUL configuration (handlers, formatters, global &
        // package-specific levels) from your logging.properties file *before*
        //  - and honors any “<package>.level” overrides (e.g. org.hibernate.orm.deprecation=SEVERE).
        // ─────────────────────────────────────────────────────────────────────────────
        try (InputStream in = new FileInputStream(logFile)) {
            LogManager.getLogManager().readConfiguration(in);
            LoggerDiagnostics.info("[LogInitializer][Bootstrap] Loaded JUL config from " + logFile);
        }
        catch (IOException ioe) {
            LoggerDiagnostics.info("[LogInitializer][Bootstrap] Couldn't to load JUL config from "
                    + logFile + ": " + ioe.getMessage());
        }



        String handlersList = props.getProperty(GLOBAL_HANDLERS);
        if (handlersList == null)
        {
            LoggerDiagnostics.error("No 'handlers' property defined.");
            configureGlobalLoggingLevel(props);
            return;
        }
        LoggerDiagnostics.info("Handlers to configure: " + handlersList);
        String[] splitHandlerAliases = handlersList.split("\\s*,\\s*");
        Set<String> handlerAliases = Arrays.stream(splitHandlerAliases)
                .map(String::trim)
                .collect(Collectors.toSet());

        removeAllHandlersBeforeInitialization();
        configureGlobalLoggingLevel(props);
        for (String alias : handlerAliases)
        {
            LoggerDiagnostics.info("Initializing handler: " + alias);
            try
            {
                Handler handler = createHandler(alias, props, logFile);
                configureCommonProperties(handler, alias, props);
                Logger.getLogger("").addHandler(handler);
                LoggerDiagnostics.info("Handler added: " + handler);
            } catch (Exception e)
            {
                LoggerDiagnostics.error("Error initializing handler '" + alias + "': " + e.getMessage(),e);
            }
        }

        // --- attach per‐logger handlers (AUTH, TRACKING, ACCESS, etc.) ---
        for (String propertyKey : props.stringPropertyNames()) {
            // look for keys like "AUTH.handlers", but skip the global "handlers"
            if (propertyKey.endsWith(INTERNAL_HANDLERS) && !propertyKey.equals(GLOBAL_HANDLERS)) {
                String loggerName = propertyKey.substring(0, propertyKey.length() - INTERNAL_HANDLERS.length());
                String[] loggerAliases = props.getProperty(propertyKey).split("\\s*,\\s*");
                Logger logger = null;
                try
                {
                    LoggerDiagnostics.error(logFile.getAbsolutePath() + " : " + loggerName);
                    logger = Logger.getLogger(loggerName);
                } catch (Throwable e)
                {
                    System.out.println(logFile.getAbsolutePath() + e);
                    throw new RuntimeException(logFile.getAbsolutePath(), e);
                }
                for (Handler h : logger.getHandlers()) {
                    logger.removeHandler(h);
                }
                for (String loggerAlias : loggerAliases) {
                    try {
                        Handler handler = createHandler(loggerAlias, props, logFile);
                        configureCommonProperties(handler, loggerAlias, props);
                        logger.setUseParentHandlers(useParentHandlers(loggerName, props));
                        logger.addHandler(handler);
                        LoggerDiagnostics.info(
                                "Attached handler '" + loggerAlias +
                                        "' to logger '" + loggerName + "'");
                    } catch (Exception e) {
                        LoggerDiagnostics.error(
                                "Failed to attach handler '" + loggerAlias +
                                        "' to logger '" + loggerName + "': " + e.getMessage(), e);
                    }
                }
            }
        }
        // --- end per‐logger handlers ---

        configureSpecificLoggerLevels(props, handlerAliases);
    }

    private static boolean useParentHandlers(String loggerName, Properties props)
    {
        String useParentHandlersKey = loggerName + USE_PARENT_HANDLERS;
        String useParentHandlersValue = props.getProperty(useParentHandlersKey);
        boolean useParentHandlers = false; // default
        if (useParentHandlersValue != null) {
            useParentHandlers = Boolean.parseBoolean(useParentHandlersValue.trim());
        }
        return useParentHandlers;
    }

    private static void removeAllHandlersBeforeInitialization()
    {
        LoggerDiagnostics.info("removeAllHandlersBeforeInitialization()");
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers())
        {
            LoggerDiagnostics.info("Removing handler: " + h);
            root.removeHandler(h);
        }

        LoggerDiagnostics.info("also reset : LogManager.getLogManager().reset()");
        //LogManager.getLogManager().reset();
    }

    private static Properties loadProperties(File configurationFile)
    {
        LoggerDiagnostics.info("loadProperties(file=" + configurationFile.getPath() + ")");
        Properties props = new Properties();
        InputStream is = null;
        boolean loaded = false;

        // 1. Try filesystem
        if (configurationFile.exists() && configurationFile.isFile())
        {
            try
            {
                LoggerDiagnostics.info("Loading properties from filesystem: " + configurationFile.getAbsolutePath());
                is = new FileInputStream(configurationFile);
            } catch (IOException e)
            {
                LoggerDiagnostics.error("IOException opening file: " + e.getMessage());
            }
        } else
        {
            LoggerDiagnostics.info("File not found on filesystem; will try classpath lookup.");
        }

        // 2. Fallback to classpath
        if (is == null)
        {
            String resourcePath = configurationFile.getPath().replace(File.separatorChar, '/');
            if (resourcePath.startsWith("/"))
            {
                resourcePath = resourcePath.substring(1);
            }
            LoggerDiagnostics.info("Trying classpath resource: " + resourcePath);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null)
            {
                is = cl.getResourceAsStream(resourcePath);
                LoggerDiagnostics.info("ContextClassLoader resourceStream=" + (is != null));
            }
            if (is == null)
            {
                is = LogInitializer.class.getResourceAsStream("/" + resourcePath);
                LoggerDiagnostics.info("Class.getResourceAsStream(/" + resourcePath + ")=" + (is != null));
                if (is == null)
                {
                    is = LogInitializer.class.getResourceAsStream(resourcePath);
                    LoggerDiagnostics.info("Class.getResourceAsStream(" + resourcePath + ")=" + (is != null));
                }
            }
        }

        if (is != null)
        {
            try (InputStream in = is)
            {
                props.load(in);
                loaded = true;
                LoggerDiagnostics.info("Properties loaded successfully.");
            } catch (IOException e)
            {
                LoggerDiagnostics.error("Error loading properties: " + e.getMessage());
            }
        } else
        {
            LoggerDiagnostics.error("Could not find configuration resource: " + configurationFile.getPath());
        }
        return loaded ? props : null;
    }

    private static void configureGlobalLoggingLevel(Properties props)
    {
        LoggerDiagnostics.info("configureGlobalLoggingLevel");
        String global = props.getProperty(GLOBAL_LEVEL_KEY);
        LoggerDiagnostics.info(".global.level=" + global);
        if (global != null && !global.isEmpty())
        {
            Level lvl = tryGetLogLevel(global);
            if (lvl != null)
            {
                Logger.getLogger("").setLevel(lvl);
                LoggerDiagnostics.info("Root logger level set to " + lvl);
            }
        }
    }

    private static Level tryGetLogLevel(String levelStr)
    {
        LoggerDiagnostics.info("tryGetLogLevel(" + levelStr + ")");
        if (levelStr == null || levelStr.isEmpty())
        {
            return null;
        }
        try
        {
            org.apache.log4j.Level log4jLevel = org.apache.log4j.Level.parse(levelStr);
            Level jul = LoggingUtils.mapToJULLevel(log4jLevel);
            LoggerDiagnostics.info("Parsed log4j level " + log4jLevel + " -> JUL level " + jul);
            return jul;
        } catch (Exception e)
        {
            try
            {
                Level jul = Level.parse(levelStr);
                LoggerDiagnostics.info("Parsed JUL level " + jul);
                return jul;
            } catch (Exception ex)
            {
                LoggerDiagnostics.error("Failed to parse level: " + levelStr);
                return null;
            }
        }
    }

    private static Handler createHandler(String alias, Properties props, File logFile) throws Exception
    {
        LoggerDiagnostics.info("createHandler(alias=" + alias + ")");
        String className = props.getProperty(alias + ".class");
        LoggerDiagnostics.info("Handler class name=" + className);
        if (className == null)
        {
            throw new IllegalArgumentException("No class defined for handler alias: \"" + alias + "\" , log file : " + logFile.getAbsolutePath());
        }
        Class<?> hc = Class.forName(className);
        Handler handler;
        if (DailyRollingFileHandler.class.isAssignableFrom(hc))
        {
            String pattern = props.getProperty(alias + LOG_FILE_NAME);
            int maxLogFileSize = Integer.parseInt(props.getProperty(alias + MAX_LOG_FILE_SIZE,
                    String.valueOf(DailyRollingFileHandler.DEFAULT_MAX_LOG_FILE_SIZE)));
            boolean append = Boolean.parseBoolean(props.getProperty(alias + APPEND, "false"));
            int maxLogRotations = Integer.parseInt(props.getProperty(alias + MAX_LOG_ROTATIONS,
                    String.valueOf(DailyRollingFileHandler.DEFAULT_MAX_LOG_ROTATIONS)));
            LoggerDiagnostics.info("Using file-handler constructor: pattern=" + pattern
                    + ", maxLogFileSize=" + maxLogFileSize
                    + ", maxLogRotations=" + maxLogRotations + ", append=" + append);

            handler = (Handler) hc.getConstructor(String.class, int.class, boolean.class, int.class)
                    .newInstance(pattern, maxLogFileSize, append, maxLogRotations);

        } else if (ConsoleHandler.class.isAssignableFrom(hc)
                    || NullHandler.class.isAssignableFrom(hc)
                    || Handler.class.isAssignableFrom(hc))
        {
            LoggerDiagnostics.info("Using no-arg constructor for handler");
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
            String messagePattern = props.getProperty(alias + MESSAGE_PATTERN);
            if (messagePattern != null) {
                PatternFormatter patternFormatter = new PatternFormatter(messagePattern);
                handler.setFormatter(patternFormatter);
            }
        }

        // Set encoding if specified.
        String encoding = props.getProperty(alias + ENCODING);
        if (encoding != null) {
            handler.setEncoding(encoding);
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
                    LoggerDiagnostics.error("Could not set host for SocketHandler for alias: " + alias);
                }
            } catch (Exception e) {
                LoggerDiagnostics.error("Error setting host for SocketHandler for alias: " + alias);
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
                    LoggerDiagnostics.error("Could not set port for SocketHandler for alias: " + alias);
                }
            } catch (Exception e) {
                LoggerDiagnostics.error("Error setting port for SocketHandler for alias: " + alias);
            }
        }
    }

    /**
     * Configures levels for specific loggers defined in the properties.
     * Example: org.hibernate.level = WARN
     */
    private static void configureSpecificLoggerLevels(Properties props, Set<String> handlerAliases) {
        LoggerDiagnostics.info("Configuring specific logger levels...");
        for (String key : props.stringPropertyNames()) {
            if (key.endsWith(LEVEL_SUFFIX) && !key.equals(GLOBAL_LEVEL_KEY)) {
                // Extract the potential logger name or handler alias part
                String loggerName = key.substring(0, key.length() - LEVEL_SUFFIX.length());

                // IMPORTANT: Check if this key belongs to a handler configuration
                if (handlerAliases.contains(loggerName)) {
                    // This is a handler's level (e.g., myConsoleHandler.level), already processed. Skip it.
                    LoggerDiagnostics.debug("Skipping handler level key: " + key); // Optional debug log
                    continue;
                }

                // If it's not global and not a handler level, assume it's a specific logger level
                String levelStr = props.getProperty(key);
                Level level = tryGetLogLevel(levelStr);

                if (level != null) {
                    try {
                        Logger logger = Logger.getLogger(loggerName);
                        logger.setLevel(level);
                        LoggerDiagnostics.info("Set level " + level.getName() + " for logger '" + loggerName + "'");
                    } catch (Exception e) {
                        // Catch potential issues getting the logger (though unlikely)
                        LoggerDiagnostics.error("ERROR: Could not set level for logger '" + loggerName + "' from property '" + key + "': " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                } else {
                    LoggerDiagnostics.error("WARNING: Could not parse level '" + levelStr + "' for logger property '" + key + "'. Level not set.");
                }
            }
        }
        LoggerDiagnostics.info("Finished configuring specific logger levels.");
    }

    /*
     * May be needed for some circumstances where an external
     * lib may reset the JUL
     */
    public static void forceReinit() {
        initialized = false;
        LoggerDiagnostics.info("forceReinit() called.");
        init();
    }

}
