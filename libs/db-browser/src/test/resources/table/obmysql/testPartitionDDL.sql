CREATE TABLE if not exists `part_hash` (
`c1` int NOT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci  PARTITION BY HASH(`c1`)
PARTITIONS 5;

CREATE TABLE if not exists`part_list` (
  `category` int(11) DEFAULT NULL
) partition by list(category)
(partition p1 values in (1,10),
partition p0 values in (3,5),
partition p2 values in (4,9),
partition p3 values in (2),
partition p4 values in (6));

CREATE TABLE if not exists `part_range` (
  `id` int(11) NOT NULL,
  `ename` varchar(30) DEFAULT NULL,
  `hired` date NOT NULL DEFAULT '1970-01-01',
  `separated` date NOT NULL DEFAULT '9999-12-31',
  `job` varchar(30) NOT NULL,
  `store_id` int(11) NOT NULL
) partition by range(store_id)
(partition p0 values less than (10),
partition P1 values less than (20),
partition p2 values less than (30));
