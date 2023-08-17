DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "BP" AS

FUNCTION f_check_validdate(v_date IN VARCHAR2,d_lastdate OUT DATE)RETURN DATE
IS
d_date DATE;
v_lastdate VARCHAR2(16);
BEGIN
 --JUDGE CLOSE DATE IS 29 TO 31  BY W54999 ON 20080129
    v_lastdate := to_char(last_day(to_date(substr(v_date,1,6),'YYYYMM')),'YYYYMMDD')||substr(v_date,length(v_date)-5,6);
    IF v_date>v_lastdate THEN
       d_lastdate := to_date(v_lastdate,'YYYYMMDDHH24MISS');
       d_date := to_date(to_char(trunc(add_months(to_date(v_lastdate,'YYYYMMDDHH24MISS'),1),'mm'),'YYYYMMDD')||substr(v_date,length(v_date)-5,6),'YYYYMMDDHH24MISS');
	   dbms_output.put_line('1');
    ELSE
       d_date := to_date(v_date,'YYYYMMDDHH24MISS');
       d_lastdate := d_date;
	   dbms_output.put_line('2');
    END IF;   
RETURN d_date;
END;

FUNCTION f_get_closedate(d_odate IN DATE,v_cdate IN VARCHAR2,n_unit IN NUMBER,d_ldate OUT DATE) RETURN DATE
IS
n_month NUMBER;
d_cdate DATE;
v_lastdate VARCHAR2(16);
BEGIN
    --get valid date for string 
    v_lastdate := to_char(last_day(to_date(substr(v_cdate,1,6),'YYYYMM')),'YYYYMMDD')||substr(v_cdate,length(v_cdate)-5,6);
    IF v_cdate>v_lastdate THEN
       d_cdate := to_date(v_lastdate,'YYYYMMDDHH24MISS');
	   dbms_output.put_line('3');
    ELSE
       d_cdate := to_date(v_cdate,'YYYYMMDDHH24MISS');
	   dbms_output.put_line('4');
    END IF;   
    --check whether the month between close date and open date exceed the month unit
    SELECT round(months_between(d_cdate,d_odate)) INTO n_month FROM dual;
    IF n_month<>n_unit THEN
	dbms_output.put_line('5');
      RETURN f_check_validdate(to_char(add_months(d_cdate,-(n_month-n_unit)),'YYYYMM')||substr(v_cdate,length(v_cdate)-7,8),d_ldate);
    ELSE
	dbms_output.put_line('6');
      RETURN f_check_validdate(v_cdate,d_ldate);
    END IF;
END;

function f_create_new_billingcycle(	theSchema 		in 		billingcycle_schema%rowtype,
									newOpenDate		in		date,
                  v_flag IN VARCHAR)
return billingcycle%rowtype
is
	theNewCycle		billingcycle%rowtype;
  d_opendate DATE;
  d_closedate DATE;
  v_seq  NUMBER;
