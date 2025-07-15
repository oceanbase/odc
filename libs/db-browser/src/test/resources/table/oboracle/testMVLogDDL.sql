CREATE TABLE TEST_MVLOG_PARALLEL (
                                                 COL1 INT PRIMARY KEY,
                                                 COL2 INT,
                                                 COL3 INT,
                                                 COL4 INT
);

CREATE TABLE TEST_MVLOG_ENABLE_AUTO_PURGE (
                                                          COL1 INT PRIMARY KEY,
                                                          COL2 INT,
                                                          COL3 INT,
                                                          COL4 INT
);

CREATE TABLE TEST_MVLOG_DISABLE_AUTO_PURGE (
                                                           COL1 INT PRIMARY KEY,
                                                           COL2 INT,
                                                           COL3 INT,
                                                           COL4 INT
);

CREATE MATERIALIZED VIEW LOG ON TEST_MVLOG_PARALLEL
  PARALLEL 5
  WITH (COL2 ,COL3 ,COL4)
  INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW LOG ON TEST_MVLOG_ENABLE_AUTO_PURGE
  WITH (COL2 ,COL3 ,COL4)
  INCLUDING NEW VALUES
  PURGE START WITH CURRENT_DATE
  NEXT CURRENT_DATE + INTERVAL '1' DAY;

CREATE MATERIALIZED VIEW LOG ON TEST_MVLOG_DISABLE_AUTO_PURGE
  WITH (COL2 ,COL3 ,COL4)
  INCLUDING NEW VALUES;