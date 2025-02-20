import autoBind from 'auto-bind'
import dto from '@src/js/services/openbis/dto.js'

class Facade {
  constructor() {
    autoBind(this)
  }

  _init() {
    let _this = this
    return new Promise((resolve, reject) => {
      /* eslint-disable-next-line no-undef */
      requirejs(
        ['openbis', 'util/Json'],
        (openbis, stjs) => {
          new openbis().getServerPublicInformation().then(
            result => {
              const afsServerUrlKey = 'server-public-information.afs-server.url'
              const afsServerUrl = result[afsServerUrlKey]
              _this.v3 = new openbis(null, afsServerUrl)
              _this.stjs = stjs
              resolve()
            },
            error => {
              reject(error)
            }
          )
        },
        error => {
          reject(error)
        }
      )
    })
  }

  _getAfsServerFacade() {
    if (!this.isAfsSet()) {
      throw new Error('server-public-information.afs-server.url is not set.')
    } else {
      return this.v3.getAfsServerFacade()
    }
  }

  isAfsSet() {
    return !!this.v3.getAfsUrl()
  }

  useSession(sessionToken) {
    this.v3._private.sessionToken = sessionToken
  }

  getSessionInformation() {
    return this.promise(this.v3.getSessionInformation())
  }

  getServerInformation() {
    return this.promise(this.v3.getServerInformation())
  }

  getServerPublicInformation() {
    return this.promise(this.v3.getServerPublicInformation())
  }

  login(user, password) {
    return this.promise(this.v3.login(user, password))
  }

  logout() {
    return this.promise(this.v3.logout())
  }

  getSpaces(ids, fo) {
    return this.promise(this.v3.getSpaces(ids, fo))
  }

  getProjects(ids, fo) {
    return this.promise(this.v3.getProjects(ids, fo))
  }

  getExperiments(ids, fo) {
    return this.promise(this.v3.getExperiments(ids, fo))
  }

  getSamples(ids, fo) {
    return this.promise(this.v3.getSamples(ids, fo))
  }

  getDataSets(ids, fo) {
    return this.promise(this.v3.getDataSets(ids, fo))
  }

  getPlugins(ids, fo) {
    return this.promise(this.v3.getPlugins(ids, fo))
  }

  getQueries(ids, fo) {
    return this.promise(this.v3.getQueries(ids, fo))
  }

  getPropertyTypes(ids, fo) {
    return this.promise(this.v3.getPropertyTypes(ids, fo))
  }

  getAuthorizationGroups(ids, fo) {
    return this.promise(this.v3.getAuthorizationGroups(ids, fo))
  }

  getPersons(ids, fo) {
    return this.promise(this.v3.getPersons(ids, fo))
  }

  updatePersons(updates) {
    return this.promise(this.v3.updatePersons(updates))
  }

  searchSpaces(criteria, fo) {
    return this.promise(this.v3.searchSpaces(criteria, fo))
  }

  searchProjects(criteria, fo) {
    return this.promise(this.v3.searchProjects(criteria, fo))
  }

  searchPropertyTypes(criteria, fo) {
    return this.promise(this.v3.searchPropertyTypes(criteria, fo))
  }

  searchPlugins(criteria, fo) {
    return this.promise(this.v3.searchPlugins(criteria, fo))
  }

  searchPersonalAccessTokens(criteria, fo) {
    return this.promise(this.v3.searchPersonalAccessTokens(criteria, fo))
  }

  searchQueries(criteria, fo) {
    return this.promise(this.v3.searchQueries(criteria, fo))
  }

  searchQueryDatabases(criteria, fo) {
    return this.promise(this.v3.searchQueryDatabases(criteria, fo))
  }

  searchMaterials(criteria, fo) {
    return this.promise(this.v3.searchMaterials(criteria, fo))
  }

  searchSamples(criteria, fo) {
    return this.promise(this.v3.searchSamples(criteria, fo))
  }

  searchExperiments(criteria, fo) {
    return this.promise(this.v3.searchExperiments(criteria, fo))
  }

  searchDataSets(criteria, fo) {
    return this.promise(this.v3.searchDataSets(criteria, fo))
  }

  searchVocabularies(criteria, fo) {
    return this.promise(this.v3.searchVocabularies(criteria, fo))
  }

  searchVocabularyTerms(criteria, fo) {
    return this.promise(this.v3.searchVocabularyTerms(criteria, fo))
  }

  searchPersons(criteria, fo) {
    return this.promise(this.v3.searchPersons(criteria, fo))
  }

  searchAuthorizationGroups(criteria, fo) {
    return this.promise(this.v3.searchAuthorizationGroups(criteria, fo))
  }

  searchPropertyAssignments(criteria, fo) {
    return this.promise(this.v3.searchPropertyAssignments(criteria, fo))
  }

  searchEvents(criteria, fo) {
    return this.promise(this.v3.searchEvents(criteria, fo))
  }

  getSampleTypes(ids, fo) {
    return this.promise(this.v3.getSampleTypes(ids, fo))
  }

  getExperimentTypes(ids, fo) {
    return this.promise(this.v3.getExperimentTypes(ids, fo))
  }

  getDataSetTypes(ids, fo) {
    return this.promise(this.v3.getDataSetTypes(ids, fo))
  }

  getMaterialTypes(ids, fo) {
    return this.promise(this.v3.getMaterialTypes(ids, fo))
  }

  getVocabularies(ids, fo) {
    return this.promise(this.v3.getVocabularies(ids, fo))
  }

  getOperationExecutions(ids, fo) {
    return this.promise(this.v3.getOperationExecutions(ids, fo))
  }

