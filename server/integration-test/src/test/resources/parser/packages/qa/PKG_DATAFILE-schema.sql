DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_DATAFILE" is
v_note          varchar2(200);
v_ret_imp       varchar2(100) := '导入记录条数：';
v_ret_new       varchar2(100) := '，新增记录条数：';
v_ret_old       varchar2(100) := '，已存在记录数：';
v_ret_diff      varchar2(100) := '，不一致记录数：';
v_ret_del       varchar2(100) := '，禁用记录条数：';
v_ret_update    varchar2(100) := '，更新记录条数：';
v_ret_dup       varchar2(100) := '，重复纪录条数：';
v_ret_ask   varchar2(100) := '要求处理记录条数：';
v_ret_done   varchar2(100) := '，正确处理记录条数：';
v_ret_error varchar2(100) := '，未处理记录条数：';
v_ret_delete varchar2(100) := '，删除记录条数：';

procedure DATAPROC(filetype number,v_result  in out  varchar2) as
/*
    基本流程（单表更新）：
        遍历临时表记录
            判断数据是否存在，如果不存在（或标志状态为禁用）标记为新增记录1，直接插入数据
            如果数据已存在，判断数据是否一致，如果不一致标记为不一致记录3，更新数据
            如果数据一致，标记为已存在2，不做进一步处理
*/
begin
    case
        when filetype=1 then PROC_MAPPING_LIST_SP(60,v_result);    --梦网短信企业局数据
		dbms_output.put_line('1');
        when filetype=3 then PROC_MAPPING_LIST_SP(62,v_result);    --WAP企业局数据
		dbms_output.put_line('2');
        when filetype=5 then PROC_MAPPING_LIST_SP(64,v_result);    --PDA企业局数据     
        dbms_output.put_line('3');
		when filetype=7 then PROC_MAPPING_LIST_SP(66,v_result);    --MMS企业局数据
        dbms_output.put_line('4');
		when filetype=9 then PROC_MAPPING_LIST_SP(68,v_result);    --KJAVA企业局数据
        dbms_output.put_line('5');
		when filetype=11 then PROC_MAPPING_LIST_SP(73,v_result);    --流媒体企业局数据
        dbms_output.put_line('6');
		when filetype=13 then PROC_MAPPING_LIST_SP(75,v_result);    --手机动画企业局数据
        dbms_output.put_line('7');
		when filetype=15 then PROC_MAPPING_LIST_SP(77,v_result);    --自有短信企业局数据
        dbms_output.put_line('8');
		when filetype=17 then PROC_MAPPING_LIST_SP(79,v_result);    --捐款短信企业局数据
        dbms_output.put_line('9');
		when filetype=19 then PROC_MAPPING_LIST_SP(81,v_result);    --手机邮箱企业局数据
dbms_output.put_line('10');
        when filetype=21 then PROC_MAPPING_LIST_SNSP(83,v_result);    --省内梦网短信企业局数据
        dbms_output.put_line('11');
		when filetype=23 then PROC_MAPPING_LIST_SNSP(84,v_result);    --省内梦网彩信企业局数据
        dbms_output.put_line('12');
		when filetype=25 then PROC_MAPPING_LIST_SNSP(85,v_result);    --省内WAP企业局数据
dbms_output.put_line('13');
        when filetype=22 then PROC_MAPPING_LIST_SNSP(-83,v_result);    --省内梦网短信业务局数据
        dbms_output.put_line('14');
		when filetype=24 then PROC_MAPPING_LIST_SNSP(-84,v_result);    --省内梦网彩信业务局数据
        dbms_output.put_line('15');
		when filetype=26 then PROC_MAPPING_LIST_SNSP(-85,v_result);    --省内WAP业务局数据
        dbms_output.put_line('16');
        when filetype=2 then PROC_MAPPING_LIST_OPER(61,v_result);    --梦网短信业务局数据
        dbms_output.put_line('17');
		when filetype=4 then PROC_MAPPING_LIST_OPER(63,v_result);    --WAP业务局数据
        dbms_output.put_line('18');
		when filetype=6 then PROC_MAPPING_LIST_OPER(65,v_result);    --PDA业务局数据     
        dbms_output.put_line('19');
		when filetype=8 then PROC_MAPPING_LIST_OPER(67,v_result);    --MMS业务局数据
        dbms_output.put_line('20');
		when filetype=10 then PROC_MAPPING_LIST_OPER(69,v_result);    --KJAVA业务局数据
        dbms_output.put_line('21');
		when filetype=12 then PROC_MAPPING_LIST_OPER(74,v_result);    --流媒体业务局数据
        dbms_output.put_line('22');
		when filetype=14 then PROC_MAPPING_LIST_OPER(76,v_result);    --手机动画业务局数据
       dbms_output.put_line('23');
		when filetype=16 then PROC_MAPPING_LIST_OPER(78,v_result);    --自有短信业务局数据
        dbms_output.put_line('24');
		when filetype=18 then PROC_MAPPING_LIST_OPER(80,v_result);    --捐款短信业务局数据
        dbms_output.put_line('25');
		when filetype=20 then PROC_MAPPING_LIST_OPER(82,v_result);    --手机邮箱业务局数据
dbms_output.put_line('26');
        when filetype=50 then PROC_HIGHCOSTEXCHANGERATE(v_result);    --高额汇率
        dbms_output.put_line('27');
        else v_result := '未找到该类型文件处理方法，请确认已定义';
		dbms_output.put_line('28');
    end case;
   
end;
procedure DATA2DB(filetype number,applydate varchar2,v_result  in out  varchar2) as
/*
    基本流程（单表更新）：
        遍历临时表记录（STATUS in（1，2）；PROCFLAG=0)
            如果是新增记录，判断是否禁用，
                是则更新失效日期使其禁用
                否则直接插入记录数据
            如果是更新记录，则更新记录数据
            操作成功，当前记录PROCFLAG置为1
*/
begin
    case
        --以下数据业务企业局数据同步：
        when filetype=1 then DB_MAPPING_LIST_SP(60,v_result);    --梦网短信企业局数据
        dbms_output.put_line('29');
		when filetype=3 then DB_MAPPING_LIST_SP(62,v_result);    --WAP企业局数据
        dbms_output.put_line('30');
		when filetype=5 then DB_MAPPING_LIST_SP(64,v_result);    --PDA企业局数据     
        dbms_output.put_line('31');
		when filetype=7 then DB_MAPPING_LIST_SP(66,v_result);    --MMS企业局数据
        dbms_output.put_line('32');
		when filetype=9 then DB_MAPPING_LIST_SP(68,v_result);    --KJAVA企业局数据
        dbms_output.put_line('33');
		when filetype=11 then DB_MAPPING_LIST_SP(73,v_result);    --流媒体企业局数据
        dbms_output.put_line('34');
		when filetype=13 then DB_MAPPING_LIST_SP(75,v_result);    --手机动画企业局数据
        dbms_output.put_line('35');
		when filetype=15 then DB_MAPPING_LIST_SP(77,v_result);    --自有短信企业局数据
        dbms_output.put_line('36');
		when filetype=17 then DB_MAPPING_LIST_SP(79,v_result);    --捐款短信企业局数据
        dbms_output.put_line('37');
		when filetype=19 then DB_MAPPING_LIST_SP(81,v_result);    --手机邮箱企业局数据
dbms_output.put_line('38');
        when filetype=21 then DB_MAPPING_LIST_SNSP(83,v_result);    --省内梦网短信企业局数据
        dbms_output.put_line('39');
		when filetype=23 then DB_MAPPING_LIST_SNSP(84,v_result);    --省内梦网彩信企业局数据
        dbms_output.put_line('40');
		when filetype=25 then DB_MAPPING_LIST_SNSP(85,v_result);    --省内WAP企业局数据
        dbms_output.put_line('41');
		when filetype=22 then DB_MAPPING_LIST_SNSP(-83,v_result);    --省内梦网短信企业局数据
       dbms_output.put_line('42');
		when filetype=24 then DB_MAPPING_LIST_SNSP(-84,v_result);    --省内梦网彩信企业局数据
        dbms_output.put_line('43');
		when filetype=26 then DB_MAPPING_LIST_SNSP(-85,v_result);    --省内WAP企业局数据
