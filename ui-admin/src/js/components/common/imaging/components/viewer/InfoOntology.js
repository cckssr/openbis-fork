import React from 'react'

import makeStyles from '@mui/styles/makeStyles';
import InfoIcon from '@mui/icons-material/Info'
import Tooltip from '@src/js/components/common/form/Tooltip.jsx'

const useStyles = makeStyles((theme) => ({
	descriptionDefault: {
		lineHeight: '0.7rem',
		'& svg': {
			color: theme.palette.hint.main,
		},
		cursor: 'pointer',
	},
	link: {
		color: '#337ab7',
		textDecoration: 'none',
		cursor: 'pointer',
	},
}));

const InfoOntology = ({ semanticAnnotation }) => {
	const classes = useStyles();
	const TRUNCATE_VALUE = 30;


	const truncateString = (str, maxLength) => {
		if (str && str.length > maxLength) {
			return str.substring(0, maxLength) + '...';
		}
		return str;
	};

	const validateLink = (link) => {
		try {
			new URL(link);
			return true;
		} catch (_) {
			return false;
		}
	};

	const createLink = (value) => {
		if (value && validateLink(value)) {
			return (
				<a href={value} target="_blank" rel="noopener noreferrer" className={classes.link}>
					{truncateString(value, TRUNCATE_VALUE)}
				</a>
			);
		}
		return truncateString(value, TRUNCATE_VALUE);
	};

	if (semanticAnnotation) {
		const { ontologyId, ontologyVersion, ontologyAnnotationId } = semanticAnnotation;

		return (
			<span data-part='description' className={classes.descriptionDefault}>
				<Tooltip
					placement='left'
					title={
						<>
							<strong>Semantic Annotation:</strong>
							<br />
							{createLink(ontologyAnnotationId)}
							<br />
							(Ontology: {createLink(ontologyId)}, OntologyVersion: {createLink(ontologyVersion)})
						</>
					}
				>
					<InfoIcon fontSize="small" />
				</Tooltip>
			</span>
		);
	}
	return null;
}


export default InfoOntology;