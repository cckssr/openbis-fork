// LeftToolbarButtons.jsx
import React from 'react';
import withStyles from '@mui/styles/withStyles';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Popover from '@mui/material/Popover';
import CreateNewFolderIcon from '@mui/icons-material/CreateNewFolder';
import DeleteIcon from '@mui/icons-material/Delete';
import RenameIcon from '@mui/icons-material/Create';
import CopyIcon from '@mui/icons-material/FileCopy';
import MoveIcon from '@mui/icons-material/ArrowRightAlt';
import MoreIcon from '@mui/icons-material/MoreVert';
import { debounce } from '@mui/material';
import Container from '@src/js/components/common/form/Container.jsx';
import InputDialog from '@src/js/components/common/dialog/InputDialog.jsx';
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx';
import LocationDialog from '@src/js/components/common/data-browser/LocationDialog.jsx';
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx';
import Download from '@src/js/components/common/data-browser/components/download/Download.jsx';
import DataBrowserController from '@src/js/components/common/data-browser/DataBrowserController.js';
import messages from '@src/js/common/messages.js';
import logger from '@src/js/common/logger.js';
import { Add, FolderSharp } from '@mui/icons-material';

const color = 'inherit';
const iconButtonSize = 'medium';
const moveLocationMode = 'move';
const copyLocationMode = 'copy';
const VALID_FILENAME_PATTERN = /^[0-9A-Za-z $!#%'()+,\-.;=@[\]^_{}~]+$/;

const styles = theme => ({
  collapsedButtonsContainer: {
    display: 'flex',
    flexDirection: 'column',
   '& > button:not(:last-child)': {
      marginBottom: theme.spacing(1),
      marginLeft: 0,
      marginRight: 0,
    },
    '&>button:nth-last-child(1)': {
      marginBottom: 0,
      marginLeft:0
    }
  },

  iconSpacing: {
    marginRight: '4px',
  }

});

class LeftToolbarButtons extends React.Component {
  constructor(props, context) {
    super(props, context);
    // Initialize the controller with owner and extOpenbis from props
    this.controller = this.props.controller

    // All the state related to dialogs, selection, etc.
    this.state = {      
      hiddenButtonsPopup: null,
      newFolderDialogOpen: false,
      deleteDialogOpen: false,
      renameDialogOpen: false,
      locationDialogMode: null,
      loading: false,
      renameError: false,
      editable: false,      
    };

    // Debounce any methods if needed
    this.onResize = debounce(this.onResize, 1);
  }

  // --- Methods for new folder ---
  openNewFolderDialog = (event) => {
    this.setState({ newFolderDialogOpen: true });
    event.currentTarget.blur() // somehow adding the external bootstrap classes, messed up with mui default behaviour
  }
  closeNewFolderDialog = () => {
    this.setState({ newFolderDialogOpen: false });
  }
  async handleNewFolderCreate(folderName) {
    this.closeNewFolderDialog();
    if (folderName && folderName.trim()) {
      await this.controller.createNewFolder(folderName.trim());
      this.props.onGridActionComplete()
    }
  }
  handleNewFolderCancel = () => {
    this.closeNewFolderDialog();
  }

  // --- Methods for delete ---
  openDeleteDialog = (event) => {
    this.setState({ deleteDialogOpen: true });
    event.currentTarget.blur() // somehow adding the external bootstrap classes, messed up with mui default behaviour
  }
  closeDeleteDialog = () => {
    this.setState({ deleteDialogOpen: false });
  }
  async handleDeleteConfirm() {
    const { multiselectedFiles } = this.props;
    this.closeDeleteDialog();
    await this.controller.delete(multiselectedFiles);
    this.props.onGridActionComplete()
  }
  handleDeleteCancel = () => {
    this.closeDeleteDialog();
  }

  // --- Methods for rename ---
  openRenameDialog = (event) => {
    this.setState({ renameError: false, renameDialogOpen: true });
    event.currentTarget.blur() // somehow adding the external bootstrap classes, messed up with mui default behaviour
  }
  closeRenameDialog = () => {
    this.setState({ renameDialogOpen: false });
  }
  async handleRenameConfirm(newName) {
    const { multiselectedFiles } = this.props;
    if (!this.isValidFilename(newName)) {
      this.setState({ renameError: true });
      return;
    } else {
      this.setState({ renameError: false });
    }
    const oldName = multiselectedFiles.values().next().value.name;
    this.closeRenameDialog();
    try {
      this.setState({ loading: true });
      await this.controller.rename(oldName, newName);
    } finally {
      this.setState({ loading: false });
    }
    this.props.onGridActionComplete()
  }
  isValidFilename(filename) {
    if (!filename || filename === '') return false;
    if (filename.startsWith(" ") || filename.endsWith(" ") || filename.startsWith(".") || filename.endsWith(".")) {
      return false;
    }
    return VALID_FILENAME_PATTERN.test(filename);
  }
  handleRenameCancel = () => {
    this.closeRenameDialog();
  }

  // --- Methods for move/copy (location) ---
  openMoveLocationDialog = (event) => {
    this.setState({ locationDialogMode: moveLocationMode });
    event.currentTarget.blur() // somehow adding the external bootstrap classes, messed up with mui default behaviour
  }
  openCopyLocationDialog = (event) => {
    this.setState({ locationDialogMode: copyLocationMode });
    event.currentTarget.blur() // somehow adding the external bootstrap classes, messed up with mui default behaviour
  }
  closeLocationDialog = () => {
    this.setState({ locationDialogMode: null });
  }
  async handleLocationConfirm(newPath) {
    const { multiselectedFiles } = this.props;
    const { locationDialogMode} = this.state;
    this.closeLocationDialog();
    try {
      this.setState({ loading: true });
      if (locationDialogMode === moveLocationMode) {
        await this.controller.move(multiselectedFiles, newPath);
      } else {
        await this.controller.copy(multiselectedFiles, newPath);
      }
    } finally {
      this.setState({ loading: false });
    }
    this.props.onGridActionComplete()    
  }
  handleLocationCancel = () => {
    this.closeLocationDialog();
  }


  // --- Rendering methods ---
  renderNoSelectionContextToolbar() {
    const { classes, buttonSize ,className} = this.props;
    return ([
      <Button
        key='new-folder'
        classes={{ root: classes.buttonLeft }}
        className={className}
        color={color}
        size={buttonSize}
        variant='outlined'
        disabled={!this.props.editable || this.props.frozen}
        title={messages.get(messages.NEW_DIRECTORY)}
        onClick={this.openNewFolderDialog}
      >        
        <Add className={classes.buttonicon} />
        <FolderSharp className={classes.buttonicon} />
      </Button>,
      <InputDialog
        key='new-folder-dialog'
        open={this.state.newFolderDialogOpen}
        title={messages.get(messages.NEW_DIRECTORY)}
        inputLabel={messages.get(messages.DIRECTORY_NAME)}
        onCancel={this.handleNewFolderCancel}
        onConfirm={this.handleNewFolderCreate.bind(this)}
      />
    ]);
  }

  renderSelectionContextToolbar() {
    const { 
        classes,
        buttonSize,
        owner,
        className,
        path,
        multiselectedFiles,
        editable,
        onDownload
     } = this.props;
    const {
      hiddenButtonsPopup,
      deleteDialogOpen,
      renameDialogOpen,
      locationDialogMode,
      renameError,
      
    } = this.state;
    
    // Use the width from props (provided by the ResponsiveLeftToolbar)
    const currentWidth = this.props.width || 0;
    const ellipsisButtonSize = 24;
    const buttonsCount = 5;
    const minSize = 500;
    const roughButtonSize = Math.floor(minSize / buttonsCount);
    const hideButtons = currentWidth < minSize;
    const visibleButtonsCount = hideButtons
      ? Math.max(Math.floor((currentWidth - 3 * ellipsisButtonSize) / roughButtonSize), 0)
      : buttonsCount;

    const buttons = [
      <Download
        key='download'
        controller={this.controller}
        buttonSize={buttonSize}
        multiselectedFiles={multiselectedFiles.size === 0}
        onDownload={onDownload}
        classes={{ root: classes.buttonLeft }}
        className={className}
        disabled={this.props.archived}
      />,
      <Button
        key='delete'
        classes={{ root: classes.buttonLeft }}
        className={className} 
        color={color}
        size={buttonSize}
        variant='text'
        disabled={!editable || this.props.frozen}
        startIcon={<DeleteIcon />}
        onClick={this.openDeleteDialog}        
      >
        {messages.get(messages.DELETE)}
      </Button>,
      <Button
        key='rename'
        classes={{ root: classes.buttonLeft }}
        className={className} 
        color={color}
        size={buttonSize}
        variant='text'
        disabled={multiselectedFiles.size !== 1 || !editable || this.props.frozen}
        startIcon={<RenameIcon />}
        onClick={this.openRenameDialog}
      >
        {messages.get(messages.RENAME)}
      </Button>,
      <Button
        key='copy'
        classes={{ root: classes.buttonLeft }}
        className={className} 
        color={color}
        size={buttonSize}
        variant='text'
        disabled={!editable || this.props.frozen}
        startIcon={<CopyIcon />}
        onClick={this.openCopyLocationDialog}
      >
        {messages.get(messages.COPY)}
      </Button>,
      <Button
        key='move'
        classes={{ root: classes.buttonLeft }}
        className={className} 
        color={color}
        size={buttonSize}
        variant='text'
        disabled={!editable || this.props.frozen}
        startIcon={<MoveIcon />}
        onClick={this.openMoveLocationDialog}
      >
        {messages.get(messages.MOVE)}
      </Button>
    ];

    const ellipsisButton = (
      <IconButton
        key='ellipsis'
        classes={{ root: classes.ellipsisButton }}
        className={className} 
        color={color}
        size={iconButtonSize}
        variant='outlined'
        onClick={(e) => this.setState({ hiddenButtonsPopup: e.currentTarget })}
      >
        <MoreIcon />
      </IconButton>
    );

    const popover = (
      <Popover
        key='more'
        open={Boolean(hiddenButtonsPopup)}
        anchorEl={hiddenButtonsPopup}
        onClose={() => this.setState({ hiddenButtonsPopup: null })}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left'
        }}
      >
        <Container square>{this.renderCollapsedButtons(buttons.slice(visibleButtonsCount))}</Container>
      </Popover>
    );

    const selectedValue = multiselectedFiles.values().next().value;
    return (
      <div className={this.props.containerClassName}>
        {hideButtons
          ? [...buttons.slice(0, visibleButtonsCount), ellipsisButton, popover]
          : buttons}
        <ConfirmationDialog
          key='delete-dialog'
          open={deleteDialogOpen}
          onConfirm={this.handleDeleteConfirm.bind(this)}
          onCancel={this.handleDeleteCancel}
          title={messages.get(messages.DELETE)}
          content={messages.get(messages.CONFIRMATION_DELETE_SELECTED)}
        />
        <InputDialog
          key='rename-dialog'
          open={renameDialogOpen}
          error={renameError}
          title={
            selectedValue && selectedValue.directory
              ? messages.get(messages.RENAME_FOLDER)
              : messages.get(messages.RENAME_FILE)
          }
          inputLabel={
            selectedValue && selectedValue.directory
              ? messages.get(messages.FOLDER_NAME)
              : messages.get(messages.FILE_NAME)
          }
          inputValue={selectedValue ? selectedValue.name : ''}
          errorText={messages.get(messages.FILENAME_INVALID_CHARACTERS)}
          onCancel={this.handleRenameCancel}
          onConfirm={this.handleRenameConfirm.bind(this)}
        />
        <LocationDialog
          key='location-dialog'
          open={!!locationDialogMode}
          title={
            locationDialogMode === moveLocationMode
              ? messages.get(messages.MOVE)
              : messages.get(messages.COPY)
          }
          content={messages.get(messages.FILE_OR_FILES, multiselectedFiles.size)}
          owner={owner}
          path={path}
          openBis={this.props.extOpenbis}
          multiselectedFiles={multiselectedFiles}
          onCancel={this.handleLocationCancel}
          onConfirm={this.handleLocationConfirm.bind(this)}
        />
      </div>
    );
  }

  renderCollapsedButtons(buttons) {
    const { classes } = this.props;
    return (
      <div className={classes.collapsedButtonsContainer}>
        {buttons}
      </div>
    );
  }

  render() {
    logger.log(logger.DEBUG, 'LeftToolbarButtons.render');
    

    const { containerClassName, classes, multiselectedFiles} = this.props;
    return (
      <div className={containerClassName || classes.buttonLefts}>
        {multiselectedFiles && multiselectedFiles.size > 0
          ? this.renderSelectionContextToolbar()
          : this.renderNoSelectionContextToolbar()}
        <LoadingDialog variant='indeterminate' loading={this.state.loading} />
      </div>
    );
  }
}

export default withStyles(styles)(LeftToolbarButtons);
