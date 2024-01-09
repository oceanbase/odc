create table TEST_VIEW_TABLE(
    c1 int,
    c2 varchar(100)
);

create view VIEW_TEST1 as select c1, c2 from TEST_VIEW_TABLE;
create view VIEW_TEST2 as select c1 from TEST_VIEW_TABLE;
