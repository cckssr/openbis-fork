import React from 'react';
import withStyles from '@mui/styles/withStyles';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import Button from '@src/js/components/common/form/Button.jsx';

const styles = (theme: any) => ({
  button: {
    marginLeft: theme.spacing(1)
  }
});

interface RestoreDraftDialogProps {
  open: boolean;
  onRestore: () => void;
  onDiscard: () => void;
  onDismiss: () => void;
  classes?: any;
}

const RestoreDraftDialog = ({ open, onRestore, onDiscard, onDismiss, classes }: RestoreDraftDialogProps) => {
  return (
    <Dialog
      open={open}
      onClose={onDismiss}
      title={'Unsaved Draft Found'}
      content={
        'An auto-saved draft was found for this type of entity. Do you want to restore it, or discard it?'
      }
      actions={(
        <>
          <Button
            name='restore-draft'
            label={'Restore Draft'}
            styles={{ root: classes.button }}
            onClick={onRestore}
          />
          <Button
            name='discard-draft'
            label={'Discard Draft'}
            type={'risky'}
            styles={{ root: classes.button }}
            onClick={onDiscard}
          />
        </>
      )}
    />
  );
};

export default withStyles(styles)(RestoreDraftDialog);
