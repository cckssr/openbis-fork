import React, { useState } from 'react';
import { SpaceFormExample } from '@src/js/components/database/new-forms-v2/examples/SpaceFormExample.tsx';
import { ProjectFormExample } from '@src/js/components/database/new-forms-v2/examples/ProjectFormExample.tsx';
import { FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

// Mock openBIS facade for demonstration
const mockOpenbisFacade = {
  // Space methods
  getSpaces: async (ids: any[], options: any) => {
    console.log('Mock getSpaces called with:', ids, options);
    return {
      'DEMO_SPACE': {
        permId: { permId: 'DEMO_SPACE' },
        code: 'DEMO_SPACE',
        description: 'This is a demo space',
        registrator: { userId: 'admin' },
        registrationDate: new Date().toISOString(),
        modifier: 'admin',
        modificationDate: new Date().toISOString(),
      }
    };
  },
  createSpaces: async (creations: any[]) => {
    console.log('Mock createSpaces called with:', creations);
    return [{ getPermId: () => 'NEW_SPACE_123' }];
  },
  updateSpaces: async (updates: any[]) => {
    console.log('Mock updateSpaces called with:', updates);
    return [{ getPermId: () => 'DEMO_SPACE' }];
  },
  deleteSpaces: async (identifiers: any[], options: any) => {
    console.log('Mock deleteSpaces called with:', identifiers, options);
    return [];
  },
  // Project methods
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
  SpacePermId: function(id: string) { this.id = id; },
  SpaceFetchOptions: function() { 
    this.withProjects = () => this;
    this.withRegistrator = () => this;
    this.withSamples = () => this;
  },
  SpaceCreation: function() {
    this.setCode = (code: string) => { this.code = code; };
    this.setDescription = (desc: string) => { this.description = desc; };
  },
  SpaceUpdate: function() {
    this.setSpaceId = (id: any) => { this.spaceId = id; };
    this.setDescription = (desc: string) => { this.description = desc; };
  },
  SpaceIdentifier: function(id: string) { this.id = id; },
  SpaceDeletionOptions: function() {
    this.setReason = (reason: string) => { this.reason = reason; };
  },
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
};

// Mock user
const mockUser = {
  userId: 'admin',
  name: 'Administrator',
  email: 'admin@example.com',
};

export const IntegrationExample: React.FC = () => {
  const [activeForm, setActiveForm] = useState<'space' | 'project' | null>('space');
  const [spaceId] = useState('DEMO_SPACE');
  const [projectId, setProjectId] = useState('PROJECT_1');
  const [mode, setMode] = useState<FormMode>(FormMode.VIEW);

  const handleSpaceSave = (result: any) => {
    console.log('Space saved:', result);
    // Handle space save
  };

  const handleSpaceDelete = (id: string) => {
    console.log('Space deleted:', id);
    // Handle space delete
  };

  const handleSpaceNewProject = (projectFormId: string) => {
    console.log('New project form created from space:', projectFormId);
    setActiveForm('project');
    setProjectId('NEW_PROJECT');
    setMode(FormMode.CREATE);
  };

  const handleProjectSave = (result: any) => {
    console.log('Project saved:', result);
    if (projectId === 'NEW_PROJECT') {
      // New project was created, switch to view mode
      setProjectId(result);
      setMode(FormMode.VIEW);
    }
  };

  const handleProjectDelete = (id: string) => {
    console.log('Project deleted:', id);
    // Switch back to space view
    setActiveForm('space');
    setMode(FormMode.VIEW);
  };

  const handleCancel = () => {
    console.log('Form cancelled');
    if (mode === FormMode.CREATE) {
      // If cancelling a new project, go back to space view
      setActiveForm('space');
      setMode(FormMode.VIEW);
    }
  };

  const switchToSpace = () => {
    setActiveForm('space');
    setMode(FormMode.VIEW);
  };

  const switchToProject = () => {
    setActiveForm('project');
    setMode(FormMode.VIEW);
  };

  const switchToEditMode = () => {
    setMode(FormMode.EDIT);
  };

  const switchToViewMode = () => {
    setMode(FormMode.VIEW);
  };

  return (
    <div className="integration-example">
      <h1>Space and Project Forms Integration Example</h1>
      
      <div className="controls" style={{ marginBottom: '20px', padding: '10px', border: '1px solid #ccc' }}>
        <h3>Controls</h3>
        <button onClick={switchToSpace} disabled={activeForm === 'space'}>
          View Space
        </button>
        <button onClick={switchToProject} disabled={activeForm === 'project'}>
          View Project
        </button>
        <button onClick={switchToEditMode} disabled={mode === FormMode.EDIT}>
          Edit Mode
        </button>
        <button onClick={switchToViewMode} disabled={mode === FormMode.VIEW}>
          View Mode
        </button>
      </div>

      <div className="form-container" style={{ border: '1px solid #ddd', padding: '20px' }}>
        {activeForm === 'space' && (
          <div>
            <h2>Space Form</h2>
            <SpaceFormExample
              openbisFacade={mockOpenbisFacade}
              user={mockUser}
              spaceId={spaceId}
              mode={mode}
              onNewProject={handleSpaceNewProject}
            />
          </div>
        )}

        {activeForm === 'project' && (
          <div>
            <h2>Project Form</h2>
            <ProjectFormExample
              projectId={projectId}
              mode={mode}
              onSave={handleProjectSave}
              onDelete={handleProjectDelete}
              onCancel={handleCancel}
            />
          </div>
        )}
      </div>

      <div className="info" style={{ marginTop: '20px', padding: '10px', backgroundColor: '#f5f5f5' }}>
        <h3>Instructions</h3>
        <ul>
          <li>Use the "View Space" and "View Project" buttons to switch between forms</li>
          <li>Use "Edit Mode" to enable editing of the current form</li>
          <li>In Space view, click the "+ Project" button to create a new project</li>
          <li>In Project view, use the Save, Edit, Cancel, and Delete buttons</li>
          <li>All actions are logged to the console for demonstration</li>
        </ul>
      </div>
    </div>
  );
};

export default IntegrationExample;
