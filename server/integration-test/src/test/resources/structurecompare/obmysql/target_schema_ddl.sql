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
    `c3` DECIMAL(9, 5) NOT NULL,
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
CREATE TABLE `fk_dependency` (
    `product_id` INT AUTO_INCREMENT PRIMARY KEY,
    `product_name` VARCHAR(100) NOT NULL
);

CREATE TABLE `add_foreign_key_constraint` (
    `order_id` INT AUTO_INCREMENT,
    `product_id` INT ,
    PRIMARY KEY (`order_id`)
);

CREATE TABLE `drop_foreign_key_constraint` (
    `order_id` INT AUTO_INCREMENT,
    `product_id` INT ,
    PRIMARY KEY (`order_id`),
    FOREIGN KEY (`product_id`) REFERENCES `fk_dependency` (`product_id`)
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

CREATE TABLE `converse_to_partition_table` (
    `id` INT(11) AUTO_INCREMENT NOT NULL PRIMARY KEY
);

CREATE TABLE `converse_to_non_partition_table` (
    `id` INT NOT NULL,
    `category` VARCHAR(100)
) PARTITION BY LIST COLUMNS(`category`) (
     PARTITION p0 VALUES IN ('category1', 'category2'),
     PARTITION p1 VALUES IN ('category3', 'category4'),
     PARTITION p2 VALUES IN ('category5', 'category6')
);

CREATE TABLE `modify_partition_type` (
    `id` INT(11) AUTO_INCREMENT NOT NULL PRIMARY KEY
)
PARTITION BY KEY(`id`) PARTITIONS 4;

-- 更新表属性 case
create table `update_options`(
  `c1` INT(11) NOT NULL,
  `c2` INT(11) NOT NULL
) CHARACTER SET = gb18030 COLLATE = gb18030_chinese_ci COMMENT = 'comment2';