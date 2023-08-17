ALTER TABLE `objectstorage_object_block` ALTER COLUMN `object_id` VARCHAR(1024);
ALTER TABLE `objectstorage_object_metadata` ALTER COLUMN `object_id` VARCHAR(1024);
ALTER TABLE `script_meta` ALTER COLUMN `object_id` VARCHAR(1024);