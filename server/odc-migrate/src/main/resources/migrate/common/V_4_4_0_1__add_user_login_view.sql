CREATE OR REPLACE VIEW `iam_user_login_time_view` AS
SELECT
  `iam_user`.`id` AS `id`,
  `iam_user`.`name` AS `name`,
  `iam_user`.`type` AS `type`,
  `iam_user`.`account_name` AS `account_name`,
  `iam_user`.`description` AS `description`,
  `iam_user`.`organization_id` AS `organization_id`,
  `iam_user`.`email_address` AS `email_address`,
  `iam_user`.`is_active` AS `is_active`,
  `iam_user`.`is_enabled` AS `is_enabled`,
  `iam_user`.`creator_id` AS `creator_id`,
  `iam_user`.`is_builtin` AS `is_builtin`,
  `iam_user`.`user_create_time` AS `user_create_time`,
  `iam_user`.`user_update_time` AS `user_update_time`,
  `iam_user`.`create_time` AS `create_time`,
  `iam_user`.`update_time` AS `update_time`,
  `iam_user`.`extra_properties_json` AS `extra_properties_json`,
  `max_login_time`.`last_login_time`
FROM
  `iam_user`
    LEFT JOIN (
    SELECT
      `user_id`,
      MAX(`login_time`) AS `last_login_time`
    FROM
      `iam_login_history`
    WHERE
      `is_success` = 1
    GROUP BY
      `user_id`
  ) AS `max_login_time` ON `iam_user`.`id` = `max_login_time`.`user_id`;