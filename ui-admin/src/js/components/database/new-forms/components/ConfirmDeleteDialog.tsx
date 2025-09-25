import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  Box,
  Alert
} from '@mui/material';
import { makeStyles } from '@mui/styles';
import messages from '@src/js/common/messages.js';

const useStyles = makeStyles((theme) => ({
  dialog: {
    '& .MuiDialog-paper': {
      minWidth: '500px',
      maxWidth: '600px',
    }
  },
  content: {
    padding: '20px 24px',
  },
  warningText: {
    color: '#ed6c02', // warning color
    marginBottom: '16px',
  },
  infoText: {
    marginBottom: '16px',
    color: 'rgba(0, 0, 0, 0.6)', // secondary text color
  },
  reasonField: {
    marginTop: '16px',
  },
  button: {
    marginLeft: '8px',
  },
  legend: {
    fontSize: '1.2rem',
    fontWeight: 'bold',
    marginBottom: '16px',
  }
}));

interface ConfirmDeleteDialogProps {
  open: boolean;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
  warningText?: string;
  includeReason?: boolean;
  numberOfEntities?: number;
  bypassesTrashcan?: boolean;
  additionalText?: string;
  customPlugin?: React.ReactNode;
}

const ConfirmDeleteDialog: React.FC<ConfirmDeleteDialogProps> = ({
  open,
  onConfirm,
  onCancel,
  warningText,
  includeReason = true,
  numberOfEntities = 1,
  bypassesTrashcan = false,
  additionalText,
  customPlugin
}) => {
  const classes = useStyles();
  const [reason, setReason] = useState('');

  const handleConfirm = () => {
    if (reason.trim()) {
      onConfirm(reason);
      setReason(''); // Reset reason after confirmation
    }
  };

  const isReasonValid = () => {
    return includeReason ? reason.trim().length > 0 : true;
  };

  const handleCancel = () => {
    onCancel();
    setReason(''); // Reset reason when canceling
  };

  const handleReasonChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setReason(event.target.value);
  };

  const renderWarningContent = () => {
    if (customPlugin) {
      return customPlugin;
    } else if (warningText) {
      return (
        <Alert severity="warning" className={classes.warningText}>
          {warningText}
        </Alert>
      );
    }
    return null;
  };

  const renderInfoText = () => {
    if (!includeReason) return null;

    let infoText = "By providing a reason for deletion and clicking 'Accept', ";
    infoText += numberOfEntities > 1 ? "these entities" : "this entity";

    if (bypassesTrashcan) {
      infoText += " will be deleted immediately.";
    } else {
      infoText += " will be moved to the Trashcan.";
    }

    return (
      <Typography className={classes.infoText}>
        {infoText}
      </Typography>
    );
  };

  const renderAdditionalText = () => {
    if (additionalText) {
      return (
        <Typography className={classes.infoText}>
          {additionalText}
        </Typography>
      );
    }
    return null;
  };

  const renderReasonField = () => {
    if (!includeReason) return null;

    return (
      <TextField
        id="reason-to-delete-id"
        label="Reason for the delete"
        value={reason}
        onChange={handleReasonChange}
        fullWidth
        multiline
        rows={3}
        variant="outlined"
        className={classes.reasonField}
        placeholder="Please provide a reason for deletion..."
        required
        error={includeReason && reason.length > 0 && !reason.trim()}
        helperText={includeReason && reason.length > 0 && !reason.trim() ? "Reason cannot be empty or just whitespace" : ""}
      />
    );
  };

  return (
    <Dialog
      open={open}
      onClose={handleCancel}
      className={classes.dialog}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle>
        <Typography className={classes.legend}>
          Confirm Delete
        </Typography>
      </DialogTitle>
      
      <DialogContent className={classes.content}>
        {renderWarningContent()}
        {renderInfoText()}
        {renderAdditionalText()}
        {renderReasonField()}
      </DialogContent>

      <DialogActions>
        <Button
          onClick={handleCancel}
          variant="outlined"
          color="inherit"
        >
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          color="error"
          className={classes.button}
          id="accept-btn"
          disabled={!isReasonValid()}
        >
          Accept
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ConfirmDeleteDialog;
