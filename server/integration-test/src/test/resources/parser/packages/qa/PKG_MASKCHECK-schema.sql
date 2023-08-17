DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_MASKCHECK" is

  --------------十进制转化为二进制 begin----------------------
 function d2b (n in number) return varchar2 is
    binval varchar2(64);
    n2     number := n;
    begin
      if n = 0 then
	   dbms_output.put_line('1');
         return '0';
      end if;
      while ( n2 > 0 ) loop
        binval := mod(n2, 2) || binval;
        n2 := trunc( n2 / 2 );
		 dbms_output.put_line('2');
      end loop;
      return binval;
  end d2b;
 --------------十进制转化为二进制 end---------------------------


  -----------某个字符串是否为数字字符串 begin-------------------
  function isNumber(p in varchar2)
  return number
  is
  result number;
  begin
    result := to_number(p);
	 dbms_output.put_line('3');
    return 1;
    exception
    when VALUE_ERROR then
	 dbms_output.put_line('4');
	return 0;
  end isNumber;
  -----------某个字符串是否为数字字符串 end-----------------------

 ------------将分隔符分隔的字符串拆分 begin-----------------------
 function fn_split (p_str IN VARCHAR2, p_delimiter IN VARCHAR2)
 RETURN ty_str_split PIPELINED
 IS
  j number:= 0;    ---分隔符的位置
  i number:= 1;    ---从哪个位置开始截取字符串
  len number:= 0;
  len1 number:= 0;
  str VARCHAR2 (64);
  BEGIN
    len := LENGTH (p_str);   ----被分割字符串的长度
    len1 := LENGTH (p_delimiter); ---分隔符的长度
    WHILE j < len
    LOOP
      j:= INSTR(p_str, p_delimiter, i);  ---取得分隔符的位置
      IF j = 0 THEN
        j := len;
        str := SUBSTR (p_str, i);
        PIPE ROW (str);
        IF i >= len THEN
		 dbms_output.put_line('5');
          EXIT;
        END IF;
      ELSE
        str := SUBSTR (p_str, i, j - i);
        i := j + len1;
        PIPE ROW (str);
		 dbms_output.put_line('6');
      END IF;
    END LOOP;
      RETURN;
    END fn_split;


 function maskcheck(a_ipmask in varchar2, a_checkip in varchar2 := null)
 return varchar2 is
  ip   varchar2(64);  ----登录IP的二进制流
  cmp  varchar2(64);  ----IP_rang_list中的IP二进制流
  mask number;        ----mask的位数
  n    number;        ----/的位置,如1.0.0.255/8，则n=10
  numFlag number;     ----是否为数字字符串
  type ty_str_split IS TABLE OF VARCHAR2 (64);
  arr2 ty_str_split := ty_str_split(); ----arr2存放IP地址拆分后的数据

  begin
  if a_ipmask = a_checkip then   --输入IP和IP_RANGE_LIST中IP一致返回1
     dbms_output.put_line('7');
	return '1';
  end if;
  -----------取得网络IP，网络位数 begin--------------------------
  n := instr(a_ipmask, '/');
  if n > 0 then                    ---取得/前面的IP和子网网络位的位数
     mask := to_number(substr(a_ipmask, n + 1));
     if mask<0 or mask >32 then    ---子网网络位数不在0-32之间
        dbms_output.put_line('8');
	   return '0';
     end if;
     if n = 1 then  ---如果只有/,则返回
	  dbms_output.put_line('9');
        return '0';
     end if;
     ip := substr(a_ipmask, 1, n-1);
  else
     mask := 0;
     ip := a_ipmask;
	  dbms_output.put_line('10');
  end if;
  -----------取得网络IP，网络位数 end--------------------------

  -----------将IP根据分隔符.进行拆分，并判断是否符合要求 begin-------
  select column_value bulk collect into arr2 from table(fn_split(ip, '.'));
  if arr2.count <> 4 then ---如果IP不是10.164.130.108这样被.分割成四部分，则返回
   dbms_output.put_line('11');
     return 0;
  end if;
  ip := '';
  for z in 1..arr2.count loop  --ip转化为二进制
     numFlag := isNumber(arr2(z));
     if numFlag = 0 or arr2(z)<0 or arr2(z)>255 then  ----如果分割后的数据，每部分不在0-255之间，或不是数字字符串，则返回
	  dbms_output.put_line('12');
        return 0;
     end if;
     ip := ip || substr(lpad(nvl(d2b(to_number(arr2(z))),'0'),8,'0'),0,8) ;
  end loop;
  if a_checkip is null then
    if mask > 0 then
      ip :=  substr(ip,1,mask);
	   dbms_output.put_line('13');
    end if ;
    return ip;
  else
    cmp := maskcheck(a_checkip);
    if mask > 0 and substr(cmp,1,mask) = substr(ip,1,mask) then
	 dbms_output.put_line('14');
      return '1';
    else
	 dbms_output.put_line('15');
      return '0';
    end if;
  end if;
  -----------将IP根据分隔符.进行拆分，并判断是否符合要求 end-------
end maskcheck;

end pkg_MaskCheck;
$$