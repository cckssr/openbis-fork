package ch.ethz.sis.afsserver.server.archiving;

import java.io.File;

import ch.ethz.sis.afsserver.server.observer.impl.OpenBISUtils;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.openbis.dss.generic.shared.IConfigProvider;

public class ConfigProvider implements IConfigProvider
{

    private final Configuration configuration;

    public ConfigProvider(Configuration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    public File getStoreRoot()
    {
        return new File(AtomicFileSystemServerParameterUtil.getStorageRoot(configuration));
    }

    @Override
    public String getDataStoreCode()
    {
        return OpenBISUtils.AFS_DATA_STORE_CODE;
    }

    @Override public String getOpenBisServerUrl()
    {
        throw new UnsupportedOperationException();
    }

    @Override public int getDataStreamTimeout()
    {
        throw new UnsupportedOperationException();
    }

    @Override public int getDataStreamMaxTimeout()
    {
        throw new UnsupportedOperationException();
    }
}
