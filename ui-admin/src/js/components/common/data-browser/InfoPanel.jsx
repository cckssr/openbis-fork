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
import IconButton from '@mui/material/IconButton'
import Tooltip from '@src/js/components/common/form/Tooltip.jsx'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import Header from '@src/js/components/common/form/Header.jsx'
import ItemIcon from '@src/js/components/common/data-browser/ItemIcon.jsx'
import withStyles from '@mui/styles/withStyles';
import messages from '@src/js/common/messages.js'
import {timeToString, sizeToString} from "@src/js/components/common/data-browser/DataBrowserUtils.js";

const SFTP_SERVER_PORT = 2222

const styles = theme => ({
  container: {
    position: 'sticky',
    overflowX: 'hidden',
    overflowY: 'auto',
    width: '28rem'
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: theme.spacing(1)
  },
  icon: {
    flex: '0 0 auto',
    verticalAlign: 'middle',
    fontSize: '3rem',
    marginRight: theme.spacing(1.5)
  },
  fileName: {
    flex: '1 1 auto',
    minWidth: 0,
    whiteSpace: 'nowrap',
    '& *': {
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  },
  labelCell: {
    whiteSpace: 'nowrap'
  },
  sftpLinkCell: {
    maxWidth: 0,
    width: '100%'
  },
  sftpLinkRow: {
    display: 'flex',
    alignItems: 'center',
    minWidth: 0
  },
  sftpLink: {
    display: 'block',
    flex: '1 1 auto',
    minWidth: 0,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  sftpLinkCopyButton: {
    flex: '0 0 auto'
  }
})

class InfoPanel extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)
    this.state = {
      sftpUrl: null,
      sftpLoading: false,
      sftpCopied: false
    }
  }

  componentDidMount() {
    this.loadSftpUrl()
  }

  componentDidUpdate(prevProps) {
    const prevPath = prevProps.selectedFile && prevProps.selectedFile.path
    const currentPath = this.props.selectedFile && this.props.selectedFile.path
    if (prevPath !== currentPath) {
      this.loadSftpUrl()
    }
  }

  async loadSftpUrl() {
    const { controller, selectedFile } = this.props

    if (!controller || !selectedFile) {
      this.setState({ sftpUrl: null, sftpLoading: false, sftpCopied: false })
      return
    }

    this.setState({ sftpUrl: null, sftpLoading: true, sftpCopied: false })

    try {
      const host = window.location.hostname
      const url = await controller.getSftpUrl(host, SFTP_SERVER_PORT, selectedFile.path)
      this.setState({ sftpUrl: url, sftpLoading: false })
    } catch (error) {
      this.setState({ sftpUrl: null, sftpLoading: false })
    }
  }

  async handleCopySftpUrl() {
    const { sftpUrl } = this.state
    if (!sftpUrl) {
      return
    }
    try {
      await navigator.clipboard.writeText(sftpUrl)
      this.setState({ sftpCopied: true })
    } catch (error) {
      // clipboard access not available, link can still be copied manually from the field
    }
  }

  renderSftpLinkRow() {
    const { classes } = this.props
    const { sftpUrl, sftpLoading, sftpCopied } = this.state

    if (sftpLoading) {
      return (
        <TableRow>
          <TableCell variant='head' component='th' className={classes.labelCell}>{messages.get(messages.SFTP_LINK)}</TableCell>
          <TableCell>{messages.get(messages.SFTP_LINK_LOADING)}</TableCell>
        </TableRow>
      )
    }

    if (!sftpUrl) {
      return null
    }

    return (
      <TableRow>
        <TableCell variant='head' component='th' className={classes.labelCell}>{messages.get(messages.SFTP_LINK)}</TableCell>
        <TableCell className={classes.sftpLinkCell}>
          <div className={classes.sftpLinkRow}>
            <Tooltip title={sftpUrl}>
              <div className={classes.sftpLink}>{sftpUrl}</div>
            </Tooltip>
            <Tooltip title={sftpCopied ? messages.get(messages.SFTP_LINK_COPIED) : messages.get(messages.SFTP_LINK_COPY)}>
              <IconButton
                name='copy-sftp-link'
                size='small'
                className={classes.sftpLinkCopyButton}
                onClick={this.handleCopySftpUrl}
              >
                <ContentCopyIcon fontSize='small' />
              </IconButton>
            </Tooltip>
            <Tooltip title={sftpUrl}>
              <IconButton
                name='open-sftp-link'
                size='small'
                component='a'
                href={sftpUrl}
                className={classes.sftpLinkCopyButton}
              >
                <OpenInNewIcon fontSize='small' />
              </IconButton>
            </Tooltip>
          </div>
        </TableCell>
      </TableRow>
    )
  }

  render() {
    const {
      classes,
      selectedFile,
      configuration
    } = this.props

    return (selectedFile &&
      <Container className={classes.container}>
        <div className={classes.header}>
          <ItemIcon file={selectedFile} classes={classes} configuration={configuration} />
          <span className={classes.fileName}>
            <Header size='big'>{selectedFile.name}</Header>
          </span>
        </div>
        <Table>
          <TableBody>
            <TableRow>
              <TableCell variant='head' component='th' className={classes.labelCell}>{messages.get(messages.SIZE)}</TableCell>
              <TableCell>{sizeToString(selectedFile.size)}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell variant='head' component='th' className={classes.labelCell}>{messages.get(messages.MODIFIED)}</TableCell>
              <TableCell>{timeToString(selectedFile.lastModifiedTime)}</TableCell>
            </TableRow>
            {this.renderSftpLinkRow()}
          </TableBody>
        </Table>
      </Container>
    )
  }
}

export default withStyles(styles)(InfoPanel)
