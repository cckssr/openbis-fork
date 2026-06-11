# raw.json — Nanonis Imaging Configuration

`raw.json` is the **default imaging configuration** for the Nanonis SXM adapter. It tells openBIS how to display a Nanonis scan image: which controls to show the user, which filters are available, and what the default preview looks like.

When a new Nanonis dataset is imported, this file is read and stored as the dataset's `IMAGING_DATA_CONFIG` property.

---

## File Structure at a Glance

```
ImagingDataSetPropertyConfig
└── images[]
    └── ImagingDataSetImage
        ├── config (ImagingDataSetConfig)
        │   ├── adaptor          — Java class that renders images on the server
        │   ├── version          — config schema version
        │   ├── playable         — whether animation playback is supported
        │   ├── speeds[]         — playback speeds in milliseconds
        │   ├── resolutions[]    — available output resolutions
        │   ├── inputs[]         — display controls shown to the user
        │   ├── exports[]        — controls shown in the export dialog
        │   ├── filters{}        — image filters with their parameters
        │   └── filterSemanticAnnotation{}  — ontology links for filters
        ├── previews[]           — pre-rendered snapshots (one by default)
        └── metadata{}           — free-form key/value data
```

---

## Section Reference

### `adaptor`

```json
"adaptor": "ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor.NanonisSxmAdaptor"
```

The fully-qualified Java class name of the server-side component that reads the raw `.sxm` file and renders images. **Do not change this** unless you are switching to a different file format or adaptor.

---

### `resolutions`

```json
"resolutions": ["original", "200x200", "2000x2000"]
```

The output sizes the server can produce. `"original"` means the native pixel size of the scan. Add or remove entries to control what resolutions are offered in the UI.

---

### `playable` and `speeds`

```json
"playable": true,
"speeds": [1000, 2000, 5000]
```

`playable: true` enables animation mode (useful for time-series or multi-channel sweeps). `speeds` lists the available playback intervals in **milliseconds** — lower means faster.

Set `playable: false` and omit `speeds` if the dataset is a single static image.

---

### `inputs` — Display Controls

Controls shown in the image viewer that the user can interact with. The current file defines six:

| Label         | Type     | Purpose                                                            |
|---------------|----------|--------------------------------------------------------------------|
| `Channel`     | Dropdown | Selects which data channel to display (`z`, `I`, `dIdV`, `dIdV_Y`) |
| `X-axis`      | Range    | Crops the visible horizontal range                                 |
| `Y-axis`      | Range    | Crops the visible vertical range                                   |
| `Color-scale` | Range    | Sets the min/max of the color scale; changes unit per channel      |
| `Scaling`     | Dropdown | `linear` or `logarithmic` intensity mapping                        |
| `Colormap`    | Colormap | Color palette used to render the image                             |

#### Control types

| Type       | Key fields                | Description                                |
|------------|---------------------------|--------------------------------------------|
| `Dropdown` | `values`, `multiselect`   | Pick one (or more) from a fixed list       |
| `Slider`   | `range: [min, max, step]` | Single numeric value on a continuous scale |
| `Range`    | `range: [min, max, step]` | Two-value range (start and end)            |
| `Colormap` | `values`                  | Specialised dropdown for color palettes    |

#### `visibility` — conditional ranges

The `Color-scale` control uses `visibility` to show **different units and ranges** depending on which `Channel` is selected:

```json
{
  "label": "Channel",
  "values": ["z"],
  "range": ["-70.189766", "-69.88171", "0.001"],
  "unit": "nm"
}
```

This means: *when Channel = `z`, show the Color-scale range in nanometres with these bounds*. Each channel (`z`, `I`, `dIdV`, `dIdV_Y`) has its own entry.

---

### `exports` — Export Dialog Controls

Controls shown when the user clicks **Export**. They are not used for rendering.

| Label            | Type             | Options                        |
|------------------|------------------|--------------------------------|
| `include`        | Dropdown (multi) | `image`, `raw data`            |
| `image-format`   | Dropdown         | `png`, `svg`                   |
| `archive-format` | Dropdown         | `zip`, `tar`                   |
| `resolution`     | Dropdown         | `original`, `150dpi`, `300dpi` |

