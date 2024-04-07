create database db_00;
create database db_01;
create database db_02;
create database db_03;

-- below are logical tables

-- db_[00-03].tb_a_[00-03]
create table if not exists db_00.tb_a_00(
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
	col27 year) comment 'tb_a_00';
create table if not exists db_01.tb_a_01(
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
	col27 year) comment 'tb_a_01';
create table if not exists db_02.tb_a_02(
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
	col27 year) comment 'tb_a_02';
create table if not exists db_03.tb_a_03(
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
	col27 year) comment 'tb_a_03';

-- db_00.tb_i_[00-01]
create table if not exists db_00.tb_i_00(col1 int comment 'comment1');
create table if not exists db_00.tb_i_01(col1 int comment 'comment2');

-- db_00.tb_j_[00-01]
create table if not exists db_00.tb_j_00(col1 int, col2 int, index tb_j_00_idx_1(col1), index tb_j_00_idx_2(col2));
create table if not exists db_00.tb_j_01(col1 int, col2 int, index tb_j_01_idx_1(col2), index tb_j_00_idx_1(col1));


-- below are not logical tables

-- not a logical table
create table if not exists db_00.tb_b_00(col1 int);
create table if not exists  db_00.tb_b_01(col2 int);

-- not a logical table
create table if not exists db_00.tb_c_00(col1 int);
create table if not exists db_00.tb_c_01(col1 varchar(32));

-- not a logical table
create table if not exists db_00.tb_d_00(col1 int, primary key(col1));
create table if not exists db_00.tb_d_01(col1 int);

-- not a logical table
create table if not exists db_00.tb_e_00(col1 int, index idx_col1(col1));
create table if not exists db_00.tb_e_01(col1 int);

-- not a logical table
create table if not exists db_00.tb_f_00(col1 int, unique key tb_g_00_idx_col1(col1));
create table if not exists db_00.tb_f_01(col1 int, index tb_g_01_idx_col1(col1));

-- not a logical table
create table if not exists db_00.tb_g_00(col1 int, col2 int, index tb_g_00_idx_col1(col1, col2));
create table if not exists db_00.tb_g_01(col1 int, col2 int, index tb_g_01_idx_col1(col2, col1));

-- not a logical table
create table if not exists db_00.tb_h_00(col1 int not null);
create table if not exists db_00.tb_h_01(col1 int);

-- not a logical table
create table if not exists db_00.tb_k_00(col1 int, col2 int);

-- not a logical table
create table if not exists db_00.tb_l_00(col1 int) default charset = 'utf8mb4';
create table if not exists db_00.tb_l_01(col1 int) default charset = 'gbk';

