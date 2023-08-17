create table if not exists test_data_type(
	col1 int,
	col2 numeric(10,2),
	col3 decimal(10,2),
	col4 bit(8),
	col5 tinyint,
	col6 smallint,
	col7 mediumint,
	col8 bigint,
	col9 float(10,2),
	col10 double(10,2),
	col11 varchar(10),
	col12 char(10),
	col13 text,
	col14 tinytext,
	col15 mediumtext,
	col16 longtext,
	col17 blob,
	col18 tinyblob,
	col19 longblob,
	col20 mediumblob,
	col21 binary(16),
	col22 varbinary(16),
	col23 timestamp,
	col24 time,
	col25 date,
	col26 datetime,
	col27 year
);

create table if not exists test_other_than_data_type(
  col1 int PRIMARY KEY NOT NULL AUTO_INCREMENT comment 'col1_comments',
  col2 varchar(10) DEFAULT NULL
);