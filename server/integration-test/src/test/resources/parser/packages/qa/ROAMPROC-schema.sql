DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "ROAMPROC" 
is

function	getHighRate(imsi varchar2)
  return number as
  r number;
begin

  r := 1;

  --根据高额门限定义与标准高额门限值 50 SDR计算比例
  --标准值50如修改, 需同时修改此处与ap_set_HIGHALERTLEVEL存储过程

	for cc in (select money/50 rate from highcostexchangerate
              where imsi like imsinum||'%' and nvl(starttime,to_date('20201023','yyyymmdd')-1)<=to_date('20201023','yyyymmdd') and nvl(endtime,to_date('20201023','yyyymmdd')+1)>to_date('20201023','yyyymmdd')
              order by imsinum desc)
  loop
		r := cc.rate;
		dbms_output.put_line('1');
		exit;
	end loop;

	return r;

end ;

function	getThreshold(imsi varchar2)
  return number as
  r number(10,2);
begin
	r := 1;
	for cc in (select money from highcostexchangerate
      where imsi like imsinum||'%' and nvl(starttime,to_date('20201023','yyyymmdd')-1)<=to_date('20201023','yyyymmdd') and nvl(endtime,to_date('20201023','yyyymmdd')+1)>to_date('20201023','yyyymmdd')
       order by imsinum desc)
  loop
		r := cc.money;
		dbms_output.put_line('2');
		exit;
	end loop;

	return r;

end ;

function GetSdrRate( sRoamType varchar2 ) return number
is
	sUserType		varchar2(3);
	sNetType		varchar2(1);
	nRate			number(10,2);
begin
	if sRoamType='I' then
		sNetType :='G';
		sUserType:='231';
		dbms_output.put_line('3');
	elsif sRoamType='K' then
		sNetType :='G';
		sUserType:='231';
		dbms_output.put_line('4');
	else
	dbms_output.put_line('5');
		return 1;
	end if;

  nRate := 1;

	select nvl(sdrrate,1) into nRate from highalertlevel
	  where nettype=sNetType and usertype=sUserType
      and nvl(apply_time,to_date('20201023','yyyymmdd')-1)<=to_date('20201023','yyyymmdd') and nvl(expire_time,to_date('20201023','yyyymmdd')+1)>to_date('20201023','yyyymmdd')
      and rownum=1;

	return nRate;
exception
	when others then
		return 1;
end;


function toAlarmLevel( nFee number,sRoamType varchar2 ,sImsi varchar2) return varchar2
is
	sAlarmLevel		varchar2(1);
	sUserType		varchar2(3);
	sNetType		varchar2(1);
	nRate			number;
begin
	if sRoamType='A' then
		sNetType :='T';
		sUserType:='221';
		dbms_output.put_line('6');
	elsif sRoamType='D' then
		sNetType :='G';
		sUserType:='221';
		dbms_output.put_line('7');
	elsif sRoamType='I' then
		sNetType :='G';
		sUserType:='231';
		dbms_output.put_line('8');
	elsif sRoamType='K' then
		sNetType :='G';
		sUserType:='231';
		dbms_output.put_line('9');
	else
	dbms_output.put_line('10');
		return null;
	end if;
	nRate := getHighRate(sImsi);
	select alarmlevelno into sAlarmLevel from highalertlevel
   	where nettype=sNetType and usertype=sUserType and nFee >= lowerlimit*nRate and nFee < upperlimit*nRate
      and nvl(apply_time,to_date('20201023','yyyymmdd')-1)<=to_date('20201023','yyyymmdd') and nvl(expire_time,to_date('20201023','yyyymmdd')+1)>to_date('20201023','yyyymmdd');

	return sAlarmLevel;
exception
	when others then
		return null;
end;


procedure stat_highcost( sRoamType varchar2, sReportDay varchar2, sReportNum varchar2 )
is
	dReportDay	date;
	sYesterday	varchar2(20);
  nSdrRate    number;
