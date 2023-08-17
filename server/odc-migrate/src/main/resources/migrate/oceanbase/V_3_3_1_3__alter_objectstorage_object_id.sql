ALTER TABLE `objectstorage_object_block` MODIFY COLUMN `object_id` VARCHAR(1024);
ALTER TABLE `objectstorage_object_metadata` MODIFY COLUMN `object_id` VARCHAR(1024);
ALTER TABLE `script_meta` MODIFY COLUMN `object_id` VARCHAR(1024);