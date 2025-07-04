---
--- v4.4.0
---
CREATE TABLE IF NOT EXISTS `iam_access_key` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Access key ID',
    `organization_id` bigint(20) NOT NULL COMMENT 'Organization ID, FK to iam_organization(id)',
    `user_id` bigint(20) NOT NULL COMMENT 'User ID',
    `access_key_id` varchar(128) NOT NULL COMMENT 'Access Key ID',
    `secret_access_key` varchar(256) NOT NULL COMMENT 'Secret Access Key (encrypted)',
    `salt` varchar(32) NOT NULL COMMENT 'Salt for encryption/decryption',
    `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Access key status: ACTIVE, SUSPENDED, DELETED',
    `creator_id` bigint(20) NOT NULL COMMENT 'Creator ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    CONSTRAINT `pk_iam_access_key_id` PRIMARY KEY (`id`),
    CONSTRAINT `uk_iam_access_key_access_key_id` UNIQUE KEY (`access_key_id`),
    KEY `idx_iam_access_key_user_id` (`user_id`)
) COMMENT='Access Key Management for API authentication';
