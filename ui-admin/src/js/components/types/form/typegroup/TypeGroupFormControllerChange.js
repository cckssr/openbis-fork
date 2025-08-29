import PageControllerChange from '@src/js/components/common/page/PageControllerChange.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'

export default class TypeGroupFormControllerChange extends PageControllerChange {
  constructor(controller) {
    super(controller)
    this.gridController = controller.gridController
  }

  async execute(params) {
    await this.context.setState(state => {
      console.log('TypeGroupFormControllerChange.execute', params)
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
}
