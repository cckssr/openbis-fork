/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
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
import { ToggleButton } from '@mui/material'
import InfoIcon from '@mui/icons-material/InfoOutlined'
import messages from '@src/js/common/messages.js'

const InfoToggleButton = ({ classes, buttonSize, selected, onChange, className }) => (
  <ToggleButton
    classes={{ root: classes.toggleButton }}
    className={className} 
    color="default"
    size={buttonSize}
    selected={selected}
    onChange={onChange}
    onClick={(event) => event.currentTarget.blur()} // somehow adding the external bootstrap classes, messed up with mui default behaviour
    value={messages.get(messages.INFO)}
    aria-label={messages.get(messages.INFO)}
  >
    <InfoIcon />
  </ToggleButton>
)

export default InfoToggleButton
