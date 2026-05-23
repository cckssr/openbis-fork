# Imaging Module

This module provides imaging dataset viewing, gallery management, and export functionality for the openBIS system.

## Quick Start

```javascript
import ImagingFacade from '@src/js/components/common/imaging/ImagingFacade.js';

// Initialize facade
const facade = new ImagingFacade(extOpenbis);

// Load paginated gallery datasets
const { previewContainerList, totalCount } = await facade.loadPaginatedGalleryDatasets(
    objId, 
    ObjectType.COLLECTION, 
    0,  // page
    8   // pageSize
);
```

## Architecture

The imaging module follows a **facade pattern** that provides a simplified API for React components:

```
React Components → ImagingFacade → ImagingMapper → openBIS API
```

### Core Components

1. **ImagingFacade** (`ImagingFacade.js`)
   - Main entry point for all imaging operations
   - Provides unified API for data access, CRUD operations, filtering, and exports
   - Handles all interactions with the openBIS API
   - Uses `ImagingMapper` for data transformation

2. **ImagingMapper** (`ImagingMapper.js`)
   - Transforms data between domain objects and openBIS API objects
   - Handles preview, export, and update parameter mapping
   - Creates API-specific objects from domain data

3. **React Components**
   - `ImagingGalleryViewer` - Gallery view for multiple datasets
   - `ImagingDatasetViewer` - Single dataset viewer/editor
   - Various sub-components in `components/` directory

## Key Classes

### ImagingFacade

Main facade class providing methods for:

- **Vocabulary Operations**: Loading vocabulary terms and dataset types
- **Dataset Operations**: Loading, saving, updating imaging datasets
- **Preview Management**: Updating previews, creating SXM previews
- **Gallery Operations**: Pagination, filtering, loading gallery datasets
- **Export Operations**: Single and multi-export functionality

All methods are documented with JSDoc comments for IDE support.

### ImagingMapper

Mapper class for data transformation:

- `getImagingDataSetPreview()` - Creates preview objects from parameters
- `mapToImagingDataSetPreview()` - Maps preview objects to API format
- `mapToImagingUpdateParams()` - Maps update parameters
- `mapToImagingExportParams()` - Maps export parameters
- `mapToImagingMultiExportParams()` - Maps multi-export parameters

## File Structure

```
imaging/
├── README.md                    # This file
│
├── ImagingFacade.js             # Main facade (public API)
├── ImagingMapper.js             # Data transformation mapper
├── ImagingGalleryViewer.jsx     # Gallery React component
├── ImagingDatasetViewer.jsx     # Dataset React component
│
├── components/                  # React components
│   ├── common/                  # Common UI components
│   │   ├── AlertDialog.jsx
│   │   ├── BlankImage.js
│   │   ├── CustomSwitch.jsx
│   │   ├── Dropdown.jsx
│   │   ├── ImageListItemBarAction.js
│   │   ├── ImageListItemSection.js
│   │   ├── InputRangeSlider.jsx
│   │   ├── InputSlider.jsx
│   │   └── Player.jsx
│   │
│   ├── gallery/                 # Gallery view components
│   │   ├── DefaultMetadataField.js
│   │   ├── EditableMetadataField.jsx
│   │   ├── GalleryControlsBar.js
│   │   ├── GalleryFilter.jsx
│   │   ├── GalleryGridView.js
│   │   ├── GalleryListView.js
│   │   └── GalleryPaging.jsx
│   │
│   └── viewer/                  # Dataset viewer components
│       ├── CollapsableSection.jsx
│       ├── Exporter.jsx
│       ├── FilterSelector.jsx
│       ├── ImageSection.js
│       ├── ImagingDataContext.jsx
│       ├── ImagingDatasetViewerContainer.jsx
│       ├── InfoOntology.js
│       ├── InputControlsSection.js
│       ├── InputFileUpload.js
│       ├── MainPreview.js
│       ├── MainPreviewInputControls.js
│       ├── MetadataSection.js
│       └── PreviewSection.jsx
│
├── constants.js                  # Module constants
├── utils.js                      # Utility functions
└── dataHandlers.js               # Data loading helpers
```

## Common Operations

### Load Gallery Datasets

```javascript
const facade = new ImagingFacade(extOpenbis);
const result = await facade.loadPaginatedGalleryDatasets(objId, objType, page, pageSize);
// Returns: { previewContainerList: [...], totalCount: 42 }
```

### Filter Gallery

```javascript
const result = await facade.filterGallery(
    objId,
    objType,
    'AND',           // operator: 'AND' or 'OR'
    'search text',   // filterText
    'IMAGING_TAGS',  // property: 'IMAGING_TAGS', 'PREVIEW_COMMENT', or dataset property
    page,
    pageSize
);
```

### Load Single Dataset

```javascript
// Get just the config
const config = await facade.loadImagingDataset(permId);

// Get config with type and file paths
const [paths, type, config] = await facade.loadImagingDataset(permId, false, true, true);

// Get raw properties
const properties = await facade.loadImagingDataset(permId, true);
```

### Update Preview

```javascript
await facade.updatePreview(permId, imageIdx, preview);
```

### Save Dataset

```javascript
await facade.saveImagingDataset(permId, imagingDataset);
```

### Export Dataset

```javascript
// Single export
const url = await facade.exportImagingDataset(objId, activeImageIdx, exportConfig, metadata);
window.open(url, '_blank');

// Multi export
const url = await facade.multiExportImagingDataset(exportConfig, exportList);
window.open(url, '_blank');
```

