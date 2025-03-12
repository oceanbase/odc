create table if not exists test_mv_base
(col1 INT AUTO_INCREMENT PRIMARY KEY,col2 INT);

CREATE MATERIALIZED VIEW test_mv
    (col1,col2,PRIMARY KEY(col1))
    PARALLEL 5
    PARTITION BY HASH (col1)
    WITH COLUMN GROUP(all columns, each column)
    REFRESH COMPLETE
    ON DEMAND
    START WITH sysdate()
    NEXT sysdate() + INTERVAL 1 DAY
    ENABLE QUERY REWRITE
    DISABLE ON QUERY COMPUTATION AS
SELECT * FROM test_mv_base;