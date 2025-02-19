/*
 *  Copyright ETH 2024 Zürich, Scientific IT Services
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

import React from "react";
import NavigationBar from "@src/js/components/common/data-browser/NavigationBar.jsx";
import FreeSpaceBar from "@src/js/components/common/progress/FreeSpaceBar.jsx";
import * as PropTypes from "prop-types";
import withStyles from '@mui/styles/withStyles';

const styles = theme => ({
  container: {
    display: 'flex',
    marginLeft: theme.spacing(1),
    marginRight: theme.spacing(1)
  },

  leftPanel: {
    width: '80%',
    overflow: 'hidden'
  },

  rightPanel: {
    width: '20%',
    display: 'flex',
    alignItems: 'center'
  }
})

class InfoBar extends React.Component {
  render() {
    const {classes} = this.props

    return (
      <div className={classes.container}>
        <div className={classes.leftPanel}>
          <NavigationBar
            path={this.props.path}
            onPathChange={this.props.onPathChange}
          />
        </div>
        <div className={classes.rightPanel}>
          <FreeSpaceBar
            free={this.props.free}
            total={this.props.total}
          />
        </div>
      </div>
    );
  }
}

export default withStyles(styles)(InfoBar)