CREATE TABLE `${const:com.oceanbase.odc.service.partitionplan.PartitionPlanServiceV2Test.REAL_RANGE_TABLE_NAME}` (
  `id` bigint(20) unsigned NOT NULL,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `datekey` int(11) NOT NULL,
  `c3` date DEFAULT NULL
) partition by range columns(datekey, c3)
(partition p20220829 values less than (20220729, '2022-07-29'),
partition p20220830 values less than (20220730, '2022-07-30'),
partition p20220831 values less than (20220731, '2022-07-31'));

CREATE TABLE `${const:com.oceanbase.odc.service.partitionplan.PartitionPlanServiceV2Test.OVERLAP_RANGE_TABLE_NAME}` (
  `id` bigint(20) unsigned NOT NULL,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `datekey` int(11) NOT NULL,
  `c3` date DEFAULT NULL
) partition by range columns(datekey, c3)
(partition p20220829 values less than (20220729, '2024-01-25'),
partition p20220830 values less than (20220730, '2022-07-30'),
partition p20220831 values less than (20220731, '2022-07-31'));