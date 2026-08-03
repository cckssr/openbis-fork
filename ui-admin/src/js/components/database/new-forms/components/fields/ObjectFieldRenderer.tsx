import React, { useState, useEffect, useRef } from 'react';
import {
	Autocomplete,
	TextField,
	Box,
	Typography,
	CircularProgress,
	Checkbox
} from '@mui/material';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx';


export const ObjectFieldRenderer: React.FC<FieldRendererProps> = ({
	field,
	openbisFacade,
	onFieldChange,
	mode
}) => {
	const [options, setOptions] = useState<any[]>([]);
	const [loading, setLoading] = useState(false);
	const [inputValue, setInputValue] = useState('');
	const [multiInputValue, setMultiInputValue] = useState('');
	const multiInputRef = useRef<HTMLInputElement | null>(null);
	const [value, setValue] = useState<any>(field.value || null);
	const [multiValues, setMultiValues] = useState<any[]>([]);

	const sampleTypeCode = field.meta?.sampleTypeCode;
	const typeHint = sampleTypeCode
		? `Select object of type ${sampleTypeCode}.`
		: 'Select object of any type.';
	const noOptionsHint = `Type at least 2 characters to search. ${typeHint}`;

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
		if (field.meta?.sampleTypeCode) {
			criteria.withType().withCode().thatEquals(field.meta.sampleTypeCode);
		}
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withExperiment();
		fetchOptions.withProject();
		fetchOptions.withSpace();
		const result = await openbisFacade.searchSamples(criteria, fetchOptions);
		return result.getObjects();
	};

	const loadSelectedSamples = async (permIds: string[]): Promise<any[]> => {
		if (!permIds || permIds.length === 0) return [];
		const { SamplePermId, SampleFetchOptions } = openbisFacade;
		const ids = permIds.map((id: string) => new SamplePermId(id));
		const fetchOptions = new SampleFetchOptions();
		fetchOptions.withExperiment();
		fetchOptions.withProject();
		fetchOptions.withSpace();
		const result = await openbisFacade.getSamples(ids, fetchOptions);
		return Object.values(result);
	};

	const formatSampleDisplay = (sample: any): string => {
		if (!sample) return '';
		if (typeof sample === 'string') return sample;
		const identifier = sample?.identifier?.identifier || sample?.displayName || sample?.code || '';
		const permId = sample?.permId?.permId;
		return permId ? `${identifier} (${permId})` : identifier;
	};

	// Debounced search
	useEffect(() => {
		const timeoutId = setTimeout(() => {
			searchEntities(inputValue);
		}, 300);

		return () => clearTimeout(timeoutId);
	}, [inputValue]);

	// Resolve permId(s) to sample object(s) on mount AND whenever field.value changes externally
	// (e.g. auto-save restore from localStorage). Content-guarded so a local pick that just
	// round-trips through the parent doesn't trigger a refetch.
	useEffect(() => {
		if (field.isMultiValue && Array.isArray(field.value)) {
			const currentPermIds = multiValues.map(v => v?.permId?.permId).filter(Boolean);
			const incoming = field.value as string[];
			const sameContent =
				currentPermIds.length === incoming.length &&
				currentPermIds.every((p, i) => p === incoming[i]);
			if (!sameContent) {
				if (incoming.length > 0) {
					loadSelectedSamples(incoming).then(setMultiValues);
				} else {
					setMultiValues([]);
				}
			}
		} else if (!field.isMultiValue && typeof field.value === 'string' && field.value) {
			if (value?.permId?.permId !== field.value) {
				loadSelectedSamples([field.value]).then(objects => {
					if (objects.length > 0) {
						setValue(objects[0]);
					}
				});
			}
		}
	}, [field.value, field.isMultiValue]);

	const handleInputChange = (event: any, newInputValue: string) => {
		setInputValue(newInputValue);
	};

	const handleValueChange = (event: any, newValue: any) => {
		setValue(newValue);
		onFieldChange(field.id, newValue?.permId?.permId || null);
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

	const renderView = () => {
		if (field.isMultiValue) {
			const lines = multiValues.map((s, i) => <div key={i}>{formatSampleDisplay(s)}</div>);
			return (
				<FormFieldView
					label={field.label}
					value={lines.length > 0 ? <>{lines}</> : undefined}
					description={field.meta?.helpText}
					disableUnderline={true}
				/>
			);
		} else {
			return (
				<FormFieldView
					label={field.label}
					value={formatSampleDisplay(value)}
					description={field.meta?.helpText}
					disableUnderline={true}
				/>
			);
		}
	};

	const renderMultiEdit = () => (
		<Box sx={{ width: '100%' }}>
			<Autocomplete
				multiple
				disableCloseOnSelect
				value={multiValues}
				onChange={(_, newValues: any[]) => {
					setMultiValues(newValues);
					onFieldChange(
						field.id,
						newValues.map((v) => v?.permId?.permId).filter(Boolean)
					);
				}}
				onInputChange={(_, newValue, reason) => {
					if (reason === 'clear') {
						setMultiInputValue('');
						setInputValue('');
						if (multiInputRef.current) {
							multiInputRef.current.value = '';
						}
					}
				}}
				onClose={() => {
					setMultiInputValue('');
					setInputValue('');
					if (multiInputRef.current) {
						multiInputRef.current.value = '';
					}
				}}
				filterOptions={(x) => x}
				options={options}
				loading={loading}
				getOptionLabel={getOptionLabel}
				isOptionEqualToValue={isOptionEqualToValue}
				renderOption={(props, option, { selected }) => {
					const { key, ...optionProps } = props;
					const displayName =
						option?.identifier?.identifier ||
						option?.displayName ||
						option?.code ||
						'Unknown';
					const permId = option?.permId?.permId;
					return (
						<Box component="li" key={key} {...optionProps}>
							<Checkbox
								icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
								checkedIcon={<CheckBoxIcon fontSize="small" />}
								style={{ marginRight: 8 }}
								checked={selected}
							/>
							<Typography variant="body1">
								{displayName} ({permId})
							</Typography>
						</Box>
					);
				}}
				renderInput={(params) => {
					const { value: _managed, ref: muiInputRef, ...htmlInputProps } = params.inputProps;
					return (
						<TextField
							{...params}
							inputProps={{
								...htmlInputProps,
								ref: (el: HTMLInputElement) => {
									multiInputRef.current = el;
									if (typeof muiInputRef === 'function') {
										muiInputRef(el);
									} else if (muiInputRef && typeof muiInputRef === 'object') {
										(muiInputRef as React.MutableRefObject<HTMLInputElement | null>).current = el;
									}
								},
								onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
									setMultiInputValue(e.target.value);
									setInputValue(e.target.value);
									(htmlInputProps as any).onChange?.(e);
								}
							}}
							label={field.label}
							required={field.required}
							variant="filled"
							sx={{ '& .MuiInputBase-input': { fontSize: '0.875rem' }, '& .MuiInputLabel-root': { fontSize: '0.875rem' } }}
							slotProps={{
								input: {
									...params.InputProps,
									endAdornment: (
										<>
											{loading ? <CircularProgress color="inherit" size={20} /> : null}
											{params.InputProps.endAdornment}
										</>
									),
								},
							}}
						/>
					);
				}}
				noOptionsText={multiInputValue.length < 2 ? noOptionsHint : 'No objects found'}
			/>
		</Box>
	);

	const renderEdit = () => {
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
							label={field.label}
							required={field.required}
							variant="filled"
							sx={{ '& .MuiInputBase-input': { fontSize: '0.875rem' }, '& .MuiInputLabel-root': { fontSize: '0.875rem' } }}
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
					noOptionsText={inputValue.length < 2 ? noOptionsHint : "No entities found"}
					clearOnEscape
					selectOnFocus
					handleHomeEndKeys
				/>

			</Box>
		);
	};

	return (
		mode === FormMode.VIEW
			? renderView()
			: field.isMultiValue
			? renderMultiEdit()
			: renderEdit()
	);
};

export default ObjectFieldRenderer;
