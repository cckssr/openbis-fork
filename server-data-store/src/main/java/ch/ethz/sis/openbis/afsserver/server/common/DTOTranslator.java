package ch.ethz.sis.openbis.afsserver.server.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ArchivingStatus;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.Complete;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.ContentCopy;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.ExternalDms;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.ExternalDmsAddressType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.Tag;
import ch.ethz.sis.openbis.generic.asapi.v3.exceptions.NotFetchedException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.AbstractExternalData;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ContainerDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetArchivingStatus;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataSetType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataStore;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ExperimentType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ExternalDataManagementSystem;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.ExternalDataManagementSystemType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.FileFormatType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.FileSystemContentCopy;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.GenericEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IContentCopy;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.LinkDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.LocatorType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Material;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.MaterialEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.MaterialIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.MaterialType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Metaproject;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Person;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PhysicalDataSet;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.PropertyType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.SampleType;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Space;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.UrlContentCopy;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.VocabularyTerm;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.VocabularyTermEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.dto.DatasetDescription;
import ch.systemsx.cisd.openbis.generic.shared.dto.SimpleDataSetInformationDTO;
import ch.systemsx.cisd.openbis.generic.shared.dto.identifier.SpaceIdentifier;

public class DTOTranslator
{

    public static AbstractExternalData translate(ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet dataSet)
    {
        if (dataSet == null)
        {
            return null;
        }

        if (!dataSet.getFetchOptions().hasPhysicalData() || !dataSet.getFetchOptions().hasLinkedData())
        {
            throw new NotFetchedException("Data set has not been fetched with physical data and linked data.");
        }

        AbstractExternalData externalData = null;

        if (dataSet.getPhysicalData() != null)
        {
            PhysicalDataSet physicalData = new PhysicalDataSet();
            physicalData.setSize(dataSet.getPhysicalData().getSize());
            physicalData.setComplete(translate(dataSet.getPhysicalData().getComplete()));
            physicalData.setStatus(translate(dataSet.getPhysicalData().getStatus()));
            physicalData.setPresentInArchive(dataSet.getPhysicalData().isPresentInArchive());
            physicalData.setStorageConfirmation(dataSet.getPhysicalData().isStorageConfirmation());
            physicalData.setSpeedHint(dataSet.getPhysicalData().getSpeedHint());
            physicalData.setLocation(dataSet.getPhysicalData().getLocation());
            physicalData.setShareId(dataSet.getPhysicalData().getShareId());
            physicalData.setArchivingRequested(dataSet.getPhysicalData().isArchivingRequested());
            if (dataSet.getPhysicalData().getFetchOptions().hasFileFormatType())
            {
                physicalData.setFileFormatType(translate(dataSet.getPhysicalData().getFileFormatType()));
            }
            if (dataSet.getPhysicalData().getFetchOptions().hasLocatorType())
            {
                physicalData.setLocatorType(translate(dataSet.getPhysicalData().getLocatorType()));
            }
            physicalData.setH5Folders(dataSet.getPhysicalData().isH5Folders());
            physicalData.setH5arFolders(dataSet.getPhysicalData().isH5arFolders());
            externalData = physicalData;
        } else if (dataSet.getLinkedData() != null)
        {
            LinkDataSet linkData = new LinkDataSet();
            linkData.setExternalCode(dataSet.getLinkedData().getExternalCode());
            linkData.setCopies(translate(dataSet.getLinkedData().getContentCopies(), DTOTranslator::translate, new ArrayList<>()));
            if (dataSet.getLinkedData().getFetchOptions().hasExternalDms())
            {
                linkData.setExternalDataManagementSystem(translate(dataSet.getLinkedData().getExternalDms()));
            }
            externalData = linkData;
        } else
        {
            ContainerDataSet containerData = new ContainerDataSet();
            if (dataSet.getFetchOptions().hasComponents())
            {
                containerData.setContainedDataSets(translate(dataSet.getComponents(), DTOTranslator::translate, new ArrayList<>()));
            }
            externalData = containerData;
        }

        externalData.setCode(dataSet.getCode());
        externalData.setDataProducerCode(dataSet.getDataProducer());
        externalData.setDerived(Boolean.FALSE.equals(dataSet.isMeasured()));
        externalData.setRegistrationDate(dataSet.getRegistrationDate());
        externalData.setModificationDate(dataSet.getModificationDate());
        externalData.setAccessTimestamp(dataSet.getAccessDate());
        externalData.setProductionDate(dataSet.getDataProductionDate());
        externalData.setPostRegistered(dataSet.isPostRegistered());
        externalData.setMetaData(dataSet.getMetaData());

        if (dataSet.getFetchOptions().hasType())
        {
            externalData.setDataSetType(translate(dataSet.getType()));
        }
        if (dataSet.getFetchOptions().hasSample())
        {
            externalData.setSample(translate(dataSet.getSample()));
        }
        if (dataSet.getFetchOptions().hasExperiment())
        {
            externalData.setExperiment(translate(dataSet.getExperiment()));
        }
        if (dataSet.getFetchOptions().hasDataStore())
        {
            externalData.setDataStore(translate(dataSet.getDataStore()));
        }
        if (dataSet.getFetchOptions().hasProperties())
        {
            externalData.setDataSetProperties(translate(dataSet.getType(), dataSet));
        }
        if (dataSet.getFetchOptions().hasRegistrator())
        {
            externalData.setRegistrator(translate(dataSet.getRegistrator()));
        }
        if (dataSet.getFetchOptions().hasModifier())
        {
            externalData.setModifier(translate(dataSet.getModifier()));
        }
        if (dataSet.getFetchOptions().hasTags())
        {
            externalData.setMetaprojects(translate(dataSet.getTags(), DTOTranslator::translate, new ArrayList<>()));
        }
        if (dataSet.getFetchOptions().hasParents())
        {
            externalData.setParents(translate(dataSet.getParents(), DTOTranslator::translate, new ArrayList<>()));
        }
        if (dataSet.getFetchOptions().hasChildren())
        {
            externalData.setChildren(translate(dataSet.getChildren(), DTOTranslator::translate, new ArrayList<>()));
        }
        if (dataSet.getFetchOptions().hasContainers())
        {
            if (dataSet.getContainers() != null)
            {
                int index = 0;
                for (ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet container : dataSet.getContainers())
                {
                    externalData.addContainer((ContainerDataSet) translate(container), index++);
                }
            }
        }

        return externalData;
    }

