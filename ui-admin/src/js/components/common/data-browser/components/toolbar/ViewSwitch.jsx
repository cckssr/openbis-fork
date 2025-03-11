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
import IconButton from '@mui/material/IconButton'
import ViewComfyIcon from '@mui/icons-material/ViewComfy'
import ViewListIcon from '@mui/icons-material/ViewList'
import messages from '@src/js/common/messages.js'

const iconButtonSize = 'medium'
const color = 'default'

const ViewSwitch = ({ viewType, onViewTypeChange, classes,className }) => {
  return viewType === 'list' ? (
    <IconButton
      classes={{ root: classes.button }}
      className={className}
      color={color}
      size={iconButtonSize}
      variant="outlined"
      onClick={(event) => { 
        onViewTypeChange('grid')
        event.currentTarget.blur() // somehow adding the external bootstrap classes, messed up with mui default behaviour
      }}
    >
      <ViewComfyIcon />
    </IconButton>
  ) : (
    <IconButton
      classes={{ root: classes.button }}
      className={className}
      color={color}
      size={iconButtonSize}
      variant="outlined"
      onClick={(event) => {
        onViewTypeChange('list')
        event.currentTarget.blur() // somehow adding the external bootstrap classes, messed up with mui default behaviour
      }}
    >
      <ViewListIcon />
    </IconButton>
  )
}

export default ViewSwitch
