DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_RERATING" AS
  /*******************************************************************************
  作    者:裘学欣39824
  创建时间:2008-06-16  CCBS CHG V300R001C01B07
  修改时间:
  --------------------------------------------------------------------------------
  主要功能:SRS.FUNC.006.001 话单回收控制改造
  *******************************************************************************/

  /*******************************************************************************
  Function:       p_Distask(a_Taskid NUMBER，a_RowCount out NUMBER)
  Description:    回收子任务分配
  Input:          a_Taskid 回收任务编号
  Output:         a_RowCount 分配的用户个数
  Return:         N/A
  *******************************************************************************/
  PROCEDURE p_Distask(a_Taskid   NUMBER,
                      a_Rowcount OUT NUMBER) AS
    v_All_Rows  NUMBER(8); --回收用户表行数
    v_Billcycle NUMBER(8); --任务表帐务周期
    v_Taskcount NUMBER(3); --子任务个数
    v_Status    NUMBER(3); --任务状态
    v_Childid   NUMBER(3); --子任务编号
    v_Set_Rows  NUMBER(8); --已分配行数
    v_Avg_Rows  NUMBER(8); --平均分配行数
  BEGIN

    --按传入的回收任务编号检查回收用户表数据
    SELECT COUNT(1)
      INTO v_All_Rows
      FROM Rerating_User
     WHERE Taskid = a_Taskid
       AND Status = 0;

    IF v_All_Rows = 0 THEN
      a_Rowcount := 0;
	   dbms_output.put_line('1');
      RETURN;
    END IF;

    --取帐务周期和子任务编号
    SELECT Billcycle,
           Taskcount,
           Status
      INTO v_Billcycle,
           v_Taskcount,
           v_Status
      FROM Rerating_Task
     WHERE Taskid = a_Taskid;

    IF v_Status > 0 THEN
      a_Rowcount := 0;
	   dbms_output.put_line('2');
      RETURN;
    END IF;

    --按地区查询用户资料表中的主机节点，填写资料内存节点
    UPDATE Rerating_User
       SET Nodeid    = f_Get_Nodeid(Region),
           Billcycle = v_Billcycle,
           Childid   = NULL
     WHERE Taskid = a_Taskid
       AND Status = 0;

    v_Avg_Rows := Ceil(v_All_Rows / v_Taskcount);

    --按分组查询各地区的回收用户条数，均匀分配子任务
    v_Childid  := 1;
    v_Set_Rows := 0;
    a_Rowcount := 0;
    FOR C2 IN (SELECT Nodeid,
                      Region,
                      COUNT(1) Sub_Rows
                 FROM Rerating_User
                WHERE Taskid = a_Taskid
                  AND Status = 0
                  AND Childid IS NULL
                GROUP BY Nodeid,
                         Region) LOOP
      BEGIN
        WHILE C2.Sub_Rows > 0 LOOP
          BEGIN
            IF C2.Sub_Rows <= (v_Avg_Rows - v_Set_Rows) THEN
              UPDATE Rerating_User
                 SET Childid = v_Childid
               WHERE Taskid = a_Taskid
                 AND Status = 0
                 AND Childid IS NULL
                 AND Nodeid = C2.Nodeid
                 AND Region = C2.Region;
              v_Set_Rows  := v_Set_Rows + C2.Sub_Rows;
              a_Rowcount  := a_Rowcount + C2.Sub_Rows;
              C2.Sub_Rows := 0;
			   dbms_output.put_line('3');
            ELSE
              v_Set_Rows := v_Avg_Rows - v_Set_Rows;
              UPDATE Rerating_User
                 SET Childid = v_Childid
               WHERE Taskid = a_Taskid
                 AND Status = 0
                 AND Childid IS NULL
                 AND Nodeid = C2.Nodeid
                 AND Region = C2.Region
                 AND Rownum <= v_Set_Rows;
              IF v_Childid < v_Taskcount THEN
                v_Childid := v_Childid + 1;
				 dbms_output.put_line('4');
              END IF;
              C2.Sub_Rows := C2.Sub_Rows - v_Set_Rows;
              a_Rowcount  := a_Rowcount + v_Set_Rows;
              v_Set_Rows  := 0;
            END IF;
          END;
        END LOOP;
      END;
    END LOOP;
    RETURN;
  END;

  /*******************************************************************************
  Function:       p_Genparam(a_Taskid NUMBER， a_Param out Varchar2)
  Description:    根据回收任务和回收类型生成回收参数
  Input:          a_Taskid回收任务编号
  Output:         a_Param 回收参数
  Return:         N/A
  *******************************************************************************/
  PROCEDURE p_Genparam(a_Taskid NUMBER,
                       a_Param  OUT VARCHAR2) AS
    v_Rerating_Type Rerating_Type%ROWTYPE;
    v_Rerating_Task Rerating_Task%ROWTYPE;
    v_Isdelaycdr    VARCHAR2(2);
  BEGIN

    --取参数数据
    SELECT *
      INTO v_Rerating_Task
      FROM Rerating_Task
     WHERE Taskid = a_Taskid;

    SELECT *
      INTO v_Rerating_Type
      FROM Rerating_Type
     WHERE Typeid = v_Rerating_Task.Typeid
       AND Inuse = 1;

    --取常量8888
    SELECT VALUE
      INTO v_Isdelaycdr
      FROM Const_Def
     WHERE Const_Id = 8888;

    IF To_Number(v_Isdelaycdr) = 1
       AND v_Rerating_Type.Charging_Module IS NULL THEN
      a_Param := '';
	   dbms_output.put_line('5');
      RETURN;
    END IF;

    --组合回收参数
    a_Param := a_Taskid || '%c0%cbill_qm%c' || v_Rerating_Type.Eventid || '%c' ||
               v_Rerating_Type.Undup_Module || '%c' ||
               v_Rerating_Type.Check_Dup || '%c' ||
               v_Rerating_Type.Rollback_From || '%c' ||
               f_Get_Tablename(a_Taskid) || '%c%s%c' ||
               v_Rerating_Type.Clear_Cycle || '%c' ||
               v_Rerating_Type.Flow_Module || '%c' ||
               v_Rerating_Type.Stat_Module;

    IF To_Number(v_Isdelaycdr) = 1 THEN
      a_Param := a_Param || '%c' || v_Rerating_Type.Charging_Module;
	   dbms_output.put_line('6');
    END IF;

    IF v_Rerating_Type.Field_Prefix IS NOT NULL THEN
      a_Param := a_Param || '%cfield_prefix=' || v_Rerating_Type.Field_Prefix;
	   dbms_output.put_line('7');
    END IF;

    IF v_Rerating_Type.Format_Proc > 0 THEN
      a_Param := a_Param || '%cformat_proc=' || v_Rerating_Type.Format_Proc;
	   dbms_output.put_line('8');
    END IF;

    IF v_Rerating_Type.Copyattrflag = 1 THEN
      a_Param := a_Param || '%crollback_copy_usages=' ||
                 v_Rerating_Type.Copyattrflag;
				  dbms_output.put_line('9');
    END IF;

    RETURN;

  EXCEPTION
    WHEN OTHERS THEN
      a_Param := '';
      RETURN;
  END;

  /*******************************************************************************
  Function:       p_Genwhere(a_Taskid NUMBER，a_Childid NUMBER，
                  a_Nodeid NUMBER, a_Where out Varchar2)
  Description:    根据回收任务、回收子任务、回收用户生成回收条件
  Input:          a_Taskid 回收任务编号，a_Childid 回收子任务编号，a_Nodeid 回收节点
  Output:         a_Where 回收条件
  Return:         N/A
  *******************************************************************************/
  PROCEDURE p_Genwhere(a_Taskid  NUMBER,
                       a_Childid NUMBER,
                       a_Nodeid  NUMBER,
                       a_Where   IN OUT VARCHAR2) AS
    v_Rerating_Type Rerating_Type%ROWTYPE;
    v_Rerating_Task Rerating_Task%ROWTYPE;
    v_User_List     VARCHAR2(800) := '';
    v_Command       VARCHAR2(2000) := '';
    v_Billcycle     VARCHAR2(32) := '';
    v_Region        VARCHAR2(32) := '';
    v_Userid        VARCHAR2(32) := '';
    v_Starttime     VARCHAR2(32) := '';
    v_Userattr      VARCHAR2(128) := '';
    v_Batchid       NUMBER(10) := 0;
    v_Rowid         VARCHAR2(32) := '';
    v_Count         NUMBER(4) := 0;
    v_Isreal        BOOLEAN := FALSE;
    v_Batchregion   VARCHAR2(32) := 'Init';
  BEGIN
    SELECT *
      INTO v_Rerating_Task
      FROM Rerating_Task
     WHERE Taskid = a_Taskid;
    SELECT *
      INTO v_Rerating_Type
      FROM Rerating_Type
     WHERE Typeid = v_Rerating_Task.Typeid;

    IF a_Where = 'REAL' THEN
      v_Isreal := TRUE;
      SELECT MAX(Batchid),
             MAX(Command),
             MAX(ROWID)
        INTO v_Batchid,
             v_Command,
             v_Rowid
        FROM Rerating_Task_Log
       WHERE Taskid = a_Taskid
         AND Childid = a_Childid
         AND Status = 0
         AND Rownum = 1;
      IF v_Batchid IS NULL THEN
        SELECT Seq_Rerating_Batchid.NEXTVAL
          INTO v_Batchid
          FROM Dual;
		   dbms_output.put_line('10');
      ELSE
        UPDATE Rerating_Task_Log
           SET Status  = 2,
               Endtime = to_date('20201022','yyyymmdd')
         WHERE ROWID = Chartorowid(v_Rowid);
        COMMIT;
        a_Where := Lpad(v_Batchid, 10, 0) || ' ' || v_Command;
		 dbms_output.put_line('11');
        RETURN;
      END IF;
    END IF;
    IF v_Rerating_Task.Status = 2 THEN
      v_Batchid := 0;
      a_Where   := 'is over.';
	   dbms_output.put_line('12');
      GOTO Addpatchid;
    ELSE
      a_Where := '';
	   dbms_output.put_line('13');
    END IF;

    v_Billcycle := Af_Get_Token(v_Rerating_Type.Fieldname, ',');
    v_Region    := Af_Get_Token(v_Rerating_Type.Fieldname, ',');
    v_Userid    := Af_Get_Token(v_Rerating_Type.Fieldname, ',');
    v_Starttime := Af_Get_Token(v_Rerating_Type.Fieldname, ',');
    v_Userattr  := Af_Get_Token(v_Rerating_Type.Fieldname, ',');

    --取回收类型条件
    IF v_Rerating_Type.Rerating_Where IS NOT NULL THEN
      a_Where := v_Rerating_Type.Rerating_Where;
 dbms_output.put_line('14');
      --取回收任务条件
      IF v_Rerating_Task.Task_Where IS NOT NULL THEN
        a_Where := a_Where || ' and ' || v_Rerating_Task.Task_Where;
		 dbms_output.put_line('15');
      END IF;
    ELSE

      --取回收任务条件
      a_Where := v_Rerating_Task.Task_Where;
	   dbms_output.put_line('16');
    END IF;

    --处理测试设置
    IF v_Rerating_Task.Istest = 1 THEN
      IF a_Where IS NOT NULL THEN
        a_Where := a_Where || ' and rownum <=' || v_Rerating_Task.Testrows;
		 dbms_output.put_line('17');
      ELSE
        a_Where := 'rownum <=' || v_Rerating_Task.Testrows;
		 dbms_output.put_line('18');
      END IF;
    END IF;

    IF v_Userid IS NULL THEN
      v_Batchid := 1;
      a_Where   := 'is invalid, userid is not found.';
	   dbms_output.put_line('19');
      GOTO Addpatchid;
    END IF;

    IF v_Rerating_Task.Wheretype = 0 THEN

      --取用户列表
      FOR C1 IN (SELECT ROWID,
                        Region,
                        Userid,
                        Status,
                        Batchid,
                        Rownum
                   FROM Rerating_User
                  WHERE Taskid = a_Taskid
                    AND Childid = a_Childid
                    AND Nodeid = a_Nodeid
                    AND Status = 0
                    AND Batchid IS NULL
                    AND Rownum <= v_Rerating_Task.Batchuser
                  ORDER BY Nodeid,
                           Region,
                           Userid) LOOP

        IF v_Batchregion = 'Init' THEN
          v_Batchregion := C1.Region;
		   dbms_output.put_line('20');
        END IF;
        IF v_Batchregion <> C1.Region
           OR (v_Batchregion IS NULL AND C1.Region IS NOT NULL)
           OR (v_Batchregion IS NOT NULL AND C1.Region IS NULL) THEN
		   dbms_output.put_line('21');
          GOTO Endbatch;
        END IF;
        IF C1."Rownum" = 1 THEN
          v_User_List := '''' || C1.Userid || '''';
		  dbms_output.put_line('22');
        ELSE
          v_User_List := v_User_List || ',''' || C1.Userid || '''';
		  dbms_output.put_line('23');
        END IF;
        IF v_Isreal THEN
          UPDATE Rerating_User
             SET Batchid = v_Batchid
           WHERE ROWID = C1.ROWID;
		   dbms_output.put_line('24');
        END IF;
      END LOOP;

      <<endbatch>>
      IF v_Isreal THEN
	  dbms_output.put_line('25');
        COMMIT;
      END IF;

      IF v_User_List IS NOT NULL THEN
        v_User_List := v_Userid || ' in (' || v_User_List || ')';
		dbms_output.put_line('26');
      ELSE
        SELECT COUNT(1)
          INTO v_Count
          FROM Rerating_User
         WHERE Taskid = a_Taskid
           AND Billcycle = v_Rerating_Task.Billcycle
           AND Rownum = 1;

        IF v_Count = 0 THEN
          IF a_Childid = 0 THEN
            v_User_List := 'MOD(' || v_Userid || ', ' ||
                           v_Rerating_Task.Taskcount || ') = 0';
						   dbms_output.put_line('27');
          ELSE
            v_User_List := 'MOD(' || v_Userid || ', ' ||
                           v_Rerating_Task.Taskcount || ') = ' ||
                           (a_Childid - 1);
						   dbms_output.put_line('28');
          END IF;
        ELSE
          v_Batchid := 0;
          a_Where   := 'is not found.';
		  dbms_output.put_line('29');
          GOTO Addpatchid;
        END IF;
      END IF;

      IF v_User_List IS NOT NULL THEN
        IF v_Billcycle IS NOT NULL THEN
          v_User_List := v_Billcycle || '=''' || v_Rerating_Task.Billcycle ||
                         ''' and ' || v_User_List;
						 dbms_output.put_line('30');
        END IF;
        IF v_Region IS NOT NULL
           AND v_Batchregion IS NOT NULL
           AND v_Batchregion <> 'Init' THEN
          v_User_List := v_Region || '=''' || v_Batchregion || ''' and ' ||
                         v_User_List;
						 dbms_output.put_line('31');
        END IF;
      ELSE
        IF v_Billcycle IS NOT NULL THEN
          IF a_Where IS NOT NULL THEN
            a_Where := v_Billcycle || '=''' || v_Rerating_Task.Billcycle ||
                       ''' and ' || a_Where;
					   dbms_output.put_line('32');
          ELSE
            a_Where := v_Billcycle || '=''' || v_Rerating_Task.Billcycle || '''';
			dbms_output.put_line('33');
          END IF;
        END IF;
      END IF;
    ELSE
      IF v_Starttime IS NULL THEN
        v_Batchid := 1;
        a_Where   := 'is invalid, starttime is not found.';
		dbms_output.put_line('34');
        GOTO Addpatchid;
      END IF;

      SELECT COUNT(1)
        INTO v_Count
        FROM Rerating_User
       WHERE Taskid = a_Taskid
         AND Childid = a_Childid
         AND Nodeid = a_Nodeid
         AND Status = 0
         AND Rownum = 1;

      IF v_Count > 0 THEN
        IF v_Isreal THEN
          UPDATE Rerating_User
             SET Batchid = v_Batchid
           WHERE Taskid = a_Taskid
             AND Childid = a_Childid
             AND Nodeid = a_Nodeid
             AND Status = 0
             AND Rownum <= v_Rerating_Task.Batchuser;
          COMMIT;
          v_User_List := '(' || v_Region || ',' || v_Billcycle || ',' ||
                         v_Userid || ',' || v_Starttime || ',' || v_Userattr ||
                         ') in (select region,billcycle,userid,cdrtime,userattr from ' ||
                         'rerating_user where Batchid = ' || v_Batchid || ') ';
						 dbms_output.put_line('35');
        ELSE
          v_User_List := '(' || v_Region || ',' || v_Billcycle || ',' ||
                         v_Userid || ',' || v_Starttime || ',' || v_Userattr ||
                         ') in (select region,billcycle,userid,cdrtime,userattr from ' ||
                         'rerating_user where taskid = ' || a_Taskid ||
                         ' and childid = ' || a_Childid || ' and nodeid = ' ||
                         a_Nodeid || ' and rownum <= ' ||
                         v_Rerating_Task.Batchuser || ')';
						 dbms_output.put_line('36');
        END IF;
      ELSE
        v_Batchid := 0;
        a_Where   := 'is not found.';
		dbms_output.put_line('37');
        GOTO Addpatchid;
      END IF;
    END IF;

    IF a_Where IS NOT NULL THEN
      IF v_User_List IS NOT NULL THEN
        a_Where := v_User_List || ' and ' || a_Where;
		dbms_output.put_line('38');
      END IF;
    ELSE
      a_Where := v_User_List;
    END IF;

    <<addpatchid>>
    IF v_Isreal THEN
      a_Where := Lpad(v_Batchid, 10, 0) || ' " where ' || a_Where || '"';
	  dbms_output.put_line('39');
    ELSE
      a_Where := ' where ' || a_Where;
	  dbms_output.put_line('40');
    END IF;
    RETURN;
  END;

  /*******************************************************************************
  Function:       f_Get_Nodeid(a_Region NUMBER)
  Description:    取计费节点号函数
  Input:          a_Region 地区
  Output:         N/A
  Return:        节点号
  *******************************************************************************/
  FUNCTION f_Get_Nodeid(a_Region NUMBER) RETURN NUMBER AS
    v_Area   VARCHAR2(64);
    v_Region VARCHAR2(64);
  BEGIN
    FOR C1 IN (SELECT Node_Id,
                      Area_Id
                 FROM Entity_Mem_Region) LOOP
      BEGIN
        v_Area := C1.Area_Id;
        WHILE Length(v_Area) > 0 LOOP
          BEGIN
            v_Region := Af_Get_Token(v_Area, ',');
            IF a_Region = To_Number(v_Region) THEN
			dbms_output.put_line('41');
              RETURN C1.Node_Id;
            END IF;
          END;
        END LOOP;
      END;
    END LOOP;
    RETURN 0;
  EXCEPTION
    WHEN OTHERS THEN
      RETURN 0;
  END;

  /*******************************************************************************
  Function:       f_Get_Tablename(a_Taskid NUMBER)
  Description:    取回收话单表函数
  Input:          a_Taskid 回收任务编号
  Output:         N/A
  Return:        回收话单表名
  *******************************************************************************/
  FUNCTION f_Get_Tablename(a_Taskid NUMBER) RETURN VARCHAR2 AS
    v_Rerating_Type Rerating_Type%ROWTYPE;
    v_Rerating_Task Rerating_Task%ROWTYPE;
    v_Eventtype     Event_Store_Def%ROWTYPE;
    v_Source_Table  VARCHAR2(32);
  BEGIN
    --取参数数据
    SELECT *
      INTO v_Rerating_Task
      FROM Rerating_Task
     WHERE Taskid = a_Taskid;

    SELECT *
      INTO v_Rerating_Type
      FROM Rerating_Type
     WHERE Typeid = v_Rerating_Task.Typeid
       AND Inuse = 1;

    BEGIN
      SELECT *
        INTO v_Eventtype
        FROM Event_Store_Def
       WHERE Upper(Table_Name) = Upper(v_Rerating_Type.Cdrtable);
      IF v_Eventtype.Event_Id <> v_Rerating_Type.Eventid THEN
	  dbms_output.put_line('42');
        Raise_Application_Error(-20201,
                                'The Rerating_Type.eventid is not valid.');
      END IF;
      IF v_Eventtype.Cycle_Flag = 1 THEN
        v_Source_Table := v_Rerating_Type.Cdrtable || v_Rerating_Task.Billcycle;
		dbms_output.put_line('43');
      ELSIF v_Eventtype.Cycle_Flag = 2 THEN
        v_Source_Table := v_Rerating_Type.Cdrtable ||
                          Substr(v_Rerating_Task.Billcycle, 1, 6);
						  dbms_output.put_line('44');
      ELSE
        v_Source_Table := v_Rerating_Type.Cdrtable;
		dbms_output.put_line('45');
      END IF;
    EXCEPTION
      WHEN No_Data_Found THEN
	  dbms_output.put_line('46');
        v_Source_Table := v_Rerating_Type.Cdrtable;
    END;
    RETURN v_Source_Table;
  END;

  /*******************************************************************************
  Function:       f_Get_Operator()
  Description:    取操作员函数
  Input:          N/A
  Output:         N/A
  Return:        操作员
  *******************************************************************************/
  FUNCTION f_Get_Operator RETURN VARCHAR2 AS
  BEGIN
dbms_output.put_line('48');
    RETURN g_Operator;
  END;

  /*******************************************************************************
  Function:       p_Set_Operator(Operator VARCHAR2)
  Description:    设置操作员函数
  Input:          操作员
  Output:         N/A
  Return:         N/A
  *******************************************************************************/
  PROCEDURE p_Set_Operator(a_Operator VARCHAR2) AS
  BEGIN
    g_Operator := a_Operator;
	dbms_output.put_line('47');
  END;
END;
$$