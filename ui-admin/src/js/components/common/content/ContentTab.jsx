import React from 'react'
import logger from '@src/js/common/logger.js'
import withStyles from '@mui/styles/withStyles';
import Icon from '@mui/material/Icon'

const styles = theme => ({
  iconTab: {
    display: 'flex',
    alignItems: 'center',
  },
  text: {
    paddingTop: '2px',
    paddingLeft: '3px',
  }
})

class ContentTab extends React.PureComponent {
  render() {
    logger.log(logger.DEBUG, 'ContentTab.render')

    const { label, icon, changed } = this.props

    let text = label + (changed ? '*' : '');
    if(!icon) {
        return text;
    }
    return (<>
                <div className={this.props.classes.iconTab}>
                    <div dangerouslySetInnerHTML={{ __html: icon }} />
                    <div className={this.props.classes.text}>{' ' + text}</div>
                </div>
            </>)
  }
}

export default withStyles(styles)(ContentTab)
