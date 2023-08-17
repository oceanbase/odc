--
--  v4.1.0
--
CREATE TABLE IF NOT EXISTS `connect_connection_set_top` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for set top relation',
	`connection_id` bigint(20) not null COMMENT 'Top connection id, reference connect_connection(id)',
	`user_id` bigint(20) not null COMMENT 'Top operator, reference iam_user(id)',
	`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT pk_connect_connection_set_top PRIMARY KEY (`id`),
  CONSTRAINT uk_connect_connection_set_top UNIQUE KEY (`connection_id`, `user_id`)
) COMMENT = 'Record the top information of the connection';
insert into connect_connection_set_top(`connection_id`, `user_id`) select `id`, `creator_id` from `connect_connection` where `visible_scope`='PRIVATE' and `is_set_top`=1;
alter table `connect_connection` drop column `is_set_top`;

CREATE TABLE IF NOT EXISTS `connect_connection_label` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for connection label relation',
	`connection_id` bigint(20) not null COMMENT 'Labeled connection id, reference connect_connection(id)',
	`label_id` bigint(20) not null COMMENT 'Label id, reference odc_session_label(id)',
	`user_id` bigint(20) not null COMMENT 'Labeling operator, reference iam_user(id)',
	`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT pk_connect_connection_label PRIMARY KEY (`id`),
  CONSTRAINT uk_connect_connection_label UNIQUE KEY (`connection_id`, `label_id`, `user_id`)
) COMMENT = 'Record the association between connections and tags';
