import React from 'react';
import { FormEngine } from '@src/js/components/database/new-forms-v2/FormEngine.tsx';
import { FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

// Mock openBIS facade for demonstration
const mockOpenbisFacade = {
  // Mock openBIS API methods
  getProjects: async (ids: any[], options: any) => {
    console.log('Mock getProjects called with:', ids, options);
    return {
      'PROJECT_1': {
        permId: { permId: 'PROJECT_1' },
        code: 'DEMO_PROJECT',
        description: 'This is a demo project',
        identifier: { identifier: '/DEMO_SPACE/DEMO_PROJECT' },
        space: { code: 'DEMO_SPACE' },
        registrator: { userId: 'admin' },
        registrationDate: new Date().toISOString(),
        modifier: 'admin',
        modificationDate: new Date().toISOString(),
      }
    };
  },
  createProjects: async (creations: any[]) => {
    console.log('Mock createProjects called with:', creations);
    return [{ getPermId: () => 'NEW_PROJECT_123' }];
  },
  updateProjects: async (updates: any[]) => {
    console.log('Mock updateProjects called with:', updates);
    return [{ getPermId: () => 'PROJECT_1' }];
  },
  deleteProjects: async (identifiers: any[], options: any) => {
    console.log('Mock deleteProjects called with:', identifiers, options);
    return [];
  },
  // Mock constructors
  ProjectPermId: function(id: string) { this.id = id; },
  ProjectFetchOptions: function() { 
    this.withSpace = () => this; 
  },
  ProjectCreation: function() {
    this.setCode = (code: string) => { this.code = code; };
    this.setSpaceId = (spaceId: any) => { this.spaceId = spaceId; };
    this.setDescription = (desc: string) => { this.description = desc; };
  },
  ProjectUpdate: function() {
    this.setProjectId = (id: any) => { this.projectId = id; };
    this.setDescription = (desc: string) => { this.description = desc; };
  },
  ProjectIdentifier: function(id: string) { this.id = id; },
  ProjectDeletionOptions: function() {
    this.setReason = (reason: string) => { this.reason = reason; };
  },
  SpacePermId: function(id: string) { this.id = id; },
};

// Mock user
const mockUser = {
  userId: 'admin',
  name: 'Administrator',
  email: 'admin@example.com',
};

interface ProjectFormExampleProps {
  projectId?: string;
  mode?: FormMode;
  onSave?: (result: any) => void;
  onDelete?: (projectId: string) => void;
  onNewProject?: (projectFormId: string) => void;
  onCancel?: () => void;
}

export const ProjectFormExample: React.FC<ProjectFormExampleProps> = ({
  projectId = 'PROJECT_1',
  mode = FormMode.VIEW,
  onSave,
  onDelete,
  onNewProject,
  onCancel,
}) => {
  const handleSave = (result: any) => {
    console.log('Project saved:', result);
    if (onSave) {
      onSave(result);
    }
  };

  const handleDelete = (id: string) => {
    console.log('Project deleted:', id);
    if (onDelete) {
      onDelete(id);
    }
  };

  const handleNewProject = (projectFormId: string) => {
    console.log('New project form created:', projectFormId);
    if (onNewProject) {
      onNewProject(projectFormId);
    }
  };

  const handleCancel = () => {
    console.log('Form cancelled');
    if (onCancel) {
      onCancel();
    }
  };

  return (
    <div className="project-form-example">
      <h2>Project Form Example</h2>
      <p>Project ID: {projectId}</p>
      <p>Mode: {mode}</p>
      
      <FormEngine
        entityType="PROJECT"
        entityId={projectId}
        mode={mode}
        user={mockUser}
        openbisFacade={mockOpenbisFacade}
        onSave={handleSave}
        onDelete={handleDelete}
        onNewProject={handleNewProject}
        onCancel={handleCancel}
      />
    </div>
  );
};

export default ProjectFormExample;
