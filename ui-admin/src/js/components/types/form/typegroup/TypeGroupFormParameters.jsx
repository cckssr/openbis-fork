import React from 'react'
import withStyles from '@mui/styles/withStyles';
import TypeGroupFormParametersTypeGroup from '@src/js/components/types/form/typegroup/TypeGroupFormParametersTypeGroup.jsx'
import TypeGroupFormParametersObjectType from '@src/js/components/types/form/typegroup/TypeGroupFormParametersObjectType.jsx'
import TypeGroupFormParametersMetadata from '@src/js/components/types/form/typegroup/TypeGroupFormParametersMetadata.jsx'
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

class TypeGroupFormParameters extends React.PureComponent {
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
      onBlur,
      classes
    } = this.props

    const { tabSelected } = this.state;

    const PROPERTIES_TAB_INDEX = 0;
    const METADATA_TAB_INDEX = 1;

    return (
      <TabContext value={tabSelected} >
        <TabList
          variant='fullWidth'
          onChange={this.handleTabChange}
          classes={{ root: classes.tabsRoot }}
          textColor='inherit'
          indicatorColor='secondary'
          slotProps={{
            indicator: {
              style: {
                transition: 'none',
              }
            }
          }}
        >
          <Tab key='type-group-tab-id'
            value={PROPERTIES_TAB_INDEX}
            label='Parameters'
            sx={{textTransform :'none', padding: 'unset'}}
          />
          <Tab key='type-group-metadata-tab-id'
            value={METADATA_TAB_INDEX}
            label='Metadata'
            sx={{textTransform :'none', padding: 'unset'}}
          />
        </TabList>
        <TabPanel classes={{ root: classes.tabPanelRoot }} value={PROPERTIES_TAB_INDEX}>
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
        </TabPanel>
        <TabPanel classes={{ root: classes.tabPanelRoot }} value={METADATA_TAB_INDEX}>
          <TypeGroupFormParametersMetadata
            controller={controller}
            typeGroup={typeGroup}
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

export default withStyles(styles)(TypeGroupFormParameters)
