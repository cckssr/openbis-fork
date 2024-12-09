const BLANK_IMG_SRC = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIAAAABgCAYAAADVenpJAAACqUlEQVR4nO3aSW4jURAD0eiG739l96LxYS88SZYqSSbjAC64+KAaJGittdZaa6211tqW/lxwjNcLjpHcUzf6+8w/3vQrgOW9DB77isuPUyOXyslPgN4bvDV2LqYvAUUwfA6uBvDRx/5mBB/975deGic+AYrgf+Pjw9wlYDsCifFh9h5gKwKZ8WH+JnAbAqnxYR4A7EEgNz5oAIB8BJLjgw4AyEUgOz5oAYA8BNLjgx4AyEEgPz5oAgB/BBbjgy4A8EVgMz5oAwA/BFbjgz4A8EFgNz54AAB9BJbjgw8A0EVgOz54AQA9BNbjgx8A0EFgPz54AoB5BBHjgy8AmEMQMz54A4DrEUSND/4A4DoEceNDBgB4PoLI8SEHADwPQez4kAUAHo8genzIAwCPQxA/PmQCgN8jWDE+5AKA+xGsGR+yAcDtCFaND/kA4OcI1o0POwDA9whWjg97AMDnCNaOD7sAwM+GXTM+7AMAXw+8anzYCaC9ayOAWx8Do9sG4N4XQbFtAvDZ3f70z8tG2wLgu0e9tQg2APjpc/5KBOkAbn3Jsw5BMoB73/CtQpAK4Levd9cgSATwqHf7KxCkAXj0FzvxCJIAPOtbvWgEKQCe/ZVuLIIEAFd9nx+JwB3A1T/miEPgDGDqlzxRCFwBTP+MKwaBI4Dp8b86ph0CNwAq4391bCsETgDUxj9ZI3ABoDr+yRaBAwD18U+WCNQBuIx/skOgDMBt/JMVAlUAruOfbBAoAnAf/2SBQA1AyvgneQRKANLGP0kjUAGQOv5JFoECgPTxT5IIpgFsGf8kh2ASwLbxT1IIpgBsHf8kg2ACwPbxTxIIrjjx3/1TG8d/3+j5mb4J3D4+DJ+DSQAd/62xc/EydWAEnoHb/CWgDVcArbXWWmuttdZaa0v6B3uNi6IiSsuWAAAAAElFTkSuQmCC'
const COLORMAP = 'Colormap';
const DEFAULT_COLLECTION_VIEW = 'DEFAULT_COLLECTION_VIEW';
const DEFAULT_OBJECT_VIEW = 'DEFAULT_OBJECT_VIEW';

//https://docs.bokeh.org/en/latest/_modules/bokeh/palettes.html#inferno
const DEFAULT_COLORMAP = {
    "gray":["#000000", "#252525", "#525252", "#737373", "#969696", "#bdbdbd", "#d9d9d9", "#f0f0f0", "#ffffff"],
    "YlOrBr":["#ffffe5","#fff7bc","#fee391","#fec44f","#fe9929","#ec7014","#cc4c02","#993404","#662506"],
    "viridis":['#440154', '#472B7A', '#3B518A', '#2C718E', '#208F8C', '#27AD80', '#5BC862', '#AADB32', '#FDE724'],
    "cividis":['#00204C', '#01356E', '#404C6B', '#5F636E', '#7B7B78', '#9B9377', '#BCAE6E', '#DFCB5D', '#FFE945'],
    "inferno":['#000003', '#1F0C47', '#550F6D', '#88216A', '#BA3655', '#E35832', '#F98C09', '#F8C931', '#FCFEA4'],
    "rainbow":['#882E72', '#1965B0', '#7BAFDE', '#4EB265', '#CAE0AB', '#F7F056', '#EE8026', '#DC050C', '#72190E'],
    "Spectral":["#3288bd", "#66c2a5", "#abdda4", "#e6f598", "#ffffbf", "#fee08b", "#fdae61", "#f46d43", "#d53e4f"],
    "RdBu":["#2166ac", "#4393c3", "#92c5de", "#d1e5f0", "#f7f7f7", "#fddbc7", "#f4a582", "#d6604d", "#b2182b"],
    "RdGy":["#4d4d4d", "#878787", "#bababa", "#e0e0e0", "#ffffff", "#fddbc7", "#f4a582", "#d6604d", "#b2182b"]
}
const DROPDOWN = 'Dropdown';
const EXPORT_TYPE = 'export';
const GENERATE = 'GENERATE';
const IMAGE_TYPE = 'image';
const IMAGING_CODE = 'imaging';
const IMAGING_DATA = 'IMAGING_DATA';
const IMAGING_DATA_CONFIG = 'IMAGING_DATA_CONFIG';
const IMAGING_NOTES = 'IMAGING_NOTES';
const IMAGING_TAGS = 'IMAGING_TAGS';
const IMAGING_TAGS_LABEL = 'Imaging Tags';
const METADATA_PREVIEW_COUNT = 'preview-total-count';
const MULTI_EXPORT_TYPE = 'multi-export';
const PREVIEW_TYPE = 'preview';
const RANGE = 'Range';
const SLIDER = 'Slider';
const USER_DEFINED_IMAGING_DATA = 'USER_DEFINED_IMAGING_DATA';

export default {
    BLANK_IMG_SRC,
    COLORMAP,
    DEFAULT_COLLECTION_VIEW,
    DEFAULT_OBJECT_VIEW,
    DEFAULT_COLORMAP,
    DROPDOWN,
    EXPORT_TYPE,
    GENERATE,
    IMAGE_TYPE,
    IMAGING_CODE,
    IMAGING_DATA,
    IMAGING_DATA_CONFIG,
    IMAGING_NOTES,
    IMAGING_TAGS,
    IMAGING_TAGS_LABEL,
    METADATA_PREVIEW_COUNT,
    MULTI_EXPORT_TYPE,
    PREVIEW_TYPE,
    RANGE,
    SLIDER,
    USER_DEFINED_IMAGING_DATA
}