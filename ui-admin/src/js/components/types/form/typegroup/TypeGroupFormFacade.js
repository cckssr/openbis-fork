import openbis from '@src/js/services/openbis.js'

export default class TypeGroupFormFacade {
  async loadTypeGroup(code) {
    const id = new openbis.TypeGroupId(code)
    const fo = new openbis.TypeGroupFetchOptions()
    fo.withTypeGroupAssignments()
    fo.withTypeGroupAssignments().withRegistrator()
    fo.withTypeGroupAssignments().withSampleType()
	  fo.withRegistrator()
    const typeGroup = await openbis.getTypeGroups([id], fo)
    console.log('TypeGroupFormFacade.loadTypeGroup', typeGroup)
    return typeGroup[code]
  }

  async executeOperations(operations, options) {
    return openbis.executeOperations(operations, options)
  }
}
