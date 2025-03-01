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
import React from 'react';
import withStyles from '@mui/styles/withStyles';
import autoBind from 'auto-bind';
import logger from '@src/js/common/logger.js';
import InfoToggleButton from '@src/js/components/common/data-browser/components/toolbar/InfoToggleButton.jsx';
import ViewSwitch from '@src/js/components/common/data-browser/components/toolbar/ViewSwitch.jsx';
import UploadSection from '@src/js/components/common/data-browser/components/toolbar/UploadSection.jsx';


const color = 'default'
const iconButtonSize = 'medium'

const styles = theme => ({
  buttons: {
    flex: '0 0 auto',
    display: 'flex',
    alignItems: 'center',
    whiteSpace: 'nowrap',
    '&>button': {
      marginRight: theme.spacing(1)
    },
    '&>button:nth-last-child(1)': {
      marginRight: 0
    }
  },
  uploadButtonsContainer: {
    display: 'flex',
    flexDirection: 'column',
    '&>button': {
      marginBottom: theme.spacing(1)
    },
    '&>button:nth-last-child(1)': {
      marginBottom: 0
    }
  },
  toggleButton: {
    border: 'none',
    borderRadius: '50%',
    display: 'inline-flex',
    padding: theme.spacing(1.5),
    '& *': {
      color: theme.palette[color].main
    }
  }
})

class RightToolbar extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)
  }

  render() {
    const {
      classes,
      onViewTypeChange,
      buttonSize,
      editable,
      viewType,
      selected,
      controller,
      onChange,
      afterUpload
    } = this.props

    return (
      <div className={classes.buttons}>
        <InfoToggleButton
          selected={selected}
          onChange={onChange}
          buttonSize={buttonSize}
          classes={classes}
        />
        <ViewSwitch
          viewType={viewType}
          onViewTypeChange={onViewTypeChange}
          buttonSize={buttonSize}
          classes={classes}
        />
        <UploadSection
          classes={classes}
          buttonSize={buttonSize}
          editable={editable}
          controller={controller}
          afterUpload={afterUpload}
        />
      </div>
    )
  }
}

export default withStyles(styles)(RightToolbar)