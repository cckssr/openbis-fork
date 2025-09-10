import React from 'react'
import Container from '@src/js/components/common/form/Container.jsx'
import logger from '@src/js/common/logger.js'
import MetadataParameters from '@src/js/components/common/form/MetadataParameters.jsx'
import MetadataController from '@src/js/components/common/form/MetadataController.js'
import { Typography } from '@mui/material';
import UserFormSelectionType from '@src/js/components/users/form/user/UserFormSelectionType.js'

class UserFormParametersMetadata extends React.PureComponent {
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
		const { selectionType, entity } = this.getEntityInfo(this.props);
		
		if (selectionType === UserFormSelectionType.USER) {
			this.props.onSelectionChange(UserFormSelectionType.USER, {
				part: event.target.name
			});
		} else if (selectionType === UserFormSelectionType.GROUP) {
			this.props.onSelectionChange(UserFormSelectionType.GROUP, {
				id: entity.id,
				part: event.target.name
			});
		}
	}

	handleBlur() {
		this.props.onBlur()
	}

	handleChange(event) {
		const { selectionType, entity } = this.getEntityInfo(this.props);
		
		if (selectionType === UserFormSelectionType.USER) {
			this.props.onChange(UserFormSelectionType.USER, {
				field: event.target.name,
				value: event.target.value
			});
		} else if (selectionType === UserFormSelectionType.GROUP) {
			this.props.onChange(UserFormSelectionType.GROUP, {
				id: entity.id,
				field: event.target.name,
				value: event.target.value
			});
		}
	}

	getMetadataArray(entity) {
		return this.controller.getMetadataArray(entity);
	}

	updateMetadata(metadataArray) {
		const { selectionType, entity } = this.getEntityInfo(this.props);
		
		if (selectionType === UserFormSelectionType.USER) {
			this.props.onChange(UserFormSelectionType.USER, {
				field: 'metadata',
				value: metadataArray
			});
		} else if (selectionType === UserFormSelectionType.GROUP) {
			this.props.onChange(UserFormSelectionType.GROUP, {
				id: entity.id,
				field: 'metadata',
				value: metadataArray
			});
		}
	}

	handleAddMetadata = () => {
		const entity = this.getEntity(this.props);
		this.controller.handleAddMetadata(entity, (metadataArray) => {
			this.updateMetadata(metadataArray);
		});
	};

	handleRemoveMetadata = (index) => {
		const entity = this.getEntity(this.props);
		this.controller.handleRemoveMetadata(entity, index, (metadataArray) => {
			this.updateMetadata(metadataArray);
		});
	}

	handleMetadataFieldChange = (index, field, value) => {
		const entity = this.getEntity(this.props);
		this.controller.handleMetadataFieldChange(entity, index, field, value, (metadataArray) => {
			this.updateMetadata(metadataArray);
		});
	}

	getHeaderTitle(selectionType) {
		const entityType = this.getEntityTypeLabel(selectionType);
		return `${entityType} Metadata`;
	}

	render() {
		logger.log(logger.DEBUG, 'UserFormParametersMetadata.render');

		const { selectionType, entity } = this.getEntityInfo(this.props);
		if (!entity) return null;

		if (selectionType === UserFormSelectionType.ROLE) {
			return (
				<Container>
					<Typography variant="body2" color="textSecondary">Metadata are not available for Roles!</Typography>
				</Container>
			);
		}

		const { mode } = this.props
		const metadataArray = this.getMetadataArray(entity)
		const title = this.getHeaderTitle(selectionType)

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
		const { user, groups, roles, selection } = props;
		
		if (!selection) return user;

		switch (selection.type) {
			case UserFormSelectionType.USER:
				return user;
			case UserFormSelectionType.GROUP:
				if (selection.params && selection.params.id) {
					return groups.find(group => group.id === selection.params.id) || null;
				}
				return null;
			case UserFormSelectionType.ROLE:
				if (selection.params && selection.params.id) {
					return roles.find(role => role.id === selection.params.id) || null;
				}
				return null;
			default:
				return null;
		}
	}

	getEntityInfo(props) {
		const { user, groups, roles, selection } = props;
		
		if (!selection) return { selectionType: UserFormSelectionType.USER, entity: user };

		switch (selection.type) {
			case UserFormSelectionType.USER:
				return { selectionType: UserFormSelectionType.USER, entity: user };
			case UserFormSelectionType.GROUP:
				const group = selection.params && selection.params.id 
					? groups.find(group => group.id === selection.params.id) 
					: null;
				return { selectionType: UserFormSelectionType.GROUP, entity: group };
			case UserFormSelectionType.ROLE:
				const role = selection.params && selection.params.id 
					? roles.find(role => role.id === selection.params.id) 
					: null;
				return { selectionType: UserFormSelectionType.ROLE, entity: role };
			default:
				return { selectionType: null, entity: null };
		}
	}

	getEntityTypeLabel(selectionType) {
		switch (selectionType) {
			case UserFormSelectionType.USER:
				return 'User';
			case UserFormSelectionType.GROUP:
				return 'Group';
			case UserFormSelectionType.ROLE:
				return 'Role';
			default:
				return '';
		}
	}
}

export default UserFormParametersMetadata
