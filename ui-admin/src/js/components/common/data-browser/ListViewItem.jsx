/*
 * Copyright ETH 2023 Zürich, Scientific IT Services
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
 */

import React from 'react'
import TableRow from '@mui/material/TableRow'
import TableCell from '@mui/material/TableCell'
import ItemIcon from '@src/js/components/common/data-browser/ItemIcon.jsx'
import withStyles from '@mui/styles/withStyles';

const styles = (theme) => ({
  nameColumn: {
    textAlign: 'left'
  },
  sizeColumn: {
    width: '11rem',
    textAlign: 'left'
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
  selected: {
    backgroundColor: '#e8f7fd'
  },
})

export class ListViewItem extends React.Component {
  render() {
    const { classes, file, configuration } = this.props

    return <TableRow className={classes.tableRow}>
      <TableCell className={`${classes.tableData} ${classes.nameColumn}`}>
        {<><ItemIcon classes={classes} file={file} configuration={configuration} />{file.name}</>}
      </TableCell>
      <TableCell
        className={`${classes.tableData} ${classes.sizeColumn}`}>{file.folder ? '-' : file.size}</TableCell>
      <TableCell
        className={`${classes.tableData} ${classes.modifiedColumn}`}>{file.lastModifiedTime.toLocaleString()}</TableCell>
    </TableRow>
  }
}

export default withStyles(styles)(ListViewItem)