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
		border: '2px solid #ebebeb',
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
			minWidth: 'auto',
			padding: theme.spacing(0.5),
		},
		semanticAnnotationTripletContainer: baseSemanticAnnotationTripletContainer,
		semanticAnnotationTripletContainerEdit: {
			...baseSemanticAnnotationTripletContainer,
			borderRight: 'unset',
			backgroundColor: theme.palette.grey[50],
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
			predicateAccessionId: '',
			action: 'CREATE'
		};

		if (semanticAnnotations && Array.isArray(semanticAnnotations.value)) {
			semanticAnnotations.value.push(newSemanticAnnotation);
		} else {
			semanticAnnotations.value = [newSemanticAnnotation];
		}

		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'semanticAnnotations',
			value: semanticAnnotations.value
		});
	};

	handleRemoveSemanticAnnotation = (id) => {
		const type = this.getType(this.props);
		let { semanticAnnotations } = type;
		const index = semanticAnnotations.value.findIndex(
			ann => (ann.permId || ann.tempPermId?.permId) === id
		);
		if (index === -1) return;
		const annotation = semanticAnnotations.value[index];

		if (!annotation.permId) {
			semanticAnnotations.value.splice(index, 1);
		} else {
			annotation.action = 'DELETE';
		}

		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'semanticAnnotations',
			value: semanticAnnotations.value
		});
	}

	handleSemanticAnnotationFieldChange = (id, field, fieldValue) => {
		const type = this.getType(this.props);
		let { semanticAnnotations } = type;
		const annotation = semanticAnnotations.value.find(
			ann => (ann.permId || ann.tempPermId?.permId) === id
		);
		if (!annotation) return;

		annotation[field] = fieldValue;

		if (annotation.permId && annotation.action !== 'DELETE' && annotation.action !== 'CREATE') {
			annotation.action = 'UPDATE';
		}

		this.props.onChange(EntityTypeFormSelectionType.TYPE, {
			field: 'semanticAnnotations',
			value: semanticAnnotations.value
		});
	}

	renderSemanticAnnotationFields(semanticAnnotationTriplet, permId, selectionType) {
		const { id, predicateOntologyId, predicateOntologyVersion, predicateAccessionId, error } = semanticAnnotationTriplet;
		const { mode, classes } = this.props;
		return (
			<>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ID)}
						name={selectionType + '-predicateOntologyId-' + permId}
						mandatory={true}
						error={error?.predicateOntologyId}
						value={predicateOntologyId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(permId, 'predicateOntologyId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_VERSION)}
						name={selectionType + '-predicateOntologyVersion-' + permId}
						mandatory={true}
						error={error?.predicateOntologyVersion}
						value={predicateOntologyVersion}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(permId, 'predicateOntologyVersion', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ANNOTATION_ID)}
						name={selectionType + '-predicateAccessionId-' + permId}
						mandatory={true}
						error={error?.predicateAccessionId}
						value={predicateAccessionId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(permId, 'predicateAccessionId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
			</>
		);
	}

	renderSemanticAnnotationTriplet(semanticAnnotationTriplet, permId, selectionType) {
		const { classes, mode } = this.props;
		return (
			<div key={`${selectionType}-annotation-${permId}`} className={mode === 'edit' ? classes.semanticAnnotationTripletContainerEdit : classes.semanticAnnotationTripletContainer}>
				<div className={classes.semanticAnnotationFieldsWrapper}>
					{this.renderSemanticAnnotationFields(semanticAnnotationTriplet, permId, selectionType)}
				</div>
				{mode === 'edit' && (
					<Button
						variant='contained'
						color='white'
						className={classes.removeButton}
						onClick={() => this.handleRemoveSemanticAnnotation(permId)}
						label={<RemoveIcon />}
						aria-label={`${messages.get(messages.REMOVE)} ${selectionType} annotation ${permId + 1}`}
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

		const filteredValue = (value || []).filter(
			annotation => annotation.action !== 'DELETE'
		)

		if (filteredValue.length === 0) {
			return <Typography variant="body2" color="textSecondary">{messages.get(messages.NO_ANNOTATIONS_DEFINED)}</Typography>
		}

		return filteredValue.map((semanticAnnotationTriplet) => {
			const permId = semanticAnnotationTriplet.permId || semanticAnnotationTriplet.tempPermId?.permId;
			return this.renderSemanticAnnotationTriplet(semanticAnnotationTriplet, permId, EntityTypeFormSelectionType.TYPE)
		})
	}

	render() {
		logger.log(logger.DEBUG, 'EntityTypeFormParametersTypeSemanticAnnotation.render');

		const type = this.getType(this.props)
		if (!type) return null

		return (
			<Container>
				{this.renderHeader(messages.get(messages.SEMANTIC_ANNOTATIONS), type)}
				{(type.objectType?.value != objectTypes.OBJECT_TYPE && type.objectType?.value != objectTypes.NEW_OBJECT_TYPE) ? 
					(<Typography variant="body2" color="textSecondary">Semantic Annotations are not yet supported for object type: [{type.objectType.value}] !</Typography>)
					: this.renderSemanticAnnotations(type)}
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
				{mode === 'edit' && (type.objectType?.value === objectTypes.OBJECT_TYPE || type.objectType?.value === objectTypes.NEW_OBJECT_TYPE) &&
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
