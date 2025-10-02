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
    console.log('TrashcanFormFacade.revertDeletions', row)
    const deletionIds = [new openbis.DeletionTechId(row.id)]
    const result = await openbis.revertDeletions(deletionIds)
    console.log('TrashcanFormFacade.revertDeletions', result)
    return result
  }

  async deletePermanently(row, includeDependent) {
    const deletionIds = [new openbis.DeletionTechId(row.id)]
    const confirmOperation = new openbis.ConfirmDeletionsOperation(deletionIds);
    confirmOperation.setForceDeletionOfDependentDeletions(includeDependent);
    const result = openbis.executeOperations([confirmOperation], new openbis.SynchronousOperationExecutionOptions())
    console.log(result)
    /* if (includeDependent){
      const result = await openbis.deletePermanentlyForced(deletionIds)
      console.log('TrashcanFormFacade.deletePermanentlyForced', result)
    } else {
      const result = await openbis.deletePermanently(deletionIds)
      console.log('TrashcanFormFacade.deletePermanently', result)
    } */
    return result
  }

  async emptyTrashcan(deletions) {
    const deleteIds = deletions.map(deletion => deletion.id)
    const result = await openbis.deletePermanently(deleteIds, true)
    console.log('TrashcanFormFacade.emptyTrashcan', result)
    return result
  }
}
