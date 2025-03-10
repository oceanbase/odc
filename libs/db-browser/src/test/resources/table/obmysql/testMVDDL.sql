create table if not exists test_mv_base (col1 INT AUTO_INCREMENT PRIMARY KEY);
CREATE MATERIALIZED VIEW test_mv
    REFRESH COMPLETE
        START WITH SYSDATE()
        NEXT SYSDATE() + INTERVAL '1' WEEK
    AS SELECT *
       FROM test_mv_base;