create database if not exists db_00;
create database if not exists db_01;
create database if not exists db_02;
create database if not exists db_03;

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

-- db_[00-03].tb_n
create table if not exists db_00.tb_n(col1 int, col2 int, primary key(col1), index idx_col2(col2));
create table if not exists db_01.tb_n(col1 int, col2 int, primary key(col1), index idx_col2(col2));
create table if not exists db_02.tb_n(col1 int, col2 int, primary key(col1), index idx_col2(col2));
create table if not exists db_03.tb_n(col1 int, col2 int, primary key(col1), index idx_col2(col2));

-- db_[00-03].tb_o_[[0-10:10]]
create table if not exists db_00.tb_o_0(col2 int, col1 int not null, index tb_o_0_idx_col2(col2));
create table if not exists db_00.tb_o_10(col1 int not null, col2 int, index tb_o_10_idx_col2(col2));
create table if not exists db_01.tb_o_0(col2 int, col1 int not null,  index tb_o_0_idx_col2(col2));
create table if not exists db_01.tb_o_10(col1 int not null, col2 int, index tb_o_10_idx_col2(col2));
create table if not exists db_02.tb_o_0(col1 int not null, col2 int, index tb_o_0_idx_col2(col2));
create table if not exists db_02.tb_o_10(col2 int, col1 int not null, index tb_o_10_idx_col2(col2));
create table if not exists db_03.tb_o_0(col1 int not null, col2 int, index tb_o_0_idx_col2(col2));
create table if not exists db_03.tb_o_10(col1 int not null, col2 int, index tb_o_10_idx_col2(col2));

-- db_[00,01,03].tb_p
create table if not exists db_00.tb_p(col1 int, col2 int, index idx_col2(col2));
create table if not exists db_01.tb_p(col1 int, col2 int, index idx_col2(col2));
create table if not exists db_03.tb_p(col1 int, col2 int, index idx_col2(col2));

-- db_[00-03].tb_q_[00-03]_[000-001]
create table if not exists db_00.tb_q_00_000(col1 int, col2 number);
create table if not exists db_00.tb_q_00_001(col1 int, col2 number);
create table if not exists db_01.tb_q_01_000(col1 int, col2 number);
create table if not exists db_01.tb_q_01_001(col1 int, col2 number);
create table if not exists db_02.tb_q_02_000(col1 int, col2 number);
create table if not exists db_02.tb_q_02_001(col1 int, col2 number);
create table if not exists db_03.tb_q_03_000(col1 int, col2 number);
create table if not exists db_03.tb_q_03_001(col1 int, col2 number);

-- db_[00,01,03].tb_r_[0,1,3]
create table if not exists db_00.tb_r_0(col1 int, col2 int);
create table if not exists db_01.tb_r_1(col1 int, col2 int);
create table if not exists db_02.tb_r_2(col1 int, col2 int, col3 int);
create table if not exists db_03.tb_r_3(col1 int, col2 int);

-- db_00.tb_s_[0-5:5],db_01.tb_s_1,db_02.tb_s_2,db_03.tb_s_3
create table if not exists db_00.tb_s_0(col1 int, col2 int);
create table if not exists db_01.tb_s_1(col1 int, col2 int);
create table if not exists db_02.tb_s_2(col1 int, col2 int);
create table if not exists db_03.tb_s_3(col1 int, col2 int);
create table if not exists db_00.tb_s_5(col1 int, col2 int);

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

-- not a logical table
create table if not exists db_00.tb_m_00(col1 int default 1);
create table if not exists db_00.tb_m_01(col1 int default 2);