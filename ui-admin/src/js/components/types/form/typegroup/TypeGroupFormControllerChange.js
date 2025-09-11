import PageControllerChange from '@src/js/components/common/page/PageControllerChange.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'

export default class TypeGroupFormControllerChange extends PageControllerChange {
  constructor(controller) {
    super(controller)
    this.gridController = controller.gridController
  }

  async execute(type, params) {
    if (type === TypeGroupFormSelectionType.TYPE_GROUP) {
      await this._handleChangeTypeGroup(params)
    } else if (type === TypeGroupFormSelectionType.OBJECT_TYPE) {
      await this._handleChangeObjectType(params)
    }
  }

  async _handleChangeTypeGroup(params) {
    await this.context.setState(state => {
      const { newObject } = FormUtil.changeObjectField(
        state.typeGroup,
        params.field,
        params.value
      )
      return {
        typeGroup: newObject
      }
    })
    await this.controller.changed(true)
  }

  async _handleChangeObjectType(params) {
    await this.context.setState(state => {
      const { newCollection } = FormUtil.changeCollectionItemField(
        state.objectTypes,
        params.id,
        params.field,
        params.value
      )
      return {
        objectTypes: newCollection
      }
    })

    if (this.gridController) {
      await this.gridController.load()
      await this.gridController.showRow(params.id)
    }

    await this.controller.changed(true)
  }
}
