-- MySQL create index syntax
-- CREATE [UNIQUE | FULLTEXT | SPATIAL] INDEX index_name
--    [index_type]
--    ON tbl_name (key_part,...)
--    [index_option]
--    [algorithm_option | lock_option] ...
--
-- key_part:
--     col_name [(length)] [ASC | DESC]
--
-- index_option: {
--     KEY_BLOCK_SIZE [=] value
--   | index_type
--   | WITH PARSER parser_name
--   | COMMENT 'string'
-- }
--
-- index_type:
--     USING {BTREE | HASH}
--
-- algorithm_option:
--     ALGORITHM [=] {DEFAULT | INPLACE | COPY}
--
-- lock_option:
--     LOCK [=] {DEFAULT | NONE | SHARED | EXCLUSIVE}


-- test index type
create table if not exists test_index_type(
  col1 int,
  col2 int
);
CREATE UNIQUE INDEX test_hash using HASH on test_index_type(col1);
CREATE UNIQUE INDEX test_btree using BTREE on test_index_type(col2);

-- test index range
create table if not exists test_index_range(
  col1 int,
  col2 int
);
CREATE INDEX test_global_idx on test_index_range(col1) GLOBAL;
CREATE INDEX test_local_idx on test_index_range(col2) LOCAL;

create table if not exists test_index_range2(
  col1 int,
  col2 int
);
CREATE INDEX test_global_idx2 on test_index_range2(col1) GLOBAL;
CREATE INDEX test_local_idx2 on test_index_range2(col2) LOCAL;