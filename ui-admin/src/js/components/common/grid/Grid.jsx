import _ from 'lodash'
import React from 'react'
import autoBind from 'auto-bind'
import withStyles from '@mui/styles/withStyles';
import { alpha } from '@mui/material/styles';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import Loading from '@src/js/components/common/loading/Loading.jsx'
import Table from '@mui/material/Table'
import TableHead from '@mui/material/TableHead'
import TableBody from '@mui/material/TableBody'
import Header from '@src/js/components/common/form/Header.jsx'
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx'
import GridController from '@src/js/components/common/grid/GridController.js'
import GridFilters from '@src/js/components/common/grid/GridFilters.jsx'
import GridHeaders from '@src/js/components/common/grid/GridHeaders.jsx'
import GridSelectionInfo from '@src/js/components/common/grid/GridSelectionInfo.jsx'
import GridRow from '@src/js/components/common/grid/GridRow.jsx'
import GridRowFullWidth from '@src/js/components/common/grid/GridRowFullWidth.jsx'
import GridExports from '@src/js/components/common/grid/GridExports.jsx'
import GridExportLoading from '@src/js/components/common/grid/GridExportLoading.jsx'
import GridExportWarnings from '@src/js/components/common/grid/GridExportWarnings.jsx'
import GridExportError from '@src/js/components/common/grid/GridExportError.jsx'
import GridPaging from '@src/js/components/common/grid/GridPaging.jsx'
import GridColumnsConfig from '@src/js/components/common/grid/GridColumnsConfig.jsx'
import GridFiltersConfig from '@src/js/components/common/grid/GridFiltersConfig.jsx'
import ComponentContext from '@src/js/components/common/ComponentContext.js'
import logger from '@src/js/common/logger.js'
import messages from '@src/js/common/messages.js'


const styles = theme => ({
  gridWrapper: {
    flex: '1 1 auto',
    position: 'relative',
    display: 'flex',
    flexDirection: 'column',
  },
  container: {
    width: '100%',
    flex: '1 1 auto',
    display: 'block',
    width: '100%',
    overflowY: 'auto',
    maxHeight: 'calc(100vh - (' + theme.spacing(36) + ' ))',
  },
  loadingContainer: {
    flex: '1 1 auto'
  },
  loading: {
    display: 'inline-block'
  },
  tableContainer: {
    display: 'inline-block',
    minWidth: '100%',
    height: '100%'
  },
  table: {
    borderCollapse: 'unset'
  },
  tableHead: {
    position: 'sticky',
    top: 0,
    zIndex: '200',
    backgroundColor: theme.palette.background.paper
  },
  titleCell: {
    border: 0,
    maxWidth: '80%'
  },
  titleContent: {
    paddingLeft: theme.spacing(2)
  },
  title: {
    paddingTop: theme.spacing(1),
    paddingBottom: 0
  },
  pagingAndConfigsAndExportsContent: {
    display: 'flex'
  },
  //drag overlay
  overlay: {
    position: 'absolute',
    inset: 0,
    backgroundColor: alpha(theme.palette.primary.main, 0.1),
    display: 'flex',
    alignItems: 'flex-end',
    paddingBottom: theme.spacing(1),
    justifyContent: 'center',
    boxShadow: `0 0 10px ${alpha(theme.palette.primary.main, 0.2)}`,
    zIndex: 9999
  },
  dropContentWrapper: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: theme.spacing(2)
  },

  cloudIcon: {
    fontSize: '3.5rem',
    color: theme.palette.primary.main,
    animation: `$floatUp 0.6s ease-in-out 10`
  },

  // Define the keyframes
  '@keyframes floatUp': {
    '0%, 100%': {
      transform: 'translateY(0)'
    },
    '50%': {
      transform: 'translateY(-10px)' // up 10px
    }
  },

  dropPill: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.palette.primary.main,
    color: theme.palette.common.white,
    borderRadius: '9999px',
    padding: theme.spacing(2, 4),
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,
    boxShadow: theme.shadows[4],
  },

  driveIcon: {
    marginRight: theme.spacing(1),
    fontSize: '1.4rem'
  }

})

class Grid extends React.PureComponent {
  static defaultProps = {
    id: 'grid'
  }

  constructor(props) {
    super(props)
    autoBind(this)

    this.state = {}

    if (this.props.controller) {
      this.controller = this.props.controller
    } else {
      this.controller = new GridController()
    }

    this.controller.init(new ComponentContext(this))

    if (this.props.controllerRef) {
      this.props.controllerRef(this.controller)
    }

  }

  componentDidMount() {
    this.controller.load()
  }

  handleClickContainer() {
    this.controller.handleRowSelect(null)
  }

