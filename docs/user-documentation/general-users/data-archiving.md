# Data archiving

 

## Dataset archiving

 

openBIS supports archiving of datasets to Strongbox
([https://www.strongboxdata.com/](https://www.strongboxdata.com/)) as
described in [Datasets
Archiving](../../system-documentation/configuration/archive-datasets.md)

 

This needs to be set up and configured on *system level*.

 

To trigger archiving manually from the ELN, navigate to a dataset and
use the *Request or disallow archiving* button, as shown below.

 

![image info](img/201012-dataset-menu-bar.png)

 

Please note that the strongbox has a minimum size requirement of
**10GB**. If a single dataset is below this threshold it will be queued
for archiving and it will be archived only when additional datasets in
the same *Space/Project/Experiment* are selected for archiving and the
minimum size is reached. All datasets are bundled together and archived
together. This implies that if unarchiving is requested for one dataset
in a bundle, all other datasets will also be unarchived.

 

### Dataset archiving helper tool

 
If you wish to archive multiple datasets, you can use the **Archiving
Helper** tool under **Utilities** in the main menu. You can search for
datasets and select multiple ones to be archived, by clicking the
**Request Archiving** button on the top of the page.

 

![image info](img/201012-archiving-helper.png)

It is possible to search datasets by size, by selecting *Property* in
the **Field Type**, *Size (bytes)\[ATTR.SIZE\]*  in the **Field Name**
and the desired **Comparator Operator**, as shown below.

 

![image info](img/arciving-helper-search-by-size-1024x423.png)



## Dataset unarchiving

 Once the dataset is archived on tapes, the button on the dataset page
changes to **Unarchive**, as shown below. Datasets can be unarchived by
using this button.

 

![image info](img/201012-dataset-unarchive-button.png)

 


### Dataset unarchiving helper tool

To unarchive several datasets it is possible to use the **Unarchiving
Helper** tool, under **Utilities** in the main menu, as shown below. You
can search for datasets and select multiple ones to be unarchived, using
the **Unarchive** button on tope of the page.

 
![image info](img/201012-unarchiving-helper.png)


