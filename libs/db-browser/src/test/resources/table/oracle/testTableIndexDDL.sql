-- test index type
create table TEST_INDEX_TYPE(
  col1 int,
  col2 int
);
CREATE UNIQUE INDEX test_normal_idx on TEST_INDEX_TYPE(col1);
CREATE UNIQUE INDEX test_btree_idx using btree on TEST_INDEX_TYPE(col2);

-- test index range
create table TEST_INDEX_RANGE(
  col1 int,
  col2 int
);
CREATE INDEX test_global_idx on TEST_INDEX_RANGE(col1) GLOBAL;
CREATE INDEX test_local_idx on TEST_INDEX_RANGE(col2) LOCAL;