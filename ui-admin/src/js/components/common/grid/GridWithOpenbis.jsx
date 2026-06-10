import React from 'react'
import autoBind from 'auto-bind'
import Grid from '@src/js/components/common/grid/Grid.jsx'
import AppController from '@src/js/components/AppController.js'
import openbis from '@src/js/services/openbis.js'
import ids from '@src/js/common/consts/ids.js'
import logger from '@src/js/common/logger.js'
import ComponentContext from '@src/js/components/common/ComponentContext.js'

export default class GridWithOpenbis extends React.PureComponent {
  constructor(props) {
    super(props)
    autoBind(this)

    if (props.id === undefined || props.id === null) {
      throw new Error('Grid id cannot be null or undefined!')
    }

    if (props.settingsId === undefined) {
      throw new Error('Grid settingsId cannot be undefined!')
    }
  }

  render() {
    logger.log(logger.DEBUG, 'GridWithOpenbis.render')

    return (
      <Grid
        {...this.props}
        loadSettings={this.loadSettings}
        onSettingsChange={this.onSettingsChange}
        onError={this.onError}
        exportXLS={this.exportXLS}
      />
    )
  }

  getSettingsId() {
    return this.props.settingsId
  }

  async loadSettings() {
    const settingsId = this.getSettingsId()

    if (!settingsId) {
      return null
    }

    return await AppController.getInstance().getSetting(settingsId)
  }

  async onSettingsChange(settings) {
    const settingsId = this.getSettingsId()

    if (!settingsId) {
      return
    }

    await AppController.getInstance().setSetting(settingsId, settings)
  }

  async onError(error) {
    await AppController.getInstance().errorChange(error)
  }

  async exportXLS({
                    exportedIds,
                    exportedFields,
                    exportedValues,
                    exportedReferredMasterData,
                    exportedImportCompatible
                  }) {

    const ids = exportedIds.map(id => new openbis.ExportablePermId(id.exportable_kind, id.perm_id));
    let fields = null;
    if(Object.keys(exportedFields).length === 0) {
      fields = new openbis.AllFields();
    } else {
      fields = new openbis.SelectedFields();
      var type = Object.keys(exportedFields.TYPE)[0];
      fields.setAttributes(exportedFields.TYPE[type].map(x => x.id));
      fields.setProperties([]);
    }

    const exportData = new openbis.ExportData(ids, fields)
    const exportOptions = new openbis.ExportOptions(
        [ openbis.ExportFormat.XLSX],
        exportedValues,
        exportedReferredMasterData,
        exportedImportCompatible,
        false
    )

    const sessionToken = AppController.getInstance().getSessionToken()
    const exportResult = await openbis.executeExport(exportData, exportOptions)

    return { sessionToken, exportResult }
  }

}
