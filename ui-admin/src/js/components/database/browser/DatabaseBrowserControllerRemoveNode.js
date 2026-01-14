import AppController from '@src/js/components/AppController.js'
import objectType from '@src/js/common/consts/objectType.js'
import openbis from '@src/js/services/openbis.js'
import pages from '@src/js/common/consts/pages.js'

const REMOVABLE_OBJECTS = [
  objectType.SPACE,
  objectType.PROJECT,
  objectType.COLLECTION,  
  objectType.OBJECT,
  objectType.DATA_SET
]

export default class DatabaseBrowserControllerRemoveNode {
  canRemoveNode(selectedObject) {
    return (
      selectedObject && REMOVABLE_OBJECTS.includes(selectedObject.type)
    )
  }

  async doRemoveNode(selectedObject) {
    if (!this.canRemoveNode(selectedObject)) {
      return
    } else {
      const { type, id } = selectedObject
      console.log('[DatabaseBrowserControllerRemoveNode] Removing node: ', { type, id })
    }

  }
}
