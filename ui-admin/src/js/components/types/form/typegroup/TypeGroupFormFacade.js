import openbis from '@src/js/services/openbis.js'
import compare from '@src/js/common/compare.js'
import _ from 'lodash'

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

  async loadObjectTypesOptions() {

    const fetchOptions = new openbis.SampleTypeFetchOptions()
    fetchOptions.withValidationPlugin()
    
    const result = await openbis.searchSampleTypes(new openbis.SampleTypeSearchCriteria(), fetchOptions)

    let objects = result.objects.map(o => ({
      id: o.getCode(),
      code: o.getCode(),
      description: _.get(o, 'description'),
      internal: _.get(o, 'managedInternally'),
      text: o.getCode()
    }))

    objects.sort((o1, o2) => compare(o1.text, o2.text))
    console.log('TypeGroupFormFacade.loadObjectTypesOptions', objects)

    return objects
  }

  async executeOperations(operations, options) {
    return openbis.executeOperations(operations, options)
  }
}
