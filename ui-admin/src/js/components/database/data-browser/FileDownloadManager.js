import messages from '@src/js/common/messages.js'
import autoBind from 'auto-bind'

// 2GB limit for total download size
const ZIP_DOWNLOAD_SIZE_LIMIT = 2147483648

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


  handleProgressUpdate(downloadedChunk, fileName, status, timeElapsed) {        
    this.updateState((prevState) => {      
      const downloadedBytes = prevState.totalDownloaded + downloadedChunk;      
      const progress = Math.round((downloadedBytes / prevState.totalDownloadSize) * 100);
      const newProgress = Math.min(progress, 100);
  
      var progressStatus = status === "" ? null : status;
      const totalElapsedTime = (prevState.totalElapsedTime || 0) + timeElapsed;  
      const averageSpeed = totalElapsedTime > 0 ? downloadedBytes / (totalElapsedTime / 1000) : 0;        

      const bytesRemaining = prevState.totalDownloadSize - downloadedBytes;        
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
        totalDownloaded: downloadedBytes,
        progress: newProgress,
        loading: true,
        fileName,        
        fileName,
        averageSpeed,
        totalElapsedTime, 
        expectedTime,        
        progressStatus,
        remainingFiles
      };
    });
  }

  
  updateSizeCalculationProgress(fileName, fileSize) {    
    this.updateState((prevState) => {
        var totalDownloadSize = prevState.totalDownloadSize + fileSize;
        var formattedTotalDownloadSize = this.controller.formatSize(totalDownloadSize);        
        var customProgressDetails = "Estimating download size: " + formattedTotalDownloadSize;        
        return {
            totalDownloadSize,
            customProgressDetails,
            fileName
        };
    })
  }

  async calculateTotalSize(files) {
    let size = 0;
    let numberOfFiles = 0;
  
    for (const file of files) {
      if (!file.directory) {  
        size += file.size;
        this.updateSizeCalculationProgress(file.name, file.size);
        numberOfFiles++; 
      } else {
        const nestedFiles = await this.controller.listFiles(file.path);
        const nestedStats = await this.calculateTotalSize(nestedFiles);
          
        size += nestedStats.size;
        numberOfFiles += nestedStats.numberOfFiles;
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
    return segments.length > 1 ? "/" + segments[0] : "/";    
  }

  async handleDownloadFiles(multiselectedFiles){    

    this.updateProgressMainMessage(false)
    try {      
      // for chrome and edge
      if (this.isDirectoryPickerAvailable()) {
        await this.handleDownloadWithDirectoryPicker(multiselectedFiles)
      } else {
        // for rest of browsers
        await this.handleDownloadAsBlob(multiselectedFiles)
      }
    } finally {            
      this.resetDownloadDialogStates() 
      this.updateProgressMainMessage(false)
    }

  }


  isDirectoryPickerAvailable() {
    return ('showSaveFilePicker' in window && 'showDirectoryPicker' in window)
  }

    // Start :File System API : Limited availability
  async checkDownloadQuota(fileSize) {
    if (!navigator.storage || !navigator.storage.estimate) {
      // Storage estimation not supported in this browser
      return { hasQuota: true, availableQuota: Infinity }; // Assume enough space as fallback
    }

    try {
      const { quota, usage } = await navigator.storage.estimate();
      const availableQuota = quota - usage;

      if (fileSize > availableQuota) {
        return { hasQuota: false, availableQuota };
      }

      return { hasQuota: true, availableQuota };
    } catch (error) {
      return { hasQuota: false, availableQuota: 0 }; // Default to 0 available on error
    }
  }

  async handleDownloadWithDirectoryPicker(files) {
    const { selectionPossible, rootDirHandle } = await this.handleDirectorySelection()
    if (!selectionPossible) {
      return;
    }
   
    const iterator = files.values()
    const file = iterator.next().value
   

    const topLevelFolder = this.getTopLevelFolder(file.path)
    
    this.showDownloadDialog(topLevelFolder, rootDirHandle.name)
    const totalSize = await this.calculateDownloadTotals(files);

    const { hasQuota, availableQuota } = await this.checkDownloadQuota(totalSize);
    if (hasQuota) {
      this.updateProgressMainMessage(true);      
      if (files.size === 1) {
        const [file] = files; 
        if (!file?.directory) {
          this.updateState({
            showApplyToAll: false
          });
        }
      }

      await this.downloadFilesAndFolders(files, rootDirHandle);
    } else {
      this.showDownloadErrorDialog(availableQuota)
    }
  }

  async calculateDownloadTotals(files){
    this.updateProgressMainMessage(false);      
    var {size, numberOfFiles} = await this.calculateTotalSize(files)
    this.resetStateAfterSizeCalculation();    
    this.updateState({
      totalDownloadSize: size,
      totalFilesToDownload: numberOfFiles,
      remainingFiles: numberOfFiles 
    })
    return size;
  }


  showDownloadDialog(fromDir, toDir){
    this.updateState({ 
      loading: true,
      progress: 0,
      downloadBarFrom: fromDir,
      downloadBarTo: toDir,
      totalDownloaded: 0,
      loadingDialogVariant:'indeterminate' 
    })   
  }

  async handleDirectorySelection() {
    try {
      // Prompt user to select a directory - this should be done 
      // immediately after click, otherwise browser may throws security error 
      // if too much time is taken by other processes before selection    
      const rootDirHandle = await window.showDirectoryPicker()

      const permission = await rootDirHandle.requestPermission({ mode: 'readwrite' });
      if (permission !== 'granted') {
        throw new Error("Permission denied by the user.");
      }

      if (!(await this.isDirectoryEmpty(rootDirHandle))) {
        this.updateState({ showMergeDialog: true })
        const decision = await new Promise((resolve) => {
          this.updateResolveDecision(resolve);
        });
        if (!decision) {
          return { selectionPossible: false, rootDirHandle };
        }
      }
      return { selectionPossible: true, rootDirHandle };
    } catch (err) {
      if (err.name === "AbortError") {
        // no feedback needed, user aborted          
      } else {
        console.error(err)
        this.openErrorDialog("An error occurred while accessing the directory. Please try again."+ err)
      }
      return { selectionPossible: false, rootDirHandle: null };
    } finally {
      this.resetDownloadDialogStates()
    }
  }

  async isDirectoryEmpty(dirHandle) {
    const entries = dirHandle.entries();

    for await (const _ of entries) {
      return false;
    }
    return true;
  }

  async downloadFilesAndFolders(files, parentDirHandle) {
    try {
      for (const file of files) {
        let currentState = this.getCurrentState();
        if (currentState.cancelDownload) {
          return;
        }

        if (!file.directory) {

          const fileExists = await parentDirHandle.getFileHandle(file.name, { create: false }).catch(() => null);

          if (fileExists) {            
            // Skip file logic if apply-to-all and skip were selected earlier
            if (currentState.applyToAllFiles && currentState.skipFile) {
              this.updateProgress(file.size, file.name, "Skipping...")
              continue;
            }

            // Prompt user decision if not applying to all files
            if (!currentState.applyToAllFiles) {              
              this.updateState({ currentFile: file, showFileExistsDialog: true });

              const decision = await new Promise((resolve) => {
                this.updateResolveDecision(resolve);
              });             

              if (currentState.skipFile) {
                this.updateProgress(file.size, file.name, "Skipping...")
                continue;
              }

              if (currentState.cancelDownload) {
                return;
              }
            }
          }

          await this.controller.downloadAndAssemble(file, parentDirHandle, this.updateProgress)

        } else {
          // Handle subfolder recursively
          const dirHandle = await parentDirHandle.getDirectoryHandle(file.name, { create: true })
          const filesInDir = await this.controller.listFiles(file.path)
          await this.downloadFilesAndFolders(filesInDir, dirHandle)
          if (currentState.cancelDownload) {
            return;
          }
        }
      }
    } catch (err) {
      console.error(err)
      this.openErrorDialog(
        `Error downloading ${[...files].map(file => file.name).join(", ")}: ` + (err.message || err)
      );
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
    this.updateState({ loading: true, totalDownloaded: 0, progress: 0 })    
    const totalSize = await this.calculateDownloadTotals(selectedFiles);

    if (totalSize >= ZIP_DOWNLOAD_SIZE_LIMIT) {
      this.showDownloadErrorDialog(ZIP_DOWNLOAD_SIZE_LIMIT)
      return;
    }

    this.updateProgressMainMessage(true);

    const { id } = this.props
    const file = selectedFiles.values().next().value;

    if (selectedFiles.size > 1 || file.directory) {
      // ZIP download

      const zipBlob = await this.prepareZipBlob(selectedFiles)
      this.downloadBlob(zipBlob, id)
      this.zip = new JSZip()
    } else {

      // Single file download
      await this.downloadFile(file)
    }
  }

  async prepareZipBlob(files) {
    for (let file of files) {
      if (!file.directory) {        
        const dataArray = await this.controller.download(file, this.updateProgress)
        this.zip.file(
          file.path,
          new Blob(dataArray, { type: this.inferMimeType(file.path) })
        )
      } else {
        this.zip.folder(file.path)
        const nestedFiles = await this.controller.listFiles(file.path)
        await this.prepareZipBlob(nestedFiles)
      }
    }
    return await this.zip.generateAsync({ type: 'blob' })
  }

  async downloadFile(file) {
    const blob = await this.fileToBlob(file)
    this.downloadBlob(blob, file.name)
  }

  showDownloadErrorDialog(sizeLimit) {
    this.openErrorDialog(messages.get(messages.CANNOT_DOWNLOAD, sizeLimit))
  }

  downloadBlob(blob, fileName) {
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  async fileToBlob(file) {
    const dataArray = await this.controller.download(file, this. downloadManager.updateProgress)
    return new Blob(dataArray, { type: this.inferMimeType(file.path) })
  }


  resetDownloadDialogStates() {
    this.updateState({
      loading:false,
      showFileExistsDialog: false,
      replaceFile: false,
      skipFile: false,
      resolveDecision: false,
      applyToAllFiles: false,
      cancelDownload: false,
      showMergeDialog: false,
      mergeTopLevelFolder: false,      
      lastConflictResolution:null,
      expectedTimeFormatted: null,
      lastTimestamp:null,
      averageSpeed:0,
      downloadBarFrom:null,
      downloadBarTo:null,
      loadingDialogVariant:'determinate',
      customProgressDetails: null,
      totalDownloadSize:0,
      totalDownloaded:0,
      totalFilesToDownload:0,
      totalSavingTime:0
    })
  }

  resetStateAfterSizeCalculation() {
    this.updateState((prevState) => {
      return { loading: true,
        totalDownloaded: 0,
        progress: 0,
        loadingDialogVariant:'determinate',
        customProgressDetails:null
    }})
  }
}