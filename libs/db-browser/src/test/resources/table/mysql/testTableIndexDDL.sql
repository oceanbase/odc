create table if not exists test_index_type(
  col1 int,
  col2 int,
  col3 int,
  col4 text,
  point geometry NOT NULL
);
CREATE UNIQUE INDEX test_hash using HASH on test_index_type(col1);
CREATE UNIQUE INDEX test_btree using BTREE on test_index_type(col2);
CREATE INDEX test_normal using BTREE on test_index_type(col3);
CREATE FULLTEXT INDEX test_fulltext on test_index_type(col4);
CREATE SPATIAL INDEX test_spatial on test_index_type(point);