---
--- v3.3.3
---

--
-- connect_connection
-- 1. name size from 64 to 128
-- 2. host size from 64 to 256
--
alter table `connect_connection` modify column `name` varchar(128) not null comment 'Database connection address';
alter table `connect_connection` modify column `host` varchar(256) not null comment 'Database connection address';

alter table `audit_event` modify column `connection_name` varchar(128) null default null comment '事件操作所属的连接名，若不属于任何连接，则为 NULL';
alter table `audit_event` modify column `connection_host` varchar(256) null default null comment '事件操作所属连接的主机名，若不属于任何连接，则为 NULL';
