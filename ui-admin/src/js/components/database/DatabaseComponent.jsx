import React from 'react'
import Container from '@src/js/components/common/form/Container.jsx'
import AppController from '@src/js/components/AppController.js'
import DataBrowser from '@src/js/components/common/data-browser/DataBrowser.jsx'
import openbis from '@src/js/services/openbis.js'
import objectType from '@src/js/common/consts/objectType.js'
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

import { EntityFormContextProvider } from '@src/js/components/database/new-forms/components/EntityFormContextProvider.tsx';
import { SpaceFormExample } from '@src/js/components/database/new-forms-v2/examples/SpaceFormExample.tsx';
import { FormDispatcher } from '@src/js/components/database/new-forms-v2/core/FormDispatcher.tsx';
import { FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

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
      value: "2",
      datasetTab: "0"
    }
  }

  async componentDidMount() {
    try {
      const { object } = this.props
      console.log('DatabaseComponent.componentDidMount', this.props);
      let json = {}
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
      objectType.DATA_SET,
      id
    )
  }

  imagingDatasetChange(id, changed) {
    console.log('imagingDatasetChange', { id }, { changed });
    AppController.getInstance().objectChange(
      pages.DATABASE,
      objectType.DATA_SET,
      id,
      changed
    )
  }

  handleTabChange(event, value) {
    this.setState({ value })
  }

  handleDatasetTabChange(event, value) {
    this.setState({ datasetTab: value })
  }

  renderImagingDataset(object) {
    const { classes } = this.props
    const { datasetTab } = this.state
    return <Container>
      <TabContext value={datasetTab}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={datasetTab}
            onChange={this.handleDatasetTabChange}
            textColor='secondary'
            indicatorColor='secondary'>
            <Tab label={messages.get(messages.DETAILS)} value="0" />
            <Tab label={messages.get(messages.IMAGES)} value="1" />
          </Tabs>
        </Box>
        <TabPanel classes={{ root: classes.tabsPanel }} value="0">
          {this.renderJson()}
        </TabPanel>
        <TabPanel classes={{ root: classes.tabsPanel }} value="1">
          <ImagingDatasetViewer onUnsavedChanges={this.imagingDatasetChange}
            objId={object.id}
            objType={object.type}
            extOpenbis={openbis}
            showSemanticAnnotations={true} />
        </TabPanel>
      </TabContext>
    </Container>
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
            <Tabs value={value}
              onChange={this.handleTabChange}
              textColor='secondary'
              indicatorColor='secondary'>
              <Tab label={messages.get(messages.DETAILS)} value="2" />
              <Tab label={messages.get(messages.FILES)} value="0" />
              <Tab label={messages.get(messages.IMAGES)} value="1" />
            </Tabs>
          </Box>
          <TabPanel classes={{ root: classes.tabsPanel }} value="0">
            {/* <DataBrowser
              key={object.id}
              id={object.id}
              objId={object.id}
              objKind={object.type}
              kind={object.type}
              viewType='list'
              extOpenbis={openbis}
              onLoadDisplaySettings={this.loadGridSettings}
              onStoreDisplaySettings={this.onGridSettingsChange}
              leftToolbar={true}
            /> */}
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
          <TabPanel classes={{ root: classes.tabsPanel }} value="2">
            {this.renderJson()}
          </TabPanel>
        </TabContext>
      </Container>
    )
  }

  spaceChange(id, changed) {
    console.log('DatabaseComponent.spaceChange', id, changed);
    AppController.getInstance().objectChange(
      pages.DATABASE,
      objectType.SPACE,
      id,
      changed
    )
  }

  handleEntityChange(id, changed) {
    console.log('DatabaseComponent.handleEntityChange.new-forms-v2', id, changed);
    const { object } = this.props;

    // Use the existing spaceChange method for spaces, or create a generic handler
    if (object.type === objectType.SPACE) {
      this.spaceChange(id, changed);
    } else {
      // Generic entity change handler
      AppController.getInstance().objectChange(
        pages.DATABASE,
        object.type,
        id,
        changed
      );
    }
  }

  objectCreate(page, oldType, oldId, newType, newId) {
    console.log('DatabaseComponent.objectCreate', page, oldType, oldId, newType, newId);
    AppController.getInstance().objectCreate(page, oldType, oldId, newType, newId)
  }

  createNewObject(newObjectType, fromObjectType, fromId) {
    console.log('DatabaseComponent.createNewObject', newObjectType, fromObjectType, fromId);
    AppController.getInstance().objectNew(
      pages.DATABASE,
      newObjectType,
      { parentId: fromId, parentType: fromObjectType }
    )
  }

  closeForm(spacePermId) {
    console.log('closeForm for space: ', spacePermId);
    AppController.getInstance().objectClose(
      pages.DATABASE,
      objectType.NEW_PROJECT,
      spacePermId
    )
  }

  // Helper method to map object type to entity type for FormDispatcher
  getEntityTypeFromObjectType(objectType) {
    switch (objectType) {
      case objectType.SPACE:
        return 'SPACE';
      case objectType.PROJECT:
      case objectType.NEW_PROJECT:
        return 'PROJECT';
      case objectType.COLLECTION:
        return 'COLLECTION';
      case objectType.OBJECT:
        return 'SAMPLE';
      case objectType.DATA_SET:
        return 'DATASET';
      default:
        return objectType;
    }
  }

  // Helper method to determine form mode
  getFormMode(object) {
    if (object.type.includes('NEW') || object.type === objectType.NEW_PROJECT) {
      return FormMode.CREATE;
    }
    return FormMode.VIEW;
  }

  // Handle form save
  handleFormSave = (result) => {
    console.log('Form saved:', result);
    // The form data is already handled by the entity-specific controllers
    // This callback can be used for additional post-save logic
  }

  // Handle form cancel
  handleFormCancel = () => {
    console.log('Form cancelled');
    // Navigate back or close the form
    AppController.getInstance().objectClose(
      pages.DATABASE,
      this.props.object.type,
      this.props.object.id
    );
  }

  // Handle form delete
  handleFormDelete = (entityId) => {
    console.log('Form delete requested for entity:', entityId);
    // Handle entity deletion
    AppController.getInstance().objectDelete(
      pages.DATABASE,
      this.props.object.type,
      entityId
    );
  }

  /* renderJson() {
    const { object } = this.props
    console.log('DatabaseComponent.renderJson', { object });
    return (<EntityFormContextProvider openbisFacade={openbis}
      params={object.params}
      entityKind={object.type}
      permId={object.id}
      user={AppController.getInstance().getUser()}
      initialMode={String(object.type).includes('new') ? 'create' : 'view'}
      onEntityChange={this.spaceChange}
      onNewObject={(newObjectType, fromId) => this.createNewObject(newObjectType, object.type, fromId)}
      onObjectCreate={(page, oldType, oldId, newType, newId) => this.objectCreate(page, oldType, oldId, newType, newId)}
      onCloseForm={(spacePermId) => this.closeForm(spacePermId)}
    />)
  } */

  renderJson() {
    const { object } = this.props
    console.log('DatabaseComponent.renderJson.new-forms-v2', { object });
    
    // Map object properties to FormDispatcher props
    const entityType = this.getEntityTypeFromObjectType(object.type);
    const formMode = this.getFormMode(object);
    const user = AppController.getInstance().getUser();
    
    return (
      <FormDispatcher
        entityType={entityType}
        entityId={object.id}
        mode={formMode}
        user={user}
        openbisFacade={openbis}
        onSave={this.handleFormSave}
        onCancel={this.handleFormCancel}
        onDelete={this.handleFormDelete}
      />
    );
  }

  render() {
    logger.log(logger.DEBUG, 'DatabaseComponent.render')
    if (!this.state.json) {
      return null
    }
    const { object } = this.props
    const { properties } = this.state.json
    if (object.type === objectType.DATA_SET && constants.IMAGING_DATA_CONFIG in properties) return this.renderImagingDataset(object)
    return this.state.showDataBrowser ? this.renderDataBrowsers() : this.renderJson()
    //return this.renderJson()
  }
}

export default withStyles(styles)(DatabaseComponent)
