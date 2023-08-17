DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_TEMPALTE_DEF" is

PROCEDURE AP_TempalteDataImport(p_Template_ID IN NUMBER, --模板编号
                                p_Tempalte_Type IN varchar2,
                                p_Rule_ID   IN VARCHAR2,
                                n_Success out number,
                                s_OutInfo out varchar2)is
  v_TariffType number := 0; --该参数为识别资费相关模板用
BEGIN
   if p_Tempalte_Type = 'TARIFF_PLAN'  --资费政策模板
   THEN
       v_TariffType := 1;
	   dbms_output.put_line('1');
   ELSIF p_Tempalte_Type = 'TARIFF_PLAN_ITEM'  --资费政策项模板
   THEN
       v_TariffType := 2;
	   dbms_output.put_line('2');
   ELSIF p_Tempalte_Type = 'TARIFF_SCHEMA'   --资费模式模板
   THEN
       v_TariffType := 3;
	   dbms_output.put_line('3');
   ELSIF p_Tempalte_Type = 'TARIFF_ITEM'  -- 资费政策项模板，实际是不存在的
   THEN
       v_TariffType := 4;
	   dbms_output.put_line('4');
   END IF;
   IF v_TariffType <> 0 THEN
       PKG_TEMPALTE_DEF.AP_TariffExport(p_Template_ID,v_TariffType, p_Rule_ID);
       n_Success := 1;
       s_OutInfo := 'Seccess';
	   dbms_output.put_line('5');
   ELSE
       dbms_output.put_line('The params input have wrong');
       s_OutInfo := 'Input param wrong ';
       n_Success := 0;
	   dbms_output.put_line('6');
   END IF;
   exception
        when others then
            dbms_output.put_line( 'Import data fail' );
            s_OutInfo := 'Unknow wrong';
            n_Success := 0;
			dbms_output.put_line('7');
END AP_TempalteDataImport;

PROCEDURE Ap_CreateTemplateCatalogId(n_Tree_Node_Id IN OUT NUMBER) IS
    v_Begid  NUMBER(8);
    v_Endid  NUMBER(8);
    v_Maxid  NUMBER(8);
    v_Count  NUMBER(8);
    v_Amount NUMBER(8);
BEGIN
    SELECT MAX(Tree_Node_Id) INTO v_Maxid FROM Template_Catalog;
    IF (v_Maxid = 99999999) THEN
        n_Tree_Node_Id := 0;
		dbms_output.put_line('8');
        RETURN;
    ELSIF (v_Maxid IS NULL) THEN
        n_Tree_Node_Id := 0;
		dbms_output.put_line('9');
        RETURN;
    END IF;
    SELECT COUNT(Tree_Node_Id)
      INTO v_Count
      FROM Template_Catalog
     WHERE Tree_Node_Id > 0;
    IF (v_Maxid = v_Count) THEN
        n_Tree_Node_Id := v_Maxid + 1;
		dbms_output.put_line('10');
        RETURN;
    END IF;
    v_Begid  := 1;
    v_Endid  := v_Maxid;
    v_Count  := 0;
    v_Amount := 0;
    LOOP
        SELECT COUNT(Tree_Node_Id)
          INTO v_Count
          FROM Template_Catalog
         WHERE Tree_Node_Id >= v_Begid
           AND Tree_Node_Id <= v_Endid;
        IF (v_Count = 1) THEN
            SELECT COUNT(Tree_Node_Id)
              INTO v_Count
              FROM Template_Catalog
             WHERE Tree_Node_Id = v_Begid;
            IF (v_Count = 0) THEN
                n_Tree_Node_Id := v_Begid;
				dbms_output.put_line('11');
            ELSE
                n_Tree_Node_Id := v_Endid;
				dbms_output.put_line('12');
            END IF;
            EXIT;
        END IF;
        v_Amount := v_Endid - v_Begid + 1;
        IF (v_Amount > v_Count) THEN
            v_Maxid := v_Endid;
            v_Endid := v_Begid + Trunc(v_Amount / 2);
			dbms_output.put_line('13');
        ELSIF (v_Amount = v_Count) THEN
            v_Begid := v_Endid;
            v_Endid := v_Maxid;
			dbms_output.put_line('14');
        ELSE
            n_Tree_Node_Id := -1;
			dbms_output.put_line('15');
            EXIT;
        END IF;
    END LOOP;
END Ap_CreateTemplateCatalogId;


PROCEDURE AP_CreateTemplateInfoId(n_template_id in out number) IS
    v_begid  NUMBER(8);
    v_endid  NUMBER(8);
    v_maxid  NUMBER(8);
    v_count  NUMBER(8);
    v_amount NUMBER(8);