dbms_output.put_line('44');
        --以下数据业务业务局数据都是全量同步：
        when filetype=2 then DB_MAPPING_LIST_OPER(61, v_result);    --梦网短信业务局数据
        dbms_output.put_line('45');
		when filetype=4 then DB_MAPPING_LIST_OPER(63, v_result);    --WAP业务局数据
        dbms_output.put_line('46');
		when filetype=6 then DB_MAPPING_LIST_OPER(65, v_result);    --PDA业务局数据    
        dbms_output.put_line('47');
		when filetype=8 then DB_MAPPING_LIST_OPER(67, v_result);    --MMS业务局数据
        dbms_output.put_line('48');
		when filetype=10 then DB_MAPPING_LIST_OPER(69, v_result);    --KJAVA业务局数据
        dbms_output.put_line('49');
		when filetype=12 then DB_MAPPING_LIST_OPER(74,v_result);    --流媒体业务局数据
        dbms_output.put_line('50');
		when filetype=14 then DB_MAPPING_LIST_OPER(76,v_result);    --手机动画业务局数据
        dbms_output.put_line('51');
		when filetype=16 then DB_MAPPING_LIST_OPER(78,v_result);    --自有短信业务局数据
        dbms_output.put_line('52');
		when filetype=18 then DB_MAPPING_LIST_OPER(80,v_result);    --捐款短信业务局数据
        dbms_output.put_line('53');
		when filetype=20 then DB_MAPPING_LIST_OPER(82,v_result);    --手机邮箱业务局数据
        dbms_output.put_line('54');
		--when filetype=1 then DB_SET_VALUE_SP(applydate, v_result);    --梦网SP局数据
        /*
        when filetype=22 then DB_MAPPING_LIST_SNOPER(83,v_result);    --省内梦网短信业务局数据
        when filetype=24 then DB_MAPPING_LIST_SNOPER(84,v_result);    --省内梦网彩信业务局数据
        when filetype=26 then DB_MAPPING_LIST_SNOPER(85,v_result);    --省内WAP业务局数据
        */
        when filetype=50 then DB_HIGHCOSTEXCHANGERATE(v_result);    --高额汇率
        dbms_output.put_line('55');
		else    v_result := '未找到该类型文件处理方法，请确认已定义';
		dbms_output.put_line('56');
    end case;
end;



procedure PROC_MAPPING_LIST_SP(v_map_id number, v_result  in out  varchar2) as
v_spcode        varchar2(100);
v_spname        varchar2(100);
v_provcode      varchar2(100);
v_hprovcode     varchar2(100);
v_gateway       varchar2(100);
v_servcode      varchar2(100);
v_applytime     varchar2(100);
v_expiretime    varchar2(100);

v_sour          varchar2(200);
v_dest          varchar2(200);

v_count         number(6) := 0;
v_delnum        number(6) := 0;
v_newnum        number(6) := 0;
v_oldnum        number(6) := 0;
v_diffnum       number(6) := 0;

begin
    for c1 in(select F1,F2,F3,F4,F5,F6,F7,F8,F9,rowid from WORK_DATAFILE where STATUS=0 order by ID)
    loop
        if (v_map_id=60 or v_map_id=66 or v_map_id=79 or v_map_id=81)  then--梦网短信,mms,捐款短信(PM),手机邮箱(YX)企业局数据
            v_spcode     := c1.F1;
            v_spname     := c1.F2;
            v_provcode   := c1.F3;
            v_gateway    := c1.F4;
            v_servcode   := c1.F5;
            v_applytime  := c1.F6;
            v_expiretime := c1.F7;
            v_sour :=v_spcode || ',' || v_servcode;
            v_dest :='1,'|| v_spname|| ','|| v_provcode;            
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F6=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('57');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
                --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F7=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('58');
            end if;
        
        elsif (v_map_id=62 or v_map_id=64 or v_map_id=75) then	--wap,pda,flh企业局数据
            v_spcode     := c1.F1;
            v_spname     := c1.F2;
            v_applytime  := c1.F3;
            v_expiretime := c1.F4;
            v_sour :=v_spcode;
            v_dest :='1,'|| v_spname|| ','|| v_provcode;            
            
            if(v_applytime is null) then
               v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
               update work_datafile set F3=v_applytime where rowid=c1.rowid;
			   dbms_output.put_line('59');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
               --v_expiretime := '20370101000000';
               v_expiretime := null;
               update work_datafile set F4=v_expiretime where rowid=c1.rowid;
			   dbms_output.put_line('60');
            end if;       
      
        elsif (v_map_id=77) then   --自有短信(CM)    
            v_spcode     := c1.F1;
            v_spname     := c1.F2;
            v_provcode   := c1.F3;
            v_hprovcode   := c1.F4;
            v_gateway    := c1.F5;
            v_servcode   := c1.F6;
            v_applytime  := c1.F7;
            v_expiretime := c1.F8;
            
            v_sour :=v_spcode || ',' || v_servcode;
            v_dest :='1,'|| v_spname|| ','|| v_provcode|| ',';            
            
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F7=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('61');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
                --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F8=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('62');
            end if;
            
        elsif (v_map_id=73) then   --流媒体企业局数据    
            v_spcode     := c1.F1;
            v_spname     := c1.F2;
            v_servcode   := c1.F3;
            v_provcode   := c1.F4;
            v_hprovcode  := c1.F5;
            v_applytime  := c1.F6;
            v_expiretime := c1.F7;
            
            v_sour :=v_spcode||','||v_servcode;
            v_dest :=v_spname|| ','|| v_provcode||',';            
            
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F6=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('63');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
                --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F7=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('64');
            end if;    
            
        elsif (v_map_id=68) then   --Kjava企业局数据    
            v_spcode     := c1.F1;
            v_spname     := c1.F2;
            v_provcode   := c1.F3;
            v_gateway    := c1.F4;
            v_applytime  := c1.F5;
            v_expiretime := c1.F6;
            v_sour :=v_spcode;
            v_dest :='1,'|| v_spname|| ',';            
         
           if(v_applytime is null) then
               v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
               update work_datafile set F5=v_applytime where rowid=c1.rowid;
       			dbms_output.put_line('65');
           end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
                --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F6=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('66');
            end if;
            
        else
             v_result := '未找到该映射id:' || v_map_id ||'，请确认已定义';
			 dbms_output.put_line('67');
             return;
        end if;
        --通过v_note传递参数存在标志，不作要求就是0，新增1，存在2，不一致3
        if (v_spcode is not null) then            
            select count(*) into v_count from mapping_list where mapping_id=v_map_id 
                and mapping_sour=v_sour;
                --and to_date('20201021','yyyymmdd') >=applytime and (to_date('20201021','yyyymmdd') <=expiretime or expiretime is null);
            if(v_count > 0)then          
                select count(*) into v_count from mapping_list where mapping_id=v_map_id 
                   and mapping_sour=v_sour
                   and mapping_dest=v_dest
                   and to_char(applytime,'yyyymmdd')=substr(v_applytime,1,8)
                   and (decode(expiretime,null,'20370101',to_char(expiretime,'yyyymmdd'))=
                   decode(v_expiretime,null,'20370101',substr(v_expiretime,1,8)));
                   --and to_date('20201021','yyyymmdd') >=applytime and (to_date('20201021','yyyymmdd') <=expiretime or expiretime is null);
                 if(v_count > 0)then
                    v_note := '2';
					dbms_output.put_line('68');
                else       
                   v_note := '3';  
