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
import withStyles from '@mui/styles/withStyles'
import autoBind from 'auto-bind'
import RightToolbar from '@src/js/components/common/data-browser/components/toolbar/RightToolbar.jsx'
import DataBrowserToolbar from '@src/js/components/common/data-browser/components/toolbar/DataBrowserToolbar.jsx'
import LeftToolbar from '@src/js/components/common/data-browser/LeftToolbar.jsx'
import logger from '@src/js/common/logger.js'
import ResponsiveLeftToolbar from './ResponsiveLeftToolbar'

const buttonSize = 'small'

const styles = theme => ({
  toolbar: {
    flex: '0 0 auto',
    display: 'flex',
    whiteSpace: 'nowrap',
    marginLeft: theme.spacing(1),
    marginRight: theme.spacing(1),
    justifyContent: 'flex-start'
  },
  rightOnly: {
    justifyContent: 'flex-end'
  }
})

class Toolbar extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)

    this.controller = this.props.controller
  }

  render() {
    logger.log(logger.DEBUG, 'Toolbar.render')

    const {
      leftToolbar,
      viewType,
      onViewTypeChange,
      classes,
      showInfo,
      onShowInfoChange,            
      owner,      
      editable,      
      onSpaceStatusChange
    } = this.props
    
    const containerClass = leftToolbar
      ? classes.toolbar
      : `${classes.toolbar} ${classes.rightOnly}`

    return (
      <>
      <div className={containerClass}>
        {leftToolbar && (

          <ResponsiveLeftToolbar
            buttonSize={buttonSize}                        
            owner={owner}          
            extOpenbis={this.props.openBis}
          />
        )}
        <RightToolbar
          buttonSize={buttonSize}
          selected={showInfo}
          onChange={onShowInfoChange}
          viewType={viewType}
          editable={editable}
          onViewTypeChange={onViewTypeChange}
          controller={this.controller}
          afterUpload={onSpaceStatusChange}
        />
      </div>

      </>
    )
  }
}

export default withStyles(styles)(Toolbar)
