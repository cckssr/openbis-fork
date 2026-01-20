import BrowserControllerReload from '@src/js/components/common/browser/BrowserControllerReload.js'
import objectType from '@src/js/common/consts/objectType.js'
import objectOperation from '@src/js/common/consts/objectOperation.js'

export default class DatabaseBrowserControllerReload extends BrowserControllerReload {
  constructor(controller) {
    super(controller)
  }

  doGetObservedModifications() {
    return {
      [objectType.SPACE]: [
        objectOperation.CREATE,
        objectOperation.DELETE
      ],
      [objectType.PROJECT]: [
        objectOperation.CREATE,
        objectOperation.UPDATE,
        objectOperation.DELETE
      ],
      [objectType.COLLECTION]: [
        objectOperation.CREATE,
        objectOperation.UPDATE,
        objectOperation.DELETE
      ],
      [objectType.OBJECT]: [
        objectOperation.CREATE,
        objectOperation.UPDATE,
        objectOperation.DELETE
      ],
      [objectType.DATA_SET]: [
        objectOperation.CREATE,
        objectOperation.UPDATE,
        objectOperation.DELETE
      ]
    }
  }
}
