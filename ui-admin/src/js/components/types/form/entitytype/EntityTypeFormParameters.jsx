import React from 'react'
import withStyles from '@mui/styles/withStyles';
import EntityTypeFormParametersType from '@src/js/components/types/form/entitytype/EntityTypeFormParametersType.jsx'
import EntityTypeFormParametersProperty from '@src/js/components/types/form/entitytype/EntityTypeFormParametersProperty.jsx'
import EntityTypeFormParametersSemanticAnnotation from '@src/js/components/types/form/entitytype/EntityTypeFormParametersSemanticAnnotation.jsx';
import EntityTypeFormParametersSection from '@src/js/components/types/form/entitytype/EntityTypeFormParametersSection.jsx'
import logger from '@src/js/common/logger.js'
import { Tab } from '@mui/material';
import { TabContext, TabList, TabPanel } from '@mui/lab';

const styles = theme => ({
  tabsRoot: {
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: theme.palette.border.primary,
  },
  tabPanelRoot: {
    padding: 0,
  }
})

class EntityTypeFormParameters extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {
      tabSelected: 0
    }
  }

  handleTabChange = (event, newValue) => {
    this.setState({
      tabSelected: newValue,
    })
  }

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
      onBlur,
      classes
    } = this.props

    const { tabSelected } = this.state;

    const PROPERTIES_TAB_INDEX = 0;
    const SEMANTIC_ANNOTATIONS_TAB_INDEX = 1;

    return (
      <TabContext value={tabSelected} >
        <TabList variant="fullWidth"
          onChange={this.handleTabChange}
          classes={{ root: classes.tabsRoot }}
          textColor='inherit'
          indicatorColor='secondary'
          TabIndicatorProps={{
            style: {
              transition: 'none',
            }
          }}>
          <Tab key='property-tab-id'
            value={PROPERTIES_TAB_INDEX}
            label="Parameters"
          />
          <Tab key='property-semantic-ann-tab-id'
            value={SEMANTIC_ANNOTATIONS_TAB_INDEX}
            label="Semantic Annotations"
          />
        </TabList>
        <TabPanel classes={{ root: classes.tabPanelRoot }} value={PROPERTIES_TAB_INDEX}>
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
        </TabPanel>
        <TabPanel classes={{ root: classes.tabPanelRoot }} value={SEMANTIC_ANNOTATIONS_TAB_INDEX}>
          <EntityTypeFormParametersSemanticAnnotation
            controller={controller}
            type={type}
            properties={properties}
            selection={selection}
            mode={mode}
            onChange={onChange}
            onSelectionChange={onSelectionChange}
            onBlur={onBlur}
          />
        </TabPanel>
      </TabContext>
    )
  }
}

export default withStyles(styles)(EntityTypeFormParameters)
