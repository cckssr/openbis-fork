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
import MetadataGrid from '@src/js/components/types/common/MetadataGrid.jsx';
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'

const styles = theme => {
	const baseSemanticAnnotationTripletContainer = {
		border: '2px solid #ebebeb', // Consider theme.palette.divider
		padding: theme.spacing(1),
		display: 'flex',
		flexDirection: 'row',
		alignItems: 'center',
		marginBottom: theme.spacing(1),
	};

	return {
		field: {
			paddingBottom: theme.spacing(1)
		},
		headerContainer: {
			display: 'flex',
			flexDirection: 'row',
			alignItems: 'center',
			justifyContent: 'space-between'
		},
		semanticAnnotationFieldsWrapper: {
			flexGrow: 1,
			marginRight: theme.spacing(1),
		},
		removeButton: {
			minWidth: 'auto', // Ensure button doesn't take too much space
			padding: theme.spacing(0.5),
		},
		semanticAnnotationTripletContainer: baseSemanticAnnotationTripletContainer,
		semanticAnnotationTripletContainerEdit: {
			...baseSemanticAnnotationTripletContainer,
			borderRight: 'unset',
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
		const type = this.getType(this.props)
		if (type && this.props.selection) {
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
		this.props.onSelectionChange(EntityTypeFormSelectionType.TYPE, {
			part: event.target.name
		})
	}

	handleBlur() {
		this.props.onBlur()
	}

	handleChange(event) {
		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: event.target.name,
			value: event.target.value
		})
	}

	handleAddMetadata = () => {
		const type = this.getType(this.props);
		const { metadata } = type;
		
		// Convert object to array of key-value pairs if needed
		let metadataArray = [];
		if (metadata && metadata.value) {
			if (Array.isArray(metadata.value)) {
				metadataArray = [...metadata.value];
			} else if (typeof metadata.value === 'object') {
				metadataArray = Object.entries(metadata.value).map(([key, value]) => ({ key, value }));
			}
		}
		
		// Add new empty key-value pair
		metadataArray.push({ key: '', value: '' });
		
		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'metadata',
			value: metadataArray
		});
	};

	handleRemoveMetadata = (index) => {
		const type = this.getType(this.props);
		const { metadata } = type;
		
		let metadataArray = [];
		if (metadata && metadata.value) {
			if (Array.isArray(metadata.value)) {
				metadataArray = [...metadata.value];
			} else if (typeof metadata.value === 'object') {
				metadataArray = Object.entries(metadata.value).map(([key, value]) => ({ key, value }));
			}
		}
		
		// Remove item at index
		metadataArray.splice(index, 1);
		
		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'metadata',
			value: metadataArray
		});
	}

	handleMetadataFieldChange = (index, field, value) => {
		const type = this.getType(this.props);
		const { metadata } = type;
		
		let metadataArray = [];
		if (metadata && metadata.value) {
			if (Array.isArray(metadata.value)) {
				metadataArray = [...metadata.value];
			} else if (typeof metadata.value === 'object') {
				metadataArray = Object.entries(metadata.value).map(([key, value]) => ({ key, value }));
			}
		}
		
		// Update the specific field
		if (metadataArray[index]) {
			metadataArray[index][field] = value;
		}
		
		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'metadata',
			value: metadataArray
		});
	}

	render() {
		logger.log(logger.DEBUG, 'EntityTypeFormParametersMetadata.render');

		const type = this.getType(this.props)
		if (!type) return null
		console.log('EntityTypeFormParametersMetadata.render', type)

		return (
			<Container>
				{this.renderHeader('Metadata', type)}
				{this.renderMetadata(type)}
			</Container>
		)
	}

	renderHeader(title, type) {
		const { mode, classes } = this.props
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
				<Header>{messages.get(map[type.objectType.value])} {type.code?.value} {title}</Header>
				{mode === 'edit' && type.objectType?.value === objectTypes.OBJECT_TYPE &&
					<Button variant='contained'
						color='white'
						onClick={this.handleAddMetadata}
						label={<AddIcon />}
						sx={{ marginRight: '8px' }} />}
			</div>
		)
	}

	renderMetadata(type) {
		const { visible, value } = { ...type.metadata }

		if (!visible) {
			return null
		}

		// Convert metadata to array format
		let metadataArray = [];
		if (value) {
			if (Array.isArray(value)) {
				metadataArray = value;
			} else if (typeof value === 'object') {
				metadataArray = Object.entries(value).map(([key, val]) => ({ key, value: val }));
			}
		}

		if (metadataArray.length === 0) {
			return <Typography variant="body2" color="textSecondary">{messages.get(messages.NO_METADATA_DEFINED)}</Typography>
		}

		return (
			<div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
				{metadataArray.map((metadata, index) => this.renderMetadataItem(metadata, index))}
			</div>
		)
	}

	renderMetadataItem(metadata, index) {
		const { key, value } = metadata;
		const { mode, classes } = this.props;
		
		return (
			<div key={`metadata-${index}`} style={{ 
				display: 'flex', 
				alignItems: 'center', 
				gap: '8px', 
				width: '100%',
				padding: '8px',
				border: '1px solid #e0e0e0',
				borderRadius: '4px',
				backgroundColor: '#fafafa'
			}}>
				<div style={{ flex: 1 }}>
					<TextField
						label='Key'
						name={`metadata-key-${index}`}
						value={key || ''}
						mode={mode}
						onChange={(event) => this.handleMetadataFieldChange(index, 'key', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
						sx={{ '& .MuiOutlinedInput-root': { height: '40px' } }}
					/>
				</div>
				<div style={{ flex: 1 }}>
					<TextField
						label='Value'
						name={`metadata-value-${index}`}
						value={value || ''}
						mode={mode}
						onChange={(event) => this.handleMetadataFieldChange(index, 'value', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
						sx={{ '& .MuiOutlinedInput-root': { height: '40px' } }}
					/>
				</div>
				{mode === 'edit' && (
					<Button
						variant='contained'
						color='white'
						onClick={() => this.handleRemoveMetadata(index)}
						label={<RemoveIcon />}
						aria-label={`Remove metadata item ${index + 1}`}
						tooltip={messages.get(messages.REMOVE)}
						sx={{ minWidth: '40px', height: '40px' }}
					/>
				)}
			</div>
		);
	}

	getType(props) {
		let { type, selection } = props

		if (!selection || selection.type === EntityTypeFormSelectionType.TYPE) {
			return type
		} else {
			return null
		}
	}
}

export default withStyles(styles)(EntityTypeFormParametersMetadata)
