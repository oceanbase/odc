CREATE TABLE rollback_tab1(c1 INT PRIMARY KEY, c2 INT);

CREATE TABLE rollback_tab2(c1 INT PRIMARY KEY, c2 INT) PARTITION BY KEY(c1) PARTITIONS 4;

CREATE TABLE rollback_tab3(id INT PRIMARY KEY, age INT default 20 , name VARCHAR(50) NOT NULL , birthday DATE NOT NULL);

CREATE TABLE rollback_tab4(c1 INT PRIMARY KEY, c2 INT);

CREATE TABLE `rollback_pri` (
  `id1` int(11) NOT NULL,
  `id2` int(11) NOT NULL,
  `uq1` int(11) NOT NULL,
  `uq2` int(11) NOT NULL,
  `nor1` int(11) NOT NULL,
  `nor2` int(11) NOT NULL,
  `name` varchar(120) NOT NULL,
  UNIQUE KEY `uq_idx1`(`id1`, `id2`, `name`) BLOCK_SIZE 16384 GLOBAL,
  UNIQUE KEY `uq_idx2` (`uq1`, `uq2`) BLOCK_SIZE 16384 GLOBAL,
  KEY `nor_rollback` (`nor1`, `nor2`) BLOCK_SIZE 16384 GLOBAL
);

CREATE TABLE rollback_unsupportedType (
    id INT AUTO_INCREMENT PRIMARY KEY,
    my_blob longblob
);

CREATE TABLE rollback_noPriOrUqKey(c1 INT, c2 INT);

CREATE TABLE rollback_use_default_value(c1 INT PRIMARY KEY, c2 INT DEFAULT NULL, c3 INT DEFAULT 0);

insert into rollback_tab1 values (1, 1);
insert into rollback_tab1 values (2, 2);
insert into rollback_tab1 values (3, 3);
insert into rollback_tab1 values (4, 4);
insert into rollback_tab1 values (5, 5);

insert into rollback_tab2 values (6, 6);
insert into rollback_tab2 values (7, 7);
insert into rollback_tab2 values (1, 1);
insert into rollback_tab2 values (2, 2);
insert into rollback_tab2 values (3, 3);

insert into rollback_tab3 values (1, 23, "aa", '2000-01-09');
insert into rollback_tab3 values (2, 24, "bb", '1999-09-09');
insert into rollback_tab3 values (3, 10, "cc", '2013-06-25');
insert into rollback_tab3 values (4, 30, "dd", '1993-03-15');

insert into rollback_tab4 values (6, 6);
insert into rollback_tab4 values (7, 7);
insert into rollback_tab4 values (1, 1);
insert into rollback_tab4 values (2, 2);
insert into rollback_tab4 values (3, 3);

insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(1,11,1,1,1,1,'aa');
insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(2,21,2,2,2,2,'bb');
insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(3,31,3,3,3,3,'cc');
insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(4,41,4,4,4,4,'dd');
insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(5,51,5,5,5,5,'ee');
insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(6,61,6,6,6,6,'ff');
insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(7,71,7,7,7,7,'gg');
insert into rollback_pri(`id1`,`id2`,`uq1`,`uq2`,`nor1`,`nor2`,`name`) values(8,81,8,8,8,8,'hh');

insert into rollback_noPriOrUqKey values (1, 1);

insert into rollback_unsupportedType values (1, "aa"), (2, "bb");

insert into rollback_use_default_value values (1, default, default);
insert into rollback_use_default_value values (2, 2, 3);









