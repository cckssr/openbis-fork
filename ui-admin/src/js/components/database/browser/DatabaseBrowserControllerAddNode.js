import AppController from '@src/js/components/AppController.js'
import objectType from '@src/js/common/consts/objectType.js'
import pages from '@src/js/common/consts/pages.js'

const NEW_OBJECTS = {
  [objectType.SPACE]: objectType.NEW_SPACE,
  [objectType.PROJECT]: objectType.NEW_PROJECT,
  [objectType.COLLECTION]: objectType.NEW_COLLECTION,
  [objectType.OBJECT]: objectType.NEW_OBJECT,
  [objectType.DATA_SET]: objectType.NEW_DATA_SET,
}

export default class DatabaseBrowserControllerAddNode {
  canAddNode(selectedObject) {
    return (
      selectedObject &&
      selectedObject.type === objectType.OVERVIEW &&
      NEW_OBJECTS[selectedObject.id]
    )
  }

  async doAddNode(selectedObject) {
    if (!this.canAddNode(selectedObject)) {
      return
    }
    await AppController.getInstance().objectNew(
      pages.DATABASE,
      NEW_OBJECTS[selectedObject.id]
    )
  }
}
