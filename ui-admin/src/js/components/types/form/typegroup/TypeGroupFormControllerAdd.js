import AppController from '@src/js/components/AppController.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'

export default class TypeGroupFormControllerAdd {
  constructor(controller) {
    this.controller = controller
    this.context = controller.context
    this.gridController = controller.gridController
  }

  async execute() {
    let { objectTypes, objectTypesCounter } = this.context.getState()

    const newObjectType = {
      id: 'objectType-' + objectTypesCounter++,
      selectObjectType: FormUtil.createField({}),
      code: FormUtil.createField({}),
      description: FormUtil.createField({}),
      registrator: FormUtil.createField({
        value: AppController.getInstance().getUser(),
        visible: false,
        enabled: false
      }),
      registrationDate: FormUtil.createField({
        visible: false,
        enabled: false
      }),
      original: null,
      internal: FormUtil.createField({
          value: false,
          visible: false,
          enabled: false
        })
    }

    const newObjectTypes = Array.from(objectTypes)
    newObjectTypes.push(newObjectType)

    await this.context.setState(state => ({
      ...state,
      objectTypes: newObjectTypes,
      objectTypesCounter,
      selection: {
        type: TypeGroupFormSelectionType.OBJECT_TYPE,
        params: {
          id: newObjectType.id,
          part: 'code'
        }
      }
    }))

    if (this.gridController) {
      await this.gridController.load()
      await this.gridController.selectRow(newObjectType.id)
      await this.gridController.showRow(newObjectType.id)
    }

    await this.controller.changed(true)
  }
}
