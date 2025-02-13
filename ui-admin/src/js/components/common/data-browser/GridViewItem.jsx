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
import Grid from '@mui/material/Grid'
import Card from '@mui/material/Card'
import { CardContent, CardMedia } from '@mui/material'
import ItemIcon from '@src/js/components/common/data-browser/ItemIcon.jsx'
import autoBind from 'auto-bind';

const styles = (theme) => ({
  cell: {
    display: 'block',
    position: 'relative',
    width: '8rem',
    height: '8rem',
    overflow: 'hidden',
    margin: '0.25rem',
    textAlign: 'center',
    '&:hover': {
      backgroundColor: '#0000000a'
    }
  },
  name: {
    padding: '0'
  },
  clickable: {
    cursor: 'pointer'
  },
  selectable: {
    cursor: 'pointer'
  },
  selected: {
    backgroundColor: '#e8f7fd'
  }
})

class GridViewItem extends React.Component {


  constructor(props, context) {
    super(props, context);
    autoBind(this);
  }

  handleClick(event) {
    const { onClick, onSelect, file } = this.props;

    onClick(event, file);
    onSelect(event, file);
  };

  render() {
    const { classes,
      file,
      configuration,
      clickable,
      selectable,
      multiselectable,
      selected
    } = this.props

    let itemClasses = [classes.cell]

    if (multiselectable) {
      itemClasses.push(classes.multiselectable)
    }
    if (selectable) {
      itemClasses.push(classes.selectable)
    }
    if (selected) {
      itemClasses.push(classes.selected)
    }
    if (clickable) {
      itemClasses.push(classes.clickable)
    }

    return (
      <Grid
        item
        component={Card}
        variant='outlined'
        className={itemClasses.join(' ')}
        onClick={this.handleClick}
      >
        <CardMedia>
          <ItemIcon file={file} configuration={configuration} />
        </CardMedia>
        <CardContent className={classes.name}>{file.name}</CardContent>
      </Grid>
    )
  }
}

export default withStyles(styles)(GridViewItem)
