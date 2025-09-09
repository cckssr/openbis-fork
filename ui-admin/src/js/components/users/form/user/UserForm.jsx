import React from 'react'
import autoBind from 'auto-bind'
import withStyles from '@mui/styles/withStyles';
import ComponentContext from '@src/js/components/common/ComponentContext.js'
import PageWithTwoPanels from '@src/js/components/common/page/PageWithTwoPanels.jsx'
import GridContainer from '@src/js/components/common/grid/GridContainer.jsx'
import UserFormController from '@src/js/components/users/form/user/UserFormController.js'
import UserFormFacade from '@src/js/components/users/form/user/UserFormFacade.js'
import UserFormSelectionType from '@src/js/components/users/form/user/UserFormSelectionType.js'
import UserFormParametersUser from '@src/js/components/users/form/user/UserFormParametersUser.jsx'
import UserFormParametersGroup from '@src/js/components/users/form/user/UserFormParametersGroup.jsx'
import UserFormParametersRole from '@src/js/components/users/form/user/UserFormParametersRole.jsx'
import UserFormParametersMetadata from '@src/js/components/users/form/user/UserFormParametersMetadata.jsx'
import UserFormGridGroups from '@src/js/components/users/form/user/UserFormGridGroups.jsx'
import UserFormGridRoles from '@src/js/components/users/form/user/UserFormGridRoles.jsx'
import UserFormButtons from '@src/js/components/users/form/user/UserFormButtons.jsx'
import ids from '@src/js/common/consts/ids.js'
import logger from '@src/js/common/logger.js'
import { Tab } from '@mui/material';
import { TabContext, TabList, TabPanel } from '@mui/lab';

const styles = theme => ({
  grid: {
    marginBottom: theme.spacing(2)
  },
  tabsRoot: {
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: theme.palette.border.primary,
  },
  tabPanelRoot: {
    padding: 0,
  }
})

class UserForm extends React.PureComponent {
  constructor(props) {
    super(props)
    autoBind(this)

    this.state = {
      tabSelected: 0
    }

    if (this.props.controller) {
      this.controller = this.props.controller
    } else {
      this.controller = new UserFormController(new UserFormFacade())
    }

    this.controller.init(new ComponentContext(this))
  }

  componentDidMount() {
    this.controller.load()
  }

  handleTabChange = (event, newValue) => {
    this.setState({
      tabSelected: newValue,
    })
  }

  handleClickContainer() {
    this.controller.handleSelectionChange()
  }

  handleSelectedGroupRowChange(row) {
    const { controller } = this
    if (row) {
      controller.handleSelectionChange(UserFormSelectionType.GROUP, {
        id: row.id
      })
    }
  }

  handleSelectedRoleRowChange(row) {
    const { controller } = this
    if (row) {
      controller.handleSelectionChange(UserFormSelectionType.ROLE, {
        id: row.id
      })
    }
  }

  handleGroupsGridControllerRef(gridController) {
    this.controller.groupsGridController = gridController
  }

  handleRolesGridControllerRef(gridController) {
    this.controller.rolesGridController = gridController
  }

  render() {
    logger.log(logger.DEBUG, 'UserForm.render')

    const { loadId, loading, loaded, user } = this.state

    return (
      <PageWithTwoPanels
        id={ids.USER_FORM_ID}
        key={loadId}
        loading={loading}
        loaded={loaded}
        object={user}
        renderMainPanel={() => this.renderMainPanel()}
        renderAdditionalPanel={() => this.renderAdditionalPanel()}
        renderButtons={() => this.renderButtons()}
      />
    )
  }

  renderMainPanel() {
    const { classes } = this.props
    const { groups, roles, selection } = this.state

    return (
      <GridContainer onClick={this.handleClickContainer}>
        <div className={classes.grid}>
          <UserFormGridGroups
            controllerRef={this.handleGroupsGridControllerRef}
            rows={groups}
            selectedRowId={
              selection && selection.type === UserFormSelectionType.GROUP
                ? selection.params.id
                : null
            }
            onSelectedRowChange={this.handleSelectedGroupRowChange}
          />
        </div>
        <div className={classes.grid}>
          <UserFormGridRoles
            controllerRef={this.handleRolesGridControllerRef}
            rows={roles}
            selectedRowId={
              selection && selection.type === UserFormSelectionType.ROLE
                ? selection.params.id
                : null
            }
            onSelectedRowChange={this.handleSelectedRoleRowChange}
          />
        </div>
      </GridContainer>
    )
  }

  renderAdditionalPanel() {
    const { controller } = this
    const {
      user,
      groups,
      roles,
      selection,
      selectedGroupRow,
      selectedRoleRow,
      mode,
      tabSelected
    } = this.state
    const { classes } = this.props

    const PROPERTIES_TAB_INDEX = 0;
    const METADATA_TAB_INDEX = 1;
    console.log('UserForm.renderAdditionalPanel', { user }, { groups }, { roles }, { selection }, { selectedGroupRow }, { selectedRoleRow }, { mode }, { tabSelected })
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
          <Tab key='user-tab-id'
            value={PROPERTIES_TAB_INDEX}
            label='Parameters'
            sx={{ textTransform: 'none', padding: 'unset' }}
          />
          <Tab key='metadata-tab-id'
            value={METADATA_TAB_INDEX}
            label='Metadata'
            sx={{ textTransform: 'none', padding: 'unset' }}
          />
        </TabList>
        <TabPanel classes={{ root: classes.tabPanelRoot }} value={PROPERTIES_TAB_INDEX}>
          <UserFormParametersUser
            controller={controller}
            user={user}
            selection={selection}
            mode={mode}
            onChange={controller.handleChange}
            onSelectionChange={controller.handleSelectionChange}
            onBlur={controller.handleBlur}
          />
          <UserFormParametersGroup
            controller={controller}
            groups={groups}
            selection={selection}
            selectedRow={selectedGroupRow}
            mode={mode}
            onChange={controller.handleChange}
            onSelectionChange={controller.handleSelectionChange}
            onBlur={controller.handleBlur}
          />
          <UserFormParametersRole
            controller={controller}
            roles={roles}
            selection={selection}
            selectedRow={selectedRoleRow}
            mode={mode}
            onChange={controller.handleChange}
            onSelectionChange={controller.handleSelectionChange}
            onBlur={controller.handleBlur}
          />
        </TabPanel>
        <TabPanel classes={{ root: classes.tabPanelRoot }} value={METADATA_TAB_INDEX}>
          <UserFormParametersMetadata
            controller={controller}
            user={user}
            groups={groups}
            roles={roles}
            selection={selection}
            mode={mode}
            onChange={controller.handleChange}
            onSelectionChange={controller.handleSelectionChange}
            onBlur={controller.handleBlur}
          />
        </TabPanel>
      </TabContext>
    )
  }

  renderButtons() {
    const { controller } = this
    const { user, roles, selection, changed, mode } = this.state

    return (
      <UserFormButtons
        onEdit={controller.handleEdit}
        onSave={controller.handleSave}
        onCancel={controller.handleCancel}
        onAddGroup={controller.handleAddGroup}
        onAddRole={controller.handleAddRole}
        onRemove={controller.handleRemove}
        user={user}
        roles={roles}
        selection={selection}
        changed={changed}
        mode={mode}
      />
    )
  }
}

export default withStyles(styles)(UserForm)
