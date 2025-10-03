import _ from 'lodash'
import React from 'react'
import autoBind from 'auto-bind'
import GridWithOpenbis from '@src/js/components/common/grid/GridWithOpenbis.jsx'
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'
import GridFilterOptions from '@src/js/components/common/grid/GridFilterOptions.js'
import logger from '@src/js/common/logger.js'
import TrashcanGridEntitiesCell from '@src/js/components/tools/form/trashcan/TrashcanGridEntitiesCell.jsx'
import TrashcanGridOperationsCell from '@src/js/components/tools/form/trashcan/TrashcanGridOperationsCell.jsx'
import GridUtil from '@src/js/components/common/grid/GridUtil.js'
import SelectField from '@src/js/components/common/form/SelectField.jsx'
import ids from '@src/js/common/consts/ids.js'
import objectTypes from '@src/js/common/consts/objectType.js'
import openbis from '@src/js/services/openbis.js'
import messages from '@src/js/common/messages.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'
import AppController from '@src/js/components/AppController.js'

class TrashcanGrid extends React.PureComponent {
	constructor(props) {
		super(props)
		autoBind(this)
		this.state = {
			operationLoading: false
		}
	}

	changeOperationLoading(loading) {
		this.setState({ operationLoading: loading })
	}

	render() {
		logger.log(logger.DEBUG, 'TrashcanGrid.render')
		const { rows, controllerRef } = this.props
		const { operationLoading } = this.state
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
								facade={this.props.facade}
								onReload={this.props.onReload}
								operationLoading={operationLoading}
								changeOperationLoading={this.changeOperationLoading}
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
