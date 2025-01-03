/*
 * Copyright ETH 2023 Zürich, Scientific IT Services
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
 */

import ComponentController from '@src/js/components/common/ComponentController.js'
import autoBind from 'auto-bind'
import openbis from '@src/js/services/openbis.js'
import error from '../../common/error/ErrorObject'

export default class DataBrowserController extends ComponentController {

  constructor(owner) {
    super()
    autoBind(this)

    this.owner = owner
    this.gridController = null
    this.path = ''
    this.fileNames = []
    this.CHUNK_SIZE = 1024 * 1024 * 10;// 10MiB
  }

  async free() {
    try {
      return await openbis.free(this.owner, this.path)
    } catch (error) {
      if (error.message.includes('NoSuchFileException')) {
        return []
      } else {
        throw error
      }
    }
  }

  async listFiles(path) {
    // Use this.path if path is not specified
    const pathToList = path ? path : this.path
    try {
      return await openbis.list(this.owner, pathToList, false)
    } catch (error) {
      if (error.message.includes('NoSuchFileException')) {
        return []
      } else {
        throw error
      }
    }
  }

  async load() {
    return await this.handleError(async() => {
      const files = await this.listFiles()
      this.fileNames = files.map(file => file.name)
      return files.map(file => ({ id: file.name, ...file }))
    })
  }

  async loadFolders() {
    return await this.handleError(async() => {
      const files = await this.listFiles()
      this.fileNames = files.map(file => file.name)
      return files.filter(file => file.directory).map(file => ({ id: file.name, ...file }))
    })
  }

  async createNewFolder(name) {
    await this.handleError(async () => {
      await openbis.create(this.owner, this.path + name, true)
    })

    if (this.gridController) {
      await this.gridController.load()
    }
  }

  async rename(oldName, newName) {
    await this.handleError(async () => {
      await openbis.move(this.owner, this.path + oldName, this.owner, this.path + newName)
    })
    if (this.gridController) {
      await this.gridController.load()
    }
  }

  async delete(files) {
    await this.handleError(async () => {
      for (const file of files) {
        await this._delete(file)
      }
    })

    if (this.gridController) {
      await this.gridController.load()
    }
  }

  async _delete(file) {
    await openbis.delete(this.owner, file.path)
  }

  async copy(files, newLocation) {
    await this.handleError(async () => {
      for (const file of files) {
        await this._copy(file, newLocation)
      }
    })

    if (this.gridController) {
      await this.gridController.clearSelection()
    }
  }

  async _copy(file, newLocation){
    if (!this.isSubdirectory(file.path, newLocation)) {
      const cleanNewLocation = this._removeLeadingSlash(newLocation) + file.name
      await openbis.copy(this.owner, file.path, this.owner, cleanNewLocation)
    }
  }

  async move(files, newLocation) {
    await this.handleError(async () => {
      for (const file of files) {
        await this._move(file, newLocation)
      }
    })

    if (this.gridController) {
      await this.gridController.load()
    }
  }

  async _move(file, newLocation){
    if (!this.isSubdirectory(file.path, newLocation)) {
      const cleanNewLocation = this._removeLeadingSlash(newLocation) + file.name
      await openbis.move(this.owner, file.path, this.owner, cleanNewLocation)
    }
  }

  isSubdirectory(parentPath, childPath) {
    // Normalize paths to remove trailing slashes and ensure uniformity
    const normalizedParentPath = parentPath.replace(/\/+$/, "")
    const normalizedChildPath = childPath.replace(/\/+$/, "")

    // Check if the child path starts with the parent path and has a directory separator after it
    return (
      normalizedChildPath.startsWith(normalizedParentPath) &&
      (normalizedChildPath[normalizedParentPath.length] === "/" ||
        normalizedParentPath.length === normalizedChildPath.length)
    )
  }


  async uploadFile(file, targetFilePath, offset,onProgressUpdate) {
    const blob = file.slice(offset, offset + this.CHUNK_SIZE)
    const arrayBuffer = await blob.arrayBuffer();
    const data = new Uint8Array(arrayBuffer);

    const uploadStartTime = Date.now();
    await this._uploadChunk(targetFilePath, offset, data)
    const uploadEndTime = Date.now();            
    // Calculate download speed      
    const elapsedTime = (uploadEndTime - uploadStartTime);
    
    onProgressUpdate(blob.size,file.name, "", elapsedTime)
    return blob.size;
  }

  async handleError(fn) {
    try {
      return await fn()
    } catch (e) {
      const message = e.message || (e.t0 ? e.t0.message || e.t0 : e)
      this.setState({ errorMessage: message })
    }
  }

