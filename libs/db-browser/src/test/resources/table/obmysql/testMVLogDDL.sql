CREATE TABLE if not exists test_mvlog_parallel (
                                          col1 INT PRIMARY KEY,
                                          col2 INT,
                                          col3 INT,
                                          col4 INT
);

CREATE TABLE if not exists test_mvlog_enable_auto_purge (
                                          col1 INT PRIMARY KEY,
                                          col2 INT,
                                          col3 INT,
                                          col4 INT
);

CREATE TABLE if not exists test_mvlog_disable_auto_purge (
                                          col1 INT PRIMARY KEY,
                                          col2 INT,
                                          col3 INT,
                                          col4 INT
);

CREATE MATERIALIZED VIEW LOG ON test_mvlog_parallel
  PARALLEL 5
  WITH (col2 ,col3 ,col4) INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW LOG ON test_mvlog_enable_auto_purge
  WITH (col2 ,col3 ,col4) INCLUDING NEW VALUES
  PURGE START WITH sysdate() NEXT sysdate() + INTERVAL 1 DAY;

CREATE MATERIALIZED VIEW LOG ON test_mvlog_disable_auto_purge
  WITH (col2 ,col3 ,col4) INCLUDING NEW VALUES;