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

const ANNOTATION_TYPES = {
	PROPERTY: 'semanticAnnotations',
	ASSIGNMENT: 'assignmentSemanticAnnotations'
};

class EntityTypeFormParametersSemanticAnnotation extends React.PureComponent {
	constructor(props) {
		super(props);
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

	handleAddSemanticAnnotation = (property) => {
		let { semanticAnnotations } = property;

		const newSemanticAnnotation = {
			id: `${property.section}-${property.id}-${semanticAnnotations?.length > 0 ? semanticAnnotations.length : 0}`,
			ontologyId: '',
			ontologyVersion: '',
			ontologyAnnotationId: ''
		};

		if (semanticAnnotations !== undefined && semanticAnnotations !== null && semanticAnnotations.length > 0) {
			semanticAnnotations.push(newSemanticAnnotation);
		} else {
			semanticAnnotations = [newSemanticAnnotation];
		}

		this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
			id: property.id,
			field: ANNOTATION_TYPES.PROPERTY,
			value: semanticAnnotations
		})
	};

	handleRemoveSemanticAnnotation = (semanticAnnotationTripletId, index) => {
		const property = this.getProperty(this.props);
		let { semanticAnnotations } = property;
		semanticAnnotations = semanticAnnotations.value.filter((semanticAnnotationTriplet, idx) => idx !== index);
		this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
			id: property.id,
			field: ANNOTATION_TYPES.PROPERTY,
			value: semanticAnnotations.value
		});
	};

	handleSemanticAnnotationFieldChange(type, index, field, fieldValue) {
		const property = this.getProperty(this.props);
		let { semanticAnnotations } = property;
		semanticAnnotations.value[index][field] = fieldValue;
		this.props.onChange(EntityTypeFormSelectionType.PROPERTY, {
			id: property.id,
			field: ANNOTATION_TYPES.PROPERTY,
			value: semanticAnnotations.value
		});
	}

	renderSemanticAnnotationFields(semanticAnnotationTriplet, index, type) {
		console.log('render semanticAnnotationTriplet: ', semanticAnnotationTriplet);
		const { id, ontologyId, ontologyVersion, ontologyAnnotationId } = semanticAnnotationTriplet;
		const { mode, classes } = this.props;
		return (
			<>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ID)}
						name={'ontologyId-' + index}
						mandatory={true}
						//error={error}
						value={ontologyId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(type, index, 'ontologyId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_VERSION)}
						name={'ontologyVersion' + index}
						mandatory={true}
						//error={annotationItem.errors?.ontologyVersion}
						value={ontologyVersion}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(type, index, 'ontologyVersion', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
				<div className={classes.field}>
					<TextField
						label={messages.get(messages.ONTOLOGY_ANNOTATION_ID)}
						name={'ontologyAnnotationId' + index}
						mandatory={true}
						//error={annotationItem.errors?.ontologyAnnotationId}
						value={ontologyAnnotationId}
						mode={mode}
						onChange={(event) => this.handleSemanticAnnotationFieldChange(type, index, 'ontologyAnnotationId', event.target.value)}
						onFocus={(event) => this.handleFocus(event)}
						onBlur={(event) => this.handleBlur()}
					/>
				</div>
			</>
		);
	}

	renderSemanticAnnotationTriplet(semanticAnnotationTriplet, index, type) {
		const { classes, mode } = this.props;
		return (
			<div key={`${type}-annotation-${index}`} className={mode === 'edit' ? classes.semanticAnnotationTripletContainerEdit : classes.semanticAnnotationTripletContainer}>
				<div className={classes.semanticAnnotationFieldsWrapper}>
					{this.renderSemanticAnnotationFields(semanticAnnotationTriplet, index, type)}
				</div>
				{mode === 'edit' && (
					<Button
						variant='contained'
						color='white'
						className={classes.removeButton}
						onClick={() => this.handleRemoveSemanticAnnotation(semanticAnnotationTriplet.id, index)}
						label={<RemoveIcon />}
						aria-label={`${messages.get(messages.REMOVE)} ${type} annotation ${index + 1}`}
						tooltip={messages.get(messages.REMOVE)}
					/>
				)}
			</div>
		);
	}

	renderSemanticAnnotations(property) {
		const { visible, value } = { ...property.semanticAnnotations }

		if (!visible) {
			return null
		}
		console.log('render semanticAnnotations: ', property.semanticAnnotations);

		if (value === undefined || value === null || value.length === 0) {
			return <Typography variant="body2" color="textSecondary">{messages.get(messages.NO_ANNOTATIONS_DEFINED)}</Typography>
		}

		return value.map((semanticAnnotationTriplet, index) => {
			return this.renderSemanticAnnotationTriplet(semanticAnnotationTriplet, index, ANNOTATION_TYPES.PROPERTY)
		})
	}

	render() {
		logger.log(logger.DEBUG, 'EntityTypeFormParametersSemanticAnnotation.render');

		const property = this.getProperty(this.props);
		if (!property) {
			return null;
		}

		console.log('render EntityTypeFormParametersSemanticAnnotation property: ', property);

		return (
			<Container>
				{this.renderHeader(messages.get(messages.PROPERTY_SEMANTIC_ANNOTATIONS), property)}

				{this.renderSemanticAnnotations(property)}

				{this.renderHeader(messages.get(messages.PROPERTY_ASSIGNMENT_SEMANTIC_ANNOTATIONS), property)}

			</Container>
		)
	}

	renderHeader(title, property) {
		const { mode, classes } = this.props

		return (
			<div className={classes.headerContainer}>
				<Header>{title}</Header>
				{mode === 'edit' &&
					<Button variant='contained'
						color='white'
						onClick={() => this.handleAddSemanticAnnotation(property)}
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

export default withStyles(styles)(EntityTypeFormParametersSemanticAnnotation)
