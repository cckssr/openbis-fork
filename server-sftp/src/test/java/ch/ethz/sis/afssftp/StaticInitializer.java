package ch.ethz.sis.afssftp;

import ch.ethz.sis.shared.log.standard.LogFactory;
import ch.ethz.sis.shared.log.standard.LogFactoryFactory;
import ch.ethz.sis.shared.log.standard.LogManager;

public class StaticInitializer {
    static {
        initLogFactory();
    }

    public static void initialize() {
        // Simply makes sure this class is loaded and statically initialized
    }

    private static void initLogFactory() {
        try {
            LogFactoryFactory logFactoryFactory = new LogFactoryFactory();
            LogFactory logFactory = logFactoryFactory.create("ch.ethz.sis.shared.log.standard.impl.StandardLogFactory");
            logFactory.configure("logging.properties");
            LogManager.setLogFactory(logFactory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