begin
	--帐务周期类型。每种帐务周期对应特定的帐务周期命名格式。
  theNewCycle.opendate := newOpenDate;
	--1:	year：	YYYY
	if(theSchema.cycle_unit = 1)then
		--format new_open_date
    IF v_flag='0' THEN
		theNewCycle.opendate := to_date(to_char(newOpenDate,'YYYY')||substr(theSchema.opendate,length(theSchema.opendate)-9,10),'YYYYMMDDHH24MISS');
		dbms_output.put_line('7');
		END IF;
    --generate close date
		theNewCycle.closedate := add_months(theNewCycle.opendate,12 * theSchema.unit_count);
		--generate invoice date
		theNewCycle.invoicedate := theNewCycle.closedate + theSchema.invoice_delay/24;
    IF theSchema.Cycleid_Base=1 THEN
		--generate new cycle name
		theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYY');
		--generate new cycle id
		theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYY');
		dbms_output.put_line('8');
    ELSE 
		--generate new cycle name
		theNewCycle.cycle_name := to_char(theNewCycle.closedate,'YYYY');
		--generate new cycle id
		theNewCycle.cycle_id := to_char(theNewCycle.closedate,'YYYY');
		dbms_output.put_line('9');
    END IF;
	--2:	season：	YYYYQ
	elsif(theSchema.cycle_unit = 2)then
		--format new_open_date
    IF v_flag='0' THEN
		theNewCycle.opendate := to_date(to_char(newOpenDate,'YYYYMM')||substr(theSchema.opendate,length(theSchema.opendate)-7,8),'YYYYMMDDHH24MISS');
		dbms_output.put_line('10');
		END IF;
    --generate close date
		theNewCycle.closedate := add_months(theNewCycle.opendate,3 * theSchema.unit_count);
		--generate invoice date
		theNewCycle.invoicedate := theNewCycle.closedate + theSchema.invoice_delay/24;
    IF theSchema.Cycleid_Base=1 THEN
		--generate new cycle name
		theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYYQ');
		--generate new cycle id
		theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYYQ');
		dbms_output.put_line('11');
    ELSE 
		--generate new cycle name
		theNewCycle.cycle_name := to_char(theNewCycle.closedate,'YYYYQ');
		--generate new cycle id
		theNewCycle.cycle_id := to_char(theNewCycle.closedate,'YYYYQ');
		dbms_output.put_line('12');
    END IF;
	--3:	month：	YYYYMM
	elsif(theSchema.cycle_unit = 3)then
		--format new_open_date
    IF v_flag='0' THEN
		theNewCycle.opendate := f_check_validdate(to_char(newOpenDate,'YYYYMM')||substr(theSchema.opendate,length(theSchema.opendate)-7,8),d_opendate);
		dbms_output.put_line('13');
		END IF;
    
    --generate close date
    --JUDGE CLOSE DATE IS 29 TO 31  BY W54999 ON 20080129
		theNewCycle.Closedate := f_get_closedate(theNewCycle.opendate,to_char(add_months(theNewCycle.opendate,1 * theSchema.unit_count),'YYYYMM')||substr(theSchema.opendate,length(theSchema.opendate)-7,8),1*theSchema.unit_count,d_closedate);
    --generate invoice date
		theNewCycle.invoicedate := theNewCycle.closedate + theSchema.invoice_delay/24;
    IF v_flag='0' THEN
      IF theSchema.Cycleid_Base=1 THEN 
		  --generate new cycle name
      theNewCycle.cycle_name := to_char(d_opendate,'YYYY')||lpad(ceil(to_number(to_char(d_opendate,'mm'))/theschema.unit_count),2,'0');
		  --generate new cycle id
      theNewCycle.cycle_id := to_char(d_opendate,'YYYY')||lpad(ceil(to_number(to_char(d_opendate,'mm'))/theschema.unit_count),2,'0');
	  dbms_output.put_line('14');
	    ELSE
		  --generate new cycle name
      theNewCycle.cycle_name := to_char(d_closedate,'YYYY')||lpad(ceil(to_number(to_char(d_closedate,'mm'))/theschema.unit_count),2,'0');
		  --generate new cycle id
      theNewCycle.cycle_id := to_char(d_closedate,'YYYY')||lpad(ceil(to_number(to_char(d_closedate,'mm'))/theschema.unit_count),2,'0');
	  dbms_output.put_line('15');
	    END IF;
    ELSE
      IF theSchema.Cycleid_Base=1 THEN
    	--generate new cycle name
      SELECT to_number(nvl(substr(MAX(cycle_id),5,2),'0')) INTO v_seq  FROM billingcycle t 
      WHERE cycle_schema_id=theSchema.Cycle_Schema_Id AND substr(cycle_id,1,4)=to_char(theNewCycle.opendate,'YYYY');
    
		  theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		  --generate new cycle id
		  theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		  dbms_output.put_line('16');
      ELSE
    	--generate new cycle name
      SELECT to_number(nvl(substr(MAX(cycle_id),5,2),'0')) INTO v_seq  FROM billingcycle t 
      WHERE cycle_schema_id=theSchema.Cycle_Schema_Id AND substr(cycle_id,1,4)=to_char(theNewCycle.closedate,'YYYY');
    
		  theNewCycle.cycle_name := to_char(theNewCycle.closedate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		  --generate new cycle id
		  theNewCycle.cycle_id := to_char(theNewCycle.closedate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		  dbms_output.put_line('17');
	    END IF;
    END IF;	
  --4:	10 day：	YYYYMMDD
	elsif(theSchema.cycle_unit = 4)then
		--format new_open_date
    IF v_flag='0' THEN
		theNewCycle.opendate := to_date(to_char(newOpenDate,'YYYYMMDD')||substr(theSchema.opendate,length(theSchema.opendate)-5,6),'YYYYMMDDHH24MISS');
		dbms_output.put_line('18');
		END IF;
    --generate close date
		theNewCycle.closedate := theNewCycle.opendate + 10 * theSchema.unit_count;
		--generate invoice date
		theNewCycle.invoicedate := theNewCycle.closedate + theSchema.invoice_delay/24;
    IF theSchema.Cycleid_Base=1 THEN
		--generate new cycle name
		theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYYMMDD');
		--generate new cycle id
		theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYYMMDD');
		dbms_output.put_line('19');
    ELSE
		--generate new cycle name
		theNewCycle.cycle_name := to_char(theNewCycle.closedate,'YYYYMMDD');
		--generate new cycle id
		theNewCycle.cycle_id := to_char(theNewCycle.closedate,'YYYYMMDD');
		dbms_output.put_line('20');
    END IF;
	--5:	week：	YYYYMMDD
	elsif(theSchema.cycle_unit = 5)then
		--format new_open_date
    IF v_flag='0' THEN
		theNewCycle.opendate := to_date(to_char(newOpenDate,'YYYYMMDD')||substr(theSchema.opendate,length(theSchema.opendate)-5,6),'YYYYMMDDHH24MISS');
		dbms_output.put_line('21');
		END IF;
    --generate close date
		theNewCycle.closedate := theNewCycle.opendate + 7 * theSchema.unit_count;
		--generate invoice date
		theNewCycle.invoicedate := theNewCycle.closedate + theSchema.invoice_delay;
		--generate new cycle name
    IF theSchema.Cycleid_Base=1 THEN
		SELECT to_number(nvl(substr(MAX(cycle_id),5,2),'0')) INTO v_seq  FROM billingcycle t 
    WHERE cycle_schema_id=theSchema.Cycle_Schema_Id AND substr(cycle_id,1,4)=to_char(theNewCycle.opendate,'YYYY');
    
    --theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		--generate new cycle id
		--theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		dbms_output.put_line('22');
    ELSE
		SELECT to_number(nvl(substr(MAX(cycle_id),5,2),'0')) INTO v_seq  FROM billingcycle t 
    WHERE cycle_schema_id=theSchema.Cycle_Schema_Id AND substr(cycle_id,1,4)=to_char(theNewCycle.closedate,'YYYY');
    
    --theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_name := to_char(theNewCycle.closedate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		--generate new cycle id
		--theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_id := to_char(theNewCycle.closedate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		dbms_output.put_line('23');
	  END IF;
	--6:	day：	YYYYMMDD
	else	--6
		--format new_open_date
    IF v_flag='0' THEN
		theNewCycle.opendate := to_date(to_char(newOpenDate,'YYYYMMDD')||substr(theSchema.opendate,length(theSchema.opendate)-5,6),'YYYYMMDDHH24MISS');
		dbms_output.put_line('24');
		END IF;
    --generate close date
		theNewCycle.closedate := theNewCycle.opendate + 1 * theSchema.unit_count;
		--generate invoice date
		theNewCycle.invoicedate := theNewCycle.closedate + theSchema.invoice_delay/24;
		--generate new cycle name
    IF theSchema.Cycleid_Base=1 THEN
		SELECT to_number(nvl(substr(MAX(cycle_id),5,2),'0')) INTO v_seq  FROM billingcycle t 
    WHERE cycle_schema_id=theSchema.Cycle_Schema_Id AND substr(cycle_id,1,4)=to_char(theNewCycle.opendate,'YYYY');
    
    --theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		--generate new cycle id
		--theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		dbms_output.put_line('25');
    ELSE
		SELECT to_number(nvl(substr(MAX(cycle_id),5,2),'0')) INTO v_seq  FROM billingcycle t 
    WHERE cycle_schema_id=theSchema.Cycle_Schema_Id AND substr(cycle_id,1,4)=to_char(theNewCycle.closedate,'YYYY');
    --theNewCycle.cycle_name := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_name := to_char(theNewCycle.closedate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		--generate new cycle id
		--theNewCycle.cycle_id := to_char(theNewCycle.opendate,'YYYYMMDD');
		theNewCycle.cycle_id := to_char(theNewCycle.closedate,'YYYY')||lpad(to_char(v_seq+1),2,'0');
		dbms_output.put_line('26');
    END IF;
	end if;
	return theNewCycle;
end;

function f_billingcycle_translate(theCond in varchar2,theBillingCycle in billingcycle%rowtype) return varchar2
is
	s_cond					varchar2(500);
	s_replace				varchar2(20);
begin
	s_cond := theCond;
	s_replace := to_char(theBillingCycle.cycle_id);
	s_cond := replace(s_cond,chr(3)||'0008-.0001'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008->0001'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=.0001'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=>0001'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!.0001'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!>0001'||chr(4),s_replace);

	s_replace := ''''||to_char(theBillingCycle.opendate,'YYYYMMDDHH24MISS')||'''';
	s_cond := replace(s_cond,chr(3)||'0008-.0002'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008->0002'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=.0002'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=>0002'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!.0002'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!>0002'||chr(4),s_replace);

	s_replace := ''''||to_char(theBillingCycle.closedate,'YYYYMMDDHH24MISS')||'''';
	s_cond := replace(s_cond,chr(3)||'0008-.0003'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008->0003'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=.0003'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=>0003'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!.0003'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!>0003'||chr(4),s_replace);

	s_replace := ''''||to_char(theBillingCycle.invoicedate,'YYYYMMDDHH24MISS')||'''';
	s_cond := replace(s_cond,chr(3)||'0008-.0004'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008->0004'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=.0004'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008=>0004'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!.0004'||chr(4),s_replace);
	s_cond := replace(s_cond,chr(3)||'0008!>0004'||chr(4),s_replace);
dbms_output.put_line('27');
	return s_cond;
end;

function f_billingcycle_translate_name(theCond in varchar2,theBillingCycle in billingcycle%rowtype) return varchar2
is
	s_cond					varchar2(500);
	s_replace				varchar2(20);
begin
	s_cond := theCond;
	s_replace := to_char(theBillingCycle.cycle_id);
	s_cond := replace(s_cond,'帐务周期.帐务周期 ID',s_replace);
	s_replace := ''''||to_char(theBillingCycle.opendate,'YYYYMMDDHH24MISS')||'''';
	s_cond := replace(s_cond,'帐务周期.帐务周期开始时间',s_replace);
	s_replace := ''''||to_char(theBillingCycle.closedate,'YYYYMMDDHH24MISS')||'''';
	s_cond := replace(s_cond,'帐务周期.帐务周期结束时间',s_replace);
	s_replace := ''''||to_char(theBillingCycle.invoicedate,'YYYYMMDDHH24MISS')||'''';
	s_cond := replace(s_cond,'帐务周期.帐务周期出帐时间',s_replace);
	if s_cond is null then
		s_cond := 'xx' ;
		dbms_output.put_line('28');
	end if;
	return s_cond;
end;


procedure p_generate_billingcycle
	(n_cycle_schema_id	in	billingcycle_schema.cycle_schema_id%type)
is
	n_cycle_origin_count	pls_integer	:= 0;
	n_pre_built_num			pls_integer;
	n_new_cycle_status		pls_integer;
	d_new_open_date			date;
	d_last_open_date		date;
	d_last_close_date		date;
  v_cycle_id           billingcycle.cycle_id%type;
	thisSchema				billingcycle_schema%rowtype;
	theNewCycle				billingcycle%rowtype;
	s_new_billing_cond		varchar2(400);
	s_g_billing_cond 		charging_cycle.g_billing_cond%type :=' ';
	WRONG_STATUS			exception;
	ldt_today				date ;
begin
	--Get ServerDate
--echo ##手动将不稳定的to_date('20201103010101','yyyymmddhh24miss')改为稳定值
--	select to_date('20201103010101','yyyymmddhh24miss') into ldt_today from dual ;
select to_date('20201020172433','YYYYMMDDHH24MISS')  into ldt_today from dual ;
	select * into thisSchema from billingcycle_schema where cycle_schema_id = n_cycle_schema_id;
	--check status and fixed_cycle
	if(thisSchema.status<>1 or thisSchema.fixed_cycle<>1) then
	dbms_output.put_line('29');
		raise WRONG_STATUS;
	end if;
	select count(*) into n_cycle_origin_count from billingcycle where cycle_schema_id = n_cycle_schema_id;
	--以前没有任何此类（CycleSchemaId）的帐务周期
	if(n_cycle_origin_count = 0) THEN
  --delete void data from charging cycle by w54999 on 20080129
  delete from charging_cycle where cycle_schema_id=n_cycle_schema_id; 
		-- 建立第一个帐务周期并激活之(status=2)
		dbms_output.put_line('no origin record.');
		d_new_open_date := to_date(thisSchema.firstcycle_opendate,'YYYYMMDDHH24MISS');
		Thenewcycle := f_create_new_billingcycle(thisSchema,d_new_open_date,'0');
		--当前激活
		insert into billingcycle
			(cycle_schema_id,cycle_id,cycle_name,cycle_status,opendate,closedate,invoicedate)
			values
			(n_cycle_schema_id,theNewCycle.cycle_id,theNewCycle.cycle_name,2,theNewCycle.opendate,theNewCycle.closedate,theNewCycle.invoicedate);
		--generate charging_cycle
		for thisCycleSchema in (select * from charging_cycle_schema where cycle_schema_id=n_cycle_schema_id) loop
			s_new_billing_cond := f_billingcycle_translate(thisCycleSchema.billing_cond,theNewCycle);
			--Get Charging_Cycle_Schema G_Billing_Cond
			s_g_billing_cond := f_billingcycle_translate_name(thisCycleSchema.g_billing_cond,theNewCycle);
			insert into charging_cycle
				(cycle_schema_id,cycle_id,charging_event_id,billing_cond,g_billing_cond)
				values
				(n_cycle_schema_id,theNewCycle.cycle_id,thisCycleSchema.charging_event_id,s_new_billing_cond,' '||s_g_billing_cond);
				dbms_output.put_line('30');
		end loop;
		--Update Param_Table_Def LastChangeTime
		update param_table_def set lastchangetime=ldt_today where upper(table_name)='BILLINGCYCLE' ;
		update param_table_def set lastchangetime=ldt_today where upper(table_name)='CHARGING_CYCLE' ;
	end if;
  --add judge whether current bill cycle exist by w54999 on 20080129
BEGIN
	select cycle_id,opendate,closedate INTO v_cycle_id,d_last_open_date,d_last_close_date	from billingcycle where cycle_schema_id = n_cycle_schema_id and cycle_status=2;
	dbms_output.put_line('31');
EXCEPTION
  WHEN no_data_found THEN
  dbms_output.put_line('32');
     raise_application_error(-20100,'Can not find the current billingcycle for cycle_schema_id = '||n_cycle_schema_id||'!');
END;
	--delete old data  Modified 2003.12.16
	delete from charging_cycle where cycle_schema_id=n_cycle_schema_id and
	  		cycle_id >= v_cycle_id;
  
  --refenerate charging_cycle according to billingcycle fixed by weidh 20071211
  FOR thebillingcycle IN (SELECT * FROM billingcycle 
  WHERE cycle_schema_id = n_cycle_schema_id  and opendate >= d_last_open_date ORDER BY cycle_id) LOOP
    --generate charging_cycle
		for thisCycleSchema in (select * from charging_cycle_schema where cycle_schema_id=n_cycle_schema_id) loop
			s_new_billing_cond := f_billingcycle_translate(thisCycleSchema.billing_cond,thebillingcycle);
			s_g_billing_cond := f_billingcycle_translate_name(thisCycleSchema.g_billing_cond,thebillingcycle);
			insert into charging_cycle
				(cycle_schema_id,cycle_id,charging_event_id,billing_cond,g_billing_cond)
				values
				(n_cycle_schema_id,thebillingcycle.cycle_id,thisCycleSchema.charging_event_id,s_new_billing_cond,s_g_billing_cond);
				dbms_output.put_line('33');
		end loop;		
  END LOOP;  

	select count(*) into n_pre_built_num from billingcycle where cycle_schema_id = n_cycle_schema_id and opendate > d_last_open_date;
  select max(opendate),max(closedate) into d_last_open_date,d_last_close_date
		from billingcycle where cycle_schema_id = n_cycle_schema_id;
	dbms_output.put_line('get max: opendate='||to_char(d_last_open_date,'YYYYMMDD HH24MISS')||' closedate='||to_char(d_last_close_date,'YYYYMMDD HH24MISS'));

	if(thisSchema.multiactivation = 1) then
		n_new_cycle_status := 1;
		dbms_output.put_line('34');
	else
		n_new_cycle_status := 5;
		dbms_output.put_line('35');
	end if;

	dbms_output.put_line('count of cycles need build = '||to_char(thisSchema.prebuilt_cycles-n_pre_built_num));
	for i in 1 .. (thisSchema.prebuilt_cycles-n_pre_built_num) loop
		theNewCycle := f_create_new_billingcycle(thisSchema,d_last_close_date,'1');
		dbms_output.put_line('new cycle: id='||theNewCycle.cycle_id||' opendate='||to_char(theNewCycle.opendate,'yyyymmdd hh24miss')||' closedate='||to_char(theNewCycle.closedate,'yyyymmdd hh24miss'));
		insert into billingcycle
			(cycle_schema_id,cycle_id,cycle_name,cycle_status,opendate,closedate,invoicedate)
			values
			(n_cycle_schema_id,theNewCycle.cycle_id,theNewCycle.cycle_name,n_new_cycle_status,d_last_close_date,theNewCycle.closedate,theNewCycle.invoicedate);
		--generate charging_cycle
		for thisCycleSchema in (select * from charging_cycle_schema where cycle_schema_id=n_cycle_schema_id) loop
			s_new_billing_cond := f_billingcycle_translate(thisCycleSchema.billing_cond,theNewCycle);
			s_g_billing_cond := f_billingcycle_translate_name(thisCycleSchema.g_billing_cond,theNewCycle);
			insert into charging_cycle
				(cycle_schema_id,cycle_id,charging_event_id,billing_cond,g_billing_cond)
				values
				(n_cycle_schema_id,theNewCycle.cycle_id,thisCycleSchema.charging_event_id,s_new_billing_cond,s_g_billing_cond);
				dbms_output.put_line('36');
		end loop;
		d_last_close_date := theNewCycle.closedate;
	end loop;

	if (thisSchema.prebuilt_cycles - n_pre_built_num) > 0 then
		--Update Param_Table_Def LastChangeTime
		update param_table_def set lastchangetime=ldt_today where upper(table_name)='BILLINGCYCLE' ;
		update param_table_def set lastchangetime=ldt_today where upper(table_name)='CHARGING_CYCLE' ;
		dbms_output.put_line('37');
	else
		update param_table_def set lastchangetime=ldt_today where upper(table_name)='CHARGING_CYCLE' ;
		dbms_output.put_line('38');
	end if ;

end;

procedure p_generate_all_billingcycle
is
begin
	for theSchemaId in (select cycle_schema_id from billingcycle_schema where status=1 and fixed_cycle=1) loop
		p_generate_billingcycle(theSchemaId.cycle_schema_id);
		dbms_output.put_line('39');
	end loop;
end;

end;
$$