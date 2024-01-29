-- 更新主键 case
create table primary_key_test(
  c1 INTEGER NOT NULL,
  c2 INTEGER NOT NULL,
  PRIMARY KEY(c1, c2)
);

-- 更新表列 case
CREATE TABLE update_column (
    id INTEGER,
    c1 VARCHAR(50),
    c2 INTEGER DEFAULT NULL,
    c3 NUMBER(9, 5) NOT NULL,
    only_in_target_col INTEGER,
    CONSTRAINT pk_update_column PRIMARY KEY (id)
);

-- 更新索引 case
CREATE TABLE update_index (
    id INTEGER,
    c1 INTEGER NOT NULL,
    c2 INTEGER NOT NULL,
    C3 INTEGER NOT NULL,
    CONSTRAINT pk_update_index PRIMARY KEY (id),
    INDEX idx1 (c1, c3),
    INDEX idx_only_in_target (c2),
    INDEX idx_c1_c2_c3 (c1,c2,c3)
);

-- 更新约束 case
CREATE TABLE fk_parent (
    parent_id1 NUMBER,
    parent_id2 NUMBER,
    CONSTRAINT pk_fk_parent PRIMARY KEY (parent_id1, parent_id2)
);

CREATE TABLE update_constraint (
    id1 NUMBER,
    id2 NUMBER,
    age NUMBER,
    gender VARCHAR2(10),
    c1 NUMBER,
    CONSTRAINT pk_update_constraint PRIMARY KEY (id1, id2),
    CONSTRAINT check_only_in_target CHECK (age >= 18)
);