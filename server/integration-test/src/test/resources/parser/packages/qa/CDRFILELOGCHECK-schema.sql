DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "CDRFILELOGCHECK" as
/*=====================================================================================================*/
function get_number(cFilename varchar2, nStart integer, aLength integer) return integer
is
	nRet	integer := -1;
begin
	nRet := to_number(substr(cFilename,nStart,aLength));
	dbms_output.put_line('1');
	return nRet;
exception when others then
	return -1;
end;
/*=====================================================================================================*/
procedure checkgsmfilelogbyname( rFormatRow	in cdrfilename_format%rowtype)
is
    v_max_filename varchar2(256);
	lastfile	sepfilelog%rowtype:=null;
	lastfilenum	integer:=null;
	curfilenum	integer:=null;

	-- ��������������D����������a����2������������t
	cursor curGsmFileLogFiles( aFileNameFormat varchar2, aFileName varchar2)
	 is select * from sepfilelog
	 where filename like aFileNameFormat and filename >= aFileName
       and filename <= v_max_filename
	 order by filename;

begin
	dbms_output.put_line('name check '||rFormatRow.filenameformat);
	-- ����3y����������������D��o����3����D������
	delete filelog_check
    	where  nettype = rFormatRow.nettype and errortype='S'
	      and filename2 like rFormatRow.filenameformat
          and filename2 >= rFormatRow.lastfilename;

    --����3��������������2��������������������t����
    select nvl(max(filename),rFormatRow.lastfilename) into v_max_filename from sepfilelog
	 where filename like rFormatRow.filenameformat and filename >= rFormatRow.lastfilename
       and endprocessdate < to_date('20201103010101','yyyymmddhh24miss')-3/24 ;

	-- ����2����������DD
	for "file" in curGsmFileLogFiles(rFormatRow.filenameformat,rFormatRow.lastfilename) loop
		if length("file".filename) <> rFormatRow.filenamelen and instr("file".filename,'.dat')=0 then
			if lastfile.filename is not null then
				insert into filelog_check(nettype,errortype,filename1,
					processtime1,filename2,processtime2)
				values(rFormatRow.nettype,'L',lastfile.filename,
					lastfile.beginprocessdate,"file".filename,"file".beginprocessdate);
					dbms_output.put_line('2');
			end if;
			lastfilenum := -1;
		else	--file name length ok
			curfilenum := get_number("file".filename,rFormatRow.checkpos,rFormatRow.checklen);
			if lastfilenum is null then
				lastfilenum := curfilenum;
				dbms_output.put_line('3');
			else	--last file num exists
				if (lastfilenum <> -1) and ( curfilenum = mod(lastfilenum + 1,
						power(10,rFormatRow.filenamelen - rFormatRow.checkpos + 1)) ) then
					lastfilenum := curfilenum;
					dbms_output.put_line('4');
				else -- error found
					insert into filelog_check(nettype,errortype,filename1,
						processtime1,filename2,processtime2)
					values(rFormatRow.nettype,'S',lastfile.filename,
						lastfile.beginprocessdate,"file".filename,"file".beginprocessdate);
					lastfilenum := curfilenum;
					dbms_output.put_line('5');
				end if;
			end if;
		end if;
		lastfile := "file";
	end loop;

	-- ��������������������2����������������������
    if lastfile.filename is not null then
    	update cdrfilename_format
    	 set lastfilename = lastfile.filename, lastnamecheckfiletime = lastfile.endprocessdate,
          lastchecktime=to_date('20201021','yyyymmdd')
    	 where nettype = rFormatRow.nettype and filenameformat = rFormatRow.filenameformat;
		 dbms_output.put_line('6');
    end if;

	commit;
	return;
	exception when others then
		dbms_output.put_line('error in check gsm file log.');
		dbms_output.put_line(sqlerrm);
		rollback;
		return;
end;
/*=====================================================================================================*/
procedure checkfilelog
is
	cursor curCdrFileNameFormat is select * from cdrfilename_format;
	rFileNameFormat		cdrfilename_format%rowtype;
begin
	for "row" in curCdrFileNameFormat loop
		rFileNameFormat := "row";
		dbms_output.put_line("row".filenameformat);
		checkgsmfilelogbyname(rFileNameFormat);
		dbms_output.put_line('7');
	end loop;
exception when others then
	dbms_output.put_line('error in check filelog');
	return;
end;
/*=====================================================================================================*/
end;
$$