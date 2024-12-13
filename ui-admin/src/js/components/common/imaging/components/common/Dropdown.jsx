import * as React from 'react';
import { FormControl, MenuItem, Select } from "@mui/material";
import OutlinedBox from "@src/js/components/common/imaging/components/common/OutlinedBox";

const Dropdown = ({ label, values, initValue, isMulti, disabled = false, onSelectChange = null }) => {
    const [value, setValue] = React.useState(initValue);

    React.useEffect(() => {
        //console.log("useEffect DROPDOWN: ", label, values, initValue, typeof initValue === "string", isMulti);
        if (initValue !== value) setValue(initValue);
    }, [initValue]);

    const handleChange = (event) => {
        setValue(event.target.value);
        if (onSelectChange != null) {
            onSelectChange(event);
        }
    };

    return (
        <OutlinedBox label={label}>
            <FormControl fullWidth variant="standard" onClick={event => event.stopPropagation()}>
                <Select
                    labelId={"select-" + label + "-label"}
                    id={"select-" + label}
                    value={value}
                    multiple={isMulti}
                    label={label}
                    name={label}
                    onChange={handleChange}
                    disabled={disabled}
                >
                    {values.map((v, i) => <MenuItem key={"select-" + label + "-menuitem-" + i} value={v} onClick={event => event.stopPropagation()}>{v}</MenuItem>)}
                </Select>
            </FormControl>
        </OutlinedBox>
    );
}

export default Dropdown;