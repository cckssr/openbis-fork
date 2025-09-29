import React, { Component } from 'react';
import withStyles from '@mui/styles/withStyles';
import { DialogContentText } from '@mui/material';
import autoBind from 'auto-bind';
import Button from '@src/js/components/common/form/Button.jsx';
import Message from '@src/js/components/common/form/Message.jsx';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import messages from '@src/js/common/messages.js';
import logger from '@src/js/common/logger.js';
import TextAreaField from '@src/js/components/common/form/TextAreaField.jsx';

const styles = (theme: any) => ({
  button: {
    marginLeft: theme.spacing(1)
  }
});

interface DeleteConfirmationDialogProps {
  open: boolean;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
  title?: string;
  inputLabel?: string;
  inputType?: string;
  content?: string;
  error?: boolean;
  errorText?: string;
  inputValue?: string;
  type?: 'warning' | 'info';
  warningText?: string;
  includeReason?: boolean;
  numberOfEntities?: number;
  bypassesTrashcan?: boolean;
  additionalText?: string;
  customPlugin?: React.ReactNode;
  dependentEntities?: any;
  entityKind?: string;
  classes?: any;
}

interface DeleteConfirmationDialogState {
  value: string;
}

class DeleteConfirmationDialog extends Component<DeleteConfirmationDialogProps, DeleteConfirmationDialogState> {
  constructor(props: DeleteConfirmationDialogProps) {
    super(props);
    autoBind(this);

    this.state = {
      value: this.props.inputValue || ''
    };
  }

  componentDidUpdate(prevProps: DeleteConfirmationDialogProps) {
    if (this.props.inputValue !== prevProps.inputValue) {
      this.setState({ value: this.props.inputValue || '' });
    }
  }

  handleClose() {
    const { onCancel } = this.props;
    if (onCancel) {
      onCancel();
    }
  }

  updateValue(event: React.ChangeEvent<HTMLInputElement>) {
    const value = event.target.value;
    this.setState({
      value: value
    });
  }

  handleConfirmClick() {
    const { onConfirm } = this.props;
    const { value } = this.state;
    onConfirm(value);

    if (!this.props.inputValue) {
      this.clearInput();
    }
  }

  handleCancelClick() {
    const { onCancel } = this.props;
    onCancel();
    if (!this.props.inputValue) {
      this.clearInput();
    }
  }

  clearInput() {
    this.setState({
      value: ''
    });
  }

  renderButtons() {
    const { classes } = this.props;
    const { value } = this.state;
    return (
      <div>
        <Button
          name='confirm'
          label={messages.get(messages.CONFIRM)}
          type={this.getButtonType()}
          styles={{ root: classes.button }}
          onClick={this.handleConfirmClick}
          disabled={!this.isReasonValid()}
        />
        <Button
          name='cancel'
          label={messages.get(messages.CANCEL)}
          styles={{ root: classes.button }}
          onClick={this.handleCancelClick}
        />
      </div>
    );
  }

  isReasonValid(): boolean {
    const { includeReason = true } = this.props;
    const { value } = this.state;
    return includeReason ? value.trim().length > 0 : true;
  }

  getMessageType() {
    const type = this.getType();

    if (type === 'warning') {
      return 'warning';
    } else if (type === 'info') {
      return 'info';
    } else {
      throw new Error('Unsupported type: ' + type);
    }
  }

  getButtonType() {
    const type = this.getType();

    if (type === 'warning') {
      return 'risky';
    } else if (type === 'info') {
      return null;
    } else {
      throw new Error('Unsupported type: ' + type);
    }
  }

  getType(): 'warning' | 'info' {
    return this.props.type || 'warning';
  }

