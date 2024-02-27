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
        EXECUTE IMMEDIATE 'drop table ' || new_table || ' cascade constraints purge';
    END IF;
END;
/

call DROPIFEXISTS_TABLE('TEST_COL_DATA_TYPE')
/
call DROPIFEXISTS_TABLE('TEST_FK_PARENT')
/
call DROPIFEXISTS_TABLE('TEST_FK_CHILD')
/
call DROPIFEXISTS_TABLE('TEST_PK_INDEX')
/
call DROPIFEXISTS_TABLE('TEST_INDEX_TYPE')
/
call DROPIFEXISTS_TABLE('TEST_VIEW_TABLE')
/
call DROPIFEXISTS_TABLE('PART_HASH_TEST')



