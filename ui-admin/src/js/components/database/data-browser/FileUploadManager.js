
import messages from '@src/js/common/messages.js'
import autoBind from 'auto-bind'

const Resolution = {
  MERGE: 'MERGE',
  SKIP: 'SKIP',
  RESUME: 'RESUME',
  CANCEL: 'CANCEL',
  REPLACE: 'REPLACE'
};

const SKIP_RESOLUTIONS = new Set([Resolution.RESUME, Resolution.SKIP]);

export default class FileUploadManager {

  constructor(controller, getCurrentState, updateStateCallback, openErrorDialog) {
    autoBind(this)
    this.controller = controller
    this.getCurrentState = getCurrentState     
    this.updateState = updateStateCallback
    this.openErrorDialog = openErrorDialog       
    this.zip = new JSZip()
    this.resolveConflict = null // This function will be shared
  }

  getDefaultState(){
    return {
      loading: false,
      showFileExistsDialog: false,
      currentFile: null,
      processedBytes: 0,
      totalTransferSize: 0,
      progress: 0,
      progressBarFrom:null,
      progressBarTo:null,
      loadingDialogVariant:'determinate',
      customProgressDetails:null,
      averageSpeed:0,
      totalFilesToDownload:0,
      replaceFile: false,
      skipFile: false,
      cancelDownload: false,
      applyToAllFiles: false,
      showMergeDialog: false,
      resolveMergeDecision: null,
      showApplyToAll: true,
      resolveDecision: null,     
      rollingSpeed:0,
      totalSavingTime:0
    }
  }


  updateSizeCalculationProgress(fileName, fileSize) {    
    this.updateState((prevState) => {
        var totalTransferSize = prevState.totalTransferSize + fileSize;
        var formattedTotalUploadSize = this.controller.formatSize(totalTransferSize);        
        var customProgressDetails = "Estimating upload size: " + formattedTotalUploadSize;        
        return {
          totalTransferSize,
          customProgressDetails,
          fileName         
        };
    })
  }

  updateProgressMainMessage(uploading) {
    if(uploading){
      this.updateState({ progressMessage : messages.get(messages.UPLOADING)})
    } else {
      this.updateState({ progressMessage : messages.get(messages.PREPARING)})
    }
  }

