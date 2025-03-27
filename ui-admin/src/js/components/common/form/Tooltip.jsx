import React from 'react';
import withStyles from '@mui/styles/withStyles';
import MaterialTooltip from '@mui/material/Tooltip';
import logger from '@src/js/common/logger.js';

const styles = (theme) => ({
  tooltip: {
    backgroundColor: '#fff',
    color: '#8d8d8d',
    border: 'none',
    borderRadius: '5px',
    boxShadow: '0 0 10px 6px rgba(0, 0, 0, 0.1)',
    fontSize: theme.typography.body2.fontSize, 
  },
  arrow: {
    color: '#fff',
  },
});

class Tooltip extends React.PureComponent {
  render() {
    logger.log(logger.DEBUG, 'Tooltip.render');

    const { children, classes, title, enterDelay = 1000, placement } = this.props;

    return (
      <MaterialTooltip
        enterDelay={enterDelay}
        title={title} 
        classes={{ tooltip: classes.tooltip, arrow: classes.arrow }}
        arrow={true}
        placement={placement}
      >
        {children}
      </MaterialTooltip>
    );
  }
}

export default withStyles(styles)(Tooltip);