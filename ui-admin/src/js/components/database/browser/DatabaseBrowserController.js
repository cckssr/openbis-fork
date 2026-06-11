import _ from 'lodash'
import AppController from '@src/js/components/AppController.js'
import BrowserController from '@src/js/components/common/browser/BrowserController.js'
import DatabaseBrowserControllerLoadNodePath from '@src/js/components/database/browser/DatabaseBrowserControllerLoadNodePath.js'
import DatabaseBrowserControllerLoadNodesFiltered from '@src/js/components/database/browser/DatabaseBrowserControllerLoadNodesFiltered.js'
import DatabaseBrowserControllerLoadNodesUnfiltered from '@src/js/components/database/browser/DatabaseBrowserControllerLoadNodesUnfiltered.js'
import DatabaseBrowserControllerReload from '@src/js/components/database/browser/DatabaseBrowserControllerReload.js'
import objectType from '@src/js/common/consts/objectType.js'
import pages from '@src/js/common/consts/pages.js'
import ids from '@src/js/common/consts/ids.js'

const OBJECT_TYPES = [objectType.SPACE, objectType.PROJECT, objectType.COLLECTION, objectType.OBJECT, objectType.DATA_SET]

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

  async selectObject(nodeObject, event) {
    if (!_.isNil(nodeObject) && nodeObject.type === objectType.SPACES) {
      AppController.getInstance().objectOpen(pages.DATABASE, objectType.SPACES, 'spaces')
      return
    }
    if (!_.isNil(nodeObject) && OBJECT_TYPES.includes(nodeObject.type)) {
      await super.selectObject(nodeObject, event);
    } else {
      await super.selectObject(null);
    }
  }

  onSelectedChange({ object }) {
    if (object) {
      AppController.getInstance().objectOpen(
        pages.DATABASE,
        object.type,
        object.id
      )
    }
  }

}
