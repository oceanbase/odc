DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_CDRBALANCE_CHECK" is

procedure PROC_BYNAME(
file_name varchar2  --话单文件名
) as
v_sect_src      varchar2(64)    := '-';      --源阶段
v_sect_dst      varchar2(64)    := '-';      --目标阶段
v_sect_tmp      varchar2(64)    := '-';      --临时阶段,用来翻转源目标阶段用
v_log_field    varchar2(64)    := '-';       --event_flag对应的表中的字段
v_update_sql    varchar2(1024)  := '-';      --动态更新统计表的SQL
v_reverse_flag  number(1)       := 0;       --翻转标志,对某阶段为源的分流数据要翻转处理(重处理,话单回收)
v_calc_factor   number(1)       := 1;       --计算因子,对于翻转运算的分流,要将其数据翻转,即*(-1)
v_flow_type     number(4)       := 0;
v_event_flag    varchar2(20)    := '-';
v_sumcount		number(14)		:= 0;
v_cmp_result	number(1)		:= 0;--标志当前文件是否平衡
begin
    begin
        --清理历史数据
       delete cdr_balancecheck where filename=file_name;
       delete cdr_balancecheck_detail where filename=file_name;
       commit;
	   dbms_output.put_line('1');
   exception when others then
        rollback;
        raise_application_error(-20101,sqlerrm);
        return;
   end;
   --遍历分拣/漫游来访文件
   for c1 in(
            select filename,recordnum,stdcount,errcount,to_number(to_char(beginprocessdate,'yyyymmdd')) proc_date from sepfilelog
                where filename=file_name
            union
            select filename,billcount recordnum,billcount stdcount,errcount,to_number(to_char(processtime,'yyyymmdd')) proc_date from visittranslog
                where filename=file_name)
    loop
        begin
            --从分拣/漫游来访日志文件得到总量数据，插入相应表中
            insert into cdr_balancecheck(procdate,filename,recordnum,stdcount,errcount)
                values(c1.proc_date, c1.filename, c1.recordnum, c1.stdcount, c1.errcount);

            --从分流日志表统计分流数据，按模块，分流ID，分流类型，事件分类
            for flow_row in (select entry_module_id,module_id,flow_id,file_event_count sumcount
                                from v_event_flow_log_sourfile where source_name=c1.filename)
            loop
                 --取源阶段/目标阶段数据,如果源为空,则取模块号;如果目标为空,则取"模块号,规则号"
                select nvl(max(flow_section),flow_row.module_id) into v_sect_src
                    from event_flow_module where module_id=flow_row.entry_module_id;
                select event_flag,flow_type,nvl(next_section,flow_row.module_id||','||flow_row.flow_id)
                    into v_event_flag,v_flow_type,v_sect_dst
                        from event_flow_def where module_id=flow_row.module_id and flow_id=flow_row.flow_id;
                --判断源阶段是否是翻转阶段
                select count(*) into v_reverse_flag from code_dict
                    where catalog='T_SECTION_REVERSE' and code=v_sect_src;
                if(v_reverse_flag > 0) then
					if(v_event_flag='CHOME') then
						select case v_sect_src
							when '90_REPROC_EMPTY' then 'EMPTY'
							when '91_REPROC_ERROR' then 'ERROR'
							when '93_REPROC_NOCHARGE' then 'WAIFS'
							else 'CHOME' end into v_event_flag from dual;
							dbms_output.put_line('2');
					end if;
                    --实现阶段翻转
                    v_sect_tmp      := v_sect_dst;
                    v_sect_dst      := v_sect_src;
                    v_sect_src      := v_sect_tmp;
                    v_calc_factor   := -1;  --计算因子取-1,实现后面统计数据取反值
                else
                    v_calc_factor  := 1;
					dbms_output.put_line('3');
                end if;
                --取日志字段,动态组织SQL,更新数据到统计表
                begin
                    select logfield into v_log_field from EVENT_FLAG_DEF where code=v_event_flag;
                    v_update_sql := 'update CDR_BALANCECHECK_DETAIL set '
                        ||v_log_field||'='||v_log_field||'+'||flow_row.sumcount*(v_calc_factor)||' '
                        ||'where procdate='''||c1.proc_date||''' and filename='''||c1.filename||''' '
                        ||'and sect_src='''||v_sect_src||''' and sect_dst='''||v_sect_dst||''' '
                        ||'and flow_type='||v_flow_type;
                    execute immediate(v_update_sql);
					dbms_output.put_line('4');
                    --如果目标记录不存在，则先增加记录再更新
                    if sql%rowcount=0 then
                         insert into CDR_BALANCECHECK_DETAIL(procdate,filename,sect_src,sect_dst,flow_type)
                                values(c1.proc_date,c1.filename, v_sect_src, v_sect_dst, v_flow_type);
                         execute immediate(v_update_sql);
						 dbms_output.put_line('5');
                    end if;
                exception when others then
                    null;
                end;
            end loop;
        	select nvl(sum(HOMECOUNT),0)+nvl(sum(EMPTYCOUNT),0)+nvl(sum(ERRORCOUNT),0)
				+nvl(sum(RMHOMECOUNT),0)+nvl(sum(RMEMPTYCOUNT),0)+nvl(sum(RMERRORCOUNT),0)
				+nvl(sum(NOCHARGECOUNT),0)+nvl(sum(REJECTCOUNT),0)
				 into v_sumcount from CDR_BALANCECHECK_DETAIL
        	where procdate=c1.proc_date and filename=c1.filename and flow_type=1
            	and sect_dst in(select code from code_dict where catalog='T_SECTION_TERMS');
			if(v_sumcount = c1.stdcount) then
				v_cmp_result := 1;
				dbms_output.put_line('6');
			else
				v_cmp_result := 0;
				dbms_output.put_line('7');
			end if;
			update cdr_balancecheck set finalcount=v_sumcount,isbalance=v_cmp_result
				where procdate=c1.proc_date and filename=c1.filename;
            commit;
        exception when others then
            rollback;
            raise_application_error(-20101,sqlerrm);
            return;
        end;
    end loop;
end PROC_BYNAME;

function  FILENAME_MATCH(file_name varchar2, service_code varchar2)
return number is
	v_tmp_pos number(4);
	v_servicetype varchar2(32);
	v_service_code varchar2(2048);
begin
	if(service_code='-1') then
	dbms_output.put_line('8');
		return 1;
	end if;
	v_service_code := service_code;
		--遍历判断配置表中是否存在匹配的业务类型
		for c1 in (select filenameformat,servicetype from servicetype_format
			where file_name like filenameformat and inuse=1 order by filenameformat desc)
		loop
			--如果匹配成功，看是否有符合的业务类型
			v_tmp_pos := instr(v_service_code,',');
			while(v_tmp_pos > 0)
			loop
				--遍历取出每个业务类型
				v_servicetype := substr(v_service_code, 1, v_tmp_pos-1);
				--匹配到业务类型，返回1
				if(v_servicetype=c1.servicetype) then
				dbms_output.put_line('9');
					return 1;
				end if;
				v_service_code := substr(v_service_code,v_tmp_pos+1);
				v_tmp_pos := instr(v_service_code,',');
			end loop;
			--如果未匹配到合适的业务类型，返回0
			dbms_output.put_line('10');
			return 0;
		end loop;
		dbms_output.put_line('11');
		return 0;
end FILENAME_MATCH;

procedure AUDIT_SHOW(
file_name varchar2, --文件名,带%模糊查询
sect_str varchar2,  --阶段串,"-1"汇总稽核,分阶段稽核串型如"'sect1','sect2','sect3'"
begindate number,   --开始日期
enddate number,      --结束日期
service_code varchar2
)
as
v_file_name varchar2(128);
v_tmp_sect  varchar2(1024) := '';
v_tmp_pos   number(4) := -1;
v_sect_param varchar2(1024) := '';
begin
  v_file_name := file_name;
  if(file_name is null) then
    v_file_name := '%';
	dbms_output.put_line('12');
  end if;
  if(trim(sect_str)='-1') then  --汇总稽核
    --遍历汇总表
    for c1 in(select * from CDR_BALANCECHECK where filename like v_file_name
		and  procdate>=begindate and procdate<=enddate
			and pkg_cdrbalance_check.FILENAME_MATCH(filename, service_code)=1)
    loop
        --针对汇总表记录,求其分阶段统计数据插入临时表
        insert into WORK_CDR_BALANCECHECK(procdate,filename,sect_code,all_count,
            HOMECOUNT,EMPTYCOUNT,ERRORCOUNT,RMHOMECOUNT,RMEMPTYCOUNT,RMERRORCOUNT,
            NOCHARGECOUNT,REJECTCOUNT,DIFFVAL)
        select c1.procdate,c1.filename,'-1',c1.stdcount,nvl(sum(HOMECOUNT),0),nvl(sum(EMPTYCOUNT),0),
			nvl(sum(ERRORCOUNT),0),nvl(sum(RMHOMECOUNT),0),nvl(sum(RMEMPTYCOUNT),0),nvl(sum(RMERRORCOUNT),0),
			nvl(sum(NOCHARGECOUNT),0),nvl(sum(REJECTCOUNT),0),c1.stdcount-(
			nvl(sum(HOMECOUNT),0)+nvl(sum(EMPTYCOUNT),0)+nvl(sum(ERRORCOUNT),0)+nvl(sum(RMHOMECOUNT),0)
			+nvl(sum(RMEMPTYCOUNT),0)+nvl(sum(RMERRORCOUNT),0)+nvl(sum(NOCHARGECOUNT),0)+nvl(sum(REJECTCOUNT),0))
		from CDR_BALANCECHECK_DETAIL
        where procdate=c1.procdate and filename=c1.filename and flow_type=1
            and sect_dst in(select code from code_dict where catalog='T_SECTION_TERMS');
			dbms_output.put_line('13');
    end loop;
  else  --分阶段稽核
    v_tmp_sect  := sect_str;
    --如果v_tmp_sect不是以','结尾,则主动添加一个,方便下面的算法
    if(substr(v_tmp_sect,length(v_tmp_sect)-1,1)<>',') then
        v_tmp_sect := v_tmp_sect||',';
		dbms_output.put_line('14');
    end if;
    v_tmp_pos   := instr(v_tmp_sect,',');
    while(v_tmp_pos > 0)
    loop
        v_sect_param := substr(v_tmp_sect,1,v_tmp_pos-1);
        --出口数据统计
        for c2 in(select procdate,filename,sect_src,nvl(sum(HOMECOUNT),0) HOMECOUNT,nvl(sum(EMPTYCOUNT),0) EMPTYCOUNT,
            nvl(sum(ERRORCOUNT),0) ERRORCOUNT,nvl(sum(RMHOMECOUNT),0) RMHOMECOUNT,nvl(sum(RMEMPTYCOUNT),0) RMEMPTYCOUNT,
            nvl(sum(RMERRORCOUNT),0) RMERRORCOUNT,nvl(sum(NOCHARGECOUNT),0) NOCHARGECOUNT,nvl(sum(REJECTCOUNT),0) REJECTCOUNT
            from CDR_BALANCECHECK_DETAIL where procdate>=begindate and procdate<=enddate and filename like v_file_name
            and sect_src=v_sect_param and flow_type=1
			and pkg_cdrbalance_check.FILENAME_MATCH(filename, service_code)=1
			group by procdate,filename,sect_src)
        loop
            insert into WORK_CDR_BALANCECHECK(procdate,filename,sect_code,all_count,
              HOMECOUNT,EMPTYCOUNT,ERRORCOUNT,RMHOMECOUNT,RMEMPTYCOUNT,RMERRORCOUNT,
              NOCHARGECOUNT,REJECTCOUNT,DIFFVAL)
            values(c2.procdate,c2.filename,c2.sect_src,0,c2.HOMECOUNT,c2.EMPTYCOUNT,c2.ERRORCOUNT,c2.RMHOMECOUNT,c2.RMEMPTYCOUNT,
            c2.RMERRORCOUNT,c2.NOCHARGECOUNT,c2.REJECTCOUNT,(c2.HOMECOUNT+c2.EMPTYCOUNT+c2.ERRORCOUNT+c2.RMHOMECOUNT+c2.RMEMPTYCOUNT+
            c2.RMERRORCOUNT+c2.NOCHARGECOUNT+c2.REJECTCOUNT)*(-1));
			dbms_output.put_line('15');
        end loop;
		--入口数据更新
		--首阶段入口特殊处理
        if(v_sect_param='01_SECTION_SEP') then
            for c3 in(select * from CDR_BALANCECHECK
                    where procdate>=begindate and procdate<=enddate and filename like v_file_name
						and pkg_cdrbalance_check.FILENAME_MATCH(filename, service_code)=1)
            loop
                update WORK_CDR_BALANCECHECK set all_count=c3.stdcount,diffval=diffval+c3.stdcount
                    where procdate=c3.procdate and filename=c3.filename and sect_code='01_SECTION_SEP';
					dbms_output.put_line('16');
            end loop;
        end if;
		--其他入口数据更新
        for c1 in(select procdate,filename,sect_dst,nvl(sum(HOMECOUNT),0)+nvl(sum(EMPTYCOUNT),0)+nvl(sum(ERRORCOUNT),0)+
        nvl(sum(RMHOMECOUNT),0)+nvl(sum(RMEMPTYCOUNT),0)+nvl(sum(RMERRORCOUNT),0)+nvl(sum(NOCHARGECOUNT),0)+nvl(sum(REJECTCOUNT),0)
        all_count from CDR_BALANCECHECK_DETAIL where filename like v_file_name and procdate>=begindate
        and procdate<=enddate and sect_dst=v_sect_param
		and pkg_cdrbalance_check.FILENAME_MATCH(filename, service_code)=1
        group by procdate,filename,sect_dst)
        loop
            update WORK_CDR_BALANCECHECK set all_count=c1.all_count,diffval=diffval+c1.all_count
                where procdate=c1.procdate and filename=c1.filename and sect_code=c1.sect_dst;
				dbms_output.put_line('17');
        end loop;
        v_tmp_sect   := substr(v_tmp_sect,v_tmp_pos+1);
        v_tmp_pos    := instr(v_tmp_sect,',');
    end loop;
  end if;
end AUDIT_SHOW;

procedure PROC_BYDATE(
proc_date varchar2  --处理日期,型如'20050601',不带时间
) as
v_sect_src      varchar2(64)    := '-';      --源阶段
v_sect_dst      varchar2(64)    := '-';      --目标阶段
v_sect_tmp      varchar2(64)    := '-';      --临时阶段,用来翻转源目标阶段用
v_log_field    varchar2(64)    := '-';       --event_flag对应的表中的字段
v_update_sql    varchar2(1024)  := '-';      --动态更新统计表的SQL
v_reverse_flag  number(1)       := 0;       --翻转标志,对某阶段为源的分流数据要翻转处理(重处理,话单回收)
v_calc_factor   number(1)       := 1;       --计算因子,对于翻转运算的分流,要将其数据翻转,即*(-1)
v_flow_type     number(4)       := 0;
v_event_flag    varchar2(20)    := '-';
v_sumcount		number(14)		:= 0;
v_cmp_result	number(1)		:= 0;	--标志当前文件是否平衡
begin
    begin
        --清理历史数据
       delete cdr_balancecheck where procdate=proc_date;
       delete cdr_balancecheck_detail where procdate=proc_date;
	   dbms_output.put_line('18');
       commit;
   exception when others then
        rollback;
        raise_application_error(-20101,sqlerrm);
        return;
   end;
   --遍历分拣/漫游来访文件
   for c1 in(
            select filename,recordnum,stdcount,errcount from sepfilelog
                where trunc(beginprocessdate)=to_date(proc_date,'yyyymmdd')
            union
            select filename,billcount recordnum,billcount stdcount,errcount from visittranslog
                where trunc(processtime)=to_date(proc_date,'yyyymmdd'))
    loop
        begin
            --从分拣/漫游来访日志文件得到总量数据，插入相应表中
            insert into cdr_balancecheck(procdate,filename,recordnum,stdcount,errcount)
                values(proc_date, c1.filename, c1.recordnum, c1.stdcount, c1.errcount);

            --从分流日志表统计分流数据，按模块，分流ID，分流类型，事件分类
            for flow_row in (select entry_module_id,module_id,flow_id,file_event_count sumcount
                                from v_event_flow_log_sourfile where source_name=c1.filename)
            loop
                 --取源阶段/目标阶段数据,如果源为空,则取模块号;如果目标为空,则取"模块号,规则号"
                select nvl(max(flow_section),flow_row.module_id) into v_sect_src
                    from event_flow_module where module_id=flow_row.entry_module_id;
                select event_flag,flow_type,nvl(next_section,flow_row.module_id||','||flow_row.flow_id)
                    into v_event_flag,v_flow_type,v_sect_dst
                        from event_flow_def where module_id=flow_row.module_id and flow_id=flow_row.flow_id;
                --判断源阶段是否是翻转阶段
                select count(*) into v_reverse_flag from code_dict
                    where catalog='T_SECTION_REVERSE' and code=v_sect_src;
                if(v_reverse_flag > 0) then
					if(v_event_flag='CHOME') then
						select case v_sect_src
							when '90_REPROC_EMPTY' then 'EMPTY'
							when '91_REPROC_ERROR' then 'ERROR'
							when '93_REPROC_NOCHARGE' then 'WAIFS'
							else 'CHOME' end into v_event_flag from dual;
							dbms_output.put_line('19');
					end if;
                    --实现阶段翻转
                    v_sect_tmp      := v_sect_dst;
                    v_sect_dst      := v_sect_src;
                    v_sect_src      := v_sect_tmp;
                    v_calc_factor   := -1;  --计算因子取-1,实现后面统计数据取反值
                else
                    v_calc_factor  := 1;
					dbms_output.put_line('20');
                end if;
                --取日志字段,动态组织SQL,更新数据到统计表
                begin
                    select logfield into v_log_field from EVENT_FLAG_DEF where code=v_event_flag;
                    v_update_sql := 'update CDR_BALANCECHECK_DETAIL set '
                        ||v_log_field||'='||v_log_field||'+'||flow_row.sumcount*(v_calc_factor)||' '
                        ||'where procdate='''||proc_date||''' and filename='''||c1.filename||''' '
                        ||'and sect_src='''||v_sect_src||''' and sect_dst='''||v_sect_dst||''' '
                        ||'and flow_type='||v_flow_type;
                    execute immediate(v_update_sql);
					dbms_output.put_line('21');
                    --如果目标记录不存在，则先增加记录再更新
                    if sql%rowcount=0 then
                         insert into CDR_BALANCECHECK_DETAIL(procdate,filename,sect_src,sect_dst,flow_type)
                                values(proc_date,c1.filename, v_sect_src, v_sect_dst, v_flow_type);
                         execute immediate(v_update_sql);
						 dbms_output.put_line('22');
                    end if;
                exception when others then
                    null;
                end;
            end loop;
        	select nvl(sum(HOMECOUNT),0)+nvl(sum(EMPTYCOUNT),0)+nvl(sum(ERRORCOUNT),0)
				+nvl(sum(RMHOMECOUNT),0)+nvl(sum(RMEMPTYCOUNT),0)+nvl(sum(RMERRORCOUNT),0)
				+nvl(sum(NOCHARGECOUNT),0)+nvl(sum(REJECTCOUNT),0)
				 into v_sumcount from CDR_BALANCECHECK_DETAIL
        	where procdate=proc_date and filename=c1.filename and flow_type=1
            	and sect_dst in(select code from code_dict where catalog='T_SECTION_TERMS');
			if(v_sumcount = c1.stdcount) then
				v_cmp_result := 1;
				dbms_output.put_line('23');
			else
				v_cmp_result := 0;
				dbms_output.put_line('24');
			end if;
			update cdr_balancecheck set finalcount=v_sumcount,isbalance=v_cmp_result
				where procdate=proc_date and filename=c1.filename;
			commit;
        exception when others then
            rollback;
            raise_application_error(-20101,sqlerrm);
            return;
        end;
    end loop;
end PROC_BYDATE;

end PKG_CDRBALANCE_CHECK;
$$