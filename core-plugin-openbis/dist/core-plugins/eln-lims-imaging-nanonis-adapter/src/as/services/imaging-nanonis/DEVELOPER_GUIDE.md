# Nanonis Python Scripts — Developer Guide

This guide is for developers who need to modify or extend the Python scripts that process Nanonis measurement files (`.sxm`, `.dat`) inside the openBIS imaging-nanonis adapter.

---

## How the Scripts Fit Together

```
Java Adaptor (NanonisSxmAdaptor / NanonisDatAdaptor)
    │
    │  subprocess call with 7 JSON arguments
    ▼
nanonis_sxm.py   or   nanonis_dat.py   ← entrypoints
    │
    │  imports
    ▼
nanonis_core.py                         ← shared image-generation logic
    │
    │  uses
    ▼
spmpy (bundled library)  +  spiepy  +  skimage  +  matplotlib
```

The Java adaptors launch the Python scripts as subprocesses. The script writes a single JSON object to **stdout**, which Java reads back as the preview result. Everything printed before that final JSON line is treated as debug output.

---

## CLI Contract (all three scripts)

All entrypoint scripts receive exactly **7 positional arguments** from the Java adaptor:

| Position | Variable | Type | Description |
|---|---|---|---|
| `argv[1]` | `file` | `str` | Absolute path to the dataset directory |
| `argv[2]` | `format` | `str` | Output image format (`png`, `jpeg`, etc.) |
| `argv[3]` | `image_config` | JSON → `dict` | Image-level configuration |
| `argv[4]` | `image_metadata` | JSON → `dict` | Image-level metadata |
| `argv[5]` | `preview_config` | JSON → `dict` | Preview parameters (main config used for rendering) |
| `argv[6]` | `preview_metadata` | JSON → `dict` | Preview metadata |
| `argv[7]` | `filter_config` | JSON → `list` | Ordered list of filter descriptors |

The script must write **exactly one line** to stdout that is a valid JSON object matching:

```json
{
  "bytes": "<base64-encoded image>",
  "width": 640,
  "height": 480
}
```

Any other `print()` calls before that line are safe — Java only parses the last output line that matches this shape.

---

## `nanonis_core.py` — Shared Logic

This module is imported by both entrypoints. It contains all image-generation and filtering logic. **When adding a new filter or changing rendering behaviour, this is the primary file to modify.**

### Functions

#### `load_image(path) → spm`
Loads a Nanonis `.sxm` file using the bundled `spmpy` library.

#### `get_lock_in(img) → str`
Returns the lock-in status string from file metadata (`lock-in>lock-in status`).

#### `get_channel(img, channel_name='z') → array`
Extracts a named channel from an `spm` image object.

#### `get_upper_case_dict(params) → dict`
Normalises all keys of a dictionary to uppercase. Used internally to make filter-parameter lookups case-insensitive.

#### `remove_line_average(chData) → ndarray`
Removes the linear trend (slope + offset) from each row of a 2-D array independently. Rows containing `NaN` are skipped. Used by the `LINE SUBTRACTION` filter.

---

#### `get_sxm_image(sxm_file_path, format, channel_name, x_axis, y_axis, scaling, color_scale, colormap, colormap_scaling, resolution, filter, other_params, filter_config, print_out=True) → (preview_dict | None, spm)`

Core function for rendering a `.sxm` file.

**Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `sxm_file_path` | `str` | Path to the `.sxm` file |
| `format` | `str` | Matplotlib output format (`png`, etc.) |
| `channel_name` | `str` | Channel to render (e.g. `z`, `LI Demod 1 X`) |
| `x_axis` | `list[float]` | X-axis display range `[min, max]` |
| `y_axis` | `list[float]` | Y-axis display range `[min, max]` |
| `scaling` | `str` | `'linear'` or `'logarithmic'` |
| `color_scale` | `list[float]` | Colour clipping range `[min, max]` in data units |
| `colormap` | `str` | Matplotlib colormap name |
| `colormap_scaling` | `bool` | Whether to rescale the colormap to the clipping range |
| `resolution` | `float \| 'figure'` | DPI for output image; `'figure'` uses matplotlib default |
| `filter` | `str` | **Legacy** single-filter name (see below). Use `filter_config` instead |
| `other_params` | `dict` | Extra parameters passed through (e.g. label display flags) |
| `filter_config` | `list[dict]` | Ordered list of filters to apply (takes priority over `filter`) |
| `print_out` | `bool` | If `False`, skips base64 encoding and returns `(None, img)` |

**Returns:** `(preview_dict, spm_image_object)`

