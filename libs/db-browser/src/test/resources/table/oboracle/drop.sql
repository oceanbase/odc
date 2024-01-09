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

CREATE OR REPLACE PROCEDURE DROPIFEXISTS_VIEW(new_view IN varchar2)
    IS
    v_count number(10);
BEGIN
    SELECT count(*)
    INTO v_count
    FROM USER_VIEWS
    WHERE VIEW_NAME = upper(new_view);
    IF v_count > 0
    THEN
        EXECUTE IMMEDIATE 'drop view ' || new_view ;
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE DROPIFEXISTS_FUNC(new_func IN varchar2)
    IS
    v_count number(10);
BEGIN
    SELECT count(*)
    INTO v_count
    FROM USER_OBJECTS
    WHERE object_name = upper(new_func) and object_type = 'FUNCTION' ;
    IF v_count > 0
    THEN
        EXECUTE IMMEDIATE 'drop function ' || new_func ;
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE DROPIFEXISTS_SEQUENCE(new_sequence IN varchar2)
    IS
    v_count number(10);
BEGIN
    SELECT count(*)
    INTO v_count
    FROM USER_SEQUENCES
    WHERE sequence_name = upper(new_sequence);
    IF v_count > 0
    THEN
        EXECUTE IMMEDIATE 'drop sequence ' || new_sequence ;
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE DROPIFEXISTS_PACKAGE(new_package IN varchar2)
    IS
    v_count number(10);
BEGIN
    SELECT count(*)
    INTO v_count
    FROM USER_OBJECTS
    WHERE object_name = upper(new_package) AND (object_type = 'PACKAGE' or object_type = 'PACKAGE BODY');
    IF v_count > 0
    THEN
        EXECUTE IMMEDIATE 'drop package ' || new_package ;
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE DROPIFEXISTS_TRIGGER(new_trigger IN varchar2)
    IS
    v_count number(10);
BEGIN
    SELECT count(*)
    INTO v_count
    FROM USER_TRIGGERS
    WHERE TRIGGER_NAME = upper(new_trigger);
    IF v_count > 0
    THEN
        EXECUTE IMMEDIATE 'drop trigger ' || new_trigger ;
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE DROPIFEXISTS_SYNONYM(new_synonym IN varchar2)
    IS
    v_count number(10);
BEGIN
    SELECT count(*)
    INTO v_count
    FROM USER_SYNONYMS
    WHERE SYNONYM_NAME = upper(new_synonym);
    IF v_count > 0
    THEN
        EXECUTE IMMEDIATE 'drop synonym ' || new_synonym ;
    END IF;
END;
/


call DROPIFEXISTS_TABLE('TEST_DATA_TYPE');
call DROPIFEXISTS_TABLE('TEST_OTHER_THAN_DATA_TYPE');
call DROPIFEXISTS_TABLE('TEST_INDEX_TYPE');
call DROPIFEXISTS_TABLE('TEST_INDEX_RANGE');
call DROPIFEXISTS_TABLE('TEST_FK_PARENT');
call DROPIFEXISTS_TABLE('TEST_FK_CHILD');
call DROPIFEXISTS_TABLE('part_hash');
call DROPIFEXISTS_TABLE('TEST_PK_INDEX');
call DROPIFEXISTS_TABLE('TEST_DEFAULT_NULL');

call DROPIFEXISTS_VIEW('VIEW_TEST1');
call DROPIFEXISTS_VIEW('VIEW_TEST2');
call DROPIFEXISTS_TABLE('TEST_VIEW_TABLE');

call DROPIFEXISTS_FUNC('FUNC_TEST');
call DROPIFEXISTS_FUNC('INVALIDE_FUNC');
call DROPIFEXISTS_FUNC('FUNC_DETAIL_TEST');

call DROPIFEXISTS_PACKAGE('T_PACKAGE');
call DROPIFEXISTS_PACKAGE('INVALID_PKG');

call DROPIFEXISTS_TRIGGER('TRIGGER_TEST');
call DROPIFEXISTS_TRIGGER('INVALID_TRIGGER');
call DROPIFEXISTS_TABLE('TRIGGER_TABLE');

call DROPIFEXISTS_SEQUENCE('SEQ_TEST');

call DROPIFEXISTS_TABLE('SYNONYM_TEST_TABLE');
call DROPIFEXISTS_SYNONYM('COMMON_SYNONYM_TEST');
call DROPIFEXISTS_SYNONYM('PUBLIC_SYNONYM_TEST');


