package ch.ethz.sis.afsserver.server.shuffling;

import java.io.IOException;

import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.pathinfo.DataSetFileRecord;
import ch.ethz.sis.pathinfo.IPathInfoAutoClosingDAO;
import net.lemnik.eodsql.QueryTool;

public class PathInfoChecksumProvider implements IChecksumProvider
{

    private IPathInfoAutoClosingDAO pathInfoDAO;

    public PathInfoChecksumProvider()
    {
        DatabaseConfiguration pathInfoConfiguration = PathInfoDatabaseConfiguration.getInstance(ServiceProvider.getConfiguration());
        if (pathInfoConfiguration != null)
        {
            pathInfoDAO = QueryTool.getQuery(pathInfoConfiguration.getDataSource(), IPathInfoAutoClosingDAO.class);
        }
    }

    @Override public long getChecksum(final String dataSetCode, final String relativePath) throws IOException
    {
        // TODO: once AFS offers an API method to get checksums use that instead of calling path info db directly

        if (pathInfoDAO != null)
        {
            Long dataSetId = pathInfoDAO.tryToGetDataSetId(dataSetCode);
            if (dataSetId != null)
            {
                DataSetFileRecord fileRecord = pathInfoDAO.tryToGetRelativeDataSetFile(dataSetId, relativePath);
                if (fileRecord != null && fileRecord.checksum_crc32 != null)
                {
                    return fileRecord.checksum_crc32;
                }
            }
        }

        return new SimpleChecksumProvider().getChecksum(dataSetCode, relativePath);
    }
}
