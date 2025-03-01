/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
import React from 'react';
import withStyles from '@mui/styles/withStyles';
import autoBind from 'auto-bind';
import logger from '@src/js/common/logger.js';
import InfoToggleButton from '@src/js/components/common/data-browser/components/toolbar/InfoToggleButton.jsx';
import ViewSwitch from '@src/js/components/common/data-browser/components/toolbar/ViewSwitch.jsx';
import UploadSection from '@src/js/components/common/data-browser/components/toolbar/UploadSection.jsx';
import LeftToolbarButtons from '@src/js/components/common/data-browser/components/toolbar/LeftToolbarButtons.jsx'
import ResizeObserver from 'rc-resize-observer';
import DataBrowserController from '@src/js/components/common/data-browser/DataBrowserController.js'
import eventBus from "@src/js/components/common/data-browser/eventBus.js";
import ResponsiveLeftToolbar from '@src/js/components/common/data-browser/components/toolbar/ResponsiveLeftToolbar.jsx'
import RightToolbar  from '@src/js/components/common/data-browser/components/toolbar/RightToolbar.jsx'

import { debounce } from '@mui/material'


const buttonSize = 'small'
const color = 'default'
const iconButtonSize = 'medium'

const styles = theme => ({
  buttons: {
    flex: '0 0 auto',
    display: 'flex',
    alignItems: 'center',
    whiteSpace: 'nowrap',
    '&>button': {
      marginRight: theme.spacing(1)
    },
    '&>button:nth-last-child(1)': {
      marginRight: 0
    }
  },
  uploadButtonsContainer: {
    display: 'flex',
    flexDirection: 'column',
    '&>button': {
      marginBottom: theme.spacing(1)
    },
    '&>button:nth-last-child(1)': {
      marginBottom: 0
    }
  },
  toggleButton: {
    border: 'none',
    borderRadius: '50%',
    display: 'inline-flex',
    padding: theme.spacing(1.5),
    '& *': {
      color: theme.palette[color].main
    }
  },
  splitToolbar: {
    flex: '1 1 auto',
    display: 'flex',
    whiteSpace: 'nowrap',
    marginLeft: theme.spacing(1),
    marginRight: theme.spacing(1),
    justifyContent: 'flex-start'
  },
})

class DataBrowserToolbar extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)

    const {owner, extOpenbis } = this.props  
  
    this.controller = new DataBrowserController(owner, extOpenbis)
    this.controller.attach(this)
    this.controller.setPath("/");

    this.state = { 
      width: 0,
      multiselectedFiles:[],
      viewType: this.props.viewType,
    };
    this.onResize = debounce(this.onResize, 1)
  }

  onResize({ width }) {
    if (width !== this.state.width) {
      this.setState({ width :width - 200});
    }
  }

  onGridActionComplete(){
    eventBus.emit('gridActionCompleted', {});
  }

  fetchSpaceStatus(){        
    eventBus.emit('spaceStatusChanged', {});
  }

  async handleDownload() {
    eventBus.emit('downloadRequested', {});
  }

  componentDidMount() {    
    eventBus.on('selectionChanged', this.handleSelectionChanged);
    eventBus.on('pathChanged', this.handlePathChanged);
    eventBus.on('rightsChanged', this.handleRightsChanged);    
  }

  componentWillUnmount() {    
    eventBus.off('selectionChanged', this.handleSelectionChanged);
    eventBus.off('pathChanged', this.handlePathChanged);
    eventBus.off('rightsChanged', this.handleRightsChanged);    
  }

  handleSelectionChanged({multiselectedFiles}){
    this.setState({ multiselectedFiles });
  }

  
  handleRightsChanged({editable}){
    this.setState({ editable });
  }

  handlePathChanged({path}) {

    const newPath = path || '/';
    
    if (newPath !== (this.state.path)) {
      const formattedPath = newPath.endsWith('/') ? newPath : newPath + '/';
      if (this.state.path !== formattedPath) {
        this.setState({ path: formattedPath });
        this.controller.setPath(formattedPath);
      }
    }
  }

  handleViewTypeChange(viewType){
    this.setState({viewType})
    eventBus.emit('viewTypeChanged', {viewType});
  }

  handleShowInfoChange(showInfo){
    eventBus.emit('showInfoChanged', {showInfo});
  } 


  renderSplitToolbar() { logger.log(logger.DEBUG, 'Toolbar.render')

    const {
      classes,                 
      owner,  
      extOpenbis    
    } = this.props

    const {
      path,
      multiselectedFiles,
      editable,
      showInfo,
      viewType, 
    } = this.state 
            

    return (      
      <div className={classes.splitToolbar }>
        <ResponsiveLeftToolbar
          buttonSize={buttonSize}                        
          owner={owner} 
          path={path}
          multiselectedFiles={multiselectedFiles}
          editable={editable}         
          extOpenbis={extOpenbis}            
          onGridActionComplete={this.onGridActionComplete}
          spaceStatusChanged={this.fetchSpaceStatus}
          onDownload={this.handleDownload}
        />
        
        <RightToolbar
          buttonSize={buttonSize}
          selected={showInfo}
          onChange={this.handleShowInfoChange}
          viewType={viewType}
          editable={editable}
          onViewTypeChange={this.handleViewTypeChange}
          controller={this.controller}
          afterUpload={this.fetchSpaceStatus}
        />
      </div>
    )
  }

  renderUnifiedToolbar() {
    const {
      classes,            
      buttonSize,                
      owner,
      extOpenbis
    } = this.props

    const {
      path,
      multiselectedFiles,
      editable,
      showInfo,
      width,
      viewType, 
    } = this.state 

    return (
      <ResizeObserver onResize={this.onResize}>
        <div className={classes.buttons}>

          <UploadSection
            classes={classes}
            buttonSize={buttonSize}
            editable={editable}
            />

          <LeftToolbarButtons
            buttonSize={buttonSize}                        
            owner={owner} 
            path={path}
            multiselectedFiles={multiselectedFiles}
            editable={editable}         
            extOpenbis={extOpenbis}
            width={width}
            onGridActionComplete={this.onGridActionComplete}
            spaceStatusChanged={this.fetchSpaceStatus}
            onDownload={this.handleDownload}
            />
          
          <ViewSwitch
            viewType={viewType}
            onViewTypeChange={this.handleViewTypeChange}
            buttonSize={buttonSize}
            classes={classes}
            />

          <InfoToggleButton
            selected={showInfo}            
            onChange={this.handleShowInfoChange}
            buttonSize={buttonSize}
            classes={classes}
            />
            
        </div>
      </ResizeObserver>
    )
  }

    
  render() {
    return this.props.toolbarType === "unifiedToolbar"
      ? this.renderUnifiedToolbar()
      : this.renderSplitToolbar();
  }
  
}

export default withStyles(styles)(DataBrowserToolbar)