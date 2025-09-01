import React from 'react'
import withStyles from '@mui/styles/withStyles';
import AppController from '@src/js/components/AppController.js'
import Container from '@src/js/components/common/form/Container.jsx'
import Header from '@src/js/components/common/form/Header.jsx'
import TextField from '@src/js/components/common/form/TextField.jsx'
import CheckboxField from '@src/js/components/common/form/CheckboxField.jsx'
import Message from '@src/js/components/common/form/Message.jsx'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import MultipleSelectCheckmarks from '@src/js/components/types/form/typegroup/MultipleSelectCheckmarks.tsx'

const styles = theme => ({
  field: {
    paddingBottom: theme.spacing(1)
  }
})

class TypeGroupFormParametersObjectType extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {}
    this.references = {
      code: React.createRef(),
      description: React.createRef()
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
    const objectType = this.getObjectType(this.props)
    if (objectType && this.props.selection) {
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
    const objectType = this.getObjectType(this.props)
    this.props.onChange(TypeGroupFormSelectionType.OBJECT_TYPE, {
      id: objectType.id,
      field: event.target.name,
      value: event.target.value
    })
  }

  handleFocus(event) {
    const objectType = this.getObjectType(this.props)
    this.props.onSelectionChange(TypeGroupFormSelectionType.OBJECT_TYPE, {
      id: objectType.id,
      part: event.target.name
    })
  }

  handleBlur() {
    this.props.onBlur()
  }

  render() {
    logger.log(logger.DEBUG, 'TypeGroupFormParametersObjectType.render')

    const objectType = this.getObjectType(this.props)
    if (!objectType) {
      return null
    }

    return (
      <Container>
        <Header>{messages.get(messages.OBJECT_TYPE)}</Header>
        {this.renderSelectObjectType(objectType)}
        {this.renderMessageVisible(objectType)}
        {this.renderMessageInternal(objectType)}
        {this.renderCode(objectType)}
        {this.renderLabel(objectType)}
        {this.renderDescription(objectType)}
        {this.renderOfficial(objectType)}
      </Container>
    )
  }

  renderMessageVisible() {
    const { classes, selectedRow } = this.props

    if (selectedRow && !selectedRow.visible) {
      return (
        <div className={classes.field}>
          <Message type='warning'>
            {messages.get(
              messages.OBJECT_NOT_VISIBLE_DUE_TO_FILTERING_AND_PAGING
            )}
          </Message>
        </div>
      )
    } else {
      return null
    }
  }

  renderMessageInternal(term) {
    const { classes, typeGroup } = this.props

    if (typeGroup.internal.value && term.internal.value) {
      if (AppController.getInstance().isSystemUser()) {
        return (
          <div className={classes.field}>
            <Message type='lock'>
              {messages.get(messages.OBJECT_TYPE_IS_INTERNAL)}
            </Message>
          </div>
        )
      } else {
        return (
          <div className={classes.field}>
            <Message type='lock'>
              {messages.get(messages.OBJECT_TYPE_IS_INTERNAL)}{' '}
              {messages.get(messages.OBJECT_TYPE_CANNOT_BE_CHANGED_OR_REMOVED)}
            </Message>
          </div>
        )
      }
    } else {
      return null
    }
  }

 renderSelectObjectType(objectType) {
     const { visible, enabled, error, value } = { ...objectType.selectObjectType }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <MultipleSelectCheckmarks
          reference={this.references.selectObjectType}
          label={messages.get(messages.SELECT_OBJECT_TYPE)}
          name='selectObjectType'
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

  renderCode(objectType) {
    const { visible, enabled, error, value } = { ...objectType.code }

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

  renderLabel(objectType) {
    const { visible, enabled, error, value } = { ...objectType.label }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <TextField
          reference={this.references.label}
          label={messages.get(messages.LABEL)}
          name='label'
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

  renderDescription(objectType) {
    const { visible, enabled, error, value } = { ...objectType.description }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <TextField
          reference={this.references.description}
          label={messages.get(messages.DESCRIPTION)}
          name='description'
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

  renderOfficial(objectType) {
    const { visible, enabled, error, value } = { ...objectType.official }

    if (!visible) {
      return null
    }

    const { mode, classes } = this.props
    return (
      <div className={classes.field}>
        <CheckboxField
          reference={this.references.official}
          label={messages.get(messages.OFFICIAL)}
          name='official'
          description={messages.get(messages.OFFICIAL_OBJECT_TYPE_HINT)}
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

  getObjectType(props) {
    let { objectTypes, selection } = props

    if (selection && selection.type === TypeGroupFormSelectionType.OBJECT_TYPE) {
      let [objectType] = objectTypes.filter(objectType => objectType.id === selection.params.id)
      return objectType
    } else {
      return null
    }
  }
}

export default withStyles(styles)(TypeGroupFormParametersObjectType)
