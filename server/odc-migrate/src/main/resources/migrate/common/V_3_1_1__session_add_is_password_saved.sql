--
-- for 3.1.1
--

ALTER TABLE `odc_session_manager`
	ADD COLUMN `is_password_saved` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '是否保存连接密码，1 保存， 0 不保存';
