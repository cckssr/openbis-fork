# Backup

The following proposal of backup procedure is simplest possible scenario of openBIS database dump, configuration files, passwords file, log and data files 
Archives can be created from running containers or when containers are stopped from raw data of Docker volumes.


Archive all openBIS releated databases and compress them to one SQL file.
```
$ docker exec openbis-db pg_dumpall -U postgres -c --if-exists --exclude-database=postgres --exclude-database=template0 --exclude-database=template1 | gzip -c > openbis-db.sql.gz;
```


Archive all openBIS configuration files to one TAR file and compress it.
```
$ docker exec openbis-app tar --one-file-system -C /etc -cf - openbis | gzip -c > openbis-app-etc.tar.gz;
```


Archive passwords of file based accounts in sperate TAR file and compress it.
```
$ docker exec openbis-app tar --one-file-system -C /etc/openbis/as -cf - passwd | gzip -c > openbis-app-passwd.tar.gz;
```


Archive all openBIS log files to one TAR file and compress it.
```
$ docker exec openbis-app tar --one-file-system -C /var/log -cf - openbis | gzip -c > openbis-app-logs.tar.gz;
```


Archive all openBIS data files to on TAR file and compress it.
```
$ docker exec openbis-app tar --one-file-system -C /data -cf - openbis | gzip -c > openbis-app-data.tar.gz;
```


Alternatively, when containers are stopped, the archiving could be executed directly on each of related volumes.
Find mountpoints of volumes by executing the inspect of volume commands and archive them adequately.
```
$ docker volume inspect openbis-db-data;
$ docker volume inspect openbis-app-etc;
$ docker volume inspect openbis-app-logs;
$ docker volume inspect openbis-app-data;
```
