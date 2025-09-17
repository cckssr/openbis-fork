// ProjectFormRenderer - Project-specific form renderer (placeholder)

import React from 'react';
import { EntityFormRendererProps } from '../../core/types';

export const ProjectFormRenderer: React.FC<EntityFormRendererProps> = (props) => {
  return (
    <div className="project-form-container">
      <h2>Project Form Renderer</h2>
      <p>Project form for entity: {props.entityId}</p>
      <p>Mode: {props.mode}</p>
      <p>This is a placeholder implementation.</p>
    </div>
  );
};

export default ProjectFormRenderer;
