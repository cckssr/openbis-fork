import React from 'react'
import withStyles from '@mui/styles/withStyles';
import TextField from '@mui/material/TextField'
import MenuItem from '@mui/material/MenuItem'
import Checkbox from '@mui/material/Checkbox'
import ListItemText from '@mui/material/ListItemText'
import Chip from '@mui/material/Chip'
import Box from '@mui/material/Box'
import FormFieldContainer from '@src/js/components/common/form/FormFieldContainer.jsx'
import FormFieldLabel from '@src/js/components/common/form/FormFieldLabel.jsx'
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx'
import compare from '@src/js/common/compare.js'
import util from '@src/js/common/util.js'
import logger from '@src/js/common/logger.js'

const ITEM_HEIGHT = 48
const ITEM_PADDING_TOP = 8
const MenuProps = {
	PaperProps: {
		style: {
			maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
			width: 250,
		},
	},
}

const styles = theme => ({
	textField: {
		margin: 0
	},
	select: {
		fontSize: theme.typography.body2.fontSize
	},
	selectDisabled: {
		pointerEvents: 'none'
	},
	option: {
		'&:after': {
			content: '"\\00a0"'
		},
		fontSize: theme.typography.body2.fontSize
	},
	chipsContainer: {
		display: 'flex',
		flexWrap: 'wrap',
		gap: theme.spacing(0.5)
	}
})

class MultiSelectFormField extends React.PureComponent {
	static defaultProps = {
		mode: 'edit',
		variant: 'filled',
		fullWidth: true,
		emptyOption: null
	}

	constructor(props) {
		super(props)
		this.inputReference = React.createRef()
		this.handleFocus = this.handleFocus.bind(this)
		this.handleBlur = this.handleBlur.bind(this)
		this.handleChange = this.handleChange.bind(this)
	}

	handleFocus(event) {
		this.handleEvent(event, this.props.onFocus)
	}

	handleBlur(event) {
		this.handleEvent(event, this.props.onBlur)
	}

	handleEvent(event, handler) {
		if (handler) {
			const newEvent = {
				...event,
				target: {
					name: this.props.name,
					value: this.props.value
				}
			}
			handler(newEvent)
		}
	}

	handleChange(event) {
		const { onChange } = this.props
		if (onChange) {
			const {
				target: { value },
			} = event
			// On autofill we get a stringified value.
			const selectedValues = typeof value === 'string' ? value.split(',') : value

			const newEvent = {
				...event,
				target: {
					name: this.props.name,
					value: selectedValues
				}
			}
			onChange(newEvent)
		}
	}

	render() {
		logger.log(logger.DEBUG, 'MultiSelectFormField.render')

		const { mode } = this.props

		if (mode === 'view') {
			return this.renderView()
		} else if (mode === 'edit') {
			return this.renderEdit()
		} else {
			throw 'Unsupported mode: ' + mode
		}
	}

	renderView() {
		const { label, value, options, disableUnderline = false } = this.props

		if (!this.isEmptyValue(value)) {
			const normalizedValues = this.normalizeValue(value)
			const optionTexts = normalizedValues
				.map(val => {
					// Try to find option by value, code, or id
					const option = options && options.find(opt =>
						opt.value === val || opt.code === val || opt.id === val
					)
					return option ? this.getOptionText(option) : val
				})
				.filter(text => text)

			const valueDisplay = optionTexts.length > 0
				? optionTexts.join(', ') 
				: null

			return <FormFieldView label={label} value={valueDisplay} disableUnderline={disableUnderline} />
		} else {
			return <FormFieldView label={label} disableUnderline={disableUnderline} />
		}
	}

	renderEdit() {
		const {
			reference,
			name,
			label,
			description,
			value,
			mandatory,
			disabled,
			error,
			metadata,
			styles,
			onClick,
			classes,
			variant,
			fullWidth
		} = this.props

		this.fixReference(reference)

		const selectedValues = this.normalizeValue(value)

		return (
			<FormFieldContainer
				description={description}
				error={error}
				metadata={metadata}
				styles={styles}
				onClick={onClick}
			>
				<TextField
					select
					slotProps={{
						select: {
							multiple: true,
							renderValue: (selected) => (
								<Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
									{selected.map((val) => {
										const option = this.props.options && this.props.options.find(opt =>
											opt.value === val || opt.code === val || opt.id === val
										)
										const label = option ? this.getOptionText(option) : val
										return <Chip key={val} label={label} size="small" />
									})}
								</Box>
							),
							MenuProps: MenuProps,
							classes: {
								root: classes.select,
								disabled: classes.selectDisabled
							}
						},
						inputLabel: {
							shrink: selectedValues.length > 0
						}
					}}
					inputRef={this.inputReference}
					label={
						label ? (
							<FormFieldLabel
								label={label}
								mandatory={mandatory}
								styles={styles}
								onClick={onClick}
							/>
						) : null
					}
					name={name}
					value={selectedValues}
					error={!!error}
					disabled={disabled}
					onChange={this.handleChange}
					onFocus={this.handleFocus}
					onBlur={this.handleBlur}
					fullWidth={fullWidth}
					variant={variant}
					margin='dense'
					classes={{
						root: classes.textField
					}}
				>
					{this.getOptions().map(option => this.renderOption(option, selectedValues))}
				</TextField>
			</FormFieldContainer>
		)
	}



	renderOption(option, selectedValues) {
		const { classes } = this.props
		// Use option.value, option.code, or option.id as the value
		const optionValue = option.value || option.code || option.id
		const isSelected = selectedValues.includes(optionValue)

		return (
			<MenuItem
				key={util.empty(optionValue) ? '' : optionValue}
				value={util.empty(optionValue) ? '' : optionValue}
				classes={{ root: classes.option }}
				disabled={option.disabled}
			>
				<Checkbox checked={isSelected} />
				<ListItemText primary={this.getOptionText(option)} />
			</MenuItem>
		)
	}

	fixReference(reference) {
		if (reference) {
			reference.current = {
				focus: () => {
					if (this.inputReference.current && this.inputReference.current.node) {
						const input = this.inputReference.current.node
						const div = input.previousSibling
						div.focus()
					}
				}
			}
		}
	}

	getOptions() {
		const { options, emptyOption, sort = true } = this.props

		if (options) {
			let result = Array.from(options)

			if (sort) {
				result.sort((option1, option2) => {
					const text1 = this.getOptionText(option1)
					const text2 = this.getOptionText(option2)
					return compare(text1, text2)
				})
			}

			if (emptyOption) {
				result.unshift(emptyOption)
			}

			return result
		} else {
			return []
		}
	}

	getOptionText(option) {
		if (option) {
			return option.label || option.value || option.code || ''
		} else {
			return ''
		}
	}

	getOptionSelectable(option) {
		if (option) {
			return option.selectable === undefined || option.selectable
		} else {
			return false
		}
	}

	isEmptyValue(value) {
		return util.empty(value) || (Array.isArray(value) && value.length === 0)
	}

	normalizeValue(value) {
		if (this.isEmptyValue(value)) {
			return []
		}

		// Handle array of values
		if (Array.isArray(value)) {
			return value.map(item => {
				// If item is an object, extract the value property (code, value, or id)
				if (typeof item === 'object' && item !== null) {
					return item.code || item.value || item.id || item
				}
				// If item is already a primitive, return as is
				return item
			}).filter(val => val !== null && val !== undefined)
		}

		// Handle single value
		if (typeof value === 'object' && value !== null) {
			return [value.code || value.value || value.id || value]
		}

		return [value]
	}
}

export default withStyles(styles)(MultiSelectFormField)

