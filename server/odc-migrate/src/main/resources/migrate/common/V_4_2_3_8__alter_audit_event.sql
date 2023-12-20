alter table `audit_event_meta` add column `database_id_extract_expression` varchar(256) DEFAULT NULL COMMENT 'database id extract SpEL';
alter table `audit_event` add column `database_id` bigint(20) DEFAULT NULL COMMENT 'database id related to the event, NULL means this event is not related to any database; refer to connect_database.id';
alter table `audit_event` add column `database_name` varchar(256) DEFAULT NULL COMMENT 'database name related to the event, NULL means this event is not related to any database; refer to connect_database.name';
