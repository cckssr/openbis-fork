// SpaceFormRenderer - Space-specific form renderer with entity-specific initialization

import React, { useState, useEffect } from 'react';
import { EntityFormRendererProps, FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';
import { useFormStore } from '@src/js/components/database/new-forms-v2/core/stores/formStore.ts';
import { SpaceFormController } from '@src/js/components/database/new-forms-v2/entities/space/SpaceFormController.ts';
import { SpaceFormModel } from '@src/js/components/database/new-forms-v2/entities/space/SpaceFormModel.ts';
import { FormEngine } from '@src/js/components/database/new-forms-v2/components/FormEngine.tsx';
import { SpaceErrorDisplay } from '@src/js/components/database/new-forms-v2/entities/space/components/SpaceErrorDisplay.tsx';
import { SpaceLoadingSpinner } from '@src/js/components/database/new-forms-v2/entities/space/components/SpaceLoadingSpinner.tsx';
import { SpaceFormHeader } from '@src/js/components/database/new-forms-v2/entities/space/components/SpaceFormHeader.tsx';
import { SpaceFormFooter } from '@src/js/components/database/new-forms-v2/entities/space/components/SpaceFormFooter.tsx';

export const SpaceFormRenderer: React.FC<EntityFormRendererProps> = (props) => {
  const [formId] = useState(() => `space_${props.entityId}_${Date.now()}`);
  const [isInitialized, setIsInitialized] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Zustand store actions
  const createEntityForm = useFormStore(state => state.createEntityForm);
  const setEntityController = useFormStore(state => state.setEntityController);
  const setEntityMetadata = useFormStore(state => state.setEntityMetadata);
  const setEntityPermissions = useFormStore(state => state.setEntityPermissions);

  // Space-specific initialization
  useEffect(() => {
    const initializeSpaceForm = async () => {
      try {
        // 1. Create Space controller
        const controller = new SpaceFormController(props.openbisFacade, props.user);
        
        // 2. Initialize controller with Space-specific logic
        await controller.init(props.entityId, props.mode);
        
        // 3. Load Space data
        const data = await controller.load(props.entityId);
        
        // 4. Get Space form model and schema
        const formModel = new SpaceFormModel();
        const schema = await formModel.getSchema({
          entityType: 'SPACE',
          entityId: props.entityId,
          mode: props.mode,
          user: props.user,
          openbisFacade: props.openbisFacade,
        });

        // 5. Get Space metadata and permissions
        const metadata = controller.getSpaceMetadata();
        const permissions = controller.getSpacePermissions();

        // 6. Initialize Zustand store with Space-specific data
        createEntityForm({
          formId,
          entityType: 'SPACE',
          entityId: props.entityId,
          mode: props.mode,
          data,
          schema,
          controller,
          metadata,
          permissions: permissions || {
            canRead: true,
            canWrite: true,
            canCreate: true,
            canDelete: false,
            canAdmin: false,
          },
        });

        // 7. Store controller reference
        setEntityController(formId, controller);
        setEntityMetadata(formId, metadata);
        setEntityPermissions(formId, permissions || {
          canRead: true,
          canWrite: true,
          canCreate: true,
          canDelete: false,
          canAdmin: false,
        });
        
        setIsInitialized(true);
      } catch (err) {
        console.error('Space form initialization failed:', err);
        setError(err instanceof Error ? err.message : 'Space initialization failed');
      }
    };

    initializeSpaceForm();
  }, [formId, props.entityId, props.mode, props.user, props.openbisFacade]);

  // Handle Space-specific actions
  const handleNewProject = () => {
    // Navigate to new project form
    console.log('Creating new project in space:', props.entityId);
  };

  const handleSpaceSettings = () => {
    // Navigate to space settings
    console.log('Opening space settings for:', props.entityId);
  };

  if (error) {
    return (
      <SpaceErrorDisplay 
        error={error} 
        onRetry={() => window.location.reload()}
        spaceId={props.entityId}
        onCancel={props.onCancel}
      />
    );
  }

  if (!isInitialized) {
    return <SpaceLoadingSpinner spaceId={props.entityId} />;
  }

  // Render Space form with useFormEngine
  return (
    <div className="space-form-container">
      <SpaceFormHeader 
        spaceId={props.entityId}
        mode={props.mode}
        onNewProject={props.mode === FormMode.VIEW ? handleNewProject : undefined}
        onSettings={handleSpaceSettings}
      />
      
      <FormEngine
        formId={formId}
        onSave={props.onSave}
        onCancel={props.onCancel}
        onDelete={props.onDelete}
        // Space-specific props
        entityType="SPACE"
        showSpaceActions={props.mode === FormMode.VIEW}
      />
      
      <SpaceFormFooter 
        spaceId={props.entityId}
        mode={props.mode}
        onSave={props.onSave}
        onCancel={props.onCancel}
        onDelete={props.onDelete}
      />
    </div>
  );
};

export default SpaceFormRenderer;