  handleClickTable(event) {
    event.stopPropagation()
  }

  render() {
    logger.log(logger.DEBUG, 'Grid.render')

    if (!this.state.loaded) {
      return <Loading loading={true}></Loading>
    }

    const { id, classes, showHeaders, isDragging } = this.props
    const { loading, rows } = this.state
    const doShowHeaders = typeof showHeaders === 'boolean' ? showHeaders : true

    return (
      <div className={[classes.gridWrapper, classes.container].join(' ')}>
        <div
          id={id}
          onClick={this.handleClickContainer}>
          <div className={classes.loadingContainer}>
            <Loading loading={loading} styles={{ root: classes.loading }}>
              <div className={classes.tableContainer}>
                <Table
                  classes={{ root: classes.table }}
                  onClick={this.handleClickTable}
                >
                  <TableHead classes={{ root: classes.tableHead }}>
                    {this.renderTitle()}
                    {this.renderPagingAndConfigsAndExports()}
                    {doShowHeaders && this.renderHeaders()}
                    {this.renderFilters()}
                    {this.renderSelectionInfo()}
                  </TableHead>
                  <TableBody>
                    {rows.map(row => {
                      return this.renderRow(row)
                    })}
                  </TableBody>
                </Table>
              </div>
              {this.renderExportState()}
            </Loading>
          </div>
        </div>
        {isDragging && (
          <div className={classes.overlay}>
            <div className={classes.dropContentWrapper}>
              <CloudUploadIcon className={classes.cloudIcon} />
              <div className={classes.dropPill}>
                {messages.get(messages.UPLOAD_DRAG_MESSAGE)}
              </div>
            </div>
          </div>
        )}
        {this.renderConfirmSelectAllPages()}
      </div>

    )
  }

  renderTitle() {
    const { header, multiselectable, classes } = this.props

    if (header === null || header === undefined) {
      return null
    }

    const visibleColumns = this.controller.getVisibleColumns()

    return (
      <GridRowFullWidth
        multiselectable={multiselectable}
        columns={visibleColumns}
        styles={{ cell: classes.titleCell, content: classes.titleContent }}
      >
        <div onClick={this.handleClickContainer}>
          <Header styles={{ root: classes.title }}>{header}</Header>
        </div>
      </GridRowFullWidth>
    )
  }

  renderPagingAndConfigsAndExports() {
    const { multiselectable, classes, showPaging, showConfigs } = this.props
    const doShowPaging = typeof showPaging === 'boolean' ? showPaging : true
    const doShowConfigs = typeof showConfigs === 'boolean' ? showConfigs : true

    const visibleColumns = this.controller.getVisibleColumns()

    return (
      <GridRowFullWidth
        multiselectable={multiselectable}
        columns={visibleColumns}
        styles={{
          content: classes.pagingAndConfigsAndExportsContent
        }}
      >
        {doShowPaging && this.renderPaging()}
        {doShowConfigs && this.renderConfigs()}
        {this.renderExports()}
      </GridRowFullWidth>
    )
  }

  renderPaging() {
    const { id, showRowsPerPage } = this.props
    const { page, pageSize, totalCount } = this.state

    return (
      <GridPaging
        id={id}
        count={totalCount}
        page={page}
        pageSize={pageSize}
        showRowsPerPage={showRowsPerPage}
        onPageChange={this.controller.handlePageChange}
        onPageSizeChange={this.controller.handlePageSizeChange}
      />
    )
  }

  renderConfigs() {
    const { id, filterModes } = this.props
    const { loading, filterMode, columnsVisibility } = this.state

    const allColumns = this.controller.getAllColumns()

    return (
      <React.Fragment>
        <GridColumnsConfig
          id={id}
          columns={allColumns}
          columnsVisibility={columnsVisibility}
          loading={loading}
          onVisibleChange={this.controller.handleColumnVisibleChange}
          onOrderChange={this.controller.handleColumnOrderChange}
        />
        <GridFiltersConfig
          id={id}
          filterModes={filterModes}
          filterMode={filterMode}
          loading={loading}
          onFilterModeChange={this.controller.handleFilterModeChange}
        />
      </React.Fragment>
    )
  }

  renderExports() {
    const { id, multiselectable } = this.props
    const { rows, multiselectedRows, exportOptions } = this.state

    const exportable = this.controller.getExportable()

    if (!exportable) {
      return null
    }

    const visibleColumns = this.controller.getVisibleColumns()
    const multiselectLimit = this.controller.getMultiselectLimit()
    const totalCount = this.controller.getTotalCount()

    return (
      <GridExports
        id={id}
        disabled={rows.length === 0}
        exportable={exportable}
        exportOptions={exportOptions}
        multiselectable={multiselectable}
        multiselectedRows={multiselectedRows}
        multiselectLimit={multiselectLimit}
        visibleColumns={visibleColumns}
        totalCount={totalCount}
        onExport={this.controller.handleExport}
        onExportOptionsChange={this.controller.handleExportOptionsChange}
      />
    )
  }

