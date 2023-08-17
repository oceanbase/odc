--
--  v3.2.1
--
alter table `connect_connection` add column is_set_top tinyint not null default 0 comment 'Flag bit, used to indicate whether the connection is on top'
