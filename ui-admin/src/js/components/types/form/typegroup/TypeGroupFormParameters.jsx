import React from 'react'
import withStyles from '@mui/styles/withStyles';
import TypeGroupFormParametersTypeGroup from '@src/js/components/types/form/typegroup/TypeGroupFormParametersTypeGroup.jsx'
import TypeGroupFormParametersObjectType from '@src/js/components/types/form/typegroup/TypeGroupFormParametersObjectType.jsx'
import TypeGroupFormParametersMetadata from '@src/js/components/types/form/typegroup/TypeGroupFormParametersMetadata.jsx'
import TabViewer from '@src/js/components/common/tab/TabViewer.jsx'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  // Empty styles object since TabViewer handles its own styling
})

class TypeGroupFormParameters extends React.PureComponent {

  render() {
    logger.log(logger.DEBUG, 'TypeGroupFormParameters.render')

    const {
      controller,
      typeGroup,
      objectTypes,
      selection,
      selectedRow,
      mode,
      onChange,
      onSelectionChange,
      onBlur
    } = this.props

    const tabs = [
      { key: 'type-group-tab-id', label: 'Parameters' },
      { key: 'type-group-metadata-tab-id', label: 'Metadata' }
    ]

    const children = [
      <div key="parameters-content">
        <TypeGroupFormParametersTypeGroup
          controller={controller}
          typeGroup={typeGroup}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
        <TypeGroupFormParametersObjectType
          controller={controller}
          typeGroup={typeGroup}
          objectTypes={objectTypes}
          selection={selection}
          selectedRow={selectedRow}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
      </div>,
      <div key="metadata-content">
        <TypeGroupFormParametersMetadata
          controller={controller}
          typeGroup={typeGroup}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
      </div>
    ]

    return (
      <TabViewer
        tabs={tabs}
        children={children}
      />
    )
  }
}

export default withStyles(styles)(TypeGroupFormParameters)
