import React, { useEffect, useMemo, useState } from 'react';
import withStyles from '@mui/styles/withStyles';
import { DialogContentText } from '@mui/material';
import Button from '@src/js/components/common/form/Button.jsx';
import Message from '@src/js/components/common/form/Message.jsx';
import Dialog from '@src/js/components/common/dialog/Dialog.jsx';
import messages from '@src/js/common/messages.js';
import logger from '@src/js/common/logger.js';
import TextAreaField from '@src/js/components/common/form/TextAreaField.jsx';
import CheckboxField from '@src/js/components/common/form/CheckboxField.jsx';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';

const styles = (theme: any) => ({
  button: {
    marginLeft: theme.spacing(1)
  }
});

interface DeleteDialogConfig {
  includeReason: boolean;
  entityKind: string;
  dependentEntities: { experiments?: any[]; samples?: any[]; datasets?: any[] } | null;
  rawDependentEntities?: any; // Original structure from controller (for Collection: { samples, datasets })
  numberOfEntities: number;
  bypassesTrashcan: boolean;
  inputValue?: string;
}

interface DeleteConfirmationDialogProps {
  open: boolean;
  onConfirm: (reason: string, includeDescendants?: boolean) => void;
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
  const rawDependentEntities = config?.rawDependentEntities || null;
  const bypassesTrashcan = !!config?.bypassesTrashcan;

  const [value, setValue] = useState<string>(inputValue);
  const [includeDescendants, setIncludeDescendants] = useState<boolean>(false);

  useEffect(() => {
    setValue(inputValue || '');
    setIncludeDescendants(false); // Reset checkbox when dialog opens
  }, [inputValue, open]);

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
    // Special handling for Collection entity type - show detailed list
    if (entityKind === EntityKind.COLLECTION && rawDependentEntities) {
      const samples = rawDependentEntities.samples || [];
      const datasets = rawDependentEntities.datasets || [];
      
      if (samples.length === 0 && datasets.length === 0) {
        return null;
      }
      
      const parts: React.ReactNode[] = [];
      
      // Show objects (samples)
      if (samples.length > 0) {
        const sampleCodes = samples.map((sample: any) => {
          try {
            return sample.getCode ? sample.getCode() : (sample.code || 'Unknown');
          } catch (e) {
            return sample.code || 'Unknown';
          }
        });
        
        parts.push(
          <React.Fragment key="samples-section">
            <br />
            The collection has {samples.length} object{samples.length > 1 ? 's' : ''}, which will also be deleted:
            <br />
            {sampleCodes.map((code: string, idx: number) => (
              <React.Fragment key={`sample-${idx}`}>
                {code}
                {idx < sampleCodes.length - 1 ? <br /> : null}
              </React.Fragment>
            ))}
            <br />
          </React.Fragment>
        );
      }
      
      // Show datasets
      if (datasets.length > 0) {
        const datasetCodes = datasets.map((dataset: any) => {
          try {
            return dataset.getCode ? dataset.getCode() : (dataset.code || 'Unknown');
          } catch (e) {
            return dataset.code || 'Unknown';
          }
        });
        
        parts.push(
          <React.Fragment key="datasets-section">
            <br />
            The collection has {datasets.length} data set{datasets.length > 1 ? 's' : ''} which will also be deleted:
            <br />
            {datasetCodes.map((code: string, idx: number) => (
              <React.Fragment key={`dataset-${idx}`}>
                {code}
                {idx < datasetCodes.length - 1 ? <br /> : null}
              </React.Fragment>
            ))}
            <br />
          </React.Fragment>
        );
      }
      
      return <>{parts}</>;
    }
    
