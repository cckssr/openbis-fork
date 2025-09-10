import React from 'react'
import withStyles from '@mui/styles/withStyles';
import EntityTypeFormParametersType from '@src/js/components/types/form/entitytype/EntityTypeFormParametersType.jsx'
import EntityTypeFormParametersTypeSemanticAnnotation from '@src/js/components/types/form/entitytype/EntityTypeFormParametersTypeSemanticAnnotation.jsx';
import EntityTypeFormParametersProperty from '@src/js/components/types/form/entitytype/EntityTypeFormParametersProperty.jsx'
import EntityTypeFormParametersPropertySemanticAnnotation from '@src/js/components/types/form/entitytype/EntityTypeFormParametersPropertySemanticAnnotation.jsx';
import EntityTypeFormParametersSection from '@src/js/components/types/form/entitytype/EntityTypeFormParametersSection.jsx'
import EntityTypeFormParametersMetadata from '@src/js/components/types/form/entitytype/EntityTypeFormParametersMetadata.jsx'
import TabViewer from '@src/js/components/common/tab/TabViewer.jsx'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  // Empty styles object since TabViewer handles its own styling
})

class EntityTypeFormParameters extends React.PureComponent {

  render() {
    logger.log(logger.DEBUG, 'EntityTypeFormParameters.render')

    const {
      controller,
      type,
      sections,
      properties,
      selection,
      mode,
      onChange,
      onSelectionChange,
      onBlur
    } = this.props

    const tabs = [
      { key: 'property-tab-id', label: 'Parameters' },
      { key: 'semantic-annotation-tab-id', label: 'Semantic Annotations' },
      { key: 'metadata-tab-id', label: 'Metadata' }
    ]

    const children = [
      // Parameters tab content
      <div key="parameters-content">
        <EntityTypeFormParametersType
          controller={controller}
          type={type}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
        <EntityTypeFormParametersSection
          sections={sections}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
        <EntityTypeFormParametersProperty
          controller={controller}
          type={type}
          properties={properties}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
      </div>,
      // Semantic Annotations tab content
      <div key="semantic-annotations-content">
        <EntityTypeFormParametersPropertySemanticAnnotation
          controller={controller}
          type={type}
          properties={properties}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
        <EntityTypeFormParametersTypeSemanticAnnotation
          controller={controller}
          type={type}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
      </div>,
      // Metadata tab content
      <div key="metadata-content">
        <EntityTypeFormParametersMetadata
          type={type}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
        <EntityTypeFormParametersMetadata
          properties={properties}
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

export default withStyles(styles)(EntityTypeFormParameters)
