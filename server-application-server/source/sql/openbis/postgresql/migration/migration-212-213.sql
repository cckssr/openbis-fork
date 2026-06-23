-- Migration from 212 to 213

-- delete predeployed plugins
DELETE FROM scripts WHERE plugin_type = 'PREDEPLOYED';

-- change 'script' column to not null
ALTER TABLE SCRIPTS DROP CONSTRAINT IF EXISTS SCRIPT_NN_CK;
ALTER TABLE SCRIPTS ALTER COLUMN SCRIPT SET NOT NULL;

-- change 'pluginType' domain not to accept 'PREDEPLOYED' value anymore
ALTER DOMAIN plugin_type DROP CONSTRAINT IF EXISTS plugin_type_check;
ALTER DOMAIN plugin_type ADD CONSTRAINT plugin_type_check CHECK (VALUE IN ('JYTHON'));