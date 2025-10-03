import _ from 'lodash'
import React from 'react'
import autoBind from 'auto-bind'
import GridWithOpenbis from '@src/js/components/common/grid/GridWithOpenbis.jsx'
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'
import logger from '@src/js/common/logger.js'
import TrashcanGridEntitiesCell from '@src/js/components/tools/form/trashcan/TrashcanGridEntitiesCell.jsx'
import TrashcanGridOperationsCell from '@src/js/components/tools/form/trashcan/TrashcanGridOperationsCell.jsx'
import GridUtil from '@src/js/components/common/grid/GridUtil.js'
import ids from '@src/js/common/consts/ids.js'
import messages from '@src/js/common/messages.js'


class TrashcanGrid extends React.PureComponent {
	constructor(props) {
		super(props)
		autoBind(this)
	}

	render() {
		logger.log(logger.DEBUG, 'TrashcanGrid.render')
		const { rows, controllerRef, onOperationSelect, operationLoading } = this.props
		const id = ids.TRASHCAN_GRID_ID
		return (
			<GridWithOpenbis
				id={id}
				settingsId={id}
				header={messages.get(messages.TRASHCAN)}
				loading={operationLoading}
				columns={[
					GridUtil.dateColumn({
						style: {width: '200px'},
						name: 'deletionDate',
						label: 'Deletion Date',
						path: 'deletionDate.value',
					}),
					{
						name: 'entities',
						label: 'Entities', 
						getValue: ({ row }) => row.deletedObjects.value,
						renderValue: ({ value, row }) => {
							return <TrashcanGridEntitiesCell value={value} />
						}
					},
					{
						style: {width: '20%'},
						name: 'reason',
						label: 'Reason', 
						getValue: ({ row }) => row.reason.value
					},
					{
						name: 'operations',
						label: 'Operations',
						style: {width: '250px'},
						getValue: ({ row }) => row.operations,
						renderValue: ({ value, row }) => {
							return <TrashcanGridOperationsCell row={row} 
								operationLoading={operationLoading}
								onOperationSelect={onOperationSelect}
							/>
						},
						filterable: false,
						sortable: false
					}
				]}
				controllerRef={controllerRef}
				rows={rows}
				sort='deletionDate'
				sortDirection='desc'
				exportable={{
					fileFormat: GridExportOptions.FILE_FORMAT.TSV,
					filePrefix: 'trashcan'
				}}
				selectable={true}
			/>
		)
	}
}

export default TrashcanGrid