dbms_output.put_line('69');				   
                end if;
                
            else
                v_note := '1';
				dbms_output.put_line('70');
            end if;
        else
            v_note := '0';
			dbms_output.put_line('71');
        end if;
       
        --更新参数标志
        if(substr(v_note,1,1)='2')  then
            update work_datafile set status=2,notes=v_note where rowid=c1.rowid;
            v_oldnum := v_oldnum+1;
			dbms_output.put_line('72');
        else
            if(substr(v_note,1,1)='3')  then
                update work_datafile set status=3,notes=v_note where rowid=c1.rowid;
                v_diffnum := v_diffnum+1;
				dbms_output.put_line('73');
            else              
                if(substr(v_note,1)<>'0')then
                    update work_datafile set status=1,notes=v_note where rowid=c1.rowid;
                    v_newnum := v_newnum+1;
					dbms_output.put_line('74');
                end if;
            end if;
        end if;
    end loop;
    v_result := v_ret_imp||(v_newnum+v_oldnum+v_diffnum+v_delnum)
               ||v_ret_new||v_newnum||v_ret_old||v_oldnum
               ||v_ret_diff||v_diffnum||v_ret_del||v_delnum;

end;


procedure DB_MAPPING_LIST_SP(v_map_id number, v_result  in out  varchar2) as
v_count         number(8);
v_dupnum        number(8);
v_note          varchar2(200);
v_sptype        varchar2(20);
begin
    
    case 
        when v_map_id=60 then v_note:='梦网短信企业局数据-'; v_sptype:='SMS';
		dbms_output.put_line('75');
        when v_map_id=62 then v_note:='WAP企业局数据-'; v_sptype:='WAP';
        dbms_output.put_line('76');
        when v_map_id=64 then v_note:='PDA企业局数据-'; v_sptype:='PDA';
        dbms_output.put_line('77');
        when v_map_id=66 then v_note:='MMS企业局数据-'; v_sptype:='MMS';
        dbms_output.put_line('78');
        when v_map_id=68 then v_note:='KJAVA企业局数据-'; v_sptype:='KJAVA';
        dbms_output.put_line('79');
        when v_map_id=73 then v_note:='流媒体企业局数据'; v_sptype:='STREAM';
        dbms_output.put_line('80');
        when v_map_id=75 then v_note:='手机动画企业局数据'; v_sptype:='FLASH';
        dbms_output.put_line('81');
        when v_map_id=77 then v_note:='自有短信企业局数据'; v_sptype:='CM';
        dbms_output.put_line('82');
        when v_map_id=79 then v_note:='捐款短信企业局数据'; v_sptype:='PM';
        dbms_output.put_line('83');
        when v_map_id=81 then v_note:='手机邮箱企业局数据'; v_sptype:='CX';
        dbms_output.put_line('84');
        
        else 
             v_result := '未找到该映射id:' || v_map_id ||'，请确认已定义';
        dbms_output.put_line('85');
             return;
    end case;

    delete from mapping_list where mapping_id=v_map_id;
    delete from STL_SP_CODE where sp_type=v_sptype;
    
    if (v_map_id=60  or v_map_id=66 or v_map_id=79 or v_map_id=81)  then--梦网短信,mms,捐款短信(PM),手机邮箱(YX)企业局数据
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
            select a.F1||','||a.F5,  '1,'||a.F2||','||a.F3, 
            to_date(substr(a.F6,1,8),'yyyymmdd'), to_date(substr(a.F7,1,8),'yyyymmdd'),
            v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;
        --修改同步表
        
        insert into STL_SP_CODE (SP_CODE,SP_TYPE,SP_NAME,VPROV,SERV_CODE,
            STL_MODE,VALID_CYCLES,STL_LEVEL,VREGION,SERV_PHONE,
            FEE_TYPE,FEE_FUCTION,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK)
            select a.F1, v_sptype, a.F2, a.F3, a.F5,'1',1,1,'000',null,null,null, 
            to_date(substr(a.F6,1,8),'yyyymmdd'), to_date(substr(a.F7,1,8),'yyyymmdd'), null, null,
            v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;
        dbms_output.put_line('86');

    elsif (v_map_id=62 or v_map_id=64 or v_map_id=75) then	--wap,pda,flh企业局数据
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
            select a.F1,  '1,'||a.F2||',', 
            to_date(substr(a.F3,1,8),'yyyymmdd'), to_date(substr(a.F4,1,8),'yyyymmdd'),
            v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;   
        --修改同步表
            
            
        insert into STL_SP_CODE (SP_CODE,SP_TYPE,SP_NAME,VPROV,SERV_CODE,
            STL_MODE,VALID_CYCLES,STL_LEVEL,VREGION,SERV_PHONE,
            FEE_TYPE,FEE_FUCTION,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK)
            select a.F1, v_sptype, a.F2, 0, null, '1',1,1, '000',null,null,null,
            to_date(substr(a.F3,1,8),'yyyymmdd'), to_date(substr(a.F4,1,8),'yyyymmdd'), null, null,
            v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;         
    dbms_output.put_line('87');
    elsif (v_map_id=77) then   --自有短信(CM)   
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
            select a.F1||','||a.F6, '1,'||a.F2||','||a.F3||',',  
            to_date(substr(a.F7,1,8),'yyyymmdd'), to_date(substr(a.F8,1,8),'yyyymmdd'),
            v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;     
        --修改同步表
            
        insert into STL_SP_CODE (SP_CODE,SP_TYPE,SP_NAME,VPROV,SERV_CODE,
            STL_MODE,VALID_CYCLES,STL_LEVEL,VREGION,SERV_PHONE,
            FEE_TYPE,FEE_FUCTION,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK) 
            select a.F1, v_sptype, a.F2, a.F3, a.F6, '1',1,1, '000',null,null,null,
            to_date(substr(a.F7,1,8),'yyyymmdd'), to_date(substr(a.F8,1,8),'yyyymmdd'), null, null,
            v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a; 
          dbms_output.put_line('88');
    elsif (v_map_id=73) then   --流媒体企业局数据  
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
            select a.F1||','||a.F3, a.F2||','||a.F4||',',  
            to_date(substr(a.F6,1,8),'yyyymmdd'), to_date(substr(a.F7,1,8),'yyyymmdd'),
            v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;     
        --修改同步表
        insert into STL_SP_CODE (SP_CODE,SP_TYPE,SP_NAME,VPROV,SERV_CODE,
            STL_MODE,VALID_CYCLES,STL_LEVEL,VREGION,SERV_PHONE,
            FEE_TYPE,FEE_FUCTION,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK) 
            select a.F1, v_sptype, a.F2, a.F4,a.F3, '1',1,1, '000',null,null,null,
            to_date(substr(a.F6,1,8),'yyyymmdd'), to_date(substr(a.F7,1,8),'yyyymmdd'), null, null,
            v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;  
      dbms_output.put_line('89');      
    elsif (v_map_id=68) then--Kjava企业局数据   
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
            select a.F1, '1,'||a.F2||',',  
            to_date(substr(a.F5,1,8),'yyyymmdd'), to_date(substr(a.F6,1,8),'yyyymmdd'),
            v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;     
        --修改同步表
    
        insert into STL_SP_CODE (SP_CODE,SP_TYPE,SP_NAME,VPROV,SERV_CODE,
            STL_MODE,VALID_CYCLES,STL_LEVEL,VREGION,SERV_PHONE,
            FEE_TYPE,FEE_FUCTION,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK)
            select a.F1, v_sptype, a.F2, a.F3, null, '1',1,1, '000',null,null,null,
            to_date(substr(a.F5,1,8),'yyyymmdd'), to_date(substr(a.F6,1,8),'yyyymmdd'), null, null,
            v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a; 
			dbms_output.put_line('90');
    end if;

    select count(*) into v_count from WORK_DATAFILE;
    --删除重复纪录      
    delete from mapping_list a where 
        a.rowid!= (select max(b.rowid) from mapping_list b  
        where a.mapping_id=b.mapping_id and a.mapping_sour=b.mapping_sour and a.mapping_dest=b.mapping_dest
        and a.applytime=b.applytime and a.expiretime=b.expiretime) and a.mapping_id=v_map_id;
    
    v_dupnum :=sql%rowcount;
                     
    v_result := v_ret_imp||v_count||v_ret_update||(v_count-v_dupnum)
               ||v_ret_dup||v_dupnum ;
              
