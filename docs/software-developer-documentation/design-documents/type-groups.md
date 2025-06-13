# Type Groups

## Description

Type Groups is a functionality that TBD

## Implementation proposal

### DB changes

New tables additional tables:
- TypeGroups
- SampleTypeTypeGroups

#### TypeGroups table

| column                 | DB type        | description                             |
|------------------------|----------------|-----------------------------------------|
| name                   | CODE (unique)  | name of the type group (unique)         |
| registration_timestamp | TIME_STAMP_DFL | time of the type group registration     |
| pres_id_registerer     | TECH_ID        | id of person registering it             |
| meta_data              | JSONB          | meta data for the particular type group |


#### SampleTypeTypeGroups table

| column  | DB type | description          |
|---------|---------|----------------------|
| SATY_ID | TECH_ID | sample type id       |
| TG_ID   | CODE    | type group id (name) |

### API changes




### AS changes





### DSS/AFS changes
N/A




### UI changes

#### ELN UI

#### ADMIN UI




### PyBIS changes