    public static SimpleDataSetInformationDTO translateToSimpleDataSet(DataSet dataSet)
    {
        SimpleDataSetInformationDTO simpleDTO = new SimpleDataSetInformationDTO();
        simpleDTO.setDataSetCode(dataSet.getCode());
        simpleDTO.setDataSetType(dataSet.getType().getCode());
        simpleDTO.setDataStoreCode(dataSet.getDataStore().getCode());
        simpleDTO.setDataSetShareId(dataSet.getPhysicalData().getShareId());
        simpleDTO.setDataSetLocation(dataSet.getPhysicalData().getLocation());
        simpleDTO.setDataSetSize(dataSet.getPhysicalData().getSize());
        simpleDTO.setStatus(DataSetArchivingStatus.valueOf(dataSet.getPhysicalData().getStatus().name()));
        simpleDTO.setPresentInArchive(dataSet.getPhysicalData().isPresentInArchive());
        simpleDTO.setSpeedHint(dataSet.getPhysicalData().getSpeedHint());
        simpleDTO.setStorageConfirmed(dataSet.getPhysicalData().isStorageConfirmation());
        simpleDTO.setRegistrationTimestamp(dataSet.getRegistrationDate());
        simpleDTO.setModificationTimestamp(dataSet.getModificationDate());
        simpleDTO.setAccessTimestamp(dataSet.getAccessDate());

        if (dataSet.getExperiment() != null)
        {
            simpleDTO.setExperimentCode(dataSet.getExperiment().getCode());
            simpleDTO.setProjectCode(dataSet.getExperiment().getProject().getCode());
            simpleDTO.setSpaceCode(dataSet.getExperiment().getProject().getSpace().getCode());
            if (dataSet.getExperiment().getImmutableDataDate() != null)
            {
                simpleDTO.setImmutableDataTimestamp(dataSet.getExperiment().getImmutableDataDate());
            }
        }

        if (dataSet.getSample() != null)
        {
            simpleDTO.setSampleCode(dataSet.getSample().getCode());
            if (dataSet.getSample().getImmutableDataDate() != null)
            {
                simpleDTO.setImmutableDataTimestamp(dataSet.getSample().getImmutableDataDate());
            }
        }

        return simpleDTO;
    }

