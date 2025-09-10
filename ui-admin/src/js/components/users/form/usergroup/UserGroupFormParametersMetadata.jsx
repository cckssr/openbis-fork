/**
 * UserFormParametersMetadata - Shared dynamic metadata component
 * 
 * This component replaces the three separate metadata components:
 * - UserFormParametersUserMetadata
 * - UserFormParametersGroupMetadata  
 * - UserFormParametersRoleMetadata
 * 
 * It dynamically handles metadata for different entity types (user, group, role)
 * based on the current selection and provides full CRUD functionality for
 * user and group metadata, while showing a read-only message for roles.
 * 
 * Props:
 * - user: User object
 * - groups: Array of group objects
 * - roles: Array of role objects
 * - selection: Current selection object with type and params
 * - mode: 'edit' or 'view' mode
 * - onChange: Function to handle field changes
 * - onSelectionChange: Function to handle selection changes
 * - onBlur: Function to handle blur events
 */

import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Container from '@src/js/components/common/form/Container.jsx'
import Header from '@src/js/components/common/form/Header.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import TextField from '@src/js/components/common/form/TextField.jsx'
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import Button from '@src/js/components/common/form/Button.jsx';
import { Typography } from '@mui/material';
import UserFormSelectionType from '@src/js/components/users/form/user/UserFormSelectionType.js'
import RoleSelectionType from '@src/js/components/users/form/common/RoleSelectionType.js'

const styles = theme => {
	const baseMetadataItem = {
		border: '2px solid #ebebeb',
		padding: theme.spacing(1),
		display: 'flex',
		flexDirection: 'row',
		alignItems: 'center',
		marginBottom: theme.spacing(1),
	};

	return {
		metadataField: {
			paddingBottom: theme.spacing(1)
		},
		headerContainer: {
			display: 'flex',
			flexDirection: 'row',
			alignItems: 'center',
			justifyContent: 'space-between'
		},
		metadataFieldsWrapper: {
			flexGrow: 1,
			marginRight: theme.spacing(1),	
		},
		removeButton: {
			minWidth: 'auto',
			padding: theme.spacing(0.5),
		},
		metadataItem: baseMetadataItem,
		metadataItemEdit: {
			...baseMetadataItem,
			borderRight: 'unset',
			backgroundColor: theme.palette.grey[50],
		},
	};
};

class UserGroupFormParametersMetadata extends React.PureComponent {
	constructor(props) {
		super(props);
		this.references = {};
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
		if (entity && this.props.selection) {
			const { part } = this.props.selection.params
			if (part) {
				const reference = this.references[part]
				if (reference && reference.current) {
					reference.current.focus()
				}
			}
		}
	}

	handleFocus(event) {
		const { selectionType, entity } = this.getEntityInfo(this.props);
		
		if (selectionType === UserFormSelectionType.USER) {
			this.props.onSelectionChange(UserFormSelectionType.USER, {
				id: entity.id,
				part: event.target.name
			});
		} else if (selectionType === UserFormSelectionType.GROUP) {
			this.props.onSelectionChange(UserFormSelectionType.GROUP, {
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
				id: entity.id,
				field: event.target.name,
				value: event.target.value
			});
		} else if (selectionType === UserFormSelectionType.GROUP) {
			this.props.onChange(UserFormSelectionType.GROUP, {
				field: event.target.name,
				value: event.target.value
			});
		}
	}

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

	updateMetadata(metadataArray) {
		const { selectionType, entity } = this.getEntityInfo(this.props);
		console.log('updateMetadata', { selectionType, entity, metadataArray })
		if (selectionType === UserFormSelectionType.USER) {
			this.props.onChange(UserFormSelectionType.USER, {
				id: entity.id,
				field: 'metadata',
				value: metadataArray
			});
		} else if (selectionType === UserFormSelectionType.GROUP) {
			this.props.onChange(UserFormSelectionType.GROUP, {
				field: 'metadata',
				value: metadataArray
			});
		}
	}

	handleAddMetadata = () => {
		const entity = this.getEntity(this.props);
		const metadataArray = this.getMetadataArray(entity);

		metadataArray.push({ key: '', value: '', action: 'CREATE' });

		this.updateMetadata(metadataArray);
	};

