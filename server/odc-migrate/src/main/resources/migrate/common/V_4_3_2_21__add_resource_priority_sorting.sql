CREATE TABLE IF NOT EXISTS `resource_priority_sorting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_type` varchar(32) NOT NULL COMMENT 'the type of resource to manage sorted resources',
  `resource_id` bigint NOT NULL COMMENT 'the id of resource to manage sorted resources',
  `sorted_resource_type` varchar(32) NOT NULL COMMENT 'the type of sorted resource',
  `sorted_resource_id` bigint NOT NULL COMMENT 'the id of sorted resource',
  `priority` bigint NOT NULL COMMENT 'the priority of sorted resource，larger priority come first',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_priority_sorting_res_sorted_res` (`resource_type`,`resource_id`,`sorted_resource_type`,`sorted_resource_id`),
  UNIQUE KEY `uk_resource_priority_sorting_res_sorted_res_type_priority` (`resource_type`,`resource_id`,`sorted_resource_type`,`priority`),
) COMMENT = 'sort resource by priority';