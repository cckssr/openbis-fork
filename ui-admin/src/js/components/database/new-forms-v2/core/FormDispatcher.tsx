// FormDispatcher - Router layer for New Forms V2
// Routes to appropriate entity-specific renderers based on entity type

import React from 'react';
import { FormDispatcherProps } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

// Import entity-specific renderers
import { SpaceFormRenderer } from '@src/js/components/database/new-forms-v2/entities/space/SpaceFormRenderer.tsx';
import { ProjectFormRenderer } from '@src/js/components/database/new-forms-v2/entities/project/ProjectFormRenderer.tsx';
import { CollectionFormRenderer } from '@src/js/components/database/new-forms-v2/entities/collection/CollectionFormRenderer.tsx';
import { DatasetFormRenderer } from '@src/js/components/database/new-forms-v2/entities/dataset/DatasetFormRenderer.tsx';
import { UnsupportedEntityRenderer } from '@src/js/components/database/new-forms-v2/components/common/UnsupportedEntityRenderer.tsx';

/**
 * FormDispatcher routes to the appropriate entity-specific form renderer
 * based on the entity type. This provides clean separation of concerns
 * and allows each entity to have its own initialization and UI logic.
 */
export const FormDispatcher: React.FC<FormDispatcherProps> = (props) => {
  const { entityType } = props;
  
  // Normalize entity type to uppercase for consistent routing
  const normalizedEntityType = entityType.toUpperCase();
  
  // Route to appropriate entity-specific renderer
  switch (normalizedEntityType) {
    case 'SPACE':
      return <SpaceFormRenderer {...props} />;
      
    case 'PROJECT':
    case 'NEWPROJECT':
      return <ProjectFormRenderer {...props} />;
      
    case 'COLLECTION':
    case 'EXPERIMENT':
      return <CollectionFormRenderer {...props} />;
      
    case 'DATASET':
    case 'SAMPLE':
      return <DatasetFormRenderer {...props} />;
      
    default:
      return (
        <UnsupportedEntityRenderer 
          entityType={entityType}
          onCancel={props.onCancel}
        />
      );
  }
};

export default FormDispatcher;
