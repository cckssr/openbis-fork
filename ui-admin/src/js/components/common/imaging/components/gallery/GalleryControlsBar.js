import React from 'react';
import { Typography, IconButton, Grid2, Stack, Divider } from '@mui/material';
import ViewListIcon from '@mui/icons-material/ViewList';
import GridOnIcon from '@mui/icons-material/GridOn';
import messages from '@src/js/common/messages.js';
import PaperBox from '@src/js/components/common/imaging/components/common/PaperBox.js';
import CustomSwitch from '@src/js/components/common/imaging/components/common/CustomSwitch.jsx';
import GalleryPaging from '@src/js/components/common/imaging/components/gallery/GalleryPaging.jsx';
import GridPagingOptions from '@src/js/components/common/grid/GridPagingOptions.js';
import Exporter from '@src/js/components/common/imaging/components/viewer/Exporter.jsx';
import GalleryFilter from '@src/js/components/common/imaging/components/gallery/GalleryFilter.jsx';
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles((theme) => ({
    fw: {
        width: '100%'
    }
}));

const GalleryControlsBar = ({
    isExportDisable, configExports,
    gridView, handleViewChange,
    paging, setPaging,
    showAll, setShowAll,
    selectAll, handleSelectAll,
    galleryFilter, onGalleryFilterChange,
    count,
    handleExport,
    dataSetTypes
}) => {

    const classes = useStyles();
    const options = GridPagingOptions.GALLERY_PAGE_SIZE_OPTIONS[paging.pageColumns - 1].map(pageSize => ({
        label: pageSize,
        value: pageSize
    }))
    // Logic for rendering options, handling view switch, filtering, etc.
    return (
        <PaperBox>
            <Typography variant='h6'>
                Gallery View
            </Typography>
            <Grid2 container direction='row' sx={{ alignItems: 'center' }}>
                <Grid2 container size={{ xs: 12, sm: 12, md: 'grow' }}>
                    <GalleryPaging id='gallery-paging'
                        count={count}
                        page={paging.page}
                        pageSize={paging.pageSize}
                        pageColumns={paging.pageColumns}
                        options={options}
                        isGridView={gridView}
                        onColumnChange={(value) => setPaging({
                            page: 0,
                            pageSize: value,
                            pageColumns: value
                        })}
                        onPageChange={(value) => setPaging({
                            ...paging,
                            page: value
                        })}
                        onPageSizeChange={(value) => setPaging({
                            ...paging,
                            page: 0,
                            pageSize: value
                        })}
                    />
                </Grid2>
                <Grid2 size={{ sm: 4, md: 'auto' }}>
                    <CustomSwitch label={messages.get(messages.SHOW)}
                        labelPlacement='top'
                        isChecked={showAll}
                        onChange={setShowAll} />
                </Grid2>
                <Grid2 size={{ sm: 4, md: 'auto' }}>
                    <CustomSwitch label='Select'
                        labelPlacement='top'
                        disabled={!gridView}
                        isChecked={selectAll}
                        onChange={handleSelectAll} />
                </Grid2>
                <Grid2 size={{ sm: 4, md: 2 }} sx={{ justifyItems: 'center' }}>
                    <Stack direction='row' divider={<Divider orientation='vertical' flexItem />} spacing={0} sx={{ alignItems: 'center' }}>
                        <IconButton
                            color={gridView ? 'primary' : 'default'}
                            onClick={() => handleViewChange(true)}
                            size='large'>
                            <GridOnIcon fontSize='large' />
                        </IconButton>
                        <IconButton
                            color={!gridView ? 'primary' : 'default'}
                            onClick={() => handleViewChange(false)}
                            size='large'>
                            <ViewListIcon fontSize='large' />
                        </IconButton>
                    </Stack>
                </Grid2>
                <Grid2 size={{ sm: 12, md: 10 }}>
                    <GalleryFilter options={dataSetTypes}
                        galleryFilter={galleryFilter}
                        onGalleryFilterChange={onGalleryFilterChange} />
                </Grid2>
                <Grid2 size={{ sm: 12, md: 2 }}>
                    {configExports.length > 0 &&
                        <Exporter config={configExports} 
                            handleExport={handleExport}
                            disabled={isExportDisable} 
                            styles={{ root: classes.fw }}/>} 
                </Grid2>
            </Grid2>
        </PaperBox>
    );
};

export default GalleryControlsBar;
