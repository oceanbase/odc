
INSERT INTO  t_2_for_migrate_test (`id`, `name`, `description`) VALUES(1, 'peter', 'user 1') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO  t_2_for_migrate_test (`id`, `name`, `description`) VALUES(2, 'david', 'user 2') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO  t_2_for_migrate_test (`id`, `name`, `description`) VALUES(3, 'sam', 'user 3') ON DUPLICATE KEY UPDATE `id`=`id`;
