create table if not exists test_view_table(
    c1 int,
    c2 varchar(100)
);

create view view_test1 as select t.c1 as c1, t.c2 as c2 from test_view_table t;
create view view_test2 as select t.c1 as c1 from test_view_table t;