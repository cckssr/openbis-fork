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

    const typeGroup = await openbis.getTypeGroups([id], fo)
    console.log('TypeGroupFormFacade.loadTypeGroup', typeGroup)
    return typeGroup[code]
  }

  async loadObjectTypesOptions(selectedObjectTypes) {

    const fetchOptions = new openbis.SampleTypeFetchOptions()
    fetchOptions.withValidationPlugin()
    
    const result = await openbis.searchSampleTypes(new openbis.SampleTypeSearchCriteria(), fetchOptions)

    let objects = result.objects
    .filter(o => !selectedObjectTypes.some(selectedObjectType => selectedObjectType.code.value === o.getCode()))
    .map(o => ({
      id: o.getCode(),
      code: o.getCode(),
      description: _.get(o, 'description'),
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
