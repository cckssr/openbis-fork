package ch.ethz.sis.afsserver.server.observer.impl;

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.ethz.sis.afs.api.dto.ExceptionReason;
import ch.ethz.sis.afs.dto.operation.CopyOperation;
import ch.ethz.sis.afs.dto.operation.CreateOperation;
import ch.ethz.sis.afs.dto.operation.MoveOperation;
import ch.ethz.sis.afs.dto.operation.Operation;
import ch.ethz.sis.afs.dto.operation.WriteOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.afs.manager.TransactionConnection;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.Worker;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.PhysicalDataCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
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
import ch.ethz.sis.shared.exception.ThrowableReason;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.Logger;
import ch.ethz.sis.shared.startup.Configuration;

public class OpenBISCreateDataSetsAPIServerObserver
{

    private static final Logger logger = LogManager.getLogger(OpenBISCreateDataSetsAPIServerObserver.class);

    private final String storageRoot;

    private final String storageUuid;

    private final Integer storageIncomingShareId;

    private final String interactiveSessionKey;

    private final OpenBISConfiguration openBISConfiguration;

    public OpenBISCreateDataSetsAPIServerObserver(Configuration configuration)
    {
        storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);
        storageUuid = AtomicFileSystemServerParameterUtil.getStorageUuid(configuration);
        storageIncomingShareId = AtomicFileSystemServerParameterUtil.getStorageIncomingShareId(configuration);
        interactiveSessionKey = AtomicFileSystemServerParameterUtil.getInteractiveSessionKey(configuration);
        openBISConfiguration = OpenBISConfiguration.getInstance(configuration);
    }

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
                List<String> owners = getOwnerCreatedInRequest(request);
                if (owners != null) {
                    for (Iterator<String> it = owners.iterator(); it.hasNext() ;)
                    {
                        String owner = it.next();
                        if (ownerExistsInTransaction(worker, owner) || ownerExistsInStore(worker, owner)) {
                            it.remove();
                        }
                    }

                    if (!owners.isEmpty()) {
                        OpenBIS openBIS = openBISConfiguration.getOpenBIS();
                        openBIS.setSessionToken(worker.getSessionToken());
                        openBIS.setTransactionId(worker.getConnection().getTransaction().getUuid());
                        openBIS.setInteractiveSessionKey(interactiveSessionKey);

                        createDataSets(openBIS, owners);
                    }
                }
            }
        }
    }

    public void afterAPICall(Worker<TransactionConnection> worker, Request request, Response response) throws Exception
    {
        if (!worker.isInteractiveSessionMode())
        {
            // We modify a mutable to avoid recreating multiple lists
            List<String> owners = getOwnerCreatedInRequest(request);
            if (owners != null) {
                for (Iterator<String> it = owners.iterator(); it.hasNext() ;)
                {
                    if (ownerExistsInStore(worker, it.next())) {
                        it.remove();
                    }
                }

                if (!owners.isEmpty()) {
                    OpenBIS openBIS = openBISConfiguration.getOpenBIS();
                    openBIS.setSessionToken(worker.getSessionToken());

                    createDataSets(openBIS, owners);
                }
            }
        }
    }

    private List<String> getOwnerCreatedInRequest(Request request)
    {
        // We return a mutable list just to avoid recreating multiple lists
        ArrayList<String> owners = null;
        switch (request.getMethod())
        {
            case "write":
                Chunk[] chunks = (Chunk[]) request.getParams().get("chunks");
                owners = new ArrayList<>(chunks.length);
                for (Chunk chunk:chunks) {
                    owners.add(chunk.getOwner());
                }
                break;
            case "create":
                owners = new ArrayList<>(1);
                owners.add((String) request.getParams().get("owner"));
                break;
            case "copy":
            case "move":
                owners = new ArrayList<>(1);
                owners.add((String) request.getParams().get("targetOwner"));
                break;
        }
        return owners;
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
        final Set<String> ownersCreatedInTransaction = getOwnersCreatedInTransaction(worker);
        return ownersCreatedInTransaction.contains(owner);
    }

    private boolean ownerExistsInStore(Worker<TransactionConnection> worker, String owner) throws Exception
    {
        try
        {
            File[] files = worker.list(owner, "", false);
            return files.length > 0;
        } catch (NoSuchFileException e) {
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
                    final List<DataSetPermId> dataSetIds = openBIS.createDataSetsAS(List.of(creation));
                    logger.info("Created data set: \"" + dataSetIds.get(0) + "\" in the application server.");
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

        Pattern compile = Pattern.compile("/\\d+/.+/../../../([^/]+)/.*");
        Matcher matcher = compile.matcher(ownerPath);

        if (matcher.matches())
        {
            return matcher.group(1);
        } else
        {
            return null;
        }
    }

}
