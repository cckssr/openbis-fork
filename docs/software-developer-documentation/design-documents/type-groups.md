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

SQL changes:
``` sql
CREATE TABLE TYPE_GROUPS (
    NAME CODE NOT NULL,
    PERS_ID_REGISTERER TECH_ID NOT NULL,
    REGISTRATION_TIMESTAMP TIME_STAMP_DFL NOT NULL,
    META_DATA JSONB
);

ALTER TABLE TYPE_GROUPS ADD CONSTRAINT TYPE_GROUPS_PK PRIMARY KEY(NAME);

ALTER TABLE TYPE_GROUPS ADD CONSTRAINT TG_PERS_FK FOREIGN KEY (PERS_ID_REGISTERER) REFERENCES PERSONS(ID) DEFERRABLE INITIALLY DEFERRED;
```


#### SampleTypeTypeGroups table

| column               | DB type | description                             |
|----------------------|---------|-----------------------------------------|
| SATY_ID              | TECH_ID | sample type id                          |
| TG_ID                | CODE    | type group id (name)                    |
| pres_id_registerer   | TECH_ID | id of person registering it             |
| meta_data            | JSONB   | meta data for the particular type group |

```sql

CREATE TABLE SAMPLE_TYPE_TYPE_GROUPS (
    SATY_ID TECH_ID NOT NULL,
    TG_ID CODE NOT NULL
    PERS_ID_REGISTERER TECH_ID NOT NULL, 
    REGISTRATION_TIMESTAMP TIME_STAMP_DFL NOT NULL
)

ALTER TABLE SAMPLE_TYPE_TYPE_GROUPS ADD CONSTRAINT SAMPLE_TYPE_TYPE_GROUPS_UK UNIQUE (SATY_ID, TG_ID);


ALTER TABLE SAMPLE_TYPE_TYPE_GROUPS ADD CONSTRAINT STTG_SATY_FK FOREIGN KEY (SATY_ID) REFERENCES SAMPLE_TYPES(ID) ON DELETE CASCADE;
ALTER TABLE SAMPLE_TYPE_TYPE_GROUPS ADD CONSTRAINT STTG_TG_FK FOREIGN KEY (TG_ID) REFERENCES TYPE_GROUPS(NAME) ON DELETE CASCADE;
ALTER TABLE SAMPLE_TYPE_TYPE_GROUPS ADD CONSTRAINT STTG_PERS_FK FOREIGN KEY (PERS_ID_REGISTERER) REFERENCES PERSONS(ID) DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX STTG_SATY_FK_I ON SAMPLE_TYPE_TYPE_GROUPS (SATY_ID);
CREATE INDEX STTG_TG_FK_I ON SAMPLE_TYPE_TYPE_GROUPS (TG_ID);
CREATE INDEX STTG_PERS_FK_I ON SAMPLE_TYPE_TYPE_GROUPS (PERS_ID_REGISTERER);

```

### API changes

Introduction of `typegroup` package to V3 API containing classes for:

- creation
- deletion (with/without trash?)
- get
- search

update/fetchoptions are not to be added

### AS changes
TypeGroupsPE, SampleTypeTypeGroups, SampleTypeTypeGroupsId




### DSS/AFS changes
N/A




### UI changes

#### ELN UI

#### ADMIN UI




### PyBIS changes

