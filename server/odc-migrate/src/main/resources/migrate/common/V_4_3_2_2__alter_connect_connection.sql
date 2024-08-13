--
-- Add column `database_name` to `connect_connection` table
--
alter table `connect_connection` add column `database_name` varchar(255) DEFAULT NULL COMMENT 'Parameters which represent an instance of a database';
