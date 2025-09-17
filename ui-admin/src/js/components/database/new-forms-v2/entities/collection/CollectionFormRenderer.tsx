// CollectionFormRenderer - Collection-specific form renderer (placeholder)

import React from 'react';
import { EntityFormRendererProps } from '../../core/types';

export const CollectionFormRenderer: React.FC<EntityFormRendererProps> = (props) => {
  return (
    <div className="collection-form-container">
      <h2>Collection Form Renderer</h2>
      <p>Collection form for entity: {props.entityId}</p>
      <p>Mode: {props.mode}</p>
      <p>This is a placeholder implementation.</p>
    </div>
  );
};

export default CollectionFormRenderer;
