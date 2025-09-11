class MetadataController {
	constructor(component) {
		this.component = component;
	}

	/**
	 * Get metadata array from entity
	 * @param {Object} entity - The entity containing metadata
	 * @returns {Array} Array of metadata items with action flags
	 */
	getMetadataArray(entity) {
		const { metadata } = entity;
		if (!metadata || !metadata.value) {
			return [];
		}

		if (Array.isArray(metadata.value)) {
			return metadata.value.map(item => ({
				...item,
				action: item.action || 'UPDATE'
			}));
		} else if (typeof metadata.value === 'object') {
			return Object.entries(metadata.value).map(([key, value]) => ({
				key,
				value,
				action: 'UPDATE'
			}));
		}

		return [];
	}

	/**
	 * Add new metadata item
	 * @param {Object} entity - The entity to add metadata to
	 * @param {Function} updateCallback - Function to call with updated metadata array
	 */
	handleAddMetadata(entity, updateCallback) {
		const metadataArray = this.getMetadataArray(entity);
		metadataArray.push({ key: '', value: '', action: 'CREATE' });
		updateCallback(metadataArray);
	}

	/**
	 * Remove metadata item at index
	 * @param {Object} entity - The entity to remove metadata from
	 * @param {number} index - Index of item to remove
	 * @param {Function} updateCallback - Function to call with updated metadata array
	 */
	handleRemoveMetadata(entity, index, updateCallback) {
		const metadataArray = this.getMetadataArray(entity);
		metadataArray.splice(index, 1);
		updateCallback(metadataArray);
	}

	/**
	 * Handle metadata field change
	 * @param {Object} entity - The entity containing the metadata
	 * @param {number} index - Index of the metadata item
	 * @param {string} field - Field name ('key' or 'value')
	 * @param {string} value - New value
	 * @param {Function} updateCallback - Function to call with updated metadata array
	 */
	handleMetadataFieldChange(entity, index, field, value, updateCallback) {
		const metadataArray = this.getMetadataArray(entity);
		
		if (metadataArray[index]) {
			metadataArray[index][field] = value;
			// Only set action to UPDATE if it's not already CREATE
			if (metadataArray[index].action !== 'CREATE') {
				metadataArray[index].action = 'UPDATE';
			}
		}

		updateCallback(metadataArray);
	}

	/**
	 * Focus management for metadata fields
	 * @param {Object} entity - The entity
	 * @param {Object} selection - Current selection
	 * @param {Object} references - References object for focus management
	 */
	focus(entity, selection, references) {
		if (entity && selection) {
			const { part } = selection.params;
			if (part) {
				const reference = references[part];
				if (reference && reference.current) {
					reference.current.focus();
				}
			}
		}
	}

	/**
	 * Check if metadata is visible
	 * @param {Object} entity - The entity
	 * @returns {boolean} Whether metadata is visible
	 */
	isMetadataVisible(entity) {
		const { metadata } = entity;
		return metadata && metadata.visible;
	}

	/**
	 * Get empty metadata message
	 * @returns {string} Message for when no metadata is defined
	 */
	getEmptyMetadataMessage() {
		return 'No metadata defined';
	}

	/**
	 * Get role not available message
	 * @returns {string} Message for when metadata is not available for roles
	 */
	getRoleNotAvailableMessage() {
		return 'Metadata are not available for Roles!';
	}

	/**
	 * Get object type assignment not available message
	 * @returns {string} Message for when metadata is not available for object type assignments
	 */
	getObjectTypeAssignmentNotAvailableMessage() {
		return 'Metadata are not available for Object Type Assignments!';
	}
}

export default MetadataController;
