DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_CHECK_PARAM" is

/*  --创建临时表
  create global temporary table wrk_check_param_infotab(
    serialno number(8),
    content varchar2(1024)
  )
  on commit preserve rows;
*/
  g_serialno number(8) := 0; --序列号

  PROCEDURE ap_check_param(a_result out varchar2) is
  v_ret number(1);
  v_count number(1);
  v_sql varchar2(256);
  begin
      a_result := '1';
      v_ret := 1;
      --检查存放错误信息表是否存在
      select count(*) into v_count from user_tables where table_name='WRK_CHECK_PARAM_INFOTAB';
      if v_count < 1 then
      begin
          v_sql := 'create global temporary table wrk_check_param_infotab(serialno number(8),content varchar2(2048)) on commit preserve rows';
          execute immediate v_sql;
		  dbms_output.put_line('1');
      exception when others then
          a_result := '-1';
          return;
      end;
      end if;

      --检查同一实体定义中是否有同一有效属性类对应多个属性
      v_ret := af_check_entity_class();

      if v_ret < 0 then
          a_result := '-1';
		  dbms_output.put_line('2');
          return;
      end if;

      if v_ret <> 1 then
          a_result := to_char(v_ret);
		  dbms_output.put_line('3');
      end if;

      --检查累计量属性在赠送量定义中有配置
      v_ret := af_check_cust_entity_attr();
      if v_ret <> 1 then
          a_result := to_char(v_ret);
		  dbms_output.put_line('4');
      end if;


      --检查赠送量属性在累计量定义中有配置

      --对计费事件的属性进行检查
      v_ret := af_check_chargeevent_continue();
      if v_ret <> 1 then
          a_result := to_char(v_ret);
		  dbms_output.put_line('5');
      end if;

      if a_result = 0 then
          dbms_output.put_line('parameter have problem, please check-look table wrk_check_param_infotab ');
		  dbms_output.put_line('6');
      end if;
  end;

  --检查同一个class对应一个实体的多个属性的情况
  function af_check_entity_class return number is
  v_result number(1);  --返回结果
  v_msg varchar2(1024);
  v_rst number(1);
  v_tmpserialno number(8);
  begin
    v_result := 1;

    --检查同一个class对应一个实体的多个属性的情况
    v_tmpserialno := g_serialno;
    g_serialno := g_serialno + 1;

    --取出有问题的实体号和属性号
    for c1 in (select entity_id, attr_class, attr_id from entity_attr_def where entity_id||attr_class in
        (select entity_id||attr_class from entity_attr_def where attr_class>0
          group by entity_id,attr_class having count(*)>1) order by entity_id,attr_class
    ) loop
    begin
        v_msg := '      entity_id='||to_char(c1.entity_id)||'  attr_class='||to_char(c1.attr_class)||'  attr_id='||to_char(c1.attr_id);

        v_rst := af_insert_data_tab(g_serialno, v_msg);
        if v_rst = -1 then
		dbms_output.put_line('7');
            return -1;
        end if;
        g_serialno := g_serialno + 1;
        v_result := 0;
		dbms_output.put_line('8');
    end;
    end loop;
    if v_result = 1 then
        dbms_output.put_line('In the entity_attr_def table, attr_class check-up have no problem.');
		dbms_output.put_line('9');
    else
        v_msg := 'A attribute class have many attribute in a entity, as follows:';
        v_rst := af_insert_data_tab(v_tmpserialno, v_msg);
		dbms_output.put_line('10');
        if v_rst = -1 then
		dbms_output.put_line('11');
            return -1;
        end if;
    end if;
    return v_result;
  end;

