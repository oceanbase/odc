---
--- v3.3.3
---

--
-- connect_connection
-- 1. name size from 64 to 128
-- 2. host size from 64 to 256
--
alter table `connect_connection` ALTER column `name` varchar(128) not null;
alter table `connect_connection` ALTER column `host` varchar(256) not null;

alter table `audit_event` ALTER column `connection_name` varchar(128) null default null;
alter table `audit_event` ALTER column `connection_host` varchar(256) null default null;