BEGIN
    SELECT MAX(template_id) INTO v_maxid FROM template_info;
    IF (v_maxid = 99999999) THEN
        n_template_id := 0;
		dbms_output.put_line('16');
        RETURN;
    ELSIF (v_maxid IS null) THEN
        n_template_id := 1;
		dbms_output.put_line('17');
        RETURN;
    END IF;
    SELECT COUNT(template_id) INTO v_count FROM template_info;
    IF (v_maxid = v_count) THEN
        n_template_id := v_maxid + 1;
		dbms_output.put_line('18');
        RETURN;
    END IF;
    v_begid  := 1;
    v_endid  := v_maxid;
    v_count  := 0;
    v_amount := 0;
    LOOP
        SELECT COUNT(template_id)
          INTO v_count
          FROM template_info
         WHERE template_id >= v_begid
           AND template_id <= v_endid;
        IF (v_count = 1) THEN
            SELECT COUNT(template_id)
              INTO v_count
              FROM template_info
             WHERE template_id = v_begid;
            IF (v_count = 0) THEN
                n_template_id := v_begid;
				dbms_output.put_line('19');
            ELSE
                n_template_id := v_endid;
				dbms_output.put_line('20');
            END IF;
            EXIT;
        END IF;
        v_amount := v_endid - v_begid + 1;
        IF (v_amount > v_count) THEN
            v_maxid := v_endid;
            v_endid := v_begid + trunc(v_amount / 2);
			dbms_output.put_line('21');
        ELSIF (v_amount = v_count) THEN
            v_begid := v_endid;
            v_endid := v_maxid;
			dbms_output.put_line('22');
        ELSE
            n_template_id := -1;
			dbms_output.put_line('23');
            EXIT;
        END IF;
    END LOOP;
END AP_CreateTemplateInfoId;



PROCEDURE AP_TariffPlan(p_TariffPlanId IN NUMBER, --资费政策编号
                                          p_TemplateId   IN NUMBER) IS
  v_Flag NUMBER(1) := 1; --表中是否存在数据（0：不存在；1：已存在；）
begin
  SELECT COUNT(tariff_plan_id)
    INTO v_flag
    FROM trd_tariff_plan
   WHERE tariff_plan_id = p_TariffPlanId
     AND template_id = p_TemplateId;
  IF (v_Flag = 0) THEN
    INSERT INTO trd_tariff_plan
  (tariff_plan_id, tariff_plan_name, is_recursive, precedence, process_before, process_after, plantype, note, precedence_accdisc, priceplantype, region, template_id)
      SELECT t.tariff_plan_id, t.tariff_plan_name, t.is_recursive, t.precedence, t.process_before, t.process_after, t.plantype, t.note, t.precedence_accdisc, t.priceplantype, t.region, p_TemplateId
        FROM tariff_plan t
       WHERE tariff_plan_id = p_TariffPlanId;
	   dbms_output.put_line('24');
  END IF;
  FOR cur IN (SELECT *
                FROM tariff_plan_item
               WHERE tariff_plan_id = p_TariffPlanId) LOOP
    --DBMS_OUTPUT.PUT_LINE('AP_TariffPlan::Beg************************');
    --DBMS_OUTPUT.PUT_LINE('AP_TariffPlan::trd_tariff_plan_item:tariff_plan_item_sn = '||cur.tariff_plan_item_sn);
    SELECT COUNT(tariff_plan_id)
      INTO v_flag
      FROM trd_tariff_plan_item
     WHERE tariff_plan_item_sn = cur.tariff_plan_item_sn
     AND template_id = p_TemplateId;
    --DBMS_OUTPUT.PUT_LINE('AP_TariffPlan::trd_tariff_plan_item:v_flag='||v_flag);
    IF (v_flag = 0) THEN
      INSERT INTO trd_tariff_plan_item
        (tariff_plan_id,
         calctype,
         fee_id,
         tariff_schema_id,
         charging_event_id,
         precedence,
         applytime,
         expiretime,
         apply_method,
         discount_type,
         base_fee_id,
         discount_fee_id,
         charging_cond,
         switch_method,
         switch_unit,
         g_charging_cond,
         billcode,
         select_method,
         tariff_plan_item_sn,
         tariff_plan_item_name,
         serv_id,
         template_id)
      VALUES
        (cur.tariff_plan_id,
         cur.calctype,
         cur.fee_id,
         cur.tariff_schema_id,
         cur.charging_event_id,
         cur.precedence,
         cur.applytime,
         cur.expiretime,
         cur.apply_method,
         cur.discount_type,
         cur.base_fee_id,
         cur.discount_fee_id,
         cur.charging_cond,
         cur.switch_method,
         cur.switch_unit,
         cur.g_charging_cond,
         cur.billcode,
         cur.select_method,
         cur.tariff_plan_item_sn,
         cur.tariff_plan_item_name,
         cur.serv_id,
         p_TemplateId);
		 dbms_output.put_line('25');
    END IF;
    --DBMS_OUTPUT.PUT_LINE('AP_TariffPlan::AP_TariffSchema('|| cur.tariff_schema_id ||')');
    AP_TariffSchema(p_TariffSchemaId => cur.tariff_schema_id,
                    p_TemplateId     => p_TemplateId);
    --DBMS_OUTPUT.PUT_LINE('AP_TariffPlan::End************************');
  END LOOP;
