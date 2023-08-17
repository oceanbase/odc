-- 禁用硬编码的不再使用的内置用户
update `iam_user` set `is_enabled`=0 where `organization_id`=1 and `account_name`='default@alibaba-inc.com';
update `iam_user` set `is_enabled`=0 where `organization_id`=1 and `account_name`='admin@alibaba-inc.com';