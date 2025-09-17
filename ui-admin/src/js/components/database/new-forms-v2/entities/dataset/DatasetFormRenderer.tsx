// DatasetFormRenderer - Dataset-specific form renderer (placeholder)

import React from 'react';
import { EntityFormRendererProps } from '../../core/types';

export const DatasetFormRenderer: React.FC<EntityFormRendererProps> = (props) => {
  return (
    <div className="dataset-form-container">
      <h2>Dataset Form Renderer</h2>
      <p>Dataset form for entity: {props.entityId}</p>
      <p>Mode: {props.mode}</p>
      <p>This is a placeholder implementation.</p>
    </div>
  );
};

export default DatasetFormRenderer;
