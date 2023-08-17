DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_VALIDUSER" AS
 PROCEDURE Setuser(v_User IN VARCHAR2,
  v_Istriggerneed NUMBER := 1,
  v_clientinfo in varchar2 := '') AS
 v_Usertype NUMBER:=0; --V3R1C01B01 39824 2007-3-19
 v_Logonid NUMBER:=0; --V3R1C12L07n11 sKF42558 2012-07-10

 BEGIN
 Pkg_Validuser.Userid := v_User;
 Pkg_Validuser.Istriggerneed := v_Istriggerneed;
 ClientInfo := v_clientinfo;

 select to_char(to_date('20201103010101','yyyymmddhh24miss'), 'yyyymmddhh24miss') ||
 lpad(to_char(seq_logon.nextval), 8, '0')
 into Pkg_Validuser.LogonSeq
 from dual;
 begin
  SELECT Usertype
  INTO v_Usertype
  FROM App_User
 WHERE User_Code = v_User
  AND Rownum = 1;  --V3R1C01B01 39824 2007-3-19
 Setusertype(v_Usertype); --V3R1C01B01 39824 2007-3-19

 --V3R1C12L07n11 sKF42558 2012-07-10
 SELECT Seq_Billingclientlog.NEXTVAL INTO v_Logonid FROM Dual;
 SetLogonid(v_Logonid);
 --V3R1C12L07n11 sKF42558 2012-07-10
				dbms_output.put_line('1');
 exception
 when others then
  null;
 end;
		dbms_output.put_line('2');
 END;

 PROCEDURE Defaultuser(v_Istriggerneed NUMBER := 1) AS
 BEGIN
 Pkg_Validuser.Userid := '_SYS_';
 Pkg_Validuser.Istriggerneed := v_Istriggerneed;
 if Pkg_Validuser.LogonSeq = '0' then
 select to_char(to_date('20201103010101','yyyymmddhh24miss'), 'yyyymmddhh24miss') ||
 lpad(to_char(seq_logon.nextval), 8, '0')
 into Pkg_Validuser.LogonSeq
 from dual;
		dbms_output.put_line('3');
 end if;
 END;

 --V3R1C01B01 39824 2007-3-19 set user type
 --modify by zkf28147 2010-8-9 增加开发模式，如果为开发模式设置usertype为1(即用户类型为管理员)
 PROCEDURE Setusertype(v_Usertype NUMBER := 0) AS
 v_SystemMode VARCHAR2(64);
 BEGIN
 select value
 into v_SystemMode
 from feature_def
  where name = 'system_mode';
 if v_SystemMode = 'development' then
 Pkg_Validuser.Usertype := 1;
	 dbms_output.put_line('4');
 else
 Pkg_Validuser.Usertype := v_Usertype;
	 dbms_output.put_line('5');
 end if;
 exception
 when no_data_found then
 Pkg_Validuser.Usertype := v_Usertype;
	 dbms_output.put_line('6');
 END;

 --V3R1C01B01 39885 2007-03-15
 --V3R1C12L07n11 sKF42558 2012-07-10 set logonid
 PROCEDURE SetLogonid(v_Logonid NUMBER := 0) AS
 BEGIN
 Pkg_Validuser.Logonid := v_Logonid;
		 dbms_output.put_line('7');
 END;
 --V3R1C12L07n11 sKF42558 2012-07-10

 function BeginAuditLog(a_AuditLog in out AuditLogType) return number is
  v_SystemMode VARCHAR2(64);
 begin
 begin
		--add by zkf28147 2010-8-9 增加开发模式，日志记录登陆操作系统的用户名 begin
  select value into v_SystemMode from feature_def
  where name = 'system_mode';
  if v_SystemMode = 'development' then
 select sys_context('userenv', 'OS_USER')
 into Pkg_Validuser.Userid
 from dual;
		 dbms_output.put_line('8');
  end if;
 exception
  when no_data_found then
  a_AuditLog.Operator := pkg_validuser.userid;
	 dbms_output.put_line('9');
 end;
 --add by zkf28147 2010-8-9 增加开发模式，日志记录登陆操作系统的用户名 end

 if istriggerneed=0 then
		dbms_output.put_line('10');
  return 0;
 end if;

 if pkg_validuser.userid is null then
  --raise_application_error(-20101, 'invalid user, can''t change data.');
  --return;
  pkg_validuser.userid := '_SYS_';
			dbms_output.put_line('11');
 end if;

 --Get Client Infomation
 --select sys_context('userenv','IP_ADDRESS')||' : '||sys_context('userenv','HOST') into vAuditLog.ClientInfo from dual;
 if ClientInfo is null then
  a_AuditLog.ClientInfo := sys_context('userenv','IP_ADDRESS')||' : '||sys_context('userenv','HOST');
  ClientInfo := a_AuditLog.ClientInfo;
			dbms_output.put_line('12');
 else
  a_AuditLog.ClientInfo := ClientInfo;
			dbms_output.put_line('13');
 end if;

  a_AuditLog.Operator := pkg_validuser.userid;

