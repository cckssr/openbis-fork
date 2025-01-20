package ch.ethz.sis.afsserver.server.shuffling;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import ch.ethz.sis.afsserver.server.common.ServiceProvider;
import ch.ethz.sis.afsserver.server.common.SimpleDataSetInformationDTO;

public class SimpleChecksumProvider implements IChecksumProvider
{
    @Override public long getChecksum(final String dataSetCode, final String relativePath) throws IOException
    {
        SimpleDataSetInformationDTO dataSet = ServiceProvider.getOpenBISService().tryGetDataSet(dataSetCode);

        if (dataSet != null)
        {
            File storeRoot = ServiceProvider.getConfigProvider().getStoreRoot();
            File shareFolder = new File(storeRoot, dataSet.getDataSetShareId());
            File dataSetFolder = new File(shareFolder, dataSet.getDataSetLocation());
            return FileUtils.checksumCRC32(new File(dataSetFolder, relativePath));
        } else
        {
            return -1;
        }
    }
}