    public static DatasetDescription translateToDescription(DataSet dataSet)
    {
        DatasetDescription description = new DatasetDescription();
        description.setDataSetCode(dataSet.getCode());
        description.setMainDataSetPath(dataSet.getType().getMainDataSetPath());
        description.setMainDataSetPattern(dataSet.getType().getMainDataSetPattern());
        description.setDatasetTypeCode(dataSet.getType().getCode());
        description.setDataStoreCode(dataSet.getDataStore().getCode());
        description.setRegistrationTimestamp(dataSet.getRegistrationDate());

        if (dataSet.getPhysicalData() != null)
        {
            description.setDataSetLocation(dataSet.getPhysicalData().getLocation());
            description.setDataSetShareId(dataSet.getPhysicalData().getShareId());
            description.setDataSetSize(dataSet.getPhysicalData().getSize());
            description.setSpeedHint(dataSet.getPhysicalData().getSpeedHint());
            description.setFileFormatType(dataSet.getPhysicalData().getFileFormatType().getCode());
            description.setStorageConfirmed(dataSet.getPhysicalData().isStorageConfirmation());
            description.setH5Folders(dataSet.getPhysicalData().isH5Folders());
            description.setH5arFolders(dataSet.getPhysicalData().isH5arFolders());
        }

        if (dataSet.getSample() != null)
        {
            description.setSampleCode(dataSet.getSample().getCode());
            description.setSampleIdentifier(dataSet.getSample().getIdentifier().getIdentifier());
            description.setSampleTypeCode(dataSet.getSample().getType().getCode());
            if (dataSet.getSample().getProject() != null)
            {
                description.setProjectCode(dataSet.getSample().getProject().getCode());
            }
            description.setSpaceCode(dataSet.getSample().getSpace().getCode());
        }

        if (dataSet.getExperiment() != null)
        {
            description.setExperimentIdentifier(dataSet.getExperiment().getIdentifier().getIdentifier());
            description.setExperimentTypeCode(dataSet.getExperiment().getType().getCode());
            description.setExperimentCode(dataSet.getExperiment().getCode());
            description.setProjectCode(dataSet.getExperiment().getProject().getCode());
            description.setSpaceCode(dataSet.getExperiment().getProject().getSpace().getCode());
        }

        int ordinal = 0;
        for (DataSet container : dataSet.getContainers())
        {
            description.addOrderInContainer(container.getCode(), ordinal++);
        }

        return description;
    }

