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
import autoBind from 'auto-bind'
import withStyles from '@mui/styles/withStyles'

import FolderIcon from '@mui/icons-material/Folder'
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile'
import AudiotrackIcon from '@mui/icons-material/Audiotrack'
import DescriptionIcon from '@mui/icons-material/Description'
import MovieIcon from '@mui/icons-material/Movie'
import ImageIcon from '@mui/icons-material/Image'
import FolderZipIcon from '@mui/icons-material/FolderZip';
import CodeIcon from '@mui/icons-material/Code'
import PictureAsPdfSharp from '@mui/icons-material/PictureAsPdfSharp'
import TableChartIcon from '@mui/icons-material/TableChart'
import SlideshowIcon from '@mui/icons-material/Slideshow'

const styles = (theme) => ({
  icon: {
    verticalAlign: 'middle',
    fontSize: '4rem',    
  }
})

const materialIcons = {
  'file-audio': AudiotrackIcon,
  'file-alt': DescriptionIcon,
  'file-video': MovieIcon,
  'file-image': ImageIcon,
  'file-archive': FolderZipIcon,
  'file-code': CodeIcon,
  'file-pdf': PictureAsPdfSharp,
  'file-word': DescriptionIcon,
  'file-excel': TableChartIcon,
  'file-powerpoint': SlideshowIcon,
  'file': InsertDriveFileIcon
};

class ItemIcon extends React.Component {
  constructor(props, context) {
    super(props, context)
    autoBind(this)

    const configuration = this.props.configuration || []
    
    this.extensionToIconType = new Map(
      configuration.flatMap(configObject =>
        configObject.extensions.map(extension => [extension, configObject.icon])
      )
    )
  }

  render() {
    const { classes, file } = this.props
    
    if (file.directory) {
      return <FolderIcon className={classes.icon} />
    } else {      
      const fileExtension = file.name.substring(file.name.lastIndexOf('.') + 1)
      const iconType = this.extensionToIconType.get(fileExtension)      
      const IconComponent = materialIcons[iconType] || InsertDriveFileIcon
      return <IconComponent className={classes.icon} />
    }
  }
}

export default withStyles(styles)(ItemIcon)
