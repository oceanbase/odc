-- add column obfuscated_secret to iam_organization
alter table `iam_organization`
add column `obfuscated_secret` varchar(256) default null
comment 'organization secret after migrate';

-- copy secret to iam_organization.obfuscated_secret
update `iam_organization` set `obfuscated_secret` = `secret`;
