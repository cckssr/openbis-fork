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

import autoBind from 'auto-bind'
import Container from '@src/js/components/common/form/Container.jsx'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableRow from '@mui/material/TableRow'
import Header from '@src/js/components/common/form/Header.jsx'
import ItemIcon from '@src/js/components/common/data-browser/ItemIcon.jsx'
import withStyles from '@mui/styles/withStyles';
import messages from '@src/js/common/messages.js'
import {timeToString} from "@src/js/components/common/data-browser/DataBrowserUtils.js";

const styles = () => ({
  container: {
    position: 'sticky',
    overflowX: 'hidden',
    overflowY: 'auto',
    width: '24rem'
  },
  icon: {
    verticalAlign: 'middle',
    fontSize: '12rem'
  },
  fileName: {
    whiteSpace: 'nowrap',
    '& *': {
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }
})

class InfoPanel extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)
  }

  render() {
    const {
      classes,
      selectedFile,
      configuration
    } = this.props

    return (selectedFile &&
      <Container className={classes.container}>
        <span className={classes.fileName}>
          <Header size='big'>{selectedFile.name}</Header>
        </span>
        <ItemIcon file={selectedFile} classes={classes} configuration={configuration} />
        <Table>
          <TableBody>
            <TableRow>
              <TableCell variant='head' component='th'>{messages.get(messages.SIZE)}</TableCell>
              <TableCell>{selectedFile.size}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell variant='head' component='th'>{messages.get(messages.MODIFIED)}</TableCell>
              <TableCell>{timeToString(selectedFile.lastModifiedTime)}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Container>
    )
  }
}

export default withStyles(styles)(InfoPanel)