  updateSampleTypes(updates) {
    return this.promise(this.v3.updateSampleTypes(updates))
  }

  updateExperimentTypes(updates) {
    return this.promise(this.v3.updateExperimentTypes(updates))
  }

  updateDataSets(updates) {
    return this.promise(this.v3.updateDataSets(updates))
  }

  updateDataSetTypes(updates) {
    return this.promise(this.v3.updateDataSetTypes(updates))
  }

  updateMaterialTypes(updates) {
    return this.promise(this.v3.updateMaterialTypes(updates))
  }

  searchSampleTypes(criteria, fo) {
    return this.promise(this.v3.searchSampleTypes(criteria, fo))
  }

  searchExperimentTypes(criteria, fo) {
    return this.promise(this.v3.searchExperimentTypes(criteria, fo))
  }

  searchDataSetTypes(criteria, fo) {
    return this.promise(this.v3.searchDataSetTypes(criteria, fo))
  }

  searchMaterialTypes(criteria, fo) {
    return this.promise(this.v3.searchMaterialTypes(criteria, fo))
  }

  deleteSampleTypes(ids, options) {
    return this.promise(this.v3.deleteSampleTypes(ids, options))
  }

  deleteExperimentTypes(ids, options) {
    return this.promise(this.v3.deleteExperimentTypes(ids, options))
  }

  deleteDataSetTypes(ids, options) {
    return this.promise(this.v3.deleteDataSetTypes(ids, options))
  }

  deleteMaterialTypes(ids, options) {
    return this.promise(this.v3.deleteMaterialTypes(ids, options))
  }

  evaluatePlugin(options) {
    return this.promise(this.v3.evaluatePlugin(options))
  }

  uploadToSessionWorkspace(file) {
    return this.promise(this.v3.uploadToSessionWorkspace(file))
  }

  executeCustomDSSService(serviceId, options) {
    return this.promise(
      this.v3.getDataStoreFacade().executeCustomDSSService(serviceId, options)
    )
  }

  searchFiles(criteria, fetchOptions) {
    return this.promise(
      this.v3.getDataStoreFacade().searchFiles(criteria, fetchOptions)
    );
  }

  fromJson(jsonType, jsonObject) {
    return this.promise(this.stjs.fromJson(jsonType, jsonObject))
  }

  list(owner, source, recursively) {
    return this.promise(
      this._getAfsServerFacade().list(owner, source, recursively)
    )
  }

  read(owner, source, offset, limit) {
    return this.promise(
      this._getAfsServerFacade().read(owner, source, offset, limit)
    )
  }

  write(owner, source, offset, data) {
    return this.promise(
      this._getAfsServerFacade().write(owner, source, offset, data)
    )
  }

  delete(owner, source) {
    return this.promise(this._getAfsServerFacade().delete(owner, source))
  }

  copy(sourceOwner, source, targetOwner, target) {
    return this.promise(
      this._getAfsServerFacade().copy(sourceOwner, source, targetOwner, target)
    )
  }

  move(sourceOwner, source, targetOwner, target) {
    return this.promise(
      this._getAfsServerFacade().move(sourceOwner, source, targetOwner, target)
    )
  }

  create(owner, source, directory) {
    return this.promise(
      this._getAfsServerFacade().create(owner, source, directory)
    )
  }

  free(owner, source) {
    return this.promise(this._getAfsServerFacade().free(owner, source))
  }

  getRightsByIds(ids, fo) {
    return this.promise(this.v3.getRights(ids, fo))
  }

  getRights(idMap, fo) {
    const ids = idMap.map(id => {
      switch (id.entityKind) {
        case 'object': {
          return new dto.SamplePermId(id.permId)
        }
        case 'collection': {
          return new dto.ExperimentPermId(id.permId)
        }
      }
      return null
    })

    return this.promise(this.v3.getRights(ids, fo))
  }

  async executeService(id, options) {
    const scheduleResult = await this.executeOperations(
      [new dto.ExecuteCustomASServiceOperation(id, options)],
      new dto.AsynchronousOperationExecutionOptions()
    )

    const executionId = scheduleResult.executionId
    const executionFetchOptions = new dto.OperationExecutionFetchOptions()
    executionFetchOptions.withDetails().withResults()
    executionFetchOptions.withDetails().withError()

    for (;;) {
      const executions = await this.getOperationExecutions(
        [executionId],
        executionFetchOptions
      )

      const execution = executions[executionId]

      if (!execution) {
        throw new Error('Execution id: ' + executionId + ' not found.')
      } else if (execution.details.error !== null) {
        throw new Error(execution.details.error.message)
      } else if (
        execution.details.results !== null &&
        execution.details.results.length > 0
      ) {
        return execution.details.results[0].result
      } else {
        await new Promise(resolve => {
          setTimeout(resolve, 1000)
        })
      }
    }
  }

  executeQuery(id, options) {
    return this.promise(this.v3.executeQuery(id, options))
  }

  executeSql(sql, options) {
    return this.promise(this.v3.executeSql(sql, options))
  }

  executeOperations(operations, options) {
    return this.promise(this.v3.executeOperations(operations, options))
  }

  promiseWithAbort(dfd) {
    const { promise, abortFn } = dfd;
    return {
        promise: this.promise(promise),        
        abortFn 
    };
  }

  promise(dfd) {
    return new Promise((resolve, reject) => {
      dfd.then(
        result => {
          resolve(result)
        },
        error => {
          reject(error)
        }
      )
    })
  }
}

const facade = new Facade()
export default facade
