ALTER TABLE `iam_user`
    ADD COLUMN `extra_properties_json` TEXT DEFAULT NULL COMMENT '用户自定义的账户额外信息';
