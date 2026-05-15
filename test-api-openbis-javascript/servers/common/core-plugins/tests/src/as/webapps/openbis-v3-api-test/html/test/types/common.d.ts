import openbis from "./openbis.esm"
import jquery from "./jquery"

export default common

export namespace common {
    interface CommonConstructor {
        new (a: any, dtos: openbis.bundle): CommonClass
    }

    interface CommonClass {
        generateId(base: string): string
        getId(entity): string
        generateId(base): string
        createSpace(facade: openbis.openbis): Promise<openbis.SpacePermId>
        createProject(facade: openbis.openbis): Promise<openbis.ProjectPermId>
        createExperiment(facade: openbis.openbis): Promise<openbis.ExperimentPermId>
        createSample(facade: openbis.openbis): Promise<openbis.SamplePermId>
        createLinkDataSet(
            facade: openbis.openbis,
            path: string,
            gitCommitHash: string,
            gitRepositoryId: string
        ): jquery.Promise<openbis.DataSetPermId>
        createDataSet(facade: openbis.openbis, dataSetType, data): Promise<openbis.DataSetPermId>
        createDataSet(facade: openbis.openbis, dataSetType): Promise<openbis.DataSetPermId>
        waitUntilEmailWith(facade: openbis.openbis, textSnippet, timeout): jquery.JQueryPromise<any>
        waitUntilIndexed(facade: openbis.openbis, dataSetCode, timeout): jquery.JQueryPromise<unknown>
        getResponseFromJSTestAggregationService(facade: openbis.openbis, params, callback): jquery.JQueryPromise<any>
        createPropertyType(facade: openbis.openbis): Promise<openbis.PropertyTypePermId>
        createPlugin(facade: openbis.openbis): Promise<openbis.PluginPermId>
        createVocabulary(facade: openbis.openbis): Promise<openbis.VocabularyPermId>
        createVocabularyTerm(facade: openbis.openbis): Promise<openbis.VocabularyTermPermId>
        createExperimentType(facade: openbis.openbis): Promise<openbis.EntityTypePermId>
        createSampleType(facade: openbis.openbis): Promise<openbis.EntityTypePermId>
        createDataSetType(facade: openbis.openbis): Promise<openbis.EntityTypePermId>
        createExternalDms(facade: openbis.openbis): Promise<openbis.ExternalDmsPermId>
        createFileExternalDms(facade: openbis.openbis): Promise<openbis.ExternalDmsPermId>
        createTag(facade: openbis.openbis): Promise<openbis.TagPermId>
        createAuthorizationGroup(facade: openbis.openbis): Promise<openbis.AuthorizationGroupPermId>
        createRoleAssignment(facade: openbis.openbis, isUser): Promise<openbis.RoleAssignmentTechId>
        createPerson(facade: openbis.openbis): Promise<openbis.PersonPermId>
        createSemanticAnnotation(facade: openbis.openbis): Promise<openbis.SemanticAnnotationPermId>
        createOperationExecution(facade: openbis.openbis): Promise<openbis.OperationExecutionPermId>
        createQuery(facade: openbis.openbis): Promise<openbis.QueryTechId>
        createPersonalAccessToken(facade: openbis.openbis): Promise<openbis.PersonalAccessTokenPermId>
        findSpace(facade: openbis.openbis, id): Promise<openbis.Space>
        findProject(facade: openbis.openbis, id): Promise<openbis.Project>
        findExperiment(facade: openbis.openbis, id): Promise<openbis.Experiment>
        findExperimentType(facade: openbis.openbis, id): Promise<openbis.ExperimentType>
        findSample(facade: openbis.openbis, id): Promise<openbis.Sample>
        findSampleType(facade: openbis.openbis, id): Promise<openbis.SampleType>
        findDataSet(facade: openbis.openbis, id): Promise<openbis.DatSet>
        findDataSetType(facade: openbis.openbis, id): Promise<openbis.DataSetType>
        findPropertyType(facade: openbis.openbis, id): Promise<openbis.PropertyType>
        findPlugin(facade: openbis.openbis, id): Promise<openbis.Plugin>
        findVocabulary(facade: openbis.openbis, id): Promise<openbis.Vocabulary>
        findVocabularyTerm(facade: openbis.openbis, id): Promise<openbis.VocabularyTerm>
        findTag(facade: openbis.openbis, id): Promise<openbis.Tag>
        findAuthorizationGroup(facade: openbis.openbis, id): Promise<openbis.AuthorizationGroup>
        findRoleAssignment(facade: openbis.openbis, id): Promise<openbis.RoleAssignment>
        findPerson(facade: openbis.openbis, id): Promise<openbis.Person>
        findSemanticAnnotation(facade: openbis.openbis, id): Promise<openbis.SemanticAnnotation>
        findExternalDms(facade: openbis.openbis, id): Promise<openbis.ExternalDms>
        findOperationExecution(facade: openbis.openbis, id): Promise<openbis.OperationExecution>
        findQuery(facade: openbis.openbis, id): Promise<openbis.Query>
        findPersonalAccessToken(facade: openbis.openbis, id): Promise<openbis.PersonalAccessToken>
        deleteSpace(facade: openbis.openbis, id): Promise<void>
        deleteProject(facade: openbis.openbis, id): Promise<void>
        deleteExperiment(facade: openbis.openbis, id): Promise<openbis.IDeletionId>
        deleteSample(facade: openbis.openbis, id): Promise<openbis.IDeletionId>
        deleteDataSet(facade: openbis.openbis, id): Promise<openbis.IDeletionId>
        deleteExternalDms(facade: openbis.openbis, id): Promise<void>
        deleteExperimentType(facade: openbis.openbis, id): Promise<void>
        deleteSampleType(facade: openbis.openbis, id): Promise<void>
        deleteDataSetType(facade: openbis.openbis, id): Promise<void>
        deletePlugin(facade: openbis.openbis, id): Promise<void>
        deletePropertyType(facade: openbis.openbis, id): Promise<void>
        deleteVocabulary(facade: openbis.openbis, id): Promise<void>
        deleteVocabularyTerm(facade: openbis.openbis, id): Promise<void>
        replaceVocabularyTerm(facade: openbis.openbis, id): Promise<void>
        deleteTag(facade: openbis.openbis, id): Promise<void>
        deleteAuthorizationGroup(facade: openbis.openbis, id): Promise<void>
        deleteRoleAssignment(facade: openbis.openbis, id): Promise<void>
        deleteOperationExecution(facade: openbis.openbis, id): Promise<void>
        deleteSemanticAnnotation(facade: openbis.openbis, id): Promise<void>
        deleteQuery(facade: openbis.openbis, id): Promise<void>
        deletePersonalAccessToken(facade: openbis.openbis, id): Promise<void>
        deletePerson(facade: openbis.openbis, id): Promise<void>
        deleteFile(facade: openbis.openbis, owner: string, source: string, trash: boolean): Promise<void>
        getObjectProperty(object: any, propertyName: string): any
        login(facade: openbis.openbis): jquery.JQueryPromise<unknown>
        createSpaceFetchOptions(): openbis.SpaceFetchOptions
        createProjectFetchOptions(): openbis.ProjectFetchOptions
        createExperimentFetchOptions(): openbis.ExperimentFetchOptions
        createExperimentTypeFetchOptions(): openbis.ExperimentTypeFetchOptions
        createSampleFetchOptions(): openbis.SampleFetchOptions
        createSampleTypeFetchOptions(): openbis.SampleTypeFetchOptions
        createDataSetFetchOptions(): openbis.DataSetFetchOptions
        createDataSetTypeFetchOptions(): openbis.DataSetTypeFetchOptions
        createPluginFetchOptions(): openbis.PluginFetchOptions
        createVocabularyFetchOptions(): openbis.VocabularyFetchOptions
        createVocabularyTermFetchOptions(): openbis.VocabularyTermFetchOptions
        createGlobalSearchObjectFetchOptions(): openbis.GlobalSearchObjectFetchOptions
        createObjectKindModificationFetchOptions(): openbis.ObjectKindModificationFetchOptions
        createTagFetchOptions(): openbis.TagFetchOptions
        createAuthorizationGroupFetchOptions(): openbis.AuthorizationGroupFetchOptions
        createRoleAssignmentFetchOptions(): openbis.RoleAssignmentFetchOptions
        createPersonFetchOptions(): openbis.PersonFetchOptions
        createPropertyTypeFetchOptions(): openbis.PropertyTypeFetchOptions
        createPropertyAssignmentFetchOptions(): openbis.PropertyAssignmentFetchOptions
        createSemanticAnnotationFetchOptions(): openbis.SemanticAnnotationFetchOptions
        createExternalDmsFetchOptions(): openbis.ExternalDmsFetchOptions
        createOperationExecutionFetchOptions(): openbis.OperationExecutionFetchOptions
        createDataStoreFetchOptions(): openbis.DataStoreFetchOptions
        createDataSetFileFetchOptions(): openbis.DataSetFileFetchOptions
        createQueryFetchOptions(): openbis.QueryFetchOptions
        createPersonalAccessTokenFetchOptions(): openbis.PersonalAccessTokenFetchOptions
        extractIdentifiers(entities): string[]
        extractCodes(entities): string[]
        assertNull(actual, msg?): void
        assertNotNull(actual, msg?): void
        assertTrue(actual, msg?): void
        assertFalse(actual, msg?): void
        assertContains(actual, expected, msg?): void
        assertEqual(actual, expected, msg?): void
        assertDeepEqual(actual, expected, msg?): void
        assertNotEqual(actual, expected, msg?): void
        assertDate(millis, msg, year, month, day, hour, minute): void
        assertToday(millis, msg?): void
        assertFileEquals(actualFile: openbis.File, expectedFile: Object): void
        assertFileExists(facade: openbis.openbis, owner: string, source: string): Promise<void>
        assertFileDoesNotExist(facade: openbis.openbis, owner: string, source: string): Promise<void>
        assertEqualDictionary(actual, expected, msg?): void
        renderDictionary(dictionary): string
        assertObjectsCount(objects: any[], count: number): void
        assertObjectsWithValues(objects: any[], propertyName: string, propertyValues: any): void
        assertObjectsWithOrWithoutCollections(objects: any[], accessor, checker): void
        assertObjectsWithCollections(objects: any[], accessor): void
        assertObjectsWithoutCollections(objects: any[], accessor): void
        shallowEqual(actual, expected): void
        start(): void
        finish(): void
        ok(msg?): void
        section(msg): void
        fail(msg?): void
        base64ToBlob(b64Data: string, contentType: string='', sliceSize: number=512): Blob
    }
}
