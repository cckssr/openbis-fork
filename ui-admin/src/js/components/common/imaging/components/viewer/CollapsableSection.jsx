import React from 'react'
import { Typography, Collapse, Divider, IconButton } from '@mui/material';
import makeStyles from '@mui/styles/makeStyles';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';

const useStyles = makeStyles((theme) => ({
	container: {
		display: 'flex',
		alignItems: 'center',
	},
	jc_sb: {
		justifyContent: 'space-between'
	},
	span: {
		marginLeft: '16px'
	},
	span2: {
		marginLeft: '32px'
	}
}));

const CollapsableSection = ({ children, renderActions = () => false, title, isCollapsed = true, canCollapse = true, renderWarnings, span = false }) => {
	const [collapsed, setCollapsed] = React.useState(isCollapsed);

	const handleChange = () => {
		setCollapsed((prev) => !prev);
	};

	const classes = useStyles();

	return (<>
		<div className={classes.container + ' ' + classes.jc_sb + ' ' + (span && classes.span)}>
			<div className={classes.container}>
				<IconButton aria-label="delete" size="large" onClick={canCollapse ? handleChange : () => false} disabled={!canCollapse}>
					{collapsed ? <KeyboardArrowRightIcon /> : <KeyboardArrowDownIcon />}
				</IconButton>
				<Typography variant='h6' >
					{title}
				</Typography>
			</div>
			<div>
				{renderWarnings}
				{collapsed && renderActions()}
			</div>
		</div>
		{collapsed && <Divider className={span && classes.span2} />}
		<Collapse in={!collapsed} className={span && classes.span2}>
			{children}
			<Divider sx={{ mt: 1 }} />
		</Collapse>
	</>
	);
}

export default CollapsableSection;