end;







procedure PROC_MAPPING_LIST_OPER(v_map_id number, v_result  in out  varchar2) as
v_spcode        varchar2(100);
v_channel       varchar2(100);
v_opercode      varchar2(100);
v_opername      varchar2(100);
v_feetype       varchar2(10);
v_fee           varchar2(20);
v_applytime     varchar2(100);
v_expiretime    varchar2(100);
v_rate          varchar2(20);
v_download      varchar2(20);

            
v_count         number(6) := 0;
v_delnum        number(6) := 0;
v_newnum        number(6) := 0;
v_oldnum        number(6) := 0;
v_diffnum       number(6) := 0;

begin
    for c1 in(select F1,F2,F3,F4,F5,F6,F7,F8,F9,rowid from WORK_DATAFILE where STATUS=0 order by ID)
    loop
        if (v_map_id=61 or v_map_id=63 or v_map_id=67 or v_map_id=78)  then--梦网短信,wap,mms,自有短信(CM)业务局数据
            v_spcode     := c1.F1;
            v_channel    := c1.F2;
            v_opercode   := c1.F3;
            v_opername   := c1.F4;
            v_feetype    := c1.F5;
            v_fee        := c1.F6;
            v_applytime  := c1.F7;
            v_expiretime := c1.F8;
            v_rate   := c1.F9;
        
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F7=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('91');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
            --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F8=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('92');
            end if;
            --修正v_feetype为双字符串
            if(v_feetype is not null)then
                v_feetype := lpad(v_feetype,2,'0');
                update work_datafile set F5=v_feetype where rowid=c1.rowid;
				dbms_output.put_line('93');
            end if;
            
        elsif (v_map_id=65 or  v_map_id=80 or v_map_id=82) then	--pda,捐款短信(PM),手机邮箱(YX)业务局数据
            v_spcode     := c1.F1;
            v_opercode   := c1.F2;
            v_opername   := c1.F3;
            v_feetype    := c1.F4;
            v_fee        := c1.F5;
            v_applytime  := c1.F6;
            v_expiretime := c1.F7;
                       
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F6=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('94');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
               --v_expiretime := '20370101000000';
               v_expiretime := null;
               update work_datafile set F7=v_expiretime where rowid=c1.rowid;
			   dbms_output.put_line('95');
            end if;
            --修正v_feetype为双字符串
            if(v_feetype is not null)then
                v_feetype := lpad(v_feetype,2,'0');
                update work_datafile set F4=v_feetype where rowid=c1.rowid;
				dbms_output.put_line('96');
            end if;
                
        elsif (v_map_id=69) then             --Kjava业务局数据    
            v_spcode     := c1.F1;
            v_channel    := c1.F2;
            v_opercode    := c1.F3;
            v_opername     := c1.F4;
            v_feetype   := c1.F5;
            v_fee        := c1.F6;
            v_download   := c1.F7;
            v_applytime  := c1.F8;
            v_expiretime := c1.F9;
                
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F8=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('97');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
                --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F9=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('98');
            end if;
            --修正v_feetype为双字符串
            if(v_feetype is not null)then
                v_feetype := lpad(v_feetype,2,'0');
                update work_datafile set F5=v_feetype where rowid=c1.rowid;
				dbms_output.put_line('99');
            end if;    
        
        elsif (v_map_id=74) then             --流媒体业务局数据    
            v_spcode     := c1.F1;
            v_opercode   := c1.F2;
            v_opername   := c1.F3;
            v_feetype    := c1.F4;
            v_fee        := c1.F5;
            v_rate       := c1.F6;
            v_applytime  := c1.F7;
            v_expiretime := c1.F8;
                
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F7=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('100');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
                --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F8=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('101');
            end if;
            --修正v_feetype为双字符串
            if(v_feetype is not null)then
                v_feetype := lpad(v_feetype,2,'0');
                update work_datafile set F4=v_feetype where rowid=c1.rowid;
				dbms_output.put_line('102');
            end if;    
        
        elsif (v_map_id=76) then	--手机动画业务局数据
            v_spcode     := c1.F1;
            v_channel    := c1.F2;
            v_opercode   := c1.F3;
            v_opername   := c1.F4;
            v_feetype    := c1.F5;
            v_fee        := c1.F6;
            v_rate       := c1.F7;
            v_applytime  := c1.F8;
            v_expiretime := c1.F9;
            
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F8=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('103');
            end if;
            if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
            --v_expiretime := '20370101000000';
                v_expiretime := null;
                update work_datafile set F9=v_expiretime where rowid=c1.rowid;
				dbms_output.put_line('104');
            end if;
            --修正v_feetype为双字符串
            if(v_feetype is not null)then
                v_feetype := lpad(v_feetype,2,'0');
                update work_datafile set F5=v_feetype where rowid=c1.rowid;
				dbms_output.put_line('105');
            end if;
          
        else
            v_result := '未找到该映射id:' || v_map_id ||'，请确认已定义';
			dbms_output.put_line('106');
            return;
                
        end if;
        
        --通过v_note传递参数存在标志，不作要求就是0，新增1，存在2，不一致3
        if (v_spcode is not null) then            
            select count(*) into v_count from mapping_list where mapping_id=v_map_id 
                and mapping_sour=v_spcode || ',' || v_opercode;
                --and to_date('20201021','yyyymmdd') >=applytime and (to_date('20201021','yyyymmdd') <=expiretime or expiretime is null);
            if(v_count > 0)then          
                select count(*) into v_count from mapping_list where mapping_id=v_map_id 
                   and mapping_sour=v_spcode || ',' || v_opercode
                   and mapping_dest=v_opername || ',' || v_feetype || '.' || v_fee || ',1|85|1'
                   and to_char(applytime,'yyyymmdd')=substr(v_applytime,1,8)
                   and (decode(expiretime,null,'20370101',to_char(expiretime,'yyyymmdd'))=
                   decode(v_expiretime,null,'20370101',substr(v_expiretime,1,8)));
                   --and to_date('20201021','yyyymmdd') >=applytime and (to_date('20201021','yyyymmdd') <=expiretime or expiretime is null);
                
                if(v_count > 0)then
                    v_note := '2';
					dbms_output.put_line('107');
                else       
                   v_note := '3';  
dbms_output.put_line('108');				   
                end if;
                
            else
                v_note := '1';
				dbms_output.put_line('109');
            end if;
        else
            v_note := '0';
			dbms_output.put_line('110');
        end if;
       
        --更新参数标志
        if(substr(v_note,1,1)='2')  then
            update work_datafile set status=2,notes=v_note where rowid=c1.rowid;
            v_oldnum := v_oldnum+1;
			dbms_output.put_line('111');
        else
            if(substr(v_note,1,1)='3')  then
                update work_datafile set status=3,notes=v_note where rowid=c1.rowid;
                v_diffnum := v_diffnum+1;
				dbms_output.put_line('112');
            else              
                if(substr(v_note,1)<>'0')then
                    update work_datafile set status=1,notes=v_note where rowid=c1.rowid;
                    v_newnum := v_newnum+1;
					dbms_output.put_line('113');
                end if;
            end if;
        end if;
    end loop;
    v_result := v_ret_imp||(v_newnum+v_oldnum+v_diffnum+v_delnum)
               ||v_ret_new||v_newnum||v_ret_old||v_oldnum
               ||v_ret_diff||v_diffnum||v_ret_del||v_delnum;

end;

