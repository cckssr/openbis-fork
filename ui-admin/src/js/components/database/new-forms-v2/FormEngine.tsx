import React, { Suspense } from 'react';
import { FormEngineConfig, FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';
import { useFormEngine } from '@src/js/components/database/new-forms-v2/core/useFormEngine.ts';
import { FormRenderer } from '@src/js/components/database/new-forms-v2/components/FormRenderer.tsx';
import { FormToolbar } from '@src/js/components/database/new-forms-v2/components/FormToolbar.tsx';
import { FormHeader } from '@src/js/components/database/new-forms-v2/components/FormHeader.tsx';
import { ErrorBoundary } from '@src/js/components/database/new-forms-v2/components/ErrorBoundary.tsx';
import { LoadingSpinner } from '@src/js/components/database/new-forms-v2/components/LoadingSpinner.tsx';

interface FormEngineProps extends FormEngineConfig {
  className?: string;
  style?: React.CSSProperties;
}

/**
 * Main FormEngine component
 * Renders any openBIS entity form through configuration
 */
export const FormEngine: React.FC<FormEngineProps> = ({
  entityType,
  entityId,
  mode,
  user,
  openbisFacade,
  onSave,
  onCancel,
  onDelete,
  className,
  style,
}) => {
  const formEngine = useFormEngine({
    entityType,
    entityId,
    mode,
    user,
    openbisFacade,
    onSave,
    onCancel,
    onDelete,
  });

  // Show loading spinner while initializing
  if (!formEngine.isInitialized) {
    return <LoadingSpinner message="Initializing form..." />;
  }

  // Show error if form failed to initialize
  if (formEngine.error) {
    return (
      <div className="form-engine-error">
        <h3>Form Error</h3>
        <p>{formEngine.error}</p>
        <button onClick={formEngine.handleCancel}>Close</button>
      </div>
    );
  }

  return (
    <ErrorBoundary>
      <div className={`form-engine ${className || ''}`} style={style}>
        <FormHeader
          title={formEngine.title}
          mode={formEngine.mode}
          isDirty={formEngine.isDirty}
          isValid={formEngine.isValid}
          onModeChange={formEngine.handleModeChange}
        />
        <FormToolbar
          mode={formEngine.mode}
          actions={formEngine.actions}
          context={formEngine.context}
          onAction={(actionId: string) => {
            // Use the centralized action handler
            formEngine.handleAction(actionId);
          }}
          isDirty={formEngine.isDirty}
          isValid={formEngine.isValid}
          isLoading={formEngine.isLoading}
          permissions={formEngine.permissions}
        />
        <Suspense fallback={<LoadingSpinner message="Loading form..." />}>
          <FormRenderer
            schema={formEngine.schema}
            data={formEngine.data}
            context={formEngine.context}
            onFieldChange={formEngine.handleFieldChange}
            getFieldRenderer={formEngine.getFieldRenderer}
            loadWidget={formEngine.loadWidget}
            isFieldVisible={formEngine.isFieldVisible}
            getFieldErrors={formEngine.getFieldErrors}
          />
        </Suspense>
      </div>
    </ErrorBoundary>
  );
};

export default FormEngine;
