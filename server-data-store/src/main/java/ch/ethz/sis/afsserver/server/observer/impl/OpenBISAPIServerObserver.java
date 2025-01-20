package ch.ethz.sis.afsserver.server.observer.impl;

import java.nio.file.NoSuchFileException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.ethz.sis.afs.dto.operation.CopyOperation;
import ch.ethz.sis.afs.dto.operation.CreateOperation;
import ch.ethz.sis.afs.dto.operation.MoveOperation;
import ch.ethz.sis.afs.dto.operation.Operation;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Worker;
import ch.ethz.sis.afsserver.server.common.DatabaseConfiguration;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.observer.APICall;
import ch.ethz.sis.afsserver.server.observer.APIServerObserver;
import ch.ethz.sis.afsserver.server.pathinfo.PathInfoDatabaseConfiguration;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.PhysicalDataCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.FileFormatTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.ProprietaryStorageFormatPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.RelativeLocationLocatorTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.pathinfo.DataSetFileRecord;
import ch.ethz.sis.pathinfo.IPathInfoAutoClosingDAO;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.startup.Configuration;
import net.lemnik.eodsql.QueryTool;

public class OpenBISAPIServerObserver implements APIServerObserver<TransactionConnection>
{

    private String storageRoot;

    private String storageUuid;

    private Integer storageIncomingShareId;

    private String interactiveSessionKey;

    private OpenBISConfiguration openBISConfiguration;

    private IPathInfoAutoClosingDAO pathInfoDAO;

    @Override
    public void init(Configuration configuration) throws Exception
    {
        storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);
        storageUuid = AtomicFileSystemServerParameterUtil.getStorageUuid(configuration);
        storageIncomingShareId = AtomicFileSystemServerParameterUtil.getStorageIncomingShareId(configuration);
        interactiveSessionKey = AtomicFileSystemServerParameterUtil.getInteractiveSessionKey(configuration);
        openBISConfiguration = OpenBISConfiguration.getInstance(configuration);

