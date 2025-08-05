import re
from ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions import ExperimentFetchOptions
from ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id import ExperimentIdentifier
from ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions import SampleFetchOptions
from ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id import SampleIdentifier
from ch.systemsx.cisd.common.mail import EMailAddress
from ch.systemsx.cisd.openbis.dss.generic.shared import ServiceProvider
from ch.systemsx.cisd.openbis.generic.client.web.client.exception import UserFailureException
from java.util.concurrent import ConcurrentHashMap
from java.io import File
from java.nio.file import Files, Paths, StandardCopyOption
from java.util import List, Map
from java.util.concurrent import ConcurrentHashMap
from java.util.regex import Pattern, PatternSyntaxException
from org.apache.commons.io import FileUtils
from org.json import JSONObject
from ch.ethz.sis import PersistentKeyValueStore

INVALID_FORMAT_ERROR_MESSAGE = "Invalid format for the folder name, should follow the pattern <ENTITY_KIND>+<SPACE_CODE>+<PROJECT_CODE>+[<EXPERIMENT_CODE>|<SAMPLE_CODE>]+<OPTIONAL_DATASET_TYPE>+<OPTIONAL_NAME>";
FAILED_TO_PARSE_ERROR_MESSAGE = "Failed to parse folder name";
FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE = "Failed to parse sample";
FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE = "Failed to parse experiment";
FOLDER_CONTAINS_NON_DELETABLE_FILES_ERROR_MESSAGE = "Folder contains non-deletable files";
SAMPLE_MISSING_ERROR_MESSAGE = "Sample not found";
EXPERIMENT_MISSING_ERROR_MESSAGE = "Experiment not found";
NAME_PROPERTY_SET_IN_TWO_PLACES_ERROR_MESSAGE = "NAME property specified twice, it should just be in either folder name or metadata.json"
EMAIL_SUBJECT = "ELN LIMS Dropbox Error";
ILLEGAL_FILES_ERROR_MESSAGE = "Directory contains illegal files";
INVALID_PATTERN_ERROR_MESSAGE = "Provided pattern could not be compiled"

errorMessages = []

