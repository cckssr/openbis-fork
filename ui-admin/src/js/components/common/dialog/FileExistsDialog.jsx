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
    const { onReplace, onResume, onSkip, onCancel, classes, selectionButtonProps } = this.props
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
        {!!onReplace && (
          <Button
            {...sharedButtonProps}
            name='replace'
            label={messages.get(messages.REPLACE)}
            type={'risky'}
            styles={mergedStyles}
            onClick={onReplace}
          />
        )}
        {!!onResume && (
          <Button
            {...sharedButtonProps}
            name='resume'
            label={messages.get(messages.RESUME)}
            type={'risky'}
            styles={mergedStyles}
            onClick={onResume}
          />
        )}
        {!!onSkip && (
          <Button
            {...sharedButtonProps}
            name='skip'
            label={messages.get(messages.SKIP)}
            styles={mergedStyles}
            onClick={onSkip}
          />
        )}
        {!!onCancel && (
          <Button
            {...sharedButtonProps}
            name='cancel'
            label={messages.get(messages.CANCEL)}
            styles={mergedStyles}
            onClick={onCancel}
          />
        )}
      </div>
    )
  }
}

export default withStyles(styles)(FileExistsDialog)
