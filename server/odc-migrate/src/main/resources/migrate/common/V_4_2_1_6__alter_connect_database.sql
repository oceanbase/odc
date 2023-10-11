alter table `connect_database` modify `charset_name` varchar(128) default null comment 'database charset name';
alter table `connect_database` modify `collation_name` varchar(128) default null comment 'database collation name';
alter table `connect_database` modify `table_count` bigint(20) default null comment 'table count which belongs to the database';
alter table `connect_database` modify `last_sync_time` datetime not null default CURRENT_TIMESTAMP comment 'last synchronizing time';
