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
	render() {
		const { row, operationLoading, onOperationSelect } = this.props

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
				onChange={(event) => onOperationSelect && onOperationSelect(event, row)}
			/>
		)
	}

}

export default TrashcanGridOperationsCell
