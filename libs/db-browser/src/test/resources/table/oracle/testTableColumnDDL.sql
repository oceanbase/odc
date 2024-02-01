create table TEST_COL_DATA_TYPE(
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
	col13 interval day to second
)
/