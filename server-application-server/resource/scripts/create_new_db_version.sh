#!/bin/bash
# Author: Tomasz Pylak
# Creates new database version (all sql files) which is identical to the previous version.
# Assumes that it is called from the directory where it is located.

SQL_DIR=../../source/sql/openbis
echo "Copy Source files $SQL_DIR"
source common.sh

copy_db_folder generic
copy_db_folder postgresql
copy_migration_file
print_finish_message

#Copy also the test
SQL_DIR=../../sourceTest/sql/openbis
echo "Copy Test files $SQL_DIR"
source common.sh
copy_db_folder postgresql
print_finish_message
