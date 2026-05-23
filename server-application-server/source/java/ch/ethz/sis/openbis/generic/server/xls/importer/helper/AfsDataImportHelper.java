package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.ExperimentIdentifier;

import java.nio.file.Path;
import java.util.*;

public final class AfsDataImportHelper
{
    private final String sessionToken;
    private AfsClientImportProxy afs;
    private final ImportModes importModes;
    private final ImportOptions options;
    private final IApplicationServerApi v3;


    private static final String DATA = "/data";
    private static final String HIERARCHY  = "hierarchy/";

    public AfsDataImportHelper(String sessionToken, ImportModes importModes, ImportOptions options,
            IApplicationServerApi applicationServerApi, AfsClient afsClient)
    {
        this.sessionToken = sessionToken;
        this.importModes = importModes;
        this.options = options;
        this.afs = AfsClientImportProxy.getAfsClient(sessionToken, applicationServerApi, afsClient);
        this.v3 = applicationServerApi;
    }

    private void validateSampleFrozen(Sample s) {
        boolean isFrozen = s.getImmutableDataDate() != null;
        if(isFrozen) {
            throw new UserFailureException(String.format("Object '%s' is frozen for modifications!", s.getIdentifier().getIdentifier()));
        }
    }

    public void importData(java.io.File baseDir, String filePath) throws Exception
    {
        String fullFilePath = Path.of(baseDir.toPath().toAbsolutePath().toString(), filePath).toString();
        String entityPath = filePath.substring(HIERARCHY.length(), filePath.indexOf(DATA));
        String[] entities = entityPath.split("/");
        if(entities.length == 2) {
            //space sample
            String spaceCode = entities[0];
            String sampleCode = getCode(entities[1]);
            Sample sample = getSample(spaceCode, null, null, sampleCode);
            if(sample != null) {
                validateSampleFrozen(sample);
                importFiles(sample.getPermId().getPermId(), Path.of(fullFilePath));
            } else {
                throw new UserFailureException(String.format("No entity has been found for path '%s'", entityPath));
            }
        } else if(entities.length == 3) {
            //project sample or project experiment
            String spaceCode = entities[0];
            String projectCode = entities[1];
            String entityCode = getCode(entities[2]);
            Sample sample = getSample(spaceCode, projectCode, null, entityCode);
            Experiment experiment = getCollection(spaceCode, projectCode, entityCode);
            if(sample != null && experiment != null) {
                throw new UserFailureException(String.format("Ambiguous identifier '%s' - there is an object and collection with such identifier!", sample.getIdentifier().getIdentifier()));
            }
            if(sample != null) {
                validateSampleFrozen(sample);
                importFiles(sample.getPermId().getPermId(), Path.of(fullFilePath));
            } else {
                if(experiment != null) {
                    boolean isFrozen = experiment.getImmutableDataDate() != null;
                    if(isFrozen) {
                        throw new UserFailureException(String.format("Collection '%s' is frozen for modifications!", experiment.getIdentifier().getIdentifier()));
                    }
                    importFiles(experiment.getPermId().getPermId(), Path.of(fullFilePath));
                } else {
                    throw new UserFailureException(String.format("No entity has been found for path '%s'", entityPath));
                }
            }
        } else if(entities.length == 4) {
            // experiment sample
            String spaceCode = entities[0];
            String projectCode = entities[1];
            String collectionCode = getCode(entities[2]);
            String sampleCode = getCode(entities[3]);
            Sample sample = getSample(spaceCode, projectCode, collectionCode, sampleCode);
            if(sample != null) {
                validateSampleFrozen(sample);
                importFiles(sample.getPermId().getPermId(), Path.of(fullFilePath));
            } else {
                throw new UserFailureException(String.format(String.format("No entity has been found for path '%s'", entityPath)));
            }
        }

    }

    private void importFiles(String permId, Path path) throws Exception
    {
        try {
            afs.listFilesBase(permId, "", false);
        } catch (Exception e)
        {
            if(e.toString().contains("NoSuchFileException")) {
                //workaround for AFS not creating structure fast enough
                afs.createDirectory(permId, "/");
            } else {
                throw e;
            }

        }
        afs.uploadFiles(path, permId, Path.of("/"), this.importModes);
    }


    public boolean isAfsConnectionAvailable() {
        return afs.isSessionValid();
    }


    private String getCode(String text) {
        if(text.contains("(")) {
            return text.substring(text.indexOf("(")+1, text.indexOf(")"));
        }
        return text;
    }

    private Sample getSample(String space, String project, String experiment, String sample) {
        SampleSearchCriteria searchCriteria = new SampleSearchCriteria();
        if(space != null) {
            searchCriteria.withSpace().withCode().thatEquals(space);
        }
        if(project != null) {
            searchCriteria.withProject().withCode().thatEquals(project);
        }
        if(experiment != null) {
            searchCriteria.withExperiment().withCode().thatEquals(experiment);
        }
        searchCriteria.withCode().thatEquals(sample);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        List<Sample> samples = v3.searchSamples(sessionToken, searchCriteria, fetchOptions).getObjects();
        if(samples.isEmpty()) {
            return null;
        } else {
            return samples.get(0);
        }
    }

    private Experiment getCollection(String space, String project, String experiment) {
        ExperimentIdentifier id = new ExperimentIdentifier(String.format("/%s/%s/%s", space, project, experiment));

        ExperimentSearchCriteria searchCriteria = new ExperimentSearchCriteria();
        searchCriteria.withIdentifier().thatEquals(id.getIdentifier());

        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        List<Experiment> experiments = v3.searchExperiments(sessionToken, searchCriteria, fetchOptions).getObjects();
        if(experiments.isEmpty()) {
            return null;
        } else {
            return experiments.get(0);
        }
    }



}
