CREATE OR REPLACE PROCEDURE DROPIFEXISTS_TABLE(new_table IN varchar2)
    IS
    v_count number(10);
BEGIN
    SELECT count(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = upper(new_table);
    IF v_count > 0
    THEN
        EXECUTE IMMEDIATE 'drop table ' || new_table || ' purge';
    END IF;
END;
/

call DROPIFEXISTS_TABLE('ROLLBACK_TAB1');
call DROPIFEXISTS_TABLE('ROLLBACK_TAB2');
call DROPIFEXISTS_TABLE('ROLLBACK_UNSUPPORTED_TYPE');
call DROPIFEXISTS_TABLE('ROLLBACK_TEST_TIME_TYPE');