def process(transaction):
    incoming = transaction.getIncoming();
    folderName = substring_up_to_hash(incoming.getName());
    emailAddress = None
    discardFilesPatternsString = getConfigurationProperty(transaction, 'discard-files-patterns')
    illegalFilesPatternsString = getConfigurationProperty(transaction, 'illegal-files-patterns')

    try:
        deleteFilesMatchingPatterns(incoming, discardFilesPatternsString)
        validateIllegalFilesMatchingPatterns(incoming, illegalFilesPatternsString)

        if not folderName.startswith('.'):
            datasetInfo = folderName.split("+");
            entityKind = None;
            sample = None;
            experiment = None;
            datasetType = None;
            name = None;

            # Parse entity Kind
            if len(datasetInfo) >= 1:
                entityKind = datasetInfo[0];
            else:
                raise UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_ERROR_MESSAGE);

            v3 = ServiceProvider.getV3ApplicationService();
            sessionToken = transaction.getOpenBisServiceSessionToken();
            projectSamplesEnabled = v3.getServerInformation(sessionToken)['project-samples-enabled'] == 'true'

            # Parse entity Kind Format
            if entityKind == "S":
                if len(datasetInfo) >= 3:
                    sampleSpace = datasetInfo[1];
                    sampleCode = datasetInfo[2];

                    emailAddress = getSampleRegistratorsEmail(transaction, sampleSpace, None, sampleCode)
                    sample = transaction.getSample("/" + sampleSpace + "/" + sampleCode);
                    if sample is None:
                        reportIssue(INVALID_FORMAT_ERROR_MESSAGE + ":" + SAMPLE_MISSING_ERROR_MESSAGE)
                        raise UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + SAMPLE_MISSING_ERROR_MESSAGE)
                    if len(datasetInfo) >= 4:
                        datasetType = datasetInfo[3];
                    if len(datasetInfo) >= 5:
                        name = datasetInfo[4];
                    if len(datasetInfo) > 5:
                        reportIssue(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE)
                else:
                    raise UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE);
            if entityKind == "O":
                if len(datasetInfo) >= 4 and projectSamplesEnabled:
                    sampleSpace = datasetInfo[1];
                    projectCode = datasetInfo[2];
                    sampleCode = datasetInfo[3];

                    emailAddress = getSampleRegistratorsEmail(transaction, sampleSpace, projectCode, sampleCode)
                    sample = transaction.getSample("/" + sampleSpace + "/" + projectCode + "/" + sampleCode);
                    if sample is None:
                        reportIssue(INVALID_FORMAT_ERROR_MESSAGE + ":" + SAMPLE_MISSING_ERROR_MESSAGE)
                        raise UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + SAMPLE_MISSING_ERROR_MESSAGE)
                    if len(datasetInfo) >= 5:
                        datasetType = datasetInfo[4];
                    if len(datasetInfo) >= 6:
                        name = datasetInfo[5];
                    if len(datasetInfo) > 6:
                        reportIssue(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE)
                else:
                    raise UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE);

                readOnlyFiles = getReadOnlyFiles(incoming)
                if readOnlyFiles:
                    reportIssue(FOLDER_CONTAINS_NON_DELETABLE_FILES_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_SAMPLE_ERROR_MESSAGE + ":\n" + pathListToStr(readOnlyFiles));
            if entityKind == "E":
                if len(datasetInfo) >= 4:
                    experimentSpace = datasetInfo[1];
                    projectCode = datasetInfo[2];
                    experimentCode = datasetInfo[3];

                    emailAddress = getExperimentRegistratorsEmail(transaction, experimentSpace, projectCode,
                                                                  experimentCode);
                    experiment = transaction.getExperiment("/" + experimentSpace + "/" + projectCode + "/" + experimentCode);
                    if experiment is None:
                        reportIssue(INVALID_FORMAT_ERROR_MESSAGE + ":" + EXPERIMENT_MISSING_ERROR_MESSAGE)
                        raise UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + EXPERIMENT_MISSING_ERROR_MESSAGE)
                    if len(datasetInfo) >= 5:
                        datasetType = datasetInfo[4];
                    if len(datasetInfo) >= 6:
                        name = datasetInfo[5];
                    if len(datasetInfo) > 6:
                        reportIssue(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE);
                else:
                    raise UserFailureException(INVALID_FORMAT_ERROR_MESSAGE + ":" + FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE);

                readOnlyFiles = getReadOnlyFiles(incoming)
                if readOnlyFiles:
                    reportIssue(FOLDER_CONTAINS_NON_DELETABLE_FILES_ERROR_MESSAGE + ":"
                                + FAILED_TO_PARSE_EXPERIMENT_ERROR_MESSAGE + ":\n" + pathListToStr(readOnlyFiles))

            # Create dataset
            dataSet = None;
            if datasetType is not None:  # Set type if found
                dataSet = transaction.createNewDataSet(datasetType);
            else:
                dataSet = transaction.createNewDataSet();

            if name is not None:
                dataSet.setPropertyValue("NAME", name);  # Set name if found

            # Set sample or experiment
            if sample is not None:
                dataSet.setSample(sample);
            else:
                dataSet.setExperiment(experiment);

            # Move folder to dataset
            filesInFolder = incoming.listFiles();

            itemsInFolder = 0;
            datasetItem = None;
            for item in filesInFolder:
                fileName = item.getName()
                if fileName == "metadata.json":
                    root = JSONObject(FileUtils.readFileToString(item, "UTF-8"))
                    properties = root.get("properties")
                    for propertyKey in properties.keys():
                        if propertyKey == "NAME" and name is not None:
                            raise UserFailureException(NAME_PROPERTY_SET_IN_TWO_PLACES_ERROR_MESSAGE)
                        propertyValue = properties.get(propertyKey)
                        if propertyValue is not None:
                            propertyValueString = unicode(propertyValue)
                            dataSet.setPropertyValue(propertyKey, propertyValueString)
                else:
                    itemsInFolder = itemsInFolder + 1;
                    datasetItem = item;

            if itemsInFolder > 1:
                tmpPath = incoming.getAbsolutePath() + "/default";
                tmpDir = File(tmpPath);
                tmpDir.mkdir();

                try:
                    for inputFile in filesInFolder:
                        Files.move(inputFile.toPath(), Paths.get(tmpPath, inputFile.getName()),
                                    StandardCopyOption.ATOMIC_MOVE);
                    transaction.moveFile(tmpDir.getAbsolutePath(), dataSet);
                finally:
                    if tmpDir is not None:
                        tmpDir.delete();
            else:
                transaction.moveFile(datasetItem.getAbsolutePath(), dataSet);
    finally:
        reportAllIssues(transaction, emailAddress)


def substring_up_to_hash(input_string):
    hash_index = input_string.rfind('#')
    return input_string[:hash_index] if hash_index >=0 else input_string


def pathListToStr(list):
    return "\n".join(list)


def getContactsEmailAddresses(transaction):
    emailString = getConfigurationProperty(transaction, "mail.addresses.dropbox-errors")
    return re.split("[,;]", emailString) if emailString is not None else []


def reportIssue(errorMessage):
    errorMessages.append(errorMessage)


def reportAllIssues(transaction, emailAddress):
    if len(errorMessages) > 0:
        contacts = getContactsEmailAddresses(transaction)
        allAddresses = [emailAddress] + contacts if emailAddress is not None else contacts
        joinedErrorMessages = "\n".join(errorMessages)
        sendMail(transaction, map(lambda address: EMailAddress(address), allAddresses), EMAIL_SUBJECT, joinedErrorMessages);
        raise UserFailureException(joinedErrorMessages)


def getReadOnlyFiles(folder):
    result = []
    if not folder.renameTo(folder):
        result.append(folder.getPath())

    files = folder.listFiles()
    if files is not None:
        for f in files:
            result.extend(getReadOnlyFiles(f))

    return result


