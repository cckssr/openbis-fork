
import messages from '@src/js/common/messages.js'
import autoBind from 'auto-bind'
import JSZip from 'jszip';
import {isUserAbortedError} from "@src/js/components/common/data-browser/DataBrowserUtils.js";

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
      totalSavingTime:0,
      cancelTransfer: false,
      lastConflictResumableResolution: null,
      lastConflictResolution: null,
      totalSkippedBytes:0,                 
      fileName:null,
      averageSpeed:0,
      totalElapsedTime:0, 
      expectedTime:0,       
      progressStatus:null,
      applyResolutionToAllResumableFiles:false
    }
  }


  updateSizeCalculationProgress(fileName, fileSize) {
    this.updateState((prevState) => {
        var totalTransferSize = prevState.totalTransferSize + fileSize;
        var formattedTotalUploadSize = this.controller.formatSize(totalTransferSize);        
        var customProgressDetails = messages.get(messages.UPLOAD_ESTIMATE_SIZE, formattedTotalUploadSize);        
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
    let files;
    try {
      e.preventDefault();
      const dataItems = e.dataTransfer.items?.length
        ? Array.from(e.dataTransfer.items)
        : Array.from(e.dataTransfer.files); // Fallback for unsupported browsers
  
      if (dataItems.length === 0) {
        this.openErrorDialog(messages.get(messages.UPLOAD_NO_DROPPED_FILES));
        return;
      }
  
      files = await this.prepareDragAndDopUpload(dataItems);
  
      if (files.length > 0) {
        await this.upload(files);
      } else {
        this.openErrorDialog(messages.get(messages.UPLOAD_NO_VALID_FILES));
      }
    } catch (err) {
      console.error(err)
      if (isUserAbortedError(err)) {
        // no feedback needed, user aborted          
      } else {
        const fileNames = files?.map(({ file }) => file.name).join(", ") || "files";
        this.openErrorDialog(
          `Error uploading ${fileNames}: ${err.message || err}`
        )
      }
    } finally {
      this.resetUploadDialogStates();
      await this.controller.gridController.load()
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
      progressBarTo: this.getTopLevelFolder(this.controller.path),
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
          this.throwAbortErrorIfTransferCancelled();
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
          this.throwAbortErrorIfTransferCancelled()
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
      if (isUserAbortedError(err)) {
        // no feedback needed, user aborted          
      } else {
        this.openErrorDialog(
          `Error uploading ${[...fileList].map(({file}) => file.name).join(", ")}: ` + (err.message || err)
        )
      }
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
      if (isUserAbortedError(err)) {
        // no feedback needed, user aborted          
      } else {
        this.openErrorDialog(
          `Error uploading ${[...event.target.files].map(file => file.name).join(", ")}: ` + (err.message || err)
        )
      }      
    } finally {
      this.resetUploadDialogStates()
      await this.controller.gridController.load()
    }
  }

  prepareUpload(topLevelFolder, fileList) {
    this.updateProgressMainMessage(false);
    this.updateState({
      loading: true,
      progress: 0,
      progressBarFrom: topLevelFolder,
      progressBarTo: this.getTopLevelFolder(this.controller.path),
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

  getTopLevelFolder(basePath) {        
    if (basePath.endsWith('/')) {
        basePath = basePath.slice(0, -1);
    }       
    const lastFolder = basePath.includes('/') 
        ? basePath.substring(basePath.lastIndexOf('/') + 1) 
        : basePath;    
    
    return lastFolder || '/';
}

  resetUploadDialogStates() {
    this.updateState({
      ...this.getDefaultState()
    })
  }

  throwAbortErrorIfTransferCancelled(){    
    if(this.getCurrentState().cancelTransfer){
      throw new DOMException('Upload was cancelled.', 'AbortError');
    }
  }

  updateProgressForResolution(uploadedChunkSize, fileName, resolution) {
    let progressStatus = ""
    switch (resolution) {
      case Resolution.RESUME:
        progressStatus = messages.get(messages.UPLOAD_DOWNLOAD_RESUMING)
        break;
      case Resolution.SKIP:
        progressStatus = messages.get(messages.UPLOAD_DOWNLOAD_SKIPPING);
        break;
      case Resolution.REPLACE:
        progressStatus = messages.get(messages.UPLOAD_DOWNLOAD_REPLACING);
        break;
      case Resolution.MERGE:
        progressStatus = messages.get(messages.UPLOAD_DOWNLOAD_MERGING);
        break;
      case Resolution.CANCEL:
        progressStatus = messages.get(messages.UPLOAD_DOWNLOAD_CANCELING);
        break;
      default:          
          break;
    }

    this.updateProgress(uploadedChunkSize, fileName, progressStatus, null, resolution)
  }

  updateProgress(uploadedChunkSize, fileName, status, timeElapsed, resolution) {
    if(this.getCurrentState().cancelTransfer){
      throw new DOMException('The operation was aborted.', 'AbortError');
    }

    this.updateState((prevState) => {
      const processedBytes = (prevState.processedBytes ?? 0) + uploadedChunkSize;
      const totalTransferSize = prevState.totalTransferSize;
      const progressFloor = Math.floor((processedBytes / totalTransferSize) * 100);
      var progressStatus = status === "" ? null : status;
      const newProgress = Math.min(progressFloor, 100);

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

  async resolveNameConflict(newFile, allowResume, allowSkip, showApplyToAll) {
    return new Promise((resolve) => {
      this.updateState({ allowResume, allowSkip,showApplyToAll,loading: false, progress: 0})
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
    
    this.updateState((prevState) => ({
       loading: true,
        progress: 0,
        lastConflictResolution: Resolution.REPLACE,
        applyResolutionToAllResumableFiles: prevState.allowResume ? prevState.applyToAllFiles : prevState.applyResolutionToAllResumableFiles,
        lastConflictResumableResolution: prevState.allowResume ? Resolution.REPLACE :prevState.lastConflictResumableResolution
      }))
  }

  handleFileExistsResume() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict(Resolution.RESUME)
    this.updateState({ loading: true, progress: 0 , applyResolutionToAllResumableFiles: this.getCurrentState().applyToAllFiles, lastConflictResumableResolution: Resolution.RESUME})
  }

  handleFileExistsSkip() {
    this.closeFileExistsDialog()
    this.resolveConflict && this.resolveConflict(Resolution.SKIP)
    this.updateState((prevState) => ({
      loading: true,
       progress: 0,
       lastConflictResolution: Resolution.SKIP,
       applyResolutionToAllResumableFiles: prevState.allowResume ? prevState.applyToAllFiles : prevState.applyResolutionToAllResumableFiles,
       lastConflictResumableResolution: prevState.allowResume ? Resolution.SKIP :prevState.lastConflictResumableResolution
     }))    
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
            
      const targetFilePath = this.controller.path +  filePath
      const tempTargetFilePath = this.controller.path + filePath + ".part"
      const tempTargetFileName = file.name + ".part"

      const {
        fileExists,
        existingFile,
        fileTempExists,
        existingTempFile
      }  = await this.checkFilesAndTempExistOnServer(file.name, tempTargetFileName, targetFilePath)

      let offset = 0;

      if (fileExists || fileTempExists) {
        const resolution = await this.handleExistingFile(file, fileExists,existingFile,
          fileTempExists, existingTempFile, fileList.length);
                  
        if (resolution === Resolution.CANCEL) return; // Cancel uploading entirely
        if (resolution === Resolution.SKIP) continue; // Skip this file and continue with others        
        if(resolution == Resolution.REPLACE){
          offset = 0
          if(fileTempExists){           
            await this.controller.deleteAndUpdateProgress(existingTempFile,this.updateProgress)
          }
        } else {
          offset = existingTempFile.size
        }             
      }

      if(file.size < 1){
        continue
      }

      // Replace or resume upload from the last point in the file
      while (offset < file.size) {      
        const sizeUploaded = await this.controller.uploadFile(file, tempTargetFilePath,
          offset, this.updateProgress)
        this.throwAbortErrorIfTransferCancelled()
        offset += sizeUploaded
      }
      
      if(fileExists){
        await this.controller.deleteAndUpdateProgress(existingFile, this.updateProgress)
      }

      await this.controller.moveFileByPath(tempTargetFilePath, targetFilePath, this.updateProgress)
    }
    
    if (this.controller.gridController) {
      await this.controller.gridController.load()
    }
  }   

  async checkFilesAndTempExistOnServer(fileName, tmpFileName, targetFilePath) {
    const parentFolderPath = this.getParentFolder(targetFilePath);
    const filesInParentFolder = await this.controller.listFilesAndUpdateProgress(parentFolderPath, this.updateProgress);

    let fileExists = false
    let existingFile = null    
    let fileTempExists = false
    let existingTempFile = null    

    for (const file of filesInParentFolder) {
      if (file.name === fileName) {
        fileExists = true        
        existingFile = file
      } else if (file.name === tmpFileName) {
        fileTempExists = true
        existingTempFile = file
      }
      
      if (fileExists && fileTempExists) {
        break;
      }      
    }

    return {
        fileExists,
        existingFile,
        fileTempExists,
        existingTempFile
      }
  }



  getParentFolder(filePath) {
    const normalizedPath = filePath.replace(/\/+$/, '');
    if (normalizedPath === '/') {
        return '/';
    }
    const lastSlashIndex = normalizedPath.lastIndexOf('/');
    if (lastSlashIndex === 0) {
        return '/';
    }
    return normalizedPath.substring(0, lastSlashIndex);
}


  async handleExistingFile(file, fileExists, existingFile,
        fileTempExists,tempFile, numberFilesUploaded) {

    var state = this.getCurrentState()
    var resolutionResult = state.lastConflictResolution
    var lastConflictResumableResolution = state.lastConflictResumableResolution
    var applyResolutionToAllResumableFiles = state.applyResolutionToAllResumableFiles
    const allowResume = fileTempExists && (tempFile.size < file.size) 

    const isLastResumableResolutionValid = applyResolutionToAllResumableFiles 
        && lastConflictResumableResolution
        && allowResume;

    const isApplyToAllValid = state.applyToAllFiles 
          && resolutionResult && !allowResume;

    const lastResolutionStillValid = isLastResumableResolutionValid || isApplyToAllValid;        

    if(allowResume){
      resolutionResult = lastConflictResumableResolution
    }
    
    if (!lastResolutionStillValid) {         
      const multiFileUpload = numberFilesUploaded > 1     
      const fileInDialog = tempFile != null ? tempFile : existingFile     
      resolutionResult = await this.resolveNameConflict(fileInDialog, allowResume,
          multiFileUpload , multiFileUpload)   
    }

    if (allowResume && (resolutionResult === Resolution.RESUME)) {              
      this.updateProgressForResolution(tempFile.size, file.name, Resolution.RESUME)
      return Resolution.RESUME
    }

    if (resolutionResult === Resolution.SKIP) {
      this.updateState({skipFile :true})
      this.updateProgressForResolution(file.size, file.name, Resolution.SKIP)
      return Resolution.SKIP
    }

    if (resolutionResult === Resolution.CANCEL) {
      this.updateProgressForResolution(file.size, file.name, Resolution.CANCEL)
      return Resolution.CANCEL
    }

    return Resolution.REPLACE
  }
}