### Load Vocabulary Terms

```javascript
const terms = await facade.loadImagingVocabularyTerms('IMAGING_TAGS');
// Returns: [{label: 'Tag1', value: 'TAG1'}, {label: 'Tag2', value: 'TAG2'}]
```

## Constants

Key constants are defined in `constants.js`:

- `IMAGING_DATA` - Imaging dataset type code
- `USER_DEFINED_IMAGING_DATA` - User-defined imaging dataset type code
- `IMAGING_DATA_CONFIG` - Property name for imaging config JSON
- `IMAGING_NOTES` - Property name for imaging notes
- `IMAGING_TAGS` - Property name for imaging tags
- `PREVIEW_COMMENT` - Property name for preview comments
- `METADATA_PREVIEW_COUNT` - Metadata key for preview count
- `IMAGING_CODE` - Custom DSS service code for imaging operations
- `EXPORT_TYPE` - Export operation type
- `MULTI_EXPORT_TYPE` - Multi-export operation type
- `PREVIEW_TYPE` - Preview operation type

## React Components

### ImagingGalleryViewer

Displays multiple imaging datasets in a gallery view with filtering and pagination.

**Props:**
- `objId` - Object ID (experiment or sample permanent ID)
- `objType` - Object type (`ObjectType.COLLECTION` or `ObjectType.OBJECT`)
- `extOpenbis` - openBIS API instance
- `onOpenPreview` - Callback when preview is opened
- `onStoreDisplaySettings` - Optional callback to store display settings
- `onLoadDisplaySettings` - Optional callback to load display settings

**Usage:**
```javascript
<ImagingGalleryViewer
    objId={objId}
    objType={ObjectType.COLLECTION}
    extOpenbis={extOpenbis}
    onOpenPreview={handleOpenPreview}
/>
```

### ImagingDatasetViewer

Displays and allows editing of a single imaging dataset.

**Props:**
- `objId` - Dataset permanent ID
- `objType` - Object type
- `extOpenbis` - openBIS API instance
- `onUnsavedChanges` - Callback for unsaved changes
- `showSemanticAnnotations` - Whether to show semantic annotations

**Usage:**
```javascript
<ImagingDatasetViewer
    objId={datasetPermId}
    objType={ObjectType.OBJECT}
    extOpenbis={extOpenbis}
    onUnsavedChanges={handleUnsavedChanges}
    showSemanticAnnotations={true}
/>
```

## API Reference

### ImagingFacade Methods

#### Vocabulary Operations

- `loadImagingVocabularyTerms(code)` - Load vocabulary terms
- `loadDataSetTypes()` - Load dataset types and properties

#### Dataset Operations

- `loadImagingDataset(objId, withProperties, withType, withDatasetsHierarchy)` - Load dataset
- `saveImagingDataset(permId, imagingDataset)` - Save dataset
- `updatePreview(permId, imageIdx, preview)` - Update preview
- `editImagingDatasetNote(permId, note)` - Edit dataset note
- `getImagingDatasetPreviewConfig(objId)` - Get preview config
- `createLocatedSXMPreview(objId, sxmPermId, sxmFilePath, activeImageIdx, selectedDatPreview)` - Create SXM preview
- `updateImagingDataset(objId, activeImageIdx, preview)` - Update via DSS service

#### Gallery Operations

- `loadPaginatedGalleryDatasets(objId, objType, page, pageSize)` - Load paginated gallery
- `filterGallery(objId, objType, operator, filterText, property, page, pageSize)` - Filter gallery
- `fetchExperimentDataSets(objId)` - Fetch experiment datasets
- `fetchSampleDataSets(objId)` - Fetch sample datasets
- `fetchDataSetsSortingInfo(dataSets)` - Calculate preview sorting info
- `paginateImagingDatasets(datasetCodeList, page, pageSize)` - Paginate datasets
- `filterAndPaginateImagingDatasets(dataSets, page, pageSize, operator, filterText, property)` - Filter and paginate

#### Export Operations

- `exportImagingDataset(objId, activeImageIdx, exportConfig, metadata)` - Single export
- `multiExportImagingDataset(exportConfig, exportList)` - Multi export

#### Utility Methods

- `getPathsList(datasetList)` - Get file paths for dataset codes
- `getDatasetFilesPath(dataset)` - Get file paths for sample-related datasets

All methods are fully documented with JSDoc comments. Use your IDE's autocomplete and hover features to see parameter descriptions and return types.

## Data Handlers

The `dataHandlers.js` file provides helper functions:

- `loadGalleryViewFilters(imagingFacade, setDataSetTypes)` - Load filter options
- `loadPreviewsInfo(imagingFacade, objId, objType, galleryFilter, paging)` - Load preview info
- `loadImagingVocabularyTerms(imagingFacade, setImagingTags)` - Load vocabulary terms

## Utilities

The `utils.js` file provides utility functions:

- `convertToBase64(file)` - Convert file to base64
- `isObjectEmpty(objectName)` - Check if object is empty
- `inRange(x, min, max)` - Check if value is in range
- `createInitValues(inputsConfig, activeConfig)` - Create initial values for inputs

## Related Files

- `DatabaseComponent.jsx` - Uses imaging components
- `EntityAutocompleterField.jsx` - May use imaging vocabulary terms

