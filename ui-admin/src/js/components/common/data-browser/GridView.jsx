/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import React from 'react'
import withStyles from '@mui/styles/withStyles';
import GridViewItem from '@src/js/components/common/data-browser/GridViewItem.jsx'
import Grid from '@mui/material/Grid2'
import autoBind from 'auto-bind'
import logger from '@src/js/common/logger.js'
import ComponentContext from '@src/js/components/common/ComponentContext.js'
import GridController from '@src/js/components/common/grid/GridController.js'
import Loading from '@src/js/components/common/loading/Loading.jsx'
import GridPaging from '@src/js/components/common/grid/GridPaging.jsx'
import Header from '@src/js/components/common/form/Header.jsx'
import SortDropdown from '@src/js/components/common/data-browser/SortDropdown.jsx'
import { Select, MenuItem, FormControl, InputLabel, IconButton, Box} from '@mui/material';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';


import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';

const styles = theme => ({
 // Outer container that wraps header and grid items.
  container: {
    width: '100%',
    display: 'block',
    color: theme.palette.text.primary, 
  },  
  header: {
    width: '100%',
    marginBottom: theme.spacing(1),
    color: theme.palette.text.primary, 
  },
  gridWrapper: {
    overflowX: 'auto',
    width: '100%',
  },
  // Grid items container using CSS grid.
  gridItems: {
    display: 'grid',
    width: '100%',
    gridTemplateColumns: 'repeat(auto-fill, minmax(9.5rem, 1fr))',
    gap: theme.spacing(3), 
    paddingBottom: theme.spacing(3),        
    overflowY: 'auto',
    overflowX: 'auto',
    maxHeight: `calc(100vh - ${theme.spacing(51)})`,
    padding: theme.spacing(2),
    minWidth: `calc(4 * 9.5rem + 2 * ${theme.spacing(3)})`,
  },
  

  titleCell: {
    border: 0,
    fontWeight: '500',
    maxWidth: '80%',
    color: theme.palette.text.primary, 
  },
  titleContent: {    
    paddingLeft: theme.spacing(2),
    lineHeight:'1.5'
  },
  title: {
    marginLeft:theme.spacing(2),
    paddingTop: 0,
    paddingBottom: 0
  },
  pagingAndConfigsAndExportsContent: {
    display: 'flex',
    alignItems: 'center', 
    justifyContent: 'space-between', 
    gap: theme.spacing(2), 
    width: '100%',
  }
  
});

class GridView extends React.Component {