procedure DB_MAPPING_LIST_OPER(v_map_id number, v_result  in out  varchar2) as
v_count         number(8);
v_dupnum        number(8);
v_note          varchar2(200);
v_sptype        varchar2(20);
begin
    
    delete from mapping_list where mapping_id=v_map_id;
    case 
        when v_map_id=61 then v_note:='梦网短信业务局数据-'; v_sptype:='SMS';
		dbms_output.put_line('114');
        when v_map_id=63 then v_note:='WAP业务局数据-'; v_sptype:='WAP';
        dbms_output.put_line('115');
        when v_map_id=65 then v_note:='PDA业务局数据-'; v_sptype:='PDA';
        dbms_output.put_line('116');
        when v_map_id=67 then v_note:='MMS业务局数据-'; v_sptype:='MMS';
        dbms_output.put_line('117');
        when v_map_id=69 then v_note:='KJAVA业务局数据-'; v_sptype:='KJAVA';
        dbms_output.put_line('118');
        when v_map_id=74 then v_note:='流媒体企业局数据'; v_sptype:='STREAM';
        dbms_output.put_line('119');
        when v_map_id=76 then v_note:='手机动画企业局数据'; v_sptype:='FLASH';
        dbms_output.put_line('120');
        when v_map_id=78 then v_note:='自有短信企业局数据'; v_sptype:='CM';
        dbms_output.put_line('121');
        when v_map_id=80 then v_note:='捐款短信企业局数据'; v_sptype:='PM';
        dbms_output.put_line('122');
        when v_map_id=82 then v_note:='手机邮箱企业局数据'; v_sptype:='CX';
        dbms_output.put_line('123');
        else 
             v_result := '未找到该映射id:' || v_map_id ||'，请确认已定义';
         dbms_output.put_line('124');
            return;
    end case;
    if (v_map_id=61 or v_map_id=63 or v_map_id=67 or v_map_id=78)  then--梦网短信,wap,mms,自有短信(CM)业务局数据
       
        /*
        delete  from work_datafile  where F8 is not null and F8 <to_date('20201021','yyyymmdd') 
        v_dupnum :=sql%rowcount;
        */
        delete  from work_datafile a where 
        a.rowid!= (select max(b.rowid) from work_datafile b  
        where a.F1=b.F1 and a.F3=b.F3 
        and a.F5=b.F5 and a.F7=b.F7)  ;
        
        v_dupnum :=sql%rowcount;
                
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
        select a.F1||','||a.F3,  a.F4||','||a.F5||'.'||a.F6||',1|85|1', 
        to_date(substr(a.F7,1,8),'yyyymmdd'), to_date(substr(a.F8,1,8),'yyyymmdd'),
        v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;
        --修改同步表
        delete from STL_SP_OPER where sp_type=v_sptype;
        
        insert into STL_SP_OPER(SP_CODE,SP_TYPE,OPER_CODE,OPER_NAME,CHRG_TYPE,FEE_RATE,
        SP_RATIO,FILTER_FLAG,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK) 
        select a.F1, v_sptype, a.F3, a.F4, a.F5, a.F6, a.F9, null, 
        to_date(substr(a.F7,1,8),'yyyymmdd'), to_date(substr(a.F8,1,8),'yyyymmdd'), null, null,
        v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;
dbms_output.put_line('125');
    elsif (v_map_id=65 or  v_map_id=80 or v_map_id=82) then	--pda,捐款短信(PM),手机邮箱(YX)业务局数据
        delete  from work_datafile a where 
        a.rowid!= (select max(b.rowid) from work_datafile b  
        where a.F1=b.F1 and a.F2=b.F2 and a.F3=b.F3 
        and a.F4=b.F4 and a.F5=b.F5  and a.F6=b.F6)  ;
        v_dupnum :=sql%rowcount;
        
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
        select a.F1||','||a.F2,  a.F3||','||a.F4||'.'||a.F5||',1|85|1', 
        to_date(substr(a.F6,1,8),'yyyymmdd'), to_date(substr(a.F7,1,8),'yyyymmdd'),
        v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;   
        --修改同步表
        delete from STL_SP_OPER where sp_type=v_sptype;
        insert into STL_SP_OPER select a.F1, v_sptype, a.F2, a.F3, a.F4, a.F5, 85, null, 
        to_date(substr(a.F6,1,8),'yyyymmdd'), to_date(substr(a.F7,1,8),'yyyymmdd'), null, null,
        v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;               
         dbms_output.put_line('126');   
    elsif (v_map_id=69) then             --Kjava业务局数据    
        delete  from work_datafile a where 
        a.rowid!= (select max(b.rowid) from work_datafile b  
        where a.F1=b.F1 and a.F3=b.F3 
        and a.F4=b.F4 and a.F5=b.F5  and a.F6=b.F6 and a.F8=b.F8)  ;
        v_dupnum :=sql%rowcount;
          
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
        select a.F1||','||a.F3,  a.F4||','||a.F5||'.'||a.F6||',1|85|1', 
        to_date(substr(a.F8,1,8),'yyyymmdd'), to_date(substr(a.F9,1,8),'yyyymmdd'),
        v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;     
        --修改同步表
        delete from STL_SP_OPER where sp_type=v_sptype;
        insert into STL_SP_OPER select a.F1, v_sptype, a.F3, a.F4, a.F5, a.F6, 85, null, 
        to_date(substr(a.F8,1,8),'yyyymmdd'), to_date(substr(a.F9,1,8),'yyyymmdd'), null, null,
        v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;      
dbms_output.put_line('127');
    elsif (v_map_id=74) then	--流媒体业务局数据    
        delete  from work_datafile a where 
        a.rowid!= (select max(b.rowid) from work_datafile b  
        where a.F1=b.F1 and a.F2=b.F2 and a.F3=b.F3 
        and a.F4=b.F4 and a.F5=b.F5  and a.F6=b.F6 and a.F7=b.F7)  ;
        v_dupnum :=sql%rowcount;
         
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
        select a.F1||','||a.F2,  a.F3||',,'||substr(a.F4,2,1)||a.F5, 
        to_date(substr(a.F7,1,8),'yyyymmdd'), to_date(substr(a.F8,1,8),'yyyymmdd'),
        v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;     
        --修改同步表
        delete from STL_SP_OPER where sp_type=v_sptype;
        insert into STL_SP_OPER select a.F1, v_sptype, a.F2, a.F3, a.F4, a.F5, a.F6, null, 
        to_date(substr(a.F7,1,8),'yyyymmdd'), to_date(substr(a.F8,1,8),'yyyymmdd'), null, null,
        v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;      
dbms_output.put_line('128');
    elsif (v_map_id=76) then	--手机动画业务局数据   
        delete  from work_datafile a where 
        a.rowid!= (select max(b.rowid) from work_datafile b  
        where a.F1=b.F1 and a.F3=b.F3 
        and a.F4=b.F4 and a.F5=b.F5  and a.F6=b.F6 and a.F8=b.F8)  ;
        v_dupnum :=sql%rowcount; 
        
        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
        select a.F1||','||a.F3,  a.F4||','||a.F5||'.'||a.F6||',1|85|1', 
        to_date(substr(a.F8,1,8),'yyyymmdd'), to_date(substr(a.F9,1,8),'yyyymmdd'),
        v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;     
        --修改同步表
        delete from STL_SP_OPER where sp_type=v_sptype;
        insert into STL_SP_OPER select a.F1, v_sptype, a.F3, a.F4, a.F5, a.F6, a.F7, null, 
        to_date(substr(a.F8,1,8),'yyyymmdd'), to_date(substr(a.F9,1,8),'yyyymmdd'), null, null,
        v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss') from WORK_DATAFILE a;      
dbms_output.put_line('129');
    end if;
    
    select count(*) into v_count from WORK_DATAFILE;
   /* --删除重复纪录      
    delete from mapping_list a where 
        a.rowid!= (select max(b.rowid) from mapping_list b  
        where a.mapping_id=b.mapping_id and a.mapping_sour=b.mapping_sour and a.mapping_dest=b.mapping_dest
        and a.applytime=b.applytime and a.expiretime=b.expiretime) and a.mapping_id=v_map_id;
    
    v_dupnum :=sql%rowcount;
                     */
    v_result := v_ret_imp||(v_count+v_dupnum)||v_ret_update||(v_count)
               ||v_ret_dup||v_dupnum ;
              
end;

