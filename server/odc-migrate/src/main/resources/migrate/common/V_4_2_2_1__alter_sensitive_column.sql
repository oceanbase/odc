--
-- Add column `type` to table `data_security_sensitive_column`
-- Set default value of column `type` to 'TABLE_COLUMN' to avoid extra migration work
--
alter table `data_security_sensitive_column` add column `type` varchar(32) not null default 'TABLE_COLUMN' comment 'Record the type of the sensitive column';
