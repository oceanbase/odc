---
--- v3.3.2
---

--
-- connect_connection add is_temp column
--
ALTER TABLE `connect_connection` ADD COLUMN `is_temp` BOOL NOT NULL default 0
  COMMENT '是否临时连接，1 是，0 否';

--
-- iam_user add type column
--
ALTER TABLE `iam_user` ADD COLUMN `type` VARCHAR(16) NOT NULL default 'USER'
  COMMENT '账号类型，可选值 SYSTEM/USER/SERVICE';
