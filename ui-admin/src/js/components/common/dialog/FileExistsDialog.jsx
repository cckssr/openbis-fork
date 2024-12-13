import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Button from '@src/js/components/common/form/Button.jsx'
import Message from '@src/js/components/common/form/Message.jsx'
import Dialog from '@src/js/components/common/dialog/Dialog.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import CheckboxField from '@src/js/components/common/form/CheckboxField.jsx'

const styles = theme => ({
  button: {
    marginLeft: theme.spacing(1)
  },
  checkboxContainer: {
    marginTop: theme.spacing(2),
    display: 'flex',
    alignItems: 'center',
  },
  checkboxWrapper: {
    marginTop: theme.spacing(1),
  }
})

class FileExistsDialog extends React.Component {
  constructor(props) {
    super(props)
    this.handleClose = this.handleClose.bind(this)
    this.handleCheckboxChange = this.handleCheckboxChange.bind(this);
  }


  handleCheckboxChange(event) {
    const { onApplyToAllChange } = this.props;
    if (onApplyToAllChange) {      
      onApplyToAllChange(event.target.value);      
    }
  }
  
  handleClose() {
    const { onCancel } = this.props
    if (onCancel) {
      onCancel()
    }
  }

  render() {    

    const { open, title} = this.props

    return (
      <Dialog
        open={open}
        onClose={this.handleClose}
        title={title || messages.get(messages.CONFIRMATION)}
        content={this.renderContent()}
        actions={this.renderButtons()}
      />
    )
  }

  renderContent() {
    const { content, onApplyToAllChange, applyToAll, classes} = this.props
    const hasCheckbox = !!onApplyToAllChange;
    return (<>
      <Message type={'warning'}>{content}</Message>
      {hasCheckbox && (  
         <div className={classes.checkboxWrapper}>
          <CheckboxField
            name='applyToAll'
            label={messages.get(messages.APPLY_TO_ALL)}
            value={applyToAll}
            onChange={this.handleCheckboxChange}
          />   
        </div>          
      )}
      </>);
  }

  renderButtons() {
    const { onReplace, onResume, onSkip, onCancel, classes } = this.props
    return (
      <div>
        {!!onReplace && (
          <Button
            name='replace'
            label={messages.get(messages.REPLACE)}
            type={'risky'}
            styles={{ root: classes.button }}
            onClick={onReplace}
          />
        )}
        {!!onResume && (
          <Button
            name='resume'
            label={messages.get(messages.RESUME)}
            type={'risky'}
            styles={{ root: classes.button }}
            onClick={onResume}
          />
        )}
        {!!onSkip && (
          <Button
            name='skip'
            label={messages.get(messages.SKIP)}
            styles={{ root: classes.button }}
            onClick={onSkip}
          />
        )}
        {!!onCancel && (
          <Button
            name='cancel'
            label={messages.get(messages.CANCEL)}
            styles={{ root: classes.button }}
            onClick={onCancel}
          />
        )}
      </div>
    )
  }
}

export default withStyles(styles)(FileExistsDialog)