/******************************************************************************
******************************************************************************/
procedure PROC_MAPPING_LIST_SNSP(v_map_id number, v_result  in out  varchar2) as
v_spcode        varchar2(100);
v_spname        varchar2(100);
v_servcode      varchar2(100);
v_servtype      varchar2(100);
v_applytime     varchar2(100);
v_expiretime    varchar2(100);
v_infofeemode   varchar2(100);
v_billcycle     varchar2(100);
v_feemode       varchar2(100);
v_feefuction    varchar2(100);
v_city          varchar2(100);

v_rate          varchar2(10);
v_feetype       varchar2(10);
v_fee           varchar2(20);
v_opercode      varchar2(100);
v_opername      varchar2(100);
v_selience_judge  varchar2(20);
v_hige_judge      varchar2(20);
v_sour          varchar2(200);
v_dest          varchar2(200);

v_sptype        varchar2(20);
v_count         number(6) := 0;
v_delnum        number(6) := 0;
v_newnum        number(6) := 0;
v_oldnum        number(6) := 0;
v_diffnum       number(6) := 0;

begin
    for c1 in(select F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,rowid from WORK_DATAFILE where STATUS=0 order by ID)
    loop
        if (v_map_id=83 or v_map_id=84 or v_map_id=85)  then--省内梦网短信,mms,wap企业局数据
            v_spcode     := c1.F2;
            v_spname     := c1.F3;
            v_servcode   := c1.F4;
            v_servtype   := c1.F5;
            v_infofeemode    := c1.F6;
            v_billcycle   := c1.F7;
            v_feemode  := c1.F8;
            v_feefuction := c1.F9;
            v_city       :=c1.F10;
            v_applytime      :=c1.F11;
            
            v_sour :=v_spcode || ',' || v_servcode;
            v_dest :='1,'|| v_spname|| ','|| v_city;            
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F11=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('130');
            end if;
            v_expiretime :=null;
           
           -----------
        elsif (v_map_id=-83 or v_map_id=-84 or v_map_id=-85)  then--省内梦网短信,mms,wap业务局数据
            case     
                when v_map_id=-83 then v_note:='省内梦网短信企业局数据'; v_sptype:='SNSMS';
				dbms_output.put_line('131');
                when v_map_id=-84 then v_note:='省内梦网彩信企业局数据'; v_sptype:='SNMMS'; 
				dbms_output.put_line('132');
                when v_map_id=-85 then v_note:='省内WAP企业局数据'; v_sptype:='SNWAP'; 
				dbms_output.put_line('133');
            end case;

            v_spcode     := c1.F2;
            v_opercode   := c1.F3;
            v_opername   := c1.F4;
            v_feetype    := c1.F5;
            v_fee        := c1.F6;
            v_rate       := c1.F7;
            v_selience_judge  := c1.F8;
            v_hige_judge := c1.F9;
            v_applytime      :=c1.F10;
            
            if(v_applytime is null) then
                v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
                update work_datafile set F10=v_applytime where rowid=c1.rowid;
				dbms_output.put_line('134');
            end if;
            v_expiretime :=null;       
        -----------         
        else
             v_result := '未找到该映射id:' || v_map_id ||'，请确认已定义';
			 dbms_output.put_line('135');
             return;
        end if;
        
        --通过v_note传递参数存在标志，不作要求就是0，新增1，存在2，不一致3
        if( v_map_id >0) then     --企业局数据
            if (v_spcode is not null) then            
                select count(*) into v_count from mapping_list where mapping_id=v_map_id 
                    and mapping_sour=v_sour
                    and to_date('20201021','yyyymmdd') >=applytime and (to_date('20201021','yyyymmdd') <=expiretime or expiretime is null);
                if(v_count > 0)then          
                    select count(*) into v_count from mapping_list where mapping_id=v_map_id 
                        and mapping_sour=v_sour
                        and mapping_dest=v_dest
                        and to_char(applytime,'yyyymmdd')=substr(v_applytime,1,8)
                        and (decode(expiretime,null,'20370101',to_char(expiretime,'yyyymmdd'))=
                        decode(v_expiretime,null,'20370101',substr(v_expiretime,1,8)));
                    if(v_count > 0)then
                        v_note := '2';
						dbms_output.put_line('136');
                    else       
                       v_note := '3';    
					   dbms_output.put_line('137');
                    end if;
                else
                    v_note := '1';
					dbms_output.put_line('138');
                end if;
            else
                v_note := '0';
				dbms_output.put_line('139');
            end if;
        else  --业务局数据
            if (v_spcode is not null) then
                select count(*) into v_count from STL_SP_OPER where sp_type=v_sptype 
                    and sp_code=v_spcode and nvl(oper_code,0)=nvl(v_opercode,0)
                    and CHRG_TYPE=v_feetype 
                    and to_date('20201021','yyyymmdd') >=eff_date and (to_date('20201021','yyyymmdd') <=exp_date or exp_date is null);
                if(v_count > 0)then          
                    select count(*) into v_count from STL_SP_OPER where sp_type=v_sptype 
                        and sp_code=v_spcode and nvl(oper_code,0)=nvl(v_opercode,0)
                        and CHRG_TYPE=v_feetype
                        and to_char(eff_date,'yyyymmdd')=substr(v_applytime,1,8)
                        and (decode(exp_date,null,'20370101',to_char(exp_date,'yyyymmdd'))=
                        decode(v_expiretime,null,'20370101',substr(v_expiretime,1,8)));
                    if(v_count > 0)then
                        v_note := '2';
						dbms_output.put_line('140');
                    else       
                       v_note := '3';  
dbms_output.put_line('141');					   
                    end if;
                else
                    v_note := '1';
					dbms_output.put_line('142');
                end if;
            else
                v_note := '0';
				dbms_output.put_line('143');
            end if;
        end if;
        
        --更新参数标志
        if(substr(v_note,1,1)='2')  then
            update work_datafile set status=2,notes=v_note where rowid=c1.rowid;
            v_oldnum := v_oldnum+1;
			dbms_output.put_line('144');
        else
            if(substr(v_note,1,1)='3')  then
                update work_datafile set status=3,notes=v_note where rowid=c1.rowid;
                v_diffnum := v_diffnum+1;
				dbms_output.put_line('145');
            else              
                if(substr(v_note,1)<>'0')then
                    update work_datafile set status=1,notes=v_note where rowid=c1.rowid;
                    v_newnum := v_newnum+1;
					dbms_output.put_line('146');
                end if;
            end if;
        end if;
    end loop;
    v_result := v_ret_imp||(v_newnum+v_oldnum+v_diffnum+v_delnum)
               ||v_ret_new||v_newnum||v_ret_old||v_oldnum
               ||v_ret_diff||v_diffnum||v_ret_del||v_delnum;

