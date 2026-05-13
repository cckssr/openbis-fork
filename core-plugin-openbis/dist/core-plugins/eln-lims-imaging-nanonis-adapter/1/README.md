# IMAGING-NANONIS core plugin
This core-plugin was created to showcase an imaging technology plugin capabilities with scientific data. It contains a set of input
.SXM and .DAT files with scripts to turn them into images. 


## Structure
This repository is split into following sections:
- `as` - directory with implementation of openbis core-plugin. It contains required master-data and adaptors needed for data-to-image conversion. 
- `nanonis-importer` - python script for uploading .sxm and .dat files to openbis in proper imaging format  
- `scripts` - directory with helpful scripts and python requirements needed for imaging-nanonis python scripts


## Prerequisites - Python Installation

### using Python Virtual Environment

It is possible to configure a virtual environment for running python scripts for this plugin.
How to configure environment
```bash
# Create venv
python3 -m venv ~/my_venv
# Activation 
source ~/my_venv/bin/activate
# Installation of packages
pip3 install -r scripts/python_requirements.txt
```

Configuring imaging plugin to use virtual environment:
1. Modify `python3-path` property of `imaging` core plugin and set it to point to your python virtual environment (either by setting environment property `eln-lims-imaging-core.as.services.imaging.python3-path` or you can find `plugin.properties` here: `<CORE_PLUGINS_FOLDER>/eln-lims-imaging-core/1/as/services/imaging/plugin.properties`)
   ```properties
   python3-path = ~/my_venv/bin/python
   ```
2. Start Openbis

### Removal of virtual environment

To deactivate virtual environment in the terminal type in:
```bash
deactivate
```

Afterwards, virtual environment can be deleted by simply removing `my_venv` directory

### Python modules configuration
This plugin has been verified to work with python 3.10.12 and modules specified in [python_requirements.txt](scripts/python_requirements.txt)

This file can be used to install packages via `pip` tool, i.e:
```bash
pip3 install -r scripts/python_requirements.txt
```

## Step-by-Step Installation
- Python 3.10 Virtual Environment ready
- Imaging core-plugin installed in Openbis

### Plugin Configuration

1. Include imaging-nanonis core plugin into core-plugins of your installation
2. (Optional) set python3-path property 

### Data import

In `imaging-nanonis/1/scripts/` you can find `import_data.sh` script. It will upload test data to a running Openbis instance.

It accepts 2 parameters:
- Openbis url (default: http://localhost:8888/openbis)
- Path to folder with nanonis data (default: ../nanonis_example/data)


## Uninstall 
1. remove `python3-path` from `<CORE_PLUGINS_FOLDER>/eln-lims-imaging-core/1/dss/services/imaging/plugin.properties`
2. run `uninstall.sh` script from `scripts` directory
3. remove imaging-nanonis from enabled core-plugins
