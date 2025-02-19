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
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import { ListViewItem } from '@src/js/components/common/data-browser/ListViewItem.jsx'
import logger from '@src/js/common/logger.js'

const styles = (theme) => ({
  content: {
    width: '100%',
    borderSpacing: '0',
    fontFamily: theme.typography.fontFamily,
    '& thead > tr > th': {
      fontWeight: 'bold'
    },
    '& tbody > tr': {
      cursor: 'pointer',
      '&:hover': {
        backgroundColor: '#0000000a'
      }
    },
  },
  tableHeader: {
    textAlign: 'left'
  },
  icon: {
    fontSize: '2.5rem'
  },
  text: {
    fontSize: theme.typography.body2.fontSize,
    lineHeight: theme.typography.body2.fontSize
  },
  listContainer: {
    flex: '1 1 100%'
  },
  modifiedColumn: {
    width: '11rem',
    textAlign: 'right'
  },
  tableRow: {
    fontSize: theme.typography.body1.fontSize,
    height: '2rem'
  },
  tableData: {
    padding: theme.spacing(2),
    borderWidth: '0'
  },
})

class ListView extends React.Component {

  render() {
    logger.log(logger.DEBUG, 'ListView.render')
    const { classes, files, configuration } = this.props

    /* Create strings in messages. */
    return (
      <TableContainer>
        <Table className={classes.content}>
          <TableHead>
            <TableRow className={classes.tableRow}>
              <TableCell className={`${classes.tableData} ${classes.tableHeader}`}>Name</TableCell>
              <TableCell className={`${classes.tableData} ${classes.tableHeader}`}>Size</TableCell>
              <TableCell className={`${classes.tableData} ${classes.modifiedColumn} ${classes.tableHeader}`}>Modified</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {files.map((file, index) =>
              <ListViewItem key={index} {...this.props} file={file} />
            )}
          </TableBody>
        </Table>
      </TableContainer>
    )
  }
}

export default withStyles(styles)(ListView)
