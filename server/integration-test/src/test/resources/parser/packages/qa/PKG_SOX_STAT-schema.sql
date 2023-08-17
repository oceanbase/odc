DELIMITER $$
CREATE OR REPLACE PACKAGE BODY  "PKG_SOX_STAT" is

    --采集非空性(空单,非Put的话单)
    procedure ap_stat_empty_file(av_statday in varchar2)
    is
        v_count         number(16);
        v_all_count     number(16);
        v_partition     varchar2(32);
        v_sql           varchar2(2048);
        v_tablename     varchar2(32);
    begin
        v_tablename:='CPRUN_'||substr(av_statday,1,6);
        v_partition:='P_00'||substr(av_statday,-2);
        --取配置历史表中昨天在用的非Put的采集配置
        for cur in (SELECT node,id,DESCRIPTION,operation_type,T.HOSTIP_PAIR FROM  cpconfig_his T
                        WHERE T.LOGTIME>=TO_DATE(av_statday,'YYYYMMDD')
                              AND T.LOGTIME<TO_DATE(av_statday,'YYYYMMDD')+1
                              AND T.OPERATION_TYPE<>'P'
                              and t.id not in('40j',
                                              '40k',
                                              '44d',
                                              '70c',
                                              '80c',
                                              '84h',
                                              'qdags18',
                                              '89j3',
                                              '89j4',
                                              '89j1'
                                              )

                   )
        loop
           v_sql:='select count(*)  from '||v_tablename||' partition ('||v_partition||')'
                   ||' where node=:node and id=:id and filesize=0 and day=:day';

            execute immediate v_sql into v_count using cur.node,cur.id,substr(av_statday,-2);

            if v_count>0 then
                v_sql:='select count(*)  from '||v_tablename||' partition ('||v_partition||')'
                       ||' where node=:node and id=:id and day=:day';
                execute immediate v_sql into v_all_count using cur.node,cur.id,substr(av_statday,-2);
                insert into sox_check_empty_file(statday,node,id,discritption,operation_type,zero_cnt,all_count)
                    values(av_statday,cur.node,cur.id,cur.description,cur.operation_type,v_count,v_all_count);
					dbms_output.put_line('1');
            end if;
        end loop;
        --提交
        commit;
    end ap_stat_empty_file;

    --话单连续性(非Put的话单，配置统计到了sox_cdr_continuity_config表中)
    procedure ap_stat_cdr_continuity(av_statday in varchar2,av_first_conn in number)
    is
        type tp_refcusor is ref cursor;
        v_refcur         tp_refcusor;
        type tp_record is record(node     VARCHAR2(1024),
                                 id       VARCHAR2(1024),
                                 filename VARCHAR2(1024),
                                 fileno   number(20));
        v_record         tp_record;
        v_record_null    tp_record;
        v_sql            varchar2(2048);
        v_tablename      varchar2(32);
        v_tablename_next varchar2(32);
        v_day            varchar2(32);
        v_day_next       varchar2(32);
        v_stat_day_next  varchar2(32);
        v_lastday        varchar2(32);
        v_partition      varchar2(32);
        v_partition_next varchar2(32);
        v_pre_no         number(20);
        v_pre_filename   varchar2(1024);
        v_cur_no         number(20);
        v_cur_filename   varchar2(1024);
        v_cnt            number(20);
    begin
        --
        v_day:=substr(av_statday,-2);
        v_tablename:='CPRUN_'||substr(av_statday,1,6);
        v_partition:='P_00'||v_day;
        --正常处理逻辑,需要取昨天最后一个文件跟今天的比对
        for cur in (select * from sox_cdr_continuity_config where inuse=1 )
        loop
            --判断统计当日日志表分区里是否包含相应的采集/分发网元的信息
            v_sql:='select count(*) from '||v_tablename||' partition('||v_partition||')'
                 --||' where node='''||cur.node||''' and id='''||cur.id||''' and filename like '''||cur.pattern||'''';
                 ||' where id='''||cur.id||''' and filename like '''||cur.pattern||'''';
            execute immediate v_sql into v_cnt;
            --日志表中有数据才处理
            if v_cnt>0 then
                --按最大序号翻转
                if cur.isdayreverse=0 then
                    --置上一次的文件名/序号
                    if av_first_conn=0 then
                        --从配置表中获取上次的信息
                        v_pre_filename:=cur.lastchkfilename;
                        v_pre_no:=substr(v_pre_filename,cur.filenostartpos,cur.filenoendpos-cur.filenostartpos+1);
						dbms_output.put_line('2');
                        if v_pre_no is null then
                           v_pre_no:=0;
						   dbms_output.put_line('3');
                        end if;
                    else
                        --以当前的采集日志中第一个文件的序号为开始序号(一定注意要初始化记录)
                        v_record:=v_record_null;
                        v_record.node:=cur.node;
                        v_record.id:=cur.id;
                        v_record.filename:='';
                        v_record.fileno:=0;
                        v_pre_no:=0;
                        --初始化上一个文件名（gsc 2015-3-2）
                        v_pre_filename:='';
						dbms_output.put_line('4');
                    end if;
                    --开始判断
                   -- if cur.id='30e2' then
                      v_sql:='select * from ( select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                         ||' from '||v_tablename||' partition('||v_partition||') t where t.id='''||cur.id
                         --||''' and node='''||cur.node||''' and day='''||v_day
                         ||''' and day='''||v_day
                         ||''' and filename like '''||cur.pattern||'''and filename not like ''%.999'' order by begintime,filename)';
                        /* and filename not like '''%.999'''*/
                  /*  else
                      v_sql:='select * from (select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                           ||' from '||v_tablename||' partition('||v_partition||') t where t.id='''||cur.id
                           --||''' and node='''||cur.node||''' and day='''||v_day
                           ||''' and day='''||v_day
                           ||''' and substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1)>'||v_pre_no
                           ||' and filename like '''||cur.pattern||''' order by filename)'
                           ||' union all '
                           ||'select * from ( select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                           ||' from '||v_tablename||' partition('||v_partition||') t where t.id='''||cur.id
                           --||''' and node='''||cur.node||''' and day='''||v_day
                           ||''' and day='''||v_day
                           ||''' and substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1)<'||v_pre_no
                           ||' and filename like '''||cur.pattern||''' order by filename)';
                    end if;*/ --修改排序方式(hxy 20150518)
                    --获取采集日志数据开始处理
                    --gv_sql:=v_sql;
                    open v_refcur for v_sql;
                    loop
                        fetch v_refcur into v_record;
                        exit when v_refcur%notfound;
                        v_cur_filename:=v_record.filename;
                        v_cur_no:=substr(v_cur_filename,cur.filenostartpos,cur.filenoendpos-cur.filenostartpos+1);
                        --
                        if v_cur_no<>v_pre_no+1 then
                            --不到最大序号
                            if v_pre_no<>cur.maxreverseno then
                                insert into sox_check_files_conn(statday,node,id,discritption,fromfilename,tofilename)
                                    values(av_statday,v_record.node,v_record.id,cur.description,v_pre_filename,v_cur_filename);
									dbms_output.put_line('5');
                            else
                                --到了最大序号，开始序号却不等于默认开始序号
                                if v_cur_no<>cur.minstartno then
                                    insert into sox_check_files_conn(statday,node,id,discritption,fromfilename,tofilename)
                                        values(av_statday,v_record.node,v_record.id,cur.description,v_pre_filename,v_cur_filename);
										dbms_output.put_line('6');
                                end if;
                            end if;
                        end if;
                        --置上一次的文件名/序号
                        v_pre_filename:=v_cur_filename;
                        v_pre_no:=substr(v_pre_filename,cur.filenostartpos,cur.filenoendpos-cur.filenostartpos+1);
                    end loop;
                    close v_refcur;
                    --更新配置中本次检查的最后一个文件名
                    update sox_cdr_continuity_config t
                         set t.lastchkfilename=v_pre_filename
                         where t.node=cur.node and t.id=cur.id and t.pattern=cur.pattern;
                    commit;
                --按天翻转,数量一定(是否必要)
                elsif cur.isdayreverse=1 then
                    --v_day是否最后1天
                    v_lastday:=to_char(last_day(trunc(to_date(av_statday,'yyyymmdd'))-1),'dd');
                    --统计日av_statday是当月的最后1天
                    if v_day=v_lastday then
                        v_day_next:=to_char(to_date(av_statday,'yyyymmdd')+1,'dd');
                        v_partition_next:='P_00'||v_day_next;
                        v_stat_day_next:=to_char(to_date(av_statday,'yyyymmdd')+1,'yyyymmdd');
                        v_tablename_next:='CPRUN_'||substr(v_stat_day_next,1,6);
                        v_sql:='select * from (select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                                 ||' from '||v_tablename||' partition('||v_partition||') t '
                                 --||' where t.id='''||cur.id||''' and node='''||cur.node||''' and day='''||v_day||''''
                                 ||' where t.id='''||cur.id||''' and day='''||v_day||''''
                                 ||' and filename like '''||cur.pattern||''''
                                 ||' and filename like ''%'||av_statday||'%'' order by filename)'
                                 ||' union all '
                                 ||'select * from (select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                                 ||' from '||v_tablename_next||' partition('||v_partition_next||') t '
                                 --||'where t.id='''||cur.id||''' and node='''||cur.node||''' and day='''||v_day_next||''''
                                 ||'where t.id='''||cur.id||''' and day='''||v_day_next||''''
                                 ||' and filename like '''||cur.pattern||''''
                                 ||' and filename like ''%'||av_statday||'%'' order by filename)';
								 dbms_output.put_line('7');
                    else
                        v_day_next:=to_char(to_date(av_statday,'yyyymmdd')+1,'dd');
                        v_partition_next:='P_00'||v_day_next;
                        v_stat_day_next:=to_char(to_date(av_statday,'yyyymmdd')+1,'yyyymmdd');
                        v_sql:='select * from (select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                                 ||' from '||v_tablename||' partition('||v_partition||') t '
                                 --||' where t.id='''||cur.id||''' and node='''||cur.node||''' and day='''||v_day||''''
                                 ||' where t.id='''||cur.id||''' and day='''||v_day||''''
                                 ||' and filename like '''||cur.pattern||''''
                                 ||' and filename like ''%'||av_statday||'%'' order by filename)'
                                 ||' union all '
                                 ||'select * from (select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                                 ||' from '||v_tablename||' partition('||v_partition_next||') t '
                                 --||'where t.id='''||cur.id||''' and node='''||cur.node||''' and day='''||v_day_next||''''
                                 ||'where t.id='''||cur.id||''' and day='''||v_day_next||''''
                                 ||' and filename like '''||cur.pattern||''''
                                 ||' and filename like ''%'||av_statday||'%'' order by filename)';
								 dbms_output.put_line('8');
                    end if;
                    --
                    open v_refcur for v_sql;
                    --第一次执行
                    v_record:=v_record_null;
                    fetch v_refcur into v_record;
                    if v_record.node is not null then
                        v_pre_filename:=v_record.filename;
                        v_pre_no:=substr(v_pre_filename,cur.filenostartpos,cur.filenoendpos-cur.filenostartpos+1);
						dbms_output.put_line('9');
                    end if;
                    loop
                        fetch v_refcur into v_record;
                        exit when v_refcur%notfound;
                        v_cur_filename:=v_record.filename;
                        v_cur_no:=substr(v_cur_filename,cur.filenostartpos,cur.filenoendpos-cur.filenostartpos+1);
                        if v_cur_no<>v_pre_no+1 then
                            --不连续日志
                            insert into sox_check_files_conn(statday,node,id,discritption,fromfilename,tofilename)
                                values(av_statday,v_record.node,v_record.id,cur.description,v_pre_filename,v_cur_filename);
								dbms_output.put_line('10');
                        end if;
                        v_pre_no:=v_cur_no;
                        v_pre_filename:=v_cur_filename;
                    end loop;
                    close v_refcur;
                    --没到最大文件序号
                    if v_cur_no<>cur.maxreverseno then
                       --结束文件序号不等于文件的最小结束序号的不连续日志
                       /*
                       insert into sox_check_files_conn(statday,node,id,discritption,fromfilename,tofilename)
                           values(av_statday,v_record.node,v_record.id,cur.description,v_pre_filename,'MAX_DAY_ENDFILE');
                       */
                       null;
					   dbms_output.put_line('11');
                    end if;
                    commit;
                --按天翻转,数量不定
                elsif cur.isdayreverse=2 then
                    v_sql:='select node,id,filename,substr(filename,'||cur.filenostartpos||','||cur.filenoendpos||'-'||cur.filenostartpos||'+1) fileno '
                             ||' from '||v_tablename||' partition('||v_partition||') t '
                             --||' where t.id='''||cur.id||''' and node='''||cur.node||''' and day='''||v_day||''''
                             ||' where t.id='''||cur.id||''' and day='''||v_day||''''
                             ||' and filename like '''||cur.pattern||''''
                             ||' and filename like ''%'||av_statday||'%'' order by filename';
                    open v_refcur for v_sql;
                    --第一次执行
                    v_record:=v_record_null;
                    fetch v_refcur into v_record;
                    if v_record.node is not null then
                        v_pre_filename:=v_record.filename;
                        v_pre_no:=substr(v_pre_filename,cur.filenostartpos,cur.filenoendpos-cur.filenostartpos+1);
                        --
                        if v_pre_no<>cur.minstartno then
                            --开始文件序号不等于文件的最小开始序号的不连续日志
                            insert into sox_check_files_conn(statday,node,id,discritption,fromfilename,tofilename)
                                values(av_statday,v_record.node,v_record.id,cur.description,'MIN_DAY_STARTFILE',v_pre_filename);
								dbms_output.put_line('12');
                        end if;
                    end if;
                    --
                    loop
                        fetch v_refcur into v_record;
                        exit when v_refcur%notfound;
                        v_cur_filename:=v_record.filename;
                        v_cur_no:=substr(v_cur_filename,cur.filenostartpos,cur.filenoendpos-cur.filenostartpos+1);
                        if v_cur_no<>v_pre_no+1 then
                            --不到最大序号
                            insert into sox_check_files_conn(statday,node,id,discritption,fromfilename,tofilename)
                                values(av_statday,v_record.node,v_record.id,cur.description,v_pre_filename,v_cur_filename);
								dbms_output.put_line('13');
                        end if;
                        v_pre_no:=v_cur_no;
                        v_pre_filename:=v_cur_filename;
                    end loop;
                    close v_refcur;
                else
                    --系统未定义
					dbms_output.put_line('14');
                    Raise_Application_error(-20001,'系统未定义的isdayreverse值，请联系张博洋检查！');
					
                end if;
                commit;
        /*
        exception
            when others then
            dbms_output.put_line(gv_id);
            dbms_output.put_line(substr(gv_sql,1,250));
            dbms_output.put_line(substr(gv_sql,251,500));
            dbms_output.put_line(substr(gv_sql,501,750));
            dbms_output.put_line(substr(gv_sql,751,1000));
        */

            end if;
        end loop;

        --assurance commit
        commit;

    end ap_stat_cdr_continuity;


    --
    procedure ap_check_config(av_statday in varchar2)
    is
        --v_cnt               number(10);
        v_statday           varchar2(8);
        v_cur_day           varchar2(8);
        v_cur_hour          varchar2(2);
    begin
        v_statday:=av_statday;
        v_cur_day:=to_char(sysdate,'YYYYMMDD');
        --统计当前日的
        if v_statday=v_cur_day then
            select to_char(sysdate,'HH24') INTO v_cur_hour FROM DUAL;
            --可以从表中取出当天的配置进行比对，否则需要比对昨天/前天的配置
            IF v_cur_day>='06' THEN
                insert into sox_check_cpconfig(NODE,ID,DESCRIPTION,OPERATION_TYPE,GATHER_MODE,HOSTIP_PAIR,USER_PAIR,PASSWORD_PAIR,ENCRYPTION,SOURCE_PATTERN,SOURCE_NAME_FILTER,SOURCE_MOVEDEL,SOURCE_PATH,SOURCE_BACKUP_PATH,DEST_PATH,DEST_NAME_FORMAT,BUSY_TIME,BUSY_INTERVAL,FREE_INTERVAL,NOFILE_LIMIT,FILE_ANALYZER,SEQUENCE_CONTROL,SEQUENCE_SCOPE,SEQUENCE_SYNC,WARN_MODE,RECORD_SIZE,FTP_TIMEOUT,FTP_RESUME,FTP_PASSIVE,SIZE_COL,EXTRACOPY,STATUS)
                       select NODE,ID,DESCRIPTION,OPERATION_TYPE,GATHER_MODE,HOSTIP_PAIR,USER_PAIR,PASSWORD_PAIR,ENCRYPTION,SOURCE_PATTERN,SOURCE_NAME_FILTER,SOURCE_MOVEDEL,SOURCE_PATH,SOURCE_BACKUP_PATH,DEST_PATH,DEST_NAME_FORMAT,BUSY_TIME,BUSY_INTERVAL,FREE_INTERVAL,NOFILE_LIMIT,FILE_ANALYZER,SEQUENCE_CONTROL,SEQUENCE_SCOPE,SEQUENCE_SYNC,WARN_MODE,RECORD_SIZE,FTP_TIMEOUT,FTP_RESUME,FTP_PASSIVE,SIZE_COL,EXTRACOPY,STATUS
                           from cpconfig
                       minus
                       select NODE,ID,DESCRIPTION,OPERATION_TYPE,GATHER_MODE,HOSTIP_PAIR,USER_PAIR,PASSWORD_PAIR,ENCRYPTION,SOURCE_PATTERN,SOURCE_NAME_FILTER,SOURCE_MOVEDEL,SOURCE_PATH,SOURCE_BACKUP_PATH,DEST_PATH,DEST_NAME_FORMAT,BUSY_TIME,BUSY_INTERVAL,FREE_INTERVAL,NOFILE_LIMIT,FILE_ANALYZER,SEQUENCE_CONTROL,SEQUENCE_SCOPE,SEQUENCE_SYNC,WARN_MODE,RECORD_SIZE,FTP_TIMEOUT,FTP_RESUME,FTP_PASSIVE,SIZE_COL,EXTRACOPY,STATUS
                           from cpconfig_his
                           where trunc(logtime)=to_date(v_statday,'yyyymmdd')-1;
						   dbms_output.put_line('15');

            END IF;
        end if;
        --
        commit;
    end ap_check_config;


end pkg_sox_stat;
$$