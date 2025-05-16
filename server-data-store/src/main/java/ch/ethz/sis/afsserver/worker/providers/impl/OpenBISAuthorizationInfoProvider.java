/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afsserver.worker.providers.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.ethz.sis.afs.dto.Lock;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.afs.manager.LockMapper;
import ch.ethz.sis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.server.observer.impl.OpenBISUtils;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameterUtil;
import ch.ethz.sis.afsserver.worker.WorkerContext;
import ch.ethz.sis.afsserver.worker.providers.AuthorizationInfoProvider;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.EntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.Event;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.EventType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.fetchoptions.EventFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.search.EventSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.rights.Right;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.rights.Rights;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.rights.fetchoptions.RightsFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.shared.io.FilePermission;
import ch.ethz.sis.shared.io.IOUtils;
import ch.ethz.sis.shared.startup.Configuration;

public class OpenBISAuthorizationInfoProvider implements AuthorizationInfoProvider, LockMapper<UUID, String>
{

    private String storageRoot;

    private String storageUuid;

    private Integer[] storageShares;

    private Integer storageIncomingShareId;

    private String interactiveSessionKey;

    private OpenBISConfiguration openBISConfiguration;

    private Pattern ownerPathPattern;

    @Override
    public void init(Configuration configuration) throws Exception
    {
        storageRoot = AtomicFileSystemServerParameterUtil.getStorageRoot(configuration);
        storageUuid = AtomicFileSystemServerParameterUtil.getStorageUuid(configuration);
        storageShares = IOUtils.getShares(storageRoot);
        if (storageShares.length == 0)
        {
            throw AFSExceptions.NoSharesFound.getInstance();
        }
        storageIncomingShareId = AtomicFileSystemServerParameterUtil.getStorageIncomingShareId(configuration);
        interactiveSessionKey = AtomicFileSystemServerParameterUtil.getInteractiveSessionKey(configuration);
        openBISConfiguration = OpenBISConfiguration.getInstance(configuration);
        ownerPathPattern = Pattern.compile("\\d/.+/../../../(.+)");
    }

    @Override
    public boolean doesSessionHaveRights(WorkerContext workerContext, String owner, Set<FilePermission> permissions)
    {
        OpenBIS openBIS = openBISConfiguration.getOpenBIS();
        openBIS.setSessionToken(workerContext.getSessionToken());

        if (workerContext.isTransactionManagerMode())
        {
            openBIS.setTransactionId(workerContext.getTransactionId());
            openBIS.setInteractiveSessionKey(interactiveSessionKey);
        }

        String ownerShare = null;
        ObjectPermId ownerPermId = null;
        Set<FilePermission> ownerSupportedPermissions = null;

        Experiment foundExperiment = findExperiment(openBIS, owner);

        if (foundExperiment != null)
        {
            ownerPermId = foundExperiment.getPermId();

            if (foundExperiment.isImmutableData())
            {
                ownerSupportedPermissions = Set.of(FilePermission.Read);
            } else
            {
                ownerSupportedPermissions = Set.of(FilePermission.Read, FilePermission.Write);
            }
        } else
        {
            Sample foundSample = findSample(openBIS, owner);

            if (foundSample != null)
            {
                ownerPermId = foundSample.getPermId();

                if (foundSample.isImmutableData())
                {
                    ownerSupportedPermissions = Set.of(FilePermission.Read);
                } else
                {
                    ownerSupportedPermissions = Set.of(FilePermission.Read, FilePermission.Write);
                }
            } else
            {
                DataSet foundDataSet = findDataSet(openBIS, owner);

                if (foundDataSet != null)
                {
                    ownerPermId = foundDataSet.getPermId();
                    ownerShare = foundDataSet.getPhysicalData().getShareId();
                    ownerSupportedPermissions = Set.of(FilePermission.Read);
                }
            }
        }

        if (ownerPermId != null)
        {
            if (hasPermissions(openBIS, ownerPermId, ownerSupportedPermissions, permissions))
            {
                String ownerPath = findOwnerPath(ownerPermId.getPermId(), ownerShare);
                workerContext.getOwnerPathMap().put(owner, ownerPath);
                return true;
            }
        } else
        {
            SessionInformation sessionInformation = openBIS.getSessionInformation();

            if (sessionInformation != null && sessionInformation.getPerson().getUserId().equals(openBISConfiguration.getOpenBISUser()))
            {
                Event deletion = findAfsDataSetDeletion(openBIS, owner);

                if (deletion != null)
                {
                    String ownerPath = findOwnerPath(deletion.getIdentifier(), null);
                    workerContext.getOwnerPathMap().put(owner, ownerPath);
                    return true;
                }
            }
        }

        return false;
    }

