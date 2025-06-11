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
		},
	};
};

const SEMANTIC_FIELD = {
	PROPERTY: 'semanticAnnotations',
	ASSIGNMENT: 'assignmentSemanticAnnotations'
};

class EntityTypeFormParametersPropertySemanticAnnotation extends React.PureComponent {
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
		const property = this.getProperty(this.props)
		if (property) {
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
		const property = this.getProperty(this.props)
		this.props.onSelectionChange(EntityTypeFormSelectionType.PROPERTY, {
			id: property.id,
			part: event.target.name
		})
	}

	handleBlur() {
		this.props.onBlur()
	}

	handleAddSemanticAnnotation = (property, semanticField) => {

		const semanticAnnotations = property[semanticField];
		//console.log('handleAddSemanticAnnotation: ', semanticAnnotations);

		const newSemanticAnnotation = {
			tempPermId: { permId: `${semanticField}-${property.section}-${property.id}-${semanticAnnotations?.value?.length > 0 ? semanticAnnotations.value.length : 0}` },
			predicateOntologyId: '',
			predicateOntologyVersion: '',
			predicateAccessionId: ''
		};

		if (semanticAnnotations !== undefined && semanticAnnotations !== null && semanticAnnotations.value?.length > 0) {
			semanticAnnotations.value.push(newSemanticAnnotation);
		} else {
			semanticAnnotations.value = [newSemanticAnnotation];
		}

		this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
			id: property.id,
			field: semanticField,
			value: semanticAnnotations.value
		});
	};

	handleRemoveSemanticAnnotation = (semanticAnnotationTripletId, index, semanticField) => {
		const property = this.getProperty(this.props);
		let semanticAnnotations = property[semanticField];
		//it's already the list of semantic annotations
		semanticAnnotations = semanticAnnotations.value.filter((semanticAnnotationTriplet, idx) => idx !== index);
		this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
			id: property.id,
			field: semanticField,
			value: semanticAnnotations
		});
	};

	handleSemanticAnnotationFieldChange(semanticField, index, field, fieldValue) {
		const property = this.getProperty(this.props);
		let semanticAnnotations = property[semanticField];
		semanticAnnotations.value[index][field] = fieldValue;
		this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
			id: property.id,
			field: semanticField,
			value: semanticAnnotations.value
		});
	}

	renderSemanticAnnotationFields(semanticAnnotationTriplet, index, semanticField) {
		const { id, predicateOntologyId, predicateOntologyVersion, predicateAccessionId, error } = semanticAnnotationTriplet;
		const { mode, classes } = this.props;
		return (
			<>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ID)}
						name={'predicateOntologyId-' + semanticField + '-' + index}
						mandatory={true}
						error={error?.predicateOntologyId}
						value={predicateOntologyId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(semanticField, index, 'predicateOntologyId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_VERSION)}
						name={'predicateOntologyVersion-' + semanticField + '-' + index}
						mandatory={true}
						error={error?.predicateOntologyVersion}
						value={predicateOntologyVersion}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(semanticField, index, 'predicateOntologyVersion', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ANNOTATION_ID)}
						name={'predicateAccessionId-' + semanticField + '-' + index}
						mandatory={true}
						error={error?.predicateAccessionId}
						value={predicateAccessionId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(semanticField, index, 'predicateAccessionId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
			</>
		);
	}

	renderSemanticAnnotationTriplet(semanticAnnotationTriplet, index, semanticField) {
		const { classes, mode } = this.props;
		return (
			<div key={`${semanticField}-annotation-${index}`} className={mode === 'edit' ? classes.semanticAnnotationTripletContainerEdit : classes.semanticAnnotationTripletContainer}>
				<div className={classes.semanticAnnotationFieldsWrapper}>
					{this.renderSemanticAnnotationFields(semanticAnnotationTriplet, index, semanticField)}
				</div>
				{mode === 'edit' && (
					<Button
						variant='contained'
						color='white'
						className={classes.removeButton}
						onClick={() => this.handleRemoveSemanticAnnotation(semanticAnnotationTriplet.id, index, semanticField)}
						label={<RemoveIcon />}
						aria-label={`${messages.get(messages.REMOVE)} ${semanticField} annotation ${index + 1}`}
						tooltip={messages.get(messages.REMOVE)}
					/>
				)}
			</div>
		);
	}

	renderSemanticAnnotations(property, semanticField) {
		const semanticAnnotationValue = property[semanticField].value || [];

		//console.log('render semanticAnnotations for ' + semanticField + ' : ', semanticAnnotationValue);

		if (semanticAnnotationValue === undefined || semanticAnnotationValue === null || semanticAnnotationValue.length === 0) {
			return <Typography variant="body2" color="textSecondary">{messages.get(messages.NO_ANNOTATIONS_DEFINED)}</Typography>
		}

		return semanticAnnotationValue.map((semanticAnnotationTriplet, index) => {
			return this.renderSemanticAnnotationTriplet(semanticAnnotationTriplet, index, semanticField)
		})
	}

	render() {
		logger.log(logger.DEBUG, 'EntityTypeFormParametersSemanticAnnotation.render');

		const property = this.getProperty(this.props);
		if (!property) {
			return null;
		}

		//console.log('render EntityTypeFormParametersSemanticAnnotation property: ', property);

		return (
			<Container>
				{this.renderHeader(messages.get(messages.PROPERTY_SEMANTIC_ANNOTATIONS), property, SEMANTIC_FIELD.PROPERTY)}

				{this.renderSemanticAnnotations(property, SEMANTIC_FIELD.PROPERTY)}

				{this.renderHeader(messages.get(messages.PROPERTY_ASSIGNMENT_SEMANTIC_ANNOTATIONS), property, SEMANTIC_FIELD.ASSIGNMENT)}

				{this.renderSemanticAnnotations(property, SEMANTIC_FIELD.ASSIGNMENT)}
			</Container>
		)
	}

	renderHeader(title, property, semanticField) {
		const { mode, classes } = this.props

		return (
			<div className={classes.headerContainer}>
				<Header>{title}</Header>
				{mode === 'edit' &&
					<Button variant='contained'
						color='white'
						onClick={() => this.handleAddSemanticAnnotation(property, semanticField)}
						label={<AddIcon />}
						sx={{ marginRight: '8px' }} />}
			</div>
		)
	}

	getProperty(props) {
		let { properties, selection } = props

		if (selection && selection.type === EntityTypeFormSelectionType.PROPERTY) {
			let [property] = properties.filter(
				property => property.id === selection.params.id
			)
			return property
		} else {
			return null
		}
	}
}

export default withStyles(styles)(EntityTypeFormParametersPropertySemanticAnnotation)