  renderExportState() {
    const { exportState } = this.state

    if (!exportState) {
      return null
    }

    return (
      <React.Fragment>
        <GridExportLoading loading={!!exportState.loading} />
        <GridExportError
          open={!_.isEmpty(exportState.error)}
          error={exportState.error}
          onClose={this.controller.handleExportCancel}
        />
        <GridExportWarnings
          open={!_.isEmpty(exportState.warnings)}
          warnings={exportState.warnings}
          onDownload={() =>
            this.controller.handleExportDownload(
              exportState.fileName,
              exportState.fileUrl
            )
          }
          onCancel={this.controller.handleExportCancel}
        />
      </React.Fragment>
    )
  }

  renderHeaders() {
    const { multiselectable } = this.props
    const { sortings, rows, multiselectedRows } = this.state

    const visibleColumns = this.controller.getVisibleColumns()

    return (
      <GridHeaders
        columns={visibleColumns}
        rows={rows}
        sortings={sortings}
        onSortChange={this.controller.handleSortChange}
        onMultiselectAllRowsChange={
          this.controller.handleMultiselectAllRowsChange
        }
        multiselectable={multiselectable}
        multiselectedRows={multiselectedRows}
      />
    )
  }

  renderFilters() {
    const { id, filterModes, multiselectable } = this.props
    const { filterMode, filters, globalFilter } = this.state

    const visibleColumns = this.controller.getVisibleColumns()

    return (
      <GridFilters
        id={id}
        columns={visibleColumns}
        filterModes={filterModes}
        filterMode={filterMode}
        filters={filters}
        onFilterChange={this.controller.handleFilterChange}
        onFilterModeChange={this.controller.handleFilterModeChange}
        globalFilter={globalFilter}
        onGlobalFilterChange={this.controller.handleGlobalFilterChange}
        multiselectable={multiselectable}
      />
    )
  }

  renderSelectionInfo() {
    const { multiselectable, actions } = this.props
    const { rows, multiselectedRows } = this.state

    const visibleColumns = this.controller.getVisibleColumns()
    const multiselectLimit = this.controller.getMultiselectLimit()

    return (
      <GridSelectionInfo
        columns={visibleColumns}
        rows={rows}
        actions={actions}
        onExecuteAction={this.controller.handleExecuteAction}
        onMultiselectionClear={this.controller.handleMultiselectionClear}
        onSelectAllPages={this.controller.handleSelectAllPages}
        multiselectable={multiselectable}
        multiselectedRows={multiselectedRows}
        multiselectLimit={multiselectLimit}
      />
    )
  }

  renderRow(row) {
    const { selectable, multiselectable, onRowClick, onRowDoubleClick } = this.props
    const { selectedRow, multiselectedRows, heights } = this.state

    const visibleColumns = this.controller.getVisibleColumns()

    return (
      <GridRow
        key={row.id}
        columns={visibleColumns}
        row={row}
        heights={heights[row.id]}
        clickable={!!onRowClick}
        doubleClickable={!!onRowDoubleClick}
        selectable={selectable}
        selected={selectedRow ? selectedRow.id === row.id : false}
        multiselectable={multiselectable}
        multiselected={multiselectedRows && multiselectedRows[row.id]}
        onClick={this.controller.handleRowClick}
        onDoubleClick={this.controller.handleRowDoubleClick}
        onSelect={this.controller.handleRowSelect}
        onMultiselect={this.controller.handleRowMultiselect}
        onMeasured={this.controller.handleMeasured}
      />
    )
  }

  renderConfirmSelectAllPages() {
    const { confirmSelectAllPagesOpen, totalCount } = this.state
    const multiselectLimit = this.controller.getMultiselectLimit()

    let content = null
    if (totalCount <= multiselectLimit) {
      content = messages.get(messages.CONFIRMATION_SELECT_ALL_PAGES, totalCount)
    } else {
      content = messages.get(messages.CONFIRMATION_SELECT_ALL_PAGES_WITH_LIMIT, multiselectLimit)
    }

    return (<ConfirmationDialog
      open={confirmSelectAllPagesOpen}
      onConfirm={this.controller.handleConfirmSelectAllPages}
      onCancel={this.controller.handleCancelSelectAllPages}
      title={messages.get(messages.SELECT_ALL_PAGES)}
      content={content}
    />)
  }
}

export default withStyles(styles)(Grid)