  renderWarningContent() {
    const { customPlugin, warningText, dependentEntities, entityKind = 'entity', numberOfEntities = 1 } = this.props;
    
    if (customPlugin) {
      return customPlugin;
    } else if (warningText) {
      return (
        <Message type="warning">
          {warningText}
        </Message>
      );
    } else {
      // Generate warning text based on dependencies
      const totalDependentEntities = numberOfEntities; // Subtract 1 for the main entity
      let generatedWarningText = '';
      
      if (totalDependentEntities > 0) {
        generatedWarningText = `This ${entityKind} has ${totalDependentEntities} dependent ${totalDependentEntities > 1 ? 'entities' : 'entity'} that will be deleted first.`;
      }
      
      return (
        <Message type="warning">
          <span style={{ color: 'orange' }}>{generatedWarningText}</span>
        </Message>
      );
    }
  }

  renderInfoText() {
    const { includeReason = true, numberOfEntities = 1, bypassesTrashcan = false } = this.props;

    if (!includeReason) return null;

    let infoText = "By providing a reason for deletion and clicking 'Accept', ";
    infoText += numberOfEntities > 1 ? "these entities" : "this entity";

    if (bypassesTrashcan) {
      infoText += " will be deleted immediately.";
    } else {
      infoText += " will be moved to the Trashcan.";
    }

    return (
      <>
        {infoText}
      </>
    );
  }

  renderAdditionalText() {
    const { additionalText, dependentEntities, numberOfEntities = 1 } = this.props;
    
    if (additionalText) {
      return (
        <DialogContentText>
          {additionalText}
        </DialogContentText>
      );
    } else if (dependentEntities && numberOfEntities > 1) {
      // Generate additional text based on dependent entities
      const experimentsCount = dependentEntities.experiments?.length || 0;
      const samplesCount = dependentEntities.samples?.length || 0;
      
      let generatedAdditionalText = 'This action cannot be undone.';
      
      if (experimentsCount > 0 || samplesCount > 0) {
        const parts = [];
        if (experimentsCount > 0) parts.push(`${experimentsCount} experiments`);
        if (samplesCount > 0) parts.push(`${samplesCount} samples`);
        
        generatedAdditionalText = `The following entities will be deleted: ${parts.join(' and ')}.`;
      }
      
      return (
        <>
          {generatedAdditionalText}
        </>
      );
    }
    return null;
  }

  renderReasonField() {
    const { includeReason = true, inputLabel = "Reason for the delete" } = this.props;
    const { value } = this.state;

    if (!includeReason) return null;

    return (<TextAreaField id="reason-to-delete-id"
      name={inputLabel}
      mandatory={true}
      label={inputLabel}
      mode={'edit'}
      disabled={false}
      value={value}
      onChange={(e: React.ChangeEvent<HTMLInputElement>) => this.updateValue(e)}
      disableUnderline={true}
    />
    );
  }

  render() {
    logger.log(logger.DEBUG, 'DeleteConfirmationDialog.render');

    const {
      open,
      title,
      content,
      error,
      errorText,
      includeReason = true
    } = this.props;
    const { value } = this.state;

    const dialogContent = [];

    // Add warning content if present
    const warningContent = this.renderWarningContent();
    if (warningContent) {
      dialogContent.push(warningContent);
    }

    // Add info text if includeReason is true
    const infoText = this.renderInfoText();
    if (infoText) {
      dialogContent.push(infoText);
    }

    // Add additional text if present
    const additionalText = this.renderAdditionalText();
    if (additionalText) {
      dialogContent.push(additionalText);
    }

    // Add original content if present
    if (content) {
      dialogContent.push(
        <DialogContentText key='dialog-content'>{content}</DialogContentText>
      );
    }

    // Add reason field if includeReason is true
    const reasonField = this.renderReasonField();
    if (reasonField) {
      dialogContent.push(reasonField);
    }

    return (
      <Dialog
        open={open}
        onClose={this.handleClose}
        title={title || "Confirm Delete"}
        content={dialogContent}
        actions={this.renderButtons()}
      />
    );
  }
}

export default withStyles(styles)(DeleteConfirmationDialog);
