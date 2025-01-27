import _ from 'lodash'
import React from 'react'
import withStyles from '@mui/styles/withStyles';
import AccountTreeIcon from '@mui/icons-material/AccountTreeOutlined'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import CloseIcon from '@mui/icons-material/Close'
import IconButton from '@mui/material/IconButton'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import ListItemIcon from '@mui/material/ListItemIcon'
import Tooltip from '@src/js/components/common/form/Tooltip.jsx'
import Message from '@src/js/components/common/form/Message.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  item: {
    flexGrow: 0,
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: 0
  },
  icon: {
    margin: '-2px 4px -2px 8px',
    minWidth: '24px'
  },
  text: {
    fontSize: theme.typography.body2.fontSize,
    lineHeight: theme.typography.body2.fontSize
  },
  path: {
    display: 'flex',
    overflow: 'hidden'
  },
  pathNodeNonRootable: {
    display: 'flex',
    cursor: 'default'
  },
  pathNodeRootable: {
    display: 'flex',
    cursor: 'pointer',
    '&:hover': {
      textDecoration: 'underline'
    }
  },
  clear: {
    padding: '4px',
    margin: '-4px'
  }
})

class BrowserRoot extends React.PureComponent {
  constructor(props) {
    super(props)
    this.handleClick = this.handleClick.bind(this)
    this.handleClear = this.handleClear.bind(this)
  }

  handleClick(event, pathNode) {
    const { onRootChange } = this.props
    if (onRootChange) {
      onRootChange(pathNode)
    }
    event.stopPropagation()
  }

  handleClear(event) {
    const { onRootClear } = this.props
    if (onRootClear) {
      onRootClear()
    }
    event.stopPropagation()
  }

  render() {
    logger.log(logger.DEBUG, 'BrowserRoot.render')

    const { rootNode, classes } = this.props

    if (!rootNode) {
      return null
    }

    return (
      <ListItemButton
        classes={{
          root: classes.item
        }}
      >
        {this.renderHome()}
        {this.renderPath()}
        {this.renderClear()}
      </ListItemButton>
    )
  }

  renderHome() {
    const { classes } = this.props

    return (
      <ListItemIcon
        onClick={this.handleClear}
        classes={{
          root: classes.icon
        }}
      >
        <AccountTreeIcon fontSize='small' />
      </ListItemIcon>
    )
  }

  renderPath() {
    const { rootNode, classes } = this.props

    if (_.isEmpty(rootNode.path)) {
      return null
    }

    const pathNodes = rootNode.path.map((pathNode, index) => (
      <div
        key={index}
        className={
          pathNode.rootable
            ? classes.pathNodeRootable
            : classes.pathNodeNonRootable
        }
      >
        {this.renderPathNodeText(index)}
        {this.renderPathNodeSeparator(index)}
      </div>
    ))

    return (
      <ListItemText>
        <div className={classes.path}>{pathNodes}</div>
      </ListItemText>
    )
  }

  renderPathNodeText(index) {
    const { rootNode } = this.props

    const pathNode = rootNode.path[index]
    const pathNodeText = pathNode.message ? (
      <Message type={pathNode.message.type}>{pathNode.message.text}</Message>
    ) : (
      <Message>{pathNode.text}</Message>
    )

    if (pathNode.rootable) {
      return (
        <div onClick={event => this.handleClick(event, pathNode)}>
          {pathNodeText}
        </div>
      )
    } else {
      return pathNodeText
    }
  }

  renderPathNodeSeparator(index) {
    const { rootNode, classes } = this.props

    if (index < rootNode.path.length - 1) {
      return (
        <ListItemIcon
          classes={{
            root: classes.icon
          }}
        >
          <ChevronRightIcon />
        </ListItemIcon>
      )
    } else {
      return null
    }
  }

  renderClear() {
    const { classes } = this.props

    return (
      <Tooltip title={messages.get(messages.CLEAR_ROOT)}>
        <IconButton
          onClick={this.handleClear}
          classes={{ root: classes.clear }}
          size='small'
        >
          <CloseIcon fontSize='small' />
        </IconButton>
      </Tooltip>
    )
  }
}

export default withStyles(styles)(BrowserRoot)
