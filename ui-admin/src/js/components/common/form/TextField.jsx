import React from 'react'
import withStyles from '@mui/styles/withStyles'
import TextField from '@mui/material/TextField'
import InputAdornment from '@mui/material/InputAdornment'
import FormFieldContainer from '@src/js/components/common/form/FormFieldContainer.jsx'
import FormFieldLabel from '@src/js/components/common/form/FormFieldLabel.jsx'
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx'
import logger from '@src/js/common/logger.js'
import date from '@src/js/common/date.js'
import { FormFieldDataType } from '@src/js/components/database/new-forms/types/formEnums.ts'

const styles = theme => ({
  startAdornment: {
    marginRight: 0
  },
  endAdornment: {
    marginLeft: 0
  },
  textField: {
    margin: 0,
    '& textarea': {
      minHeight: '19px !important'
    }
  },
  input: {
    fontSize: theme.typography.body2.fontSize
  },
  inputDisabled: {
    pointerEvents: 'none'
  }
})

class TextFormField extends React.PureComponent {
  static defaultProps = {
    mode: 'edit',
    variant: 'filled',
    autoComplete: 'off'
  }

  arrayToString(array, dataType) {
    switch (dataType) {
      case FormFieldDataType.ARRAY_INTEGER:
      case FormFieldDataType.ARRAY_REAL:
        return `[${array.join(', ')}]`
      case FormFieldDataType.ARRAY_STRING:
        return `["${array.join('", "')}"]`
      case FormFieldDataType.ARRAY_TIMESTAMP:
        return `["${array.map(v => date.format(new Date(v), true)).join('", "')}"]`
      default:
        throw 'Unsupported array data type: ' + dataType
    }
  }

  render() {
    logger.log(logger.DEBUG, 'TextFormField.render')

    const { mode } = this.props

    if (mode === 'view') {
      return this.renderView()
    } else if (mode === 'edit') {
      return this.renderEdit()
    } else {
      throw 'Unsupported mode: ' + mode
    }
  }

  renderValue(value, dataType) {
    if (value) {
      if (dataType === FormFieldDataType.HYPERLINK) {
        return <a href={value} target='_blank'>{value}</a>
      } else if (
        dataType === FormFieldDataType.ARRAY_TIMESTAMP ||
        dataType === FormFieldDataType.ARRAY_STRING ||
        dataType === FormFieldDataType.ARRAY_INTEGER ||
        dataType === FormFieldDataType.ARRAY_REAL
      ) {
        return this.arrayToString(value, dataType)
      } else {
        return value
      }
    } else {
      return null
    }
  }

  renderView() {
    const { label, value, description, disableUnderline, dataType } = this.props

    return (
      <FormFieldView
        label={label}
        value={this.renderValue(value, dataType)}
        description={description}
        disableUnderline={disableUnderline || false}
      />
    )
  }

  renderEdit() {
    const {
      reference,
      id,
      type,
      name,
      label,
      description,
      value,
      mandatory,
      disabled,
      hyperlink,
      autoComplete,
      error,
      multiline,
      metadata,
      startAdornment,
      endAdornment,
      styles,
      classes,
      variant,
      onClick,
      onKeyPress,
      onChange,
      onFocus,
      onBlur
    } = this.props

    return (
      <FormFieldContainer
        description={description}
        error={error}
        metadata={metadata}
        styles={styles}
        onClick={onClick}
      >
        <TextField
          inputRef={reference}
          id={id}
          type={type}
          label={
            <FormFieldLabel
              label={label}
              mandatory={mandatory}
              styles={styles}
              onClick={onClick}
            />
          }
          name={name}
          value={value || ''}
          error={!!error}
          disabled={disabled}
          multiline={multiline}
          onKeyPress={onKeyPress}
          onChange={onChange}
          onFocus={onFocus}
          onBlur={onBlur}
          fullWidth={true}
          autoComplete={autoComplete}
          variant={variant}
          margin='dense'
          classes={{
            root: classes.textField
          }}
          slotProps={{
            input: {
              startAdornment: startAdornment ? (
                <InputAdornment
                  position='start'
                  classes={{ positionStart: classes.startAdornment }}
                >
                  {startAdornment}
                </InputAdornment>
              ) : null,
              endAdornment: endAdornment ? (
                <InputAdornment
                  position='end'
                  classes={{ positionEnd: classes.endAdornment }}
                >
                  {endAdornment}
                </InputAdornment>
              ) : null,
              classes: {
                input: classes.input,
                disabled: classes.inputDisabled
              }
            }
          }}
        />
      </FormFieldContainer>
    )
  }
}

export default withStyles(styles)(TextFormField)
