package ch.ethz.sis.openbis.afsserver.server.observer.impl.api;

import java.time.OffsetDateTime;
import java.util.List;

import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsserver.server.Worker;
import ch.ethz.sis.afsserver.server.observer.APICall;
import ch.ethz.sis.openbis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.pathinfo.DataSetFileRecord;
import ch.ethz.sis.pathinfo.IPathInfoAutoClosingDAO;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import ch.systemsx.cisd.common.db.DBUtils;
import net.lemnik.eodsql.QueryTool;

public class UsePathInfoDatabaseAPIServerObserver
{

    private static final Logger logger = LogManager.getLogger(UsePathInfoDatabaseAPIServerObserver.class);

    private IPathInfoAutoClosingDAO pathInfoDAO;

    public UsePathInfoDatabaseAPIServerObserver(final Configuration configuration)
    {
        if (PathInfoDatabaseConfiguration.hasInstance(configuration))
        {
            pathInfoDAO = QueryTool.getQuery(PathInfoDatabaseConfiguration.getInstance(configuration).getDataSource(), IPathInfoAutoClosingDAO.class);
        }
    }

    public Object duringAPICall(Worker<TransactionConnection> worker, APICall apiCall)
            throws Exception
    {
        if ("list".equals(apiCall.getMethod()))
        {
            return list(worker, apiCall);
        }

        return apiCall.executeDefault();
    }

    private Object list(Worker<TransactionConnection> worker, APICall apiCall) throws Exception
    {
        if (pathInfoDAO != null)
        {
            String sourceOwner = (String) apiCall.getParams().get("sourceOwner");
            String source = (String) apiCall.getParams().get("source");
            Boolean recursively = (Boolean) apiCall.getParams().get("recursively");

            Long dataSetId = pathInfoDAO.tryToGetDataSetId(sourceOwner);

            if (dataSetId != null)
            {
                if (source == null || source.isBlank())
                {
                    source = "";
                } else
                {
                    if (source.startsWith("/"))
                    {
                        source = source.substring(1);
                    }
                    if (source.endsWith("/"))
                    {
                        source = source.substring(0, source.length() - 1);
                    }
                }

                DataSetFileRecord fileOrFolderRecord = pathInfoDAO.tryToGetRelativeDataSetFile(dataSetId, source);

                if (fileOrFolderRecord != null)
                {
                    logger.info("Owner: \"" + sourceOwner + "\" source: \"" + source + "\" found in the path info database with id: "
                            + fileOrFolderRecord.id);

                    if (fileOrFolderRecord.is_directory)
                    {
                        List<DataSetFileRecord> fileRecords;

                        if (Boolean.TRUE.equals(recursively))
                        {
                            String likeClause = DBUtils.escapeLikeClauseSpecialCharacters(source);

                            if (source.isEmpty() || source.endsWith("/"))
                            {
                                likeClause += "%";
                            } else
                            {
                                likeClause += "/%";
                            }

                            fileRecords = pathInfoDAO.listDataSetFilesByRelativePathLikeExpression(dataSetId, likeClause);
                        } else
                        {
                            fileRecords = pathInfoDAO.listChildrenByParentId(dataSetId, fileOrFolderRecord.id);
                        }

                        return fileRecords.stream().filter(fileRecord -> fileRecord.relative_path != null && !fileRecord.relative_path.isEmpty())
                                .map(fileRecord -> convert(sourceOwner, fileRecord))
                                .toArray(File[]::new);
                    } else
                    {
                        return new File[] { convert(sourceOwner, fileOrFolderRecord) };
                    }
                }
            }
        }

        return apiCall.executeDefault();
    }

    private static File convert(String owner, DataSetFileRecord record)
    {
        return new File(owner, "/" + record.relative_path, record.file_name, record.is_directory, record.is_directory ? null : record.size_in_bytes,
                record.last_modified != null ? record.last_modified.toInstant().atOffset(OffsetDateTime.now().getOffset()) : null);
    }

}
