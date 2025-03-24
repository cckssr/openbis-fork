import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Container from '@src/js/components/common/form/Container.jsx'
import Header from '@src/js/components/common/form/Header.jsx'
import TextField from '@src/js/components/common/form/TextField.jsx'
import DateField from '@src/js/components/common/form/DateField.jsx'
import SelectField from '@src/js/components/common/form/SelectField.jsx'
import CheckboxField from '@src/js/components/common/form/CheckboxField.jsx'
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx'
import UserFormSelectionType from '@src/js/components/users/form/user/UserFormSelectionType.js'
import Message from '@src/js/components/common/form/Message.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  field: {
    paddingBottom: theme.spacing(1)
  }
})

class UserFormParametersUser extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {}
    this.references = {
      userId: React.createRef(),
      space: React.createRef(),
      firstName: React.createRef(),
      lastName: React.createRef(),
      email: React.createRef(),
      userStatus: React.createRef(),
      userStatusExpiryDate: React.createRef()
    }
    this.handleChange = this.handleChange.bind(this)
    this.handleFocus = this.handleFocus.bind(this)
    this.handleBlur = this.handleBlur.bind(this)
  }

  componentDidMount() {
    this.focus()
  }

  componentDidUpdate(prevProps) {
    const prevSelection = prevProps.selection
    const selection = this.props.selection

    if (prevSelection !== selection) {
      this.focus()
    }
  }

  focus() {
    const user = this.getUser(this.props)
    if (user && this.props.selection) {
      const { part } = this.props.selection.params
      if (part) {
        const reference = this.references[part]
        if (reference && reference.current) {
          reference.current.focus()
        }
      }
    }
  }

  handleChange(event) {
    this.props.onChange(UserFormSelectionType.USER, {
      field: event.target.name,
      value: event.target.value
    })
  }

  handleFocus(event) {
    this.props.onSelectionChange(UserFormSelectionType.USER, {
      part: event.target.name
    })
  }

  handleBlur() {
    this.props.onBlur()
  }

  render() {
    logger.log(logger.DEBUG, 'UserFormParametersUser.render')

    const user = this.getUser(this.props)
    if (!user) {
      return null
    }

    return (
      <Container>
        {this.renderHeader(user)}
        {this.renderUserId(user)}
        {this.renderFirstName(user)}
        {this.renderLastName(user)}
        {this.renderEmail(user)}
        {this.renderSpace(user)}
        {this.renderUserRolesManagementHeader()}
        {this.renderAccessRevokedInformation(user)}
        {this.renderUserStatus(user)}
        {this.renderStatusExpiryDate(user)}
      </Container>
    )
  }

  renderHeader(user) {
    const message = user.original ? messages.USER : messages.NEW_USER
    return <Header>{messages.get(message)}
        </Header>
  }

  renderUserRolesManagementHeader() {
    return <Header>{messages.get(messages.USER_ROLES_MANAGEMENT)}</Header>
  }

  renderUserId(user) {
    const { visible, enabled, error, value } = { ...user.userId }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <TextField
          reference={this.references.userId}
          label={messages.get(messages.USER_ID)}
          name='userId'
          mandatory={true}
          error={error}
          disabled={!enabled}
          value={value}
          mode={mode}
          onChange={this.handleChange}
          onFocus={this.handleFocus}
          onBlur={this.handleBlur}
        />
      </div>
    )
  }

  renderFirstName(user) {
    const { visible, enabled, error, value } = { ...user.firstName }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <TextField
          reference={this.references.firstName}
          label={messages.get(messages.FIRST_NAME)}
          name='firstName'
          error={error}
          disabled={!enabled}
          value={value}
          mode={mode}
          onChange={this.handleChange}
          onFocus={this.handleFocus}
          onBlur={this.handleBlur}
        />
      </div>
    )
  }

  renderLastName(user) {
    const { visible, enabled, error, value } = { ...user.lastName }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <TextField
          reference={this.references.lastName}
          label={messages.get(messages.LAST_NAME)}
          name='lastName'
          error={error}
          disabled={!enabled}
          value={value}
          mode={mode}
          onChange={this.handleChange}
          onFocus={this.handleFocus}
          onBlur={this.handleBlur}
        />
      </div>
    )
  }

  renderEmail(user) {
    const { visible, enabled, error, value } = { ...user.email }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <TextField
          reference={this.references.email}
          label={messages.get(messages.EMAIL)}
          name='email'
          error={error}
          disabled={!enabled}
          value={value}
          mode={mode}
          onChange={this.handleChange}
          onFocus={this.handleFocus}
          onBlur={this.handleBlur}
        />
      </div>
    )
  }

  renderSpace(user) {
    const { visible, enabled, error, value } = { ...user.space }

    if (!visible) {
      return null
    }

    const { mode, classes, controller } = this.props
    const { spaces } = controller.getDictionaries()

    let options = []

    if (spaces) {
      options = spaces.map(space => {
        return {
          label: space.code,
          value: space.code
        }
      })
    }

    return (
      <div className={classes.field}>
        <SelectField
          reference={this.references.space}
          label={messages.get(messages.HOME_SPACE)}
          name='space'
          error={error}
          disabled={!enabled}
          value={value}
          options={options}
          emptyOption={{}}
          mode={mode}
          onChange={this.handleChange}
          onFocus={this.handleFocus}
          onBlur={this.handleBlur}
        />
      </div>
    )
  }


  renderAccessRevokedInformation(user) {
        const { mode, classes } = this.props

        if(!user.original) {
            return null;
        }

        const { visible, enabled, error, value } = { ...user.userStatus }

        if(value === messages.INACTIVE) {
            return (
              <div className={classes.field}>
                <Message type='warning'>
                    {messages.get(messages.USER_DEACTIVATED)}
                  </Message>
              </div>
            )
        }

        let expiryDateValue = user.userStatusExpiryDate.value;
        if(value === messages.ACTIVE_UNTIL_EXPIRY_DATE && expiryDateValue && expiryDateValue.dateObject && expiryDateValue.dateObject.getTime() - new Date().getTime() <= 0) {
            return (
                      <div className={classes.field}>
                        <Message type='warning'>
                            {messages.get(messages.USER_ROLES_EXPIRED)}
                          </Message>
                      </div>
                    )
        }

        let hasRolesValue = user.hasRoles.value;
        if(!hasRolesValue) {
             return (
                  <div className={classes.field}>
                    <Message type='warning'>
                        {messages.get(messages.USER_HAVE_NO_ROLES)}
                      </Message>
                  </div>
                )
        }

        return null;
      }

  renderUserStatus(user) {
      const { mode, classes, controller } = this.props

      const { visible, enabled, error, value } = { ...user.userStatus }
      let options = [
              {
                label: messages.get(messages.ACTIVE),
                value: messages.ACTIVE
              },
              {
                label: messages.get(messages.ACTIVE_UNTIL_EXPIRY_DATE),
                value: messages.ACTIVE_UNTIL_EXPIRY_DATE
              },
          ];

      if(user.original) {
          options.push({
                       label: messages.get(messages.INACTIVE),
                       value: messages.INACTIVE
                     })
      }


      let displayValue = options.filter(x => x.value === value)[0].value;


      return (
            <div className={classes.field}>
              <SelectField
                reference={this.references.userStatus}
                label={messages.get(messages.USER_STATUS)}
                name='userStatus'
                error={error}
                disabled={!enabled}
                value={displayValue}
                options={options}
                mode={mode}
                onChange={this.handleChange}
                onFocus={this.handleFocus}
                onBlur={this.handleBlur}
              />
            </div>
          )
  }

  renderStatusExpiryDate(user) {

      const { visible, enabled, error, value } = { ...user.userStatusExpiryDate }

      if (user.userStatus.value && user.userStatus.value !== messages.ACTIVE_UNTIL_EXPIRY_DATE) {
        return null
      }

      const { mode, classes, controller } = this.props


        return (
          <div className={classes.field}>
            <DateField
              reference={this.references.userStatusExpiryDate}
              label={messages.get(messages.EXPIRY_DATE_FIELD)}
              name='userStatusExpiryDate'
              mandatory={true}
              error={error}
              disabled={!enabled}
              value={value}
              mode={mode}
              onChange={this.handleChange}
              onFocus={this.handleFocus}
              onBlur={this.handleBlur}
    //             color={this.getColor(value)}
            />
          </div>
        )
    }

  getColor(value) {
      let date = new Date().getTime();
      if(value && value.dateObject) {
        if(date - value.dateObject.getTime() > 0) {
            return 'error';
        }
      }
    return null;
  }

  getUser(props) {
    let { user, selection } = props

    if (!selection || selection.type === UserFormSelectionType.USER) {
      return user
    } else {
      return null
    }
  }
}

export default withStyles(styles)(UserFormParametersUser)
