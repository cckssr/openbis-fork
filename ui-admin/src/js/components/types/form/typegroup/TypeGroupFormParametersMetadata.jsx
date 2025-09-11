import React from 'react'
import Container from '@src/js/components/common/form/Container.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'
import MetadataParameters from '@src/js/components/common/form/MetadataParameters.jsx'
import MetadataController from '@src/js/components/common/form/MetadataController.js'
import { Typography } from '@mui/material';
import objectTypes from '@src/js/common/consts/objectType.js'

class TypeGroupFormParametersMetadata extends React.PureComponent {
	constructor(props) {
		super(props);
		this.references = {};
		this.controller = new MetadataController(this);
	}

	componentDidMount() {
		this.focus()
	}

	componentDidUpdate(prevProps) {
		const prevSelection = prevProps.selection
		const selection = this.props.selection

		if (prevSelection !== selection) {
			this.focus()
		}
	}

	focus() {
		const typeGroup = this.getTypeGroup(this.props)
		this.controller.focus(typeGroup, this.props.selection, this.references)
	}

	handleFocus(event) {
		this.props.onSelectionChange(TypeGroupFormSelectionType.TYPE_GROUP, {
			part: event.target.name
		})
	}

	handleBlur() {
		this.props.onBlur()
	}

	handleChange(event) {
		this.props.onChange(TypeGroupFormSelectionType.TYPE_GROUP, {
			field: event.target.name,
			value: event.target.value
		})
	}

	getMetadataArray(typeGroup) {
		return this.controller.getMetadataArray(typeGroup);
	}

	updateMetadata(metadataArray) {
		this.props.onChange(TypeGroupFormSelectionType.TYPE_GROUP, {
			field: 'metadata',
			value: metadataArray
		});
	}

	handleAddMetadata = () => {
		const typeGroup = this.getTypeGroup(this.props);
		this.controller.handleAddMetadata(typeGroup, (metadataArray) => {
			this.updateMetadata(metadataArray);
		});
	};

	handleRemoveMetadata = (index) => {
		const typeGroup = this.getTypeGroup(this.props);
		this.controller.handleRemoveMetadata(typeGroup, index, (metadataArray) => {
			this.updateMetadata(metadataArray);
		});
	}

	handleMetadataFieldChange = (index, field, value) => {
		const typeGroup = this.getTypeGroup(this.props);
		this.controller.handleMetadataFieldChange(typeGroup, index, field, value, (metadataArray) => {
			this.updateMetadata(metadataArray);
		});
	}

	getHeaderTitle(typeGroup) {
		const objectTypeValue = typeGroup.objectType?.value
		const map = {
			[objectTypes.OBJECT_TYPE_GROUP]: messages.OBJECT_TYPE_GROUP,
			[objectTypes.NEW_OBJECT_TYPE_GROUP]: messages.NEW_OBJECT_TYPE_GROUP,
		}

		return `${messages.get(map[objectTypeValue])} Metadata`
	}

	render() {
		logger.log(logger.DEBUG, 'TypeGroupFormParametersMetadata.render');

		const typeGroup = this.getTypeGroup(this.props)
		if (!typeGroup) return <Container><Typography variant="body2" color="textSecondary">Metadata are not available for Object Type Assignments!</Typography></Container>
		
		const { mode } = this.props
		const metadataArray = this.getMetadataArray(typeGroup)
		const title = this.getHeaderTitle(typeGroup)

		return (
			<MetadataParameters
				title={title}
				metadataArray={metadataArray}
				mode={mode}
				onAddMetadata={this.handleAddMetadata}
				onRemoveMetadata={this.handleRemoveMetadata}
				onMetadataFieldChange={this.handleMetadataFieldChange}
				onFocus={(event) => this.handleFocus(event)}
				onBlur={() => this.handleBlur()}
			/>
		)
	}

	getTypeGroup(props) {
		let { typeGroup, selection } = props

		if (!selection || selection.type === TypeGroupFormSelectionType.TYPE_GROUP) {
			return typeGroup
		} else {
			return null
		}
	}
}

export default TypeGroupFormParametersMetadata
