ALTER TABLE `lab_tutorial` ALTER COLUMN `overview` RENAME TO `overview_deprecated`;
ALTER TABLE `lab_tutorial` ADD COLUMN `overview` TEXT COMMENT '教程概要';
