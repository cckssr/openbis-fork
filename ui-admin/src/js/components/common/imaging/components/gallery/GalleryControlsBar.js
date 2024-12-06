import React from 'react';
import { Typography, IconButton, Grid2 } from "@mui/material";
import ViewListIcon from '@mui/icons-material/ViewList';
import GridOnIcon from '@mui/icons-material/GridOn';
import messages from "@src/js/common/messages.js";
import OutlinedBox from "@src/js/components/common/imaging/components/common/OutlinedBox.js";
import PaperBox from "@src/js/components/common/imaging/components/common/PaperBox.js";
import CustomSwitch from "@src/js/components/common/imaging/components/common/CustomSwitch.jsx";
import GalleryPaging from "@src/js/components/common/imaging/components/gallery/GalleryPaging.jsx";
import GridPagingOptions from "@src/js/components/common/grid/GridPagingOptions.js";
import Exporter from "@src/js/components/common/imaging/components/viewer/Exporter.jsx";
import GalleryFilter from "@src/js/components/common/imaging/components/gallery/GalleryFilter.jsx";

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
            <Grid2 container direction="row" spacing={2} sx={{ alignItems: "center" }}>
                <Grid2 size={{xs:12, sm:7}}>
                    <OutlinedBox label='Paging'>
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
                    </OutlinedBox>
                </Grid2>
                <Grid2 size="auto">
                    <OutlinedBox style={{ width: 'fit-content' }}
                        label={messages.get(messages.SHOW)}>
                        <CustomSwitch isChecked={showAll} onChange={setShowAll} />
                    </OutlinedBox>
                </Grid2>
                <Grid2 size="auto">
                    <OutlinedBox style={{ width: 'fit-content' }} label='Select'>
                        <CustomSwitch disabled={!gridView} isChecked={selectAll}
                            onChange={handleSelectAll} />
                    </OutlinedBox>
                </Grid2>
                <Grid2 size="auto">
                    <OutlinedBox style={{ width: 'fit-content' }} label='View Mode'>
                        <IconButton
                            color={gridView ? 'primary' : 'default'}
                            onClick={() => handleViewChange(true)}
                            size="large">
                            <GridOnIcon fontSize="large" />
                        </IconButton>
                        <IconButton
                            color={!gridView ? 'primary' : 'default'}
                            onClick={() => handleViewChange(false)}
                            size="large">
                            <ViewListIcon fontSize="large" />
                        </IconButton>
                    </OutlinedBox>
                </Grid2>
                <Grid2 size={{xs:10, sm:8}}>
                    <OutlinedBox label='Filter'>
                        <GalleryFilter options={dataSetTypes}
                            galleryFilter={galleryFilter}
                            onGalleryFilterChange={onGalleryFilterChange} />
                    </OutlinedBox>
                </Grid2>
                <Grid2 size={{xs:2, sm:2}}>
                    {configExports.length > 0 &&
                        <Exporter config={configExports} handleExport={handleExport}
                            disabled={isExportDisable} />}
                </Grid2>
            </Grid2>
        </PaperBox>
    );
};

export default GalleryControlsBar;
