import React from 'react';
import withStyles from '@mui/styles/withStyles';
import TextareaAutosize from '@mui/material/TextareaAutosize';
import FormFieldContainer from '@src/js/components/common/form/FormFieldContainer.jsx';
import FormFieldLabel from '@src/js/components/common/form/FormFieldLabel.jsx';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx';
import { Typography } from '@mui/material';

const styles = theme => ({
  textarea: {
    width: '95%',
    fontSize: theme.typography.body2.fontSize,
    padding: theme.spacing(1),
    borderRadius: theme.shape.borderRadius,
    borderColor: theme.palette.border.primary,
    resize: 'vertical',
    fontFamily: theme.typography.fontFamily,
    backgroundColor: theme.palette.background.field,
  },
  monospaceFont: {
    fontFamily: theme.typography.sourceCode.fontFamily
  },
  label: {
    fontSize: theme.typography.label.fontSize,
    color: theme.typography.label.color
  },
});

class TextAreaField extends React.PureComponent {
  static defaultProps = {
    mode: 'edit',
    autoComplete: 'off'
  };

  render() {
    const { mode } = this.props;
    if (mode === 'view') {
      return this.renderView();
    } else if (mode === 'edit') {
      return this.renderEdit();
    } else {
      throw 'Unsupported mode: ' + mode;
    }
  }

  renderView() {
    const { label, value, description, disableUnderline, styles } = this.props;
    return (
      <FormFieldView
        label={label}
        value={value}
        description={description}
        disableUnderline={disableUnderline || false}
        monospaceFont={styles.monospaceFont || false}
      />
    );
  }

  renderEdit() {
    const {
      id,
      name,
      label,
      description,
      value,
      mandatory,
      disabled,
      error,
      styles,
      classes,
      onChange,
      onClick,
      onFocus,
      onBlur,
      disableUnderline,
      ...rest
    } = this.props;
    return (
      <FormFieldContainer
        description={description}
        error={error}
        styles={styles}
        onClick={onClick}
      >
        <Typography
          component='label'
          className={classes.label}
        >
          <FormFieldLabel
            label={label}
            mandatory={mandatory}
            styles={styles}
            onClick={onClick}
          />
        </Typography>

        <TextareaAutosize
          id={id}
          name={name}
          value={value || ''}
          disabled={disabled}
          className={styles.monospaceFont ? `${classes.textarea} ${classes.monospaceFont}` : classes.textarea}
          onChange={onChange}
          onFocus={onFocus}
          onBlur={onBlur}
          minRows={2}
          {...rest}
        />
      </FormFieldContainer>
    );
  }
}

export default withStyles(styles)(TextAreaField); 