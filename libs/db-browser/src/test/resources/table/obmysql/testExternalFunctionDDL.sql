create function `external_jar_func`()
  returns int
  PROPERTIES (
            	symbol = 'org.example.MyAdd',
            	type = 'ODPSJAR',
            	file = 'test_java_udf1'
            );