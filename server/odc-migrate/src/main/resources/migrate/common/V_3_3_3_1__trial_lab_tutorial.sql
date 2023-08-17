CREATE TABLE IF NOT EXISTS `lab_tutorial` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator_id` bigint COMMENT '创建者ID，FK to iam_user.id',
  `last_modifier_id` bigint DEFAULT NULL COMMENT '最后修改者id，FK to iam_user.id',
  `name` varchar(64) NOT NULL COMMENT '教程名',
  `author` varchar(128) DEFAULT NULL COMMENT '作者，FK to iam_user.name',
  `overview` varchar(128) COMMENT '教程概要',
  `filename` varchar(32) COMMENT '教程文件名',
  `content` MEDIUMTEXT COMMENT '教程内容',
  `language` varchar(32) COMMENT '教程语言',
  CONSTRAINT pk_tutorial_id PRIMARY KEY (`id`),
  CONSTRAINT uk_tutorial_name_language UNIQUE KEY (`name`, `language`),
  CONSTRAINT uk_tutorial_filename_language UNIQUE KEY (`filename`, `language`)
) COMMENT = '教程表';