function af_check_cust_entity_attr return number is
	v_count number(8):= 0;
	v_result number(1);
  v_ret boolean;
	function checkit(a_row entity_attr_def%rowtype, a_result out number, a_info varchar2) return boolean
	is
  v_msg varchar2(1024);
  v_rst number(1);
	begin
      v_msg := 'entity_id='||a_row.entity_id||', attr_id='||a_row.attr_id||a_info;
      v_rst := af_insert_data_tab(g_serialno, v_msg);
      if v_rst = -1 then
	  dbms_output.put_line('12');
          return false;
      end if;
      g_serialno := g_serialno + 1;
      a_result := 0;
	  dbms_output.put_line('13');
			return true;
	end;
  begin
    v_result := 1;
    v_ret := true;
    --检查累计量属性在赠送量定义中有配置
	  for r in ( select * from entity_attr_def where attr_type=3 order by entity_id,attr_id )
	  loop
		  select count(*) into v_count from total_usage_attr_def where entity_id=r.entity_id and attr_id=r.attr_id;
      if v_count > 1 then
          v_ret := checkit(r, v_result, '  total usage attr has more than 1 defination in table total_usage_attr_def.');
		  dbms_output.put_line('14');
      end if;

      select count(*) into v_count from free_usage_attr_def where entity_id=r.entity_id and attr_id=r.attr_id;
      if v_count > 0 then
          v_ret := checkit(r, v_result, '  total usage attr has defination in table free_usage_attr_def.');
		  dbms_output.put_line('15');
      end if;
      if v_ret = false then
	  dbms_output.put_line('16');
          return -1;
      end if;
	  end loop;

  --检查赠送量属性在累计量定义中有配置
	  for r in ( select * from entity_attr_def where attr_type=4 order by entity_id,attr_id )
	  loop
      select count(*) into v_count from free_usage_attr_def where entity_id=r.entity_id and attr_id=r.attr_id;
      if v_count > 1 then
          v_ret := checkit(r, v_result, '  free usage attr has more than 1 defination in table free_usage_attr_def.');
		  dbms_output.put_line('17');
      end if;

      select count(*) into v_count from total_usage_attr_def where entity_id=r.entity_id and attr_id=r.attr_id;
      if v_count > 0 then
          v_ret := checkit(r, v_result, '  free usage attr has defination in table total_usage_attr_def.');
		  dbms_output.put_line('18');
      end if;
      if v_ret = false then
	  dbms_output.put_line('19');
          return -1;
      end if;

	  end loop;
    return v_result;
  end;


  --对计费事件的属性进行检查
  function af_check_chargeevent_continue return number is
  v_result number(1);
  v_rst number(1);
  --v_totallocation number(6); --3R1C01B05 AR.FUNC.004.002 z39863 2007-12-21 delete
  v_msg varchar2(1024);
  v_tmpserialno number(8);
  begin
    v_result := 1;
    --V3R1C01B05 AR.FUNC.004.002 z39863 2007-12-21 delete begin
    /*
    v_tmpserialno := g_serialno;
    g_serialno := g_serialno + 1;
    for c1 in (select * from charging_event_def order by event_id) loop
    begin
        --对计费事件的属性进行处理，
        v_totallocation := 3;

        for c2 in (select * from event_attr_def where event_id = c1.event_id order by event_id,attr_id) loop
        begin
            --比较，若不相等，则返回attr_id和位置
            if v_totallocation <> c2.position then
                v_msg := '      event_id='||to_char(c2.event_id)||',  attr_id='||to_char(c2.attr_id);

                --写表
                v_rst := af_insert_data_tab(g_serialno, v_msg);
                if v_rst = -1 then
                    return -1;
                end if;
                g_serialno := g_serialno + 1;

                v_result := 0;
                exit;
            end if;
            v_totallocation := c2.position + c2.length;
        end ;
        end loop;
    end;
    end loop;
    */
    --V3R1C01B05 AR.FUNC.004.002 z39863 2007-12-21 delete end
    --检查没有问题，打印正确的提示，否则返回值为0
    if v_result = 1 then
        dbms_output.put_line('Chaging event definition have no problem.');
		dbms_output.put_line('20');
    else
        v_msg := 'Check charging_event attribute continuity ，event problem as follows:';
        v_rst := af_insert_data_tab(v_tmpserialno, v_msg);
        if v_rst = -1 then
		dbms_output.put_line('21');
            return -1;
        end if;
    end if;
    return v_result;
  end;

  function af_insert_data_tab(a_serialno number, a_msg varchar2) return number is
  begin
      insert into wrk_check_param_infotab(serialno , content) values(a_serialno, a_msg);
	  dbms_output.put_line('22');
      return 1;
  exception when others then
      return -1;
  end;

end pkg_check_param;
$$