END AP_TariffPlan;

  /*
  call AP_TariffPlan(1);
  commit;
  */

PROCEDURE AP_TariffSchema(p_TariffSchemaId IN NUMBER, --资费模式编号
                                            p_TemplateId     IN NUMBER) IS
  v_shemaids varchar2(2048) := ''; --工作变量，保存资费模式号 以"，"号分割
  v_sql      varchar2(3000);

  PROCEDURE Get_Schema_IDS(schema_id number) AS
  BEGIN
    FOR shemaid_row IN (SELECT tariff_id
                          FROM tariff_item
                         WHERE subtariff_type = 2
                           AND tariff_schema_id = schema_id
                         ORDER BY tariff_id) LOOP
      --首先判断是否已经存在，防止重复增加的情况
      IF (INSTR(v_shemaids, shemaid_row.tariff_id, 1) = 0) THEN
        v_shemaids := v_shemaids || ',''' || shemaid_row.TARIFF_ID || '''';
        Get_Schema_IDS(shemaid_row.tariff_id);
		dbms_output.put_line('26');
      END IF;
    END LOOP;
  END; --内部函数 Get_Schema_IDS 结束
BEGIN
  v_shemaids := p_TariffSchemaId;

  Get_Schema_IDS(p_TariffSchemaId);

  --DBMS_OUTPUT.PUT_LINE(v_shemaids);
  v_sql := 'DELETE FROM trd_tariff_schema WHERE tariff_schema_id in (' ||
           REPLACE(v_shemaids, '''') || ')';
  --DBMS_OUTPUT.PUT_LINE(v_sql);
  EXECUTE IMMEDIATE v_sql;
  v_sql := 'INSERT INTO trd_tariff_schema(tariff_schema_id, tariff_name, tariff_type, fieldcount, field_def, match_order, match_type, apply_method, refid, discount_fee_id, g_field_def, round_method, round_scale, ref_offset, event_id, billcode_order, createtime, region, template_id) SELECT tariff_schema_id, tariff_name, tariff_type, fieldcount, field_def, match_order, match_type, apply_method, refid, discount_fee_id, g_field_def, round_method, round_scale, ref_offset, event_id, billcode_order, createtime, region, ' || p_TemplateId ||
           ' FROM tariff_schema t WHERE tariff_schema_id in (' ||
           v_shemaids || ')';
  EXECUTE IMMEDIATE v_sql;
  v_sql := 'DELETE FROM trd_tariff_item WHERE tariff_schema_id in (' ||
           v_shemaids || ')';
  EXECUTE IMMEDIATE v_sql;
  v_sql := 'INSERT INTO trd_tariff_item (tariff_schema_id, applytime, expiretime, tariff_criteria, subtariff_type, tariff_id, ratio, ratetype, param_string, precedence, expr_id, g_param, is_dynamic, g_criteria, billcode, tariff_item_sn, tariff_item_name, template_id) SELECT tariff_schema_id, applytime, expiretime, tariff_criteria, subtariff_type, tariff_id, ratio, ratetype, param_string, precedence, expr_id, g_param, is_dynamic, g_criteria, billcode, tariff_item_sn, tariff_item_name, ' || p_TemplateId ||
           ' FROM tariff_item t WHERE tariff_schema_id in (' || v_shemaids || ')';
  EXECUTE IMMEDIATE v_sql;
  dbms_output.put_line('27');
END AP_TariffSchema;
/*
  CALL AP_TariffSchema(4,10000);
  COMMIT;
  */

PROCEDURE AP_TariffExport(p_TemplateId IN NUMBER, --模板编号
                                            p_TariffType IN NUMBER DEFAULT 1, --资费类型（1：资费政策；2：资费政策项；3：资费模式；4：资费模式项）
                                            p_TariffId   IN VARCHAR) AS
  v_Flag       NUMBER(1) := 1; --表中是否存在数据（0：不存在；1：已存在；）
  v_TariffId   tariff_schema.tariff_schema_id%TYPE; --资费模式编号
  v_TariffType tariff_item.subtariff_type%TYPE; --下级资费类型（2：资费模式；）
