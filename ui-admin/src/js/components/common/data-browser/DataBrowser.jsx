import React from 'react'
import withStyles from '@mui/styles/withStyles';
import autoBind from 'auto-bind'
import JSZip from 'jszip';
import Toolbar from '@src/js/components/common/data-browser/Toolbar.jsx'
import GridView from '@src/js/components/common/data-browser/GridView.jsx'
import fileTypeConfig from '@src/js/components/common/data-browser/fileTypeConfig.js';

import Grid from '@src/js/components/common/grid/Grid.jsx'
import GridFilterOptions from '@src/js/components/common/grid/GridFilterOptions.js'
import AppController from '@src/js/components/AppController.js'
import ItemIcon from '@src/js/components/common/data-browser/ItemIcon.jsx'
import InfoPanel from '@src/js/components/common/data-browser/InfoPanel.jsx'
import DataBrowserController from '@src/js/components/common/data-browser/DataBrowserController.js'
import FileDownloadManager from '@src/js/components/common/data-browser/FileDownloadManager.js'
import FileUploadManager from '@src/js/components/common/data-browser/FileUploadManager.js'
import messages from '@src/js/common/messages.js'
import InfoBar from '@src/js/components/common/data-browser/InfoBar.jsx'
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx'
import ErrorDialog from '@src/js/components/common/error/ErrorDialog.jsx'
import FileExistsDialog from '@src/js/components/common/dialog/FileExistsDialog.jsx'
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx'
import LinearLoadingDialog from '@src/js/components/common/loading/LinearLoadingDialog.jsx';
import {isUserAbortedError, timeToString} from "@src/js/components/common/data-browser/DataBrowserUtils.js";


