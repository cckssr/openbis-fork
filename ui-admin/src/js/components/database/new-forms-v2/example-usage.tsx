// Example usage of New Forms V2 - Three-Layer Architecture

import React from 'react';
import { FormDispatcher } from './core/FormDispatcher';
import { FormMode } from './core/types';

// Example component showing how to use the new form system
export const ExampleFormUsage: React.FC = () => {
  // Mock openBIS facade and user
  const openbisFacade = {
    getSpace: (id: string) => Promise.resolve({ code: id, description: 'Test space' }),
    createSpace: (data: any) => Promise.resolve(data),
    updateSpace: (data: any) => Promise.resolve(data),
    deleteSpace: (id: string) => Promise.resolve(),
    getSpacePermissions: (id: string) => Promise.resolve({
      canRead: true,
      canWrite: true,
      canCreate: true,
      canDelete: false,
      canAdmin: false,
    }),
    getSpaceMetadata: (id: string) => Promise.resolve({ id, type: 'SPACE' }),
    getSpaceProjects: (id: string) => Promise.resolve([]),
    getSpaceSpaces: (id: string) => Promise.resolve([]),
    getSpaceHistory: (id: string) => Promise.resolve([]),
  };

  const user = {
    id: 'user1',
    name: 'Test User',
    permissions: ['READ', 'WRITE', 'CREATE'],
  };

  // Example callbacks
  const handleSave = (result: any) => {
    console.log('Form saved:', result);
  };

  const handleCancel = () => {
    console.log('Form cancelled');
  };

  const handleDelete = (entityId: string) => {
    console.log('Entity deleted:', entityId);
  };

  return (
    <div className="example-forms">
      <h1>New Forms V2 - Three-Layer Architecture Examples</h1>
      
      {/* Space Form Examples */}
      <div className="form-examples">
        <h2>Space Forms</h2>
        
        {/* Create Space */}
        <div className="form-example">
          <h3>Create New Space</h3>
          <FormDispatcher
            entityType="SPACE"
            entityId="NEW"
            mode={FormMode.CREATE}
            user={user}
            openbisFacade={openbisFacade}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        </div>

        {/* Edit Space */}
        <div className="form-example">
          <h3>Edit Existing Space</h3>
          <FormDispatcher
            entityType="SPACE"
            entityId="MY_SPACE"
            mode={FormMode.EDIT}
            user={user}
            openbisFacade={openbisFacade}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        </div>

        {/* View Space */}
        <div className="form-example">
          <h3>View Space</h3>
          <FormDispatcher
            entityType="SPACE"
            entityId="MY_SPACE"
            mode={FormMode.VIEW}
            user={user}
            openbisFacade={openbisFacade}
            onSave={handleSave}
            onCancel={handleCancel}
            onDelete={handleDelete}
          />
        </div>
      </div>

      {/* Project Form Examples */}
      <div className="form-examples">
        <h2>Project Forms</h2>
        
        <div className="form-example">
          <h3>Create New Project</h3>
          <FormDispatcher
            entityType="PROJECT"
            entityId="NEW"
            mode={FormMode.CREATE}
            user={user}
            openbisFacade={openbisFacade}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        </div>
      </div>

      {/* Collection Form Examples */}
      <div className="form-examples">
        <h2>Collection Forms</h2>
        
        <div className="form-example">
          <h3>Create New Collection</h3>
          <FormDispatcher
            entityType="COLLECTION"
            entityId="NEW"
            mode={FormMode.CREATE}
            user={user}
            openbisFacade={openbisFacade}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        </div>
      </div>

      {/* Dataset Form Examples */}
      <div className="form-examples">
        <h2>Dataset Forms</h2>
        
        <div className="form-example">
          <h3>Create New Dataset</h3>
          <FormDispatcher
            entityType="DATASET"
            entityId="NEW"
            mode={FormMode.CREATE}
            user={user}
            openbisFacade={openbisFacade}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        </div>
      </div>

      {/* Unsupported Entity Example */}
      <div className="form-examples">
        <h2>Unsupported Entity</h2>
        
        <div className="form-example">
          <h3>Unsupported Entity Type</h3>
          <FormDispatcher
            entityType="UNSUPPORTED"
            entityId="TEST"
            mode={FormMode.VIEW}
            user={user}
            openbisFacade={openbisFacade}
            onSave={handleSave}
            onCancel={handleCancel}
          />
        </div>
      </div>
    </div>
  );
};

export default ExampleFormUsage;
