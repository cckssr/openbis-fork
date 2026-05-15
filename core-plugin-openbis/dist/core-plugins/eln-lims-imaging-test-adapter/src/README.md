# IMAGING-TEST core plugin
This core-plugin was created to showcase a basic imaging technology. It contains a set of input
JSON files with pre-rendered images. Update and export flow consists of a Python script that generates an image
with randomly selected pixels. 


## Prerequisites
- Python >= 3.10 with [`numpy`, `pillow`] modules installed
- `imaging` core-plugin installed in Openbis

## Configuration

1. Include imaging-test core plugin into core-plugins of your installation
2. Configure python3 path for AS  as service property `imaging.as.services.imaging.python3-path`
3. Start Openbis

## Data import



In `imaging-test/1/imaging_test_importer/` you can find a python script (`importer.py`) that uploads test data into the system.

## Prerequisites

- Python >= 3.10 with [`pybis`] module installed