import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Button from '@src/js/components/common/form/Button.jsx'
import Dialog from '@src/js/components/common/dialog/Dialog.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import { DialogContentText } from '@mui/material'
import TextField from '@mui/material/TextField'
import autoBind from 'auto-bind'

const styles = theme => ({
  button: {
    marginLeft: theme.spacing(1)
  }
})

class InputDialog extends React.Component {
  constructor(props) {
    super(props)
    autoBind(this)

    this.state = {
      value: this.props.inputValue || ''
    }
  }

  componentDidUpdate(prevProps) {  
    if (this.props.inputValue !== prevProps.inputValue) {
      this.setState({ value: this.props.inputValue || '' });
    }
  }

  handleClose() {
    const { onCancel } = this.props
    if (onCancel) {
      onCancel()
    }
  }

  updateValue(event) {
    const value = event.target.value
    this.setState({
      value: value
    })
  }

  handleConfirmClick() {
    const { onConfirm } = this.props
    const { value } = this.state
    onConfirm(value)

    if (!this.props.inputValue) {
      this.clearInput()
    }
  }

  handleCancelClick() {
    const { onCancel } = this.props
    onCancel()
    if (!this.props.inputValue) {
      this.clearInput()
    }
  }

  clearInput() {
    this.setState({
      value: ''
    })
  }

  renderButtons() {
    const { classes } = this.props
    const { value } = this.state
    return (
      <div>
        <Button
          name='confirm'
          label={messages.get(messages.CONFIRM)}
          type={this.getButtonType()}
          styles={{ root: classes.button }}
          onClick={this.handleConfirmClick}
          disabled={!value || !value.trim()}
        />
        <Button
          name='cancel'
          label={messages.get(messages.CANCEL)}
          styles={{ root: classes.button }}
          onClick={this.handleCancelClick}
        />
      </div>
    )
  }

  getMessageType() {
    const type = this.getType()

    if (type === 'warning') {
      return 'warning'
    } else if (type === 'info') {
      return 'info'
    } else {
      throw new Error('Unsupported type: ' + type)
    }
  }

  getButtonType() {
    const type = this.getType()

    if (type === 'warning') {
      return 'risky'
    } else if (type === 'info') {
      return null
    } else {
      throw new Error('Unsupported type: ' + type)
    }
  }

  getType() {
    return this.props.type || 'warning'
  }

  render() {
    logger.log(logger.DEBUG, 'InputDialog.render')

    const { open, title, inputLabel, inputType, content, error, errorText} = this.props
    const { value} = this.state

    return (
      <Dialog
        open={open}
        onClose={this.handleClose}
        title={title || messages.get(messages.INPUT)}
        content={[<DialogContentText key='dialog-content'>{content}</DialogContentText>,
          <TextField
            key='dialog-text'
            error={error} 
            autoFocus
            margin='dense'
            label={inputLabel}
            type={inputType || 'text'}
            fullWidth
            variant='standard'
            helperText={error ? errorText : ""}
            value={value}
            onChange={this.updateValue}
            />]}
        actions={this.renderButtons()}
      />
    )
  }
}

export default withStyles(styles)(InputDialog)