---

### `filters` — Image Filters

Each key is a filter name; its value is a list of parameter controls for that filter (can be empty if the filter needs no parameters).

```json
"filters": {
  "Gaussian":          [ /* Sigma slider, Truncate slider */ ],
  "Laplace":           [ /* Size slider */ ],
  "Zero background":   [],
  "Line Subtraction":  [],
  "Plane Subtraction": []
}
```

Empty arrays (`[]`) mean the filter is available but has no user-adjustable parameters.

---

### `filterSemanticAnnotation`

Maps a filter name to an ontology term. Used for machine-readable metadata.

```json
"filterSemanticAnnotation": {
  "Gaussian": {
    "ontologyId": "schema.org",
    "ontologyVersion": "https://schema.org/version/28.1",
    "ontologyAnnotationId": "https://schema.org/headline"
  }
}
```

This is optional — filters without an entry here still work normally.

---

### `previews`

A list of pre-rendered snapshots stored alongside the image. The default preview captures the initial view with:

```json
"config": {
  "Channel": "z",
  "X-axis": ["0", "3.0"],
  "Y-axis": ["0", "3.0"],
  "Color-scale": ["-70.189766", "-69.88171"],
  "Scaling": "linear",
  "Colormap": "gray"
}
```

---

## How to Extend

### Add a new channel

1. Add the channel name to the `Channel` dropdown `values`:
   ```json
   "values": ["z", "I", "dIdV", "dIdV_Y", "MyNewChannel"]
   ```
2. Add a matching entry in `Color-scale.visibility` with appropriate unit and range:
   ```json
   {
     "label": "Channel",
     "values": ["MyNewChannel"],
     "range": ["-1.0", "1.0", "0.01"],
     "unit": "nA"
   }
   ```

---

### Add a new colormap

Append its name to the `Colormap` control's `values` list. The name must be a valid [Matplotlib colormap](https://matplotlib.org/stable/gallery/color/colormap_reference.html):

```json
"values": ["gray", "YlOrBr", "viridis", "cividis", "inferno", "rainbow", "Spectral", "RdBu", "RdGy", "plasma"]
```

---

### Add a new export resolution

Append a `"<width>dpi"` string to the `resolution` export control:

```json
"values": ["original", "150dpi", "300dpi", "600dpi"]
```

---

### Add a new filter without parameters

Add the filter name as a key with an empty array:

```json
"filters": {
  "Gaussian": [ ... ],
  "My New Filter": []
}
```

Handling of the new filter needs to be supported by the adapter code!

---

### Add a new filter with parameters

Add the filter name and define its controls. This example adds a **Median** filter with a kernel size slider:

```json
"filters": {
  "Gaussian": [ ... ],
  "Median": [
    {
      "@type": "imaging.dto.ImagingDataSetControl",
      "type": "Slider",
      "label": "Kernel size",
      "section": "Median",
      "range": ["3", "21", "2"],
      "unit": null,
      "speeds": null,
      "values": null,
      "playable": null,
      "visibility": null,
      "multiselect": null,
      "metadata": null,
      "semanticAnnotation": null
    }
  ]
}
```

> **Note on `@id`:** The `@id` numbers in the original file are assigned automatically during serialisation. When editing `raw.json` by hand you can set them to any unique integers, or omit them — they are not used for logic.

---

### Add a second image

Append a new object to the top-level `images` array. Each image has its own independent `config`, `previews`, and `metadata`:

```json
"images": [
  { /* existing image, index: 0 */ },
  {
    "@type": "imaging.dto.ImagingDataSetImage",
    "index": 1,
    "config": { /* ... */ },
    "previews": [ { "@type": "imaging.dto.ImagingDataSetPreview", "format": "png", "config": {}, "index": 0, "comment": "", "tags": [], "filterConfig": [] } ],
    "imageConfig": {},
    "metadata": { "comment": "Second channel overview" }
  }
]
```