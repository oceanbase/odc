-- add service name for oracle mode
alter table `connect_connection` add column `service_name` varchar(256) DEFAULT NULL COMMENT 'Parameters specific to Oracle mode, which represent a database';

-- add SID for oracle mode
alter table `connect_connection` add column `sid` varchar(256) DEFAULT NULL COMMENT 'Parameters specific to Oracle mode, which represent an instance of a database';

-- add user role for oracle mode
alter table `connect_connection` add column `user_role` varchar(256) DEFAULT NULL COMMENT 'Parameters specific to Oracle mode, which represent the role of a user, enumeration values: normal、sysdba、sysoper';