# System Software Requirements

Mandatory:

- OpenJDK 21 (JDK is necessary for the development, for running the system OpenJRE 21 headless is sufficient)
- Postgres 18 with prepared transactions functionality enabled

## Postgres required configuration

- OpenBIS V3 API two-phase commit needs to have enabled Postgres prepared transaction functionality. This can be done by adding to postgresql.conf file "max_prepared_transactions" setting. For instance:

    ```
    max_prepared_transactions = 10
    ```

    More information on prepared transactions: https://www.postgresql.org/docs/18/sql-prepare-transaction.html

### How to enable it (Postgres 18 - MacOS/Linux)

1. Run following sql script:
```SQL
ALTER SYSTEM SET max_prepared_transactions = 10;
```
2. Restart postgresql: `systemctl restart postgresql`


**OR**

1. Open your postgresql.conf file (most likely it is here: `/etc/postgresql/18/main/postgresql.conf`)
2. Add `max_prepared_transactions = 10` line and save the file
3. Restart postgresql: `systemctl restart postgresql`



Afterwards, you can verify if prepared transactions are enabled by running following SQL script:
```SQL
SHOW max_prepared_transactions;
```