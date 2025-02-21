import React from 'react'
import { Divider, Grid2, TextField, Autocomplete, Checkbox, Typography, Tab } from '@mui/material';
import { isObjectEmpty, createInitValues } from '@src/js/components/common/imaging/utils.js';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import constants from '@src/js/components/common/imaging/constants.js';
import CustomSwitch from '@src/js/components/common/imaging/components/common/CustomSwitch.jsx';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import AddToQueueIcon from '@mui/icons-material/AddToQueue';

import messages from '@src/js/common/messages.js'
import Message from '@src/js/components/common/form/Message.jsx'
import Button from '@src/js/components/common/form/Button.jsx'
import InputControlsSection from '@src/js/components/common/imaging/components/viewer/InputControlsSection.js';
import { useImagingDataContext } from '@src/js/components/common/imaging/components/viewer/ImagingDataContext.jsx';
import { TabContext, TabList, TabPanel } from '@mui/lab';
import TuneIcon from '@mui/icons-material/Tune';
import PhotoFilterIcon from '@mui/icons-material/PhotoFilter';
import FilterSelector from '@src/js/components/common/imaging/components/viewer/FilterSelector.jsx';
import { makeStyles } from '@mui/styles';

const useStyles = makeStyles(theme => ({
  scrollableTab: {
    padding: '0',
    maxHeight: '60vh'
  },
  overflowAuto: {
    overflowY: 'auto',
    overflowX: 'hidden'
  }
}));

