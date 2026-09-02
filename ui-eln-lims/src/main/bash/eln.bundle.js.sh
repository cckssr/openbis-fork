#!/bin/bash

# 0. Define input folder
INPUT_FOLDER="../../../src/core-plugins/eln-lims/src/as/webapps/eln-lims/html/js"

# 1. Define the output file
OUTPUT_FILE="../../../src/core-plugins/eln-lims/src/as/webapps/eln-lims/html/eln.bundle.js"

# 2. List your files in the exact order they need to load
FILES=(
  "$INPUT_FOLDER/controllers/LayoutManager.js"
  "$INPUT_FOLDER/controllers/MainController.js"

  "$INPUT_FOLDER/util/ChatAssistantManager.js"
  "$INPUT_FOLDER/util/IdentifierUtil.js"
  "$INPUT_FOLDER/config/ELNDictionary.js"
  "$INPUT_FOLDER/config/Profile.js"
  "$INPUT_FOLDER/config/StandardProfile.js"
  "$INPUT_FOLDER/config/SettingsManager.js"
  "$INPUT_FOLDER/server/ServerFacade.js"

  "$INPUT_FOLDER/util/Util.js"
  "$INPUT_FOLDER/util/ExportUtil.js"
  "$INPUT_FOLDER/config/ELNLIMSPlugin.js"
  "$INPUT_FOLDER/util/FormUtil.js"
  "$INPUT_FOLDER/util/Select2Manager.js"
  "$INPUT_FOLDER/util/CKEditorManager.js"
  "$INPUT_FOLDER/util/JExcelEditorManager.js"
  "$INPUT_FOLDER/util/PrintUtil.js"
  "$INPUT_FOLDER/util/TreeUtil.js"
  "$INPUT_FOLDER/util/JupyterUtil.js"
  "$INPUT_FOLDER/util/AnnotationUtil.js"
  "$INPUT_FOLDER/util/HierarchyUtil.js"
  "$INPUT_FOLDER/util/BarcodeUtil.js"
  "$INPUT_FOLDER/util/IconUtil.js"
  "$INPUT_FOLDER/util/LabelUtil.js"

  "$INPUT_FOLDER/views/TrashManager/TrashManagerController.js"
  "$INPUT_FOLDER/views/TrashManager/TrashManagerModel.js"
  "$INPUT_FOLDER/views/TrashManager/TrashManagerView.js"

  "$INPUT_FOLDER/views/StorageManager/StorageManagerController.js"
  "$INPUT_FOLDER/views/StorageManager/StorageManagerModel.js"
  "$INPUT_FOLDER/views/StorageManager/StorageManagerView.js"
  "$INPUT_FOLDER/views/StorageManager/widgets/StorageController.js"
  "$INPUT_FOLDER/views/StorageManager/widgets/StorageModel.js"
  "$INPUT_FOLDER/views/StorageManager/widgets/StorageView.js"
  "$INPUT_FOLDER/views/StorageManager/widgets/GridController.js"
  "$INPUT_FOLDER/views/StorageManager/widgets/GridModel.js"
  "$INPUT_FOLDER/views/StorageManager/widgets/GridView.js"

  "$INPUT_FOLDER/views/DataGrid/DataGridExportOptions.js"
  "$INPUT_FOLDER/views/DataGrid/DataGridController.js"
  "$INPUT_FOLDER/views/DataGrid/SampleDataGridUtil.js"

  "$INPUT_FOLDER/views/SampleForm/SampleFormController.js"
  "$INPUT_FOLDER/views/SampleForm/SampleFormModel.js"
  "$INPUT_FOLDER/views/SampleForm/SampleFormView.js"

  "$INPUT_FOLDER/views/AdvancedSearch/AdvancedSearchController.js"
  "$INPUT_FOLDER/views/AdvancedSearch/AdvancedSearchModel.js"
  "$INPUT_FOLDER/views/AdvancedSearch/AdvancedSearchView.js"

  "$INPUT_FOLDER/views/SampleForm/widgets/StorageListController.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/StorageListModel.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/StorageListView.js"

  "$INPUT_FOLDER/views/SampleForm/widgets/DilutionTableController.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/DilutionTableModel.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/DilutionTableView.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/FreeFormTableController.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/FreeFormTableModel.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/FreeFormTableView.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/DeleteEntityController.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/DeleteEntityModel.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/DeleteEntityView.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/CommentsController.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/CommentsModel.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/CommentsView.js"

  "$INPUT_FOLDER/views/SampleForm/widgets/ordering/NewProductsController.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/ordering/NewProductsModel.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/ordering/NewProductsView.js"

  "$INPUT_FOLDER/views/SampleForm/widgets/LinksController.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/LinksModel.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/LinksView.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/SampleField.js"
  "$INPUT_FOLDER/views/SampleForm/widgets/EntityLinkWidget.js"

  "$INPUT_FOLDER/views/SpaceForm/SpaceFormController.js"
  "$INPUT_FOLDER/views/SpaceForm/SpaceFormModel.js"
  "$INPUT_FOLDER/views/SpaceForm/SpaceFormView.js"

  "$INPUT_FOLDER/views/ProjectForm/ProjectFormController.js"
  "$INPUT_FOLDER/views/ProjectForm/ProjectFormModel.js"
  "$INPUT_FOLDER/views/ProjectForm/ProjectFormView.js"

  "$INPUT_FOLDER/views/ExperimentForm/ExperimentFormController.js"
  "$INPUT_FOLDER/views/ExperimentForm/ExperimentFormModel.js"
  "$INPUT_FOLDER/views/ExperimentForm/ExperimentFormView.js"

  "$INPUT_FOLDER/views/DropboxMonitor/DropboxMonitorController.js"
  "$INPUT_FOLDER/views/DropboxMonitor/DropboxMonitorModel.js"
  "$INPUT_FOLDER/views/DropboxMonitor/DropboxMonitorView.js"
  "$INPUT_FOLDER/views/DropboxMonitor/DropboxMonitorUtil.js"
  "$INPUT_FOLDER/views/DropboxMonitor/modal/DropboxLogsController.js"
  "$INPUT_FOLDER/views/DropboxMonitor/modal/DropboxLogsModel.js"
  "$INPUT_FOLDER/views/DropboxMonitor/modal/DropboxLogsView.js"
  "$INPUT_FOLDER/views/ArchivingHelper/ArchivingHelperController.js"
  "$INPUT_FOLDER/views/ArchivingHelper/ArchivingHelperModel.js"
  "$INPUT_FOLDER/views/ArchivingHelper/ArchivingHelperView.js"
  "$INPUT_FOLDER/views/UnarchivingHelper/UnarchivingHelperController.js"
  "$INPUT_FOLDER/views/UnarchivingHelper/UnarchivingHelperModel.js"
  "$INPUT_FOLDER/views/UnarchivingHelper/UnarchivingHelperView.js"
  "$INPUT_FOLDER/views/UserManager/UserManagerController.js"
  "$INPUT_FOLDER/views/UserManager/UserManagerModel.js"
  "$INPUT_FOLDER/views/UserManager/UserManagerView.js"
  "$INPUT_FOLDER/views/UserManager/modal/CreateUserController.js"
  "$INPUT_FOLDER/views/UserManager/modal/CreateUserModel.js"
  "$INPUT_FOLDER/views/UserManager/modal/CreateUserView.js"
  "$INPUT_FOLDER/views/UserManager/modal/ResetPasswordController.js"
  "$INPUT_FOLDER/views/UserManager/modal/ResetPasswordModel.js"
  "$INPUT_FOLDER/views/UserManager/modal/ResetPasswordView.js"
  "$INPUT_FOLDER/views/UserManagementConfig/UserManagementConfigController.js"
  "$INPUT_FOLDER/views/UserManagementConfig/UserManagementConfigModel.js"
  "$INPUT_FOLDER/views/UserManagementConfig/UserManagementConfigView.js"

  "$INPUT_FOLDER/views/CustomImport/CustomImportController.js"
  "$INPUT_FOLDER/views/CustomImport/CustomImportModel.js"
  "$INPUT_FOLDER/views/CustomImport/CustomImportView.js"

  "$INPUT_FOLDER/views/DataSetForm/DataSetFormController.js"
  "$INPUT_FOLDER/views/DataSetForm/DataSetFormModel.js"
  "$INPUT_FOLDER/views/DataSetForm/DataSetFormView.js"

  "$INPUT_FOLDER/views/DataSetForm/widgets/AdvancedEntitySearchDropdown.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/DatasetViewerController.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/DatasetViewerModel.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/DatasetViewerView.js"

  "$INPUT_FOLDER/views/DataSetForm/widgets/JupyterNotebookController.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/JupyterNotebookModel.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/JupyterNotebookView.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/JupyterCopyNotebookController.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/JupyterCopyNotebookModel.js"
  "$INPUT_FOLDER/views/DataSetForm/widgets/JupyterCopyNotebookView.js"

  "$INPUT_FOLDER/views/DataSetForm/widgets/ImagePreviewIconLoader.js"

  "$INPUT_FOLDER/views/SideMenu/SideMenuWidgetController.js"
  "$INPUT_FOLDER/views/SideMenu/SideMenuWidgetBrowserController.js"
  "$INPUT_FOLDER/views/SideMenu/SideMenuWidgetModel.js"
  "$INPUT_FOLDER/views/SideMenu/SideMenuWidgetView.js"
  "$INPUT_FOLDER/views/SideMenu/SideMenuWidgetViewController.js"

  "$INPUT_FOLDER/views/VocabularyManager/VocabularyManagerController.js"
  "$INPUT_FOLDER/views/VocabularyManager/VocabularyManagerModel.js"
  "$INPUT_FOLDER/views/VocabularyManager/VocabularyManagerView.js"

  "$INPUT_FOLDER/views/HierarchyTable/HierarchyTableController.js"
  "$INPUT_FOLDER/views/HierarchyTable/HierarchyTableModel.js"
  "$INPUT_FOLDER/views/HierarchyTable/HierarchyTableView.js"
  "$INPUT_FOLDER/views/HierarchyTable/widgets/HierarchyFilterController.js"
  "$INPUT_FOLDER/views/HierarchyTable/widgets/HierarchyFilterModel.js"
  "$INPUT_FOLDER/views/HierarchyTable/widgets/HierarchyFilterView.js"

  "$INPUT_FOLDER/views/History/HistoryController.js"
  "$INPUT_FOLDER/views/History/HistoryModel.js"
  "$INPUT_FOLDER/views/History/HistoryView.js"

  "$INPUT_FOLDER/views/SampleTable/SampleTableController.js"
  "$INPUT_FOLDER/views/SampleTable/SampleTableModel.js"
  "$INPUT_FOLDER/views/SampleTable/SampleTableView.js"
  "$INPUT_FOLDER/views/SampleTable/widgets/BatchController.js"
  "$INPUT_FOLDER/views/SampleTable/widgets/BatchModel.js"
  "$INPUT_FOLDER/views/SampleTable/widgets/BatchView.js"
  "$INPUT_FOLDER/views/SampleTable/widgets/TypeAndFileController.js"
  "$INPUT_FOLDER/views/SampleTable/widgets/TypeAndFileModel.js"
  "$INPUT_FOLDER/views/SampleTable/widgets/TypeAndFileView.js"

  "$INPUT_FOLDER/views/MainHeader/MainHeaderController.js"
  "$INPUT_FOLDER/views/MainHeader/MainHeaderModel.js"
  "$INPUT_FOLDER/views/MainHeader/MainHeaderView.js"

  "$INPUT_FOLDER/views/Login/LoginView.js"

  "$INPUT_FOLDER/views/legacy/SampleHierarchy.js"

  "$INPUT_FOLDER/views/LabNotebook/LabNotebookController.js"
  "$INPUT_FOLDER/views/LabNotebook/LabNotebookModel.js"
  "$INPUT_FOLDER/views/LabNotebook/LabNotebookView.js"

  "$INPUT_FOLDER/views/SettingsForm/SettingsFormController.js"
  "$INPUT_FOLDER/views/SettingsForm/SettingsFormModel.js"
  "$INPUT_FOLDER/views/SettingsForm/SettingsFormView.js"
  "$INPUT_FOLDER/views/SettingsForm/widgets/InstanceSettingsController.js"
  "$INPUT_FOLDER/views/SettingsForm/widgets/InstanceSettingsModel.js"
  "$INPUT_FOLDER/views/SettingsForm/widgets/InstanceSettingsView.js"
  "$INPUT_FOLDER/views/SettingsForm/widgets/RootNodeSettings.js"
  "$INPUT_FOLDER/views/SettingsForm/widgets/ToolbarSettings.js"

  "$INPUT_FOLDER/views/Inventory/InventoryController.js"
  "$INPUT_FOLDER/views/Inventory/InventoryModel.js"
  "$INPUT_FOLDER/views/Inventory/InventoryView.js"

  "$INPUT_FOLDER/views/Stock/StockController.js"
  "$INPUT_FOLDER/views/Stock/StockModel.js"
  "$INPUT_FOLDER/views/Stock/StockView.js"

  "$INPUT_FOLDER/views/UserProfile/UserProfileController.js"
  "$INPUT_FOLDER/views/UserProfile/UserProfileModel.js"
  "$INPUT_FOLDER/views/UserProfile/UserProfileView.js"

  "$INPUT_FOLDER/views/Export/ExportTreeController.js"
  "$INPUT_FOLDER/views/Export/ExportTreeModel.js"
  "$INPUT_FOLDER/views/Export/ExportTreeView.js"

  "$INPUT_FOLDER/views/ResearchCollectionExport/ResearchCollectionExportController.js"
  "$INPUT_FOLDER/views/ResearchCollectionExport/ResearchCollectionExportModel.js"
  "$INPUT_FOLDER/views/ResearchCollectionExport/ResearchCollectionExportView.js"

  "$INPUT_FOLDER/views/ZenodoExport/ZenodoExportController.js"
  "$INPUT_FOLDER/views/ZenodoExport/ZenodoExportModel.js"
  "$INPUT_FOLDER/views/ZenodoExport/ZenodoExportView.js"

  "$INPUT_FOLDER/views/DrawingBoards/DrawingBoardsController.js"
  "$INPUT_FOLDER/views/DrawingBoards/DrawingBoardsModel.js"
  "$INPUT_FOLDER/views/DrawingBoards/DrawingBoardsView.js"

  "$INPUT_FOLDER/views/Shared/widgets/MoveEntityController.js"
  "$INPUT_FOLDER/views/Shared/widgets/MoveEntityModel.js"
  "$INPUT_FOLDER/views/Shared/widgets/MoveEntityView.js"

  "$INPUT_FOLDER/views/DataGrid/ExperimentDataGridUtil.js"
  "$INPUT_FOLDER/views/ExperimentTable/ExperimentTableController.js"
  "$INPUT_FOLDER/views/ExperimentTable/ExperimentTableModel.js"
  "$INPUT_FOLDER/views/ExperimentTable/ExperimentTableView.js"

  "$INPUT_FOLDER/views/TabContent/TabContentController.js"
  "$INPUT_FOLDER/views/TabContent/TabContentModel.js"
  "$INPUT_FOLDER/views/TabContent/TabContentViewer.js"
  "$INPUT_FOLDER/views/TabContent/TabController.js"
  "$INPUT_FOLDER/util/TabContentUtil.js"

  "$INPUT_FOLDER/views/RoCrateExport/RoCrateExportController.js"
  "$INPUT_FOLDER/views/RoCrateExport/RoCrateExportModel.js"
  "$INPUT_FOLDER/views/RoCrateExport/RoCrateExportView.js"
  "$INPUT_FOLDER/views/RoCrateImport/RoCrateImportController.js"
  "$INPUT_FOLDER/views/RoCrateImport/RoCrateImportModel.js"
  "$INPUT_FOLDER/views/RoCrateImport/RoCrateImportView.js"

  "$INPUT_FOLDER/views/SciCatExport/SciCatExportController.js"
  "$INPUT_FOLDER/views/SciCatExport/SciCatExportModel.js"
  "$INPUT_FOLDER/views/SciCatExport/SciCatExportView.js"

  "$INPUT_FOLDER/../plugins/generic/plugin.js"

  "$INPUT_FOLDER/util/EventUtil.js"
  "$INPUT_FOLDER/test/TestUtil.js"
  "$INPUT_FOLDER/test/UserTests.js"
  "$INPUT_FOLDER/test/AdminTests.js"
  "$INPUT_FOLDER/test/TestProtocol.js"
  "$INPUT_FOLDER/test/ReactTestUtils.js"
)

# 3. Bundle
./bundle.sh "$OUTPUT_FILE" "${FILES[@]}"