/*
 else
  select max(user_code) into a_AuditLog.Operator from app_user where user_code=userid;
  if a_AuditLog.Operator is null then
 raise_application_error(-20102, 'invalid user, can''t change data.');
 return 1;
  end if;
 end if;
*/
 --get max formnum
 select to_char(seq_audit.nextval) into a_AuditLog.LogNum from dual;
 a_AuditLog.LogNum:=a_AuditLog.Operator||lpad(a_AuditLog.LogNum,8,'0');
 a_AuditLog.LogTime:=to_date('20201103010101','yyyymmddhh24miss');
 --a_AuditLog.TableName:=a_TableName;
 --a_AuditLog.MaintainType := a_ChangeType;
 if a_AuditLog.MaintainType = 'I' then
 a_AuditLog.KeyValue := '';
		 dbms_output.put_line('14');
 end if;
 return 1;
 end;

 procedure WriteAuditLog(a_AuditLog in AuditLogType,
  a_fieldname varchar2,
  a_oldvalue date,
  a_newvalue date)is
 begin
 WriteAuditLog(a_AuditLog, a_fieldname,
  to_char(a_oldvalue, 'yyyy-mm-dd hh24:mi:ss'),
  to_char(a_newvalue, 'yyyy-mm-dd hh24:mi:ss'));
					 dbms_output.put_line('15');
 end;

 procedure WriteAuditLog(a_AuditLog in AuditLogType,
  a_fieldname varchar2,
  a_oldvalue varchar2,
  a_newvalue varchar2)is
 begin
 if (a_newvalue is not null and a_oldvalue is null or
  a_newvalue is null and a_oldvalue is not null) or
  (a_newvalue != a_oldvalue) then
 if Pkg_Validuser.LogonSeq = '0' then
 select to_char(to_date('20201103010101','yyyymmddhh24miss'), 'yyyymmddhh24miss') ||
  lpad(to_char(seq_logon.nextval), 8, '0')
 into Pkg_Validuser.LogonSeq
 from dual;
		 dbms_output.put_line('16');
 end if;
 -- add 21525
 if pkg_validuser.Logonid = 0 then
  SELECT Seq_Billingclientlog.NEXTVAL INTO pkg_validuser.Logonid FROM Dual;
	 dbms_output.put_line('17');
 end if;
 -- end
 insert into audit_log
 (lognum,
 operator,
 logtime,
 tablename,
 maintaintype,
 keyvalue,
 colname,
 oldvalue,
 newvalue,
 clientinfo,
 logonid,
 LogonSeq,
 OPERNUM)
 values
 (a_AuditLog.LogNum,
 a_AuditLog.Operator,
 a_AuditLog.LogTime,
 a_AuditLog.TableName,
 a_AuditLog.MaintainType,
 a_AuditLog.KeyValue,
 a_fieldname,
 a_oldvalue,
 a_newvalue,
 pkg_validuser.clientinfo,
 pkg_validuser.Logonid,
 pkg_validuser.LogonSeq,
 a_AuditLog.OPERNUM);
		 dbms_output.put_line('18');
 end if;
 end;

 procedure EndAuditLog(a_AuditLog in out AuditLogType) is
 begin
 --notify to bpm background process
 update param_table_def
  set lastchangetime = to_date('20201022','yyyymmdd')
  where table_name = a_AuditLog.TableName;
	 dbms_output.put_line('19');
 end;
 --V3R1C01B01 39885 2007-03-15

 procedure GetLogonSeq(a_LogonSeq OUT VARCHAR2) is
 begin
 a_LogonSeq := Pkg_Validuser.LogonSeq;
	dbms_output.put_line('20');
 end;

 function GetUserid(a_userid in varchar2) return varchar2 is
 v_userid varchar2(20);
 begin
 v_userid := Pkg_Validuser.Userid;
	 dbms_output.put_line('21');
 return v_userid;
 end;
END Pkg_Validuser;
$$