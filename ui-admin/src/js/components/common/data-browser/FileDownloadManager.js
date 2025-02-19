import messages from '@src/js/common/messages.js'
import autoBind from 'auto-bind'
import mimeTypeMap from '@src/js/components/common/data-browser/mimeTypes.js';
import JSZip from 'jszip';
import {isUserAbortedError} from "@src/js/components/common/data-browser/DataBrowserUtils.js";

// 2GB limit for total download size
const ZIP_DOWNLOAD_SIZE_LIMIT = 2147483648

const Resolution = {
  MERGE: 'MERGE',
  SKIP: 'SKIP',
  RESUME: 'RESUME',
  CANCEL: 'CANCEL',
  REPLACE: 'REPLACE'
};

export default class FileDownloadManager {

  constructor(controller, getCurrentState, updateStateCallback, openErrorDialog, updateResolveDecision) {
    autoBind(this)
    this.controller = controller
    this.getCurrentState = getCurrentState     
    this.updateState = updateStateCallback
    this.openErrorDialog = openErrorDialog    
    this.updateResolveDecision = updateResolveDecision
    this.zip = new JSZip()    
  }

  getDefaultState() {
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
      cancelTransfer: false,
      applyToAllFiles: false,
      showMergeDialog: false,
      resolveMergeDecision: null,
      showApplyToAll: true,
      resolveDecision: null,     
      rollingSpeed:0,
      totalSavingTime:0,
      totalSkippedBytes:0,
      cancelTransfer:false,
      fileName : null,
      totalElapsedTime:0, 
      expectedTime:0,        
      progressStatus:null,
      remainingFiles:0
    }
  }

  inferMimeType(fileName) {
    const extension = fileName.slice(fileName.lastIndexOf('.')).toLowerCase()
    return mimeTypeMap[extension] || 'application/octet-stream'
  }

  throwAbortErrorIfTransferCancelled(){    
    if(this.getCurrentState().cancelTransfer){
      throw new DOMException('Download was cancelled.', 'AbortError');
    }
  }

  updateProgress(downloadedChunk, fileName, status, timeElapsed, savingTime=0) {        
    if(savingTime > 0){      
      this.updateState((prevState) => {     
        const totalSavingTime = prevState.totalSavingTime + savingTime      
        const remainingFiles =  prevState.remainingFiles - 1 
        return {totalSavingTime , remainingFiles}
      })
    } else {
      this.handleProgressUpdate(downloadedChunk, fileName, status, timeElapsed)
    }
  }

  updateProgressForResolution(downloadedChunk, fileName, resolution) {
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

    this.handleProgressUpdate(downloadedChunk, fileName, progressStatus, null, resolution)
  }

  handleProgressUpdate(downloadedChunk, fileName, status, timeElapsed, resolution) {   
    if(this.getCurrentState().cancelTransfer){
      throw new DOMException('The operation was aborted.', 'AbortError');
    }

    this.updateState((prevState) => {      
      const processedBytes = prevState.processedBytes + downloadedChunk;  
      const totalTransferSize = prevState.totalTransferSize;   
      const progress = Math.floor((processedBytes / totalTransferSize) * 100);
      const newProgress = Math.min(progress, 100);
  
      var progressStatus = status === "" ? null : status;

      let totalSkippedBytes = (prevState.totalSkippedBytes ?? 0);
      switch (resolution) {
        case Resolution.RESUME:
        case Resolution.SKIP:
            totalSkippedBytes += downloadedChunk;
            break;     
        default:            
            break;
      }    
      
      const processedBytesForSpeed = processedBytes - totalSkippedBytes;
      const totalTransferSizeForSpeed = totalTransferSize - totalSkippedBytes;

      const totalElapsedTime = (prevState.totalElapsedTime || 0) + timeElapsed;  
      const averageSpeed = totalElapsedTime > 0 ? processedBytesForSpeed / (totalElapsedTime / 1000) : 0;

      const bytesRemaining = totalTransferSizeForSpeed - processedBytesForSpeed;
      const expectedDownloadTime = averageSpeed > 0 ? bytesRemaining / averageSpeed : 0; 

      const remainingFiles = prevState.remainingFiles

      const numberOfFilesSaved = prevState.totalFilesToDownload - remainingFiles
      
      // average only after 10 files, otherwise default to 100ms per file save
      const averageSavingTimePerFile = numberOfFilesSaved > 10
          ? prevState.totalSavingTime / numberOfFilesSaved
          : 100; 

      const expectedSavingTime = remainingFiles * (averageSavingTimePerFile / 1000); 

      const expectedTime = expectedDownloadTime + expectedSavingTime;

      return {
        processedBytes,
        progress: newProgress,
        loading: true,
        fileName,
        averageSpeed,
        totalElapsedTime, 
        expectedTime,        
        progressStatus,
        remainingFiles,
        totalSkippedBytes
      };
    });
  }

  
  updateSizeCalculationProgress(fileName, fileSize) {
    this.updateState((prevState) => {
        var totalTransferSize = prevState.totalTransferSize + fileSize;
        var formattedtotalTransferSize = this.controller.formatSize(totalTransferSize);        
        var customProgressDetails = messages.get(messages.DOWNLOAD_ESTIMATE_SIZE ,formattedtotalTransferSize);        
        return {
            totalTransferSize,
            customProgressDetails,
            fileName,
            progressStatus: null
        };
    })
  }

  async calculateTotalSize(files, maxAllowedSize) {
    let size = 0;
    let numberOfFiles = 0;
  
    for (const file of files) {
      if (size > maxAllowedSize) {
        break;
      }
  
      if (!file.directory) {
        size += file.size;
        this.updateSizeCalculationProgress(file.name, file.size);
        this.throwAbortErrorIfTransferCancelled()
        numberOfFiles++;
      } else {
        const nestedFiles = await this.controller.listFilesAndUpdateProgress(file.path,this.updateProgress)
        const nestedStats = await this.calculateTotalSize(nestedFiles, maxAllowedSize - size);
  
        size += nestedStats.size;
        numberOfFiles += nestedStats.numberOfFiles;
          
        if (size > maxAllowedSize) {          
          break;
        }
      }
    }
  
    return { size, numberOfFiles };
  }

  updateProgressMainMessage(downloading) {
    if(downloading){
      this.updateState({ progressMessage : messages.get(messages.DOWNLOADING)})
    } else {
      this.updateState({ progressMessage : messages.get(messages.PREPARING)})
    }
  }

  getTopLevelFolder(filePath) {    
    const normalizedPath = filePath.replace(/\/+/g, '/');
    const cleanPath = normalizedPath.startsWith('/') ? normalizedPath.slice(1) : normalizedPath;
    const segments = cleanPath.split('/');
    return segments.length > 2 ? segments[segments.length - 2] : "/";    
  }

  getContainingFolder(filePath) {    
    let normalizedPath = filePath.replace(/\/+/g, '/');
    if (normalizedPath.length > 1 && normalizedPath.endsWith('/')) {
      normalizedPath = normalizedPath.slice(0, -1);
    }
  
    const lastSlashIndex = normalizedPath.lastIndexOf('/');
    let folderPath = normalizedPath.slice(0, lastSlashIndex + 1);
    if (!folderPath) {
      folderPath = '/';
    }
  
    return folderPath;
  }
  
  async handleDownloadFiles(multiselectedFiles){    

    this.updateProgressMainMessage(false)
    try {      
      // for chrome and edge
      if (this.isFileSystemAccessApiAvailable()) {
        await this.handleDownloadWithDirectoryPicker(multiselectedFiles)
      } else {
        // for rest of browsers
        await this.handleDownloadAsBlob(multiselectedFiles)
      }
    }catch (err){
      if (isUserAbortedError(err)) {
          // no feedback needed, user aborted          
      } else {    
        if (typeof console !== 'undefined' && typeof console.error === 'function') {
          console.error(err); 
        }
        this.openErrorDialog(`Error downloading : ` + (err.message || err))        
      }
    } finally {            
      this.resetDownloadDialogStates() 
      this.updateProgressMainMessage(false)
    }

  }


  // Start :File System API : Limited availability
  isFileSystemAccessApiAvailable() {
    
    const hasFileSystemAccess =
      'showSaveFilePicker' in window &&
      'showOpenFilePicker' in window &&
      'showDirectoryPicker' in window;  
    
    const isSecureContext = window.isSecureContext;
  
    return hasFileSystemAccess && isSecureContext;
  }

  async checkDownloadQuota(fileSize) {
    if (!navigator.storage || !navigator.storage.estimate) {
      // Storage estimation not supported in this browser
      return Infinity ; // Assume enough space as fallback
    }

    try {
      const { quota, usage } = await navigator.storage.estimate();
      return quota - usage;      
    } catch (error) {
      return 0; // Default to 0 available on error
    }
  }

  async handleDownloadWithDirectoryPicker(files) {
    const rootDirHandle =  await this.handleDirectorySelection()
    if(rootDirHandle == null)  {
      return
    }

    const selectionPossible = await this.validateFolderAndShowMergeDialog(rootDirHandle, files)
    if (!selectionPossible) {
      return
    }

    const iterator = files.values()
    const firstFile = iterator.next().value
   
    const topLevelFolder = this.getTopLevelFolder(firstFile.path)
    
    this.showDownloadDialog(topLevelFolder, rootDirHandle.name)
    const availableQuota  = await this.checkDownloadQuota(totalSize);
    const totalSize = await this.calculateDownloadTotals(files, availableQuota);   
    
    if (availableQuota >= totalSize) {           
      
      this.updateProgressMainMessage(true);      
      const singleFile = files.size === 1 && ![...files][0]?.directory;

      this.updateState({
        allowResume: false, // not available for download
        showApplyToAll: !singleFile,
        allowSkip: !singleFile,
      });
      const localDirPath = rootDirHandle.name + "/"
      await this.downloadFilesAndFolders(Array.from(files), rootDirHandle, localDirPath);
    } else {
      this.showDownloadErrorDialog(availableQuota)
    }
  }

  async calculateDownloadTotals(files, maxAllowedSize){
    this.updateProgressMainMessage(false);      
    var {size, numberOfFiles} = await this.calculateTotalSize(files,maxAllowedSize)
    this.resetStateAfterSizeCalculation();    
    this.updateState({
      totalTransferSize: size,
      totalFilesToDownload: numberOfFiles,
      remainingFiles: numberOfFiles 
    })
    return size;
  }


  showDownloadDialog(fromDir, toDir){
    this.updateState({ 
      loading: true,
      progress: 0,
      progressBarFrom: fromDir,
      progressBarTo: toDir,
      processedBytes: 0,
      loadingDialogVariant:'indeterminate' 
    })   
  }

  async handleDirectorySelection() {    
      // Prompt user to select a directory - this should be done 
      // immediately after click, otherwise browser may throw security error 
      // if too much time is taken by other processes before selection    
      const rootDirHandle = await window.showDirectoryPicker()

      const permission = await rootDirHandle.requestPermission({ mode: 'readwrite' });
      if (permission !== 'granted') {
        throw new Error(messages.get(messages.DOWNLOAD_PERMISSION_DENIED));
      }
    
      return rootDirHandle     
  }

  async validateFolderAndShowMergeDialog(rootDirHandle, files) {
    const fileIterator = files.values();
    const firstFile = fileIterator.next().value;
  
    const isSingleFile = firstFile && fileIterator.next().done;
    const isSingleFileValid = isSingleFile && !firstFile.directory;    
  
    if (!isSingleFileValid && !(await this.isDirectoryEmpty(rootDirHandle))) {
      this.updateState({ showMergeDialog: true });
  
      const userDecision = await new Promise((resolve) => {
        this.updateResolveDecision(resolve);
      });
  
      return userDecision;
    }
    return true;
  }
  
  


  async isDirectoryEmpty(dirHandle) {

    for await (const _ of dirHandle.entries()) {
      return false;
    }
    return true;
  }

  async downloadFilesAndFolders(files, parentDirHandle, localDirPath) {
       
    for (const file of files) {
      let currentState = this.getCurrentState();
      if (currentState.cancelTransfer) {
        return;
      }

      if (!file.directory) {

        const fileExists = await parentDirHandle.getFileHandle(file.name, { create: false }).catch(() => null);

        if (fileExists) {            
          // Skip file logic if apply-to-all and skip were selected earlier
          if (currentState.applyToAllFiles && currentState.skipFile) {
            this.updateProgressForResolution(file.size, file.name, Resolution.SKIP)
            continue;
          }

          // Prompt user decision if not applying to all files
          if (!currentState.applyToAllFiles) {              
            this.updateState({ currentFile: localDirPath + file.name, 
              showFileExistsDialog: true ,               
              loading: false,
              progress: 0, 
            });

            const decision = await new Promise((resolve) => {
              this.updateResolveDecision(resolve);
            });             

            currentState = this.getCurrentState();
            if (currentState.skipFile) {
              this.updateProgressForResolution(file.size, file.name, Resolution.SKIP)
              continue;
            }

            if (currentState.cancelTransfer) {
              return;
            }
          }
        }

        await this.controller.downloadAndAssemble(file, parentDirHandle,
                  this.updateProgress, this.throwAbortErrorIfTransferCancelled)

      } else {
        // Handle subfolder recursively          
        const dirHandle = await parentDirHandle.getDirectoryHandle(file.name, { create: true })
        const filesInDir = await this.controller.listFilesAndUpdateProgress(file.path,this.updateProgress)
        localDirPath += file.name + "/"
        await this.downloadFilesAndFolders(filesInDir, dirHandle, localDirPath)
        if (currentState.cancelTransfer) {
          return;
        }
      }
    }
   
  }

  async fileExistsInDirectory(dirHandle, fileName) {
    try {
      await dirHandle.getFileHandle(fileName); // Attempt to get file handle
      return true; // File exists
    } catch (err) {
      if (err.name === "NotFoundError") {
        return false; // File does not exist
      }
      throw err; // Re-throw unexpected errors
    }
  }

  // End :File System API : Limited availability

  async handleDownloadAsBlob(selectedFiles) {
    if(selectedFiles && selectedFiles.size == 0){
      return
    }
    const file = selectedFiles.values().next().value;

    const topLevelFolder = this.getTopLevelFolder(file.path)
    
    this.showDownloadDialog(topLevelFolder, messages.get(messages.DOWNLOAD_TO_DEFAULT_FOLDER))   
   
    const totalSize = await this.calculateDownloadTotals(selectedFiles, ZIP_DOWNLOAD_SIZE_LIMIT);

    if (totalSize >= ZIP_DOWNLOAD_SIZE_LIMIT) {
      this.showDownloadErrorDialog(ZIP_DOWNLOAD_SIZE_LIMIT)
      return;
    }

    this.updateProgressMainMessage(true);
    
    this.resetStateAfterSizeCalculation();    

    if (selectedFiles.size > 1 || file.directory) {
      // ZIP download
      const containingFolder = this.getContainingFolder(file.path)
      const zipFileName = this.createZipFileNameFromPath(file.path, file.name)
      const zipBlob = await this.prepareZipBlob(selectedFiles, zipFileName, containingFolder)    
    
      this.downloadBlob(zipBlob, zipFileName)
      this.zip = new JSZip()
    } else {

      // Single file download
      await this.downloadFile(file)
    }
  }
  createZipFileNameFromPath(filePath, fileName, maxLength = 50) {
    const sanitizedPath = filePath.startsWith('/') ? filePath.slice(1) : filePath;
    let pathWithoutLastElement = sanitizedPath.split('/').slice(0, -1).join('/');
    if (!pathWithoutLastElement || pathWithoutLastElement === '') {
      pathWithoutLastElement = fileName;
    }
    const zipFileName = pathWithoutLastElement.replace(/\//g, '-');
    let finalFileName = `openbis_${zipFileName}`;
    if (finalFileName.length > maxLength - 4) {
      finalFileName = finalFileName.slice(0, maxLength - 4);
    }
    return `${finalFileName}.zip`;
  }
  
 
  async prepareZipBlob(files, zipFileName, containingFolder) {
    for (let file of files) {
      const filePath = this.removeContainingFolder(file.path, containingFolder)
      if (!file.directory) {        
        const dataArray = await this.controller.download(file, this.updateProgress, this.throwAbortErrorIfTransferCancelled)
        this.zip.file(
          filePath,
          new Blob(dataArray, { type: this.inferMimeType(file.path) })
        )
      } else {
        this.zip.folder(filePath)
        const nestedFiles = await this.controller.listFilesAndUpdateProgress(file.path,this.updateProgress)
        await this.prepareZipBlob(nestedFiles, zipFileName, containingFolder)
      }
    }

    return await this.generateWithProgress(zipFileName);
  }

  removeContainingFolder(filePath, folderPath) {
    if (!folderPath.endsWith('/')) {
      folderPath += '/';
    }
    if (filePath.startsWith(folderPath)) {
      return filePath.slice(folderPath.length);
    }
    return filePath;
  }

  // message will only update on UI if it takes longer than 1 sec
  async generateWithProgress(zipFileName) {
    let progressTimeout;
    let isTimeoutElapsed = false;
  
    const timeoutPromise = new Promise((resolve) => {
        progressTimeout = setTimeout(() => {
            isTimeoutElapsed = true;
            this.updateProgress(0, zipFileName, messages.get(messages.DOWNLOAD_STATUS_ZIP_FILES), 0, 0);
            resolve();
        }, 1000);
    });
    
    const zipPromise = this.zip.generateAsync({ type: 'blob' });


    const result = await Promise.race([timeoutPromise, zipPromise]);
    clearTimeout(progressTimeout);
    if (result !== undefined) {
        return result;
    }

    return await zipPromise;
}

  async downloadFile(file) {
    const blob = await this.fileToBlob(file)   
    this.downloadBlob(blob, file.name)
  }

  showDownloadErrorDialog(sizeLimit) {
    this.openErrorDialog(messages.get(messages.CANNOT_DOWNLOAD, sizeLimit))
  }

  downloadBlob(blob, fileName) {
    this.updateProgress(0, fileName, messages.get(messages.DOWNLOAD_STATUS_SAVING_FILES), 0, 0) 
    this.throwAbortErrorIfTransferCancelled()

    const link = document.createElement('a')
    const objectURL = window.URL.createObjectURL(blob)
    link.download = fileName
    link.href = objectURL
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(objectURL)
  }

  async fileToBlob(file) {
    const dataArray = await this.controller.download(file, this.updateProgress, this.throwAbortErrorIfTransferCancelled)
    return new Blob(dataArray, { type: this.inferMimeType(file.path) })
  }


  resetDownloadDialogStates() {
    this.updateState({
      ...this.getDefaultState()
    })
  }

  resetStateAfterSizeCalculation() {
    this.updateState((prevState) => {
      return { loading: true,
        processedBytes: 0,
        progress: 0,
        loadingDialogVariant:'determinate',
        customProgressDetails:null
    }})
  }
}