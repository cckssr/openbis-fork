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

interface DeleteDialogConfig {
  includeReason: boolean;
  entityKind: string;
  dependentEntities: { experiments?: any[]; samples?: any[] } | null;
  numberOfEntities: number;
  bypassesTrashcan: boolean;
  inputValue?: string;
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

  const includeReason = config?.includeReason ?? true;
  const inputValue = config?.inputValue || '';
  const entityKind = config?.entityKind || 'entity';
  const numberOfEntities = config?.numberOfEntities || 0;
  const dependentEntities = config?.dependentEntities || null;
  const bypassesTrashcan = !!config?.bypassesTrashcan;

  const [value, setValue] = useState<string>(inputValue);

  useEffect(() => {
    setValue(inputValue || '');
  }, [inputValue]);

  const isReasonValid = useMemo(() => {
    return includeReason ? value.trim().length > 0 : true;
  }, [includeReason, value]);

  const getButtonType = () => 'risky';

  const renderWarningContent = () => {
    const totalDependentEntities = numberOfEntities || 0;
    if (totalDependentEntities > 0) {
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
    const count = numberOfEntities || 1;
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
    const count = numberOfEntities || 1;
    if (dependentEntities && count > 1) {
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
  if (warningContent) dialogContent.push(<React.Fragment key='warning-content'>{warningContent}</React.Fragment>);

  const infoText = renderInfoText();
  if (infoText) dialogContent.push(<React.Fragment key='info-text'>{infoText}</React.Fragment>);

  const additionalText = renderAdditionalText();
  if (additionalText) dialogContent.push(<React.Fragment key='additional-text'>{additionalText}</React.Fragment>);

  dialogContent.push(<DialogContentText key='dialog-content' />);

  if (includeReason) {
    dialogContent.push(
      <TextAreaField
        key="reason-to-delete-id"
        id="reason-to-delete-id"
        name={'Reason for the delete'}
        mandatory={true}
        label={'Reason for the delete'}
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
      title={'Confirm Delete'}
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
