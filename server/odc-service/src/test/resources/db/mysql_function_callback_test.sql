create function ${const:com.oceanbase.odc.service.db.util.OBMysqlCallFunctionCallBackTest.TEST_CASE_1} (
  p1 int,
  p2 int) returns int
begin
  return p1+p2;
end;
$$

create function ${const:com.oceanbase.odc.service.db.util.OBMysqlCallFunctionCallBackTest.TEST_CASE_2} (
  p0 int,
  p1 int,
  p2 varchar(20)) returns int
begin
  if p2 is null then
    return p0;
  end if;
  if p2 = '' then
    return p1;
  end if;
  return p0+p1;
end;
$$

create function ${const:com.oceanbase.odc.service.db.util.OBMysqlCallFunctionCallBackTest.TEST_CASE_3} (
  p0 varchar(20)) returns varchar(20)
begin
  return p0;
end;
$$