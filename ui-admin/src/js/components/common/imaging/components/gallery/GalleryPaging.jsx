import React from 'react';
import { Typography, IconButton, Grid2, Stack } from '@mui/material';
import FirstPageIcon from '@mui/icons-material/FirstPage';
import KeyboardArrowLeft from '@mui/icons-material/KeyboardArrowLeft';
import KeyboardArrowRight from '@mui/icons-material/KeyboardArrowRight';
import LastPageIcon from '@mui/icons-material/LastPage';
import SelectField from '@src/js/components/common/form/SelectField.jsx';
import GridPagingOptions from '@src/js/components/common/grid/GridPagingOptions.js';
import messages from '@src/js/common/messages.js';
import { makeStyles } from '@mui/styles';

const useStyles = makeStyles((theme) => ({
    pageSize: {
        marginLeft: theme.spacing(2)
    },
    pageSizeLabel: {
        fontSize: theme.typography.body2.fontSize,
        marginRight: '12px',
        whiteSpace: 'nowrap',
        lineHeight: '46px'
    }
})
)

const GalleryPaging = ({id, count, page, pageSize, pageColumns, onColumnChange, options, isGridView, onPageSizeChange, onPageChange}) => { 

    const classes = useStyles();

    function handlePageSizeChange(event) {
        onPageSizeChange(event.target.value);
    }

    function handleFirstPageButtonClick() {
        onPageChange(0);
    }

    function handleBackButtonClick() {
        onPageChange(page - 1);
    }

    function handleNextButtonClick() {
        onPageChange(page + 1);
    }

    function handleLastPageButtonClick() {
        onPageChange(
            Math.max(0, Math.ceil(count / pageSize) - 1)
        );
    }

    const renderRange = () => {
        if (count === 0) {
            return <span>{messages.get(messages.NO_RESULTS_FOUND)}</span>
        } else if (count === 1) {
            return <span>{messages.get(messages.RESULTS_RANGE, 1, 1)}</span>
        } else {
            const from = Math.min(count, page * pageSize + 1)
            const to = Math.min(count, (page + 1) * pageSize)

            return (
                <span>
                    {messages.get(messages.RESULTS_RANGE, from + '-' + to, count)}
                </span>
            )
        }
    }

    return (<>
        <Grid2 size={{ xs: 12, sm: 12, md: 'grow' }} sx={{ width: 'auto' }}>
            <Stack direction='row' spacing={1} sx={{ alignItems: 'center' }}>
                <IconButton
                    id={id + '.first-page-id'}
                    onClick={handleFirstPageButtonClick}
                    disabled={page === 0}
                    aria-label={messages.get(messages.FIRST_PAGE)}
                    data-part='firstPage'
                    size='large'>
                    <FirstPageIcon fontSize='small' />
                </IconButton>
                <IconButton
                    id={id + '.prev-page-id'}
                    onClick={handleBackButtonClick}
                    disabled={page === 0}
                    aria-label={messages.get(messages.PREVIOUS_PAGE)}
                    data-part='prevPage'
                    size='large'>
                    <KeyboardArrowLeft fontSize='small' />
                </IconButton>

                <Typography variant='body2' data-part='range'>
                    {renderRange()}
                </Typography>

                <IconButton
                    id={id + '.next-page-id'}
                    onClick={handleNextButtonClick}
                    disabled={page >= Math.ceil(count / pageSize) - 1}
                    aria-label={messages.get(messages.NEXT_PAGE)}
                    data-part='nextPage'
                    size='large'>
                    <KeyboardArrowRight fontSize='small' />
                </IconButton>
                <IconButton
                    id={id + '.last-page-id'}
                    onClick={handleLastPageButtonClick}
                    disabled={page >= Math.ceil(count / pageSize) - 1}
                    aria-label={messages.get(messages.LAST_PAGE)}
                    data-part='lastPage'
                    size='large'>
                    <LastPageIcon fontSize='small' />
                </IconButton>
            </Stack>
        </Grid2>
        <Grid2 size={{ xs: 12, sm: 6, md: 3 }} id={id + '.page-size-id'} className={classes.pageSize}>
            <SelectField label={messages.get(messages.ITEMS_PER_PAGE)}
                value={pageSize}
                options={options}
                onChange={handlePageSizeChange}
                variant='standard'
            />
        </Grid2>
        <Grid2 size={{ xs: 12, sm: 6, md: 3 }} id={id + '.grid-cols-id'} className={classes.pageSize}>
            <SelectField label={messages.get(messages.COLUMNS) + ':'}
                disabled={!isGridView}
                value={pageColumns}
                options={GridPagingOptions.COLUMN_OPTIONS.map(cols => ({
                    label: cols,
                    value: cols
                }))}
                onChange={event => onColumnChange(event.target.value)}
                variant='standard' />
        </Grid2>
    </>
    );
}

export default GalleryPaging;
