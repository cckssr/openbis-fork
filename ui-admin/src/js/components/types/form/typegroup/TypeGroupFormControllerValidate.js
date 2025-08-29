import PageControllerValidate from '@src/js/components/common/page/PageConrollerValidate.js'
import messages from '@src/js/common/messages.js'

export default class TypeGroupFormControllerValidate extends PageControllerValidate {
  validate(validator) {
    const { typeGroup } = this.context.getState()

    const newTypeGroup = this._validateTypeGroup(validator, typeGroup)

    return {
      typeGroup: newTypeGroup
    }
  }

  async select(firstError) {
    const { typeGroup } = this.context.getState()

    if (firstError.object === typeGroup) {
      await this.setSelection({
        type: TypeGroupFormSelectionType.TYPE_GROUP,
        params: {
          part: firstError.name
        }
      })
    } else if (typeGroup.includes(firstError.object)) {
      await this.setSelection({
        type: TypeGroupFormSelectionType.TYPE_GROUP,
        params: {
          id: firstError.object.id,
          part: firstError.name
        }
      })

      if (this.controller.gridController) {
        await this.controller.gridController.showRow(firstError.object.id)
      }
    }
  }

  _validateTypeGroup(validator, typeGroup) {
    validator.validateNotEmpty(typeGroup, 'code', messages.get(messages.CODE))

    validator.validateCode(typeGroup, 'code', messages.get(messages.CODE))

    return validator.withErrors(typeGroup)
  }
}
