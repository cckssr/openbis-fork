# App RO-Crate

This converts an openBIS export excel file to an RO-Crate mannifest.

The tool is used in the following way:

Write.java with arguments `-i [in-file.xlsx] -o [out-dir]`. The RO-Crate is written as a directory.
All contents are in the `ro-crate-metadata.json`. 