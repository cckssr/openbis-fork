import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Container from '@src/js/components/common/form/Container.jsx'
import Header from '@src/js/components/common/form/Header.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import TextField from '@src/js/components/common/form/TextField.jsx'
import EntityTypeFormSelectionType from '@src/js/components/types/form/entitytype/EntityTypeFormSelectionType.js'
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import Button from '@src/js/components/common/form/Button.jsx';
import { Typography } from '@mui/material';
import objectTypes from '@src/js/common/consts/objectType.js'

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

class EntityTypeFormParametersMetadata extends React.PureComponent {
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
		const metadataArray = this.getMetadataArray(entity);

		metadataArray.push({ key: '', value: '', action: 'CREATE' });

		this.updateMetadata(entity, metadataArray);
	};

	handleRemoveMetadata = (index) => {
		const entity = this.getEntity(this.props);
		const metadataArray = this.getMetadataArray(entity);

		metadataArray.splice(index, 1);

		this.updateMetadata(entity, metadataArray);
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

		this.updateMetadata(entity, metadataArray);
	}

	render() {
		logger.log(logger.DEBUG, 'EntityTypeFormParametersMetadata.render');

		const entity = this.getEntity(this.props)
		if (!entity) return null

		return (
			<Container>
				{this.renderHeader(entity)}
				{this.renderMetadata(entity)}
			</Container>
		)
	}

	renderHeader(entity) {
		const { mode, classes } = this.props
		
		if (this.isPropertyMode()) {
			return (
				<div className={classes.headerContainer}>
					<Header>Property Metadata</Header>
					{mode === 'edit' &&
						<Button variant='contained'
							color='white'
							onClick={this.handleAddMetadata}
							label={<AddIcon />}
							sx={{ marginRight: '8px' }} />}
				</div>
			)
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

		return (
			<div className={classes.headerContainer}>
				<Header>{messages.get(map[objectTypeValue])} Metadata</Header>
				{mode === 'edit' && (objectTypeValue in map) &&
					<Button variant='contained'
						color='white'
						onClick={this.handleAddMetadata}
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
			metadataArray.map((metadata, index) => this.renderMetadataItem(entity, metadata, index))
		)
	}

	renderMetadataItem(entity, metadata, index) {
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
						sx={{
							minWidth: 'auto',
							padding: '4px',
						}}
					/>
				)}
			</div>
		);
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

export default withStyles(styles)(EntityTypeFormParametersMetadata)
