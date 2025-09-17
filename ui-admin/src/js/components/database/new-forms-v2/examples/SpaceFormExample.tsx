import React from 'react';
import { FormEngine } from '@src/js/components/database/new-forms-v2/FormEngine.tsx';
import { FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';
import { SpaceFormModel } from '../entities/space/SpaceFormModel';
import { SpaceFormController } from '../entities/space/SpaceFormController';

interface SpaceFormExampleProps {
  openbisFacade: any;
  user: any;
  spaceId?: string;
  mode?: FormMode;
  onNewProject?: (projectFormId: string) => void;
}

export const SpaceFormExample: React.FC<SpaceFormExampleProps> = ({
  openbisFacade,
  user,
  spaceId = 'DEFAULT',
  mode = FormMode.VIEW,
  onNewProject,
}) => {
  const handleSave = async (data: any) => {
    console.log('Saving Space data:', data);
    // This would be handled by the SpaceFormController
  };

  const handleCancel = () => {
    console.log('Canceling Space form');
    // This would close the form or navigate away
  };

  const handleDelete = async (entityId: string) => {
    console.log('Deleting Space:', entityId);
    // This would be handled by the SpaceFormController
  };

  const handleNewProject = (projectFormId: string) => {
    console.log('New project form created:', projectFormId);
    if (onNewProject) {
      onNewProject(projectFormId);
    }
  };

  return (
      <FormEngine
        entityType="SPACE"
        entityId={spaceId}
        mode={mode}
        user={user}
        openbisFacade={openbisFacade}
        onSave={handleSave}
        onCancel={handleCancel}
        onDelete={handleDelete}
        onNewProject={handleNewProject}
        className="space-form"
        style={{ margin: '0 auto' }}
      />
  );
};

export default SpaceFormExample;
