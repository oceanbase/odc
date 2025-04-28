-- add column secret_new to iam_organization
alter table `iam_organization`
add column `secret_new` varchar(256) default null
comment 'confused organization secret use caesar';

alter table `iam_organization` MODIFY `secret` varchar(256) null;
-- copy secret to iam_organization.secret_new
update `iam_organization` set `secret_new` = `secret`;