end;
/*******************************************************
********************************************************/
procedure DB_MAPPING_LIST_SNSP(v_map_id number, v_result  in out  varchar2) as
v_count         number(8);
v_oldnum        number(8);
v_notexistnum   number(8);
v_insert        number(8);
v_update        number(8);
v_delete        number(8);
v_note          varchar2(200);
v_sptype        varchar2(20);
v_sour          varchar2(200);
v_dest          varchar2(200);
v_num           number(8);
begin
    
    case     
        when v_map_id=83 then v_note:='省内梦网短信企业局数据'; v_sptype:='SNSMS';
		dbms_output.put_line('147');
        when v_map_id=84 then v_note:='省内梦网彩信企业局数据'; v_sptype:='SNMMS'; 
        dbms_output.put_line('148');
        when v_map_id=85 then v_note:='省内WAP企业局数据'; v_sptype:='SNWAP'; 
        dbms_output.put_line('149');
        when v_map_id=-83 then v_note:='省内梦网短信业务局数据'; v_sptype:='SNSMS';
        dbms_output.put_line('150');
        when v_map_id=-84 then v_note:='省内梦网彩信业务局数据'; v_sptype:='SNMMS'; 
        dbms_output.put_line('151');
        when v_map_id=-85 then v_note:='省内WAP业务局数据'; v_sptype:='SNWAP'; 
        dbms_output.put_line('152');
        else 
             v_result := '未找到该映射id:' || v_map_id ||'，请确认已定义';
        dbms_output.put_line('153');
             return;
    end case;
    
    v_count := 0;
    v_oldnum :=0;
    v_notexistnum :=0;
    v_insert :=0;
    v_update :=0;
    v_delete :=0;
                  
    if (v_map_id=83  or v_map_id=84 or v_map_id=85)  then--省内梦网短信,省内mms,省内wap企业局数据
        for c2 in(select F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,STATUS,rowid from WORK_DATAFILE  order by ID)
        loop   
            v_count := v_count + 1;
            if(c2.F1='新增') then
                if(c2.STATUS<>1) then
                    v_oldnum := v_oldnum + 1;
					dbms_output.put_line('154');
                else 
                    v_insert := v_insert + 1;
                    insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
                    values( c2.F2||','||c2.F4,  '1,'||c2.F3||','||c2.F10, 
                    to_date(substr(c2.F11,1,8),'yyyymmdd'), null,
                    v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss')) ;
                --修改同步表
                /*
                    insert into STL_SP_CODE values (
                    c2.F2, v_sptype, c2.F3, c2.F10, c2.F4, 
                    c2.F6, c2.F7, c2.F5, null, c2.F8, c2.F9, 
                    to_date(substr(c2.F11,1,8),'yyyymmdd'), null, null, null,
                    v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss')) ;
                    */
                    insert into STL_SP_CODE (SP_CODE,SP_TYPE,SP_NAME,VPROV,SERV_CODE,
                    STL_MODE,VALID_CYCLES,STL_LEVEL,VREGION,SERV_PHONE,
                    FEE_TYPE,FEE_FUCTION,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK)
                    values (c2.F2, v_sptype, c2.F3, c2.F10, c2.F4, 
                    c2.F6, c2.F7, c2.F5, c2.F10, null, c2.F8, c2.F9, 
                    to_date(substr(c2.F11,1,8),'yyyymmdd'), null, null, null,
                    v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss'));
                    dbms_output.put_line('155');
                end if;               
            elsif (c2.F1='修改') then
                if(c2.STATUS<>2) then
                    v_notexistnum := v_notexistnum + 1;
					dbms_output.put_line('156');
                else                 
                    v_update := v_update + 1;
                    
                    v_sour := c2.F2||','||c2.F4;
                    v_dest := '1,'||c2.F3||','||c2.F10;
                    /*如果mapping_list里的数据没改变，就不需要修改mapping_list*/
                    select count(*) into v_num from mapping_list where mapping_id=v_map_id and 
                        mapping_sour=v_sour and mapping_dest=v_dest;
                    if( v_num =0) then 
                        update mapping_list set expiretime=to_date(substr(c2.F11,1,8),'yyyymmdd')
                        where mapping_sour=c2.F2||','||c2.F4 and (expiretime >to_date('20201021','yyyymmdd') or expiretime is null);
                    
                        insert into mapping_list (MAPPING_SOUR,MAPPING_DEST,APPLYTIME,EXPIRETIME,MAPPING_ID,NOTE)
                        values( c2.F2||','||c2.F4,  '1,'||c2.F3||','||c2.F10, 
                        to_date(substr(c2.F11,1,8),'yyyymmdd'), null,
                        v_map_id, v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss')) ;    
dbms_output.put_line('157');						
                    end if;
                    
                    --修改同步表
                    update STL_SP_CODE set exp_date=to_date(substr(c2.F11,1,8),'yyyymmdd')
                    where sp_code=c2.F2 and sp_type=v_sptype and 
                    (exp_date >to_date('20201021','yyyymmdd') or exp_date is null);
                    /*
                    insert into STL_SP_CODE values (
                    c2.F2, v_sptype, c2.F3, c2.F10, c2.F4, 
                    c2.F6, c2.F7, c2.F5, null, c2.F8, c2.F9, 
                    to_date(substr(c2.F11,1,8),'yyyymmdd'), null, null, null,
                    v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss')) ;
                    */
                    insert into STL_SP_CODE (SP_CODE,SP_TYPE,SP_NAME,VPROV,SERV_CODE,
                    STL_MODE,VALID_CYCLES,STL_LEVEL,VREGION,SERV_PHONE,
                    FEE_TYPE,FEE_FUCTION,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK)
                    values (c2.F2, v_sptype, c2.F3, c2.F10, c2.F4, 
                    c2.F6, c2.F7, c2.F5, c2.F10,null, c2.F8, c2.F9, 
                    to_date(substr(c2.F11,1,8),'yyyymmdd'), null, null, null,
                    v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss')) ;
                    dbms_output.put_line('158');
                end if;
            elsif (c2.F1='删除') then
                if(c2.STATUS<2) then
                    v_oldnum := v_oldnum + 1;
					dbms_output.put_line('159');
                else 
                    v_delete := v_delete + 1;
                    update mapping_list set expiretime=to_date(substr(c2.F11,1,8),'yyyymmdd')
                    where mapping_sour=c2.F2||','||c2.F4 and (expiretime >to_date('20201021','yyyymmdd') or expiretime is null);
                    --修改同步表
                    update STL_SP_CODE set exp_date=to_date(substr(c2.F11,1,8),'yyyymmdd')
                    where sp_code=c2.F2 and sp_type=v_sptype and 
                    (exp_date >to_date('20201021','yyyymmdd') or exp_date is null);
					dbms_output.put_line('160');
                end if;
            else
                v_result := '非法的操作类型:' || c2.F1 ||'，目前只支持''新增'',''修改'',''删除''';
				dbms_output.put_line('161');
                return;    
            end if;
            
        end loop;
/*
对于省内业务局数据中，如果是：
1。业务代码为空，且计费类型为03（包月），且是否核减沉默用户为0（不参与），则插入该spcode到集合81（不参与梦网沉默用户稽核的SP）
2。业务代码不为空，且计费类型为03（包月），且是否核减沉默用户为0（不参与），则插入该opercode到集合82（不参与稽核梦网业务代码）
*/  
    elsif (v_map_id=-83  or v_map_id=-84 or v_map_id=-85)  then--省内梦网短信,省内mms,省内wap业务局数据
        for c2 in(select F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,STATUS,rowid from WORK_DATAFILE  order by ID)
        loop   

            v_count := v_count + 1;
            if(c2.F1='新增') then 
                if(c2.STATUS<>1) then
                    v_oldnum := v_oldnum + 1;
					dbms_output.put_line('162');
                else 
                    v_insert := v_insert + 1;
                    insert into STL_SP_OPER (SP_CODE,SP_TYPE,OPER_CODE,OPER_NAME,CHRG_TYPE,FEE_RATE,		
                    SP_RATIO,FILTER_FLAG,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK)
                    values( c2.F2,v_sptype,c2.F3,c2.F4, c2.F5,c2.F6, 
                    c2.F7, c2.F8||c2.F9||'00000000000000', 
                    to_date(substr(c2.F10,1,8),'yyyymmdd'), null, null,null,
                    v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss')) ;
                    if(c2.F3 is null and c2.F5='03' and c2.F8='0') then
                        insert into set_value (set_id,applytime,expiretime,value,note)
                        values (81,to_date(substr(c2.F10,1,8),'yyyymmdd'),null,c2.F2,c2.F2||',不参与梦网沉默用户稽核');
						dbms_output.put_line('163');
                    end if;
                    if(c2.F3 is not null and c2.F5='03' and c2.F8='0') then
                        insert into set_value (set_id,applytime,expiretime,value,note)
                        values (82,to_date(substr(c2.F10,1,8),'yyyymmdd'),null,c2.F2||','||c2.F3,
                        c2.F2||','||c2.F3||',不参与梦网沉默用户稽核');
						dbms_output.put_line('164');
                    end if;
                    
                --修改同步表
                end if;               
            elsif (c2.F1='修改') then
                if(c2.STATUS<>2) then
                    v_notexistnum := v_notexistnum + 1;
					dbms_output.put_line('165');
                else                 
                    v_update := v_update + 1;
                  
                    --修改同步表
                    update STL_SP_OPER set exp_date=to_date(substr(c2.F10,1,8),'yyyymmdd')
                    where sp_code=c2.F2 and sp_type=v_sptype and CHRG_TYPE=c2.F5 and
                    (exp_date >to_date('20201021','yyyymmdd') or exp_date is null);
                    
                    insert into STL_SP_OPER (SP_CODE,SP_TYPE,OPER_CODE,OPER_NAME,CHRG_TYPE,FEE_RATE,		
                    SP_RATIO,FILTER_FLAG,EFF_DATE,EXP_DATE,CYCLE1,CYCLE2,REMARK)
                    values( c2.F2,v_sptype,c2.F3,c2.F4, c2.F5,c2.F6, 
                    c2.F7, c2.F8||c2.F9||'00000000000000', 
                    to_date(substr(c2.F10,1,8),'yyyymmdd'), null, null,null,
                    v_note||to_char(to_date('20201021','yyyymmdd'),'yyyymmddhh24miss')) ;
					dbms_output.put_line('166');
                end if;
            elsif (c2.F1='删除') then
                if(c2.STATUS<>2) then
                    v_oldnum := v_oldnum + 1;
					dbms_output.put_line('167');
                else 
                    v_delete := v_delete + 1;
                    --修改同步表
                    update STL_SP_OPER set exp_date=to_date(substr(c2.F10,1,8),'yyyymmdd')
                    where sp_code=c2.F2 and sp_type=v_sptype and CHRG_TYPE=c2.F5  and
                    (exp_date >to_date('20201021','yyyymmdd') or exp_date is null);
                    
                    if(c2.F3 is null and c2.F5='03' and c2.F8='0') then
                        update set_value set expiretime=to_date(substr(c2.F10,1,8),'yyyymmdd')
                        where  set_id=81 and value=c2.F2;
						dbms_output.put_line('168');
                    end if;
                    if(c2.F3 is not null and c2.F5='03' and c2.F8='0') then
                        update set_value set expiretime=to_date(substr(c2.F10,1,8),'yyyymmdd')
                        where  set_id=82 and value=c2.F2||','||c2.F3;
						dbms_output.put_line('169');
                    end if;
                    
                end if;
            else
                v_result := '非法的操作类型:' || c2.F1 ||'，目前只支持''新增'',''修改'',''删除''';
				dbms_output.put_line('170');
                return;    
            end if;
        end loop;

    end if;

    v_result := v_ret_ask||v_count ||v_ret_done||(v_insert+v_update+v_delete)
    ||v_ret_new||v_insert ||v_ret_update||v_update ||v_ret_delete||v_delete
    || v_ret_error ||(v_count-v_insert-v_update-v_delete)
   ;
              
end;


procedure PROC_HIGHCOSTEXCHANGERATE(v_result  in out  varchar2) as
v_imsi      varchar2(100);
v_money     varchar2(100);
v_note      varchar2(200);
v_unit      varchar2(100);
v_managecode    varchar2(100);
v_applytime     varchar2(100);
v_expiretime    varchar2(100);

v_count         number(6) := 0;
v_delnum        number(6) := 0;
v_newnum        number(6) := 0;
v_oldnum        number(6) := 0;
v_diffnum       number(6) := 0;

begin
    for c1 in(select F1,F2,F3,F4,F5,F6,F7,rowid from WORK_DATAFILE where STATUS=0 order by ID)
    loop
        v_managecode     := c1.F1;
        v_imsi     := c1.F2;
        v_note   := c1.F3;
        v_money    := c1.F4;
        v_unit   := c1.F5;
        v_applytime  := c1.F6;
        v_expiretime := c1.F7;
        if(v_applytime is null) then
            v_applytime := to_char(to_date('20201021','yyyymmdd'),'yyyy-mm-dd hh24:mi:ss');
            update work_datafile set F6=v_applytime where rowid=c1.rowid;
			dbms_output.put_line('171');
        end if;
        if((v_expiretime is null) or (to_date(v_expiretime,'yyyy-mm-dd hh24:mi:ss')>to_date('2037-01-01','yyyy-mm-dd'))) then
            v_expiretime := '2037-01-01 00:00:00';
            update work_datafile set F7=v_expiretime where rowid=c1.rowid;
			dbms_output.put_line('172');
        end if;
        
          
        --通过v_note传递参数存在标志，不作要求就是0，新增1，存在2，不一致3
        if (v_imsi is not null) then            
            select count(*) into v_count from highcostexchangerate where imsinum=v_imsi 
                and to_date('20201021','yyyymmdd') >=starttime and (to_date('20201021','yyyymmdd') <=endtime or endtime is null);
            if(v_count > 0)then          
                select count(*) into v_count from highcostexchangerate where imsinum=v_imsi
                   and to_char(starttime,'yyyy-mm-dd')=substr(v_applytime,1,10)
                   and (decode(endtime,null,'2037-01-01',to_char(endtime,'yyyy-mm-dd'))=
                   decode(v_expiretime,null,'2037-01-01',substr(v_expiretime,1,10)));
                   --and to_date('20201021','yyyymmdd') >=applytime and (to_date('20201021','yyyymmdd') <=expiretime or expiretime is null);
                 if(v_count > 0)then
                    v_note := '2';
					dbms_output.put_line('173');
                else       
                   v_note := '3';    
				   dbms_output.put_line('174');
                end if;
                
            else
                v_note := '1';
				dbms_output.put_line('175');
            end if;
        else
            v_note := '0';
			dbms_output.put_line('176');
        end if;
       
        --更新参数标志
        if(substr(v_note,1,1)='2')  then
            update work_datafile set status=2,notes=v_note where rowid=c1.rowid;
            v_oldnum := v_oldnum+1;
			dbms_output.put_line('177');
        else
            if(substr(v_note,1,1)='3')  then
                update work_datafile set status=3,notes=v_note where rowid=c1.rowid;
                v_diffnum := v_diffnum+1;
				dbms_output.put_line('178');
            else              
                if(substr(v_note,1)<>'0')then
                    update work_datafile set status=1,notes=v_note where rowid=c1.rowid;
                    v_newnum := v_newnum+1;
					dbms_output.put_line('179');
                end if;
            end if;
        end if;
    end loop;
    v_result := v_ret_imp||(v_newnum+v_oldnum+v_diffnum+v_delnum)
               ||v_ret_new||v_newnum||v_ret_old||v_oldnum
               ||v_ret_diff||v_diffnum||v_ret_del||v_delnum;

end;


procedure DB_HIGHCOSTEXCHANGERATE(v_result  in out  varchar2) as
v_count         number(8);
v_dupnum        number(8);

begin

    delete from HIGHCOSTEXCHANGERATE;
    insert into HIGHCOSTEXCHANGERATE (IMSINUM,MONEY,RATE,NOTE,UNIT,MANAGECODE,STARTTIME,ENDTIME)
    select a.F2,  a.F4, 1, a.F3, a.F5, a.F1,
         to_date(a.F6,'yyyy-mm-dd hh24:mi:ss'), 
         to_date(a.F7,'yyyy-mm-dd hh24:mi:ss')
         from WORK_DATAFILE a;
       
    select count(*) into v_count from WORK_DATAFILE;
    --删除重复纪录      
    delete from HIGHCOSTEXCHANGERATE a where 
        a.rowid!= (select max(b.rowid) from HIGHCOSTEXCHANGERATE b  
        where a.imsinum=b.imsinum 
        and a.starttime=b.starttime and a.endtime=b.endtime);
    
    v_dupnum :=sql%rowcount;
                     
    v_result := v_ret_imp||v_count||v_ret_update||(v_count-v_dupnum)
               ||v_ret_dup||v_dupnum ;
			   dbms_output.put_line('180');
              
end;


end;
$$