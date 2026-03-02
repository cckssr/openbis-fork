System Requirements
===================

## Operating System

We recommend to set up openBIS on a Linux operating system. We provide support for installing and operating openBIS on supported [Ubuntu Server LTS releases ](https://ubuntu.com/server).
- Operating System: Linux / MacOS X

## Third-Party Packages

The following software packages are required:

- The binaries `bash`, `awk`, `sed` and `unzip` need to be installed and in the `PATH` of the openBIS user.
- Java Runtime Environment: OpenJDK 21
- PostgreSQL 18

## Microservices memory settings

openBIS operates as several separate services. Because each service reserves its own memory, the total available memory must be divided among them accordingly.

- Application Server (AS)
- RO-Crate Server (ROS)
- Datastore Server (DSS)
- Atomic File System Server (AFS)

All services share the same PostgreSQL database service.

### CPU and Memory Configuration

Please bear in mind that recommended settings may vary from the suggested ones depending on how openBIS is used.

Take these as initial suggestions.

| Concurrent Users              | Small | Medium | Big |
|-------------------------------|-------|--------|-----|
| up to 5 concurrent users      | x     | x      | x   |
| up to 20 concurrent users     |       | x      | x   |
| more than 20 concurrent users |       |        | x   |


### Small instance

| Item                           | Recommended Setting    |
|--------------------------------|------------------------|
| CPUs                           | 2 modern x86 CPU cores |
| Total RAM                      | 6 GB                   |
| Memory allocated to OS         | 1.5 GB                 |
| Memory allocated to PostgreSQL | 1 GB                   |
| Memory allocated to AS         | 2 GB                   |
| Memory allocated to ROS        | 0.5 GB                 |
| Memory allocated to DSS        | 0.5 GB                 |
| Memory allocated to AFS        | 0.5 GB                 |

### Medium instance

| Item                           | Recommended Setting    |
|--------------------------------|------------------------|
| CPUs                           | 4 modern x86 CPU cores |
| Total RAM                      | 10 GB                  |
| Memory allocated to OS         | 1.5 GB                 |
| Memory allocated to PostgreSQL | 1.5 GB                 |
| Memory allocated to AS         | 4 GB                   |
| Memory allocated to ROS        | 1 GB                   |
| Memory allocated to DSS        | 1 GB                   |
| Memory allocated to AFS        | 1 GB                   |

### Big instance

| Item                           | Recommended Setting    |
|--------------------------------|------------------------|
| CPUs                           | 8 modern x86 CPU cores |
| Total RAM                      | 16 GB                  |
| Memory allocated to OS         | 2 GB                   |
| Memory allocated to PostgreSQL | 2 GB                   |
| Memory allocated to AS         | 8 GB                   |
| Memory allocated to ROS        | 1 GB                   |
| Memory allocated to DSS        | 1 GB                   |
| Memory allocated to AFS        | 2 GB                   |

### Postgres Memory Settings

Memory-related settings of your PostgreSQL server can be obtained from https://pgtune.leopard.in.ua/.

After clicking on "Generate", you get the matching settings of the postgresql.conf displayed.

Example for 2 GB dedicated to PostgreSQL
```properties
# DB Version: 18
# OS Type: linux
# DB Type: web
# Total Memory (RAM): 2 GB
# CPUs num: 4
# Connections num: 50
# Data Storage: ssd

max_connections = 50
shared_buffers = 512MB
effective_cache_size = 1536MB
maintenance_work_mem = 128MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 9709kB
huge_pages = off
min_wal_size = 1GB
max_wal_size = 4GB
max_worker_processes = 4
max_parallel_workers_per_gather = 2
max_parallel_workers = 4
max_parallel_maintenance_workers = 2
```

### Tuning Of Hardware Settings In Case Of Issues

| Symptom                                  | Recommended Action                                                                                                                                                                       |
|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Long query execution times               | Increase CPU number and/or AS & Postgres memory settings, reconfigure Postgres and openBIS memory settings following the recommended settings provided by https://pgtune.leopard.in.ua/. |
| AS log shows out of memory errors        | Increase AS Memory.                                                                                                                                                                      |
| Service X log shows out of memory errors | Increase service X Memory.                                                                                                                                                               |


## Additional Requirements

An SMTP server needs to be accessible if you want openBIS to send out notifications via mail. We recommend to use the a local mail transfer agent such as Postfix configured for message sending.