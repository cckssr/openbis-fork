import React, { useEffect, useMemo, useState } from 'react';
import withStyles from '@mui/styles/withStyles';
import { DialogContentText } from '@mui/material';
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

type MessageType = 'warning' | 'info';

interface DeleteDialogEntityContext {
  kind?: string;
  dependentEntities?: { experiments?: any[]; samples?: any[] } | null;
  count?: number; // number of dependent entities
  bypassesTrashcan?: boolean;
}

interface DeleteDialogUIConfig {
  title?: string;
  content?: string;
  warningText?: string;
  additionalText?: string;
  inputLabel?: string;
  customPlugin?: React.ReactNode;
  type?: MessageType;
}

interface DeleteDialogConfig {
  includeReason?: boolean;
  inputValue?: string;
  entity?: DeleteDialogEntityContext;
  ui?: DeleteDialogUIConfig;
}

interface DeleteConfirmationDialogProps {
  open: boolean;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
  config?: DeleteDialogConfig;
  classes?: any;
}

const DeleteConfirmationDialog = ({ open, onConfirm, onCancel, config, classes }: DeleteConfirmationDialogProps) => {
  logger.log(logger.DEBUG, 'DeleteConfirmationDialog.render');

  const includeReason = config?.includeReason !== undefined ? config.includeReason : true;
  const inputValue = config?.inputValue || '';
  const ui = config?.ui || {};
  const entity = config?.entity || {};

  const [value, setValue] = useState<string>(inputValue);

  useEffect(() => {
    setValue(inputValue || '');
  }, [inputValue]);

  const type: MessageType = ui.type || 'warning';

  const isReasonValid = useMemo(() => {
    return includeReason ? value.trim().length > 0 : true;
  }, [includeReason, value]);

  const getButtonType = () => {
    if (type === 'warning') return 'risky';
    if (type === 'info') return null;
    throw new Error('Unsupported type: ' + type);
  };

  const renderWarningContent = () => {
    const { customPlugin, warningText } = ui;
    const entityKind = entity.kind || 'entity';
    const totalDependentEntities = entity.count || 0;

    if (customPlugin) {
      return customPlugin;
    } else if (warningText) {
      return <Message type="warning">{warningText}</Message>;
    } else if (totalDependentEntities > 0) {
      return (
        <Message type="warning">
          <>
            This {entityKind} has {totalDependentEntities} dependent {totalDependentEntities > 1 ? 'entities' : 'entity'} that will be deleted first.
          </>
        </Message>
      );
    }
  };

  const renderInfoText = () => {
    if (!includeReason) return null;
    const count = entity.count || 1;
    const bypassesTrashcan = !!entity.bypassesTrashcan;
    const infoText = (
      <>
        <br />
        By providing a reason for deletion and clicking <i>'Confirm'</i>,
        {count > 1 ? 'these entities' : 'this entity'} {bypassesTrashcan ? ' will be deleted immediately.' : ' will be moved to the Trashcan.'}
        <br />
      </>
    )
    return infoText;
  };

  const renderAdditionalText = () => {
    const { additionalText } = ui;
    const dependentEntities = entity.dependentEntities;
    const count = entity.count || 1;

    if (additionalText) {
      return <>{additionalText}</>;
    } else if (dependentEntities && count > 1) {
      const experimentsCount = dependentEntities.experiments?.length || 0;
      const samplesCount = dependentEntities.samples?.length || 0;
      let generatedAdditionalText = 'This action cannot be undone.';
      if (experimentsCount > 0 || samplesCount > 0) {
        const parts: string[] = [];
        if (experimentsCount > 0) parts.push(`${experimentsCount} experiments`);
        if (samplesCount > 0) parts.push(`${samplesCount} samples`);
        generatedAdditionalText = `The following entities will be deleted: ${parts.join(' and ')}.`;
      }
      return <><br />{generatedAdditionalText}</>;
    }
    return null;
  };

  const dialogContent: any[] = [];

  const warningContent = renderWarningContent();
  if (warningContent) dialogContent.push(warningContent);

  const infoText = renderInfoText();
  if (infoText) dialogContent.push(infoText);

  const additionalText = renderAdditionalText();
  if (additionalText) dialogContent.push(additionalText);

  if (ui.content) {
    dialogContent.push(<DialogContentText key='dialog-content'>{ui.content}</DialogContentText>);
  }

  if (includeReason) {
    dialogContent.push(
      <TextAreaField
        key="reason-to-delete-id"
        id="reason-to-delete-id"
        name={ui.inputLabel || 'Reason for the delete'}
        mandatory={true}
        label={ui.inputLabel || 'Reason for the delete'}
        mode={'edit'}
        disabled={false}
        value={value}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setValue(e.target.value)}
        disableUnderline={true}
      />
    );
  }

  const handleConfirmClick = () => {
    onConfirm(value);
    if (!inputValue) setValue('');
  };

  const handleCancelClick = () => {
    onCancel();
    if (!inputValue) setValue('');
  };

  return (
    <Dialog
      open={open}
      onClose={onCancel}
      title={ui.title || 'Confirm Delete'}
      content={dialogContent}
      actions={(
        <>
          <Button
            name='confirm'
            label={messages.get(messages.CONFIRM)}
            type={getButtonType()}
            styles={{ root: classes.button }}
            onClick={handleConfirmClick}
            disabled={!isReasonValid}
          />
          <Button
            name='cancel'
            label={messages.get(messages.CANCEL)}
            styles={{ root: classes.button }}
            onClick={handleCancelClick}
          />
        </>
      )}
    />
  );
};

export default withStyles(styles)(DeleteConfirmationDialog);
