import ThemeProvider from '@src/js/components/common/theme/ThemeProviderDefault.jsx'
import { StyledEngineProvider } from "@mui/material/styles";
import Loading from '@src/js/components/common/loading/Loading.jsx'
import Resizable from '@src/js/components/common/resizable/Resizable.jsx'
import Browser from '@src/js/components/common/browser/Browser.jsx'
import BrowserCommon from '@src/js/components/common/browser/BrowserCommon.js'
import BrowserController from '@src/js/components/common/browser/BrowserController.js'
import Grid from '@src/js/components/common/grid/Grid.jsx'
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'
import SelectField from '@src/js/components/common/form/SelectField.jsx'
import DatePickerProvider from '@src/js/components/common/date/DatePickerProvider.jsx'
import DateRangeField from '@src/js/components/common/form/DateRangeField.jsx'
import ImagingDatasetViewer from '@src/js/components/common/imaging/ImagingDatasetViewer.jsx'
import ImagingGalleryViewer from "@src/js/components/common/imaging/ImagingGalleryViewer.jsx";
import DataBrowser from '@src/js/components/common/data-browser/DataBrowser.jsx';
import DataBrowserToolbar from '@src/js/components/common/data-browser/components/toolbar/DataBrowserToolbar.jsx'
import Menu from '@src/js/components/common/menu/Menu.jsx'
import LoadingDialog from "@src/js/components/common/loading/LoadingDialog.jsx";
import openbis from '@src/js/services/openbis.js'

export default {
  ThemeProvider,
  StyledEngineProvider,
  Loading,
  Resizable,
  Browser,
  BrowserCommon,
  BrowserController,
  Grid,
  GridExportOptions,
  SelectField,
  DatePickerProvider,
  DateRangeField,
  ImagingDatasetViewer,
  ImagingGalleryViewer,
  DataBrowser,
  DataBrowserToolbar,
  Menu,
  LoadingDialog,
  openbis
}
