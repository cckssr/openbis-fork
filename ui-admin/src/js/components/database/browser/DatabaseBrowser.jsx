import _ from 'lodash'
import React from 'react'
import autoBind from 'auto-bind'
import BrowserWithOpenbis from '@src/js/components/common/browser/BrowserWithOpenbis.jsx'
import DatabaseBrowserController from '@src/js/components/database/browser/DatabaseBrowserController.js'
import AppController from '@src/js/components/AppController.js'
import pages from '@src/js/common/consts/pages.js'
import logger from '@src/js/common/logger.js'

class DatabaseBrowser extends React.Component {
  constructor(props) {
    super(props)
    autoBind(this)
    this.controller = this.props.controller || new DatabaseBrowserController()
  }

  componentDidMount() {
    this.componentDidUpdate({})
  }

  componentDidUpdate(prevProps) {
    if (!_.isEqual(this.props.selectedObject, prevProps.selectedObject)) {
      this.controller.selectObject(this.props.selectedObject)
    }

    if (
      !_.isEqual(
        this.props.lastObjectModifications,
        prevProps.lastObjectModifications
      )
    ) {
      this.controller.reload(this.props.lastObjectModifications)
    }
  }

  render() {
    logger.log(logger.DEBUG, 'DatabaseBrowser.render')
    return <BrowserWithOpenbis controller={this.controller} />
  }
}

export default AppController.getInstance().withState(() => ({
  selectedObject: AppController.getInstance().getSelectedObject(pages.DATABASE),
  lastObjectModifications: AppController.getInstance().getLastObjectModifications()
}))(DatabaseBrowser)
