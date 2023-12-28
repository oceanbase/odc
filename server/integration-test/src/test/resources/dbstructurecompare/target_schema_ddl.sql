-- 按照外键依赖顺序删除表 case
CREATE TABLE `fk_dependency_only_in_target1` (
    `customer_id` INT AUTO_INCREMENT PRIMARY KEY,
    `customer_name` VARCHAR(100) NOT NULL
);

CREATE TABLE `fk_dependency_only_in_target2` (
    `product_id` INT AUTO_INCREMENT PRIMARY KEY,
    `product_name` VARCHAR(100) NOT NULL
);

CREATE TABLE `fk_only_in_target` (
    `order_id` INT AUTO_INCREMENT,
    `customer_id` INT ,
    `product_id` INT ,
    `amount` DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (`order_id`),
    FOREIGN KEY (`customer_id`) REFERENCES `fk_dependency_only_in_target1` (`customer_id`),
    FOREIGN KEY (`product_id`) REFERENCES `fk_dependency_only_in_target2` (`product_id`)
);

-- 更新主键 case
create table `primary_key_test`(`c1` INT(11) NOT NULL, `c2` INT(11) PRIMARY KEY);

-- 更新表列 case
CREATE TABLE `update_column` (
    `id` INT(11) PRIMARY KEY,
    `c1` VARCHAR(50),
    `c2` INT(11) DEFAULT NULL,
    `C3` DECIMAL(9, 5) NOT NULL,
    `only_in_target_col` INT(11)
);

-- 更新索引 case
CREATE TABLE `update_index` (
    `c1` INT(11) NOT NULL,
    `c2` INT(11) NOT NULL,
    `C3` INT(11) NOT NULL,
    INDEX `idx1` (`c1`, `c3`),
    INDEX `idx_only_in_target` (`c2`)
);

-- 更新约束 case
CREATE TABLE `update_constraint` (
    `c1` INT(11) NOT NULL,
    `c2` INT(11) NOT NULL,
    `C3` INT(11) NOT NULL,
    CONSTRAINT `cons` UNIQUE (`c1`, `c2`),
    CONSTRAINT `cons_only_in_target` UNIQUE (`c2`)
);

-- 更新分区 case
CREATE TABLE `update_partition` (
    `id` INT NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`, `created_at`)
)
PARTITION BY RANGE (YEAR(`created_at`)) (
    PARTITION p0 VALUES LESS THAN (1995),
    PARTITION p1 VALUES LESS THAN (2005),
    PARTITION p2 VALUES LESS THAN (2015),
    PARTITION p3 VALUES LESS THAN (2025),
    PARTITION p4 VALUES LESS THAN MAXVALUE
);

-- 更新表属性 case
create table `update_options`(
  `c1` INT(11) NOT NULL,
  `c2` INT(11) NOT NULL
) CHARACTER SET = gb18030 COLLATE = gb18030_chinese_ci COMMENT = 'comment2';