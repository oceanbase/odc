-- 更新主键 case
create table primary_key_test(
  c1 INTEGER PRIMARY KEY,
  c2 INTEGER NOT NULL
);

-- only in source
CREATE TABLE ONLY_IN_SOURCE_PARENT (
    product_id NUMBER NOT NULL,
    CONSTRAINT PK_ONLY_IN_SOURCE_PARENT PRIMARY KEY (product_id)
);

CREATE TABLE ONLY_IN_SOURCE_CHILD (
    sale_id NUMBER NOT NULL,
    product_id NUMBER NOT NULL,
    sale_date DATE NOT NULL,
    customer_id NUMBER NOT NULL,
    salesperson_id NUMBER NOT NULL,
    CONSTRAINT PK_ONLY_IN_SOURCE_CHILD PRIMARY KEY (sale_id),
    CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES ONLY_IN_SOURCE_PARENT (product_id),
    CONSTRAINT uk_sales UNIQUE (sale_id, sale_date),
    CONSTRAINT chk_sales_date CHECK (sale_date > TO_DATE('2020-01-01', 'YYYY-MM-DD'))
)
PARTITION BY HASH (sale_id) PARTITIONS 3;

CREATE INDEX idx_sales_customer ON ONLY_IN_SOURCE_CHILD (customer_id) LOCAL;

CREATE INDEX idx_sales_salesperson ON ONLY_IN_SOURCE_CHILD (salesperson_id);

-- 更新表列 case
CREATE TABLE update_column (
    id INTEGER,
    c1 VARCHAR(100),
    c2 date NOT NULL,
    c3 NUMBER(10, 2) NOT NULL,
    only_in_source_col INTEGER,
    CONSTRAINT pk_update_column PRIMARY KEY (id)
);

-- 更新索引 case
CREATE TABLE update_index (
    id INTEGER,
    c1 INTEGER NOT NULL,
    c2 INTEGER NOT NULL,
    C3 INTEGER NOT NULL,
    CONSTRAINT pk_update_index PRIMARY KEY (id)
);
create index idx1 on update_index(c1);
create index idx_only_in_source on update_index(c2);
create index idx_c1_c2_c3 on update_index (c1,c2,c3);

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
    c1 NUMBER UNIQUE NOT NULL,
    CONSTRAINT pk_update_constraint PRIMARY KEY (id1, id2),
    CONSTRAINT check_only_in_source CHECK (gender IN ('Male', 'Female', 'Other')),
    CONSTRAINT fk_only_in_source FOREIGN KEY (id1, id2) REFERENCES fk_parent(parent_id1, parent_id2)
);