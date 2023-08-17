CREATE TABLE IF NOT EXISTS `dlm_config_limiter_configuration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `order_id` bigint(20) NOT NULL,
  `data_size_limit` bigint(20) NOT NULL,
  `row_limit` int(11) NOT NULL,
  `batch_size` int(11) NOT NULL,
  PRIMARY KEY (`id`)
);