`preview_dict` is `None` when `print_out=False`, otherwise:
```python
{'bytes': '<base64>', 'width': int, 'height': int}
```

**Extra params recognised in `other_params`:**

| Key | Values | Default | Effect |
|---|---|---|---|
| `include labels` | `"true"` / `"false"` | `"true"` | Show axis labels on the plot |
| `include parameters` | `"true"` / `"false"` | `"false"` | Show measurement parameters on the plot |

---

#### `get_dat_image(folder_dir, format, channel_x, channel_y, x_axis, y_axis, colormap, scaling, grouping, print_legend, resolution) → preview_dict`

Renders one or more `.dat` spectroscopy curves as a single figure.

**Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `folder_dir` | `str` | Directory containing the `.dat` files |
| `format` | `str` | Matplotlib output format |
| `channel_x` | `str` | Name of the X-axis channel (e.g. `Bias (V)`) |
| `channel_y` | `str` | Name of the Y-axis channel (e.g. `Current (A)`) |
| `x_axis` | `list[float]` | X-axis display range |
| `y_axis` | `list[float]` | Y-axis display range |
| `colormap` | `str` | Matplotlib colormap for multi-curve colouring |
| `scaling` | `str` | `'linear'` or `'logarithmic'` |
| `grouping` | `list[str]` | List of file names (without extension) to include. Files not in this list are excluded |
| `print_legend` | `bool` | Whether to print curve names in the legend |
| `resolution` | `float \| 'figure'` | DPI for output image |

Files are sorted by their embedded `Saved Date` timestamp before plotting. The subset in `grouping` is then further sorted alphabetically.

---

### Filters

Filters are applied to the raw channel data **before** plotting. The colour-scale range is rescaled proportionally after each filter so it tracks the same relative portion of the data.

Filters can be specified in two ways:

1. **`filter_config`** (preferred) — a list of filter dicts applied in order:
   ```json
   [
     {"gaussian": {"sigma": 2, "truncate": 4.0}},
     {"line subtraction": {}}
   ]
   ```

2. **`filter`** (legacy, single filter) — a string name passed directly, with extra parameters in `other_params`.

Both paths apply the same filters; `filter_config` takes priority when it is non-empty.

#### Available filters

| Filter name | Parameters | Description |
|---|---|---|
| `GAUSSIAN` | `SIGMA` (int), `TRUNCATE` (float) | Gaussian blur via `skimage.filters.gaussian` |
| `LAPLACE` | `SIZE` (int) | Laplace edge-detection via `skimage.filters.laplace` |
| `ZERO BACKGROUND` | _(none)_ | Shifts all data so the minimum value becomes zero |
| `PLANE SUBTRACTION` | _(none)_ | Fits and subtracts a plane using `spiepy.flatten_xy`. Handles partial-scan images with trailing `NaN` rows |
| `LINE SUBTRACTION` | _(none)_ | Removes a linear (slope + offset) trend from each row independently |

#### Adding a new filter

1. Add a new `elif` branch inside the `for f in filter_config:` loop in `get_sxm_image`.
2. Match on `filter_name.upper()`.
3. Apply your transformation to `chData` (a 2-D `ndarray`).
4. The colour-scale rescaling that follows the `if/elif` block is automatic — do not duplicate it.
5. Mirror the same branch in the `elif filter != "NONE":` block for legacy single-filter support.

---

## `nanonis_sxm.py` — SXM Entrypoint

**File type handled:** `.sxm` (scanning tunnelling microscopy images)

This script is the subprocess entrypoint for `NanonisSxmAdaptor`. It:

1. Reads the 7 CLI arguments.
2. Finds the first `.sxm` file inside the dataset directory.
3. Calls `sxm_mode()`, which parses `preview_config` and delegates to `get_sxm_image()`.
4. Prints the preview JSON to stdout.

### `sxm_mode(sxm_file_path, format, parameters, filter_config, print_out=True)`

Parses `preview_config` key-value pairs (case-insensitive) into typed arguments for `get_sxm_image`.

**Recognised `preview_config` keys:**

| Key | Expected value | Notes |
|---|---|---|
| `channel` | `str` | Channel name (e.g. `z`) |
| `x-axis` | `list` of numeric strings | Cast to `float` |
| `y-axis` | `list` of numeric strings | Cast to `float` |
| `color-scale` | `list` of numeric strings | Cast to `float` |
| `colormap` | `str` | Matplotlib colormap name |
| `scaling` | `str` | `linear` or `logarithmic` |
| `colormap_scaling` | `str` | `"true"` / `"false"` |
| `resolution` | `str` | `"ORIGINAL"`, `"150DPI"`, or a bare number |
| `filter` | `str` | Legacy filter name; use `filter_config` instead |
| _(anything else)_ | any | Passed through in `other_params` |

