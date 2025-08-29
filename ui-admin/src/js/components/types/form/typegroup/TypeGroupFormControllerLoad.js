import _ from 'lodash'
import AppController from '@src/js/components/AppController.js'
import PageControllerLoad from '@src/js/components/common/page/PageControllerLoad.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'

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

    if (loadedTypeGroup && loadedTypeGroup.typeGroupAssignments) {
      objectTypes = loadedTypeGroup.typeGroupAssignments.map(typeGroupAssignment =>
        this._createObjectType('objectType-' + objectTypesCounter++, loadedTypeGroup, typeGroupAssignment)
      )
    }
    const selection = this._createSelection(objectTypes)

    console.log('TypeGroupFormControllerLoad.load', typeGroup)
    console.log('TypeGroupFormControllerLoad.load', objectTypes)

    return this.context.setState({
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
    const registrator = _.get(loadedTypeGroup, 'registrator.userId', null)
    const internal = _.get(loadedTypeGroup, 'managedInternally', false)

    const typeGroup = {
      id: _.get(loadedTypeGroup, 'code', null),
      code: FormUtil.createField({
        value: _.get(loadedTypeGroup, 'code', null),
        enabled: loadedTypeGroup === null
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
      })
    }
    if (loadedTypeGroup) {
      typeGroup.original = _.cloneDeep(typeGroup)
    }
    return typeGroup
  }

  _createObjectType(id, loadedTypeGroup, loadedTypeGroupAssignment) {
    const official = _.get(loadedTypeGroupAssignment, 'official', false)
    const registrator = _.get(loadedTypeGroupAssignment, 'registrator.userId', null)
    const internalTypeGroup = _.get(
      loadedTypeGroup,
      'managedInternally',
      false
    )
    const managedInternally = _.get(loadedTypeGroupAssignment, 'managedInternally', false)
    const internalTypeGroupAssignment = internalTypeGroup && managedInternally

    const sampleType = _.get(loadedTypeGroupAssignment, 'sampleType', null)

    const objectType = {
      id: id,
      code: FormUtil.createField({
        value: _.get(sampleType, 'code', null),
        enabled: false
      }),
      label: FormUtil.createField({
        value: _.get(sampleType, 'label', null),
        enabled: !internalTypeGroupAssignment || AppController.getInstance().isSystemUser()
      }),
      description: FormUtil.createField({
        value: _.get(sampleType, 'description', null),
        enabled: !internalTypeGroupAssignment || AppController.getInstance().isSystemUser()
      }),
      official: FormUtil.createField({
        value: official,
        enabled:
          !official &&
          (!internalTypeGroupAssignment || AppController.getInstance().isSystemUser())
      }),
      registrator: FormUtil.createField({
        value: registrator,
        visible: false,
        enabled: false
      }),
      registrationDate: FormUtil.createField({
        value: _.get(loadedTypeGroupAssignment, 'registrationDate', null),
        visible: false,
        enabled: false
      }),
      internal: FormUtil.createField({
        value: managedInternally,
        visible: false,
        enabled: false
      })
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