    @Override public Lock<UUID, String> mapLock(final Lock<UUID, String> lock)
    {
        Path storageRootPath = Paths.get(storageRoot);
        Path lockPath = Paths.get(lock.getResource());
        Path relativeLockPath = storageRootPath.relativize(lockPath);

        Matcher matcher = ownerPathPattern.matcher(relativeLockPath.toString());
        if (matcher.matches())
        {
            return new Lock<>(lock.getOwner(), IOUtils.RELATIVE_PATH_ROOT + matcher.group(1), lock.getType());
        } else
        {
            throw new IllegalArgumentException("Lock relative path: " + lockPath + " does not match the expected format: " + ownerPathPattern);
        }
    }

    private boolean hasPermissions(OpenBIS openBIS, ObjectPermId ownerPermId,
            Set<FilePermission> ownerSupportedPermissions,
            Set<FilePermission> requestedPermissions)
    {
        for (FilePermission requestPermission : requestedPermissions)
        {
            if (!ownerSupportedPermissions.contains(requestPermission))
            {
                return false;
            }
        }

        Set<FilePermission> foundPermissions = new HashSet<>();
        foundPermissions.add(FilePermission.Read);

        if (requestedPermissions.equals(foundPermissions))
        {
            return true;
        }

        Rights rights = openBIS.getRights(List.of(ownerPermId), new RightsFetchOptions()).get(ownerPermId);

        if (rights.getRights().contains(Right.UPDATE))
        {
            foundPermissions.add(FilePermission.Write);
        }

        for (FilePermission requestedPermission : requestedPermissions)
        {
            if (!foundPermissions.contains(requestedPermission))
            {
                return false;
            }
        }

        return true;
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

    private DataSet findDataSet(OpenBIS openBIS, String dataSetPermId)
    {
        IDataSetId dataSetId = new DataSetPermId(dataSetPermId);

        DataSetFetchOptions fo = new DataSetFetchOptions();
        fo.withPhysicalData();

        Map<IDataSetId, DataSet> dataSets = openBIS.getDataSets(List.of(dataSetId), fo);

        if (!dataSets.isEmpty())
        {
            return dataSets.values().iterator().next();
        } else
        {
            return null;
        }
    }

    private Event findAfsDataSetDeletion(OpenBIS openBIS, String identifier)
    {
        EventSearchCriteria criteria = new EventSearchCriteria();
        criteria.withEventType().thatEquals(EventType.DELETION);
        criteria.withEntityType().thatEquals(EntityType.DATA_SET);
        criteria.withIdentifier().thatEquals(identifier);

        SearchResult<Event> searchResult = openBIS.searchEvents(criteria, new EventFetchOptions());

        if (!searchResult.getObjects().isEmpty())
        {
            Event deletion = searchResult.getObjects().get(0);

            if (deletion.getDescription() != null && deletion.getDescription().startsWith("/" + OpenBISUtils.AFS_DATA_STORE_CODE))
            {
                return deletion;
            }
        }

        return null;
    }

    private String findOwnerPath(String ownerPermId, String ownerShare)
    {
        final String[] shards = IOUtils.getShards(ownerPermId);

        if (ownerShare != null)
        {
            return createOwnerPath(ownerShare, storageUuid, shards, ownerPermId);
        } else
        {
            for (Integer share : storageShares)
            {
                String potentialOwnerPath = createOwnerPath(share.toString(), storageUuid, shards, ownerPermId);

                if (Files.exists(Paths.get(storageRoot, potentialOwnerPath)))
                {
                    return potentialOwnerPath;
                }
            }

            return createOwnerPath(storageIncomingShareId.toString(), storageUuid, shards, ownerPermId);
        }
    }

    private String createOwnerPath(String shareId, String storageUuid, String[] shards, String ownerFolder)
    {
        List<String> elements = new LinkedList<>();
        elements.add(shareId);
        elements.add(storageUuid);
        elements.addAll(Arrays.asList(shards));
        elements.add(ownerFolder);
        return IOUtils.getPath("", elements.toArray(new String[] {}));
    }

}
