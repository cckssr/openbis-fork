## ✅ Tests
Jira Task : https://ethsis.atlassian.net/browse/BIS-2087

There are three main test scripts in the repository. Each one tests a different upload scenario in OpenBIS:

1. **`test_obis_upload_kill_client.sh`**
    - Simulates a file upload where the client is killed mid-process.
    - Verifies that OpenBIS handles partial uploads gracefully.

2. **`test_obis_upload_parallel.sh`**
    - Runs multiple uploads in parallel.
    - Tests performance and consistency under concurrent uploads.

3. **`test_obis_upload_while_modifying.sh`**
    - Uploads files while they are being modified.
    - Ensures OpenBIS can detect changes and maintain data integrity.

---