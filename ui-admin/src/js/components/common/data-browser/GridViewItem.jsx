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
import React from 'react';
import withStyles from '@mui/styles/withStyles';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import { CardContent, CardMedia } from '@mui/material';
import ItemIcon from '@src/js/components/common/data-browser/ItemIcon.jsx';
import autoBind from 'auto-bind';


const styles = (theme) => ({
  // Updated gridCard: takes full width of its grid cell and is borderless.
  gridCard: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'flex-start',
    position: 'relative',
    width: '100%', // Fill the grid cell width
    height: '9.5rem',    
    overflow: 'hidden',    
    textAlign: 'center',
    boxShadow: 'none',
    border: 'none', // Remove border
    borderRadius: theme.shape.borderRadius,
    transition: theme.transitions.create('background-color', {
      duration: theme.transitions.duration.short,
    }),
    '&:hover': {
      backgroundColor: theme.palette.action.hover,
    },
  },
  // Additional classes to handle dynamic props:
  clickable: {
    cursor: 'pointer',
  },
  selected: {
    backgroundColor: '#e8f7fd',
    
    boxShadow: `0 0 5px ${theme.palette.primary.main}`,
  },
  // Updated cardContent style for file name.
  cardContent: {
    padding: theme.spacing(0.5),
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,
    color: theme.palette.text.primary, 
    letterSpacing: "normal",
    maxWidth: '100%',
    wordBreak: 'break-word',
    overflowWrap: 'anywhere',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    display: '-webkit-box',
    WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical',
    whiteSpace: 'normal',    
    height: '3.6em',
  },
  body2: {
    ...theme.typography.body2, 
  },
  cardMedia: {
    marginTop: theme.spacing(2)
  }

});

class GridViewItem extends React.Component {
  constructor(props, context) {
    super(props, context);
    autoBind(this);
  }

  handleClick(event) {
    const { onClick, onSelect, row } = this.props;
    //onClick(row);
    onSelect(row);
  }

  handleDoubleClick() {
    const { doubleClickable, onDoubleClick, row } = this.props

    if (onDoubleClick) {
      onDoubleClick(row)
    }
  }

  truncateFilename(filename, maxLength = 50) {
    if (filename.length <= maxLength) return { text: filename, isTruncated: false };
  
    const extIndex = filename.lastIndexOf(".");
    const name = extIndex !== -1 ? filename.slice(0, extIndex) : filename;
    const ext = extIndex !== -1 ? filename.slice(extIndex) : "";
  
    const visibleChars = maxLength - ext.length - 4; 
    const start = name.slice(0, visibleChars);
    const end = name.slice(-4);
  
    return { text: `${start}...${end}${ext}`, isTruncated: true };
  }
  


  render() {
    const { classes, row, configuration, clickable, multiselected} = this.props;

    // Build a list of classes based on props
    let classNames = [classes.gridCard];
    if (clickable) {
      classNames.push(classes.clickable);
    }
    if (multiselected) {
      classNames.push(classes.selected);
    }
    const { text, isTruncated } = this.truncateFilename(row.name);
    
    return (      
      <Grid
        item
        component={Card}
        className={classNames.join(' ')}
        onClick={this.handleClick}
        onDoubleClick={this.handleDoubleClick}
      > 
        <div className={classes.cardMedia}>
          <CardMedia component="div">
            <ItemIcon file={row} configuration={configuration} />
          </CardMedia>
        </div>
        <CardContent 
            className={[classes.cardContent, classes.body2].join(' ')} 
            {...(isTruncated ? { title: row.name } : {})}
          >
            {text}
          </CardContent>
      </Grid>
    );
  }
}

export default withStyles(styles)(GridViewItem);

