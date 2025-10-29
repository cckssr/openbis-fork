package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.client.web.client.dto.ExperimentIdentifier;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class AfsDataImportHelper
{
    private final String sessionToken;
    private final AfsClientImportProxy afs;
    private final ImportModes importModes;
    private final ImportOptions options;
    private final Map<SampleIdentifier, Sample> sampleCache = new HashMap<>();
    private final Map<String, File[]> sampleFiles = new HashMap<>();

    private final Map<ExperimentIdentifier, Experiment> collectionCache = new HashMap<>();
    private final Map<String, File[]> collectionFiles = new HashMap<>();

    private static final String DATA = "/data";

    public AfsDataImportHelper(String sessionToken, ImportModes importModes, ImportOptions options) {
        this.sessionToken = sessionToken;
        this.importModes = importModes;
        this.options = options;
        this.afs = AfsClientImportProxy.getAfsClient(sessionToken);
    }

    public void beginIfNotStarted() {
        this.afs.begin();
    }

    public void commit() {
        this.afs.commit();
    }

    public void rollback() {
        this.afs.rollback();
    }

    public void importData(final ZipInputStream zip, ZipEntry entry) {
        String filePath = entry.getName();

        String entityPath = filePath.substring("hierarchy/".length(), filePath.indexOf(DATA));
        String path = filePath.substring(filePath.indexOf(DATA) + DATA.length());
        Path afsPath = Path.of(path);
        String[] entities = entityPath.split("/");
        if(entities.length == 2) {
            //space sample
            String spaceCode = entities[0];
            String sampleCode = getCode(entities[1]);
            Sample sample = getSample(spaceCode, null, null, sampleCode);
            if(sample != null) {
                if(!"/".equals(path) && path.startsWith("/"))
                {
                    validateSampleFrozen(sample);
                    File[] existingFiles = sampleFiles.get(sample.getPermId().getPermId());
                    importFile(sample.getPermId().getPermId(), afsPath, existingFiles, entry, zip);
                }
            } else {
                throw new UserFailureException(String.format("No entity has been found for path '%s'", entityPath));
            }
        } else if(entities.length == 3) {
            //project sample or project experiment
            String spaceCode = entities[0];
            String projectCode = entities[1];
            String entityCode = getCode(entities[2]);
            Sample sample = getSample(spaceCode, projectCode, null, entityCode);
            if(sample != null) {
                if(!"/".equals(path) && path.startsWith("/"))
                {
                    validateSampleFrozen(sample);
                    File[] existingFiles = sampleFiles.get(sample.getPermId().getPermId());
                    importFile(sample.getPermId().getPermId(), afsPath, existingFiles, entry, zip);
                }
            } else {
                Experiment experiment = getCollection(spaceCode, projectCode, entityCode);
                if(experiment != null) {
                    if(!"/".equals(path) && path.startsWith("/"))
                    {
                        boolean isFrozen = experiment.getImmutableDataDate() != null;
                        if(isFrozen) {
                            throw new UserFailureException(String.format("Collection '%s' is frozen for modifications!", experiment.getIdentifier().getIdentifier()));
                        }
                        File[] existingFiles = collectionFiles.get(experiment.getPermId().getPermId());
                        importFile(experiment.getPermId().getPermId(), afsPath, existingFiles, entry, zip);
                    }
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
                if(!"/".equals(path) && path.startsWith("/"))
                {
                    validateSampleFrozen(sample);
                    File[] existingFiles = sampleFiles.get(sample.getPermId().getPermId());
                    importFile(sample.getPermId().getPermId(), afsPath, existingFiles, entry, zip);
                }
            } else {
                throw new UserFailureException(String.format(String.format("No entity has been found for path '%s'", entityPath)));
            }
        }

    }

    private void validateSampleFrozen(Sample s) {
        boolean isFrozen = s.getImmutableDataDate() != null;
        if(isFrozen) {
            throw new UserFailureException(String.format("Object '%s' is frozen for modifications!", s.getIdentifier().getIdentifier()));
        }
    }

    private void importFile(String permId, Path path, File[] existingFiles, ZipEntry entry, InputStream stream) {

        Optional<File> existingFile = Stream.of(existingFiles)
                .filter(x -> Path.of(x.getPath()).equals(path))
                .findAny();

        switch (this.importModes) {
            case FAIL_IF_EXISTS -> {
                if(existingFile.isPresent()) {
                    throw new UserFailureException(String.format("File '%s' is already present in '%s'", existingFile.get().getPath(), permId));
                } else {
                    if(entry.isDirectory()) {
                        File[] afsFiles = afs.listFiles(permId);
                        Optional<File> file = Stream.of(afsFiles).filter(x -> Path.of(x.getPath()).equals(path)).findAny();
                        if(file.isEmpty()){
                            afs.createDirectory(permId, path.toString());
                        }
                    } else
                    {
                        afs.upload(permId, path.toString(), entry.getSize(), stream);
                    }
                }
            }
            case IGNORE_EXISTING -> {
                if(existingFile.isEmpty()) {
                    if(entry.isDirectory()) {
                        File[] afsFiles = afs.listFiles(permId);
                        Optional<File> file = Stream.of(afsFiles).filter(x -> Path.of(x.getPath()).equals(path)).findAny();
                        if(file.isEmpty()){
                            afs.createDirectory(permId, path.toString());
                        }
                    } else
                    {
                        afs.upload(permId, path.toString(), entry.getSize(), stream);
                    }
                }
            }
            case UPDATE_IF_EXISTS -> {
                if(existingFile.isPresent() && !entry.isDirectory()) {
                    //remove file before override?
                    afs.remove(permId, path.toString());
                }
                if(entry.isDirectory()) {
                    File[] afsFiles = afs.listFiles(permId);
                    Optional<File> file = Stream.of(afsFiles).filter(x -> Path.of(x.getPath()).equals(path)).findAny();
                    if(file.isEmpty()){
                        afs.createDirectory(permId, path.toString());
                    }
                } else
                {
                    afs.upload(permId, path.toString(), entry.getSize(), stream);
                }
            }
        }

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
        SampleIdentifier id = new SampleIdentifier(space, project, sample);
        if(sampleCache.containsKey(id)) {
            return sampleCache.get(id);
        }

        IApplicationServerApi api = CommonServiceProvider.getApplicationServerApi();
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
        List<Sample> samples = api.searchSamples(this.sessionToken, searchCriteria, fetchOptions).getObjects();
        if(samples.isEmpty()) {
            sampleCache.put(id, null);
        } else {
            Sample s = samples.get(0);
            sampleCache.put(id, s);
            File[] files = afs.listFiles(s.getPermId().getPermId());
            sampleFiles.put(s.getPermId().getPermId(), files);
        }
        return sampleCache.get(id);
    }

    private Experiment getCollection(String space, String project, String experiment) {
        ExperimentIdentifier id = new ExperimentIdentifier(String.format("/%s/%s/%s", space, project, experiment));
        if(collectionCache.containsKey(id)) {
            return collectionCache.get(id);
        }

        IApplicationServerApi api = CommonServiceProvider.getApplicationServerApi();
        ExperimentSearchCriteria searchCriteria = new ExperimentSearchCriteria();
        searchCriteria.withIdentifier().thatEquals(id.getIdentifier());

        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        List<Experiment> experiments = api.searchExperiments(this.sessionToken, searchCriteria, fetchOptions).getObjects();
        if(experiments.isEmpty()) {
            collectionCache.put(id, null);
        } else {
            Experiment s = experiments.get(0);
            collectionCache.put(id, s);
            File[] files = afs.listFiles(s.getPermId().getPermId());
            collectionFiles.put(s.getPermId().getPermId(), files);
        }
        return collectionCache.get(id);
    }



}
