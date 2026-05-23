import openbis from '@src/js/services/openbis.js'

export default class ImportAllFormFacade {
  async import(file, updateMode) {
    return new Promise((resolve, reject) => {
      openbis.uploadToSessionWorkspace(file)
        .then(() => {
            const importData = new openbis.ImportData();
            importData.setFormat(openbis.ImportFormat.EXCEL);
            importData.setSessionWorkspaceFiles([file.name]);

            let mode = openbis.ImportMode.FAIL_IF_EXISTS;
            if(updateMode == "IGNORE_EXISTING") {
                mode = openbis.ImportMode.IGNORE_EXISTING;
            } else if(updateMode == "FAIL_IF_EXISTS") {
                mode = openbis.ImportMode.FAIL_IF_EXISTS;
            } else if(updateMode == "UPDATE_IF_EXISTS") {
                mode = openbis.ImportMode.UPDATE_IF_EXISTS;
            } else {
                throw new Error("Update mode has to be one of following: IGNORE_EXISTING FAIL_IF_EXISTS UPDATE_IF_EXISTS but was '" + updateMode + "'");
            }
            const importOptions = new openbis.ImportOptions();
            importOptions.setMode(mode);

            return openbis.executeImport(importData, importOptions)
                .then(result => resolve(result))
                .catch(error => reject(error))
        }, (error) => reject(error));
    });
  }
}
