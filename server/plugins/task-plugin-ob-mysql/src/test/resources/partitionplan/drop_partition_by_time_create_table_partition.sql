create table `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLKeepLatestPartitionByTimeGeneratorTest.DATETIME_RANGE_PARTI_TBL}` (
    id int(11),
    parti_key datetime
) partition by range columns(parti_key) (
    partition p20230901 values less than ('2023-09-01 00:00:00'),
    partition p20231001 values less than ('2023-10-01 00:00:00'),
    partition p20231101 values less than ('2023-11-01 00:00:00'),
    partition p20231201 values less than ('2023-12-01 00:00:00'),
    partition p20240101 values less than ('2024-01-01 00:00:00'),
    partition p20240201 values less than ('2024-02-01 00:00:00'),
    partition p20240301 values less than ('2024-03-01 00:00:00')
);

create table `${const:com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLKeepLatestPartitionByTimeGeneratorTest.UNIXTIMESTAMP_RANGE_PARTI_TBL}` (
    id int(11),
    parti_key timestamp
) partition by range (UNIX_TIMESTAMP(parti_key)) (
    partition p20230901 values less than (1693497600),
    partition p20231001 values less than (1696089600),
    partition p20231101 values less than (1698768000),
    partition p20231201 values less than (1701360000),
    partition p20240101 values less than (1704038400),
    partition p20230201 values less than (1706716800)
);