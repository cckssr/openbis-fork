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
import { Button, Popover } from '@mui/material'
import Container from '@src/js/components/common/form/Container.jsx'
import PublishIcon from '@mui/icons-material/Publish'
import UploadButton from '@src/js/components/common/data-browser/components/upload/UploadButton.jsx'
import DriveFolderUpload from '@mui/icons-material/DriveFolderUploadSharp'
import UploadFile from '@mui/icons-material/UploadFileSharp'
import LinearLoadingDialog from '@src/js/components/common/loading/LinearLoadingDialog.jsx'
import ErrorDialog from '@src/js/components/common/error/ErrorDialog.jsx'
import FileExistsDialog from '@src/js/components/common/dialog/FileExistsDialog.jsx'
import messages from '@src/js/common/messages.js'
import FileUploadManager from '@src/js/components/common/data-browser/components/upload/FileUploadManager.js'
import clsx from 'clsx';

const uploadButtonsColor = ''
const color = 'primary'

class UploadSection extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      uploadButtonsPopup: null,
      loading: false,
      progress: 0,
      allowResume: true,
      uploadFileExistsDialogFile: null,
      processedBytes: 0,
      applyToAllFiles: false,
      totalTransferSize: 0,
      expectedTime: null,
      averageSpeed: 0,
      progressBarFrom: null,
      progressBarTo: null,
      fileName: null,
      loadingDialogVariant: 'determinate',
      errorMessage: null
    }

    this.uploadManager = new FileUploadManager(
      props.controller,
      () => this.state,
      this.updateStateCallback.bind(this),
      this.openErrorDialog.bind(this),
      props.afterUpload
    )

    this.handleUploadButtonClick = this.handleUploadButtonClick.bind(this)
    this.handlePopoverClose = this.handlePopoverClose.bind(this)
    this.renderUploadButtons = this.renderUploadButtons.bind(this)
    this.cancelFileTransfer = this.cancelFileTransfer.bind(this)
    this.closeErrorDialog = this.closeErrorDialog.bind(this)
  }

  updateStateCallback(partialStateOrUpdater) {
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

  cancelFileTransfer() {
    this.setState({ cancelTransfer: true })
  }

  handleUploadButtonClick(event) {
    this.setState({ uploadButtonsPopup: event.currentTarget })
    if (this.props.onUploadClick) {
      this.props.onUploadClick()
    }
  }

  handlePopoverClose() {
    this.setState({ uploadButtonsPopup: null })
    if (this.props.onPopoverClose) {
      this.props.onPopoverClose()
    }
  }

  renderUploadButtons() {
    const { classes, buttonSize, className} = this.props
    return (
      <div className={classes.uploadButtonsContainer}>
        <UploadButton
          className={clsx(classes.button, className)}
          color={className ? '' : uploadButtonsColor}
          size={buttonSize}
          variant="contained"
          startIcon={<UploadFile />}
          folderSelector={false}
          onClick={this.uploadManager.handleUpload}
        >
          {messages.get(messages.FILE_UPLOAD)}
        </UploadButton>
        <UploadButton
          className={clsx(classes.button, className)}
          color={className ? '' : uploadButtonsColor}
          size={buttonSize}
          variant="contained"
          startIcon={<DriveFolderUpload />}
          folderSelector={true}
          onClick={this.uploadManager.handleUpload}
        >
          {messages.get(messages.FOLDER_UPLOAD)}
        </UploadButton>
      </div>
    )
  }

  render() {
    const { classes, buttonSize, editable, controller, className, primaryClassName, frozen } = this.props
    const {
      uploadButtonsPopup,
      loading,
      progress,
      uploadFileExistsDialogFile,
      processedBytes,
      totalTransferSize,
      expectedTime,
      averageSpeed,
      progressBarFrom,
      progressBarTo,
      fileName,
      loadingDialogVariant,
      errorMessage      
    } = this.state

    return (
      <>
        <Button
          className={clsx(classes.button, classes.primaryButton, primaryClassName)}
          color={primaryClassName ? '' : color }
          size={buttonSize}
          variant={className ?  '' : "contained"}
          disabled={!editable || frozen}
          startIcon={<PublishIcon />}
          onClick={this.uploadManager.handleUploadClick}
        >
          {messages.get(messages.UPLOAD)}
        </Button>
        <Popover
          id="toolbar.columns-popup-id"
          open={Boolean(uploadButtonsPopup)}
          anchorEl={uploadButtonsPopup}
          onClose={this.uploadManager.handlePopoverClose || this.handlePopoverClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left'
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left'
          }}
        >
          <Container square>{this.renderUploadButtons()}</Container>
        </Popover>
        <LinearLoadingDialog
          controller={controller}
          open={loading}
          title={this.state.progressMessage}
          from={progressBarFrom}
          to={progressBarTo}
          fileName={fileName}
          progress={progress}
          currentSize={processedBytes}
          totalSize={totalTransferSize}
          timeLeft={expectedTime}
          speed={averageSpeed}
          variant={loadingDialogVariant}
          customProgressDetails={this.state.customProgressDetails}
          progressStatus={this.state.progressStatus}
          onCancel={this.cancelFileTransfer}
        />
        <ErrorDialog
          open={!!errorMessage}
          error={errorMessage}
          onClose={this.closeErrorDialog}
        />
        <FileExistsDialog
          open={!!uploadFileExistsDialogFile}
          onReplace={this.uploadManager.handleFileExistsReplace}
          onResume={this.state.allowResume ? this.uploadManager.handleFileExistsResume : null}
          onSkip={this.state.allowSkip ? this.uploadManager.handleFileExistsSkip : null}
          onCancel={this.uploadManager.handleFileExistsCancel}
          onApplyToAllChange={this.state.showApplyToAll ? this.uploadManager.handleApplyToAllSelection : null}
          applyToAll={this.state.applyToAllFiles}
          title={messages.get(messages.FILE_EXISTS)}
          content={messages.get(
            messages.CONFIRMATION_FILE_NAME_CONFLICT,
            uploadFileExistsDialogFile?.name
          )}
        />
      </>
    )
  }
}

export default UploadSection
