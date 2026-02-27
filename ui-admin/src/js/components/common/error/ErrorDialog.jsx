import React from 'react'
import withStyles from '@mui/styles/withStyles';
import ErrorObject from '@src/js/components/common/error/ErrorObject.js'
import Message from '@src/js/components/common/form/Message.jsx'
import Button from '@src/js/components/common/form/Button.jsx'
import Dialog from '@src/js/components/common/dialog/Dialog.jsx'
import Link from '@mui/material/Link'
import Collapse from '@mui/material/Collapse'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  button: {
    marginLeft: theme.spacing(1)
  },
  content: {
    whiteSpace: 'pre'
  },
  stackContainer: {
    marginTop: theme.spacing(1)
  },
  stackLink: {
    cursor: 'pointer'
  },
  stackContent: {
    marginTop: theme.spacing(1),
    marginBottom: theme.spacing(1)
  }
})

class ErrorDialog extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      stackVisible: false
    }
    this.handleStackVisibleChange = this.handleStackVisibleChange.bind(this)
  }

  handleStackVisibleChange() {
    this.setState(state => ({
      stackVisible: !state.stackVisible
    }))
  }

  render() {
    logger.log(logger.DEBUG, 'ErrorDialog.render')

    const { error } = this.props

    return (
      <Dialog
        open={!!error}
        title={messages.get(messages.ERROR)}
        content={this.renderContent()}
        actions={this.renderButtons()}
      />
    )
  }

  renderContent() {
    const { classes } = this.props

    const errorObject = new ErrorObject(this.props.error)

    return (
      <div className={classes.content}>
        <Message type='error'>
          <div>
            {errorObject.getMessage() && <div>{errorObject.getMessage()}</div>}
            {this.renderStack()}
          </div>
        </Message>
      </div>
    )
  }

  renderStack() {
    const errorObject = new ErrorObject(this.props.error)

    if (!errorObject.getStackTrace()) {
      return null
    }

    const { classes } = this.props
    const { stackVisible } = this.state

    return (
      <div className={classes.stackContainer}>
        <Link
        underline='none'
          onClick={this.handleStackVisibleChange}
          className={classes.stackLink}
        >
          {stackVisible
            ? messages.get(messages.HIDE_STACK_TRACE)
            : messages.get(messages.SHOW_STACK_TRACE)}
        </Link>
        <Collapse in={stackVisible} mountOnEnter={true} unmountOnExit={true}>
          <pre className={classes.stackContent}>
            {errorObject.getStackTrace()}
          </pre>
        </Collapse>
      </div>
    )
  }

  renderButtons() {
    const { onClose, classes, selectionButtonProps } = this.props
    const sharedButtonProps = selectionButtonProps || {}
    const incomingStyles = sharedButtonProps.styles
    const mergedStyles = incomingStyles
      ? {
          ...incomingStyles,
          root: [classes.button, incomingStyles.root].filter(Boolean).join(' ')
        }
      : { root: classes.button }

    return (
      <div>
        <Button
          {...sharedButtonProps}
          label={messages.get(messages.CLOSE)}
          styles={mergedStyles}
          onClick={onClose}
        />
      </div>
    )
  }
}

export default withStyles(styles)(ErrorDialog)
