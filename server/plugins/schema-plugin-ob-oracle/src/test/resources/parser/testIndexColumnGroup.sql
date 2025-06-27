create table INDEX_COLUMN_GROUP_TABLE(c1 int, c2 int, c3 int) with column group(each column);
create index IDX_ALL_AND_EACH on INDEX_COLUMN_GROUP_TABLE(c1) with column group(all columns, each column);
create index IDX_ALL on INDEX_COLUMN_GROUP_TABLE(c2) with column group(all columns);
create index IDX_EACH on INDEX_COLUMN_GROUP_TABLE(c3) with column group(each column);