  async wait(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  async handleDragAndDropUpload(e) {
    try {
      e.preventDefault();
      const dataItems = e.dataTransfer.items?.length
        ? Array.from(e.dataTransfer.items)
        : Array.from(e.dataTransfer.files); // Fallback for unsupported browsers
  
      if (dataItems.length === 0) {
        this.openErrorDialog("No files dropped!");
        return;
      }
  
      const files = await this.prepareDragAndDopUpload(dataItems);
  
      if (files.length > 0) {
        await this.upload(files);
      } else {
        this.openErrorDialog("No valid files found!");
      }
    } catch (err) {
      console.error(err);
      this.openErrorDialog(
        `Error uploading ${
          files?.map(({ file }) => file.name).join(", ") || "files"
        }: ${err.message || err}`
      );
    } finally {
      this.resetUploadDialogStates();
    }
  }
  
  async prepareDragAndDopUpload(dataItems) {
    const items =[]
    const files = []

    this.updateProgressMainMessage(false)
    this.updateState({
      loading: true,
      progress: 0,
      progressBarFrom: "",
      progressBarTo: this.normalizePath(this.controller.path, '/'),
      totalTransferSize: 0,
      loadingDialogVariant: 'indeterminate'
    })

    // DataTransferItemList is live and may change state as you process it.
    // Specifically, browsers like Chrome can clear e.dataTransfer.items 
    // after the first iteration
    for (let i = 0; i < dataItems.length; i++) {
      const dItem = dataItems[i].webkitGetAsEntry()
      items.push(dItem)
    }

    for (let i = 0; i < items.length; i++) {
      const item = items[i]
      if (item) {
        if (item.isFile) {
          const file = await this.readFile(item)
          files.push({ file, path: "" })
          this.updateSizeCalculationProgress(file.name, file.size)
        } else if (item.isDirectory) {
          this.updateState({ progressBarFrom: item.name })
          const folderFiles = await this.readDirectory(item, item.name)
          files.push(...folderFiles)
        }
      }
    }

    this.updateState({      
      loadingDialogVariant: 'determinate',
      customProgressDetails:null
    });
    this.updateProgressMainMessage(true);

    return files;
  }

  async readFile(fileEntry) {
    return this.fileEntryToFile(fileEntry);
  }
  
  async readDirectory(directoryEntry, currentPath = "") {
    const reader = directoryEntry.createReader();
    let allFiles = [];
    let entries;
  
    do {
      entries = await this.readEntries(reader);
      for (const entry of entries) {
        const entryPath = `${currentPath}/${entry.name}`;
        if (entry.isFile) {
          const file = await this.readFile(entry);
          allFiles.push({ file, path: entryPath });
          this.updateSizeCalculationProgress(file.name, file.size);
        } else if (entry.isDirectory) {
          const folderFiles = await this.readDirectory(entry, entryPath);
          allFiles.push(...folderFiles);
        }
      }
    } while (entries.length > 0);
  
    return allFiles;
  }
  
  async fileEntryToFile(fileEntry) {
    return new Promise((resolve, reject) => {
      fileEntry.file(resolve, reject);
    });
  }
  
  async readEntries(reader) {
    return new Promise((resolve, reject) => {
      reader.readEntries(resolve, reject);
    });
  }


  async handleUploadedFiles(fileList) {
    try {   
      const {file, path} = fileList[0]; 
      const filePath = path ? path : file.name
      const topLevelFolder = filePath.includes('/')
          ? filePath.split('/')[0]
          : file.name;
      const totalSize = this.prepareUpload(topLevelFolder, fileList);
      await this.upload(fileList)
    } catch(err){
      console.error(err)
      this.openErrorDialog(
        `Error uploading ${[...fileList].map(({file}) => file.name).join(", ")}: ` + (err.message || err)
      );
    } finally {
      this.resetUploadDialogStates()
    }
  }


  async handleUpload(event) {
    try {      
      this.handlePopoverClose() 
      let allFiles = [];      
      for (const file of event.target.files ) {                
        allFiles.push({ file, path: file.webkitRelativePath });        
      }
      const {file, path} = allFiles[0]; 
      const filePath = path ? path : file.name
      const topLevelFolder = filePath.includes('/')
          ? filePath.split('/')[0]
          : file.name;
      this.prepareUpload(topLevelFolder, allFiles);
      await this.upload(allFiles)
    } catch(err){
      console.error(err)
      this.openErrorDialog(
        `Error uploading ${[...event.target.files].map(file => file.name).join(", ")}: ` + (err.message || err)
      );
    } finally {
      this.resetUploadDialogStates()
    }
  }

  prepareUpload(topLevelFolder, fileList) {
    this.updateProgressMainMessage(false);
    this.updateState({
      loading: true,
      progress: 0,
      progressBarFrom: topLevelFolder,
      progressBarTo: this.normalizePath(this.controller.path, '/'),
      totalTransferSize: 0,
      loadingDialogVariant: 'indeterminate'
    });

    const totalSize = Array.from(fileList)
      .reduce((acc, {file}) => {        
        this.updateSizeCalculationProgress(file.name, file.size);
        return acc + file.size;
      }, 0);

    this.updateState({
      totalTransferSize: totalSize,      
      loadingDialogVariant: 'determinate',
      customProgressDetails:null
    });
    this.updateProgressMainMessage(true);
    return totalSize;
  }

  normalizePath(basePath, suffix) {        
    if (basePath.endsWith('/')) {
        basePath = basePath.slice(0, -1);
    }    
    if (suffix.startsWith('/')) {
        suffix = suffix.slice(1);
    }
    return `${basePath}/${suffix}`;
  };

  resetUploadDialogStates() {
    this.updateState({
      loading: false,
      processedBytes:0,      
      totalTransferSize:0,      
      applyToAllFiles: false,
      cancelDownload: false,
      allowResume: false,
      progress:0,
      lastConflictResolution: null,
      averageSpeed:0,
      expectedTime: null,
      lastTimestamp:null,
      progressBarFrom:null,
      progressBarTo:null,
      progressBarFrom:null,
      progressBarTo:null,
      progressStatus: null,
      totalSkippedBytes:0
    })
  }

  updateProgressForResolution(uploadedChunkSize, fileName, resolution) {
    let progressStatus = ""
    switch (resolution) {
      case Resolution.RESUME:
        progressStatus = "Resuming..."
        break;
      case Resolution.SKIP:
        progressStatus = "Skipping..."
        break;
      case Resolution.REPLACE:
        progressStatus = "Replacing..."
        break;
      case Resolution.MERGE:
        progressStatus = "Merging..."
        break;
      case Resolution.CANCEL:
        progressStatus = "Cancelling..."
        break;
      default:          
          break;
    }

    this.updateProgress(uploadedChunkSize, fileName, progressStatus, null, resolution)
  }

  updateProgress(uploadedChunkSize, fileName, status, timeElapsed, resolution) {
    
    this.updateState((prevState) => {
      const processedBytes = (prevState.processedBytes ?? 0) + uploadedChunkSize;
      const totalTransferSize = prevState.totalTransferSize;
      const progress = Math.floor((processedBytes / totalTransferSize) * 100);
      var progressStatus = status === "" ? null : status;
      const newProgress = Math.min(progress, 100);

      let totalSkippedBytes = (prevState.totalSkippedBytes ?? 0);
      switch (resolution) {
        case Resolution.RESUME:
        case Resolution.SKIP:
            totalSkippedBytes += uploadedChunkSize;
            break;     
        default:            
            break;
      }     
      
      const processedBytesForSpeed = processedBytes - totalSkippedBytes;
      const totalTransferSizeForSpeed = totalTransferSize - totalSkippedBytes;

      const totalElapsedTime = (prevState.totalElapsedTime || 0) + timeElapsed;
      const averageSpeed = totalElapsedTime > 0 ? processedBytesForSpeed / (totalElapsedTime / 1000) : 0;
      const bytesRemaining = totalTransferSizeForSpeed - processedBytesForSpeed;

      const expectedTime = averageSpeed > 0 ? bytesRemaining / averageSpeed : 0;
  
      return {
        processedBytes,
        progress: newProgress,
        loading: true,        
        fileName,
        averageSpeed,
        totalElapsedTime, 
        expectedTime,       
        progressStatus,
        totalSkippedBytes
      };
    });
  }

  async resolveNameConflict(newFile, allowResume) {
    return new Promise((resolve) => {
      this.updateState({ allowResume, loading: false, progress: 0 })
      this.openFileExistsDialog(newFile)
      this.resolveConflict = resolve
    })
  }

  openFileExistsDialog(newFile) {
    this.updateState({ uploadFileExistsDialogFile: newFile })
  }

  closeFileExistsDialog() {
    this.updateState({ uploadFileExistsDialogFile: null })
  }

  handleUploadClick(event) {
    this.updateState({
      uploadButtonsPopup: event.currentTarget
    })
  }

  handlePopoverClose() {
    this.updateState({
      uploadButtonsPopup: null
    })
  }

  handleFileExistsReplace() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict(Resolution.REPLACE)
    this.updateState({ loading: true, progress: 0,lastConflictResolution: Resolution.REPLACE})
  }

