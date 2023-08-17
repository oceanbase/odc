-- !!从tenant_virtual_object_definition获取object定义时，执行的sql中需要包含六个主键过滤条件
-- 且顺序与主键定义的顺序一致，顺序为object_type, object_name, schema, version, model, transform
CREATE OR REPLACE PACKAGE BODY dbms_metadata AS

  FUNCTION show_create_table_or_view(
    object_type VARCHAR,
    name VARCHAR,
    ob_schema VARCHAR)
  RETURN CLOB
  IS
    ddl_stmt CLOB DEFAULT '';
    obj_sql VARCHAR(32767)
      DEFAULT 'SELECT object_id FROM all_objects WHERE OWNER = ''' || ob_schema
              || ''' and object_name = ''' || name || ''' AND object_type = ''' || object_type || '''';
    show_create_table_sql VARCHAR(32767)
      DEFAULT 'SELECT create_table FROM SYS.TENANT_VIRTUAL_SHOW_CREATE_TABLE WHERE table_id = ?';
    obj_id INTEGER;
    
    table_not_exist EXCEPTION;
    PRAGMA EXCEPTION_INIT(table_not_exist, -5019);
  BEGIN
    EXECUTE IMMEDIATE obj_sql INTO obj_id;
    EXECUTE IMMEDIATE show_create_table_sql INTO ddl_stmt USING obj_id;
    RETURN ddl_stmt;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      RAISE table_not_exist;
  END;

  FUNCTION show_create_routine(
    object_type VARCHAR,
    name VARCHAR,
    ob_schema VARCHAR)
  RETURN CLOB
  IS
    ddl_stmt CLOB DEFAULT '';
    obj_sql VARCHAR(32767)
      DEFAULT 'SELECT object_id FROM all_objects WHERE OWNER = ''' || ob_schema
              || ''' and object_name = ''' || name || ''' AND object_type = ''' || object_type || '''';
    show_create_routine_sql VARCHAR(32767)
      DEFAULT 'SELECT create_routine FROM SYS.TENANT_VIRTUAL_SHOW_CREATE_PROCEDURE WHERE routine_id = ?';
    obj_id INTEGER;

    routine_not_exist EXCEPTION;
    PRAGMA EXCEPTION_INIT(routine_not_exist, -5542);
  BEGIN
    EXECUTE IMMEDIATE obj_sql INTO obj_id;
    EXECUTE IMMEDIATE show_create_routine_sql INTO ddl_stmt USING obj_id;
    RETURN ddl_stmt;

    EXCEPTION WHEN NO_DATA_FOUND THEN
      RAISE routine_not_exist;
  END;

  FUNCTION show_create_package(
    object_type VARCHAR,
    name VARCHAR,
    ob_schema VARCHAR)
  RETURN CLOB
  IS
    ddl_stmt CLOB DEFAULT '';
    show_create_package_sql VARCHAR(32767)
      DEFAULT 'SELECT text FROM all_source WHERE type = ''PACKAGE'' and OWNER = '''
               || ob_schema || ''' and name = ''' || name || '''';
    show_create_package_body_sql VARCHAR(32767)
      DEFAULT 'SELECT text FROM all_source WHERE type = ''PACKAGE BODY'' and OWNER = '''
               || ob_schema || ''' and name = ''' || name || '''';
    spec_ddl_stmt CLOB default '';
    body_ddl_stmt CLOB default '';

    package_not_exist EXCEPTION;
    PRAGMA EXCEPTION_INIT(package_not_exist, -5559);
  BEGIN
    EXECUTE IMMEDIATE show_create_package_sql INTO spec_ddl_stmt;
    EXECUTE IMMEDIATE show_create_package_body_sql INTO body_ddl_stmt;
    IF object_type = 'PACKAGE SPEC' THEN
      ddl_stmt := CONCAT('create or replace ', spec_ddl_stmt);
    ELSIF object_type = 'PACKAGE BODY' THEN
      ddl_stmt := CONCAT('create or replace ', body_ddl_stmt);
    ELSIF object_type = 'PACKAGE' THEN
      ddl_stmt := 'create or replace ' || spec_ddl_stmt || '\n create or replace ' || body_ddl_stmt;
    END IF;
    RETURN ddl_stmt;

    EXCEPTION WHEN NO_DATA_FOUND THEN
      RAISE package_not_exist;
  END;

  FUNCTION get_object_def(
    object_type INT,
    name VARCHAR,
    ob_schema VARCHAR,
    version VARCHAR,
    model VARCHAR,
    transform VARCHAR)
  RETURN CLOB
  IS
    ddl_stmt CLOB DEFAULT '';
    get_obj_def_sql VARCHAR(32767);
  BEGIN
    IF ob_schema IS NULL THEN
      get_obj_def_sql := 'SELECT definition FROM SYS.TENANT_VIRTUAL_OBJECT_DEFINITION WHERE object_type = '''
          || object_type || ''' and object_name = ''' || name || ''' and schema is null and version = '''
          || version || ''' and model = ''' || model || ''' and transform = ''' || transform || '''';
    ELSE
      get_obj_def_sql := 'SELECT definition FROM SYS.TENANT_VIRTUAL_OBJECT_DEFINITION WHERE object_type = '''
          || object_type || ''' and object_name = ''' || name || ''' and schema = '''
          || ob_schema || ''' and version = ''' || version || ''' and model = '''
          || model || ''' and transform = ''' || transform || '''';
    END IF;
    EXECUTE IMMEDIATE get_obj_def_sql INTO ddl_stmt;
    RETURN ddl_stmt;
  END;

  FUNCTION get_ddl (
    object_type     VARCHAR,
    name            VARCHAR,
    ob_schema       VARCHAR DEFAULT NULL,
    version         VARCHAR DEFAULT 'COMPATIBLE',
    model           VARCHAR DEFAULT 'ORACLE',
    transform       VARCHAR DEFAULT 'DDL')
  RETURN CLOB
  IS
    ddl_stmt VARCHAR(32767) default '';
    real_schema VARCHAR(32767);

    invalid_type EXCEPTION;
    PRAGMA EXCEPTION_INIT(invalid_type, -5351);

    not_supported EXCEPTION;
    PRAGMA EXCEPTION_INIT(not_supported, -4007);

    invalid_input_value EXCEPTION;
    PRAGMA EXCEPTION_INIT(invalid_input_value, -9504);
  BEGIN
    -- case sensitive in object type, name, ob_schema, do not transform it;

    IF ob_schema IS NOT NULL and (object_type = 'TABLESPACE' OR object_type = 'USER') THEN
      RAISE invalid_input_value;
    ELSIF ob_schema IS NULL THEN
      real_schema := SYS_CONTEXT('USERENV','CURRENT_USER');
    ELSE 
      real_schema := ob_schema;
    END IF;

    CASE
      WHEN object_type = 'TABLE' OR object_type = 'VIEW' OR object_type = 'INDEX' THEN
        RETURN show_create_table_or_view(object_type, name, real_schema);
      WHEN object_type = 'PROCEDURE' OR object_type = 'FUNCTION' THEN
        RETURN show_create_routine(object_type, name, real_schema);
      WHEN object_type = 'PACKAGE' OR object_type = 'PACKAGE SPEC' OR object_type = 'PACKAGE BODY' THEN
        RETURN show_create_package(object_type, name, real_schema);
      WHEN object_type = 'CONSTRAINT' THEN
        RETURN get_object_def(3, name, real_schema, version, model, transform);
      WHEN object_type = 'REF_CONSTRAINT' THEN
        RETURN get_object_def(4, name, real_schema, version, model, transform);
      WHEN object_type = 'TABLESPACE' THEN
        RETURN get_object_def(5, name, real_schema, version, model, transform);
      WHEN object_type = 'SEQUENCE' THEN
        RETURN get_object_def(6, name, real_schema, version, model, transform);
      WHEN object_type = 'TRIGGER' THEN
        RETURN get_object_def(7, name, real_schema, version, model, transform);
      WHEN object_type = 'USER' THEN
        RETURN get_object_def(8, name, real_schema, version, model, transform);
      WHEN object_type = 'SYNONYM' THEN
        RETURN get_object_def(9, name, real_schema, version, model, transform);
      WHEN object_type = 'TYPE' THEN
        RETURN get_object_def(10, name, real_schema, version, model, transform);
      ELSE
        RAISE invalid_type;
    END CASE;
  END;
END DBMS_METADATA;
//
