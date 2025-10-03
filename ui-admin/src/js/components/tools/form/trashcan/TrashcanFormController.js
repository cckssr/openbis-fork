import autoBind from 'auto-bind'
import pages from '@src/js/common/consts/pages.js'
import TrashcanFormControllerLoad from '@src/js/components/tools/form/trashcan/TrashcanFormControllerLoad.js'

export default class TrashcanFormController {
  constructor(facade) {
    autoBind(this)
    this.facade = facade
  }

  getPage() {
    return pages.TOOLS
  }

  init(context) {
    this.context = context
    this.object = context.getProps().object
  }

  load() {
    return new TrashcanFormControllerLoad(this).execute()
  }

  getFacade() {
    return this.facade
  }

  getContext() {
    return this.context
  }

  getObject() {
    return this.object
  }
}