  handleFileExistsResume() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict(Resolution.RESUME)
    this.updateState({ loading: true, progress: 0 ,lastConflictResolution: Resolution.RESUME})
  }

  handleFileExistsSkip() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict(Resolution.SKIP)
    this.updateState({ loading: true, progress: 0 ,lastConflictResolution: Resolution.SKIP})
  }

  handleFileExistsCancel() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict(Resolution.CANCEL)
    this.updateState({ loading: false,lastConflictResolution: Resolution.CANCEL })
  }

  handleApplyToAllSelection(checked) {    
    this.updateState({ applyToAllFiles: checked })    
  }

  async upload(fileList) {      

    for (const fileEntry of fileList) {
      const {file, path} = fileEntry
      const filePath = path ? path : file.name

      const topLevelFolder = filePath.includes('/')
        ? filePath.split('/')[0]
        : file.name;
      this.updateState({ progressBarFrom: topLevelFolder})
      
      const targetFilePath = this.controller.path + '/' + filePath
      const existingFiles = await this.controller.listFiles(targetFilePath)

      const fileExists = existingFiles.length > 0
      const existingFileSize = existingFiles.length === 0 ? 0
        : existingFiles[0].size

      let offset = 0;

      if (fileExists) {
        const resolution = await this.handleExistingFile(file, existingFileSize);
        if (resolution === Resolution.CANCEL) return; // Cancel uploading entirely
        if (resolution === Resolution.SKIP) continue; // Skip this file and continue with others
        offset = resolution === Resolution.REPLACE ? 0 : existingFileSize;// handles resume also                
      }

      // Replace or resume upload from the last point in the file
      while (offset < file.size) {      
        const sizeUploaded = await this.controller.uploadFile(file, targetFilePath,
          offset, this.updateProgress)
        offset += sizeUploaded
      }
    }
    
    if (this.controller.gridController) {
      await this.controller.gridController.load()
    }
  }   


  async handleExistingFile(file, existingFileSize) {
    var state = this.getCurrentState();
    var resolutionResult = state.lastConflictResolution;
    
    if (!state.applyToAllFiles) {      
      resolutionResult = await this.resolveNameConflict(file, true);
    }

    if (resolutionResult === Resolution.SKIP) {
      this.updateState({skipFile :true});
      this.updateProgressForResolution(file.size, file.name, Resolution.SKIP);
      return Resolution.SKIP;
    }

    if (resolutionResult === Resolution.RESUME) {        
      this.updateProgressForResolution(existingFileSize, file.name, Resolution.RESUME);
      return Resolution.RESUME;
    }

    if (resolutionResult === Resolution.CANCEL) {
      this.updateProgressForResolution(file.size, file.name, Resolution.CANCEL);
      return Resolution.CANCEL;
    }

    return Resolution.REPLACE;
  }
}