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

CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPointTest.REAL_RANGE_TABLE_NAME}` (
  `id` bigint(20) unsigned NOT NULL,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `datekey` int(11) NOT NULL,
  `c3` date DEFAULT NULL
) partition by range columns(datekey, c3)
(partition p20220829 values less than (20220729, '2022-07-29'),
partition p20220830 values less than (20220730, '2022-07-30'),
partition p20220831 values less than (20220731, '2022-07-31'));

CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPointTest.REAL_RANGE_COL_TABLE_NAME}` (
  `id` bigint(20) unsigned NOT NULL,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `datekey` int(11) NOT NULL,
  `c3` date DEFAULT NULL
) partition by range (datekey)
(partition p20220829 values less than (20220729),
partition p20220830 values less than (20220730),
partition p20220831 values less than (20220731));



