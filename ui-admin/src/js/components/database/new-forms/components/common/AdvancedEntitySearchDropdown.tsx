import React, { useState, useEffect } from 'react';
import {
  Autocomplete,
  TextField,
  Box,
  Typography,
  CircularProgress,
  InputAdornment
} from '@mui/material';
import AutocompleterField from '@src/js/components/common/form/AutocompleterField.jsx'
import FormFieldContainer from '@src/js/components/common/form/FormFieldContainer';
import FormFieldLabel from '@src/js/components/common/form/FormFieldLabel';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';


interface AdvancedEntitySearchDropdownProps {
  openbisFacade: any;
  entityType: string;
  onSelectionChange: (selected: any) => void;
  selectedEntity?: any;
  placeholder?: string;
  // Additional props that might be needed based on the original implementation
  includeProjects?: boolean;
  includeExperiments?: boolean;
  includeSamples?: boolean;
  includeSpaces?: boolean;
  includeDatasets?: boolean;
  required?: boolean;
}

/**
 * Advanced Entity Search Dropdown Component
 * TODO: Implement the full functionality based on the original AdvancedEntitySearchDropdown
 * 
 * Original JavaScript usage:
 * - new AdvancedEntitySearchDropdown(false, true, "search entity to move to", false, false, false, true, false)
 * - advancedEntitySearchDropdown.onChange(function(selected) { moveEntityModel.selected = selected[0]; });
 * - advancedEntitySearchDropdown.init($entityBox);
 */
export const AdvancedEntitySearchDropdown: React.FC<AdvancedEntitySearchDropdownProps> = ({
  openbisFacade,
  entityType,
  onSelectionChange,
  selectedEntity,
  placeholder = "Search target entity to move to",
  includeProjects = false,
  includeExperiments = false,
  includeSamples = false,
  includeSpaces = false,
  includeDatasets = false,
  required = false
}) => {
  const [options, setOptions] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [value, setValue] = useState<any>(selectedEntity || null);

  // TODO: Implement search functionality
  const searchEntities = async (searchTerm: string) => {
    if (!searchTerm || searchTerm.length < 2) {
      setOptions([]);
      return;
    }

    setLoading(true);
    
    try {
      // TODO: Implement actual search logic based on entityType and search criteria
      // This should call the appropriate openBIS API methods based on the entity type
      let entityOptions: any[] = [];
      switch (entityType) {
        case EntityKind.PROJECT:
          entityOptions = [...entityOptions, ...await searchSpaces(searchTerm)];
          break;
        case EntityKind.EXPERIMENT:
          entityOptions = [...entityOptions, ...await searchSpaces(searchTerm)];
          entityOptions = [...entityOptions, ...await searchProject(searchTerm)];
          //entityOptions.experiments = await searchExperiment(searchTerm);
          break;
        case EntityKind.SAMPLE:
          entityOptions = [...entityOptions, ...await searchSpaces(searchTerm)];
          entityOptions = [...entityOptions, ...await searchProject(searchTerm)];
          //entityOptions.experiments = await searchExperiment(searchTerm);
          //entityOptions.samples = await searchSample(searchTerm);
          break;
      }
      console.log({ entityOptions });
      setOptions(entityOptions);
    } catch (error) {
      console.error('Error searching entities:', error);
      setOptions([]);
    } finally {
      setLoading(false);
    }
  };

  const searchSpaces = async (searchTerm: string) => {
    const { SpacePermId, SpaceFetchOptions, SpaceSearchCriteria } = openbisFacade;
    const criteria = new SpaceSearchCriteria();
    criteria.withCode().thatContains(searchTerm);
    const fetchOptions = new SpaceFetchOptions();
    const result = await openbisFacade.searchSpaces(criteria, fetchOptions);
    console.log({ result });
    return result.getObjects();
  };

  const searchProject = async (searchTerm: string) => {
    const { ProjectPermId, ProjectFetchOptions, ProjectSearchCriteria, RightsFetchOptions } = openbisFacade;
    const criteria = new ProjectSearchCriteria();
    criteria.withCode().thatContains(searchTerm);
    const fetchOptions = new ProjectFetchOptions();
    fetchOptions.withSpace();
    const result = await openbisFacade.searchProjects(criteria, fetchOptions);
    return result.getObjects();
  };

  // Debounced search
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      searchEntities(inputValue);
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [inputValue]);

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
            required={required}
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
          console.log({ option });
          const displayName = option?.identifier?.identifier || option?.displayName || option?.code || 'Unknown';
          const permId = option?.permId?.permId;
          
          return (
            <Box component="li" {...props}>
                <Typography variant="body1">
                  {displayName} ({permId})
                </Typography>
            </Box>
          );
        }}
        noOptionsText={inputValue.length < 2 ? "Type at least 2 characters to search" : "No entities found"}
        clearOnEscape
        selectOnFocus
        handleHomeEndKeys
      />
      
    </Box>
  );
};

export default AdvancedEntitySearchDropdown;
