import _ from 'lodash'
import PageControllerSave from '@src/js/components/common/page/PageControllerSave.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'
import openbis from '@src/js/services/openbis.js'
import messages from '@src/js/common/messages.js'

export default class UserFormControllerSave extends PageControllerSave {
  async save() {
    const state = this.context.getState()

    const user = FormUtil.trimFields({ ...state.user })
    const groups = state.groups
    const roles = state.roles

    const operations = []

    if (user.original) {
      if (this._isUserUpdateNeeded(user)) {
        operations.push(this._updateUserOperation(user))
      }
    } else {
      operations.push(this._createUserOperation(user))
    }

    state.original.groups.forEach(originalGroup => {
      const group = _.find(groups, ['id', originalGroup.id])
      if (!group) {
        operations.push(
          this._deleteGroupAssignmentOperation(user, originalGroup)
        )
      }
    })

    groups.forEach(group => {
      if (group.original) {
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

    state.original.roles.forEach(originalRole => {
      if (originalRole.inheritedFrom.value) {
        return
      }
      const role = _.find(roles, ['id', originalRole.id])
      if (!role) {
        operations.push(this._deleteRoleAssignmentOperation(user, originalRole))
      }
    })

    roles.forEach(role => {
      if (role.inheritedFrom.value) {
        return
      }
      if (role.original) {
        if (this._isRoleAssignmentUpdateNeeded(role)) {
          operations.push(
            this._deleteRoleAssignmentOperation(user, role.original)
          )
          operations.push(this._createRoleAssignmentOperation(user, role))
        }
      } else {
        operations.push(this._createRoleAssignmentOperation(user, role))
      }
    })

    const options = new openbis.SynchronousOperationExecutionOptions()
    options.setExecuteInOrder(true)
    await this.facade.executeOperations(operations, options)

    return user.userId.value
  }

  _isUserUpdateNeeded(user) {
    return FormUtil.haveFieldsChanged(user, user.original, ['space', 'userStatus', 'userStatusExpiryDate', 'metadata'])
  }

  _isGroupAssignmentUpdateNeeded(group) {
    return FormUtil.haveFieldsChanged(group, group.original, ['code', 'metadata'])
  }

  _isRoleAssignmentUpdateNeeded(role) {
    return FormUtil.haveFieldsChanged(role, role.original, [
      'level',
      'space',
      'project',
      'role'
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

  _createUserOperation(user) {
    const creation = new openbis.PersonCreation()
    creation.setUserId(user.userId.value)
    if (user.space.value) {
      creation.setSpaceId(new openbis.SpacePermId(user.space.value))
    }
    if(user.userStatusExpiryDate.value && user.userStatusExpiryDate.value.dateObject)
    {
      creation.setExpiryDate(user.userStatusExpiryDate.value.dateObject.getTime());
    }
    const metadataObject = this._transformMetadataToObject(user.metadata.value)
    creation.setMetaData(metadataObject)
    return new openbis.CreatePersonsOperation([creation])
  }

  _updateUserOperation(user) {
    const update = new openbis.PersonUpdate()
    update.setUserId(new openbis.PersonPermId(user.userId.value))
    update.setSpaceId(
      user.space.value ? new openbis.SpacePermId(user.space.value) : null
    )
    if(user.userStatus.value !== user.original.userStatus.value ||
            (user.userStatus.value === messages.ACTIVE_UNTIL_EXPIRY_DATE &&
            +user.userStatusExpiryDate.value.dateObject !== +user.original.userStatusExpiryDate.value.dateObject)) {
        if(user.userStatus.value === messages.ACTIVE) {
            update.activate()
            update.setExpiryDate(null)
        } else if(user.userStatus.value == messages.INACTIVE) {
            update.deactivate()
            update.setExpiryDate(null)
        } else {
            update.activate()
            if(user.userStatusExpiryDate.value.dateObject == null)
            {
              update.setExpiryDate(user.userStatusExpiryDate.value.dateObject);
            }
            else
            {
              update.setExpiryDate(user.userStatusExpiryDate.value.dateObject.getTime());
            }
        }
    }
    const metadataObject = this._transformMetadataToObject(user.metadata.value)
    update.getMetaData().set(metadataObject)
    return new openbis.UpdatePersonsOperation([update])
  }

  _createGroupAssignmentOperation(user, group) {
    const update = new openbis.AuthorizationGroupUpdate()
    update.setAuthorizationGroupId(
      new openbis.AuthorizationGroupPermId(group.code.value)
    )
    update.getUserIds().add(new openbis.PersonPermId(user.userId.value))
    const metadataObject = this._transformMetadataToObject(group.metadata.value)
    update.getMetaData().set(metadataObject)
    return new openbis.UpdateAuthorizationGroupsOperation([update])
  }

  _deleteGroupAssignmentOperation(user, group) {
    const update = new openbis.AuthorizationGroupUpdate()
    update.setAuthorizationGroupId(
      new openbis.AuthorizationGroupPermId(group.code.value)
    )
    update.getUserIds().remove(new openbis.PersonPermId(user.userId.value))
    return new openbis.UpdateAuthorizationGroupsOperation([update])
  }

  _createRoleAssignmentOperation(user, role) {
    const creation = new openbis.RoleAssignmentCreation()
    creation.setUserId(new openbis.PersonPermId(user.userId.value))
    creation.setRole(role.role.value)

    const level = role.level.value
    if (level === openbis.RoleLevel.SPACE) {
      creation.setSpaceId(new openbis.SpacePermId(role.space.value))
    } else if (level === openbis.RoleLevel.PROJECT) {
      creation.setProjectId(
        new openbis.ProjectIdentifier(
          '/' + role.space.value + '/' + role.project.value
        )
      )
    }
    return new openbis.CreateRoleAssignmentsOperation([creation])
  }

  _deleteRoleAssignmentOperation(user, role) {
    const id = new openbis.RoleAssignmentTechId(role.techId.value)
    const options = new openbis.RoleAssignmentDeletionOptions()
    options.setReason('deleted via ng_ui')
    return new openbis.DeleteRoleAssignmentsOperation([id], options)
  }
}
