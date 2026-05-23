# IMAGING-NANONIS data importer

`nanonis_importer.py` is a python3 script that allows for upload of files in nanonis format (`.SXM`, `.DAT`).

## Requirements

- Python 3.10 or newer
- libraries specified in `../scripts/python_requirements.txt`


### using Python Virtual Environment
If you don't want to pollute your python environment with additional libraries, we recommend using Python Virtual Environment.

It is possible to configure a virtual environment for running python scripts for this plugin.
How to configure environment
```bash
# Create venv
python3 -m venv ~/my_venv
# Activation 
source ~/my_venv/bin/activate
# Installation of packages
pip3 install -r ../scripts/python_requirements.txt
```

#### Removal of virtual environment

To deactivate virtual environment in the terminal type in:
```bash
deactivate
```

Afterwards, virtual environment can be deleted by simply removing `my_venv` directory


## Usage

python3 nanonis_importer.py <OPENBIS_URL> <PATH_TO_DATA_FOLDER>

In `imaging-nanonis/1/scripts/` you can find `import_data.sh` script. It will upload test data to a running Openbis instance.

It accepts 2 parameters:
- Openbis url (default: http://localhost:8888/openbis)
- Path to folder with nanonis data (default: ../nanonis_example/data)