begin

	dReportDay := to_date(sReportDay,'yyyymmdd');
	sYesterday := to_char(dReportDay -1, 'yyyymmdd');

  if sRoamType <> 'I' then
  	insert into guesthighcostreport
  		(roamtype,reportday,reportnum,nettype,imsimin,firstcalltime,totalduration,totalfee,totalcount,hprovince,alarmlevel,
      SERVICETYPE,LASTCALLTIME,VOLUME,SDR,THRESHOLD,recordtype)
  		select sRoamType,sReportDay,sReportNum,nettype,imsimin,firstcalltime,totalduration,totalfee,totalcount,
            hprovince,toAlarmLevel(totalfee,sRoamType,imsimin),
            servicetype,lastcalltime,totalvolume,0,0,'01'
  		from guesthighcoststat
  		where roamtype=sRoamType and statday in (sYesterday,sReportDay) and lastchangetime>=dReportDay and toAlarmLevel(totalfee,sRoamType,imsimin) is not null;
		dbms_output.put_line('11');
  else
    nSdrRate := GetSdrRate(sRoamType);
  	insert into guesthighcostreport
  		(roamtype,reportday,reportnum,nettype,imsimin,firstcalltime,
        totalduration,totalfee,totalcount,hprovince,alarmlevel,
        SERVICETYPE,LASTCALLTIME,VOLUME,SDR,THRESHOLD,recordtype)
  		select sRoamType,sReportDay,sReportNum,nettype,imsimin,firstcalltime,
          trunc(totalduration/3600)*10000+trunc(mod(totalduration,3600)/60)*100+mod(totalduration,60),totalfee,totalcount,hprovince,'1',
          servicetype,lastcalltime,totalvolume,totalfee/nSdrRate*1000,getThreshold(imsimin),'01'
  		from guesthighcoststat
  		where roamtype=sRoamType and statday in (sYesterday,sReportDay)
         and lastchangetime>=dReportDay
         and (RoamType,statday,nettype,imsimin) in (
        		select RoamType,statday,nettype,imsimin
        		from guesthighcoststat
        		where roamtype=sRoamType and statday in (sYesterday,sReportDay)
               and lastchangetime>=dReportDay
            group by RoamType,statday,nettype,imsimin
               having toAlarmLevel(sum(totalfee),sRoamType,imsimin) is not null
         );
		 dbms_output.put_line('12');
  end if;

	/* 非国际漫游来访高额报告, delete dup (imsi,firstcallday,alarmlevel) records in this time */
  if sRoamType <> 'I' then
  	delete from guesthighcostreport
  	where roamtype=sRoamType and reportday=sReportDay and reportnum=sReportNum
  		and (imsimin,trunc(firstcalltime,'dd'),alarmlevel) in
  		(select imsimin,trunc(firstcalltime,'dd'),alarmlevel
            from guesthighcostreport
        where roamtype=sRoamType and reportday in (sYesterday,sReportDay)
  		 group by imsimin,trunc(firstcalltime,'dd'),alarmlevel
  		 having count(*) > 1);
		 dbms_output.put_line('13');
  else
    --删除本次报告中的重复记录
  	delete from guesthighcostreport
  	where roamtype=sRoamType and reportday=sReportDay and reportnum=sReportNum
  		and imsimin in
  		(select imsimin
            from guesthighcostreport
        where roamtype=sRoamType and reportday=sReportDay and reportnum=sReportNum
  		 group by imsimin,servicetype
  		 having count(*) > 1);

    --删除本天已生成高额报告且金额未发生变化的高额报告
   	delete from guesthighcostreport
      	where roamtype=sRoamType and reportday=sReportDay and reportnum=sReportNum
         and imsimin in (select imsimin from
          	(
            select imsimin,sum(totalfee)
                  from guesthighcostreport
              where roamtype=sRoamType and reportday=sReportDay
                  and reportnum=sReportNum
        		 group by imsimin,reportnum
             intersect
              select imsimin,sum(totalfee)
                  from guesthighcostreport
              where roamtype=sRoamType and reportday=sReportDay
                  and reportnum<>sReportNum
        		 group by imsimin,reportnum
            )
           );
		   dbms_output.put_line('14');
  end if;
	return;
end;

procedure newdaytask( sNetType varchar2 )
is
begin
	/* clean guesthighcoststat */
	delete from guesthighcoststat where statday < to_char(to_date('20201023','yyyymmdd') -3,'YYYYMMDD') and nettype=sNetType;
	/* other new day tasks may been added below */
	/* ... */
	dbms_output.put_line('15');
end;

end;
$$