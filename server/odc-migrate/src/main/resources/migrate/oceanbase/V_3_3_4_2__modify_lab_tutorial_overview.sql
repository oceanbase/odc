ALTER TABLE `lab_tutorial` CHANGE COLUMN `overview` `overview_deprecated` varchar(128);
ALTER TABLE `lab_tutorial` ADD COLUMN `overview` TEXT COMMENT '教程概要';
UPDATE `lab_tutorial` SET `overview` = `overview_deprecated`;
