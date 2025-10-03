import React from 'react'
import SelectField from '@src/js/components/common/form/SelectField.jsx'

const OPERATIONS_OPTIONS = [
	{
		label: 'Revert Deletions',
		value: 'revert'
	},
	{
		label: 'Delete Permanently',
		value: 'delete'
	},
	{
		label: 'Delete Permanently (including dependent entries in trashcan)',
		value: 'deleteWithDependents'
	}
]

class TrashcanGridOperationsCell extends React.PureComponent {
	constructor(props) {
		super(props)
		
	}

	render() {
		const { row, operationLoading } = this.props

		return (
			<SelectField
				label="Operations"
				value=""
				disabled={operationLoading}
				emptyOption={{
					label: operationLoading ? 'Processing...' : 'Select operation...',
					value: ''
				}}
				sort={false}
				options={OPERATIONS_OPTIONS}
				onChange={(event) => this.handleOperationChange(event, row)}
			/>
		)
	}

	handleOperationChange = async (event, row) => {
		const selectedOperation = event.target.value

		if (!selectedOperation || !row) {
			return
		}
		this.props.changeOperationLoading(true)

		try {
			// Route to the appropriate handler based on the selected operation
			switch (selectedOperation) {
				case 'revert':
					await this.props.facade.revertDeletions(row)
					break
				case 'delete':
					await this.props.facade.deletePermanently(row, false)
					break
				case 'deleteWithDependents':
					await this.props.facade.deletePermanently(row, true)
					break
				default:
					console.log('Unknown operation:', selectedOperation)
			}
		} catch (error) {
			console.error('Operation failed:', error)
		} finally {
			this.props.changeOperationLoading(false)
			if (this.props.onReload) {
				this.props.onReload()
			}
		}
	}
}

export default TrashcanGridOperationsCell
