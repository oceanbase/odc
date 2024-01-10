--
-- Add column `expire_time`, `authorization_type` and `ticket_id` to `iam_permission` table
--
ALTER TABLE `iam_permission` ADD COLUMN `expire_time` DATETIME DEFAULT NULL COMMENT 'Expiration time of the permission, the default value is null which means it will never expire';
ALTER TABLE `iam_permission` ADD COLUMN `authorization_type` VARCHAR ( 32 ) NOT NULL DEFAULT 'USER_AUTHORIZATION' COMMENT 'Type of authorization, optional value: USER_AUTHORIZATION ｜ TICKET_APPLICATION';
ALTER TABLE `iam_permission` ADD COLUMN `ticket_id` BIGINT ( 20 ) DEFAULT NULL COMMENT 'ID of the ticket associated with the permission, refer to flow_instance.id, valid only if the source_type is TICKET_APPLICATION';
