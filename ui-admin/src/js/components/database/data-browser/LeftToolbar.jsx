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
import ResizeObserver from 'rc-resize-observer'
import Button from '@mui/material/Button'
import CreateNewFolderIcon from '@mui/icons-material/CreateNewFolderOutlined'
import DeleteIcon from '@mui/icons-material/Delete'
import RenameIcon from '@mui/icons-material/Create'
import CopyIcon from '@mui/icons-material/FileCopy'
import MoveIcon from '@mui/icons-material/ArrowRightAlt'
import MoreIcon from '@mui/icons-material/MoreVert'
import messages from '@src/js/common/messages.js'
import withStyles from '@mui/styles/withStyles';
import logger from '@src/js/common/logger.js'
import autoBind from 'auto-bind'
import IconButton from '@mui/material/IconButton'
import { debounce } from '@mui/material'
import Container from '@src/js/components/common/form/Container.jsx'
import Popover from '@mui/material/Popover'
import InputDialog from '@src/js/components/common/dialog/InputDialog.jsx'
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx'
import LocationDialog from '@src/js/components/database/data-browser/LocationDialog.jsx'
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx'
import Download from '@src/js/components/database/data-browser/Download.jsx'

const color = 'inherit'
const iconButtonSize = 'medium'
const moveLocationMode = 'move'
const copyLocationMode = 'copy'
const VALID_FILENAME_PATTERN = /^[0-9A-Za-z $!#%'()+,\-.;=@[\]^_{}~]+$/;

const styles = theme => ({
  buttons: {
    flex: '1 1 auto',
    display: 'flex',
    alignItems: 'center',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    '&>button': {
      marginRight: theme.spacing(1)
    },
    '&>button:nth-last-child(1)': {
      marginRight: 0
    }
  },
  toggleButton: {},
  collapsedButtonsContainer: {
    display: 'flex',
    flexDirection: 'column',
    '&>button': {
      marginBottom: theme.spacing(1)
    },
    '&>button:nth-last-child(1)': {
      marginBottom: 0
    }
  },
})

class LeftToolbar extends React.Component {

  constructor(props, context) {
    super(props, context)
    autoBind(this)

    this.state = {
      width: 0,
      hiddenButtonsPopup: null,
      newFolderDialogOpen: false,
      deleteDialogOpen: false,
      renameDialogOpen: false,
      locationDialogMode: null,
      loading: false,
      renameError: false
    }

    this.controller = this.props.controller
    this.onResize = debounce(this.onResize, 1)
  }

  async handleNewFolderCreate(folderName) {
    this.closeNewFolderDialog()
    if (folderName && folderName.trim()) {
      await this.controller.createNewFolder(folderName.trim())
    }
  }

  handleNewFolderCancel() {
    this.closeNewFolderDialog()
  }

  openNewFolderDialog() {
    this.setState({ newFolderDialogOpen: true })
  }

  closeNewFolderDialog() {
    this.setState({ newFolderDialogOpen: false })
  }

  openDeleteDialog() {
    this.setState({ deleteDialogOpen: true })
  }

  closeDeleteDialog() {
    this.setState({ deleteDialogOpen: false })
  }

  openRenameDialog() {
    this.setState({ renameError: false, renameDialogOpen: true })
  }

  closeRenameDialog() {
    this.setState({ renameDialogOpen: false })
  }

  async handleRenameConfirm(newName) {
    const { multiselectedFiles } = this.props

    if (!this.isValidFilename(newName)) {
      this.setState({ renameError: true })
      return;
    } else {
      this.setState({ renameError: false })
    }

    const oldName = multiselectedFiles.values().next().value.name
    this.closeRenameDialog()

    try {
      this.setState({ loading: true })
      await this.controller.rename(oldName, newName)
    } finally {
      this.setState({ loading: false })
    }
  }

  isValidFilename(filename) {
    if (!filename || filename === '') {
        return false;
    }
    
    if (filename.startsWith(" ") || filename.endsWith(" ") || filename.startsWith(".") || filename.endsWith(".")) {
        return false;
    }
    
    return VALID_FILENAME_PATTERN.test(filename);
  }

  handleRenameCancel() {
    this.closeRenameDialog()
  }

  openMoveLocationDialog() {
    this.setState({ locationDialogMode: moveLocationMode })
  }

  openCopyLocationDialog() {
    this.setState({ locationDialogMode: copyLocationMode })
  }

  closeLocationDialog() {
    this.setState({ locationDialogMode: null })
  }

  async handleLocationConfirm(newPath) {
    const { multiselectedFiles } = this.props
    const { locationDialogMode} = this.state
    this.closeLocationDialog()

    try {
      this.setState({ loading: true })
      if (locationDialogMode === moveLocationMode) {
        await this.controller.move(multiselectedFiles, newPath)
      } else {
        await this.controller.copy(multiselectedFiles, newPath)
      }
    } finally {
      this.setState({ loading: false })
    }
  }

  handleLocationCancel() {
    this.closeLocationDialog()
  }

  async handleDeleteConfirm() {
    const { multiselectedFiles } = this.props

    this.closeDeleteDialog()
    await this.controller.delete(multiselectedFiles)
  }

  handleDeleteCancel() {
    this.closeDeleteDialog()
  }

  renderNoSelectionContextToolbar() {
    const { classes, buttonSize, editable } = this.props
    return ([
      <Button
        key='new-folder'
        classes={{ root: classes.button }}
        color={color}
        size={buttonSize}
        variant='outlined'
        disabled={!editable}
        startIcon={<CreateNewFolderIcon />}
        onClick={this.openNewFolderDialog}
      >
        {messages.get(messages.NEW_FOLDER)}
      </Button>,
      <InputDialog
        key='new-folder-dialog'
        open={this.state.newFolderDialogOpen}
        title={messages.get(messages.NEW_FOLDER)}
        inputLabel={messages.get(messages.FOLDER_NAME)}
        onCancel={this.handleNewFolderCancel}
        onConfirm={this.handleNewFolderCreate}
        />
    ])
  }

  renderSelectionContextToolbar() {
    const {
      classes,
      buttonSize,
      multiselectedFiles,
      sessionToken,
      owner,
      path,
      onDownload,
      editable
    } = this.props
    const {
      width,
      hiddenButtonsPopup,
      deleteDialogOpen,
      renameDialogOpen,
      locationDialogMode,
      renameError
    } = this.state

    const ellipsisButtonSize = 24
    const buttonsCount = 5
    const minSize = 500
    const roughButtonSize = Math.floor(minSize / buttonsCount)
    const hideButtons = width < minSize
    const visibleButtonsCount = Math.max(hideButtons ? Math.floor((width - 3 * ellipsisButtonSize) / roughButtonSize) : buttonsCount, 0)

    const buttons = [
      <Download
        key='download' 
        controller= {this.controller}              
        buttonSize={buttonSize}
        multiselectedFiles={multiselectedFiles.size === 0}
        onDownload={onDownload}
        classes={{ root: classes.button }}
      />
      ,
      <Button
        key='delete'
        classes={{ root: classes.button }}
        color={color}
        size={buttonSize}
        variant='text'
        disabled={!editable}
        startIcon={<DeleteIcon />}
        onClick={this.openDeleteDialog}
      >
        {messages.get(messages.DELETE)}
      </Button>,
      <Button
        key='rename'
        classes={{ root: classes.button }}
        color={color}
        size={buttonSize}
        variant='text'
        disabled={multiselectedFiles.size !== 1 || !editable}
        startIcon={<RenameIcon />}
        onClick={this.openRenameDialog}
      >
        {messages.get(messages.RENAME)}
      </Button>,
      <Button
        key='copy'
        classes={{ root: classes.button }}
        color={color}
        size={buttonSize}
        variant='text'
        disabled={!editable}
        startIcon={<CopyIcon />}
        onClick={this.openCopyLocationDialog}
      >
        {messages.get(messages.COPY)}
      </Button>,
      <Button
        key='move'
        classes={{ root: classes.button }}
        color={color}
        size={buttonSize}
        variant='text'
        disabled={!editable}
        startIcon={<MoveIcon />}
        onClick={this.openMoveLocationDialog}
      >
        {messages.get(messages.MOVE)}
      </Button>
    ]
    const ellipsisButton = (
      <IconButton
        key='ellipsis'
        classes={{ root: classes.button }}
        color={color}
        size={iconButtonSize}
        variant='outlined'
        onClick={this.handleOpen}
      >
        <MoreIcon />
      </IconButton>
    )

    const popover = (
      <Popover
        key='more'
        open={Boolean(hiddenButtonsPopup)}
        anchorEl={hiddenButtonsPopup}
        onClose={this.handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left'
        }}
      >
        <Container square={true}>{this.renderCollapsedButtons(buttons.slice(visibleButtonsCount))}</Container>
      </Popover>
    )

    const selectedValue = multiselectedFiles.values().next().value;
    return (
      <div className={classes.buttons}>
        {hideButtons
          ? [...buttons.slice(0, visibleButtonsCount), ellipsisButton, popover]
          : buttons}
        <ConfirmationDialog
          key='delete-dialog'
          open={deleteDialogOpen}
          onConfirm={this.handleDeleteConfirm}
          onCancel={this.handleDeleteCancel}
          title={messages.get(messages.DELETE)}
          content={messages.get(messages.CONFIRMATION_DELETE_SELECTED)}
        />
        <InputDialog
          key='rename-dialog'
          open={renameDialogOpen}
          error={renameError}
          title={selectedValue.directory ? messages.get(messages.RENAME_FOLDER) : messages.get(messages.RENAME_FILE)}
          inputLabel={selectedValue.directory ? messages.get(messages.FOLDER_NAME) : messages.get(messages.FILE_NAME)}
          inputValue={selectedValue.name}
          errorText={messages.get(messages.FILENAME_INVALID_CHARACTERS)}
          onCancel={this.handleRenameCancel}
          onConfirm={this.handleRenameConfirm}
        />
        <LocationDialog
          key='location-dialog'
          open={!!locationDialogMode}
          title={locationDialogMode === moveLocationMode ? messages.get(messages.MOVE) : messages.get(messages.COPY)}
          content={messages.get(messages.FILE_OR_FILES, multiselectedFiles.size)}
          sessionToken={sessionToken}
          owner={owner}
          path={path}
          multiselectedFiles={multiselectedFiles}
          onCancel={this.handleLocationCancel}
          onConfirm={this.handleLocationConfirm}
          />
      </div>
    );
  }

  renderCollapsedButtons(buttons) {
    const { classes } = this.props
    return (
      <div className={classes.collapsedButtonsContainer}>
        {buttons}
      </div>
    )
  }

  onResize({ width }) {
    if (width !== this.state.width) {
      this.setState({ width, hiddenButtonsPopup: null })
    }
  }

  handleOpen(event) {
    this.setState({
      hiddenButtonsPopup: event.currentTarget
    })
  }

  handleClose() {
    this.setState({
      hiddenButtonsPopup: null
    })
  }

  render() {
    logger.log(logger.DEBUG, 'LeftToolbar.render')

    const { multiselectedFiles, classes, owner } = this.props
    const { loading } = this.state

    return ([
      <ResizeObserver key='resize-observer' onResize={this.onResize}>
        <div className={classes.buttons}>
          {multiselectedFiles && multiselectedFiles.size > 0
            ? this.renderSelectionContextToolbar()
            : this.renderNoSelectionContextToolbar()}
        </div>
      </ResizeObserver>,
      <LoadingDialog key='left-toolbar-loaging-dialog' variant='indeterminate' loading={loading} />
    ])
  }
}

export default withStyles(styles)(LeftToolbar)
