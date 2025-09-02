import React from 'react'
import GridWithOpenbis from '@src/js/components/common/grid/GridWithOpenbis.jsx'
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'
import GridUtil from '@src/js/components/common/grid/GridUtil.js'
import TypeGroupLink from '@src/js/components/common/link/TypeGroupLink.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import LockLabel from '@src/js/components/common/form/LockLabel.jsx'

class TypeGroupsGrid extends React.PureComponent {
  render() {
    logger.log(logger.DEBUG, 'TypeGroupsGrid.render')

    const { id, rows, selectedRowId, onSelectedRowChange, controllerRef } =
      this.props

    return (
      <GridWithOpenbis
        id={id}
        settingsId={id}
        controllerRef={controllerRef}
        header={messages.get(messages.OBJECT_TYPE_GROUPS)}
        columns={[
          {
            name: 'code',
            label: messages.get(messages.CODE),
            exportableField: GridExportOptions.EXPORTABLE_FIELD.CODE,
            getValue: ({ row }) => row.code,
            renderValue: ({ row }) => {
              return <TypeGroupLink typeGroupCode={row.code} />
            }
          },
          {
            name: 'Internal',
            label: messages.get(messages.INTERNAL),
            getValue: ({ row }) => {
              return row.internal;
            },
            renderValue: ({ value }) => {
              if (value) {
                return <LockLabel fontSize='small' color='disabled' />
              }
              return null;
            }
          },
          GridUtil.registratorColumn({ path: 'registrator' }),
          GridUtil.registrationDateColumn({ path: 'registrationDate' }),
          GridUtil.modifierColumn({ path: 'modifier' }),
          GridUtil.modificationDateColumn({ path: 'modificationDate' })
        ]}
        rows={rows}
        sort='code'
        exportable={{
          fileFormat: GridExportOptions.FILE_FORMAT.XLS,
          filePrefix: 'type-groups',
          fileContent: GridExportOptions.FILE_CONTENT.TYPE_GROUPS
        }}
        selectable={true}
        selectedRowId={selectedRowId}
        onSelectedRowChange={onSelectedRowChange}
      />
    )
  }
}

export default TypeGroupsGrid
