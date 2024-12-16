package ch.ethz.sis.openbis.generic.server.dss.plugins.imaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AdapterRepository
{
    private final Map<String, Properties> REPOSITORY = new HashMap<>();

    private static AdapterRepository instance;

    private AdapterRepository()
    {
    }

    public static AdapterRepository getInstance()
    {
        if(instance == null)
        {
            instance = new AdapterRepository();
        }
        return instance;
    }

    public void put(String key, Properties properties) {
        REPOSITORY.put(key, properties);
    }

    public Properties get(String key) {
        return REPOSITORY.getOrDefault(key, null);
    }

}
