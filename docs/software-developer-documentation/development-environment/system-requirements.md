# System Software Requirements

Mandatory:

- JDK 21 (JDK is necessary for the development, for running the system JRE 21 headless is sufficient)
- Postgres 18

Optional:

- In order to use V3 API two-phase commit please enable Postgres prepared transactions functionality. This can be done by adding to postgresql.conf file "max_prepared_transactions" setting. For instance:

    ```
    max_prepared_transactions = 10
    ```

    More information: https://www.postgresql.org/docs/15/sql-prepare-transaction.html