	handleRemoveMetadata = (index) => {
		const entity = this.getEntity(this.props);
		const metadataArray = this.getMetadataArray(entity);

		metadataArray.splice(index, 1);

		this.updateMetadata(metadataArray);
	}

	handleMetadataFieldChange = (index, field, value) => {
		const entity = this.getEntity(this.props);
		const metadataArray = this.getMetadataArray(entity);

		if (metadataArray[index]) {
			metadataArray[index][field] = value;
			if (metadataArray[index].action !== 'CREATE') {
				metadataArray[index].action = 'UPDATE';
			}
		}

		this.updateMetadata(metadataArray);
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

		return (
			<Container>
				{this.renderHeader('Metadata', entity, selectionType)}
				{this.renderMetadata(entity)}
			</Container>
		)
	}

	renderHeader(title, entity, selectionType) {
		const { mode, classes } = this.props
		const entityType = this.getEntityTypeLabel(selectionType);
		
		return (
			<div className={classes.headerContainer}>
				<Header>{entityType} {title}</Header>
				{mode === 'edit' &&
					<Button variant='contained'
						color='white'
						onClick={() => this.handleAddMetadata(entity)}
						label={<AddIcon />}
						sx={{ marginRight: '8px' }} />}
			</div>
		)
	}

	renderMetadata(entity) {
		const { classes } = this.props;
		const { visible } = { ...entity.metadata }

		if (!visible) {
			return null
		}

		const metadataArray = this.getMetadataArray(entity);

		if (metadataArray.length === 0) {
			return <Typography variant="body2" color="textSecondary">{messages.get(messages.NO_METADATA_DEFINED)}</Typography>
		}

		return (
			metadataArray.map((metadata, index) => this.renderMetadataItem(metadata, index))
		)
	}

	renderMetadataItem(metadata, index) {
		const { key, value, action } = metadata;
		const { mode, classes } = this.props;

		const isKeyEditable = action === 'CREATE';

		return (
			<div key={`metadata-${index}`} className={mode === 'edit' ? classes.metadataItemEdit : classes.metadataItem}>
				<div className={classes.metadataFieldsWrapper}>
					<div className={classes.metadataField}>
						<TextField
							label='Key'
							name={`metadata-key-${index}`}
							value={key || ''}
							mode={mode}
							disabled={!isKeyEditable}
							onChange={(event) => this.handleMetadataFieldChange(index, 'key', event.target.value)}
							onFocus={(event) => this.handleFocus(event)}
							onBlur={(event) => this.handleBlur()}
							sx={classes.textField}
						/>
					</div>
					<div className={classes.metadataField}>
						<TextField
							label='Value'
							name={`metadata-value-${index}`}
							value={value || ''}
							mode={mode}
							onChange={(event) => this.handleMetadataFieldChange(index, 'value', event.target.value)}
							onFocus={(event) => this.handleFocus(event)}
							onBlur={(event) => this.handleBlur()}
							sx={classes.textField}
						/>
					</div>
				</div>
				{mode === 'edit' && (
					<Button
						variant='contained'
						color='white'
						onClick={() => this.handleRemoveMetadata(index)}
						label={<RemoveIcon />}
						aria-label={`Remove metadata item ${index + 1}`}
						tooltip={messages.get(messages.REMOVE)}
						sx={classes.removeButton}
					/>
				)}
			</div>
		);
	}

	getEntity(props) {
		const { users, group, roles, selection } = props;
		if (!selection) return group;

		switch (selection.type) {
			case UserFormSelectionType.USER:
				if (selection.params && selection.params.id) {
					return users.find(user => user.id === selection.params.id) || null;
				}
				return null;
			case UserFormSelectionType.GROUP:
				return group;
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
		const { users, group, roles, selection } = props;

		if (!selection) return { selectionType: UserFormSelectionType.GROUP, entity: group };

		switch (selection.type) {
			case UserFormSelectionType.USER:
				const user = selection.params && selection.params.id 
					? users.find(user => user.id === selection.params.id) 
					: null;
				return { selectionType: UserFormSelectionType.USER, entity: user };
			case UserFormSelectionType.GROUP:
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

export default withStyles(styles)(UserGroupFormParametersMetadata)
