-- add column secret_before_migrate to iam_organization
alter table `iam_organization`
add column `secret_before_migrate` varchar(256) default null
comment 'organization secret before migrate';

-- copy secret to iam_organization.secret_before_migrate
update `iam_organization` set `secret_before_migrate` = `secret`;
