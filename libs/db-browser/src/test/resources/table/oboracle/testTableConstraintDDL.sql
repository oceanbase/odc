create table TEST_FK_PARENT(
  col1 int,
  col2 int,
 CONSTRAINT pk_parent_col1_col2 PRIMARY KEY (col1, col2)
);

CREATE TABLE TEST_FK_CHILD(
  col1 int,
  col2 int,
  CONSTRAINT fk_child_col1_col2 FOREIGN KEY (col1, col2) REFERENCES TEST_FK_PARENT(col1, col2)
);

CREATE TABLE TEST_PK_INDEX (
"A" INTEGER NOT NULL,
CONSTRAINT "PK_TEST" PRIMARY KEY ("A")
);
