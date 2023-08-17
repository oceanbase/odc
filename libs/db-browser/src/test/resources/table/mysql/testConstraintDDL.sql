CREATE TABLE test_fk_parent (
	col1 INT NOT NULL,
	col2 INT NOT NULL,
	constraint pk_parent_col1_col2 primary key (`col1`, `col2`)
);

CREATE TABLE test_fk_child (
	col1 INT NOT NULL,
	col2 INT NOT NULL,
	constraint fk_child_col1_col2 FOREIGN KEY(`col1`, `col2`) REFERENCES test_fk_parent (`col1`, `col2`)
);