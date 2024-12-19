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
import FileIcon from '@mui/icons-material/InsertDriveFileOutlined'
import FolderIcon from '@mui/icons-material/FolderOpen'
import logger from '@src/js/common/logger.js'
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx'
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
    this.resolveConflict = null // This function will be shared

    this.state = {
      uploadButtonsPopup: null,
      loading: false,
      progress: 0,
      allowResume: true,
      fileExistsDialogFile: null,
      uploadedBytes:0,
      applyToAllFiles: false,
      totalBytesToUpload:0,
      lastConflictResolution:null

    }
  }

  async handleUpload(event) {
    try {
      this.handlePopoverClose()      
      this.setState({ loading: true, progress: 0 })
      
      const fileList = event.target.files
      const totalSize = Array.from(fileList)
      .reduce((acc, file) => acc + file.size, 0)

      this.setState({ totalBytesToUpload: totalSize})

      await this.upload(fileList,totalSize)
    } catch(err){
      console.error(err)
      this.openErrorDialog(
        `Error uploading ${[...event.target.files].map(file => file.name).join(", ")}: ` + (err.message || err)
      );
    } finally {
      this.resetUploadDialogStates()
    }
  }

  resetUploadDialogStates() {
    this.setState({
      loading: false,
      uploadedBytes:0,
      totalBytesToUpload:0,
      applyToAllFiles: false,
      cancelDownload: false,
      allowResume: false,
      progress:0,
      lastConflictResolution: null
    })
  }


  updateProgress(uploadedChunkSize, fileName, speed) {
    this.setState((prevState) => {
      const uploadedBytes = prevState.uploadedBytes + uploadedChunkSize;
      const progress = Math.round((uploadedBytes / prevState.totalBytesToUpload) * 100);
      const newProgress = Math.min(progress, 100);
      const speedFormatted = this.controller.formatSpeed(speed);
  
      return {
        uploadedBytes,
        progress: newProgress,
        loading: true,
        progressDetailPrimary: fileName,
        progressDetailSecondary: speedFormatted
      };
    });
  }
  


  async resolveNameConflict(newFile, allowResume) {
    return new Promise((resolve) => {
      this.setState({ allowResume, loading: false, progress: 0 })
      this.openFileExistsDialog(newFile)
      this.resolveConflict = resolve
    })
  }

  openFileExistsDialog(newFile) {
    this.setState({ fileExistsDialogFile: newFile })
  }

  closeFileExistsDialog() {
    this.setState({ fileExistsDialogFile: null })
  }

  handleUploadClick(event) {
    this.setState({
      uploadButtonsPopup: event.currentTarget
    })
  }

  handlePopoverClose() {
    this.setState({
      uploadButtonsPopup: null
    })
  }

  handleFileExistsReplace() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict('replace')
    this.setState({ loading: true, progress: 0,lastConflictResolution: 'replace'})
  }

  handleFileExistsResume() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict('resume')
    this.setState({ loading: true, progress: 0 ,lastConflictResolution: 'resume'})
  }

  handleFileExistsSkip() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict('skip')
    this.setState({ loading: true, progress: 0 ,lastConflictResolution: 'skip'})
  }

  handleFileExistsCancel() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict('cancel')
    this.setState({ loading: false,lastConflictResolution: 'cancel' })
  }

  handleApplyToAllSelection(checked) {    
    this.setState({ applyToAllFiles: checked })    
  }

  async upload(fileList) {      

    for (const file of fileList) {
      const filePath = file.webkitRelativePath ? file.webkitRelativePath
        : file.name
      const targetFilePath = this.controller.path + '/' + filePath
      const existingFiles = await this.controller.listFiles(targetFilePath)

      const fileExists = existingFiles.length > 0
      const existingFileSize = existingFiles.length === 0 ? 0
        : existingFiles[0].size

      let offset = 0;

      if (fileExists) {
        const resolution = await this.handleExistingFile(file, existingFiles, existingFileSize);
        if (resolution === 'cancel') return; // Cancel uploading entirely
        if (resolution === 'skip') continue; // Skip this file and continue with others
        offset = resolution === 'replace' ? 0 : existingFileSize;// handles resume also                
      }

      // Replace or resume upload from the last point in the file
      while (offset < file.size) {      
        const sizeUploaded = await this.controller.uploadFile(file, targetFilePath,
          offset, this.updateProgress)
        offset += sizeUploaded
      }
    }

    this.updateProgress(0, "Finalizing...", "Finalizing...")
    if (this.controller.gridController) {
      await this.controller.gridController.load()
    }
  }   


  async handleExistingFile(file, existingFiles, existingFileSize) {
    var resolutionResult = this.state.lastConflictResolution;
    
    if (!this.state.applyToAllFiles) {      
      resolutionResult = await this.resolveNameConflict(file, true);
    }

    if (resolutionResult === 'skip') {
      this.state.skipFile = true;
      this.updateProgress(file.size, file.name, "Skipping...");
      return 'skip';
    }

    if (resolutionResult === 'resume') {        
      this.updateProgress(existingFileSize, file.name, "Resuming...");
      return 'resume';
    }

    if (resolutionResult === 'cancel') {
      this.updateProgress(file.size, file.name, "Cancelling...");
      return 'cancel';
    }

    return "replace";
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
          onClick={this.handleUpload}
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
          onClick={this.handleUpload}
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
      fileExistsDialogFile,
      progressDetailPrimary,
      progressDetailSecondary,
      applyToAllFiles,
      errorMessage,
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
          onClick={this.handleUploadClick}
        >
          {messages.get(messages.UPLOAD)}
        </Button>
        <Popover
          id={'toolbar.columns-popup-id'}
          open={Boolean(uploadButtonsPopup)}
          anchorEl={uploadButtonsPopup}
          onClose={this.handlePopoverClose}
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
      <LoadingDialog key='right-toolbar-loaging-dialog' variant='determinate'
        value={progress}
        loading={loading}
        message={messages.get(messages.UPLOADING)}
        showBackground='true'
        detailPrimary={progressDetailPrimary}
        detailSecondary={progressDetailSecondary}
        />,
        <ErrorDialog
          key='right-toolbar-error-dialog'
          open={!!errorMessage}
          error={errorMessage}
          onClose={this.closeErrorDialog}
        />,
        <FileExistsDialog
          key='file-exists-dialog'
          open={!!fileExistsDialogFile}
          onReplace={this.handleFileExistsReplace}
          onResume={allowResume ? this.handleFileExistsResume : null}
          onSkip={this.handleFileExistsSkip}
          onCancel={this.handleFileExistsCancel}
          onApplyToAllChange={this.handleApplyToAllSelection}
          applyToAll={applyToAllFiles}
          title={messages.get(messages.FILE_EXISTS)}        
          content={messages.get(messages.CONFIRMATION_FILE_NAME_CONFLICT,
            fileExistsDialogFile ? fileExistsDialogFile.name : '')}
        />
    ])
  }
}

export default withStyles(styles)(RightToolbar)