  async _fileSliceToBinaryString(blob) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result)
      reader.onerror = (error) => reject(error)
      reader.readAsBinaryString(blob)
    })
  }

  async _uploadChunk(source, offset, data) {
    return await openbis.write(this.owner, source, offset, data)
  }

  async download(file, onProgressUpdate) {
    let offset = 0
    const dataArray = []

    while (offset < file.size) {
      const downloadStartTime = Date.now();
      const blob = await this._download(file, offset)
      const downloadEndTime = Date.now();
      dataArray.push(await blob.arrayBuffer())
      offset += this.CHUNK_SIZE
      
      const elapsedTime = (downloadEndTime - downloadStartTime) / 1000; // Seconds
      
      onProgressUpdate(blob.size,file.name, "", elapsedTime)
    }

    return dataArray
  }

  async _createWritableStream(dirHandle, fileName) {
    // Create or access the file in the selected directory
    const fileHandle = await dirHandle.getFileHandle(fileName, { create: true });
    return await fileHandle.createWritable();
  }

  async downloadAndAssemble(file, dirHandle, onProgressUpdate) {
    let offset = 0;    
    // Create a writable stream for the file in the selected directory
    const fileStream = await this._createWritableStream(dirHandle, file.name);

    let writeToDiskOffset = 0;
    var dataArrayForDisk = []
    while (offset < file.size) {
      const downloadStartTime = Date.now();
      const blob = await this._download(file, offset);
      const downloadEndTime = Date.now();

      dataArrayForDisk.push(await blob.arrayBuffer());
      offset += this.CHUNK_SIZE;

      writeToDiskOffset += this.CHUNK_SIZE;

      // Calculate download speed      
      const elapsedTime = (downloadEndTime - downloadStartTime)      

      onProgressUpdate(blob.size ,file.name, "", elapsedTime)

      // write to file only when almost 100MB
      if(writeToDiskOffset > 100_000_000 || offset >= file.size){

        // Write the chunk directly to the file
        var combinedBuffer = new Uint8Array(dataArrayForDisk.reduce((acc, buf) => acc + buf.byteLength, 0));
        let offset = 0;
        for (const buf of dataArrayForDisk) {
            combinedBuffer.set(new Uint8Array(buf), offset);
            offset += buf.byteLength;
        }
        
        this.writeToDisk(file, fileStream, combinedBuffer, onProgressUpdate)

        combinedBuffer = null;
        writeToDiskOffset = 0;
        dataArrayForDisk = []
      }

    }
  
    await this.saveFile(file, fileStream, onProgressUpdate)    
  }

  // show message only if write to disk takes 1 sec or more 
  async writeToDisk(file, fileStream, combinedBuffer, onProgressUpdate) {    
    const DELAY_BEFORE_SHOWING_MESSAGE = 1000;
    let didShowSaving = false;
      
    const savingTimeout = setTimeout(() => {
      didShowSaving = true;
      onProgressUpdate(0,file.name, "write to disk", 0)
    }, DELAY_BEFORE_SHOWING_MESSAGE);
  
    try {  
      await fileStream.write(combinedBuffer);
    } finally {      
      clearTimeout(savingTimeout);
  
      if (didShowSaving) {
        onProgressUpdate(0, file.name, "", 0);
      }
    }
  }

  // show message only if it takes 1 sec or more to save file
  async saveFile(file, fileStream, onProgressUpdate) {    
    const DELAY_BEFORE_SHOWING_MESSAGE = 1000;
    let didShowSaving = false;
      
    const savingTimeout = setTimeout(() => {
      didShowSaving = true;
      onProgressUpdate(0, file.name, "Saving file", 0);
    }, DELAY_BEFORE_SHOWING_MESSAGE);
  
    try {  
      const startTime = Date.now()
      await fileStream.close();  
      const endTime = Date.now();
      const savingTime = endTime - startTime; 
      onProgressUpdate(0, file.name, "", 0, savingTime);
    } finally {      
      clearTimeout(savingTimeout);
  
      if (didShowSaving) {
        onProgressUpdate(0, file.name, "", 0);
      }
    }
  }



  formatSize(bytes) {
    // Convert bytes to KB, MB, GB, etc.
    const units = ['B', 'KB', 'MB', 'GB'];
    let unitIndex = 0;
    while (bytes >= 1024 && unitIndex < units.length - 1) {
      bytes /= 1024;
      unitIndex++;
    }
    return `${bytes.toFixed(2)} ${units[unitIndex]}`;
  }

  formatSpeed(speed) {
    if (isNaN(speed)) {
      return speed;
    }

    return this.formatSize(speed) + '/s'; 
  }

  formatTime(seconds) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    return minutes > 0
      ? `${minutes} min ${remainingSeconds} sec`
      : `${remainingSeconds} sec`;
  }

  async _download(file, offset) {
    const limit = Math.min(this.CHUNK_SIZE, file.size - offset)
    return await openbis.read(this.owner, file.path, offset, limit)
  }

  _removeLeadingSlash(path) {
    return path && path[0] === '/' ? path.substring(1) : path
  }

  setPath(path) {
    this.path = path
  }

  async getRights(ids) {
    return await openbis.getRights(ids, new openbis.RightsFetchOptions())
  }
}