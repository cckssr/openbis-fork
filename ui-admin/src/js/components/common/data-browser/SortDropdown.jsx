import React, { useState } from "react";
import { Box, Select, MenuItem, FormControl, IconButton } from "@mui/material";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";

const SortDropdown = ({ columns, onSortChange }) => {
  const [sortingColumn, setSortingColumn] = useState(columns[0]?.name || "");
  const [sortingOrder, setSortingOrder] = useState("asc");


  const handleSortChange = (event) => {
    const selectedColumn = columns.find((col) => col.name === event.target.value);
    if (selectedColumn) {
      setSortingColumn(selectedColumn.name);
      setSortingOrder("asc");
      onSortChange(selectedColumn, "asc");
    }
  };


  const toggleSortOrder = () => {
    const newOrder = sortingOrder === "asc" ? "desc" : "asc";
    setSortingOrder(newOrder);
    const selectedColumn = columns.find((col) => col.name === sortingColumn);
    onSortChange(selectedColumn, newOrder);
  };

  return (
    <Box display="flex" alignItems="center" justifyContent="flex-end" gap={1} width="100%">
      <FormControl variant="standard" size="small" sx={{ minWidth: 180 }}>
        <Select
          value={sortingColumn}
          onChange={handleSortChange}
          disableUnderline
          IconComponent={ArrowDropDownIcon}
          MenuProps={{
            sx: {
              "& .MuiPaper-root": { textAlign: "right" },
            },
          }}
          sx={{
            backgroundColor: "transparent",
            "& .MuiOutlinedInput-notchedOutline": { border: "none" },
            "&:hover .MuiOutlinedInput-notchedOutline": { border: "none" },
            "&.Mui-focused .MuiOutlinedInput-notchedOutline": { border: "none" },
            "& .MuiSelect-select": {
              padding: 0,
              fontSize: "0.875rem",
              fontWeight: 400,
              textAlign: "right",
            },
          }}
        >
          {columns.map((column) => (
            <MenuItem key={column.name} value={column.name} sx={{ textAlign: "right" }}>
              {column.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <IconButton
        onClick={toggleSortOrder}
        size="small"
        title="Reverse Sort Direction"
        sx={{
          borderRadius: "4px",
          backgroundColor: sortingOrder ? "rgba(0, 0, 0, 0.05)" : "transparent",
          "&:hover": { backgroundColor: "rgba(0, 0, 0, 0.1)" },
          color: sortingOrder === "asc" ? "primary.main" : "secondary.main",
          padding: "3px",
        }}
      >
        {sortingOrder === "asc" ? <ArrowUpwardIcon sx={{ fontSize: 18 }} /> : <ArrowDownwardIcon sx={{ fontSize: 18 }} />}
      </IconButton>
    </Box>
  );
};

export default SortDropdown;