    public static Sample translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample sample)
    {
        if (sample == null)
        {
            return null;
        }

        Sample result = new Sample();
        if (sample.getContainer() != null)
        {
            result.setCode(sample.getContainer().getCode() + ":" + sample.getContainer());
        } else
        {
            result.setCode(sample.getCode());
        }
        result.setSubCode(sample.getCode());
        result.setPermId(sample.getPermId().getPermId());
        result.setIdentifier(sample.getIdentifier().getIdentifier());
        result.setRegistrationDate(sample.getRegistrationDate());
        result.setModificationDate(sample.getModificationDate());
        result.setMetaData(sample.getMetaData());

        if (sample.getFetchOptions().hasType())
        {
            result.setSampleType(translate(sample.getType()));
        }
        if (sample.getFetchOptions().hasSpace())
        {
            result.setSpace(translate(sample.getSpace()));
        }
        if (sample.getFetchOptions().hasProject())
        {
            result.setProject(translate(sample.getProject()));
        }
        if (sample.getFetchOptions().hasExperiment())
        {
            result.setExperiment(translate(sample.getExperiment()));
        }
        if (sample.getFetchOptions().hasRegistrator())
        {
            result.setRegistrator(translate(sample.getRegistrator()));
        }
        if (sample.getFetchOptions().hasModifier())
        {
            result.setModifier(translate(sample.getModifier()));
        }
        if (sample.getFetchOptions().hasContainer())
        {
            result.setContainer(translate(sample.getContainer()));
        }
        if (sample.getFetchOptions().hasProperties())
        {
            result.setProperties(translate(sample.getType(), sample));
        }
        if (sample.getFetchOptions().hasComponents())
        {
            result.setContainedSample(translate(sample.getComponents(), DTOTranslator::translate, new ArrayList<>()));
        }
        if (sample.getFetchOptions().hasParents())
        {
            result.setParents(translate(sample.getParents(), DTOTranslator::translate, new HashSet<>()));
        }
        if (sample.getFetchOptions().hasTags())
        {
            result.setMetaprojects(translate(sample.getTags(), DTOTranslator::translate, new ArrayList<>()));
        }

        return result;
    }

    public static Experiment translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment experiment)
    {
        if (experiment == null)
        {
            return null;
        }

        Experiment result = new Experiment();
        result.setCode(experiment.getCode());
        result.setPermId(experiment.getPermId().getPermId());
        result.setIdentifier(experiment.getIdentifier().getIdentifier());
        result.setRegistrationDate(experiment.getRegistrationDate());
        result.setModificationDate(experiment.getModificationDate());
        result.setMetaData(experiment.getMetaData());

        if (experiment.getFetchOptions().hasType())
        {
            result.setExperimentType(translate(experiment.getType()));
        }
        if (experiment.getFetchOptions().hasProject())
        {
            result.setProject(translate(experiment.getProject()));
        }
        if (experiment.getFetchOptions().hasRegistrator())
        {
            result.setRegistrator(translate(experiment.getRegistrator()));
        }
        if (experiment.getFetchOptions().hasModifier())
        {
            result.setModifier(translate(experiment.getModifier()));
        }
        if (experiment.getFetchOptions().hasProperties())
        {
            result.setProperties(translate(experiment.getType(), experiment));
        }
        if (experiment.getFetchOptions().hasTags())
        {
            result.setMetaprojects(translate(experiment.getTags(), DTOTranslator::translate, new ArrayList<>()));
        }

        return result;
    }

    public static Project translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project project)
    {
        if (project == null)
        {
            return null;
        }

        Project result = new Project();
        result.setCode(project.getCode());
        result.setPermId(project.getPermId().getPermId());
        result.setIdentifier(project.getIdentifier().getIdentifier());
        result.setDescription(project.getDescription());
        result.setRegistrationDate(project.getRegistrationDate());
        result.setModificationDate(project.getModificationDate());

        if (project.getFetchOptions().hasSpace())
        {
            result.setSpace(translate(project.getSpace()));
        }
        if (project.getFetchOptions().hasLeader())
        {
            result.setProjectLeader(translate(project.getLeader()));
        }
        if (project.getFetchOptions().hasRegistrator())
        {
            result.setRegistrator(translate(project.getRegistrator()));
        }
        if (project.getFetchOptions().hasModifier())
        {
            result.setModifier(translate(project.getModifier()));
        }

        return result;
    }

    public static Space translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space space)
    {
        if (space == null)
        {
            return null;
        }

        Space result = new Space();
        result.setCode(space.getCode());
        result.setIdentifier(new SpaceIdentifier(space.getCode()).toString());
        result.setDescription(space.getDescription());
        result.setRegistrationDate(space.getRegistrationDate());
        result.setModificationDate(space.getModificationDate());

        if (space.getFetchOptions().hasRegistrator())
        {
            result.setRegistrator(translate(space.getRegistrator()));
        }

        return result;
    }

    public static SampleType translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType type)
    {
        if (type == null)
        {
            return null;
        }

        SampleType result = new SampleType();
        result.setCode(type.getCode());
        result.setDescription(type.getDescription());
        result.setListable(type.isListable());
        result.setSubcodeUnique(type.isSubcodeUnique());
        result.setAutoGeneratedCode(type.isAutoGeneratedCode());
        result.setShowContainer(type.isShowContainer());
        result.setShowParents(type.isShowParents());
        result.setShowParentMetadata(type.isShowParentMetadata());
        result.setGeneratedCodePrefix(type.getGeneratedCodePrefix());
        result.setModificationDate(type.getModificationDate());
        result.setMetaData(type.getMetaData());
        result.setManagedInternally(type.isManagedInternally());
        return result;
    }

    public static ExperimentType translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType type)
    {
        if (type == null)
        {
            return null;
        }

        ExperimentType result = new ExperimentType();
        result.setCode(type.getCode());
        result.setDescription(type.getDescription());
        result.setModificationDate(type.getModificationDate());
        result.setMetaData(type.getMetaData());
        result.setManagedInternally(type.isManagedInternally());
        return result;
    }

    public static DataSetType translate(ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType type)
    {
        if (type == null)
        {
            return null;
        }

        DataSetType result = new DataSetType();
        result.setCode(type.getCode());
        result.setDescription(type.getDescription());
        result.setMainDataSetPath(type.getMainDataSetPath());
        result.setMainDataSetPattern(type.getMainDataSetPattern());
        result.setDeletionDisallow(type.isDisallowDeletion());
        result.setModificationDate(type.getModificationDate());
        result.setMetaData(type.getMetaData());
        result.setManagedInternally(type.isManagedInternally());
        return result;
    }

    public static Person translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person person)
    {
        if (person == null)
        {
            return null;
        }

        Person result = new Person();
        result.setUserId(person.getUserId());
        result.setFirstName(person.getFirstName());
        result.setLastName(person.getLastName());
        result.setEmail(person.getEmail());
        result.setRegistrationDate(person.getRegistrationDate());
        result.setActive(person.isActive());

        if (person.getFetchOptions().hasRegistrator())
        {
            result.setRegistrator(translate(person.getRegistrator()));
        }

        return result;
    }

    public static DataStore translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore dataStore)
    {
        if (dataStore == null)
        {
            return null;
        }

        DataStore result = new DataStore();
        result.setCode(dataStore.getCode());
        result.setDownloadUrl(dataStore.getDownloadUrl());
        return result;
    }

    public static List<IEntityProperty> translate(final IEntityType entityType, final AbstractEntity<?> entity)
    {
        List<IEntityProperty> result = new ArrayList<>();

        for (PropertyAssignment propertyAssignment : entityType.getPropertyAssignments())
        {
            ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType propertyType = propertyAssignment.getPropertyType();
            Serializable propertyValue = entity.getProperty(propertyType.getCode());

            AbstractEntityProperty property;

            switch (propertyType.getDataType())
            {
                case CONTROLLEDVOCABULARY:
                    property = new VocabularyTermEntityProperty();
                    if (propertyValue instanceof String)
                    {
                        VocabularyTerm term = new VocabularyTerm();
                        term.setCode((String) propertyValue);
                        property.setVocabularyTerm(term);
                    }
                    break;
                case MATERIAL:
                    property = new MaterialEntityProperty();
                    if (propertyValue instanceof String)
                    {
                        MaterialIdentifier identifier = MaterialIdentifier.tryParseIdentifier((String) propertyValue);
                        if (identifier != null)
                        {
                            MaterialType materialType = new MaterialType();
                            materialType.setCode(identifier.getTypeCode());
                            Material material = new Material();
                            material.setCode(identifier.getCode());
                            material.setMaterialType(materialType);
                            property.setMaterial(material);
                        }
                    }
                    break;
                case SAMPLE:
                    property = new SampleEntityProperty();
                    if (propertyValue instanceof String)
                    {
                        Sample sample = new Sample();
                        sample.setPermId((String) propertyValue);
                        property.setSample(sample);
                    }
                    break;
                default:
                    property = new GenericEntityProperty();
            }

            property.setPropertyType(translate(propertyType));
            property.setOrdinal(Long.valueOf(propertyAssignment.getOrdinal()));
            property.setValue(propertyValue);

            result.add(property);
        }

        return result;
    }

    public static PropertyType translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType propertyType)
    {
        if (propertyType == null)
        {
            return null;
        }

        PropertyType result = new PropertyType();
        result.setCode(propertyType.getCode());
        result.setLabel(propertyType.getLabel());
        result.setDescription(propertyType.getDescription());
        result.setDataType(translate(propertyType.getDataType()));
        result.setMultiValue(propertyType.isMultiValue());
        result.setInternalNamespace(propertyType.isInternalNameSpace());
        result.setManagedInternally(propertyType.isManagedInternally());
        result.setSchema(propertyType.getSchema());
        result.setTransformation(propertyType.getTransformation());

        if (propertyType.getFetchOptions().hasRegistrator())
        {
            result.setRegistrator(translate(propertyType.getRegistrator()));
        }

        return result;
    }

    public static LocatorType translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.LocatorType locatorType)
    {
        if (locatorType == null)
        {
            return null;
        }
        LocatorType result = new LocatorType(locatorType.getCode());
        result.setDescription(locatorType.getDescription());
        return result;
    }

    public static FileFormatType translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.FileFormatType fileFormatType)
    {
        if (fileFormatType == null)
        {
            return null;
        }
        FileFormatType result = new FileFormatType(fileFormatType.getCode());
        result.setDescription(fileFormatType.getDescription());
        return result;
    }

    public static DataSetArchivingStatus translate(final ArchivingStatus status)
    {
        if (status == null)
        {
            return null;
        }
        return DataSetArchivingStatus.valueOf(status.name());
    }

    public static DataType translate(final ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType dataType)
    {
        if (dataType == null)
        {
            return null;
        }

        DataType result = new DataType();
        result.setCode(DataTypeCode.valueOf(dataType.name()));
        return result;
    }

    public static Boolean translate(final Complete complete)
    {
        if (complete == null)
        {
            return null;
        }
        switch (complete)
        {
            case UNKNOWN:
                return null;
            case YES:
                return true;
            case NO:
                return false;
            default:
                throw new IllegalArgumentException(complete.name());
        }
    }

    public static IContentCopy translate(final ContentCopy contentCopy)
    {
        if (contentCopy == null)
        {
            return null;
        }

        ExternalDms externalDms = contentCopy.getExternalDms();

        if (ExternalDmsAddressType.FILE_SYSTEM.equals(externalDms.getAddressType()))
        {
            String[] addressParts = externalDms.getAddress().split(":");
            return new FileSystemContentCopy(externalDms.getCode(), externalDms.getLabel(),
                    addressParts[0], addressParts[1], contentCopy.getPath(), contentCopy.getGitCommitHash(), contentCopy.getGitRepositoryId());
        } else
        {
            String url = externalDms.getAddress().replaceAll(Pattern.quote("${") + ".*" + Pattern.quote("}"), contentCopy.getExternalCode());
            return new UrlContentCopy(externalDms.getCode(), externalDms.getLabel(), url, contentCopy.getExternalCode());
        }
    }

    public static ExternalDataManagementSystem translate(final ExternalDms externalDms)
    {
        if (externalDms == null)
        {
            return null;
        }

        ExternalDataManagementSystem result = new ExternalDataManagementSystem();
        result.setCode(externalDms.getCode());
        result.setLabel(externalDms.getLabel());
        result.setUrlTemplate(externalDms.getUrlTemplate());
        result.setAddress(externalDms.getAddress());
        if (externalDms.getAddressType() != null)
        {
            result.setAddressType(ExternalDataManagementSystemType.valueOf(externalDms.getAddressType().name()));
        }
        result.setOpenBIS(externalDms.isOpenbis());
        return result;
    }

    public static Metaproject translate(final Tag tag)
    {
        if (tag == null)
        {
            return null;
        }

        Metaproject result = new Metaproject();
        result.setName(tag.getCode());
        result.setIdentifier(tag.getPermId().getPermId());
        result.setOwnerId(tag.getOwner().getUserId());
        result.setDescription(tag.getDescription());
        result.setPrivate(tag.isPrivate());
        result.setCreationDate(tag.getRegistrationDate());
        return result;
    }

    public static <C extends Collection<O>, I, O> C translate(Collection<I> input, Function<I, O> convert, C output)
    {
        if (input == null)
        {
            return output;
        }

        for (I item : input)
        {
            output.add(convert.apply(item));
        }

        return output;
    }

}
