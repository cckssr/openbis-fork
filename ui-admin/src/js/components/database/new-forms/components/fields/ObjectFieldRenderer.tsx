import React, { useState, useEffect } from 'react';
import {
	Autocomplete,
	TextField,
	Box,
	Typography,
	CircularProgress
} from '@mui/material';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';


export const ObjectFieldRenderer: React.FC<FieldRendererProps> = ({
	field,
	openbisFacade,
	onFieldChange,
	mode
}) => {
	const [options, setOptions] = useState<any[]>([]);
	const [loading, setLoading] = useState(false);
	const [inputValue, setInputValue] = useState('');
	const [value, setValue] = useState<any>(field.value || null);

	const searchEntities = async (searchTerm: string) => {
		if (!searchTerm || searchTerm.length < 2) {
			setOptions([]);
			return;
		}
		setLoading(true);
		try {
			const sampleOptions = await searchSample(searchTerm);
			setOptions(sampleOptions);
		} catch (error) {
			console.error('Error searching entities:', error);
			setOptions([]);
		} finally {
			setLoading(false);
		}
	};

	const searchSample = async (searchTerm: string) => {
		const { SampleFetchOptions, SampleSearchCriteria } = openbisFacade;
		const criteria = new SampleSearchCriteria();
		criteria.withCode().thatContains(searchTerm);
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withExperiment();
		fetchOptions.withProject();
		fetchOptions.withSpace();
		const result = await openbisFacade.searchSamples(criteria, fetchOptions);
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
		onFieldChange(field.id, newValue.permId.permId);
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
						label="Search object to link to"
						required={field.required}
						variant="filled"
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

export default ObjectFieldRenderer;
