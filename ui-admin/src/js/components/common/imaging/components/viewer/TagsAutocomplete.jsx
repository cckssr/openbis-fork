import React from 'react';
import { TextField, Autocomplete, Checkbox } from '@mui/material';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';

const TagsAutocomplete = ({ activePreviewTags, imagingTags, label = 'Tags', size, onChange }) => {
	const [tags, setTags] = React.useState([]);
	const [inputValue, setInputValue] = React.useState('');

	// Update tags when preview tags change
	React.useEffect(() => {
		if (activePreviewTags && activePreviewTags.length > 0) {
			const transformedTags = [];
			for (const activePreviewTag of activePreviewTags) {
				const matchTag = imagingTags?.find(imagingTag => imagingTag.value === activePreviewTag);
				if (matchTag) {
					transformedTags.push(matchTag);
				}
			}
			setTags(transformedTags);
			setInputValue(transformedTags.map(t => t.label).join(', '));
		} else {
			setTags([]);
			setInputValue('');
		}
	}, [activePreviewTags, imagingTags]);

	const handleTagsChange = (event, newTags) => {
		setTags(newTags);
		const tagsArray = newTags.map(tag => tag.value);
		if (onChange) {
			onChange(event, tagsArray);
		}
	};

	return (
		<Autocomplete
			multiple
			id='tags-autocomplete'
			options={imagingTags || []}
			disableCloseOnSelect
			getOptionLabel={(option) => option.label || option}
			inputValue={inputValue}
			value={tags}
			onInputChange={(event, newInputValue) => {
				setInputValue(newInputValue);
			}}
			renderInput={(params) => (
				<TextField variant='standard' label={label} {...params} placeholder='Search Tag' />
			)}
			renderOption={(props, option, { selected }) => {
				const { key, ...optionProps } = props;
				return (
					<li key={key} {...optionProps}>
						<Checkbox
							icon={<CheckBoxOutlineBlankIcon fontSize='small' />}
							checkedIcon={<CheckBoxIcon fontSize='small' />}
							style={{ marginRight: 8 }}
							checked={selected}
						/>
						{option.label || option}
					</li>
				);
			}}
			onChange={handleTagsChange}
			size={size}
		/>
	);
};

export default TagsAutocomplete;

