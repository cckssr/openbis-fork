package ch.ethz.sis.openbis.generic.server.as.plugins.imaging;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.CustomASServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.ICustomASServiceExecutor;
import ch.ethz.sis.openbis.generic.asapi.v3.plugin.service.context.CustomASServiceContext;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.service.CustomDSSServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.service.id.ICustomDSSServiceId;
import ch.ethz.sis.openbis.generic.dssapi.v3.plugin.service.ICustomDSSServiceExecutor;

import java.io.Serializable;
import java.util.Properties;

public class ImagingPropertiesInjector implements ICustomASServiceExecutor
{
    private static final String SEPARATOR = ",";

    public ImagingPropertiesInjector(Properties properties)
    {
        if(properties.containsKey("adaptors")) {
            AdapterRepository repository = AdapterRepository.getInstance();
            String[] adaptors = properties.getProperty("adaptors").split(SEPARATOR);
            for (String s : adaptors)
            {
                String adaptor = s.trim();
                if(!adaptor.isEmpty())
                {
                    repository.put(adaptor, properties);
                }
            }
        }
    }


//    @Override
//    public Serializable executeService(String sessionToken, ICustomDSSServiceId serviceId,
//            CustomDSSServiceExecutionOptions options)
//    {
//        return null;
//    }

    @Override
    public Object executeService(CustomASServiceContext context,
            CustomASServiceExecutionOptions options)
    {
        return null;
    }
}
