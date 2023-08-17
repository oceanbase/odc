DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "UTIL" is

  procedure print_clob(a_script clob)
  is
        vScript clob := a_script;
    vLen number(10);
    vIndex number(10);
  begin
    vLen := length(vScript);
    vIndex := instr(vScript, chr(10));
    while vIndex < vLen and vIndex > 0
    loop
      dbms_output.put_line(substr(vScript,1,vIndex-1));
      vScript := substr(vScript, vIndex + 1);
      vIndex := instr(vScript, chr(10));
	  dbms_output.put_line('1');
    end loop;
    dbms_output.put_line(vScript);
  end;

    procedure parse_event_str(a_str varchar2, a_trim boolean) is
        vEventId number;
        vVersionId number;
        vValue   varchar2(1024);
        vPosition number;
    begin

        if substr(a_str,1,1) = 'V' then
            vEventId := substr(a_str, 2, 3);
            vPosition := 8;
			dbms_output.put_line('2');
        else
            vEventId := substr(a_str, 1, 3);
            vPosition := 3;
			dbms_output.put_line('3');
        end if;

        select nvl(max(version),1) into vVersionId
              from charging_event_def where event_id = vEventId;
        if substr(a_str,1,1) = 'V' then
            vVersionId := substr(a_str, 5, 4);
			dbms_output.put_line('4');
        end if;

        dbms_output.put_line('');
        dbms_output.put_line('## event_id = ' || vEventId || '## version_id = ' || vVersionId);
        for r in (select a.attr_id, a.attr_name, a.field_name, a.length,
                         decode(a.version,
                                vVersionId, decode(a.datatype,
                                                   1, 'str',
                                                   2, 'dt',
                                                   3, 'int',
                                                   4, 'dec',
                                                   'xxx'),
                                nvl(decode(b.datatype,
                                           1, 'str',
                                           2, 'dt',
                                           3, 'int',
                                           4, 'dec',
                                           'xxx'), decode(a.datatype,
                                                          1, 'str',
                                                          2, 'dt',
                                                          3, 'int',
                                                          4, 'dec',
                                                          'xxx'))) datatype_
                   from
                   (select attr_id,attr_name,field_name,length,datatype,
                           (select version from charging_event_def where event_id=vEventId) version
                           from event_attr_def where event_id=vEventId) a,
                   (select * from event_attr_datatype_def where event_id=vEventId
                           and nvl(minversion,1)<=vVersionId and
                           vVersionId <= decode(nvl(maxversion,0),0,9999,maxversion)) b
                   where a.attr_id=b.attr_id(+)
                   )
        loop

            vValue := substrb(a_str, vPosition + 1, r.length);
            if a_trim then
                vValue := trim(vValue);
				dbms_output.put_line('5');
            end if;
            dbms_output.put_line(lpad(r.attr_id, 4) || '#' ||
                                 lpad(vPosition, 4) || ':' ||
                                 lpad(r.length, 4) || ':' ||
                                 lpad(r.datatype_, 3) || ':' ||
                                 lpad(r.attr_name, 16) || ':' ||
                                 lpad(r.field_name, 15) || '=[' || vValue || ']');
            vPosition := vPosition + r.length;
        end loop;
    end;

    --select table_name, count(*), min(trigger_name), max(trigger_name) from user_triggers group by table_name having count(*)>1;

    function get_audit_trigger_script(a_table_name varchar2) return clob is
        vScript    clob := '';
        vTableName varchar2(100);
        vTrigName  varchar2(100);
        vStr       clob;
        vTmpStr    varchar2(2048);
        vIndex     number(10);
    begin
        vTableName := upper(a_table_name);
        vTrigName  := 'TRIG_' || vTableName || '_AUDIT';
        if length(vTrigName) > 30 then
            vTrigName := 'TRIG_' || vTableName;
			dbms_output.put_line('6');
        end if;

        vScript := 'create or replace trigger ' || vTrigName || chr(10);
        vScript := vScript || '    after delete or insert or update on ' ||
                   vTableName || ' for each row' || chr(10);
        vScript := vScript ||
                   '-- trigger created using util.create_audit_trigger() [by: Junqiang,Luo]' ||
                   chr(10);

        vScript := vScript || 'declare' || chr(10) ||
                   '    vAuditLog       pkg_validuser.AuditLogType;' || chr(10) ||
                   'begin' || chr(10) ||
                   '    vAuditLog.TableName:=''' || vTableName || ''';' || chr(10) ||
                   '    if INSERTING then' || chr(10) ||
                   '        vAuditLog.MaintainType := ''I'';' || chr(10) ||
                   '    elsif UPDATING then' || chr(10) ||
                   '        vAuditLog.MaintainType := ''U'';' || chr(10) ||
                   '    elsif DELETING then' || chr(10) ||
                   '        vAuditLog.MaintainType := ''D'';' || chr(10) ||
                   '    else' || chr(10) ||
                   '        vAuditLog.MaintainType := ''?'';' || chr(10) ||
                   '    end if;' || chr(10) ||
                   '    --get key of ' || vTableName || chr(10);
        vStr   := '';
        vIndex := 0;
        for r in (select b.column_name, a.data_type
                    from user_tab_columns a, user_ind_columns b
                   where b.index_name = (select index_name
                                           from user_indexes
                                          where table_name = vTableName
                                            and uniqueness = 'UNIQUE'
                                            and rownum = 1)
                     and a.table_name = vTableName
                     and a.column_name = b.column_name)
        loop
            vIndex := vIndex + 1;
            if r.data_type = 'DATE' then
                vTmpStr := 'to_char(:old.' || r.column_name || ',''yyyy-mm-dd hh24:mi:ss'')';
				dbms_output.put_line('7');
            else
                vTmpStr := ':old.' || r.column_name;
				dbms_output.put_line('8');
            end if;
            if vIndex <> 1 then
                vStr := vStr || ' and ''' || chr(10);
				dbms_output.put_line('9');
            end if;
            vStr := vStr || '        || ''' || r.column_name || '=''''''||' || vTmpStr || '||''''''';
        end loop;
        if vIndex = 0 then
		dbms_output.put_line('10');
            raise_application_error(-20001, 'table has no unique index.');
        end if;
        vScript := vScript ||
                    '    vAuditLog.KeyValue := ''''' || chr(10) || vStr || ''';' || chr(10) || chr(10) ||
                    '    if pkg_validuser.BeginAuditLog(vAuditLog) = 0 then' || chr(10) ||
                    '        return;' || chr(10) ||
                    '    end if;' || chr(10) || chr(10);
        for r in (select column_name, data_type
                    from user_tab_columns
                   where table_name = vTableName
                   order by column_id)
        loop
            vScript := vScript ||
                       '    pkg_validuser.WriteAuditLog(vAuditLog, '''|| r.column_name ||
                       ''', :old.' || r.column_name || ', :new.' || r.column_name || ');' || chr(10);
					   dbms_output.put_line('11');
        end loop;
        vScript := vScript || chr(10) ||
                   '    pkg_validuser.EndAuditLog(vAuditLog);' || chr(10) ||
                   'end; --end of this trigger' || chr(10);
        return vScript;
    end;

    procedure create_audit_trigger(a_table_name varchar2,
                                   a_execute    boolean) is
        vScript long;
    begin
        vScript := get_audit_trigger_script(a_table_name);
        if a_execute then
            execute immediate vScript;
			dbms_output.put_line('12');
        else
		dbms_output.put_line('13');
            print_clob(vScript);
        end if;
    end;
/*
  function get_plan_used_tariffs(a_tarifflist sys_refcursor) return PlanUsedTariffSet pipelined
  is
  begin
    return;
  end;

  function get_tariff_plan_used_tariffs(a_planlist sys_refcursor) return PlanUsedTariffSet pipelined
  is
    v PlanUsedTariffSet;
  begin
    v := get_plan_used_tariffs(a_planlist);
    return;
  end;

  function get_tariff_plan_usage_attr(a_planlist sys_refcursor) return PlanUsageAttrSet pipelined
  is
    v_planid number(8);
    v_row PlanUsageAttrRec;
  begin
    loop
        fetch a_planlist into v_planid;
        exit when a_planlist%notfound;
      for r in (select * from tariff_plan where tariff_plan_id=v_planid)
      loop

      v_row.plan_id := v_planid;
      v_row.usage_type := 0;
      v_row.attr_id := 999;
      pipe row(v_row);
    end loop;
    return;
  end;
*/
    function get_event_script(a_event_id number, a_table_name varchar2 := 'event') return clob is
        vScript clob := '';
        vIndex  number(10) := 0;
        vStr    varchar2(100);

        --V3R1C1B093 AR.FUNC.011 39824 add begin
        v_Length  number(10) := 0;
        v_Version  number(10) := 0;
        i  number(10) := 1;
        vScriptvar    varchar2(32767);
        INVALID__DML exception;
        pragma EXCEPTION_INIT(INVALID__DML, -14552);
        --V3R1C1B093 AR.FUNC.011 39824 add end

    begin
        vScript := 'create table '||upper(a_table_name)||' (' || chr(10);
        for r in (select *
                    from event_attr_def
                   where event_id = a_event_id
                     and trim(field_name) is not null
                   order by attr_id)
        loop
            if vIndex > 0 then
                vScript := vScript || ',' || chr(10);
				dbms_output.put_line('14');
            end if;

            --V3R1C1B093 AR.FUNC.011 39824 add get version length begin
            while i <= length(r.field_name) loop
                if ascii(substr(r.field_name, i, 1)) > 127 then
                    raise_application_error(-20001,
                    'include nonsupport character in event_attr_def.field_name: '
                    ||r.field_name);
					dbms_output.put_line('15');
                end if;
                i := i + 1;
            end loop;

            select max(version) into v_Version
              from charging_event_def
             where event_id = a_event_id;

       begin
            select r.length into v_Length
              from event_attr_datatype_def
             where event_id = a_event_id and attr_id = r.attr_id
               and v_Version >= nvl(minversion, 1)
               and v_Version <= decode(nvl(maxversion,0),0,9999,maxversion);
			   dbms_output.put_line('16');
            exception
               when others then
                    v_Length := r.length;
       end;

            if r.datatype = 1 then
                --string
                vStr := 'varchar2(' || v_Length || ')';
				dbms_output.put_line('17');
            elsif r.datatype = 2 then
                --datetime
                vStr := 'date';
				dbms_output.put_line('18');
            elsif r.datatype = 3 then
                --integer
                vStr := 'number(' || v_Length || ')';
				dbms_output.put_line('19');
            elsif r.datatype = 4 then
                --float
                if r.scale > 0 then
                    v_Length := v_Length - 1;
					dbms_output.put_line('20');
                end if;
                vStr := 'number(' || v_Length || ',' || r.scale || ')';
            elsif r.datatype = 6 then
                --bigint
                vStr := 'number(' || v_Length || ')';
				dbms_output.put_line('21');
            else
			dbms_output.put_line('22');
                raise_application_error(-20001, 'unknown datatype.');
            --V3R1C1B093 AR.FUNC.011 39824 add get version length end

            end if;
            vScript := vScript || upper( r.field_name ||' '|| vStr);
      vIndex := vIndex + 1;
        end loop;
        vScript := vScript || chr(10) || ')' || chr(10);

        --V3R1C1B093 AR.FUNC.011 39824 add get version length begin
        begin
            vScriptvar := 'explain plan for '||vScript;
            execute immediate vScriptvar;
			dbms_output.put_line('23');
        exception
            when INVALID__DML then
			dbms_output.put_line('24');
                return vScript;
            when others then
                return chr(10)||chr(10)||sqlerrm||chr(10)||chr(10)||chr(10)||vScript;
        end;
        --V3R1C1B093 AR.FUNC.011 39824 add get version length end
    end;

    --V3R1C1B093 AR.FUNC.011 39824 2009-2-27 add begin
    procedure check_sql_syntax(a_sql clob)
    is
        l_cursor integer default 0;
    begin
        l_cursor := dbms_sql.open_cursor;
        dbms_sql.parse(l_cursor, 'explain plan for '||a_sql, dbms_sql.native);
        dbms_sql.close_cursor(l_cursor);
		dbms_output.put_line('25');
    end;
    --V3R1C1B093 AR.FUNC.011 39824 2009-2-27 add end

  function get_billingchecks(a_billingdef sys_refcursor) return BillingCheckSet pipelined
  is
    v_item_expr varchar2(200);
    v_pos number(4) := 1;
    v_record BillingCheckRec;
  begin
    loop
      fetch a_billingdef into v_record.event_id, v_item_expr;
      exit when a_billingdef%notfound;
      v_pos := 1;
      loop
          v_pos := instr(v_item_expr,'0005',v_pos);
          exit when v_pos = 0;
        v_pos := v_pos + 6;
        v_record.attr_id := substr(v_item_expr,v_pos,4);
        pipe row(v_record);
        v_pos := v_pos + 4;
		dbms_output.put_line('26');
      end loop;
    end loop;
    return;
  end;
end util;
$$