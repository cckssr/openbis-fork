openBIS Drive
==========================

openBIS Drive is an application that allows you to syncronize your files and folders from a local storage to the openBIS storage.

You can download openBIS Drive for Mac, Windows and Linux here:


For installation, you can follow instructions provided here: [openBIS Drive installation](../advance-features/openbis-drive.md#installation)

When you open openBIS Drive, you can create a new sync jon by clicking on 


- **Title**. you can enter a name for the syncronization job. If you do not entre anything, this field will be automatically filled in with the name of the openBIS entity selected for syncing
- **openBIS entity ID**. this is the openBIS entity (Collection or Object) to which you want to sync data. If you start typing the name or code of the entity a list of options from which you can choose will appear.
- **openBIS server URL**. This is the URL of the openBIS server you want to sync to. E.g https://openbis-labX.com
- **openBIS server directory**. By default with will be the home directory on the entity you wnat to sync to 

![](img/7.0-entity-afs-home-folder.png)

If you want to sync to a specific folder inside the home folder, you can provide the path here.

![](img/7.0-entity-afs-home-subfolder.png)

- **Local directory**. This is the directory on you local storage you want to sync to an openBIS entity.
- **Personal accees token**. PATs can now only be created via the [admin UI](../../user-documentation/general-admin-users/admins-documentation/pat.md) or [pyBIS](../../software-developer-documentation/apis/python-v3-api.md#personal-access-token-pat). This is used to connect to openBIS.