# Restore

The following proposal of restore procedure is simplest possible scenario of restoring openBIS database dump, configuration files and data files 
Log files and password files restore is ommited in this example. Restore can be proceeded from compressed archive files or raw data of Docker volumes.

It is recommended the containers are recreated as fresh initial start. Database container should be running and healty.
Configuration restoration is provided however it is recommended to use independent configuration management system. In such a case only passwords file is necessary to restore.

Docker container openbis-db should be left running while openbis-app is stopped.
```
$ docker stop openbis-app;
```

Uncompress SQL file and restore it to openbis-db container.
```
$ gunzip -ck openbis-db.sql.gz | docker exec -i openbis-db psql -U postgres;
```

Inspect Docker volumes to find according mountpoints.
```
$ docker volume inspect openbis-app-etc;
$ docker volume inspect openbis-app-data;
```

Delete all files from Docker volume, uncompress and restore TAR archive to the volume mountpoint.
```
$ find /var/lib/docker/volumes/openbis-app-etc/_data -type f -delete;
$ gunzip -ck openbis-app-etc.tar.gz | tar --strip-components=1 -C /var/lib/docker/volumes/openbis-app-etc/_data -xf -;
```

Delete all files from Docker volume, uncompress and restore TAR archive to the volume mountpoint.
```
$ find /var/lib/docker/volumes/openbis-app-data/_data -type f -delete;
$ gunzip -ck openbis-app-data.tar.gz | tar -C /var/lib/docker/volumes/openbis-app-data/_data -xf -;
```
