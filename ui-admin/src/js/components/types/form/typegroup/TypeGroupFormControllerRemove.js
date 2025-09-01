import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'

export default class TypeGroupFormControllerRemove {
  constructor(controller) {
    this.controller = controller
    this.context = controller.context
    this.gridController = controller.gridController
  }

  execute() {
    const { selection } = this.context.getState()
    if (selection.type === TypeGroupFormSelectionType.OBJECT_TYPE) {
      this._handleRemoveObjectType(selection.params.id)
    }
  }

  async _handleRemoveObjectType(objectTypeId) {
    const { objectTypes } = this.context.getState()

    const objectTypeIndex = objectTypes.findIndex(objectType => objectType.id === objectTypeId)

    const newObjectTypes = Array.from(objectTypes)
    newObjectTypes.splice(objectTypeIndex, 1)

    await this.context.setState(state => ({
      ...state,
      objectTypes: newObjectTypes,
      selection: null
    }))

    if (this.gridController) {
      await this.gridController.selectRow(null)
      await this.gridController.load()
    }

    await this.controller.changed(true)
  }
}