---

## `nanonis_dat.py` — DAT/Spectra Entrypoint

**File type handled:** `.dat` (point spectroscopy sweeps)

This script is the subprocess entrypoint for `NanonisDatAdaptor`. It has **two rendering modes** controlled by the `spectraLocator` key in `preview_config`.

### Mode 1 — Standard DAT plot (`spectraLocator` absent or `"FALSE"`)

Calls `dat_mode(preview_config)`, which parses parameters and delegates to `get_dat_image()`.

**Recognised `preview_config` keys:**

| Key | Expected value | Notes |
|---|---|---|
| `channel x` | `str` | X-axis data channel |
| `channel y` | `str` | Y-axis data channel |
| `x-axis` | `list` of numeric strings | Cast to `float` |
| `y-axis` | `list` of numeric strings | Cast to `float` |
| `grouping` | `list[str]` | File names to include |
| `colormap` | `str` | Matplotlib colormap |
| `scaling` | `str` | `linear` or `logarithmic` |
| `color` | any | Optional per-curve colour override |
| `print legend` | `str` | `"true"` / `"false"` |
| `resolution` | `str` | `"ORIGINAL"`, `"150DPI"`, or a bare number |

### Mode 2 — Spectra Locator (`spectraLocator == "TRUE"`)

Overlays the spectra acquisition point locations on top of a background SXM image. This is a composite rendering mode used when `.dat` files are spatially linked to an `.sxm` scan.

**Additional `preview_config` keys required in this mode:**

| Key | Description |
|---|---|
| `sxmPreviewConfig` | Nested `dict` — the SXM rendering parameters (same keys as SXM mode) |
| `sxmRootPath` | Absolute path to the SXM dataset directory (injected by Java, not set manually) |
| `sxmFilePath` | Relative path to the `.sxm` file within the SXM dataset |
| `Grouping` | Optional list of `.dat` file names to include (if absent, all files are shown) |
| `resolution` | Output DPI (same syntax as other modes) |

The Java adaptor (`NanonisDatAdaptor`) is responsible for resolving `sxmPermId` to the physical `sxmRootPath` before the script is called.

**Extra fields in the output preview dict (Spectra Locator only):**

```json
{
  "bytes": "...",
  "width": 640,
  "height": 480,
  "spectraLocator": "true",
  "sxmPermId": "<dataset-perm-id>",
  "sxmFilePath": "<relative-path-to-sxm>",
  "sxmConfig": "<JSON string of sxmPreviewConfig>"
}
```

---

## Dependencies

| Library | Used for |
|---|---|
| `spmpy` (bundled) | Reading `.sxm` and `.dat` files; plotting |
| `numpy` | Array operations and NaN handling |
| `matplotlib` | Figure rendering and base64 export |
| `skimage` | Gaussian and Laplace filters |
| `spiepy` | Plane flattening (`flatten_xy`) |
| `PIL` / `Pillow` | Imported but not directly used in rendering |

> **Note:** `spmpy_terry.py` is a deprecated predecessor to `spmpy`. It is kept for reference but should not be used in new code. The commented-out imports at the top of each script show where the old library was used.

---

## Common Modification Scenarios

### Add a new image filter

1. Edit `nanonis_core.py` → `get_sxm_image`.
2. In the `for f in filter_config:` loop, add a new `elif filter_name.upper() == "MY FILTER":` branch.
3. Transform `chData` in place or reassign it.
4. Optionally mirror the change in the `elif filter != "NONE":` block for legacy support.
5. Document the filter name and its parameters in the imaging dataset type configuration.

### Add a new rendering parameter for SXM

1. Add a new `elif key == 'my-param':` branch inside `sxm_mode()` in `nanonis_sxm.py` (and in the duplicate `sxm_mode()` in `nanonis_dat.py` if the parameter should also work in spectra-locator mode).
2. Pass the parsed value through to `get_sxm_image()` via `other_params`, or add it as an explicit argument if the function signature needs to change.

### Add a new rendering parameter for DAT

1. Add a new `elif key == 'my-param':` branch inside `dat_mode()` in `nanonis_dat.py`.
2. Pass the parsed value through to `get_dat_image()` in `nanonis_core.py`, updating the function signature as needed.

### Change the output image size or DPI

The `resolution` parameter controls DPI. The rendered pixel dimensions depend on matplotlib's default figure size multiplied by DPI. To change the figure dimensions, modify the `img.plot(...)` or `plotting.specs_plot(...)` call, or set `plt.rcParams['figure.figsize']` before rendering.