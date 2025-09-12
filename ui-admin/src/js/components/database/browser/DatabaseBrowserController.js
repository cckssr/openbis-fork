import AppController from '@src/js/components/AppController.js'
import BrowserController from '@src/js/components/common/browser/BrowserController.js'
import DatabaseBrowserControllerLoadNodePath from '@src/js/components/database/browser/DatabaseBrowserControllerLoadNodePath.js'
import DatabaseBrowserControllerLoadNodesFiltered from '@src/js/components/database/browser/DatabaseBrowserControllerLoadNodesFiltered.js'
import DatabaseBrowserControllerLoadNodesUnfiltered from '@src/js/components/database/browser/DatabaseBrowserControllerLoadNodesUnfiltered.js'
import DatabaseBrowserControllerAddNode from '@src/js/components/database/browser/DatabaseBrowserControllerAddNode.js'
import DatabaseBrowserControllerRemoveNode from '@src/js/components/database/browser/DatabaseBrowserControllerRemoveNode.js'
import DatabaseBrowserControllerReload from '@src/js/components/database/browser/DatabaseBrowserControllerReload.js'
import pages from '@src/js/common/consts/pages.js'
import ids from '@src/js/common/consts/ids.js'

export default class DatabaseBrowserController extends BrowserController {
  getId() {
    return ids.DATABASE_BROWSER_ID
  }

  async loadNodePath(params) {
    return await new DatabaseBrowserControllerLoadNodePath().doLoadNodePath(
      params
    )
  }

  async loadNodes(params) {
    const { filter } = params

    if (filter) {
      return await new DatabaseBrowserControllerLoadNodesFiltered(
        this
      ).doLoadFilteredNodes(params)
    } else {
      return await new DatabaseBrowserControllerLoadNodesUnfiltered().doLoadUnfilteredNodes(
        params
      )
    }
  }

  async reload(objectModifications) {
    new DatabaseBrowserControllerReload(this).reload(objectModifications)
  }

  onSelectedChange({ object }) {
    if (object) {
      AppController.getInstance().objectOpen(
        pages.DATABASE,
        object.type,
        object.id,
        object.params
      )
    }
  }

  canAddNode() {
    return new DatabaseBrowserControllerAddNode().canAddNode(
      this.getSelectedObject()
    )
  }

  async addNode() {
    await new DatabaseBrowserControllerAddNode().doAddNode(this.getSelectedObject())
  }

  canRemoveNode() {
    return new DatabaseBrowserControllerRemoveNode().canRemoveNode(
      this.getSelectedObject()
    )
  }

  async removeNode() {
    await new DatabaseBrowserControllerRemoveNode().doRemoveNode(
      this.getSelectedObject()
    )
  }
}
