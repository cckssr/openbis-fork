import React from 'react'
import withStyles from '@mui/styles/withStyles';
import AppController from '@src/js/components/AppController.js'
import Container from '@src/js/components/common/form/Container.jsx'
import Header from '@src/js/components/common/form/Header.jsx'
import TextField from '@src/js/components/common/form/TextField.jsx'
import CheckboxField from '@src/js/components/common/form/CheckboxField.jsx'
import Message from '@src/js/components/common/form/Message.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'

const styles = theme => ({
  field: {
    paddingBottom: theme.spacing(1)
  }
})

class TypeGroupFormParametersTypeGroup extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {}
    this.references = {
      code: React.createRef(),
      internal: React.createRef(),
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
    const typeGroup = this.getTypeGroup(this.props)
    if (typeGroup && this.props.selection) {
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
    console.log('TypeGroupFormParameters.handleChange', event)
    this.props.onChange(TypeGroupFormSelectionType.TYPE_GROUP, {
      field: event.target.name,
      value: event.target.value
    })
  }

  handleFocus(event) {
    console.log('TypeGroupFormParameters.handleFocus', event)
    this.props.onSelectionChange(TypeGroupFormSelectionType.TYPE_GROUP, {
      part: event.target.name
    })
  }

  handleBlur() {
    console.log('TypeGroupFormParameters.handleBlur')
    this.props.onBlur()
  }

  render() {
    logger.log(logger.DEBUG, 'TypeGroupFormParameters.render')

    const typeGroup = this.getTypeGroup(this.props)
    if (!typeGroup) {
      return null
    }

    console.log('TypeGroupFormParametersTypeGroup.render', typeGroup)
    return (
      <Container>
        {this.renderHeader(typeGroup)}
        {this.renderMessageInternal(typeGroup)}
        {this.renderCode(typeGroup)}
        {this.renderInternal(typeGroup)}
      </Container>
    )
  }

  renderHeader(typeGroup) {
    const message = typeGroup.original
      ? messages.OBJECT_TYPE_GROUP
      : messages.NEW_OBJECT_TYPE_GROUP
    return <Header>{messages.get(message)}</Header>
  }

  renderMessageInternal(typeGroup) {
    const { classes } = this.props

    if (typeGroup.internal.value) {
      if (AppController.getInstance().isSystemUser()) {
        return (
          <div className={classes.field}>
            <Message type='lock'>
              {messages.get(messages.OBJECT_TYPE_GROUP_IS_INTERNAL)}
            </Message>
          </div>
        )
      } else {
        return (
          <div className={classes.field}>
            <Message type='lock'>
              {messages.get(messages.OBJECT_TYPE_GROUP_IS_INTERNAL)}{' '}
              {messages.get(messages.OBJECT_TYPE_GROUP_CANNOT_BE_CHANGED_OR_REMOVED)}
            </Message>
          </div>
        )
      }
    } else {
      return null
    }
  }

  renderCode(typeGroup) {
    const { visible, enabled, error, value } = { ...typeGroup.code }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <TextField
          reference={this.references.code}
          label={messages.get(messages.CODE)}
          name='code'
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

  renderInternal(typeGroup) {
    const { visible, enabled, error, value } = { ...typeGroup.internal }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <CheckboxField
          reference={this.references.internal}
          label={messages.get(messages.INTERNAL)}
          name='internal'
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

  getTypeGroup(props) {
    let { typeGroup, selection } = props

    if (
      !selection ||
      selection.type === TypeGroupFormSelectionType.TYPE_GROUP
    ) {
      return typeGroup
    } else {
      return null
    }
  }
}

export default withStyles(styles)(TypeGroupFormParametersTypeGroup)
