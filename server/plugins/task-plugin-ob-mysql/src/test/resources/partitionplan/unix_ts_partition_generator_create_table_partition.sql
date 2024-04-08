create table `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLHistoricalPartitionPlanCreateGeneratorTest.UNIXTIMESTAMP_RANGE_PARTI_TBL}` (
    id int(11),
    parti_key timestamp
) partition by range (UNIX_TIMESTAMP(parti_key)) (
    partition p20230901 values less than (1693497600),
    partition p20231001 values less than (1696089600),
    partition p20231101 values less than (1698768000),
    partition p20231201 values less than (1701360000),
    partition p20240101 values less than (1704038400),
    partition p20240201 values less than (1706716800),
    partition p20240301 values less than (1709222400),
    partition p20240401 values less than (1711900800),
    partition p20240501 values less than (1714492800)
);