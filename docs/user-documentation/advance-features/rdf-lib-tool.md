RDF lib tool
===========================
https://openbis.readthedocs.io/en/latest/user-documentation/advance-features/rdf-lib-tool.html

`rdf-lib-tool` is a Java library that converts input RDF files into XLSX files that are easily
ingested by openBIS.

This tool supports various input and output formats and includes options for specifying credentials and other configurations.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Options](#options)
- [Examples](#examples)


## Installation

### Requirements

1. Java 17

To use `rdf-lib-tool`, you need to have Java 17 installed on your system. 

### Build the JAR

1. Download and set up the openBIS project (follow [building openBIS](#https://openbis.readthedocs.io/en/latest/software-developer-documentation/development-environment/installation-and-configuration-guide.html#building-openbis) guide)
2. Run the gradle task `buildRdfTool`
3. The JAR will be located under `lib-rdf/build/libs/lib-rdf-tool.jar`


## Usage

For the basic usage, this [turtle schema example](./files/schema.ttl) can be downloaded.

Open a terminal in the directory where the `rdf-lib-tool.jar` is located or simply use the path to
it instead `path/to/rdf-lib-tool.jar`.

To run the rdf-lib-tool, use the following command:

```bash
java -jar rdf-lib-tool.jar [options]
```

### Options
```bash
(mandatory) -i, --input-format <file>:  specifies the format of the input file (currently supports TTL).
(mandatory) -o, --output-format <file>: specifies the format of the output (currently supports XLSX, ZIP, OPENBIS, OPENBIS-DEV).
(mandatory) -f, inputpaths <file>: Path to input files, has to be at least one. All files have to be of the same format.
(mandatory) -r, resultfile <file>: Path to the resulting excel file. At the moment, this is always written out for troubleshooting purposes.
-pid, --project: specifies the openBIS project identifier. Must be of the format '/{space}/{project}' e.g. '/DEFAULT/DEFAULT'
-u, --username <username>: specifies the username for authentication (needed for OPENBIS and OPENBIS-DEV output format options).
-p, --password <password>: specifies the password for authentication (needed for OPENBIS and OPENBIS-DEV output format options).
-v, --verbose: displays detailed information on the process.
-h, --help: displays the help message.
-a, --additionalfiles: specifies additional file paths for resolving ontologies and terminology.  
-dangling, --remove-dangling-references: removes references that can't be resolved. Use with caution. 
-singlevalue, --enforce-single-values: only uses the first value encountered when converting single-valued properties. This makes no guaranatees about the value chosen, the other values are still logged. 
```

Other mandatory parameters must be provided based on what output format has been chosen:

1.**XLSX**
requires `-i <input format> -o <output format> <path to input file> <path to output file>`

2.**ZIP**
   requires `-i <input format> -o <output format> <path to input file> <path to output file>`, this
   can handle longer string values

3.**OPENBIS**
requires `-i <input format> -o <output format> <path to input file> -u <yourUsername> -p <AS openBIS URL>`

4.**OPENBIS-DEV**
requires `-i <input format> -o <output format> <path to input file> -u <yourUsername> -p <AS openBIS URL> <DSS openBIS URL>`

## Examples

### Help Message
Display the help message to see all available options:

```bash
java -jar rdf-lib-tool.jar -h
```

### Basic Usage
Convert an RDF file in TTL format to an XLSX file:

```bash
java -jar lib-rdf-tool.jar -i TTL -o XLSX -f path/to/schema.ttl -r /path/to/result.xlsx 
```

Convert convert multiple files:

```bash
java -jar lib-rdf-tool.jar -i TTL -o XLSX -f path/to/schema.ttl -f path/to/data.ttl -r /path/to/result.xlsx 
```

### Connect to openBIS

Import a schema directly from TTL into an openBIS instance.:
```bash
java -jar lib-rdf-tool.jar -i TTL -o OPENBIS -f path/to/schema.ttl -r /path/to/result.xlsx -u yourUsername -p http://localhost:8888
```

For development environment add the DSS URL:
```bash
java -jar lib-rdf-tool.jar -i TTL -o OPENBIS-DEV -f path/to/schema.ttl -r /path/to/result.xlsx -u yourUsername -p http://localhost:8888/openbis/openbis http://localhost:8889/datastore_server
```

In both cases a username and a password to login into the openBIS instance are required. 

The password will be inserted in a safe prompt.
