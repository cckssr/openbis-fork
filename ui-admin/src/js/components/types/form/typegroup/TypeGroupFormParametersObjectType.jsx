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
import SelectField from '@src/js/components/common/form/SelectField.jsx'

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
        {this.renderMessageVisible(objectType)}
        {this.renderCode(objectType)}
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

  renderCode(objectType) {
    const { visible, enabled, error, value } = { ...objectType.code }

    if (!visible) {
      return null
    }

    const { mode, classes, controller } = this.props
    const objectTypesOptions = controller.getObjectTypesOptions()

    let options = []
    if (objectTypesOptions) {
      options = objectTypesOptions.map(objectType => {
        return {
          label: objectType.text,
          value: objectType.id
        }
      })
    }

    return (
      <div className={classes.field}>
        <SelectField
          reference={this.references.code}
          label={messages.get(messages.CODE)}
          name='code'
          error={error}
          disabled={!enabled}
          mandatory={true}
          value={value}
          options={options}
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