const MainPreviewInputControls = ({ activePreview, configInputs, configFilters, configResolutions }) => {
  const classes = useStyles();

  const [tags, setTags] = React.useState([])
  const [inputValue, setInputValue] = React.useState('');
  const [spectraLocator, setSpectraLocator] = React.useState(constants.NONE);
  const [tab, setTab] = React.useState('1');

  const { state, handleUpdate, handleTagImage,
    handleResolutionChange, handleActiveConfigChange,
    handleShowPreview, createLocatedSXMPreview, handleOnAddFilter } = useImagingDataContext();

  const { imagingDataset, resolution, isChanged, imagingTags, datasetType, datasetFilePaths } = state;

  React.useEffect(() => {
    if (imagingDataset.metadata[constants.GENERATE] && imagingDataset.metadata[constants.GENERATE].toLowerCase() === 'true')
      handleUpdate();
  }, []);

  React.useEffect(() => {
    if (activePreview && activePreview.tags != null) {
      var trasformedTags = []
      for (const activePreviewTag of activePreview.tags) {
        const matchTag = imagingTags.find(imagingTag => imagingTag.value === activePreviewTag);
        trasformedTags.push(matchTag);
      }
      setTags(trasformedTags);
      setInputValue(trasformedTags.join(', '));
    }
  }, [activePreview]);

  const handleChange = (event, newValue) => {
    setTab(newValue);
  };

  const handleTagsChange = (event, newTags) => {
    setTags(newTags);
    const tagsArray = newTags.map(tag => tag.value);
    handleTagImage(false, tagsArray);
  }

  const handleLocatorWidget = async () => {
    const [sxmPermId, sxmFilePath] = spectraLocator.split(' - ');
    createLocatedSXMPreview(sxmPermId, sxmFilePath);
  }

  const renderStaticUpdateControls = (isUploadedPreview,) => {
    return (<>
      <Grid2 size={{ md: 12 }} container sx={{ justifyContent: 'space-around', alignContent: 'center', height: '24px' }}>
        {isChanged && !isUploadedPreview && (
          <Message type='info'>
            {messages.get(messages.UPDATE_CHANGES)}
          </Message>
        )}
      </Grid2>
      <Grid2 container direction='row' sx={{ alignItems: 'center', mb: 1, justifyContent: 'space-between' }} size={{ xs: 12, sm: 12 }}>
        <Button label={messages.get(messages.UPDATE)}
          variant='outlined'
          color='primary'
          startIcon={<RefreshIcon />}
          onClick={handleUpdate}
          disabled={!isChanged || isUploadedPreview} />

        <CustomSwitch labelPlacement='start'
          label={messages.get(messages.SHOW)}
          isChecked={activePreview.show}
          onChange={handleShowPreview} />
      </Grid2>
      <Dropdown onSelectChange={handleResolutionChange}
        label={messages.get(messages.RESOLUTIONS)}
        values={configResolutions}
        initValue={resolution.join('x')}
        isMulti={false}
        disabled={false}
        key={'InputsPanel-resolutions'} />

      <Grid2 sx={{ alignItems: 'center', mb: 1, px: 1 }} size={{ xs: 12, sm: 12 }}>
        <Autocomplete multiple
          id='tags-autocomplete'
          options={imagingTags}
          disableCloseOnSelect
          getOptionLabel={(option) => option.label}
          inputValue={inputValue}
          value={tags}
          onInputChange={(event, newInputValue) => {
            setInputValue(newInputValue);
          }}
          renderInput={(params) => (
            <TextField variant='standard' label='Preview Tags' {...params} placeholder='Search Tag' />
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
                {option.label}
              </li>
            );
          }}
          onChange={handleTagsChange}
        />
      </Grid2>
    </>)
  }

  const renderDynamicControls = (configInputs) => {
    if (configInputs !== null && configInputs.length > 0) {
      const sectionGroups = Map.groupBy(configInputs, imageDatasetControl => imageDatasetControl.section)
      var sectionsArray = []
      for (let [key, imageDatasetControlList] of sectionGroups) {
        sectionsArray.push(<InputControlsSection key={key} sectionKey={key} imageDatasetControlList={imageDatasetControlList} inputValues={inputValues} isUploadedPreview={isUploadedPreview} onChangeActConf={handleActiveConfigChange} />)
      }
      return (<Grid2 container sx={{ justifyContent: 'space-between', maxHeight: '70%', width: '100%', minHeight: '300px' }}> {sectionsArray} </Grid2>);
    } else {
      return <Typography px={1} my={1} textAlign={'center'}>{messages.get(messages.NO_CONFIG_PROVIDED, messages.get(messages.PARAMETERS))}</Typography>
    }
  }

  const onAddFilter = (filterHistory) => {
    handleOnAddFilter(filterHistory);
  }

  const inputValues = createInitValues(configInputs, activePreview.config);
  activePreview.config = inputValues;
  const currentMetadata = activePreview.metadata;
  const isUploadedPreview = datasetType === constants.USER_DEFINED_IMAGING_DATA ? true : isObjectEmpty(currentMetadata) ? false : ('file' in currentMetadata);

  const datasetFilePathsMenu = datasetFilePaths?.map(datasetFilePath => datasetFilePath[0] + ' - ' + datasetFilePath[1])
  datasetFilePathsMenu?.splice(0, 0, constants.NONE);

  //console.log('datasetFilePathsMenu', imagingDataset);

  return (
    <Grid2 container direction='row' size={{ sm: 12, md: 4 }} sx={{ px: '8px', display: 'block' }}>

      <Grid2 container sx={{ justifyContent: 'space-between', maxHeight: '30%', width: '100%' }}>
        {(datasetType === constants.IMAGING_DATA || datasetType === constants.USER_DEFINED_IMAGING_DATA)
          && renderStaticUpdateControls(isUploadedPreview)}

        <Grid2 container sx={{ justifyItems: 'center', alignItems: 'center' }} size={{ xs: 12, sm: 12 }}>
          <Grid2 size='grow'>
            <Dropdown onSelectChange={event => setSpectraLocator(event.target.value)}
              label='Spectra Locator'
              values={datasetFilePathsMenu}
              initValue={spectraLocator}
              isMulti={false}
              disabled={false}
              key={'InputsPanel-Spectra'} />
          </Grid2>
          <Grid2 size='auto'>
            <Button label='NEW'
              variant='outlined'
              color='inherit'
              startIcon={<AddToQueueIcon />}
              onClick={handleLocatorWidget}
              disabled={spectraLocator === constants.NONE} />
          </Grid2>
        </Grid2>

      </Grid2>
      <Grid2 container size={{ xs: 12 }}>
        <Divider variant='middle' sx={{ margin: '16px 0px 16px 0px', width: '100%', height: '2px' }} />
      </Grid2>
      <Grid2 >
        <TabContext value={tab} >
          <TabList onChange={handleChange} aria-label='Preview control' variant='fullWidth' textColor='inherit' indicatorColor='secondary'>
            <Tab icon={<TuneIcon />} label={messages.get(messages.PARAMETERS)} value='1' />
            <Tab icon={<PhotoFilterIcon />} label={messages.get(messages.FILTERS)} value='2' />
          </TabList>
          <TabPanel className={classes.scrollableTab + ' ' + classes.overflowAuto} value='1'>
            {renderDynamicControls(configInputs)}
          </TabPanel>
          <TabPanel className={classes.scrollableTab} value='2'>
            <FilterSelector configFilters={configFilters} onAddFilter={onAddFilter} historyFilters={activePreview.filterConfig} />
          </TabPanel>
        </TabContext>
      </Grid2>

    </Grid2>
  );
};

export default MainPreviewInputControls;