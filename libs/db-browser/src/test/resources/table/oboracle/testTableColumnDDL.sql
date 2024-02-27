create table TEST_DATA_TYPE(
	col1 int,
	col2 number(22),
	col3 char(10),
	col4 varchar2(10),
	col5 blob,
	col6 clob,
	col7 date,
	col8 timestamp,
	col9 timestamp with time zone,
	col10 timestamp with local time zone,
	col11 raw(100),
	col12 interval year to month,
	col13 interval day to second,
  col14 number
);

create table TEST_OTHER_THAN_DATA_TYPE(
	col1 int NOT NULL,
	col2 int,
	col3 GENERATED ALWAYS AS (col1 + col2) VIRTUAL
);
comment on column TEST_OTHER_THAN_DATA_TYPE.col1 is 'col1_comments';

CREATE TABLE TEST_DEFAULT_NULL (
  COL1 NUMBER(*,0) DEFAULT NULL,
  COL2 NUMBER(22) DEFAULT null,
  COL3 CHAR(10) DEFAULT (null),
  COL4 VARCHAR2(10) DEFAULT 'null'
);