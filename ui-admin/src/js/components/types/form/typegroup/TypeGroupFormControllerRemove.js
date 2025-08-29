export default class TypeGroupFormControllerRemove {
  constructor(controller) {
    this.controller = controller
    this.context = controller.context
    this.gridController = controller.gridController
  }

  execute() {
   console.log('TypeGroupFormControllerRemove.execute')
  }
}
