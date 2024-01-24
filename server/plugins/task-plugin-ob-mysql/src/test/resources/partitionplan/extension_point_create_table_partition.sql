CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPointTest.RANGE_COLUMNS_TABLE_NAME}` (
  `c1` int(11) DEFAULT NULL,
  `c2` varchar(64) DEFAULT NULL,
  `c3` date DEFAULT NULL
) partition by range columns(c2, c3)
(partition p1 values less than ('aaa', '2020-12-31'),
partition p2 values less than ('bbb', '2021-12-31'),
partition p3 values less than ('ccc', '2022-12-31'));

CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPointTest.RANGE_TABLE_NAME}` (
  `c1` int(11) DEFAULT NULL,
  `c2` varchar(64) DEFAULT NULL,
  `c3` date DEFAULT NULL
) partition by range (year(`c3`))
(partition p1 values less than (2020),
partition p2 values less than (2021),
partition p3 values less than (2022));

CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPointTest.RANGE_MAX_VALUE_TABLE_NAME}` (
  `c1` int(11) DEFAULT NULL,
  `c2` varchar(64) DEFAULT NULL,
  `c3` date DEFAULT NULL
) partition by range columns(c2, c3)
(partition p1 values less than ('aaa', '2020-12-31'),
partition p2 values less than ('bbb', '2021-12-31'),
partition p3 values less than ('ccc', MAXVALUE));

CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPointTest.NO_PARTI_TABLE_NAME}` (
  `c1` int(11) DEFAULT NULL,
  `c2` varchar(64) DEFAULT NULL,
  `c3` date DEFAULT NULL
);

CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPointTest.LIST_PARTI_TABLE_NAME}` (
  `c1` int(11) DEFAULT NULL,
  `c2` varchar(64) DEFAULT NULL,
  `c3` date DEFAULT NULL
) partition by list columns(c2)
(partition p1 values in ('aaa', '2020-12-31'),
partition p2 values in ('bbb', '2021-12-31'));