    // Special handling for Object entity type - show datasets that will be moved
    if (entityKind === EntityKind.OBJECT && rawDependentEntities) {
      const datasets = rawDependentEntities.datasets || [];
      if (datasets.length > 0) {
        const datasetCodes = datasets.map((dataset: any) => {
          try {
            return dataset.getCode ? dataset.getCode() : (dataset.code || 'Unknown');
          } catch (e) {
            return dataset.code || 'Unknown';
          }
        });
        
        return (
          <React.Fragment>
            <br />
            The object has {datasets.length} data set{datasets.length > 1 ? 's' : ''} attached, which will also be moved to trashcan:
            <br />
            {datasetCodes.map((code: string, idx: number) => (
              <React.Fragment key={`object-dataset-${idx}`}>
                {code}
                {idx < datasetCodes.length - 1 ? <br /> : null}
              </React.Fragment>
            ))}
            <br />
          </React.Fragment>
        );
      }
    }
    
    // Default handling for other entity types
    const count = numberOfEntities || 1;
    if (dependentEntities && count > 1) {
      const experimentsCount = dependentEntities.experiments?.length || 0;
      const samplesCount = dependentEntities.samples?.length || 0;
      const datasetsCount = dependentEntities.datasets?.length || 0;
      let generatedAdditionalText = 'This action cannot be undone.';
      if (experimentsCount > 0 || samplesCount > 0 || datasetsCount > 0) {
        const parts: string[] = [];
        if (experimentsCount > 0) parts.push(`${experimentsCount} experiment${experimentsCount > 1 ? 's' : ''}`);
        if (samplesCount > 0) parts.push(`${samplesCount} sample${samplesCount > 1 ? 's' : ''}`);
        if (datasetsCount > 0) parts.push(`${datasetsCount} dataset${datasetsCount > 1 ? 's' : ''}`);
        generatedAdditionalText = `The following entities will be moved to trashcan: ${parts.join(', ')}.`;
      }
      return <><br />{generatedAdditionalText}</>;
    }
    return null;
  };

  const renderDescendantsCheckbox = () => {
    // Show descendants checkbox for Object and Dataset entity types
    const showCheckbox = entityKind === EntityKind.OBJECT || entityKind === EntityKind.DATASET;
    if (!showCheckbox) return null;

    // For Object: descendants are children (samples)
    // For Dataset: descendants are datasets
    let descendantsCount = 0;
    let labelText = '';
    
    if (entityKind === EntityKind.OBJECT) {
      descendantsCount = dependentEntities?.samples?.length || 0; // children normalized to samples
      if (descendantsCount === 0) return null;
      labelText = `Also trash descendant objects and their datasets (${descendantsCount} descendant${descendantsCount > 1 ? 's' : ''})`;
    } else if (entityKind === EntityKind.DATASET) {
      descendantsCount = dependentEntities?.datasets?.length || 0;
      if (descendantsCount === 0) return null;
      labelText = `Also trash descendant datasets (${descendantsCount} descendant${descendantsCount > 1 ? 's' : ''})`;
    }

    const CheckboxFieldAny = CheckboxField as any;
    return (
      <CheckboxFieldAny
        key="include-descendants-checkbox"
        name="includeDescendants"
        label={labelText}
        value={includeDescendants}
        onChange={(e: any) => setIncludeDescendants(e.target.value)}
        mode="edit"
        disabled={false}
      />
    );
  };

  const dialogContent: any[] = [];

  const warningContent = renderWarningContent();
  if (warningContent) dialogContent.push(<React.Fragment key='warning-content'>{warningContent}</React.Fragment>);
  const additionalText = renderAdditionalText();
  if (additionalText) dialogContent.push(<React.Fragment key='additional-text'>{additionalText}</React.Fragment>);

  const infoText = renderInfoText();
  if (infoText) dialogContent.push(<React.Fragment key='info-text'>{infoText}</React.Fragment>);

  
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
        styles={{}}
      />
    );
  }

  // Add descendants checkbox for Object and Dataset
  const descendantsCheckbox = renderDescendantsCheckbox();
  if (descendantsCheckbox) {
    dialogContent.push(<React.Fragment key='descendants-checkbox'>{descendantsCheckbox}</React.Fragment>);
  }

  const handleConfirmClick = () => {
    onConfirm(value, includeDescendants);
    if (!inputValue) setValue('');
    setIncludeDescendants(false);
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
