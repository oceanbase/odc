drop materialized view if exists test_mv_autoRefresh;
drop materialized view if exists test_mv_never;
drop materialized view if exists test_mv_force;
drop materialized view if exists test_mv_fast;
drop materialized view if exists test_mv_complete;
drop materialized view if exists test_mv_eachColumn;
drop materialized view if exists test_mv_queryRewrite;
drop materialized view if exists test_mv_computation;
drop materialized view if exists test_mv_allSyntax;
drop materialized view log on test_mv_base
drop table if exists test_mv_base;