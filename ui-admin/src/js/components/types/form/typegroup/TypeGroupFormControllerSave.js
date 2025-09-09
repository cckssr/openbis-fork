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
          this._deleteTypeGroupAssignmentOperation(typeGroup, originalObjectType)
        )
      }
    })

    objectTypes.forEach(objectType => {
      if (!objectType.original) {
        operations.push(this._createTypeGroupAssignmentOperation(typeGroup, objectType))
      }
    })

    const options = new openbis.SynchronousOperationExecutionOptions()
    options.setExecuteInOrder(true)
    await this.facade.executeOperations(operations, options)

    return typeGroup.code.value
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
      'metadata'
    ])
  }

  _transformMetadataToObject(metadataValue) {
    let metadataObject = {}
    if (metadataValue && Array.isArray(metadataValue)) {
      metadataValue.forEach(item => {
        if (item.key && item.value !== undefined) {
          metadataObject[item.key] = item.value
        }
      })
    } else if (metadataValue && typeof metadataValue === 'object') {
      metadataObject = metadataValue
    }
    return metadataObject
  }

  _createTypeGroupOperation(typeGroup) {
    const creation = new openbis.TypeGroupCreation()
    creation.setCode(typeGroup.code.value)
    creation.setManagedInternally(typeGroup.internal.value)
    const metadataObject = this._transformMetadataToObject(typeGroup.metadata.value)
    creation.setMetaData(metadataObject)
    return new openbis.CreateTypeGroupsOperation([creation])
  }

  _updateTypeGroupOperation(typeGroup) {
    const update = new openbis.TypeGroupUpdate()
    update.setTypeGroupId(new openbis.TypeGroupId(typeGroup.original.code.value))
    update.setCode(typeGroup.code.value)
    const metadataObject = this._transformMetadataToObject(typeGroup.metadata.value)
    update.getMetaData().set(metadataObject)
    return new openbis.UpdateTypeGroupsOperation([update])
  }

  _createTypeGroupAssignmentOperation(typeGroup, objectType) {
    const assignment = new openbis.TypeGroupAssignmentCreation()
    assignment.setTypeGroupId(new openbis.TypeGroupId(typeGroup.code.value))
    assignment.setSampleTypeId(new openbis.EntityTypePermId(objectType.code.value, openbis.EntityKind.SAMPLE))
    assignment.setManagedInternally(objectType.internal.value)
    return new openbis.CreateTypeGroupAssignmentsOperation([assignment])
  }

  _deleteTypeGroupAssignmentOperation(typeGroup, objectType) {
    const typeGroupAssignmentId = new openbis.TypeGroupAssignmentId( 
      new openbis.EntityTypePermId(objectType.code.value, openbis.EntityKind.SAMPLE),
      new openbis.TypeGroupId(typeGroup.code.value)
    )
    const options = new openbis.TypeGroupAssignmentDeletionOptions()
    options.setReason('deleted via ng_ui')
    return new openbis.DeleteTypeGroupAssignmentOperation([typeGroupAssignmentId], options)
  }
}
