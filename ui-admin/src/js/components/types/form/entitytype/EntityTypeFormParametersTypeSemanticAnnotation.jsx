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

class EntityTypeFormParametersTypeSemanticAnnotation extends React.PureComponent {
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

	handleAddSemanticAnnotation = (type) => {
		const { semanticAnnotations } = type;

		const newSemanticAnnotation = {
			tempPermId: { permId: `semantic-ann-${type.code.value}-${semanticAnnotations?.value?.length > 0 ? semanticAnnotations.value.length : 0}` },
			predicateOntologyId: '',
			predicateOntologyVersion: '',
			predicateAccessionId: ''
		};

		if (semanticAnnotations !== undefined && semanticAnnotations !== null && semanticAnnotations.value?.length > 0) {
			semanticAnnotations.value.push(newSemanticAnnotation);
		} else {
			semanticAnnotations.value = [newSemanticAnnotation];
		}

		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'semanticAnnotations',
			value: semanticAnnotations.value
		});
	};

	handleRemoveSemanticAnnotation = (semanticAnnotationTripletId, index) => {
		const type = this.getType(this.props);
		let { semanticAnnotations } = type;
		//it's already the list of semantic annotations
		semanticAnnotations = semanticAnnotations.value.filter((semanticAnnotationTriplet, idx) => idx !== index);
		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'semanticAnnotations',
			value: semanticAnnotations
		});
	};

	handleSemanticAnnotationFieldChange(index, field, fieldValue) {
		const type = this.getType(this.props);
		let { semanticAnnotations } = type;
		semanticAnnotations.value[index][field] = fieldValue;
		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'semanticAnnotations',
			value: semanticAnnotations.value
		});
	}

	renderSemanticAnnotationFields(semanticAnnotationTriplet, index, selectionType) {
		const { id, predicateOntologyId, predicateOntologyVersion, predicateAccessionId, error } = semanticAnnotationTriplet;
		const { mode, classes } = this.props;
		return (
			<>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ID)}
						name={selectionType + '-predicateOntologyId-' + index}
						mandatory={true}
						error={error?.predicateOntologyId}
						value={predicateOntologyId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(index, 'predicateOntologyId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_VERSION)}
						name={selectionType + '-predicateOntologyVersion-' + index}
						mandatory={true}
						error={error?.predicateOntologyVersion}
						value={predicateOntologyVersion}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(index, 'predicateOntologyVersion', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ANNOTATION_ID)}
						name={selectionType + '-predicateAccessionId-' + index}
						mandatory={true}
						error={error?.predicateAccessionId}
						value={predicateAccessionId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(index, 'predicateAccessionId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
			</>
		);
	}

	renderSemanticAnnotationTriplet(semanticAnnotationTriplet, index, selectionType) {
		const { classes, mode } = this.props;
		return (
			<div key={`${selectionType}-annotation-${index}`} className={mode === 'edit' ? classes.semanticAnnotationTripletContainerEdit : classes.semanticAnnotationTripletContainer}>
				<div className={classes.semanticAnnotationFieldsWrapper}>
					{this.renderSemanticAnnotationFields(semanticAnnotationTriplet, index, selectionType)}
				</div>
				{mode === 'edit' && (
					<Button
						variant='contained'
						color='white'
						className={classes.removeButton}
						onClick={() => this.handleRemoveSemanticAnnotation(semanticAnnotationTriplet.id, index)}
						label={<RemoveIcon />}
						aria-label={`${messages.get(messages.REMOVE)} ${selectionType} annotation ${index + 1}`}
						tooltip={messages.get(messages.REMOVE)}
					/>
				)}
			</div>
		);
	}

	renderSemanticAnnotations(type) {
		const { visible, value } = { ...type.semanticAnnotations }

		if (!visible) {
			return null
		}
		console.log('render semanticAnnotations: ', type.semanticAnnotations);

		if (value === undefined || value === null || value.length === 0) {
			return <Typography variant="body2" color="textSecondary">{messages.get(messages.NO_ANNOTATIONS_DEFINED)}</Typography>
		}

		return value.map((semanticAnnotationTriplet, index) => {
			return this.renderSemanticAnnotationTriplet(semanticAnnotationTriplet, index, EntityTypeFormSelectionType.TYPE)
		})
	}

	render() {
		logger.log(logger.DEBUG, 'EntityTypeFormParametersTypeSemanticAnnotation.render');

		const type = this.getType(this.props)
		if (!type) {
			return null
		}

		console.log('render EntityTypeFormParametersTypeSemanticAnnotation type: ', type);

		return (
			<Container>
				{this.renderHeader(messages.get(messages.SEMANTIC_ANNOTATIONS), type)}

				{this.renderSemanticAnnotations(type)}

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
				<Header>{messages.get(map[type.objectType.value])} {title}</Header>
				{mode === 'edit' &&
					<Button variant='contained'
						color='white'
						onClick={() => this.handleAddSemanticAnnotation(type)}
						label={<AddIcon />}
						sx={{ marginRight: '8px' }} />}
			</div>
		)
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

export default withStyles(styles)(EntityTypeFormParametersTypeSemanticAnnotation)
