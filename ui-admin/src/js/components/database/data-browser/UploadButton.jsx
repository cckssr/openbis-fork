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
import Button from '@mui/material/Button'
import autoBind from 'auto-bind'

const styles = () => ({
  invisible: {
    display: 'none'
  }
})

class UploadButton extends React.Component {

  constructor(props) {
    super(props);

    autoBind(this)

    // Using the callback ref approach to ensure we have access to the input element
    // This is needed because 'webkitdirectory' and 'directory' are not considered acceptable HTML attributes by React.
    this.fileInputRef = React.createRef();
  }

  componentDidMount() {
    this.updateDirectoryAttributes();
  }

  componentDidUpdate(prevProps) {
    if (this.props.folderSelector !== prevProps.folderSelector) {
      this.updateDirectoryAttributes();
    }
  }

  updateDirectoryAttributes() {
    const { folderSelector } = this.props;
    const input = this.fileInputRef.current;
    if (input) {
      if (folderSelector) {
        // If folderSelector is true, add the attributes
        input.setAttribute('webkitdirectory', '');
        input.setAttribute('directory', '');
      } else {
        // If folderSelector is false, remove the attributes
        input.removeAttribute('webkitdirectory');
        input.removeAttribute('directory');
      }
    }
  };

  render() {
    const { children, classes, size, variant, color, onClick,
      startIcon, accept } = this.props;

    return (
      <>
        {/* Hidden file input */}
        <input
          accept={accept}
          type="file"
          multiple
          ref={this.fileInputRef}
          className={classes.invisible}
          onChange={onClick}
        />

        {/* Button to trigger the file input */}
        <Button
          classes={{ root: classes.button }}
          color={color}
          size={size}
          variant={variant}
          startIcon={startIcon}
          onClick={() => this.fileInputRef.current && this.fileInputRef.current.click()}
        >
          {children}
        </Button>
      </>
    );
  }
}

export default withStyles(styles)(UploadButton)