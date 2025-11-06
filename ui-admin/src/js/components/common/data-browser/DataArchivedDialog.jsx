import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Button from '@src/js/components/common/form/Button.jsx'
import Message from '@src/js/components/common/form/Message.jsx'
import Dialog from '@src/js/components/common/dialog/Dialog.jsx'
import messages from '@src/js/common/messages.js'

const styles = theme => ({
  button: {
    marginLeft: theme.spacing(1)
  }
})

class FileArchivedDialog extends React.Component {
  constructor(props) {
    super(props)
    this.handleClose = this.handleClose.bind(this)
  }


  handleClose() {
    const { onClose } = this.props
    if (onClose) {
      onClose()
    }
  }

  render() {

    const { open } = this.props

    return (
      <Dialog
        open={open}
        onClose={this.handleClose}
        title={messages.get(messages.DATA_IN_THE_ARCHIVE)}
        content={this.renderContent()}
        actions={this.renderButtons()}
      />
    )
  }

  renderContent() {
    return <Message type={'info'}>{messages.get(messages.DATA_IN_THE_ARCHIVE_CANNOT_BE_DOWNLOADED)}</Message>
  }

  renderButtons() {
    const { onClose, classes } = this.props
    return (
      <div>
        {!!onClose && (
          <Button
            name='close'
            label={messages.get(messages.CLOSE)}
            styles={{ root: classes.button }}
            onClick={onClose}
          />
        )}
      </div>
    )
  }
}

export default withStyles(styles)(FileArchivedDialog)
