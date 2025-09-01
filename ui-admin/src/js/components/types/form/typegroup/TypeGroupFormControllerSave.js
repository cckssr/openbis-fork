import _ from 'lodash'
import PageControllerSave from '@src/js/components/common/page/PageControllerSave.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'
import openbis from '@src/js/services/openbis.js'

export default class TypeGroupFormControllerSave extends PageControllerSave {
  async save() {
    const state = this.context.getState()
    console.log('TypeGroupFormControllerSave.save', {state})

    const objectTypes = state.objectTypes
    const typeGroup = this._prepareTypeGroup(state.typeGroup)

    console.log('TypeGroupFormControllerSave.save', {typeGroup})
    console.log('TypeGroupFormControllerSave.save', {objectTypes})
    const operations = []


    if (typeGroup.original) {
      if (this._isTypeGroupUpdateNeeded(typeGroup)) {
        operations.push(this._updateTypeGroupOperation(typeGroup))
      }
    } else {
      operations.push(this._createTypeGroupOperation(typeGroup))
    }

    state.original.objectTypes.forEach(originalObjectType => {
      const objectType = _.find(objectTypes, ['id', originalObjectType.id])
      if (!objectType) {
        operations.push(
          this._deleteObjectTypeOperation(typeGroup, originalObjectType)
        )
      }
    })

    objectTypes.forEach(objectType => {
      if (objectType.original) {
        if (this._isGroupAssignmentUpdateNeeded(group)) {
          operations.push(
            this._deleteGroupAssignmentOperation(user, group.original)
          )
          operations.push(this._createGroupAssignmentOperation(user, group))
        }
      } else {
        operations.push(this._createGroupAssignmentOperation(user, group))
      }
    })

    const options = new openbis.SynchronousOperationExecutionOptions()
    options.setExecuteInOrder(true)
    await this.facade.executeOperations(operations, options)

    return typeGroup.code.value
  }


  _isTypeGroupAssignmentUpdateNeeded(objectType) {
    return FormUtil.haveFieldsChanged(objectType, objectType.original, [
      'code',
    ])
  }

  _prepareTypeGroup(typeGroup) {
    const code = typeGroup.code.value
    return FormUtil.trimFields({
      ...typeGroup,
      code: {
        value: code ? code.toUpperCase() : null
      }
    })
  }

  _isTypeGroupUpdateNeeded(typeGroup) {
    return FormUtil.haveFieldsChanged(typeGroup, typeGroup.original, [
      'code',
    ])
  }

  _createTypeGroupOperation(typeGroup) {
    const creation = new openbis.TypeGroupCreation()
    creation.setCode(typeGroup.code.value)
    creation.setManagedInternally(typeGroup.internal.value)
    //creation.setMetaData(typeGroup.metaData.value)
    return new openbis.CreateTypeGroupsOperation([creation])
  }

  _updateTypeGroupOperation(typeGroup) {
    const update = new openbis.TypeGroupUpdate()
    update.setTypeGroupId(new openbis.TypeGroupId(typeGroup.code.value))
    //update.setMetaData(typeGroup.metaData.value)
    return new openbis.UpdateTypeGroupsOperation([update])
  }

  _deleteObjectTypeOperation(typeGroup, objectType) {
    const objectTypeId = new openbis.ObjectTypePermId(objectType.code.value)
    const options = new openbis.ObjectTypeDeletionOptions()
    options.setReason('deleted via ng_ui')
    return new openbis.DeleteObjectTypesOperation([objectTypeId], options)
  }
}
