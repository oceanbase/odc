-- add column custom_secret to iam_organization
alter table `iam_organization`
add column `custom_secret` varchar(256) default null
comment 'organization secret after migrate';

-- copy secret to iam_organization.custom_secret
update `iam_organization` set `custom_secret` = `secret`;
