DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "APK_COMMON_DATA_IMPORT" is

  type ref_cursor is ref cursor;

  TYPE tp_array IS VARRAY(26)  OF varchar2(4000);

  function af_get_column_type(av_tablename in varchar2,av_columnname in varchar2) return varchar2
  is
     v_datatype          varchar2(106);
  begin
     --获取列的类型，不需要例外处理
     select data_type into v_datatype from user_tab_cols
        where table_name=upper(av_tablename) and column_name=upper(av_columnname);
		dbms_output.put_line('1');
     return v_datatype;
  end af_get_column_type;

  --向对应的表中插入数据(通用导入界面程序调用)
  procedure ap_insert_data(av_module_id in number,
                           av_column1 in varchar2,
                           av_column2 in varchar2,
                           av_column3 in varchar2,
                           av_column4 in varchar2,
                           av_column5 in varchar2,
                           av_column6 in varchar2,
                           av_column7 in varchar2,
                           av_column8 in varchar2,
                           av_column9 in varchar2,
                           av_column10 in varchar2,
                           av_column11 in varchar2,
                           av_column12 in varchar2,
                           av_column13 in varchar2,
                           av_column14 in varchar2,
                           av_column15 in varchar2,
                           av_column16 in varchar2,
                           av_column17 in varchar2,
                           av_column18 in varchar2,
                           av_column19 in varchar2,
                           av_column20 in varchar2,
                           av_column21 in varchar2,
                           av_column22 in varchar2,
                           av_column23 in varchar2,
                           av_column24 in varchar2,
                           av_column25 in varchar2,
                           av_column26 in varchar2)
  is
     v_ins_sql       varchar2(32767);
     v_ins_value     varchar2(4000);
     v_ref_sql       varchar2(200);
     v_coltype       varchar2(106);
     v_tablename     varchar2(32);
     ref_cur         ref_cursor;
     v_rowtype       param_exp_imp_column_def%rowtype;
     v_array         tp_array;
     v_col_cnt       number(2);
  begin
     --初始化数组
     v_array := tp_array(av_column1,av_column2,av_column3,av_column4,av_column5,av_column6,av_column7,av_column8,
                         av_column9,av_column10,av_column11,av_column12,av_column13,av_column14,av_column15,
                         av_column16,av_column17,av_column18,av_column19,av_column20,av_column21,av_column22,
                         av_column23,av_column24,av_column25,av_column26);
     --获取表名
     select sql_text into v_tablename from param_exp_imp_module t
         where module_type=1 and module_id=av_module_id and inuse=1;

     --构建Insert SQL语句
     v_ins_sql:='insert into '||v_tablename||'(';
     --按照列进行循环，必须按照COLUMN_ID排序
     v_ref_sql:='select * from param_exp_imp_column_def t where t.module_id='
                   ||av_module_id||' and t.inuse=1 order by t.COLUMN_ID';
     --列数
     v_col_cnt:=0;
     v_ins_value:='';
     open ref_cur for v_ref_sql;
     loop
        fetch ref_cur into v_rowtype;
        exit when ref_cur%notfound;
        v_ins_sql:=v_ins_sql||v_rowtype.column_id||',';
        v_col_cnt:=v_col_cnt+1;
        --判断当前列的类型是否是DATE型
        v_coltype:=af_get_column_type(v_tablename,v_rowtype.column_id);
        if(v_coltype='DATE') then
           --判断日期是否为空
           if v_array(v_col_cnt)='' or v_array(v_col_cnt) is null then
              v_ins_value:=v_ins_value||'null,';
			  dbms_output.put_line('2');
           else
              v_ins_value:=v_ins_value||'to_date('''||v_array(v_col_cnt)||''',''yyyy-mm-dd hh24:mi:ss''),';
			  dbms_output.put_line('3');
           end if;
        else
           v_ins_value:=v_ins_value||''''||v_array(v_col_cnt)||''',';
		   dbms_output.put_line('4');
        end if;
     end loop;
     close ref_cur;
     v_ins_sql:=substr(v_ins_sql,1,length(v_ins_sql)-1)||') values(';
     v_ins_value:=substr(v_ins_value,1,length(v_ins_value)-1)||')';
     v_ins_sql:=v_ins_sql||v_ins_value;
     begin
          execute immediate v_ins_sql;
		  dbms_output.put_line('5');
     exception
       when others then
          raise_application_error(-20001,'插入数据失败'||chr(10)||sqlerrm||chr(10)||'SQL: '||v_ins_sql);
          return;
     end;

     commit work;
  end ap_insert_data;


  --向全局临时表中插入数据校验记录
  --向对应的表中插入数据(参数核对界面程序调用)
  procedure ap_insert_check_data(av_flag in number,
                           av_module_id in number,
                           av_column1 in varchar2,
                           av_column2 in varchar2,
                           av_column3 in varchar2,
                           av_column4 in varchar2,
                           av_column5 in varchar2,
                           av_column6 in varchar2,
                           av_column7 in varchar2,
                           av_column8 in varchar2,
                           av_column9 in varchar2,
                           av_column10 in varchar2,
                           av_column11 in varchar2,
                           av_column12 in varchar2,
                           av_column13 in varchar2,
                           av_column14 in varchar2,
                           av_column15 in varchar2,
                           av_column16 in varchar2,
                           av_column17 in varchar2,
                           av_column18 in varchar2,
                           av_column19 in varchar2,
                           av_column20 in varchar2,
                           av_column21 in varchar2,
                           av_column22 in varchar2,
                           av_column23 in varchar2,
                           av_column24 in varchar2,
                           av_column25 in varchar2,
                           av_column26 in varchar2)
  is
     v_ins_sql       varchar2(32767);
     v_ins_value     varchar2(4000);
     v_ref_sql       varchar2(200);
     v_coltype       varchar2(106);
     v_tablename     varchar2(32);
     ref_cur         ref_cursor;
     v_rowtype       param_exp_imp_column_def%rowtype;
     v_array         tp_array;
     v_col_cnt       number(2);
  begin
     --初始化数组
     v_array := tp_array(av_column1,av_column2,av_column3,av_column4,av_column5,av_column6,av_column7,av_column8,
                         av_column9,av_column10,av_column11,av_column12,av_column13,av_column14,av_column15,
                         av_column16,av_column17,av_column18,av_column19,av_column20,av_column21,av_column22,
                         av_column23,av_column24,av_column25,av_column26);
     --获取表名，模块类型为2:参数核对
     select sql_text into v_tablename from param_exp_imp_module t
         where module_type=2 and module_id=av_module_id and inuse=1;

     --构建Insert SQL语句
     v_ins_sql:='insert into '||v_tablename||'(';
     --按照列进行循环，必须按照COLUMN_ID排序
     v_ref_sql:='select * from param_exp_imp_column_def t where t.module_id='
                   ||av_module_id||' and t.inuse=1 order by t.COLUMN_ID';
     --列数
     v_col_cnt:=0;
     v_ins_value:='';
     open ref_cur for v_ref_sql;
     loop
        fetch ref_cur into v_rowtype;
        exit when ref_cur%notfound;
        v_ins_sql:=v_ins_sql||v_rowtype.column_id||',';
        v_col_cnt:=v_col_cnt+1;
        --判断当前列的类型是否是DATE型
        v_coltype:=af_get_column_type(v_tablename,v_rowtype.column_id);
        if(v_coltype='DATE') then
           --判断日期是否为空
           if v_array(v_col_cnt)='' or v_array(v_col_cnt) is null then
              v_ins_value:=v_ins_value||'null,';
			  dbms_output.put_line('6');
           else
              v_ins_value:=v_ins_value||'to_date('''||v_array(v_col_cnt)||''',''yyyy-mm-dd hh24:mi:ss''),';
			  dbms_output.put_line('7');
           end if;
        else
           v_ins_value:=v_ins_value||''''||v_array(v_col_cnt)||''',';
		   dbms_output.put_line('8');
        end if;
     end loop;
     close ref_cur;
     v_ins_sql:=v_ins_sql||'flag) values(';
     v_ins_value:=v_ins_value||av_flag||')';
     v_ins_sql:=v_ins_sql||v_ins_value;
     begin
          execute immediate v_ins_sql;
		  dbms_output.put_line('9');
     exception
       when others then
          raise_application_error(-20001,'插入数据失败'||chr(10)||sqlerrm||chr(10)||'SQL: '||v_ins_sql);
          return;
     end;

     commit work;
  end ap_insert_check_data;

  --同步参数核对结果数据(av_synctype:0 删除 1 插入)
  procedure ap_sync_parameter_del(av_module_id in number,
                           av_column1 in varchar2,
                           av_column2 in varchar2,
                           av_column3 in varchar2,
                           av_column4 in varchar2,
                           av_column5 in varchar2,
                           av_column6 in varchar2,
                           av_column7 in varchar2,
                           av_column8 in varchar2,
                           av_column9 in varchar2,
                           av_column10 in varchar2,
                           av_column11 in varchar2,
                           av_column12 in varchar2,
                           av_column13 in varchar2,
                           av_column14 in varchar2,
                           av_column15 in varchar2,
                           av_column16 in varchar2,
                           av_column17 in varchar2,
                           av_column18 in varchar2,
                           av_column19 in varchar2,
                           av_column20 in varchar2,
                           av_column21 in varchar2,
                           av_column22 in varchar2,
                           av_column23 in varchar2,
                           av_column24 in varchar2,
                           av_column25 in varchar2,
                           av_column26 in varchar2)
  is
     v_tablename        varchar2(32);
     v_ref_sql          varchar2(1000);
     v_del_head         varchar2(100);
     v_del_body         varchar2(32767);
     v_col_cnt          number(2);
     v_coltype          varchar2(106);
     v_sql              varchar2(32767);
     ref_cur            ref_cursor;
     v_array            tp_array;
     v_rowtype          param_exp_imp_column_def%rowtype;
  begin
     --初始化传入参数
     v_array := tp_array(av_column1,av_column2,av_column3,av_column4,av_column5,av_column6,av_column7,av_column8,
                         av_column9,av_column10,av_column11,av_column12,av_column13,av_column14,av_column15,
                         av_column16,av_column17,av_column18,av_column19,av_column20,av_column21,av_column22,
                         av_column23,av_column24,av_column25,av_column26);
     --获取表名，模块类型为3:参数同步
     select sql_text into v_tablename from param_exp_imp_module t
         where module_type=3 and module_id=av_module_id and inuse=1;
     --按照列进行循环,必须按照COLUMN_ID排序，程序中在向存储过程传入参数是的map也是按照column_id进行的
     v_ref_sql:='select * from param_exp_imp_column_def t where t.module_id='
                   ||av_module_id||' and t.inuse=1 order by t.COLUMN_ID';
     --列数
     v_col_cnt:=0;
     --
     v_del_head:='Delete From '||v_tablename||' Where ';
     open ref_cur for v_ref_sql;
     loop
        fetch ref_cur into v_rowtype;
        exit when ref_cur%notfound;
        v_col_cnt:=v_col_cnt+1;
        --判断当前列的类型是否是DATE型
        v_coltype:=af_get_column_type(v_tablename,v_rowtype.column_id);
        if(v_coltype='DATE') then
           --判断日期是否为空
           if v_array(v_col_cnt)='' or v_array(v_col_cnt) is null then
              null;
			  dbms_output.put_line('10');
           else
              v_del_body:=v_del_body||v_rowtype.column_id||'=to_date('''||v_array(v_col_cnt)||''',''yyyy-mm-dd hh24:mi:ss'') and ';
			  dbms_output.put_line('11');
           end if;
        else
           if v_array(v_col_cnt)='' or v_array(v_col_cnt) is null then
              null;
			  dbms_output.put_line('12');
           else
              v_del_body:=v_del_body||v_rowtype.column_id||'='''||v_array(v_col_cnt)||''' and ';
			  dbms_output.put_line('13');
           end if;
        end if;
     end loop;
     close ref_cur;
     v_sql:=v_del_head||v_del_body||' 1=1';
     --execute immediate v_sql;
     begin
          execute immediate v_sql;
		  dbms_output.put_line('14');
     exception
       when others then
          raise_application_error(-20001,'插入数据失败'||chr(10)||sqlerrm||chr(10)||'SQL: '||v_sql);
          return;
     end;
     --提交数据
     commit work;

  end ap_sync_parameter_del;

  --同步数据过程之插入数据平台系统正确的参数
  procedure ap_sync_parameter_ins(av_module_id in number,
                           av_column1 in varchar2,
                           av_column2 in varchar2,
                           av_column3 in varchar2,
                           av_column4 in varchar2,
                           av_column5 in varchar2,
                           av_column6 in varchar2,
                           av_column7 in varchar2,
                           av_column8 in varchar2,
                           av_column9 in varchar2,
                           av_column10 in varchar2,
                           av_column11 in varchar2,
                           av_column12 in varchar2,
                           av_column13 in varchar2,
                           av_column14 in varchar2,
                           av_column15 in varchar2,
                           av_column16 in varchar2,
                           av_column17 in varchar2,
                           av_column18 in varchar2,
                           av_column19 in varchar2,
                           av_column20 in varchar2,
                           av_column21 in varchar2,
                           av_column22 in varchar2,
                           av_column23 in varchar2,
                           av_column24 in varchar2,
                           av_column25 in varchar2,
                           av_column26 in varchar2)
  is
     v_tablename        varchar2(32);
     v_ref_sql          varchar2(1000);
     v_ins_head         varchar2(100);
     v_sql_body_field   varchar2(32767);
     v_sql_body_value   varchar2(32767);
     v_col_cnt          number(2);
     v_coltype          varchar2(106);
     v_sql              varchar2(32767);
     ref_cur            ref_cursor;
     v_array            tp_array;
     v_rowtype          param_exp_imp_column_def%rowtype;
  begin
     --初始化传入参数
     v_array := tp_array(av_column1,av_column2,av_column3,av_column4,av_column5,av_column6,av_column7,av_column8,
                         av_column9,av_column10,av_column11,av_column12,av_column13,av_column14,av_column15,
                         av_column16,av_column17,av_column18,av_column19,av_column20,av_column21,av_column22,
                         av_column23,av_column24,av_column25,av_column26);
     --获取表名，模块类型为3:参数同步
     select sql_text into v_tablename from param_exp_imp_module t
         where module_type=3 and module_id=av_module_id and inuse=1;
     --按照列进行循环,必须按照COLUMN_ID排序
     v_ref_sql:='select * from param_exp_imp_column_def t where t.module_id='
                   ||av_module_id||' and t.inuse=1 order by t.COLUMN_ID';
     --列数
     v_col_cnt:=0;
     --
     v_ins_head:='Insert Into '||v_tablename||'(';
     v_sql_body_field:='';
     v_sql_body_value:='';
     open ref_cur for v_ref_sql;
     loop
        fetch ref_cur into v_rowtype;
        exit when ref_cur%notfound;
        v_col_cnt:=v_col_cnt+1;
        --判断当前列的类型是否是DATE型
        v_coltype:=af_get_column_type(v_tablename,v_rowtype.column_id);
        if(v_coltype='DATE') then
           --判断日期是否为空
           if v_array(v_col_cnt)='' or v_array(v_col_cnt) is null then
              null;
			  dbms_output.put_line('15');
           else
              v_sql_body_field:=v_sql_body_field||v_rowtype.column_id||',';
              v_sql_body_value:=v_sql_body_value||'to_date('''||v_array(v_col_cnt)||''',''yyyy-mm-dd hh24:mi:ss''),';
			  dbms_output.put_line('16');
           end if;
        else
           if v_array(v_col_cnt)='' or v_array(v_col_cnt) is null then
              null;
			  dbms_output.put_line('17');
           else
              v_sql_body_field:=v_sql_body_field||v_rowtype.column_id||',';
              v_sql_body_value:=v_sql_body_value||''''||v_array(v_col_cnt)||''',';
			  dbms_output.put_line('18');
           end if;
        end if;
     end loop;
     close ref_cur;
     v_sql:=v_ins_head||substr(v_sql_body_field,1,length(v_sql_body_field)-1)||') values ('
                      ||substr(v_sql_body_value,1,length(v_sql_body_value)-1)||')';
     begin
          execute immediate v_sql;
		  dbms_output.put_line('19');
     exception
       when others then
          raise_application_error(-20001,'插入数据失败'||chr(10)||sqlerrm||chr(10)||'SQL: '||v_sql);
          return;
     end;
     --不可以提交数据
     commit work;

  end ap_sync_parameter_ins;


end apk_common_data_import;
$$