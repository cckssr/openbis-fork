/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
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

import React from 'react'
import { ToggleButton } from '@mui/material';
import messages from '@src/js/common/messages.js'
import InfoIcon from '@mui/icons-material/InfoOutlined'
import IconButton from '@mui/material/IconButton'
import ViewComfyIcon from '@mui/icons-material/ViewComfy'
import ViewListIcon from '@mui/icons-material/ViewList'
import Button from '@mui/material/Button'
import PublishIcon from '@mui/icons-material/Publish'
import Popover from '@mui/material/Popover'
import Container from '@src/js/components/common/form/Container.jsx'
import withStyles from '@mui/styles/withStyles';
import autoBind from 'auto-bind'
import UploadButton from '@src/js/components/database/data-browser/UploadButton.jsx'
import FileUploadManager from '@src/js/components/database/data-browser/FileUploadManager.js'
import FileIcon from '@mui/icons-material/InsertDriveFileOutlined'
import FolderIcon from '@mui/icons-material/FolderOpen'
import logger from '@src/js/common/logger.js'
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx'
import LinearLoadingDialog from '@src/js/components/common/loading/LinearLoadingDialog.jsx';
import FileExistsDialog from '@src/js/components/common/dialog/FileExistsDialog.jsx'
import ErrorDialog from '@src/js/components/common/error/ErrorDialog.jsx'

const color = 'default'
const uploadButtonsColor = 'secondary'
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
  }
})

class RightToolbar extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)

    this.controller = this.props.controller

    this.uploadManager = new FileUploadManager(
      this.controller,
      () => this.state, 
      this.updateStateCallback,
      this.openErrorDialog      
    )

    this.state = {
      uploadButtonsPopup: null,
      loading: false,
      progress: 0,
      allowResume: true,
      uploadFileExistsDialogFile: null,
      processedBytes:0,      
      applyToAllFiles: false,
      totalTransferSize:0,      
      lastConflictResolution:null,
      expectedTime: null,
      lastTimestamp:null,
      averageSpeed:0,
      progressBarFrom:null,
      progressBarTo:null,
      totalUploadSize:0,
      customProgressDetails:null,
      fileName:null,
      loadingDialogVariant:'determinate',
      ...this.uploadManager.getDefaultState(),
    }

    
  }

  updateStateCallback(partialStateOrUpdater){   
    if (typeof partialStateOrUpdater === 'function') {
      this.setState(prev => partialStateOrUpdater(prev))
    } else {
      this.setState(partialStateOrUpdater)
    }
  }

 

  openErrorDialog(errorMessage) {
    this.setState({ errorMessage })
  }

  closeErrorDialog() {
    this.setState({ errorMessage: null })
  }


  renderUploadButtons() {
    const { classes, buttonSize} = this.props
    return (
      <div className={classes.uploadButtonsContainer}>
        <UploadButton
          classes={{ root: classes.button }}
          color={uploadButtonsColor}
          size={buttonSize}
          variant='contained'
          startIcon={<FileIcon />}
          folderSelector={false}
          onClick={this.uploadManager.handleUpload}
        >
          {messages.get(messages.FILE_UPLOAD)}
        </UploadButton>
        <UploadButton
          classes={{ root: classes.button }}
          color={uploadButtonsColor}
          size={buttonSize}
          variant='contained'
          startIcon={<FolderIcon />}
          folderSelector={true}
          onClick={this.uploadManager.handleUpload}
        >
          {messages.get(messages.FOLDER_UPLOAD)}
        </UploadButton>
      </div>
    )
  }

  render() {
    logger.log(logger.DEBUG, 'RightToolbar.render')

    const { classes, onViewTypeChange, buttonSize, editable } = this.props
    const { uploadButtonsPopup,
      progress,
      loading,
      allowResume,
      uploadFileExistsDialogFile,
      fileName,      
      applyToAllFiles,
      errorMessage,      
      expectedTime,
      averageSpeed,
      progressBarFrom,
      progressBarTo,
      processedBytes,
      totalTransferSize,
      progressStatus,
      progressMessage,
      customProgressDetails,
      loadingDialogVariant
    } = this.state
    return ([
      <div key='right-toolbar-main' className={classes.buttons}>
        <ToggleButton
          classes={{ root: classes.toggleButton }}
          color={color}
          size={buttonSize}
          selected={this.props.selected}
          onChange={this.props.onChange}
          value={messages.get(messages.INFO)}
          aria-label={messages.get(messages.INFO)}
        >
          <InfoIcon />
        </ToggleButton>
        {this.props.viewType === 'list' && (
          <IconButton
            classes={{ root: classes.button }}
            color={color}
            size={iconButtonSize}
            variant='outlined'
            onClick={() => onViewTypeChange('grid')}
          >
            <ViewComfyIcon />
          </IconButton>
        )}
        {this.props.viewType === 'grid' && (
          <IconButton
            classes={{ root: classes.button }}
            color={color}
            size={iconButtonSize}
            variant='outlined'
            onClick={() => onViewTypeChange('list')}
          >
            <ViewListIcon />
          </IconButton>
        )}
        <Button
          classes={{ root: classes.button }}
          color={color}
          size={buttonSize}
          variant='outlined'
          disabled={!editable}
          startIcon={<PublishIcon />}
          onClick={this.uploadManager.handleUploadClick}
        >
          {messages.get(messages.UPLOAD)}
        </Button>
        <Popover
          id={'toolbar.columns-popup-id'}
          open={Boolean(uploadButtonsPopup)}
          anchorEl={uploadButtonsPopup}
          onClose={this.uploadManager.handlePopoverClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left'
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left'
          }}
        >
          <Container square={true}>{this.renderUploadButtons()}</Container>
        </Popover>
      </div>,
      <LinearLoadingDialog 
        key='right-toolbar-loaging-dialog' 
        controller={this.controller}
        open={loading}
        title={progressMessage}        
        from={progressBarFrom}
        to={progressBarTo}
        fileName={fileName}
        progress={progress}
        currentSize={processedBytes}
        totalSize={totalTransferSize}
        timeLeft={expectedTime}
        speed={averageSpeed}
        variant={loadingDialogVariant}        
        customProgressDetails={customProgressDetails}        
        progressStatus={progressStatus}
      />,
      <ErrorDialog
        key='right-toolbar-error-dialog'
        open={!!errorMessage}
        error={errorMessage}
        onClose={this.closeErrorDialog}
      />,
      <FileExistsDialog
        key='file-exists-dialog'
        open={!!uploadFileExistsDialogFile}
        onReplace={this.uploadManager.handleFileExistsReplace}
        onResume={allowResume ? this.uploadManager.handleFileExistsResume : null}
        onSkip={this.uploadManager.handleFileExistsSkip}
        onCancel={this.uploadManager.handleFileExistsCancel}
        onApplyToAllChange={this.uploadManager.handleApplyToAllSelection}
        applyToAll={applyToAllFiles}
        title={messages.get(messages.FILE_EXISTS)}        
        content={messages.get(messages.CONFIRMATION_FILE_NAME_CONFLICT,
          uploadFileExistsDialogFile?.name)}
      />
    ])
  }
}

export default withStyles(styles)(RightToolbar)
