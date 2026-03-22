-- SmartCourier Oracle DB Initialization Script
-- Run automatically when Oracle container starts
-- Single shared schema: capgdb / capgdb

CREATE USER capgdb IDENTIFIED BY capgdb
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;

GRANT CONNECT, RESOURCE, CREATE SESSION TO capgdb;
GRANT CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO capgdb;
GRANT UNLIMITED TABLESPACE TO capgdb;

COMMIT;