def sendMail(transaction, emailAddresses, subject, body):
    transaction.getGlobalState().getMailClient().sendEmailMessage(subject, body, None, None, emailAddresses);


def getSampleRegistratorsEmail(transaction, spaceCode, projectCode, sampleCode):
    v3 = ServiceProvider.getV3ApplicationService();
    sampleIdentifier = SampleIdentifier(spaceCode, projectCode, None, sampleCode);
    fetchOptions = SampleFetchOptions();
    fetchOptions.withRegistrator();
    foundSample = v3.getSamples(transaction.getOpenBisServiceSessionToken(), List.of(sampleIdentifier), fetchOptions)\
        .get(sampleIdentifier)
    return foundSample.getRegistrator().getEmail() if foundSample is not None else None


def getExperimentRegistratorsEmail(transaction, spaceCode, projectCode, experimentCode):
    v3 = ServiceProvider.getV3ApplicationService();
    experimentIdentifier = ExperimentIdentifier(spaceCode, projectCode, experimentCode);
    fetchOptions = ExperimentFetchOptions();
    fetchOptions.withRegistrator();
    foundExperiment = v3.getExperiments(transaction.getOpenBisServiceSessionToken(), List.of(experimentIdentifier),
                                        fetchOptions).get(experimentIdentifier)
    return foundExperiment.getRegistrator().getEmail() if foundExperiment is not None else None


def getConfigurationProperty(transaction, propertyName):
    threadProperties = transaction.getGlobalState().getThreadParameters().getThreadProperties();
    try:
        return threadProperties.getProperty(propertyName);
    except:
        return None


def getStringPatternMap():
    stringPatternMap = None
    if PersistentKeyValueStore.containsKey('eln-lims-dropbox-string-pattern-map'):
        stringPatternMap = PersistentKeyValueStore.get('eln-lims-dropbox-string-pattern-map')
    else:
        stringPatternMap = ConcurrentHashMap()
        PersistentKeyValueStore.put('eln-lims-dropbox-string-pattern-map', stringPatternMap)
    return stringPatternMap


def stringArrayStrip(sArray):
    sArrayStripped = []
    for s in sArray:
        sArrayStripped.append(s.strip())
    return sArrayStripped


def deleteFilesMatchingPatterns(incoming, discardFilesPatterns):
    stringToPatternMap = getStringPatternMap()
    if discardFilesPatterns:
        stringPatterns = stringArrayStrip(discardFilesPatterns.split(","))
        patterns = []
        try:
            for stringPattern in stringPatterns:
                pattern = None
                if stringToPatternMap.containsKey(stringPattern):
                    pattern = stringToPatternMap.get(stringPattern)
                else:
                    pattern = Pattern.compile(stringPattern)
                    stringToPatternMap.put(stringPattern, pattern)
                patterns.append(pattern)
        except PatternSyntaxException as err:
            reportIssue(str(err))
            raise UserFailureException(INVALID_PATTERN_ERROR_MESSAGE)
        deleteFilesMatchingPatternsExec(incoming, patterns)


def validateIllegalFilesMatchingPatterns(incoming, illegalFilesPatterns):
    stringToPatternMap = getStringPatternMap()
    if illegalFilesPatterns:
        stringPatterns = stringArrayStrip(illegalFilesPatterns.split(","))
        patterns = []
        try:
            for stringPattern in stringPatterns:
                pattern = None
                if stringToPatternMap.containsKey(stringPattern):
                    pattern = stringToPatternMap.get(stringPattern)
                else:
                    pattern = Pattern.compile(stringPattern)
                    stringToPatternMap.put(stringPattern, pattern)
                patterns.append(pattern)
        except PatternSyntaxException as err:
            reportIssue(str(err))
            raise UserFailureException(INVALID_PATTERN_ERROR_MESSAGE)
        illegalFiles = getIllegalFilesMatchingPatterns(incoming, patterns)
        if illegalFiles:
            reportIssue(ILLEGAL_FILES_ERROR_MESSAGE + ":\n" + pathListToStr(illegalFiles))


def deleteFiles(file):
    if file.isDirectory():
        for fileInDirectory in file.listFiles():
            deleteFiles(fileInDirectory)
    file.delete()


def deleteFilesMatchingPatternsExec(file, patterns):
    for pattern in patterns:
        if pattern.matcher(file.getName()).find():
            deleteFiles(file)
            return
    if file.isDirectory():
        for fileInDirectory in file.listFiles():
            deleteFilesMatchingPatternsExec(fileInDirectory, patterns)


def getIllegalFilesMatchingPatterns(file, patterns):
    result = []
    for pattern in patterns:
        if pattern.matcher(file.getName()).find():
            result.append(file.getPath())
            break
    if file.isDirectory():
        for fileInDirectory in file.listFiles():
            result.extend(getIllegalFilesMatchingPatterns(fileInDirectory, patterns))
    return result