        DatabaseConfiguration pathInfoConfiguration = PathInfoDatabaseConfiguration.getInstance(configuration);
        if (pathInfoConfiguration != null)
        {
            pathInfoDAO = QueryTool.getQuery(pathInfoConfiguration.getDataSource(), IPathInfoAutoClosingDAO.class);
        }
    }

    @Override
    public void beforeAPICall(Worker<TransactionConnection> worker, Request request) throws Exception
    {
        if (worker.isInteractiveSessionMode())
        {
            boolean isOnePhaseTransaction = !worker.isTransactionManagerMode();
            boolean isTwoPhaseTransaction = worker.isTransactionManagerMode();

            if (isOnePhaseTransaction)
            {
                if (request.getMethod().equals("commit"))
                {
                    Set<String> owners = getOwnersCreatedInTransaction(worker);

                    if (!owners.isEmpty())
                    {
                        OpenBIS openBIS = openBISConfiguration.getOpenBIS();
                        openBIS.setSessionToken(worker.getSessionToken());

                        createDataSets(openBIS, owners);
                    }
                }
            } else if (isTwoPhaseTransaction)
            {
                String owner = getOwnerCreatedInRequest(request);

                if (owner != null && !ownerExistsInTransaction(worker, owner) && !ownerExistsInStore(worker, owner))
                {
                    OpenBIS openBIS = openBISConfiguration.getOpenBIS();
                    openBIS.setSessionToken(worker.getSessionToken());
                    openBIS.setTransactionId(worker.getConnection().getTransaction().getUuid());
                    openBIS.setInteractiveSessionKey(interactiveSessionKey);

                    createDataSets(openBIS, List.of(owner));
                }
            }
        }
    }

    @Override public Object duringAPICall(Worker<TransactionConnection> worker, APICall apiCall)
            throws Exception
    {
        if ("list".equals(apiCall.getMethod()))
        {
            if (pathInfoDAO != null)
            {
                String sourceOwner = (String) apiCall.getParams().get("sourceOwner");
                String source = (String) apiCall.getParams().get("source");

                Long dataSetId = pathInfoDAO.tryToGetDataSetId(sourceOwner);

                if (dataSetId != null)
                {
                    DataSetFileRecord fileOrFolderRecord = pathInfoDAO.tryToGetRelativeDataSetFile(dataSetId, source);
                    if (fileOrFolderRecord != null)
                    {
                        if (fileOrFolderRecord.is_directory)
                        {
                            List<DataSetFileRecord> fileRecords = pathInfoDAO.listChildrenByParentId(dataSetId, fileOrFolderRecord.id);
                            return fileRecords.stream().map(fileRecord -> convert(sourceOwner, fileRecord))
                                    .collect(Collectors.toList());
                        } else
                        {
                            return List.of(convert(sourceOwner, fileOrFolderRecord));
                        }
                    }
                }
            }
        }

        return apiCall.executeDefault();
    }

    @Override
    public void afterAPICall(Worker<TransactionConnection> worker, Request request) throws Exception
    {
        if (!worker.isInteractiveSessionMode())
        {
            String owner = getOwnerCreatedInRequest(request);

            if (owner != null && !ownerExistsInStore(worker, owner))
            {
                OpenBIS openBIS = openBISConfiguration.getOpenBIS();
                openBIS.setSessionToken(worker.getSessionToken());

                createDataSets(openBIS, List.of(owner));
            }
        }
    }

    private String getOwnerCreatedInRequest(Request request)
    {
        switch (request.getMethod())
        {
            case "write":
            case "create":
                return (String) request.getParams().get("owner");
            case "copy":
            case "move":
                return (String) request.getParams().get("targetOwner");
            default:
                return null;
        }
    }

    private Set<String> getOwnersCreatedInTransaction(Worker<TransactionConnection> worker)
    {
        List<String> paths = new ArrayList<>();

        if (worker.getConnection().getTransaction().getOperations() != null)
        {
            for (Operation operation : worker.getConnection().getTransaction().getOperations())
            {
                if (operation instanceof CreateOperation)
                {
                    paths.add(((CreateOperation) operation).getSource());
                } else if (operation instanceof WriteOperation)
                {
                    paths.add(((WriteOperation) operation).getSource());
                } else if (operation instanceof CopyOperation)
                {
                    paths.add(((CopyOperation) operation).getTarget());
                } else if (operation instanceof MoveOperation)
                {
                    paths.add(((MoveOperation) operation).getTarget());
                }
            }
        }

        return paths.stream().map(this::extractOwnerFromPath).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private boolean ownerExistsInTransaction(Worker<TransactionConnection> worker, String owner)
    {
        return getOwnersCreatedInTransaction(worker).contains(owner);
    }

    private boolean ownerExistsInStore(Worker<TransactionConnection> worker, String owner) throws Exception
    {
        try
        {
            List<File> files = worker.list(owner, "", false);
            return !files.isEmpty();
        } catch (NoSuchFileException e)
        {
            return false;
        }
    }

    private void createDataSets(OpenBIS openBIS, Collection<String> owners) throws Exception
    {
        List<DataSetCreation> creations = new ArrayList<>();

        for (String owner : owners)
        {
            Experiment foundExperiment = findExperiment(openBIS, owner);

            if (foundExperiment != null)
            {
                creations.add(createDataSetCreation(owner, null));
            } else
            {
                Sample foundSample = findSample(openBIS, owner);

                if (foundSample != null)
                {
                    creations.add(createDataSetCreation(null, owner));
                }
            }
        }

        if (!creations.isEmpty())
        {
            for (DataSetCreation creation : creations)
            {
                try
                {
                    openBIS.createDataSetsAS(List.of(creation));
                } catch (Exception e)
                {
                    if (e.getMessage() == null || !e.getMessage().contains("DataSet already exists in the database and needs to be unique"))
                    {
                        throw e;
                    }
                }

            }
        }
    }

    private DataSetCreation createDataSetCreation(String experimentPermId, String samplePermId)
    {
        PhysicalDataCreation physicalCreation = new PhysicalDataCreation();
        physicalCreation.setShareId(storageIncomingShareId.toString());
        physicalCreation.setFileFormatTypeId(new FileFormatTypePermId("PROPRIETARY"));
        physicalCreation.setLocatorTypeId(new RelativeLocationLocatorTypePermId());
        physicalCreation.setStorageFormatId(new ProprietaryStorageFormatPermId());
        physicalCreation.setH5arFolders(false);
        physicalCreation.setH5Folders(false);

        DataSetCreation creation = new DataSetCreation();
        creation.setAfsData(true);
        creation.setDataStoreId(new DataStorePermId(OpenBISUtils.AFS_DATA_STORE_CODE));
        creation.setDataSetKind(DataSetKind.PHYSICAL);
        creation.setTypeId(new EntityTypePermId(OpenBISUtils.AFS_DATA_SET_TYPE_CODE));
        creation.setPhysicalData(physicalCreation);

        if (experimentPermId != null)
        {
            creation.setCode(experimentPermId);
            creation.setExperimentId(new ExperimentPermId(experimentPermId));
            physicalCreation.setLocation(createDataSetLocation(experimentPermId));
        } else if (samplePermId != null)
        {
            creation.setCode(samplePermId);
            creation.setSampleId(new SamplePermId(samplePermId));
            physicalCreation.setLocation(createDataSetLocation(samplePermId));
        }

        return creation;
    }

    private String createDataSetLocation(String dataSetCode)
    {
        List<String> elements = new LinkedList<>(Arrays.asList(IOUtils.getShards(dataSetCode)));
        elements.add(dataSetCode);
        return IOUtils.getPath(storageUuid, elements.toArray(new String[] {}));
    }

    private Experiment findExperiment(OpenBIS openBIS, String experimentPermId)
    {
        Map<IExperimentId, Experiment> experiments =
                openBIS.getExperiments(List.of(new ExperimentPermId(experimentPermId)), new ExperimentFetchOptions());

        if (!experiments.isEmpty())
        {
            return experiments.values().iterator().next();
        } else
        {
            return null;
        }
    }

    private Sample findSample(OpenBIS openBIS, String samplePermId)
    {
        Map<ISampleId, Sample> samples =
                openBIS.getSamples(List.of(new SamplePermId(samplePermId)), new SampleFetchOptions());

        if (!samples.isEmpty())
        {
            return samples.values().iterator().next();
        } else
        {
            return null;
        }
    }

    private String extractOwnerFromPath(String ownerPath)
    {
        if (ownerPath.startsWith(storageRoot))
        {
            ownerPath = ownerPath.substring(storageRoot.length());
        }

        Pattern compile = Pattern.compile("/\\d+/.+/../../../(.+)/.*");
        Matcher matcher = compile.matcher(ownerPath);

        if (matcher.matches())
        {
            return matcher.group(1);
        } else
        {
            return null;
        }
    }

    private static File convert(String owner, DataSetFileRecord record)
    {
        return new File(owner, record.relative_path, record.file_name, record.is_directory, record.size_in_bytes,
                record.last_modified != null ? record.last_modified.toInstant().atOffset(OffsetDateTime.now().getOffset()) : null);
    }

}
