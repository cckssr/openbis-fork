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
import autoBind from 'auto-bind'
import logger from "@src/js/common/logger.js";
import Container from "@src/js/components/common/form/Container.jsx";
import HomeIcon from "@mui/icons-material/Home";
import IconButton from "@mui/material/IconButton";
import Button from "@mui/material/Button";

const color = 'default'
const buttonSize = 'small'
const iconButtonSize = 'medium'

const styles = theme => ({
  containerDefault: {
    flex: '0 0 auto',
    display: 'flex',
    whiteSpace: 'nowrap',
    fontSize: '1.125rem',
    padding: '0 0',
    alignItems: 'center',
    '&>*': {
      minWidth: 'auto',
      height: theme.spacing(4),
      fontSize: '1.125rem',
      textTransform: 'none',
      padding: '0 ' + theme.spacing(1),
      margin: theme.spacing(0.5)
    },
    '&>*:first-child':  {
      padding: '0 0',
      margin: theme.spacing(1) + ' ' + theme.spacing(2) + ' ' +
        theme.spacing(1) + ' ' + theme.spacing(1)
    },
    '&>*:first-child svg': {
      width: theme.spacing(4),
      height: theme.spacing(4)
    },
    '&>*:not(:first-child):last-child':  {
      pointerEvents: 'none',
      color: 'inherit',
      fontWeight: 'bold'
    }
  },
  homeButton: {
    marginRight: '1rem'
  }
})

class NavigationBar extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)
  }

  splitPath(path) {
    const folders = path.split('/').filter((folder) => folder.length > 0)
    let paths = new Array(folders.length)

    if (paths.length > 0) {
      paths[0] = '/' + folders[0]
      for (let i = 1; i < paths.length; i++) {
        paths[i] = paths[i - 1] + '/' + folders[i]
      }
    }

    return { folders, paths }
  }

  renderItems() {
    const { classes, path, onPathChange } = this.props
    const { folders, paths } = this.splitPath(path)
    const components = new Array(2 * paths.length + 1)

    components[0] = <IconButton
          key='root'
          classes={{ root: classes.button }}
          color='secondary'
          size={iconButtonSize}
          variant='outlined'
          onClick={() => onPathChange('/')}
          disabled={paths.length === 0}
        >
      <HomeIcon />
    </IconButton>
    for (let i = 0; i < paths.length; i++) {
      components[2 * i + 1] = '/'
      components[2 * i + 2] = <Button
        key={'path-' + i}
        classes={{ root: classes.button }}
        color={color}
        size={buttonSize}
        variant='text'
        onClick={() => onPathChange(paths[i])}
      >
        {folders[i]}
      </Button>
    }

    return components.length === 1
      ? [components[0], '/']
      : components
  }

  render() {
    logger.log(logger.DEBUG, 'NavigationBar.render')
    const { classes } = this.props

    return (
      <Container classes={{ containerDefault: classes.containerDefault }}>
        { this.renderItems() }
      </Container>
    )
  }
}

export default withStyles(styles)(NavigationBar)
