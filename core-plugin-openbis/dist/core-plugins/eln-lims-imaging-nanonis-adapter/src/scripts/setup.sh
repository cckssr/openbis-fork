#! /bin/bash

usage() {
    echo "Usage: $0 <path_to_directory>"
    exit 1
}

# Checks whether the number of arguments is smaller than one.
check_arguments() {
    if [ $# -lt 1 ]; then
        usage
    fi
}

check_arguments $@

# setup virtual environment
python3 -m venv $1/nanonis_venv

#source `dirname "$1"`/nanonis_venv/bin/activate

#pip3 install -r python_requirements.txt

$1/nanonis_venv/bin/pip3 list

echo `pwd .`/$1

echo `pwd "$1"`/$1/nanonis_venv/bin/python3
