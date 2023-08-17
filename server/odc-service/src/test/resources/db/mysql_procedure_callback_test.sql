create procedure ${const:com.oceanbase.odc.service.db.util.OBMysqlCallProcedureCallBackTest.TEST_CASE_2} (
  IN `p1` int(11),
  IN `p2` int(11),
  OUT `p3` int(11))
begin
  set p3 = p1 + p2;
end;
$$
