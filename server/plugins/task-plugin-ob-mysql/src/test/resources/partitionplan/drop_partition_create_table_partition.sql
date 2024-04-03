CREATE TABLE `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLKeepLatestPartitionGeneratorTest.RANGE_COLUMNS_DATE_TABLE_NAME}` (
  `c1` int(11) DEFAULT NULL,
  `c2` varchar(64) DEFAULT NULL,
  `c3` date DEFAULT NULL
) partition by range columns(c2, c3)
(partition p1 values less than ('aaa', '2020-12-31'),
partition p2 values less than ('bbb', '2021-12-31'),
partition p3 values less than ('ccc', '2022-12-31'));