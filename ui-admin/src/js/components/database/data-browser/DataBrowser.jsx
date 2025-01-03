import React from 'react'
import withStyles from '@mui/styles/withStyles';
import autoBind from 'auto-bind'
import Toolbar from '@src/js/components/database/data-browser/Toolbar.jsx'
import GridView from '@src/js/components/database/data-browser/GridView.jsx'
import mimeTypeMap from '@src/js/components/database/data-browser/mimeTypes.js'; 
import fileTypeConfig from '@src/js/components/database/data-browser/fileTypeConfig.js';

import Grid from '@src/js/components/common/grid/Grid.jsx'
import GridFilterOptions from '@src/js/components/common/grid/GridFilterOptions.js'
import AppController from '@src/js/components/AppController.js'
import ItemIcon from '@src/js/components/database/data-browser/ItemIcon.jsx'
import InfoPanel from '@src/js/components/database/data-browser/InfoPanel.jsx'
import DataBrowserController from '@src/js/components/database/data-browser/DataBrowserController.js'
import FileDownloadManager from '@src/js/components/database/data-browser/FileDownloadManager.js'
import messages from '@src/js/common/messages.js'
import InfoBar from '@src/js/components/database/data-browser/InfoBar.jsx'
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx'
import ErrorDialog from '@src/js/components/common/error/ErrorDialog.jsx'
import FileExistsDialog from '@src/js/components/common/dialog/FileExistsDialog.jsx'
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx'
import LinearLoadingDialog from '@src/js/components/common/loading/LinearLoadingDialog.jsx';



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

    const { sessionToken, controller, id, showFileExistsDialog, currentFile } = this.props

    this.controller = controller || new DataBrowserController(id)
    this.controller.attach(this)

    this.state = {
      viewType: props.viewType,
      files: [],
      selectedFile: null,
      multiselectedFiles: new Set([]),
      showInfo: false,
      path: '/',
      freeSpace: -1,
      totalSpace: -1,
      loading: false,
      totalDownloaded: 0,
      totalDownloadSize: 0,
      progress: 0,
      errorMessage: null,
      editable: false,
      showFileExistsDialog: false,
      currentFile: null,
      resolveDecision: null,
      replaceFile: false,
      skipFile: false,
      cancelDownload: false,
      applyToAllFiles: false,
      showMergeDialog: false,
      resolveMergeDecision: null,
      showApplyToAll: true,
      formattedTotalDownloadSize:0,
      lastConflictResolution:null,
      expectedTimeFormatted: null,
      lastTimestamp:null,
      rollingSpeed:0,
      downloadBarFrom:null,
      downloadBarTo:null,
      loadingDialogVariant:'determinate',
      customProgressDetails:null,
      averageSpeed:0,
      totalFilesToDownload:0
    }
    this.zip = new JSZip()

    this.downloadManager = new FileDownloadManager(
      this.controller,
      () => this.state, 
      this.updateStateCallback,
      this.openErrorDialog,
      this.updateResolveDecision      
    )
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
    this.setState({ showFileExistsDialog: false, cancelDownload: true }, () => {
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
    this.downloadManager.resetDownloadDialogStates()
    const { multiselectedFiles } = this.state
    await this.downloadManager.handleDownloadFiles(multiselectedFiles)
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

  timeToString(time) {
    return new Date(time).toLocaleString()
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

  fetchRights() {
    const { id, kind } = this.props
    this.controller.getRights([{ permId: id, entityKind: kind }]).then(right => {
      if (right[id] && right[id].rights) {
        const editable = right[id].rights.includes("UPDATE")
        this.setState({ editable: editable })
      } else {
        this.setState({ editable: false })
      }
    })
  }

  componentDidMount() {
    this.fetchSpaceStatus()
    this.fetchRights()
  }

  openErrorDialog(errorMessage) {
    this.setState({ errorMessage })
  }

  closeErrorDialog() {
    this.setState({ errorMessage: null })
  }



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
      totalDownloaded, 
      totalDownloadSize,
      expectedTime,
      downloadBarFrom,
      downloadBarTo,
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
        >
          {viewType === 'list' && (
            <Grid
              id='data-browser-grid'
              controllerRef={this.handleGridControllerRef}
              filterModes={[GridFilterOptions.COLUMN_FILTERS]}
              header='Files'
              classes={{ container: classes.grid }}
              columns={[
                {
                  name: 'name',
                  label: messages.get(messages.NAME),
                  sortable: true,
                  visible: true,
                  getValue: ({ row }) => row.name,
                  renderValue: ({ row }) => (
                    <div className={classes.nameCell}>
                      <ItemIcon
                        file={row}
                        classes={{ icon: classes.icon }}
                        configuration={fileTypeConfig}
                      />
                      <span>{row.name}</span>
                    </div>
                  ),
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
                  name: 'created',
                  label: messages.get(messages.CREATED),
                  sortable: true,
                  visible: false,
                  getValue: ({ row }) => row.creationTime,
                  renderValue: ({ row }) => this.timeToString(row.creationTime)
                },
                {
                  name: 'modified',
                  label: messages.get(messages.MODIFIED),
                  sortable: true,
                  visible: true,
                  getValue: ({ row }) => row.lastModifiedTime,
                  renderValue: ({ row }) =>
                    this.timeToString(row.lastModifiedTime)
                },
                {
                  name: 'accessed',
                  label: messages.get(messages.ACCESSED),
                  sortable: true,
                  visible: false,
                  getValue: ({ row }) => row.lastAccessTime,
                  renderValue: ({ row }) =>
                    this.timeToString(row.lastAccessTime)
                }
              ]}
              loadRows={this.controller.load}
              exportable={false}
              selectable={true}
              multiselectable={true}
              loadSettings={null}
              showHeaders={true}
              onSettingsChange={null}
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
        from={downloadBarFrom}
        to={downloadBarTo}
        fileName={fileName}
        progress={progress}
        currentSize={totalDownloaded}
        totalSize={totalDownloadSize}
        timeLeft={expectedTime}
        speed={averageSpeed}
        variant={loadingDialogVariant}
        customProgressDetails={customProgressDetails}        
        progressStatus={progressStatus}
      />,
      <ErrorDialog
        key='data-browser-error-dialog'
        open={!!errorMessage}
        error={errorMessage}
        onClose={this.closeErrorDialog}
      />,
      <FileExistsDialog
        key='overwrite-modal-title'
        open={showFileExistsDialog}
        onReplace={this.handleReplace}
        onSkip={this.handleSkip}
        onCancel={this.handleCancel}
        title={messages.get(messages.FILE_EXISTS)}
        onApplyToAllChange={ showApplyToAll ? this.handleApplyToAllSelection: null}
        applyToAll={applyToAllFiles}
        content={messages.get(messages.CONFIRMATION_FILE_OVERWRITE,
          this.state.currentFile?.name)}
      />,
      <ConfirmationDialog
        key='merge-modal-title'
        open={showMergeDialog}
        onConfirm={this.handleConfirmMerge}
        onCancel={this.handleCancelMerge}
        title={messages.get(messages.CONFIRM_MERGE)}
        content={messages.get(messages.CONFIRMATION_MERGE_DOWNLOAD)}
      />
    ]
  }
}

export default withStyles(styles)(DataBrowser)
