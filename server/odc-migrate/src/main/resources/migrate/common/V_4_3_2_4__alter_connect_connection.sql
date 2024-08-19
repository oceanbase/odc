--
-- Add column `catalog_name` to `connect_connection` table
--
alter table `connect_connection` add column `catalog_name` varchar(255) DEFAULT NULL COMMENT 'Parameters specific to PostgreSql mode, which represent an instance of a database';