BEGIN
  CASE p_TariffType
    WHEN 1 THEN
      --资费政策
      PKG_TEMPALTE_DEF.AP_TariffPlan(TO_NUMBER(p_TariffId), p_TemplateId);
	  dbms_output.put_line('28');
    WHEN 2 THEN
      --资费政策项
      SELECT COUNT(tariff_plan_item_sn)
        INTO v_flag
        FROM trd_tariff_plan_item
       WHERE tariff_plan_item_sn = p_TariffId
         AND template_id = p_TemplateId;
      IF (v_Flag = 0) THEN
        /*INSERT INTO trd_tariff_plan_item
          SELECT t.*, p_TemplateId
            FROM tariff_plan_item t
           WHERE tariff_plan_item_sn = p_TariffId;*/
		  INSERT INTO trd_tariff_plan_item
    (TARIFF_PLAN_ID,
     CALCTYPE,
     FEE_ID,
     TARIFF_SCHEMA_ID,
     CHARGING_EVENT_ID,
     PRECEDENCE,
     APPLYTIME,
     EXPIRETIME,
     APPLY_METHOD,
     DISCOUNT_TYPE,
     BASE_FEE_ID,
     DISCOUNT_FEE_ID,
     CHARGING_COND,
     SWITCH_METHOD,
     SWITCH_UNIT,
     G_CHARGING_COND,
     BILLCODE,
     SELECT_METHOD,
     TARIFF_PLAN_ITEM_NAME,
     SERV_ID,
     TARIFF_PLAN_ITEM_SN,
     TEMPLATE_ID)
    SELECT t.TARIFF_PLAN_ID,
           t.CALCTYPE,
           t.FEE_ID,
           t.TARIFF_SCHEMA_ID,
           t.CHARGING_EVENT_ID,
           t.PRECEDENCE,
           t.APPLYTIME,
           t.EXPIRETIME,
           t.APPLY_METHOD,
           t.DISCOUNT_TYPE,
           t.BASE_FEE_ID,
           t.DISCOUNT_FEE_ID,
           t.CHARGING_COND,
           t.SWITCH_METHOD,
           t.SWITCH_UNIT,
           t.G_CHARGING_COND,
           t.BILLCODE,
           t.SELECT_METHOD,
           t.TARIFF_PLAN_ITEM_NAME,
           t.SERV_ID,
           t.TARIFF_PLAN_ITEM_SN,
           p_TemplateId
      FROM tariff_plan_item t
     WHERE t.tariff_plan_item_sn = p_TariffId;
	 dbms_output.put_line('29');
      END IF;
      SELECT tariff_schema_id
        INTO v_TariffId
        FROM tariff_plan_item
       WHERE tariff_plan_item_sn = p_TariffId;
      PKG_TEMPALTE_DEF.AP_TariffSchema(v_TariffId, p_TemplateId);
    WHEN 3 THEN
      --资费模式
      PKG_TEMPALTE_DEF.AP_TariffSchema(TO_NUMBER(p_TariffId), p_TemplateId);
	  dbms_output.put_line('30');
    WHEN 4 THEN
      --资费模式项
      SELECT COUNT(tariff_item_sn)
        INTO v_flag
        FROM trd_tariff_item
       WHERE tariff_item_sn = p_TariffId
         AND template_id = p_TemplateId;
      IF (v_Flag = 0) THEN
        /*INSERT INTO trd_tariff_item
          SELECT t.*, p_TemplateId
            FROM tariff_item t
           WHERE tariff_item_sn = p_TariffId;*/
		INSERT INTO trd_tariff_item (tariff_schema_id, applytime, expiretime, tariff_criteria, subtariff_type, tariff_id, ratio, ratetype, param_string, precedence, expr_id, g_param, is_dynamic, g_criteria, billcode, tariff_item_sn, tariff_item_name, template_id) SELECT tariff_schema_id, applytime, expiretime, tariff_criteria, subtariff_type, tariff_id, ratio, ratetype, param_string, precedence, expr_id, g_param, is_dynamic, g_criteria, billcode, tariff_item_sn, tariff_item_name, p_TemplateId
        FROM tariff_item t WHERE tariff_item_sn = p_TariffId;
		dbms_output.put_line('31');
      END IF;
      SELECT subtariff_type, tariff_schema_id
        INTO v_TariffType, v_TariffId
        FROM tariff_item
       WHERE tariff_item_sn = p_TariffId;
      IF (v_TariffType = 2) THEN
        --是否为资费类型
        --DBMS_OUTPUT.PUT_LINE('AP_TariffExport::tariff_plan_item:subtariff_type=2');
        PKG_TEMPALTE_DEF.AP_TariffSchema(v_TariffId, p_TemplateId);
		dbms_output.put_line('32');
      END IF;
  END CASE;
END AP_TariffExport;



end PKG_TEMPALTE_DEF;
$$