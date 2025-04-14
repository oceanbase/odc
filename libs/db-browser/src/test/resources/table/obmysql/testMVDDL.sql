CREATE TABLE if not exists test_mv_base (
  col1 INT PRIMARY KEY,
  col2 INT,
  col3 INT,
  col4 INT
);

CREATE MATERIALIZED VIEW LOG ON test_mv_base
  WITH SEQUENCE (col2, col3,col4) INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW test_mv_all_syntax(
  PRIMARY KEY(prim))
    PARALLEL 5
    PARTITION BY HASH (prim)
    WITH COLUMN GROUP(all columns, each column)
    REFRESH COMPLETE
    ON DEMAND
    START WITH sysdate()
    NEXT sysdate() + INTERVAL 1 DAY
    DISABLE QUERY REWRITE
    DISABLE ON QUERY COMPUTATION AS
select `col1` AS `prim`,`col2` AS `col2`,`col3` AS `col3`,`col4` AS `col4` from `test_mv_base`;

CREATE MATERIALIZED VIEW `test_mv_computation`
  ENABLE ON QUERY COMPUTATION
  AS SELECT col1,count(*) FROM test_mv_base group by col1;

CREATE MATERIALIZED VIEW `test_mv_query_rewrite`
  ENABLE QUERY REWRITE
  AS SELECT * FROM test_mv_base;

CREATE MATERIALIZED VIEW `test_mv_each_column`
  WITH COLUMN GROUP(each column)
  AS SELECT * FROM test_mv_base;

CREATE MATERIALIZED VIEW `test_mv_complete`
  REFRESH COMPLETE
  AS SELECT * FROM test_mv_base;

CREATE MATERIALIZED VIEW `test_mv_fast`
  REFRESH FAST
  AS SELECT col1,count(*) FROM test_mv_base group by col1;

CREATE MATERIALIZED VIEW `test_mv_force`
  REFRESH FORCE
  AS SELECT * FROM test_mv_base;

CREATE MATERIALIZED VIEW `test_mv_never`
  NEVER REFRESH
  AS SELECT * FROM test_mv_base;

CREATE MATERIALIZED VIEW `test_mv_auto_refresh`
  REFRESH COMPLETE
  START WITH sysdate() NEXT sysdate() + interval 1 day
  AS SELECT * FROM test_mv_base;