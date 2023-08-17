create table SYNONYM_TEST_TABLE (col varchar(20));

create or replace synonym COMMON_SYNONYM_TEST for SYNONYM_TEST_TABLE;

create or replace public synonym PUBLIC_SYNONYM_TEST for SYNONYM_TEST_TABLE;