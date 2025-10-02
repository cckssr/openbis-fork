import _ from 'lodash'
import React from 'react'
import autoBind from 'auto-bind'
import GridWithOpenbis from '@src/js/components/common/grid/GridWithOpenbis.jsx'
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'
import GridFilterOptions from '@src/js/components/common/grid/GridFilterOptions.js'
import logger from '@src/js/common/logger.js'
import TrashcanGridEntitiesCell from '@src/js/components/tools/form/trashcan/TrashcanGridEntitiesCell.jsx'
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

	render() {
		logger.log(logger.DEBUG, 'TrashcanGrid.render')
		console.log('TrashcanGrid.props', this.props)
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
							return this.renderOperations(row)
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

	renderOperations(row) {
		console.log('renderOperations.row', row)
		// Create options for the SelectField based on available operations
		const operationOptions = [
			{
				label: 'Revert Deletions',
				value: 'revert'
			},
			{
				label: 'Delete Permanently',
				value: 'delete'
			},
			{
				label: 'Delete Permanently (including dependent entries)',
				value: 'deleteWithDependents'
			}
		]

		return (
			<SelectField
				label="Operations"
				value=""
				emptyOption={{
					label: 'Select operation...',
					value: ''
				}}
				options={operationOptions}
				onChange={(event) => this.handleOperationChange(event, row)}
			/>
		)
	}

	handleOperationChange = async (event, row) => {
		const selectedOperation = event.target.value

		if (!selectedOperation || !row) {
			return
		}

		// Set loading state and record start time
		const startTime = Date.now()
		this.setState({ operationLoading: true })

		try {
			// Route to the appropriate handler based on the selected operation
			switch (selectedOperation) {
				case 'revert':
					await this.handleRevertDeletions(row)
					break
				case 'delete':
					await this.handleDeletePermanently(row, false)
					break
				case 'deleteWithDependents':
					await this.handleDeletePermanently(row, true)
					break
				default:
					console.log('Unknown operation:', selectedOperation)
			}
		} catch (error) {
			console.error('Operation failed:', error)
		} finally {
			this.setState({ operationLoading: false })
			this.reloadTabContent()
		}
	}

	handleRevertDeletions = async (row) => {
		if (row) {
			// Call the controller method to revert deletions
			console.log('Revert deletions for row:', row)
			await this.props.facade.revertDeletions(row)
			AppController.getInstance().objectUpdate(objectTypes.TRASHCAN)
			this.props.onDeleteChange()
		}
	}

	handleDeletePermanently = async (row, includeDependent = false) => {
		if (row) {
			// Call the controller method to delete permanently
			console.log('Delete permanently for row:', row, 'includeDependent:', includeDependent)
			return await this.props.facade.deletePermanently(row, includeDependent)
		}
	}

	reloadTabContent = () => {
		// Get the controller from the grid controller ref
		const gridController = this.props.controllerRef
		if (gridController && gridController.controller) {
			// Call the controller's load method to reload the tab content
			gridController.controller.load()

		}
	}
}

export default TrashcanGrid