const styles = theme => ({
  columnFlexContainer: {
    flexDirection: 'column',
    display: 'flex',
    height: 'calc(100vh - ' + theme.spacing(21) + 'px)'
  },
  boundary: {
    padding: theme.spacing(1),
    borderColor: theme.palette.border.secondary,
    backgroundColor: theme.palette.background.paper
  },
  icon: {
    fontSize: '1.5rem',
    paddingRight: '0.5rem'
  },
  flexContainer: {
    display: 'flex',
    '&>*': {
      flex: '0 0 auto',
      padding: theme.spacing(1),
      borderWidth: '1px',
      borderStyle: 'solid',
      borderColor: theme.palette.border.secondary,
      backgroundColor: theme.palette.background.paper
    },
  },
  grid: {
    flexGrow: 1,
    flex: 1,
    height: 'auto',
    overflowY: 'auto',
    paddingTop: 0,
    paddingBottom: 0
  },
  content: {
    flex: '1 1 100%',
    height: 0,
  },
  nameCell: {
    display: 'flex',
    alignItems: 'center',
    '&>span': {
      flex: 1,
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  },
})



class DataBrowser extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)

    const { sessionToken,
            controller,
            id,
            showFileExistsDialog,
            currentFile,
            extOpenbis,
            fromExternalApp } = this.props

    this.controller = controller || new DataBrowserController(id, extOpenbis)
    this.controller.attach(this)
    this.dragDepth = 0;

    this.downloadManager = new FileDownloadManager(
      this.controller,
      () => this.state, 
      this.updateStateCallback,
      this.openErrorDialog,
      this.updateResolveDecision      
    )

    this.uploadManager = new FileUploadManager(
      this.controller,
      () => this.state, 
      this.updateStateCallback,
      this.openErrorDialog      
    )

    this.state = {
      viewType: props.viewType,
      files: [],
      selectedFile: null,
      multiselectedFiles: new Set([]),
      showInfo: false,
      path: '/',
      freeSpace: -1,
      totalSpace: -1,      
      errorMessage: null,
      editable: false,                                                
      isDragging: false,
      ...(this.downloadManager.getDefaultState()),      
      ...(this.uploadManager.getDefaultState()),      
    }

    this.zip = new JSZip()
   
  }

  
  updateStateCallback(partialStateOrUpdater){   
    if (typeof partialStateOrUpdater === 'function') {
      this.setState(prev => partialStateOrUpdater(prev))
    } else {
      this.setState(partialStateOrUpdater)
    }
  }

  updateResolveDecision(resolve){
    this.resolveDecision = resolve
  }

  cancelFileTransfer() {
    this.setState({ cancelTransfer: true })
    this.controller.abortCurrentApiOperation()
  }

  handleConfirmMerge() {
    this.setState({ showMergeDialog: false }, () => {
      if (this.resolveDecision) {
        this.resolveDecision(true);
        this.resolveDecision = null;
      }
    })
  }

  handleCancelMerge() {
    this.setState({ showMergeDialog: false }, () => {
      if (this.resolveDecision) {
        this.resolveDecision(false);
        this.resolveDecision = null;
      }
    })
  }


  // Triggered when the user confirms to overwrite
  handleReplace() {
    this.setState({ showFileExistsDialog: false, replaceFile: true, skipFile: false }, () => {
      if (this.resolveDecision) {
        this.resolveDecision(true);
        this.resolveDecision = null;
      }
    });
  }

  handleSkip() {
    this.setState({ showFileExistsDialog: false, replaceFile: false, skipFile: true }, () => {
      if (this.resolveDecision) {
        this.resolveDecision(true);
        this.resolveDecision = null;
      }
    });
  }

  // Triggered when the user cancels to overwrite
  handleCancel() {
    this.setState({ showFileExistsDialog: false, cancelTransfer: true }, () => {
      if (this.resolveDecision) {
        this.resolveDecision(false);
        this.resolveDecision = null;
      }
    });
  }


  handleViewTypeChange(viewType) {
    this.setState({ viewType })
  }

  handleClick(file) {
    // TODO: implement
  }

  async handleRowDoubleClick(row) {
    const file = row.data
    const { directory, path } = file
    if (directory) {
      await this.setPath(path)
    } else {        
      await this.downloadManager.handleDownloadFiles(new Set([file]))      
    }
  }

  handleSelect(selectedRow) {
    this.setState({ selectedFile: selectedRow && selectedRow.data })
  }

  handleMultiselect(selectedRow) {
    this.setState({
      multiselectedFiles: new Set(
        Object.values(selectedRow).map(value => value.data)
      )
    })
  }


  handleApplyToAllSelection(checked) {    
    this.setState({ applyToAllFiles: checked })    
  }


  async handleDownload() {
    try {      
      this.downloadManager.resetDownloadDialogStates()
      const { multiselectedFiles } = this.state
      await this.downloadManager.handleDownloadFiles(multiselectedFiles)
    } catch (err) {      
      if (isUserAbortedError(err)) {
        // no feedback needed, user aborted          
      } else {
        this.openErrorDialog(err)
      }
    }
  }

  async onError(error) {
    await AppController.getInstance().errorChange(error)
  }

  handleShowInfoChange() {
    this.setState({ showInfo: !this.state.showInfo })
  }

  handleGridControllerRef(gridController) {
    this.controller.gridController = gridController
  }

  async handlePathChange(path) {
    await this.setPath(path)
  }

  async setPath(path) {
    if (this.state.path !== path + '/') {
      this.setState({ path: path + '/' })
      this.controller.setPath(path + '/')
      await this.controller.gridController.load()
    }
  }

  sizeToString(bytes) {
    if (!bytes) {
      return null
    }

    if (typeof bytes == 'string') {
      bytes = parseInt(bytes)
    }

    let size
    let unit
    const kbytes = bytes / 1024.0
    const mbytes = kbytes / 1024.0
    const gbytes = mbytes / 1024.0
    if (gbytes > 1.0) {
      size = gbytes
      unit = 'GB'
    } else if (mbytes > 1.0) {
      size = mbytes
      unit = 'MB'
    } else if (kbytes > 1.0) {
      size = kbytes
      unit = 'kB'
    } else {
      size = bytes
      unit = 'bytes'
    }
    return size.toFixed(1) + '\xa0' + unit
  }

  fetchSpaceStatus() {
    this.controller.free().then(space => {
      this.setState({ freeSpace: space.free, totalSpace: space.total })
    })
  }

  inferMimeType(fileName) {
    const extension = fileName.slice(fileName.lastIndexOf('.')).toLowerCase()
    return mimeTypeMap[extension] || 'application/octet-stream'
  }

  async fetchRights() {
    const { objId, objKind } = this.props
    const right = await this.controller.getRights([{ permId: objId, entityKind: objKind }])
    
      if (right[objId] && right[objId].rights) {
        const editable = right[objId].rights.includes("UPDATE")
        this.setState({ editable: editable })
      } else {
        this.setState({ editable: false })
      }
    
  }

  async componentDidMount() {
    try {
      this.fetchSpaceStatus()
      await this.fetchRights()    
      if (this.state.viewType === 'grid') {
        this.fetchFiles();
      }
    } catch (err){
        this.openErrorDialog(err)
    }
  }

  componentDidUpdate(prevProps, prevState) {
    if (prevState.viewType !== this.state.viewType && this.state.viewType === 'grid') {
      this.fetchFiles();
    }
  }

  async fetchFiles() {
    try {
      const filesList = await this.controller.listFiles();
      this.setState({ files: filesList });
    } catch (error) {
      console.error('Error loading files:', error);
      this.setState({ errorMessage: 'Failed to load files.' });
    }
  }

  openErrorDialog(errorMessage) {
    this.setState({ errorMessage })
  }

  closeErrorDialog() {
    this.setState({ errorMessage: null })
  }

  // Prevent default to allow drop
  handleDragOver(e) {
    e.preventDefault()
  }

  handleDragEnter(e) {
    e.preventDefault()    
    this.dragDepth++
         
    if (!this.containsFiles(e)) {     
      return;
    }
    
    this.setState({ isDragging: true });

  }

  handleDragLeave(e) {
    e.preventDefault()
    this.dragDepth--    
    if (this.dragDepth === 0) {
      this.setState({ isDragging: false });
    }
  }

  containsFiles(e){
    if (e.dataTransfer.types) {
      for (var i = 0; i < e.dataTransfer.types.length; i++) {
        if (e.dataTransfer.types[i] === "Files") return true;
      }
    }
    return false;
  };


  async handleDrop(e) {    
    e.preventDefault();
    this.dragDepth = 0;
    this.setState({ isDragging: false });

    if (!this.containsFiles(e)) {     
      return;
    }

    this.uploadManager.handleDragAndDropUpload(e)
  }
   
  renderFileExistsDialog(key, dialogProps) {
    const { open, onReplace, onResume, onSkip, onCancel, onApplyToAllChange, applyToAll, title, content } = dialogProps;
  
    return (
      <FileExistsDialog
        key={key}
        open={open}
        onReplace={onReplace}
        onResume={onResume}
        onSkip={onSkip}
        onCancel={onCancel}
        onApplyToAllChange={onApplyToAllChange}
        applyToAll={applyToAll}
        title={title}
        content={content}
      />
    );
  };

  render() {
    const { classes, sessionToken, id } = this.props
    const {
      viewType,
      files,
      selectedFile,
      multiselectedFiles,
      showInfo,
      path,
      freeSpace,
      totalSpace,
      loading,
      errorMessage,
      editable,
      progress,
      progressMessage,
      fileName,
      averageSpeed,
      showFileExistsDialog,
      applyToAllFiles,
      showMergeDialog,
      showApplyToAll,
      processedBytes, 
      totalTransferSize,
      expectedTime,
      progressBarFrom,
      progressBarTo,
      loadingDialogVariant,
      customProgressDetails,      
      progressStatus
    } = this.state


    return [
      <div
        key='data-browser-content'
        className={[classes.boundary, classes.columnFlexContainer].join(' ')}
      >
        <Toolbar
          controller={this.controller}
          viewType={viewType}
          onViewTypeChange={this.handleViewTypeChange}
          onShowInfoChange={this.handleShowInfoChange}
          onDownload={this.handleDownload}
          showInfo={showInfo}
          multiselectedFiles={multiselectedFiles}
          sessionToken={sessionToken}
          owner={id}
          editable={editable}
          path={path}
        />
        <InfoBar
          path={path}
          onPathChange={this.handlePathChange}
          free={freeSpace}
          total={totalSpace}
        />
        <div
          className={[
            classes.flexContainer,
            classes.boundary,
            classes.content
          ].join(' ')}
            onDragEnter={this.handleDragEnter}
            onDragOver={this.handleDragOver}
            onDragLeave={this.handleDragLeave}
            onDrop={this.handleDrop}
        >
          {viewType === 'list' && (
            <Grid
              id='data-browser-grid'
              settingsId='data-browser-grid'              
              loadSettings={this.props.onLoadDisplaySettings}
              onSettingsChange={this.props.onStoreDisplaySettings}
              controllerRef={this.handleGridControllerRef}
              filterModes={[GridFilterOptions.COLUMN_FILTERS]}
              header='Files'
              classes={{container: classes.grid}}
              isDragging={this.state.isDragging}
              fromExternalApp={this.props.fromExternalApp}
              columns={[
                {
                  name: 'name',
                  label: messages.get(messages.NAME),
                  sortable: true,
                  visible: true,
                  getValue: ({ row }) => row.name,
                  renderValue: ({ row }) => {
                    const isTruncated = _.isString(row.name) && row.name.length > 100;
                    const displayedName = isTruncated
                      ? row.name.slice(0, 100) + '...'
                      : row.name;
                  
                    return (
                      <div className={classes.nameCell}>
                        <ItemIcon
                          file={row}
                          classes={{ icon: classes.icon }}
                          configuration={fileTypeConfig}
                        />
                        <span {...(isTruncated ? { title: row.name } : {})}>
                          {displayedName}
                        </span>
                      </div>
                    );
                  },
                  renderFilter: null
                },
                {
                  name: 'type',
                  label: messages.get(messages.TYPE),
                  sortable: true,
                  visible: false,
                  getValue: ({ row }) => (row.directory ? 'Directory' : 'File')
                },
                {
                  name: 'size',
                  label: messages.get(messages.SIZE),
                  sortable: true,
                  visible: true,
                  getValue: ({ row }) => this.sizeToString(row.size)
                },
                {
                  name: 'modified',
                  label: messages.get(messages.MODIFIED),
                  sortable: true,
                  visible: true,
                  getValue: ({ row }) => row.lastModifiedTime,
                  renderValue: ({ row }) =>
                    timeToString(row.lastModifiedTime)
                }
              ]}
              loadRows={this.controller.load}
              exportable={false}
              selectable={true}
              multiselectable={true}              
              showHeaders={true}              
              onError={this.onError}
              onSelectedRowChange={this.handleSelect}
              onMultiselectedRowsChange={this.handleMultiselect}
              onRowDoubleClick={this.handleRowDoubleClick}
              exportXLS={null}
            />
          )}
          {viewType === 'grid' && (
            <GridView
              clickable={true}
              selectable={true}
              multiselectable={true}
              onClick={this.handleClick}
              onSelect={this.handleSelect}
              onMultiselect={this.handleMultiselect}
              configuration={fileTypeConfig}
              files={files}
              selectedFile={selectedFile}
              multiselectedFiles={multiselectedFiles}
              filterModes={[GridFilterOptions.COLUMN_FILTERS]}
              header='Files'
            />
          )}
          {showInfo && selectedFile && (
            <InfoPanel
              selectedFile={selectedFile}
              configuration={fileTypeConfig}
            />
          )}
        </div>
      </div>,
      <LinearLoadingDialog 
        key='data-browser-loaging-dialog' 
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
        onCancel={this.cancelFileTransfer}
      />,
      <ErrorDialog
        key='data-browser-error-dialog'
        open={!!errorMessage}
        error={errorMessage}
        onClose={this.closeErrorDialog}
      />,
      <FileExistsDialog
        key="db-overwrite-modal-title"
        open={showFileExistsDialog}
        onReplace={this.handleReplace}
        onSkip={this.state.allowSkip ? this.handleSkip : null}
        onCancel={this.handleCancel}
        onApplyToAllChange={showApplyToAll ? this.handleApplyToAllSelection : null}
        applyToAll={applyToAllFiles}
        title={messages.get(messages.FILE_EXISTS)}
        content={messages.get(messages.CONFIRMATION_FILE_OVERWRITE, this.state.currentFile)}
      />,
      <FileExistsDialog
        key="upload-file-exists-dialog"
        open={!!this.state.uploadFileExistsDialogFile}
        onReplace={this.uploadManager.handleFileExistsReplace}
        onResume={this.state.allowResume ? this.uploadManager.handleFileExistsResume : null}
        onSkip={this.state.allowSkip ? this.uploadManager.handleFileExistsSkip : null}
        onCancel={this.uploadManager.handleFileExistsCancel}
        onApplyToAllChange={showApplyToAll ? this.uploadManager.handleApplyToAllSelection : null}
        applyToAll={applyToAllFiles}
        title={messages.get(messages.FILE_EXISTS)}
        content={messages.get(
          this.state.allowResume 
            ? (this.state.allowSkip 
                ? messages.CONFIRMATION_FILE_NAME_RESUME_CONFLICT 
                : messages.CONFIRMATION_FILE_NAME_RESUME_NOSKIP_CONFLICT)
            : (this.state.allowSkip 
                ? messages.CONFIRMATION_FILE_NAME_CONFLICT 
                : messages.CONFIRMATION_FILE_NAME_REPLACE_CONFLICT),
          this.state.uploadFileExistsDialogFile?.path
        )}
      />,
      <ConfirmationDialog
        key='merge-modal-title'
        open={showMergeDialog}
        onConfirm={this.handleConfirmMerge}
        onCancel={this.handleCancelMerge}
        title={messages.get(messages.NON_EMPTY_FOLDER)}
        content={messages.get(messages.NON_EMPTY_FOLDER_MSG)}
      />
    ]
  }

}

export default withStyles(styles)(DataBrowser)
