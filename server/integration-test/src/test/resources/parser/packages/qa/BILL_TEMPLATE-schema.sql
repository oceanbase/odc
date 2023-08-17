DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "BILL_TEMPLATE" is
  TYPE REF_CURSOR IS REF CURSOR;
  TestException Exception;
  FormatError Exception;
  function getKeyFieldName(a_template_id number) return varchar2
  is
    key_field_name varchar2(100);
    tmp_type varchar2(100);
  begin
      select TEMPLATE_RULE_DEF_TYPE into  tmp_type from template_info where template_id=a_template_id;
      case lower(tmp_type)
      when 'tariff_plan' then
          key_field_name:='tariff_plan_id';
		  dbms_output.put_line('1');
      when 'tariff_plan_item' then
          key_field_name:='tariff_plan_item_sn';
		  dbms_output.put_line('2');
      when 'tariff_schema' then
          key_field_name:='tariff_schema_id';
		  dbms_output.put_line('3');
      when 'tariff_item' then
          key_field_name:='tariff_item_sn';
		  dbms_output.put_line('4');
      end case;
      return key_field_name;
  end;

  function getIDfromInfoTable(a_instid number, a_template_id number,
            a_tablename varchar2, a_source_id varchar2) return varchar2
  is
    new_id varchar2(100) := null;
    cnt number;
  begin
      select count(*) into cnt from TEMPLATE_INSTANTE_INFO
      where instantiation_id = a_instid
        and template_id = a_template_id
        and source_rule_table_name = a_tablename
        and source_rule_id = a_source_id;
      if cnt <> 0 then
            select dest_rule_id into new_id from TEMPLATE_INSTANTE_INFO
            where instantiation_id = a_instid
              and template_id = a_template_id
              and source_rule_table_name = a_tablename
              and source_rule_id = a_source_id;
			  dbms_output.put_line('5');
      end if;
      return new_id;
  end;

   procedure getInstID(maxorfree in number,instid out number)
   is
   cnt number(8);
   begin
     select count(*)  into cnt from template_instantiation ;
     if maxorfree = 1 then
        select max(INSTANTIATION_ID)+1 into instid from TEMPLATE_INSTANTIATION;
        if instid > 99999999 then
            select rownum_ into instid from (select rownum rownum_,b.* from (select a.* from template_instantiation a order by instantiation_id )b) where rownum_< instantiation_id and rownum=1;
        dbms_output.put_line('6');
		end if;
        instid:=nvl(instid,1);
     elsif maxorfree =2 then
        select rownum_ into instid from (select rownum rownum_,b.* from (select a.* from template_instantiation a order by instantiation_id )b) where rownum_< instantiation_id and rownum=1;
     dbms_output.put_line('7');
	 end if;
     exception
     when no_data_found then
        if maxorfree=2 then
           if cnt>0 then
              select max(INSTANTIATION_ID)+1 into instid from TEMPLATE_INSTANTIATION;
			  dbms_output.put_line('8');
           else
              instid := 1;
			  dbms_output.put_line('9');
           end if;
        end if;
     when others then
        raise_application_error(-20001,'error when generate instantiation_id');
   end getInstID;
   --实例化
   procedure Instantiation(a_template_type in varchar2,a_param in varchar2,a_instid in number ,a_template_id in number,a_instCnt out number)
   is
   template_type varchar2(40);
   begin
       template_type := lower(a_template_type);
       if template_type= 'tariff_plan' then
            move_tariff_plan(a_instid,a_template_id);
			dbms_output.put_line('10');
       elsif template_type= 'tariff_plan_item' then
           move_tariff_schema(a_instid,a_template_id);
           move_tariff_plan_item(to_number(a_param),a_instid,a_template_id);
		   dbms_output.put_line('11');
       elsif template_type= 'tariff_schema' then
           move_tariff_schema(a_instid,a_template_id);
		   dbms_output.put_line('12');
       elsif template_type= 'tariff_item' then
           move_tariff_schema_item(to_number(a_param),to_number(a_param),a_instid,a_template_id);
		   dbms_output.put_line('13');
       end if;

       update_param_value(a_instid,a_template_id);
       commit;
       select count(*) into a_instCnt from template_instante_info a where a.instantiation_id=a_instid and a.template_id=a_template_id;
       exception
          when others then
		  dbms_output.put_line('14');
            rollback;
            raise_application_error(-20001,'error when move the data from template table');
   end Instantiation;


   procedure justify_data_type(a_table_name in varchar2 ,a_column_name in varchar2,a_lquotation out varchar2,a_rquotation out varchar2)
   is
   m_data_type varchar2(20);
   begin
       select  data_type into m_data_type from user_tab_columns where table_name=a_table_name and column_name=a_column_name;
       case lower(m_data_type)
       when 'number'then
          a_lquotation:='';
          a_rquotation:='';
		  dbms_output.put_line('15');
       when 'varchar2'then
          a_lquotation:='''';
          a_rquotation:='''';
		  dbms_output.put_line('16');
       when 'date' then
          a_lquotation:='to_date(''';
          a_rquotation:=''', ''yyyy-mm-dd hh24:mi:ss'')';
		  dbms_output.put_line('17');
       else
          a_lquotation:='''';
          a_rquotation:='''';
		  dbms_output.put_line('18');
       end case;
       Exception
          when others then
             raise_application_error(-20001,'don''t exist '||a_table_name||'.'||a_column_name);
   end ;
   procedure update_param_value(a_instid in number ,a_template_id in number)
   is
       recPtr template_param_value%rowtype;
       recPtrMap TEMPLATE_PARAM_MAP%rowtype;
       sql_text varchar2(2000);
       lquotation varchar2(30);
       rquotation varchar2(30);
       val varchar2(1024);
       sep varchar2(4);
       idx number(2);
       idx1 number(2);
       new_map_cond varchar2(256);
       new_rule_id varchar2(20);
       pos number(3);
       col_index number(3);
       trd_table_name varchar2(40);
       real_table_name varchar2(40);
   begin
       for recPtr in (select * from template_param_value where INSTANTIATION_ID=a_instid and template_id=a_template_id)
       loop

           for recPtrMap in (select * from template_param_map where template_id=recPtr.Template_Id and param_id=recPtr.Param_Id)
           loop
              val:='';
              trd_table_name:=recPtrMap.Mapping_Table_Name;
              real_table_name:=substr(recPtrMap.Mapping_Table_Name,5,length(recPtrMap.Mapping_Table_Name)-4);
              if recPtrMap.Mapping_Col_Index > 0 then
                  sql_text:='select '||recPtrMap.Mapping_Col_Name||' from '||real_table_name||' where '||recPtrMap.Mapping_Cond;
                  execute immediate sql_text into val;
                  sep:=recPtrMap.Mapping_Sep_Sign;
                  if recPtrMap.Mapping_Col_Index = 1 then--头部分
                     idx1:=instr(val,sep,1,1);
                     if idx1 = 0 then
                        val:=recPtr.Param_Value ;
						dbms_output.put_line('19');
                     else
                        val:=recPtr.Param_Value||substr(val,idx1,length(val)-idx1+1);
						dbms_output.put_line('20');
                     end if;
                  elsif instr(val,sep,1,recPtrMap.Mapping_Col_Index) = 0 then --尾部分
                     col_index :=recPtrMap.Mapping_Col_Index-1;
                     loop
                        idx:=instr(val,sep,1,col_index);
                        exit when idx <>0 ;
                        col_index:=col_index-1;
                        if col_index <=0 then
						dbms_output.put_line('21');
                           raise FormatError;
                        end if;
						dbms_output.put_line('22');
                     end loop;
                     val:=substr(val,1,idx)||recPtr.Param_Value ;
                  else       --中间部分
                    idx:=instr(val,sep,1,recPtrMap.Mapping_Col_Index-1);
                    idx1:=instr(val,sep,1,recPtrMap.Mapping_Col_Index);
                    val:=substr(val,1,idx)||recPtr.Param_Value||substr(val,idx1,length(val)-idx1+1);
					dbms_output.put_line('23');
                  end if;
              else
                  val :=recPtr.Param_Value;
				  dbms_output.put_line('24');
              end if;
              --得到新的id
              pos:=instr(recPtrMap.Mapping_Cond,'=',1);
              select DEST_RULE_ID
                into new_rule_id
                from TEMPLATE_INSTANTE_INFO
               where INSTANTIATION_ID = a_instid
                 and TEMPLATE_ID = a_template_id
                 and SOURCE_RULE_TABLE_NAME = trd_table_name
                 and SOURCE_RULE_ID =
                     substr(recPtrMap.Mapping_Cond,
                            pos + 2,
                            length(rtrim(recPtrMap.Mapping_Cond)) - pos - 2);
              new_map_cond:=substr(recPtrMap.Mapping_Cond,1,pos)||new_rule_id;
              justify_data_type(real_table_name,recPtrMap.Mapping_Col_Name,lquotation,rquotation);
              sql_text:='update '||real_table_name||' set '||recPtrMap.Mapping_Col_Name||'='||lquotation||val||rquotation ||' where '||new_map_cond;
              execute_immediate(sql_text);
           end loop;
       end loop;
       exception
          when FormatError then
		  dbms_output.put_line('25');
            raise_application_error(-20001,'param format exception');
          when no_data_found then
		  dbms_output.put_line('26');
            raise_application_error(-20001,'no data found');
          when others then
            raise_application_error(-20001,'update param error');
   end;


   procedure move_tariff_plan(a_instid in number,a_template_id in number)
   is

   new_plan_id varchar2(20);
   recPtr trd_tariff_plan%rowtype;
   begin
        select * into recPtr from trd_tariff_plan where template_id=a_template_id;
        new_plan_id := getIDfromInfoTable(a_instid,a_template_id,upper('trd_tariff_plan'),to_char(recPtr.tariff_plan_id));
        if new_plan_id is null then
           generateid(1,'tariff_plan',new_plan_id);
		   dbms_output.put_line('27');
        end if;
        insert into tariff_plan
        (
          TARIFF_PLAN_ID     ,
          TARIFF_PLAN_NAME   ,
          IS_RECURSIVE       ,
          PRECEDENCE         ,
          PROCESS_BEFORE     ,
          PROCESS_AFTER      ,
          PLANTYPE           ,
          NOTE               ,
          PRECEDENCE_ACCDISC ,
          PRICEPLANTYPE      ,
          REGION
        )
         values
         (
         to_number(new_plan_id),
         recPtr.TARIFF_PLAN_NAME,
         recPtr.IS_RECURSIVE,
         recPtr.PRECEDENCE,
         recPtr.PROCESS_BEFORE,
         recPtr.PROCESS_AFTER,
         recPtr.PLANTYPE,
         recPtr.NOTE,
         recPtr.PRECEDENCE_ACCDISC,
         recPtr.PRICEPLANTYPE,
         recPtr.REGION
         );
        --插入记录表TEMPLATE_INSTANTE_INFO
        insert into TEMPLATE_INSTANTE_INFO (INSTANTIATION_ID,TEMPLATE_ID,SOURCE_RULE_TABLE_NAME,SOURCE_RULE_ID,DEST_RULE_ID)
        values(a_instid,a_template_id,upper('trd_tariff_plan'),to_char(recPtr.tariff_plan_id),new_plan_id);
        move_tariff_schema(a_instid,a_template_id);
        move_tariff_plan_item(to_number(new_plan_id),a_instid,a_template_id);
        exception
          when others then
             raise_application_error(-20001,'move tariff plan  error');
   end move_tariff_plan;

   procedure move_tariff_schema(a_instid in number,a_template_id in number)
   is
   recPtr trd_tariff_schema%rowtype;
   recInfo TEMPLATE_INSTANTE_INFO%rowtype;
   a_new_schema_id varchar2(20);
   begin
       for recPtr in (select * from trd_tariff_schema where template_id=a_template_id)
       loop
           a_new_schema_id := getIDfromInfoTable(a_instid,a_template_id,upper('trd_tariff_schema'),to_char(recPtr.tariff_schema_id));
            if a_new_schema_id is null then
               generateid(1,'tariff_schema',a_new_schema_id);
			   dbms_output.put_line('28');
            end if;
           insert into tariff_schema
           (
              TARIFF_SCHEMA_ID,
              TARIFF_NAME     ,
              TARIFF_TYPE     ,
              FIELDCOUNT      ,
              FIELD_DEF       ,
              MATCH_ORDER     ,
              MATCH_TYPE      ,
              APPLY_METHOD    ,
              REFID           ,
              DISCOUNT_FEE_ID ,
              G_FIELD_DEF     ,
              ROUND_METHOD    ,
              ROUND_SCALE     ,
              REF_OFFSET      ,
              EVENT_ID        ,
              BILLCODE_ORDER  ,
              CREATETIME      ,
              REGION
            )
           values
           (
              to_number(a_new_schema_id),
              recPtr.TARIFF_NAME    ,
              recPtr.TARIFF_TYPE    ,
              recPtr.FIELDCOUNT     ,
              recPtr.FIELD_DEF      ,
              recPtr.MATCH_ORDER    ,
              recPtr.MATCH_TYPE     ,
              recPtr.APPLY_METHOD   ,
              recPtr.REFID          ,
              recPtr.DISCOUNT_FEE_ID,
              recPtr.G_FIELD_DEF    ,
              recPtr.ROUND_METHOD   ,
              recPtr.ROUND_SCALE    ,
              recPtr.REF_OFFSET     ,
              recPtr.EVENT_ID       ,
              recPtr.BILLCODE_ORDER ,
              recPtr.CREATETIME     ,
              recPtr.REGION);
            insert into TEMPLATE_INSTANTE_INFO(INSTANTIATION_ID,TEMPLATE_ID,SOURCE_RULE_TABLE_NAME,SOURCE_RULE_ID,DEST_RULE_ID)
             values(a_instid,a_template_id,upper('trd_tariff_schema'),to_char(recPtr.tariff_schema_id),a_new_schema_id);
         end loop;
         for recInfo in (select * from TEMPLATE_INSTANTE_INFO where INSTANTIATION_ID =a_instid
                     and TEMPLATE_ID = a_template_id  and SOURCE_RULE_TABLE_NAME ='TRD_TARIFF_SCHEMA')
         loop
             move_tariff_schema_item(recInfo.Dest_Rule_Id,recInfo.Source_Rule_Id,a_instid,a_template_id);
			 dbms_output.put_line('29');
         end loop;
       exception
          when others then
             raise_application_error(-20001,'move tariff schema  error');
   end move_tariff_schema;


   procedure move_tariff_plan_item(a_new_plan_id in number,a_instid in number,a_template_id in number)
   is

   new_tariff_plan_item_sn varchar2(20);
   recPtr trd_tariff_plan_item%rowtype;
   a_new_schema_id varchar2(20);
   begin
      for recPtr in  (select * from trd_tariff_plan_item where template_id=a_template_id)
      loop

          generateid(1,'tariff_plan_item',new_tariff_plan_item_sn);
          a_new_schema_id := getIDfromInfoTable(a_instid,a_template_id,upper('trd_tariff_schema'),to_char(recPtr.tariff_schema_id));
            if a_new_schema_id is null then
               generateid(1,'tariff_schema',a_new_schema_id);
			   dbms_output.put_line('30');
            end if;
          insert into tariff_plan_item(
                TARIFF_PLAN_ID,CALCTYPE,FEE_ID,TARIFF_SCHEMA_ID,CHARGING_EVENT_ID,
                PRECEDENCE,APPLYTIME,EXPIRETIME,APPLY_METHOD,DISCOUNT_TYPE,BASE_FEE_ID ,
                DISCOUNT_FEE_ID,CHARGING_COND,SWITCH_METHOD,SWITCH_UNIT,G_CHARGING_COND,
                BILLCODE,SELECT_METHOD,TARIFF_PLAN_ITEM_NAME,SERV_ID,TARIFF_PLAN_ITEM_SN)
           values(
                  to_number(a_new_plan_id),
                  recPtr.Calctype,
                  recPtr.Fee_Id,
                  to_number(a_new_schema_id),
                  recPtr.Charging_Event_Id,
                  recPtr.Precedence,
                  recPtr.Applytime,
                  recPtr.Expiretime,
                  recPtr.Apply_Method,
                  recPtr.Discount_Type,
                  recPtr.Base_Fee_Id,
                  recPtr.Discount_Fee_Id,
                  recPtr.Charging_Cond,
                  recPtr.Switch_Method,
                  recPtr.Switch_Unit,
                  recPtr.g_Charging_Cond,
                  recPtr.Billcode,
                  recPtr.Select_Method,
                  recPtr.Tariff_Plan_Item_Name,
                  recPtr.Serv_Id,
                  new_tariff_plan_item_sn
                  );
           --插入记录表
           insert into TEMPLATE_INSTANTE_INFO(INSTANTIATION_ID,TEMPLATE_ID,SOURCE_RULE_TABLE_NAME,SOURCE_RULE_ID,DEST_RULE_ID)
           values(a_instid,a_template_id,upper('trd_tariff_plan_item'),to_char(recPtr.tariff_plan_item_sn),new_tariff_plan_item_sn);
       end loop;
       exception
          when others then
             raise_application_error(-20001,'move tariff plan item error');
   end move_tariff_plan_item;


   procedure move_tariff_schema_item(a_new_schema_id in number,a_old_schema_id in number,a_instid in number,a_template_id in number)
   is
      rec REF_CURSOR;
      new_schema_item_sn varchar(20);
      recPtr trd_tariff_item%rowtype;
      new_schema_id varchar2(20);

   begin
         if a_new_schema_id <> a_old_schema_id then
             open rec for 'select * from trd_tariff_item where template_id='||to_char(a_template_id)
                      ||' and tariff_schema_id ='||to_char(a_old_schema_id);
					   dbms_output.put_line('31');
         else
              open rec for 'select * from trd_tariff_item where template_id='||to_char(a_template_id);
			   dbms_output.put_line('32');
         end if;

         loop
             fetch rec into recPtr;
             exit when rec%notfound;


             generateid(1,'tariff_item',new_schema_item_sn);
             if recPtr.Subtariff_Type = 2 then
                 new_schema_id := getIDfromInfoTable(a_instid,a_template_id,upper('trd_tariff_schema'),to_char(recPtr.Tariff_Id));
                 if new_schema_id is null then
                    generateid(1,'tariff_schema',new_schema_id);
					 dbms_output.put_line('33');
                 end if;
                 insert into tariff_item
                 (
                  TARIFF_SCHEMA_ID,
                  APPLYTIME,
                  EXPIRETIME ,
                  TARIFF_CRITERIA,
                  SUBTARIFF_TYPE ,
                  TARIFF_ID,
                  RATIO  ,
                  RATETYPE,
                  PARAM_STRING,
                  PRECEDENCE,
                  EXPR_ID,
                  G_PARAM ,
                  IS_DYNAMIC,
                  G_CRITERIA,
                  BILLCODE,
                  TARIFF_ITEM_SN ,
                  TARIFF_ITEM_NAME
                )
                 values (a_new_schema_id,
                         recPtr.Applytime,
                         recPtr.Expiretime,
                         recPtr.Tariff_Criteria,
                         recPtr.Subtariff_Type,--2：资费模式
                         to_number(new_schema_id),
                         recPtr.Ratio,
                         recPtr.Ratetype,
                         recPtr.Param_String,
                         recPtr.Precedence,
                         recPtr.Expr_Id,
                         recPtr.g_Param,
                         recPtr.Is_Dynamic,
                         recPtr.g_Criteria,
                         recPtr.Billcode,
                         new_Schema_item_sn,
                         recPtr.Tariff_Item_Name
                        );
                 --插入记录表
                 insert into  TEMPLATE_INSTANTE_INFO(INSTANTIATION_ID,TEMPLATE_ID,SOURCE_RULE_TABLE_NAME,SOURCE_RULE_ID,DEST_RULE_ID)
                  values(a_instid,a_template_id,upper('trd_tariff_item'),recPtr.Tariff_Item_Sn,new_schema_item_sn);
             else
                 insert into tariff_item
                 (
                  TARIFF_SCHEMA_ID,
                  APPLYTIME,
                  EXPIRETIME ,
                  TARIFF_CRITERIA,
                  SUBTARIFF_TYPE ,
                  TARIFF_ID,
                  RATIO  ,
                  RATETYPE,
                  PARAM_STRING,
                  PRECEDENCE,
                  EXPR_ID,
                  G_PARAM ,
                  IS_DYNAMIC,
                  G_CRITERIA,
                  BILLCODE,
                  TARIFF_ITEM_SN ,
                  TARIFF_ITEM_NAME
                )
                  values
                  (
                   to_number(a_new_schema_id),
                   recPtr.Applytime,
                   recPtr.Expiretime,
                   recPtr.Tariff_Criteria,
                   recPtr.Subtariff_Type,--2：资费模式
                   recPtr.Tariff_Id,
                   recPtr.Ratio,
                   recPtr.Ratetype,
                   recPtr.Param_String,
                   recPtr.Precedence,
                   recPtr.Expr_Id,
                   recPtr.g_Param,
                   recPtr.Is_Dynamic,
                   recPtr.g_Criteria,
                   recPtr.Billcode,
                   new_Schema_item_sn,
                   recPtr.Tariff_Item_Name
                  );
                  --将数据插入实例表
                  insert into TEMPLATE_INSTANTE_INFO(INSTANTIATION_ID,TEMPLATE_ID,SOURCE_RULE_TABLE_NAME,SOURCE_RULE_ID,DEST_RULE_ID)
                   values(a_instid,a_template_id,upper('trd_tariff_item'),recPtr.Tariff_Item_Sn,new_schema_item_sn);
				    dbms_output.put_line('34');
              end if;
          end loop;
          close rec;
          Exception
              when others then
                  raise_application_error(-20001,'the tariff_item template was destroyed');
   end move_tariff_schema_item;


   procedure generateid(maxorfree in number,whoseid in varchar2,newid out varchar2)
   is
   cnt number(6);
   begin
       case lower(whoseid)
       when 'tariff_plan' then
           select count(*) into cnt from tariff_plan ;
           if maxorfree = 1 then
               select to_char(max(tariff_plan_id)+1) into newid from tariff_plan;
               if length(newid)>6 then
                  select to_char(rownum_) into newid from (select rownum rownum_,b.* from (select tariff_plan_id from tariff_plan a order by tariff_plan_id )b) where rownum_< tariff_plan_id and rownum=1;
                dbms_output.put_line('35');
			   end if;

           else
               select to_char(rownum_) into newid from (select rownum rownum_,b.* from (select tariff_plan_id from tariff_plan a order by tariff_plan_id )b) where rownum_< tariff_plan_id and rownum=1;
            dbms_output.put_line('36');
		   end if;
       when 'tariff_plan_item' then
           if maxorfree = 1 then
               select to_char(to_date('20201103010101','yyyymmddhh24miss'),'yymmddhh24miss')||lpad(to_char(seq_recid8.nextval ),8,0) into newid from dual;
			    dbms_output.put_line('37');
           else
               null;
			    dbms_output.put_line('38');
           end if;
       when 'tariff_schema' then
           select count(*) into cnt from tariff_schema ;
           if maxorfree = 1 then
                select to_char(max(tariff_schema_id)+1) into newid from tariff_schema;
                if length(newid)>9 then
                   select to_char(rownum_) into newid
                       from (select rownum rownum_,b.*
                           from (select tariff_schema_id
                               from tariff_schema a order by tariff_schema_id )b) where rownum_< tariff_schema_id and rownum=1;
							    dbms_output.put_line('39');
                end if;

           else
                select to_char(rownum_) into newid
                from (select rownum rownum_,b.*
                       from (select tariff_schema_id
                             from tariff_schema a order by tariff_schema_id )b) where rownum_< tariff_schema_id and rownum=1;
							  dbms_output.put_line('40');
           end if;
       when 'tariff_item' then
           if maxorfree = 1 then
               select to_char(to_date('20201103010101','yyyymmddhh24miss'),'yymmddhh24miss')||lpad(to_char(seq_recid8.nextval ),8,0) into newid from dual;
			   dbms_output.put_line('41');
           else
               null;
			   dbms_output.put_line('42');
           end if;
       end case;
       newid := nvl(newid,'1');
       exception
           when no_data_found then
             if maxorfree=2 then
               if cnt>0 then
                  case lower(whoseid)
                  when 'tariff_plan' then
                      select to_char(max(tariff_plan_id)+1) into newid from tariff_plan;
					  dbms_output.put_line('43');
                  when 'tariff_schema' then
                      select to_char(max(tariff_schema_id)+1) into newid from tariff_schema;
					  dbms_output.put_line('44');
                  end case;
               else
                  newid := '1';
				  dbms_output.put_line('45');
               end if;
             end if;
           when others then
             raise_application_error(-20001,'generate record id error');
   end generateid;

   procedure clearInstTable(a_instid in number)
   is
   begin
       delete from TEMPLATE_INSTANTIATION where INSTANTIATION_ID=a_instid;
       delete from TEMPLATE_PARAM_VALUE where  INSTANTIATION_ID=a_instid;
	   dbms_output.put_line('46');
       commit;
       exception
       when others then
          rollback;
   end clearInstTable;

   procedure ret_inst_recmsg(a_instid in number ,a_template_id in number,a_start in number, a_end in number, a_retMsg out varchar2)
   is
   tmp_tab_name varchar2(128);
   begin
     for rec in ( select dest_rule_id,Source_Rule_Table_Name from(select rownum rownum_,a.* from template_instante_info a where instantiation_id=a_instid and template_id=a_template_id order by dest_rule_id) where rownum_ between a_start and a_end ) loop
        tmp_tab_name:='';
        select name into tmp_tab_name from code_dict a where a.catalog='TEMPLATE_TABLE_NAME' and a.code=upper(rec.Source_Rule_Table_Name);
        tmp_tab_name:=nvl(tmp_tab_name,rec.Source_Rule_Table_Name);
        a_retMsg:=a_retMsg||tmp_tab_name||' : ['||rec.Dest_Rule_Id||']'||chr(13)||chr(10);
		dbms_output.put_line('47');
     end loop;

     exception
         when others then
            raise_application_error(-20001,'return insert message error');
   end ret_inst_recmsg;


   procedure export_insert_sql(a_instid in number ,a_template_id in number ,a_source_rule_table_name in varchar2,a_dest_rule_id in varchar2,a_ret_sql out varchar2)
   is
     singal_sql_text1 varchar2(2000);
     singal_sql_text2 varchar2(2000);
     real_tab_name varchar2(128);
     exec_sql varchar2(500);
     col_val varchar2(2000);
     index_col_name varchar2(128);
     lquotation varchar2(40);--
     rquotation varchar2(40);
     ilquotation varchar2(30);--index_column 的左右
     irquotation varchar2(30);
     d2c_lquotation varchar2(30);
     d2c_rquotation varchar2(30);

   begin
      for recPtr in (select * from template_instante_info where instantiation_id = a_instid and template_id = a_template_id and source_rule_table_name = a_source_rule_table_name and dest_rule_id=a_dest_rule_id)
      loop
        real_tab_name:=substr(recPtr.source_rule_table_name,5,length(recPtr.source_rule_table_name)-4);
        singal_sql_text1:='insert into '||real_tab_name||' (';
        singal_sql_text2:=' values(';
        select note into index_col_name from code_dict where upper(catalog)='TEMPLATE_TABLE_NAME' and upper(code)=upper(recPtr.source_rule_table_name);
        justify_data_type(real_tab_name,index_col_name,ilquotation,irquotation);
        for tabColPtr in (select * from user_tab_columns where table_name=real_tab_name)
        loop
            singal_sql_text1:=singal_sql_text1||tabColPtr.column_name||',';
            justify_data_type(real_tab_name,tabColPtr.column_name,lquotation,rquotation);
            --将时间类型转换成yyyy-mm-dd hh24:mi:ss 格式
            if lquotation='to_date(''' then
              d2c_lquotation:='to_char(';
              d2c_rquotation:=',''yyyy-mm-dd hh24:mi:ss'')';
			  dbms_output.put_line('48');
            else
              d2c_lquotation:='';
              d2c_rquotation:='';
			  dbms_output.put_line('49');
            end if;
            exec_sql:='select '||d2c_lquotation||tabColPtr.column_name||d2c_rquotation||' from '||real_tab_name||' where '||index_col_name||' = '||ilquotation||upper(recPtr.dest_rule_id)||irquotation;

            execute immediate exec_sql into col_val;
            singal_sql_text2:=singal_sql_text2||lquotation||col_val||rquotation||',';
        end loop;
        singal_sql_text1:=substr(singal_sql_text1,1,length(singal_sql_text1)-1)||')'||chr(13)||chr(10);
        singal_sql_text2:=substr(singal_sql_text2,1,length(singal_sql_text2)-1)||');'||chr(13)||chr(10);
        a_ret_sql:=a_ret_sql||singal_sql_text1||singal_sql_text2;

      end loop;

      exception
        when others then
          raise_application_error(-20001,'export sql text error');
   end export_insert_sql;

begin
  -- Initialization
  --<Statement>;
  null;
  dbms_output.put_line('50');
end BILL_TEMPLATE;
$$