  constructor(props) {
      super(props)
      autoBind(this)
  
      this.state = { sortingColumn: 'name' }
  
      if (this.props.controller) {
        this.controller = this.props.controller
      } else {
        this.controller = new GridController()
      }
  
      this.controller.init(new ComponentContext(this))
  
      if (this.props.controllerRef) {
        this.props.controllerRef(this.controller)
      }

      this.containerRef = React.createRef(); 
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

  handleClick(event, file) {
    const { clickable, onClick } = this.props;

    if (clickable && onClick) {
      onClick(file);
    }
  }

  handleSelect(event, file) {
    const { selectable, onSelect } = this.props;

    if (selectable && onSelect) {
      onSelect(file);
    }
  }

  handleMultiselect(event) {
    event.preventDefault();
    event.stopPropagation();

    const { multiselectable, onMultiselect, file } = this.props;

    if (multiselectable && onMultiselect) {
      onMultiselect(file);
    }
  }

  // 📌 Mouse Down - Starts Drag Selection
  handleMouseDown = (event) => {
    if (event.target.closest('.grid-item')) return;

    const { clientX, clientY } = event;
    this.setState({
      dragStart: { x: clientX, y: clientY },
      dragging: true,
      selectionBox: { x: clientX, y: clientY, width: 0, height: 0 }
    });

    document.addEventListener('mousemove', this.handleMouseMove);
    document.addEventListener('mouseup', this.handleMouseUp);
  };

  // 📌 Mouse Move - Updates Selection Box & Selected Items
  handleMouseMove = (event) => {
    if (!this.state.dragging) return;

    const { dragStart } = this.state;
    const { clientX, clientY } = event;

    const newBox = {
      x: Math.min(clientX, dragStart.x),
      y: Math.min(clientY, dragStart.y),
      width: Math.abs(clientX - dragStart.x),
      height: Math.abs(clientY - dragStart.y)
    };

    this.setState({ selectionBox: newBox });
    this.updateSelectedItems(newBox);
  };

  // 📌 Mouse Up - Ends Drag Selection
  handleMouseUp = () => {
    this.setState({ dragging: false, selectionBox: null });

    document.removeEventListener('mousemove', this.handleMouseMove);
    document.removeEventListener('mouseup', this.handleMouseUp);
  };

  // 📌 Updates Selected Items Based on Selection Box
  updateSelectedItems(selectionBox) {
    if (!this.containerRef.current) return;

    const newSelectedItems = new Set();
    const items = this.containerRef.current.querySelectorAll('.grid-item');

    items.forEach((item) => {
      const rect = item.getBoundingClientRect();
      if (
        rect.right > selectionBox.x &&
        rect.left < selectionBox.x + selectionBox.width &&
        rect.bottom > selectionBox.y &&
        rect.top < selectionBox.y + selectionBox.height
      ) {
        newSelectedItems.add(item.dataset.id);
      }
    });

    this.setState({ selectedItems: newSelectedItems });
  }

  render() {
    logger.log(logger.DEBUG, 'GridView.render')
    if (!this.state.loaded) {
      return <Loading loading={true}></Loading>
    }

    const { classes,item, ...otherProps } = this.props;    
    const { loading, rows, selectedRow, multiselectedRows} = this.state
    const { dragging, selectionBox, selectedItems } = this.state;
    return ( 
      <div className={classes.gridWrapper}>   
      <div className={classes.container}
          ref={this.containerRef} // Added container ref
          onMouseDown={this.handleMouseDown}>

          {/* Header/Paging/Configs on top */}
          <div className={classes.header}>
            {this.renderTitle()}
            {this.renderPagingAndConfigsAndExports()}
          </div>

         
            {/* Grid Items below */}
            <Grid container className={classes.gridItems}>
              {rows.map(row => {

                return <GridViewItem
                  key={row.id}
                  {...otherProps}
                  multiselected={multiselectedRows && multiselectedRows[row.id]}
                  row={row}                
                  onDoubleClick={this.controller.handleRowDoubleClick}
                  onSelect={this.controller.handleRowMultiselect}                
                />
              })}
            </Grid>
          
          {/* New Selection Box */}
          {dragging && selectionBox && (
            <div
              className={classes.selectionBox}
              style={{
                left: selectionBox.x,
                top: selectionBox.y,
                width: selectionBox.width,
                height: selectionBox.height
              }}
            />
          )}
      </div>
      </div>
    );
  }

    renderTitle() {
      const { header, multiselectable, classes } = this.props
  
      if (header === null || header === undefined) {
        return null
      }
  
      const visibleColumns = this.controller.getVisibleColumns()
  
      return (
        <div
          styles={{ cell: classes.titleCell, content: classes.titleContent }}
        >
          <div onClick={this.handleClickContainer}>
            <Header styles={{ root: classes.title }}>{header}</Header>
          </div>
        </div>
      )
    }


    renderPagingAndConfigsAndExports() {
      const { multiselectable, classes, showPaging, showConfigs } = this.props;
      const doShowPaging = typeof showPaging === 'boolean' ? showPaging : true;      
    
      return (
        <div className={classes.pagingAndConfigsAndExportsContent}>
          {doShowPaging && this.renderPaging()}   {/* Paging on the left */}
          {this.renderSortDropdown()}            {/* Sorting on the right */}          
        </div>
      );
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
    
       
      renderSortDropdown() {
        return (
          <SortDropdown columns={this.props.columns}
                onSortChange={(column, order) => this.controller.handleSortChange(column, order)} />
            )
      }
        
        

        renderGridSorting(){
          const { columns, onSortChange } = this.props
          const { sortings, rows, multiselectedRows } = this.state;
          return (
            <div style={{ display: 'grid', gridTemplateColumns: `repeat(${columns.length}, 1fr)`, padding: '10px' }}>
              {columns.map((column) => (
                <div 
                  key={column.id}
                  onClick={() => onSortChange(column)}
                  style={{ fontWeight: 'bold', cursor: 'pointer', textAlign: 'left' }}
                >
                  {column.name} {sortings[column.id] === 'asc' ? '▲' : '▼'}
                </div>
              ))}
            </div>
          );
        };



}

export default withStyles(styles)(GridView);
