import React from 'react'
import Container from '@src/js/components/common/form/Container.jsx'
import AppController from '@src/js/components/AppController.js'
import DataBrowser from '@src/js/components/common/data-browser/DataBrowser.jsx'
import openbis from '@src/js/services/openbis.js'
import objectType from '@src/js/common/consts/objectType.js'
import objectTypes from '@src/js/common/consts/objectType.js'
import logger from '@src/js/common/logger.js'
import constants from '@src/js/components/common/imaging/constants.js'
import pages from '@src/js/common/consts/pages'
import ImagingGalleryViewer from '@src/js/components/common/imaging/ImagingGalleryViewer.jsx'
import ImagingDatasetViewer from '@src/js/components/common/imaging/ImagingDatasetViewer.jsx'
import Tabs from '@mui/material/Tabs'
import Tab from '@mui/material/Tab'
import Box from '@mui/material/Box'
import { TabContext, TabPanel } from '@mui/lab'
import autoBind from 'auto-bind'
import withStyles from '@mui/styles/withStyles';
import messages from '@src/js/common/messages.js'

const styles = theme => ({
  tabsPanel: {
    padding: "0"
  }
})

class DatabaseComponent extends React.PureComponent {
  constructor(props) {
    super(props)

    autoBind(this)

    this.state = {
      json: null,
      showDataBrowser: false,
      value: "0"
    }
  }

  async componentDidMount() {
    try {
      const { object } = this.props

      let json = null
      let showDataBrowser = false
      if (object.type === objectType.SPACE) {
        const spaces = await openbis.getSpaces(
          [new openbis.SpacePermId(object.id)],
          new openbis.SpaceFetchOptions()
        )
        json = spaces[object.id]
      } else if (object.type === objectType.PROJECT) {
        const projects = await openbis.getProjects(
          [new openbis.ProjectPermId(object.id)],
          new openbis.ProjectFetchOptions()
        )
        json = projects[object.id]
      } else if (object.type === objectType.COLLECTION) {
        const fetchOptions = new openbis.ExperimentFetchOptions()
        fetchOptions.withProperties()
        fetchOptions.withDataSets().withProperties()
        const experiments = await openbis.getExperiments(
          [new openbis.ExperimentPermId(object.id)],
          fetchOptions
        )
        json = experiments[object.id]
        showDataBrowser = openbis.isAfsSet()
      } else if (object.type === objectType.OBJECT) {
        const fetchOptions = new openbis.SampleFetchOptions()
        fetchOptions.withSpace()
        fetchOptions.withProject()
        fetchOptions.withExperiment()
        fetchOptions.withParents()
        fetchOptions.withProperties()
        fetchOptions.withDataSets().withProperties()
        const samples = await openbis.getSamples(
          [new openbis.SamplePermId(object.id)],
          fetchOptions
        )
        json = samples[object.id]
        showDataBrowser = openbis.isAfsSet()
      } else if (object.type === objectType.DATA_SET) {
        const fetchOptions = new openbis.DataSetFetchOptions()
        fetchOptions.withExperiment()
        fetchOptions.withSample()
        fetchOptions.withParents()
        fetchOptions.withProperties()
        const dataSets = await openbis.getDataSets(
          [new openbis.DataSetPermId(object.id)],
          fetchOptions
        )
        json = dataSets[object.id]
      }

      this.setState({
        json,
        showDataBrowser
      })
    } catch (error) {
      AppController.getInstance().errorChange(error)
    }
  }

  datasetOpenTab(id) {
    AppController.getInstance().objectOpen(
      pages.DATABASE,
      objectTypes.DATA_SET,
      id
    )
  }

  imagingDatasetChange(id, changed) {
    AppController.getInstance().objectChange(
      pages.DATABASE,
      objectTypes.DATA_SET,
      id,
      changed
    )
  }

  handleTabChange(event, value) {
    this.setState({ value })
  }

  renderImagingDataset(object) {
    return <ImagingDatasetViewer onUnsavedChanges={this.imagingDatasetChange}
        objId={object.id}
        objType={object.type}
        extOpenbis={openbis} />
  }

  getGridSettingsId() {
    return "ata-browser-grid"
  }

  async loadGridSettings() {
    const settingsId = this.getGridSettingsId()

    if (!settingsId) {
      return null
    }

    return await AppController.getInstance().getSetting(settingsId)
  }

  async onGridSettingsChange(settings) {
    const settingsId = this.getGridSettingsId()

    if (!settingsId) {
      return
    }

    await AppController.getInstance().setSetting(settingsId, settings)
  }

  renderDataBrowsers() {
    const { object, classes } = this.props
    const { value } = this.state
    return (
      <Container>
        <TabContext value={value}>
          <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
            <Tabs value={value} onChange={this.handleTabChange} 
            textColor='secondary'
            indicatorColor='secondary'>
              <Tab label={messages.get(messages.FILES)} value="0" />
              <Tab label={messages.get(messages.IMAGES)} value="1" />
            </Tabs>
          </Box>
          <TabPanel classes={{ root: classes.tabsPanel }} value="0">
            <DataBrowser
              key={object.id}
              id={object.id}
              objId={object.id}
              objKind={object.type}
              kind={object.type}
              viewType='list'
              extOpenbis={openbis}
              onLoadDisplaySettings={this.loadGridSettings}
              onStoreDisplaySettings={this.onGridSettingsChange}
              sessionToken={AppController.getInstance().getSessionToken()}
            />
          </TabPanel>
          <TabPanel classes={{ root: classes.tabsPanel }} value="1">
            {(object.type === objectType.COLLECTION
              || object.type === objectType.OBJECT)
              && <ImagingGalleryViewer onStoreDisplaySettings={null}
                onLoadDisplaySettings={null}
                onOpenPreview={this.datasetOpenTab}
                objId={object.id}
                objType={object.type}
                extOpenbis={openbis} />}
          </TabPanel>
        </TabContext>
      </Container>
    )
  }

  renderJson() {
    return (
      <Container>
        <pre>{JSON.stringify(this.state.json || {}, null, 2)}</pre>
      </Container>
    )
  }

  render() {
    logger.log(logger.DEBUG, 'DatabaseComponent.render')
    if (!this.state.json) {
      return null
    }
    const { object } = this.props
    const { properties } = this.state.json
    if (object.type === objectType.DATA_SET && constants.IMAGING_DATA_CONFIG in properties) return this.renderImagingDataset(object)
    return this.state.showDataBrowser ? this.renderDataBrowsers()
      : this.renderJson()
  }
}

export default withStyles(styles)(DatabaseComponent)
