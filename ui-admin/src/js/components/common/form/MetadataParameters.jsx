import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Container from '@src/js/components/common/form/Container.jsx'
import Header from '@src/js/components/common/form/Header.jsx'
import messages from '@src/js/common/messages.js'
import TextField from '@src/js/components/common/form/TextField.jsx'
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import Button from '@src/js/components/common/form/Button.jsx';
import { Typography } from '@mui/material';
import AppController from '@src/js/components/AppController.js'
import Message from '@src/js/components/common/form/Message.jsx'

const styles = theme => ({
	metadataField: {
		paddingBottom: theme.spacing(1)
	},
	headerContainer: {
		display: 'flex',
		flexDirection: 'row',
		alignItems: 'center',
		justifyContent: 'space-between'
	},
	metadataFieldsWrapper: {
		flexGrow: 1,
		marginRight: theme.spacing(1),	
	},
	removeButton: {
		minWidth: 'auto',
		padding: theme.spacing(0.5),
	},
	metadataItem: {
		border: '2px solid #ebebeb',
		padding: theme.spacing(1),
		display: 'flex',
		flexDirection: 'row',
		alignItems: 'center',
		marginBottom: theme.spacing(1),
	},
	metadataItemEdit: {
		border: '2px solid #ebebeb',
		padding: theme.spacing(1),
		display: 'flex',
		flexDirection: 'row',
		alignItems: 'center',
		marginBottom: theme.spacing(1),
		borderRight: 'unset',
		backgroundColor: theme.palette.grey[50],
	},
});

const MetadataParameters = ({
  title,
  metadataArray = [],
  enabled = true,
  visible = true,
  mode,
  onAddMetadata,
  onRemoveMetadata,
  onMetadataFieldChange,
  onFocus,
  onBlur,
  showAddButton = true,
  classes
}) => {

  const renderHeader = () => (
    <div className={classes.headerContainer}>
      <Header>{title}</Header>
      {enabled && mode === 'edit' && showAddButton && (
        <Button 
          variant='contained'
          color='white'
          onClick={onAddMetadata}
          label={<AddIcon />}
          sx={{
			padding: '4px',
		  marginRight: '8px' }} 
        />
      )}
    </div>
  );

  const renderMessageInternal = (isEnabled = false) => {

    if (!isEnabled) {
      if (AppController.getInstance().isSystemUser()) {
        return (
          <div className={classes.metadataField}>
            <Message type='lock'>
              {messages.get(messages.META_DATA_IS_INTERNAL)}
            </Message>
          </div>
        )
      } else {
        return (
          <div className={classes.metadataField}>
            <Message type='lock'>
              {messages.get(messages.META_DATA_IS_INTERNAL)}{' '}
              {messages.get(
                messages.META_DATA_CANNOT_BE_CHANGED_OR_REMOVED
              )}
            </Message>
          </div>
        )
      }
    } else {
      return null
    }
  }

  const renderMetadataItem = (metadata, index) => {
    const { key, value, action } = metadata;
    const isKeyEditable = action === 'CREATE';

    return (
      <div key={`metadata-${index}`} className={enabled && mode === 'edit' ? classes.metadataItemEdit : classes.metadataItem}>
        <div className={classes.metadataFieldsWrapper}>
          <div className={classes.metadataField}>
            <TextField
              label='Key'
              name={`metadata-key-${index}`}
              value={key || ''}
              mode={enabled ? mode : 'view'}
              disabled={!isKeyEditable}
              onChange={(event) => onMetadataFieldChange(index, 'key', event.target.value)}
              onFocus={onFocus}
              onBlur={onBlur}
            />
          </div>
          <div className={classes.metadataField}>
            <TextField
              label='Value'
              name={`metadata-value-${index}`}
              value={value || ''}
              mode={enabled ? mode : 'view'}
              onChange={(event) => onMetadataFieldChange(index, 'value', event.target.value)}
              onFocus={onFocus}
              onBlur={onBlur}
            />
          </div>
        </div>
        {enabled && mode === 'edit' && (
          <Button
            variant='contained'
            color='white'
            onClick={() => onRemoveMetadata(index)}
            label={<RemoveIcon />}
            aria-label={`Remove metadata item ${index + 1}`}
            tooltip={messages.get(messages.REMOVE)}
            sx={{
              padding: '4px',
            }}
          />
        )}
      </div>
    );
  };

  const renderMetadata = () => {
    if (metadataArray.length === 0) {
      return (
        <Typography variant="body2" color="textSecondary">
          {messages.get(messages.NO_METADATA_DEFINED)}
        </Typography>
      );
    }

    return metadataArray.map((metadata, index) => renderMetadataItem(metadata, index));
  };

  return (
    <Container>
      {renderHeader()}
      {renderMessageInternal(enabled)}
      {renderMetadata()}
    </Container>
  );
};

export default withStyles(styles)(MetadataParameters);