import _ from 'lodash'
import AppController from '@src/js/components/AppController.js'
import PageControllerLoad from '@src/js/components/common/page/PageControllerLoad.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'

export default class TypeGroupFormControllerLoad extends PageControllerLoad {
  async load(object, isNew) {
    let loadedTypeGroup = null

    if (!isNew) {
      loadedTypeGroup = await this.facade.loadTypeGroup(object.id)
      if (!loadedTypeGroup) {
        return
      }
    }
    
    const typeGroup = this._createTypeGroup(loadedTypeGroup)

    let objectTypesCounter = 0
    let objectTypes = []

    console.log('TypeGroupFormControllerLoad.load', loadedTypeGroup)
    if (loadedTypeGroup && loadedTypeGroup.typeGroupAssignments) {
      objectTypes = loadedTypeGroup.typeGroupAssignments.map(typeGroupAssignment =>
        this._createObjectType('objectType-' + objectTypesCounter++, loadedTypeGroup, typeGroupAssignment)
      )
    }
    
    const selection = this._createSelection(objectTypes)

    console.log('TypeGroupFormControllerLoad.load', typeGroup)
    console.log('TypeGroupFormControllerLoad.load', objectTypes)

    const objectTypesOptions = await this.facade.loadObjectTypesOptions(objectTypes)

    return this.context.setState({
      objectTypesOptions,
      typeGroup,
      objectTypes,
      objectTypesCounter,
      selection,
      original: {
        typeGroup: typeGroup.original,
        objectTypes: objectTypes.map(objectType => objectType.original)
      }
    })
  }

  _createTypeGroup(loadedTypeGroup) {
    const code = _.get(loadedTypeGroup, 'code', null)
    const internal = _.get(loadedTypeGroup, 'managedInternally', false)
    const registrator = _.get(loadedTypeGroup, 'registrator.userId', null)
    const metadata = Object.entries(_.get(loadedTypeGroup, 'metaData', [])).map(([key, value]) => ({
			key: key,
			value: value,
		}))
    const typeGroup = {
      id: code,
      code: FormUtil.createField({
        value: code,
        enabled: true
      }),
      objectType: FormUtil.createField({
        value: this.object.type,
        enabled: true
      }),
      internal: FormUtil.createField({
        value: internal,
        visible: AppController.getInstance().isSystemUser(),
        enabled:
          loadedTypeGroup === null &&
          AppController.getInstance().isSystemUser()
      }),
      registrator: FormUtil.createField({
        value: registrator,
        visible: false,
        enabled: false
      }),
      metadata: FormUtil.createField({
        value: metadata,
        enabled: true
      })
    }
    if (loadedTypeGroup) {
      typeGroup.original = _.cloneDeep(typeGroup)
    }
    return typeGroup
  }

  _createObjectType(id, loadedTypeGroup, loadedTypeGroupAssignment) {
    const sampleType = _.get(loadedTypeGroupAssignment, 'sampleType', null)

    const objectType = {
      id: id,
      code: FormUtil.createField({
        value: _.get(sampleType, 'code', null),
        enabled: false
      }),
      description: FormUtil.createField({
        value: _.get(sampleType, 'description', null),
        enabled: false
      }),
      registrator: FormUtil.createField({
        value:  _.get(loadedTypeGroupAssignment, 'registrator.userId', null),
        visible: false,
        enabled: false
      }),
      registrationDate: FormUtil.createField({
        value: _.get(loadedTypeGroupAssignment, 'registrationDate', null),
        visible: false,
        enabled: false
      }),
      internal: FormUtil.createField({
        value: _.get(loadedTypeGroupAssignment, 'managedInternally', false),
        visible: AppController.getInstance().isSystemUser(),
        enabled:
          loadedTypeGroupAssignment === null &&
          AppController.getInstance().isSystemUser()
      }),
    }
    objectType.original = _.cloneDeep(objectType)
    return objectType
  }

  _createSelection(newObjectTypes) {
    const { selection: oldSelection, objectTypes: oldObjectTypes } = this.context.getState()

    if (!oldSelection) {
      return null
    } else if (oldSelection.type === TypeGroupFormSelectionType.OBJECT_TYPE) {
      const oldObjectType = _.find(
        oldObjectTypes,
        oldObjectType => oldObjectType.id === oldSelection.params.id
      )
      const newObjectType = _.find(newObjectTypes, newObjectType => {
        const newValue = newObjectType.code.value
          ? newObjectType.code.value.toLowerCase()
          : null
        const oldValue = oldObjectType.code.value
          ? oldObjectType.code.value.toLowerCase()
          : null
        return newValue === oldValue
      })

      if (newObjectType) {
        return {
          type: TypeGroupFormSelectionType.OBJECT_TYPE,
          params: {
            id: newObjectType.id
          }
        }
      }
    } else {
      return null
    }
  }
}
