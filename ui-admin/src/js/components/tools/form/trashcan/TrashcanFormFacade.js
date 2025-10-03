import openbis from '@src/js/services/openbis.js'

export default class TrashcanFormFacade {

  async loadDeletions() {
    const searchCriteria = new openbis.DeletionSearchCriteria()
    const fetchOptions = new openbis.DeletionFetchOptions()
    fetchOptions.withDeletedObjects()

    const result = await openbis.searchDeletions(searchCriteria, fetchOptions)
    return result.getObjects()
  }

  async revertDeletions(row) {
    const deletionIds = [new openbis.DeletionTechId(row.id)]
    const result = await openbis.revertDeletions(deletionIds)
    return result
  }

  async deletePermanently(row, includeDependent) {
    const deletionIds = [new openbis.DeletionTechId(row.id)]
    const confirmOperation = new openbis.ConfirmDeletionsOperation(deletionIds);
    confirmOperation.setForceDeletionOfDependentDeletions(includeDependent);
    const result = await openbis.executeOperations([confirmOperation], new openbis.SynchronousOperationExecutionOptions())
    return result
  }

  async emptyTrashcan(deletions) {
    const deleteIds = deletions.map(deletion => new openbis.DeletionTechId(deletion.id))
    const confirmOperation = new openbis.ConfirmDeletionsOperation(deleteIds);
    confirmOperation.setForceDeletionOfDependentDeletions(true);
    const result = await openbis.executeOperations([confirmOperation], new openbis.SynchronousOperationExecutionOptions())
    return result
  }
}
