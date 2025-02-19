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
import Grid from '@mui/material/Grid'
import autoBind from 'auto-bind'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  container: {
    fontFamily: theme.typography.fontFamily,
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(8rem, 1fr))',
    gridGap: '0.5rem'
  }
});

class GridView extends React.Component {
  constructor(props, context) {
    super(props, context);
    autoBind(this);
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

  render() {
    logger.log(logger.DEBUG, 'GridView.render')
    const {
      classes,
      files,
      selectedFile,
      multiselectedFiles
    } = this.props

    return (
      <Grid container className={classes.container}>
        {files.map((file, index) => (
          <GridViewItem
            key={index}
            {...this.props}
            selected={selectedFile === file}
            multiselected={multiselectedFiles.has(file)}
            file={file}
            onClick={this.handleClick}
            onSelect={this.handleSelect}
            onMultiselect={this.handleMultiselect}
          />
        ))}
      </Grid>
    );
  }
}

export default withStyles(styles)(GridView);
