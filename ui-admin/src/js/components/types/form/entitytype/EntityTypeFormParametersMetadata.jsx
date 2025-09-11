import React from 'react'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import EntityTypeFormSelectionType from '@src/js/components/types/form/entitytype/EntityTypeFormSelectionType.js'
import MetadataParameters from '@src/js/components/common/form/MetadataParameters.jsx'
import MetadataController from '@src/js/components/common/form/MetadataController.js'
import objectTypes from '@src/js/common/consts/objectType.js'

class EntityTypeFormParametersMetadata extends React.PureComponent {
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
		const entity = this.getEntity(this.props)
		this.controller.focus(entity, this.props.selection, this.references)
	}

	handleFocus(event) {
		const entity = this.getEntity(this.props)
		const selectionType = this.getSelectionType()
		
		if (this.isPropertyMode()) {
			this.props.onSelectionChange(EntityTypeFormSelectionType.PROPERTY, {
				id: entity.id,
				part: event.target.name
			})
		} else {
			this.props.onSelectionChange(EntityTypeFormSelectionType.TYPE, {
				part: event.target.name
			})
		}
	}

	handleBlur() {
		this.props.onBlur()
	}

	handleChange(event) {
		const entity = this.getEntity(this.props)
		const selectionType = this.getSelectionType()
		
		if (this.isPropertyMode()) {
			this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
				id: entity.id,
				field: event.target.name,
				value: event.target.value
			})
		} else {
			this.props.onChange(EntityTypeFormSelectionType.TYPE, {
				field: event.target.name,
				value: event.target.value
			})
		}
	}

	isPropertyMode() {
		return this.props.properties !== undefined;
	}

	getSelectionType() {
		return this.isPropertyMode() ? EntityTypeFormSelectionType.PROPERTY : EntityTypeFormSelectionType.TYPE;
	}

	getMetadataArray(entity) {
		return this.controller.getMetadataArray(entity);
	}

	updateMetadata(entity, metadataArray) {
		const selectionType = this.getSelectionType();
		
		if (this.isPropertyMode()) {
			this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
				id: entity.id,
				field: 'metadata',
				value: metadataArray
			});
		} else {
			this.props.onChange(EntityTypeFormSelectionType.TYPE, {
				field: 'metadata',
				value: metadataArray
			});
		}
	}

	handleAddMetadata = () => {
		const entity = this.getEntity(this.props);
		this.controller.handleAddMetadata(entity, (metadataArray) => {
			this.updateMetadata(entity, metadataArray);
		});
	};

	handleRemoveMetadata = (index) => {
		const entity = this.getEntity(this.props);
		this.controller.handleRemoveMetadata(entity, index, (metadataArray) => {
			this.updateMetadata(entity, metadataArray);
		});
	}

	handleMetadataFieldChange = (index, field, value) => {
		const entity = this.getEntity(this.props);
		this.controller.handleMetadataFieldChange(entity, index, field, value, (metadataArray) => {
			this.updateMetadata(entity, metadataArray);
		});
	}

	getHeaderTitle(entity) {
		if (this.isPropertyMode()) {
			return 'Property Metadata'
		}
		
		const objectTypeValue = entity.objectType?.value
		const map = {
			[objectTypes.OBJECT_TYPE]: messages.OBJECT_TYPE,
			[objectTypes.COLLECTION_TYPE]: messages.COLLECTION_TYPE,
			[objectTypes.DATA_SET_TYPE]: messages.DATA_SET_TYPE,
			[objectTypes.MATERIAL_TYPE]: messages.MATERIAL_TYPE,
			[objectTypes.NEW_OBJECT_TYPE]: messages.NEW_OBJECT_TYPE,
			[objectTypes.NEW_COLLECTION_TYPE]: messages.NEW_COLLECTION_TYPE,
			[objectTypes.NEW_DATA_SET_TYPE]: messages.NEW_DATA_SET_TYPE,
			[objectTypes.NEW_MATERIAL_TYPE]: messages.NEW_MATERIAL_TYPE
		}

		return `${messages.get(map[objectTypeValue])} Metadata`
	}

	render() {
		logger.log(logger.DEBUG, 'EntityTypeFormParametersMetadata.render');

		const entity = this.getEntity(this.props)
		if (!entity) return null

		const { mode } = this.props
		const metadataArray = this.getMetadataArray(entity)
		const title = this.getHeaderTitle(entity)

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

	getEntity(props) {
		if (this.isPropertyMode()) {
			let { properties, selection } = props

			if (selection && selection.type === EntityTypeFormSelectionType.PROPERTY) {
				let [property] = properties.filter(
					property => property.id === selection.params.id
				)
				return property
			} else {
				return null
			}
		} else {
			let { type, selection } = props

			if (!selection || selection.type === EntityTypeFormSelectionType.TYPE) {
				return type
			} else {
				return null
			}
		}
	}
}

export default EntityTypeFormParametersMetadata
