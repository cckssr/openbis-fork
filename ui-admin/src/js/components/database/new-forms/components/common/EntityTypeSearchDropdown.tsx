import React, { useState, useEffect, useCallback } from 'react';
import {
  Autocomplete,
  TextField,
  Box,
  Typography,
  CircularProgress
} from '@mui/material';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';


interface EntityTypeSearchDropdownProps {
  openbisFacade: any;
  actionName: string;
  onSelectionChange: (selected: any) => void;
  selectedEntity?: any;
  placeholder?: string;
}


export const EntityTypeSearchDropdown: React.FC<EntityTypeSearchDropdownProps> = ({
  openbisFacade,
  actionName,
  onSelectionChange,
  selectedEntity,
  placeholder = "Search entity type",
}) => {
  const [options, setOptions] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [value, setValue] = useState<any>(selectedEntity || null);

  const searchCollectionTypes = useCallback(async (searchTerm: string | null, limit?: number) => {
    const { ExperimentTypeFetchOptions, ExperimentTypeSearchCriteria } = openbisFacade;
    const criteria = new ExperimentTypeSearchCriteria();
    if (searchTerm && searchTerm.length >= 2) {
      criteria.withCode().thatContains(searchTerm);
    }
    const fetchOptions = new ExperimentTypeFetchOptions();
    if (limit !== undefined) {
      fetchOptions.from(0).count(limit);
    }
    const result = await openbisFacade.searchExperimentTypes(criteria, fetchOptions);
    return result.getObjects();
  }, [openbisFacade]);

  const searchObjectTypes = useCallback(async (searchTerm: string | null, limit?: number) => {
    const { SampleTypeFetchOptions, SampleTypeSearchCriteria } = openbisFacade;
    const criteria = new SampleTypeSearchCriteria();
    if (searchTerm && searchTerm.length >= 2) {
      criteria.withCode().thatContains(searchTerm);
    }
    const fetchOptions = new SampleTypeFetchOptions();
    if (limit !== undefined) {
      fetchOptions.from(0).count(limit);
    }
    const result = await openbisFacade.searchSampleTypes(criteria, fetchOptions);
    return result.getObjects();
  }, [openbisFacade]);

  const searchDatasetTypes = useCallback(async (searchTerm: string | null, limit?: number) => {
    const { DataSetTypeFetchOptions, DataSetTypeSearchCriteria } = openbisFacade;
    const criteria = new DataSetTypeSearchCriteria();
    if (searchTerm && searchTerm.length >= 2) {
      criteria.withCode().thatContains(searchTerm);
    }
    const fetchOptions = new DataSetTypeFetchOptions();
    if (limit !== undefined) {
      fetchOptions.from(0).count(limit);
    }
    const result = await openbisFacade.searchDataSetTypes(criteria, fetchOptions);
    return result.getObjects();
  }, [openbisFacade]);

  const searchEntityTypes = useCallback(async (searchTerm: string, limit?: number) => {
    setLoading(true);
    let entityOptions: any[] = [];
    switch (actionName) {
      case EntityKind.NEW_COLLECTION:
        entityOptions = [...entityOptions, ...await searchCollectionTypes(searchTerm, limit)];
        break;
      case EntityKind.NEW_OBJECT:
        entityOptions = [...entityOptions, ...await searchObjectTypes(searchTerm, limit)];
        break;
      case EntityKind.NEW_DATASET:
        entityOptions = [...entityOptions, ...await searchDatasetTypes(searchTerm, limit)];
        break;
      default:
        throw new Error(`Unknown new entity type action: ${actionName}`);
    }
    setOptions(entityOptions);
    setLoading(false);
  }, [actionName, searchCollectionTypes, searchObjectTypes, searchDatasetTypes]);

  // Load initial 10 items on mount
  useEffect(() => {
    searchEntityTypes('', 10);
  }, [searchEntityTypes]);

  // Debounced search - remove limit when user types 2+ characters
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (inputValue.length >= 2) {
        // Remove limit when searching with 2+ characters
        searchEntityTypes(inputValue);
      } else if (inputValue.length === 0) {
        // Reset to initial 10 items when input is cleared
        searchEntityTypes('', 10);
      }
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [inputValue, searchEntityTypes]);

  const handleInputChange = (event: any, newInputValue: string) => {
    setInputValue(newInputValue);
  };

  const handleValueChange = (event: any, newValue: any) => {
    setValue(newValue);
    onSelectionChange(newValue);
  };

  const getOptionLabel = (option: any) => {
    if (typeof option === 'string') return option;

    // Handle nested identifier structure (for Projects, Spaces, etc.)
    if (option?.identifier?.identifier) {
      return option.identifier.identifier;
    }

    // Fall back to other properties
    return option?.displayName || option?.code || option?.identifier || option?.id || 'Unknown';
  };

  const isOptionEqualToValue = (option: any, value: any) => {
    if (!option || !value) return false;

    // Compare permIds
    if (option?.permId?.permId && value?.permId?.permId) {
      return option.permId.permId === value.permId.permId;
    }

    // Compare identifiers
    if (option?.identifier?.identifier && value?.identifier?.identifier) {
      return option.identifier.identifier === value.identifier.identifier;
    }

    // Compare IDs
    return option?.id === value?.id;
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Autocomplete
        value={value}
        onChange={handleValueChange}
        inputValue={inputValue}
        onInputChange={handleInputChange}
        options={options}
        groupBy={(option) => option?.['@type']?.split('.').pop() || 'Unknown'}
        loading={loading}
        getOptionLabel={getOptionLabel}
        isOptionEqualToValue={isOptionEqualToValue}
        renderInput={(params) => (
          <TextField
            {...params}
            label={placeholder}
            slotProps={{
              input: {
                ...params.InputProps,
                endAdornment: (
                  <>
                    {loading ? <CircularProgress color="inherit" size={20} /> : null}
                    {params.InputProps.endAdornment}
                  </>
                ),
              }
            }}
          />
        )}
        renderOption={(props, option) => {
          const displayName = option?.identifier?.identifier || option?.displayName || option?.code || 'Unknown';
          const permId = option?.permId?.permId;
          const generatedCodePrefix = option?.generatedCodePrefix;
          const description = option?.description;
          return (
            <Box component="li" {...props}>
              <Typography variant="body1">
                {displayName}
              </Typography>
            </Box>
          );
        }}
        noOptionsText={inputValue.length < 2 ? "Type at least 2 characters to search" : "No entity types found"}
        clearOnEscape
        selectOnFocus
        handleHomeEndKeys
      />

    </Box>
  );
};

export default EntityTypeSearchDropdown;
