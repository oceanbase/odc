DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "TU" IS
  -- Private type declarations
  --type <TypeName> is <Datatype>;
  -- Private constant declarations
  --<ConstantName> constant <Datatype> := <Value>;
  -- Private variable declarations
  --<VariableName> <Datatype>;
  -- Function and procedure implementations
  /*function <FunctionName>(<Parameter> <Datatype>) return <Datatype> is
  <LocalVariable> <Datatype>;
  begin
  <Statement>;
  return(<Result>);
  end;
  */
  PROCEDURE add_default_plan_item(a_plan_id NUMBER) IS
  BEGIN
    INSERT INTO tariff_plan_item
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
       g_charging_cond)
    VALUES
      (a_plan_id,
       1,
       1,
       388,
       7,
       5,
       to_date('01-01-2000', 'dd-mm-yyyy'),
       NULL,
       2,
       6,
       0,
       0,
       ' ',
       2,
       60,
       ' ');
    COMMIT;
	dbms_output.put_line('1');
  END;

  PROCEDURE copy_remind_limit_para(from_plan_id NUMBER, to_plan_id NUMBER) IS
    v_cnt NUMBER;
    v_paraid remind_limit_para.paraid%type;
  BEGIN
    v_cnt := 0;

    SELECT COUNT(*)
      INTO v_cnt
      FROM remind_limit_para a
     WHERE a.tariffid = to_plan_id;

    IF v_cnt > 0
    THEN
	dbms_output.put_line('2');
      raise_application_error(-20101,
                              '复制的计费事件在目标库的REMIND_LIMIT_PARA有定义');
      RETURN;
    END IF;
    select max(paraid) into v_paraid from remind_limit_para;
    INSERT INTO remind_limit_para
      ( paraid,
        region,
        tariffid,
        attrid,
        attrtype,
        limitvalue,
        tariffname,
        stdfee,
        templateno,
        availtime,
        expiretime,
        rate,
        logicexpr,
        reminddefault,
        stdfree,
        halffree,
        halflimit,
        remindattrvalue,
        rtcctemplateno,
        batch_id,
        remind_type,
        expr_cond,
        g_expr_cond,
        g_limitvalue,
        g_stdfree)
      SELECT  v_paraid+rownum,
              region,
              to_plan_id,
              attrid,
              attrtype,
              limitvalue,
              tariffname,
              stdfee,
              templateno,
              availtime,
              expiretime,
              rate,
              logicexpr,
              reminddefault,
              stdfree,
              halffree,
              halflimit,
              remindattrvalue,
              rtcctemplateno,
              batch_id,
              remind_type,
              expr_cond,
              g_expr_cond,
              g_limitvalue,
              g_stdfree
        FROM remind_limit_para a
       WHERE a.tariffid = from_plan_id;
  END;

  PROCEDURE copy_plan_items(a_from_plan_id NUMBER, a_to_plan_id NUMBER) IS
  BEGIN
    INSERT INTO tariff_plan_item
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
       select_method)
      SELECT a_to_plan_id,
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
             select_method
        FROM tariff_plan_item
       WHERE tariff_plan_id = a_from_plan_id;
    COMMIT;
	dbms_output.put_line('3');
  END;
  PROCEDURE copy_schema(a_from_schema_id NUMBER, a_to_schema_id NUMBER) IS
  BEGIN
    DELETE FROM tariff_schema
     WHERE tariff_schema_id = a_to_schema_id
       AND TRIM(tariff_name) IS NULL;
    INSERT INTO tariff_schema
      (tariff_schema_id,
        tariff_name,
        tariff_type,
        fieldcount,
        field_def,
        match_order,
        match_type,
        apply_method,
        refid,
        discount_fee_id,
        g_field_def,
        round_method,
        round_scale,
        ref_offset,
        event_id,
        billcode_order,
        createtime,
        region)
      SELECT a_to_schema_id,
        tariff_name,
        tariff_type,
        fieldcount,
        field_def,
        match_order,
        match_type,
        apply_method,
        refid,
        discount_fee_id,
        g_field_def,
        round_method,
        round_scale,
        ref_offset,
        event_id,
        billcode_order,
        createtime,
        region
        FROM tariff_schema
       WHERE tariff_schema_id = a_from_schema_id;
    --values (200, '费用 (0.20元/分钟)', 1, 0, null, 2, 1, 2, 0, 0, null, 0, 0, 0, null);
    DELETE FROM tariff_item WHERE tariff_schema_id = a_to_schema_id;
    INSERT INTO tariff_item
      (tariff_schema_id,
       applytime,
       expiretime,
       tariff_criteria,
       subtariff_type,
       tariff_id,
       ratio,
       ratetype,
       param_string,
       precedence,
       expr_id,
       g_param,
       is_dynamic,
       g_criteria,
       TARIFF_ITEM_NAME)
      SELECT a_to_schema_id,
             applytime,
             expiretime,
             tariff_criteria,
             subtariff_type,
             tariff_id,
             ratio,
             ratetype,
             param_string,
             precedence,
             expr_id,
             g_param,
             is_dynamic,
             g_criteria,
             TARIFF_ITEM_NAME
        FROM tariff_item
       WHERE tariff_schema_id = a_from_schema_id;
	   dbms_output.put_line('4');
    --values (200, to_date('01-01-2001', 'dd-mm-yyyy'), null, ' ', 1, 86, 1, ' ', null, 5, null, null, 0, ' ');
    COMMIT;
  END;
  /* Formatted on 2005/10/24 15:52 (Formatter Plus v4.8.6) */
  PROCEDURE copy_month_priprivilege(a_from_plan_id NUMBER,
                                    a_to_plan_id   NUMBER) IS
  BEGIN
    INSERT INTO privilege_new_month_callfee
      ( hregion,
        itemname,
        feetype,
        value,
        freevolume,
        attr_id,
        billingcode,
        freetype,
        halftype,
        billifstop,
        calctype,
        specialtype,
        startcycle,
        endcycle,
        group_flag,
        group_share_flag,
        cycle_count,
        entity_id,
        process_type,
        ratioattr_id,
        present_type)
      SELECT  hregion,
              itemname,
              feetype,
              value,
              freevolume,
              attr_id,
              a_to_plan_id,
              freetype,
              halftype,
              billifstop,
              calctype,
              specialtype,
              startcycle,
              endcycle,
              group_flag,
              group_share_flag,
              cycle_count,
              entity_id,
              process_type,
              ratioattr_id,
              present_type
        FROM privilege_new_month_callfee
       WHERE billingcode = a_from_plan_id;
      insert into privilege_usage_def
           (hregion,
            billingcode,
            itemname,
            freevolume,
            attr_id,
            freetype,
            startcycle,
            endcycle,
            entity_id,
            share_entity_id,
            share_attrid,
            limit_attrid,
            limit_entity,
            limit_type,
            halftype)
     select hregion,
            a_to_plan_id,
            itemname,
            freevolume,
            attr_id,
            freetype,
            startcycle,
            endcycle,
            entity_id,
            share_entity_id,
            share_attrid,
            limit_attrid,
            limit_entity,
            limit_type,
            halftype from privilege_usage_def a
      where billingcode=a_from_plan_id;
	  dbms_output.put_line('5');
    COMMIT;
  END;
  PROCEDURE copy_schema2(a_from_schema_id NUMBER, a_to_schema_id NUMBER) IS
  BEGIN
    DELETE FROM tariff_schema WHERE tariff_schema_id = a_to_schema_id;
    INSERT INTO tariff_schema
      (tariff_schema_id,
       tariff_name,
       tariff_type,
       fieldcount,
       field_def,
       match_order,
       match_type,
       apply_method,
       refid,
       discount_fee_id,
       g_field_def,
       round_method,
       round_scale,
       ref_offset,
       event_id)
      SELECT a_to_schema_id,
             tariff_name,
             tariff_type,
             fieldcount,
             field_def,
             match_order,
             match_type,
             apply_method,
             refid,
             discount_fee_id,
             g_field_def,
             round_method,
             round_scale,
             ref_offset,
             event_id
        FROM tariff_schema
       WHERE tariff_schema_id = a_from_schema_id;
    --values (200, '费用 (0.20元/分钟)', 1, 0, null, 2, 1, 2, 0, 0, null, 0, 0, 0, null);
    DELETE FROM tariff_item WHERE tariff_schema_id = a_to_schema_id;
    INSERT INTO tariff_item
      (tariff_schema_id,
       applytime,
       expiretime,
       tariff_criteria,
       subtariff_type,
       tariff_id,
       ratio,
       ratetype,
       param_string,
       precedence,
       expr_id,
       g_param,
       is_dynamic,
       g_criteria)
      SELECT a_to_schema_id,
             applytime,
             expiretime,
             tariff_criteria,
             subtariff_type,
             tariff_id,
             ratio,
             ratetype,
             param_string,
             precedence,
             expr_id,
             g_param,
             is_dynamic,
             g_criteria
        FROM tariff_item
       WHERE tariff_schema_id = a_from_schema_id;
    --values (200, to_date('01-01-2001', 'dd-mm-yyyy'), null, ' ', 1, 86, 1, ' ', null, 5, null, null, 0, ' ');
    COMMIT;
	dbms_output.put_line('6');
  END;
  FUNCTION new_cdrtype(cdrtype VARCHAR2) RETURN VARCHAR2 IS
  BEGIN
    IF cdrtype = '01'
    THEN
	dbms_output.put_line('7');
      RETURN 'O';
    ELSIF cdrtype = '02'
    THEN
	dbms_output.put_line('8');
      RETURN 'T';
    ELSIF cdrtype = '03'
    THEN
	dbms_output.put_line('9');
      RETURN 'F';
    ELSE
	dbms_output.put_line('10');
      RETURN NULL;
    END IF;
  END;
  FUNCTION new_calltype(callflag VARCHAR2) RETURN VARCHAR2 IS
  BEGIN
    IF callflag = '000'
    THEN
	dbms_output.put_line('11');
      RETURN NULL;
    ELSIF callflag = 'L00'
    THEN
	dbms_output.put_line('12');
      RETURN '0|1';
    ELSIF callflag = 'L01'
    THEN
	dbms_output.put_line('13');
      RETURN '21|22';
    ELSIF callflag = 'L02'
    THEN
	dbms_output.put_line('14');
      RETURN '23';
    ELSE
	dbms_output.put_line('15');
      RETURN NULL;
    END IF;
  END;
  FUNCTION get_ratecode(a_rate NUMBER, a_unit NUMBER) RETURN NUMBER IS
    code NUMBER := 0;
  BEGIN
    --  select rate_code into code from tcl_rate_dict where rate=a_rate and unit=a_unit;
    --  return code;
    --exception
    --    when others then
	dbms_output.put_line('16');
    RETURN NULL;
  END;
  FUNCTION correct_date(a_date DATE) RETURN DATE IS
  BEGIN
    IF a_date > to_date('20370101', 'yyyymmdd')
    THEN
	dbms_output.put_line('17');
      RETURN to_date('20370101', 'yyyymmdd');
    ELSE
	dbms_output.put_line('18');
      RETURN a_date;
    END IF;
  END;
  PROCEDURE copy_usage_attr(a_from_entityid NUMBER, a_from_attr NUMBER, a_to_attr NUMBER) IS
  BEGIN
    INSERT INTO entity_attr_def
      (entity_id,
       attr_id,
       attr_name,
       attr_type,
       multivalue,
       isunique,
       isresident,
       attr_class,
       datatype,
       length,
       scale,
       expire_days)
      SELECT entity_id,
             a_to_attr,
             attr_name,
             attr_type,
             multivalue,
             isunique,
             isresident,
             attr_class,
             datatype,
             length,
             scale,
             expire_days
        FROM entity_attr_def
       WHERE entity_id = a_from_entityid
         AND attr_id = a_from_attr;
    INSERT INTO total_usage_attr_def
      (entity_id, attr_id, usage_type)
      SELECT entity_id, a_to_attr, usage_type
        FROM total_usage_attr_def
       WHERE entity_id = a_from_entityid
         AND attr_id = a_from_attr;
    INSERT INTO free_usage_attr_def
      (entity_id,
       attr_id,
       fixed_free_usage,
       free_limit,
       free_type)
      SELECT entity_id,
             a_to_attr,
             fixed_free_usage,
             free_limit,
             free_type
        FROM free_usage_attr_def
       WHERE entity_id = a_from_entityid
         AND attr_id = a_from_attr;
    INSERT INTO base_attr_def
      ( entity_id,
        attr_id,
        base_attr_type,
        base_attr_id,
        charging_event_id,
        call_usage_cond,
        round_unit,
        round_type,
        g_call_usage_cond,
        seqno,
        convert_unit)
      SELECT  entity_id,
              attr_id,
              base_attr_type,
              base_attr_id,
              charging_event_id,
              call_usage_cond,
              round_unit,
              round_type,
              g_call_usage_cond,
              (select max(seqno)+1 from base_attr_def b where b.entity_id=a_from_entityid),
              convert_unit
        FROM base_attr_def
       WHERE entity_id = a_from_entityid
         AND attr_id = a_from_attr;
    COMMIT;
	dbms_output.put_line('19');
  END;
  PROCEDURE ap_copy_custinfo_def(a_from_entityid NUMBER,a_from_attr NUMBER, a_count NUMBER) IS
    v_cnt NUMBER := a_count;
    a_to_attr entity_attr_def.attr_id%type;
  BEGIN
    select max(attr_id)+1 into a_to_attr from entity_attr_def b
     where b.entity_id=a_from_entityid;
    if mod(v_cnt,1)=0 and v_cnt>0 then
       while (v_cnt>0) loop
	        copy_usage_attr(a_from_entityid,a_from_attr,a_to_attr);
          a_to_attr := a_to_attr + 1;
          v_cnt := v_cnt - 1;
		  dbms_output.put_line('20');
       end loop;
    else
	dbms_output.put_line('21');
      raise_application_error(-20101,'执行次数必须为>0的整数！');
      RETURN;
    end if;
    COMMIT;
  END;
  FUNCTION deep_copy_tariff_schema(a_from_schema NUMBER,
                                   a_tariff_name VARCHAR2 := NULL)
    RETURN NUMBER AS
    v_new_schema NUMBER(9);
    v_schema_id  NUMBER(9);
  BEGIN
    SELECT MIN(tariff_schema_id)
      INTO v_new_schema
      FROM tariff_schema
     WHERE tariff_name LIKE '#UNUSED#'
       AND tariff_schema_id > 460;
    dbms_output.put_line('new tariff_schema from:' || a_from_schema ||
                         ' to : ' || v_new_schema);
    FOR r IN (SELECT *
                FROM tariff_schema
               WHERE tariff_schema_id = a_from_schema)
    LOOP
      r.tariff_schema_id := v_new_schema;
      IF a_tariff_name IS NOT NULL
      THEN
        r.tariff_name := a_tariff_name;
		dbms_output.put_line('22');
      END IF;
      UPDATE tariff_schema
         SET ROW = r
       WHERE tariff_schema_id = v_new_schema;
      DELETE FROM tariff_item WHERE tariff_schema_id = v_new_schema;
    END LOOP;
    FOR r IN (SELECT *
                FROM tariff_item
               WHERE tariff_schema_id = a_from_schema)
    LOOP
      r.tariff_schema_id := v_new_schema;
      INSERT INTO tariff_item VALUES r;
	  dbms_output.put_line('23');
    END LOOP;
    FOR r IN (SELECT DISTINCT to_number(tariff_id) schema_id
                FROM tariff_item
               WHERE tariff_schema_id = v_new_schema
                 AND subtariff_type = 2)
    LOOP
      v_schema_id := deep_copy_tariff_schema(r.schema_id);
      UPDATE tariff_item
         SET tariff_id = v_schema_id
       WHERE tariff_schema_id = v_new_schema
         AND subtariff_type = 2
         AND tariff_id = r.schema_id;
      dbms_output.put_line('updated ' || SQL%ROWCOUNT || ' rows.');
	  dbms_output.put_line('24');
    END LOOP;
    RETURN v_new_schema;
  END;
  FUNCTION af_getstr(str VARCHAR2, v_split VARCHAR2, pos NUMBER)
    RETURN VARCHAR2 IS
    v_arstr    VARCHAR2(400);
    v_str      VARCHAR2(300);
    v_last_pos NUMBER;
    v_pos      NUMBER;
  BEGIN
    v_str := '';
    IF substr(str, length(str), 1) <> v_split
    THEN
      v_arstr := str || v_split;
	  dbms_output.put_line('25');
    ELSE
      v_arstr := str;
	  dbms_output.put_line('26');
    END IF;
    IF pos != 1
    THEN
      v_last_pos := instr(v_arstr, v_split, 1, pos - 1);
      v_pos      := instr(v_arstr, v_split, 1, pos);
      v_str      := substr(v_arstr, v_last_pos + 1, v_pos - v_last_pos - 1);
	  dbms_output.put_line('27');
    ELSE
      v_pos := instr(v_arstr, v_split, 1, pos);
      v_str := substr(v_arstr, 1, v_pos - 1);
	  dbms_output.put_line('28');
    END IF;

    RETURN v_str;
  END;
  --/*拷贝资费政策*/
  PROCEDURE ap_copy_tariff_plan(from_plan_id NUMBER, to_plan_id NUMBER) IS
    v_cnt NUMBER;
  BEGIN
    v_cnt := 0;

    SELECT COUNT(*)
      INTO v_cnt
      FROM tariff_plan a
     WHERE a.tariff_plan_id = to_plan_id;

    SELECT COUNT(*)
      INTO v_cnt
      FROM tariff_plan_item a
     WHERE a.tariff_plan_id = to_plan_id;

    IF v_cnt > 0
    THEN
	dbms_output.put_line('29');
      raise_application_error(-20101,
                              '复制的计费事件在目标库的TARIFF_PLAN或TARIFF_PLAN_ITEM有定义');
      RETURN;
    END IF;

    INSERT INTO tariff_plan
      ( tariff_plan_id,
        tariff_plan_name,
        is_recursive,
        precedence,
        process_before,
        process_after,
        plantype,
        note,
        precedence_accdisc,
        priceplantype,
        region,
        history)
      SELECT to_plan_id,
        tariff_plan_name,
        is_recursive,
        precedence,
        process_before,
        process_after,
        plantype,
        note,
        precedence_accdisc,
        priceplantype,
        region,
        history
        FROM tariff_plan a
       WHERE a.tariff_plan_id = from_plan_id;

    INSERT INTO tariff_plan_item
      ( tariff_plan_id,
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
        tariff_plan_item_name,
        serv_id)
      SELECT to_plan_id,
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
        tariff_plan_item_name,
        serv_id
        FROM tariff_plan_item a
       WHERE a.tariff_plan_id = from_plan_id;

    INSERT INTO product_element(product_element_type, product_element_code, product_element_name, note, history)
    select product_element_type, to_plan_id, product_element_name, note, history
    from product_element a where a.product_element_code=to_plan_id;

    INSERT INTO product_element_tariff(PRODUCT_ELEMENT_TYPE,PRODUCT_ELEMENT_CODE,TARIFF_PLAN_ID)
    select product_element_type, to_plan_id, decode(tariff_plan_id,a.product_element_code,to_plan_id,tariff_plan_id)
    from product_element_tariff a where a.product_element_code=to_plan_id;

  END;
  --/*拷贝账单分拆定义*/
  PROCEDURE ap_copy_bill_split(from_plan_id NUMBER, to_plan_id NUMBER) IS
    v_cnt NUMBER;
  BEGIN
    v_cnt := 0;

    SELECT COUNT(*)
      INTO v_cnt
      FROM ItemSplit_Cfg a
     WHERE a.ITEMADDCODE = to_plan_id;

    SELECT COUNT(*)
      INTO v_cnt
      FROM ItemSplit_Def a
     WHERE a.ITEMADDCODE = to_plan_id;

    IF v_cnt > 0
    THEN
	dbms_output.put_line('30');
      raise_application_error(-20101,
                              '复制的计费事件在目标库的ItemSplit_Cfg或ItemSplit_Def有定义');
      RETURN;
    END IF;

    INSERT INTO ItemSplit_Cfg
      ( itemcode,
        itemaddcode,
        splitcode,
        discval,
        maxrefval,
        inuse)
      SELECT  itemcode,
              to_plan_id,
              splitcode,
              discval,
              maxrefval,
              inuse
        FROM ItemSplit_Cfg a
       WHERE a.itemaddcode = from_plan_id;

    INSERT INTO ItemSplit_Def
      ( itemcode,
        itemaddcode,
        itemcode_sub,
        percentum,
        inuse,
        businessclass,
        business,
        isusecount,
        beyonditemcode)
      SELECT  itemcode,
              to_plan_id,
              itemcode_sub,
              percentum,
              inuse,
              businessclass,
              business,
              isusecount,
              beyonditemcode
        FROM ItemSplit_Def a
       WHERE a.itemaddcode = from_plan_id;
  END;
  PROCEDURE ap_do_base(v_tariff_plan_id IN NUMBER,
                       v_attr_id        IN NUMBER,
                       v_freevolum      IN NUMBER) AS
  BEGIN
    INSERT INTO billdisc_def
      (tariffplan_id,
       planitem_id,
       disc_entity,
       disc_cond,
       g_disc_cond,
       disc_type,
       refitem_type,
       freerefitem_type,
       refitem_code,
       refitem_cycles,
       refitem_cycle_offset,
       refvalue_calc_type,
       refvalue_unit,
       disc_object_type,
       disc_item_code,
       start_cycle_offset,
       valid_cycle_type,
       valid_cycles,
       inuse,
       tariffplan_type,
       disc_item_type,
       freerefitem_min_usage,
       note,
       isdisctspec,
       specdisct_expr,
       g_specdisct_expr)
    VALUES
      (v_tariff_plan_id,
       50,
       1,
       '0010-.0011==50 and((not(0003-.0084 match [''US2'']and 0003-.0084<>''US28'')))',
       'Class引用源事件.时长==50 and((not(用户.用户状态 match [''US2'']and 用户.用户状态<>''US28'')))',
       4,
       1,
       1,
       '',
       1,
       0,
       1,
       2,
       2,
       v_attr_id,
       0,
       4,
       1,
       1,
       3,
       1,
       0,
       '',
       '0',
       '0',
       '');
    INSERT INTO discitem_def
      (tariffplan_id,
       planitem_id,
       discitem_id,
       min_refvalue,
       max_refvalue,
       calc_type,
       disc_value,
       max_disc_value,
       inuse,
       refvalue_add,
       disct_expr,
       g_disct_expr,
       discitem_cond,
       g_discitem_cond)
    VALUES
      (v_tariff_plan_id,
       50,
       1,
       0,
       99999999,
       1,
       v_freevolum,
       v_freevolum,
       1,
       NULL,
       '0',
       '',
       '',
       '');
    INSERT INTO disc_batch_list
      (batch_id, tariffplan_id, start_cycle, end_cycle)
    VALUES
      (50, v_tariff_plan_id, to_char(to_date('20201030','yyyymmdd'), 'yyyymm') || '01', 99999999);
	  dbms_output.put_line('31');

  END;
  PROCEDURE ap_do_inter(v_imsi      IN VARCHAR2,
                        v_roamfee   IN NUMBER, --0 不需要维护 国际漫游费用，单位元  如果是有主被叫，是主叫的漫游费用
                        v_troamfee  IN NUMBER, --0 不需要维护 国际漫游费用，单位元  如果是有主被叫，是被叫的漫游费用
                        v_lfee      IN NUMBER, -- 0 不需要维护 拨打非本国长途 单位元计费单元是分钟的
                        v_lfee2     IN NUMBER, -- 0 不需要维护，如果是0 单位元，计费单元是分钟的
                        v_smsfee    IN NUMBER, -- 不需要维护,如果是0，单位是元，短信费用
                        v_gprsfee   IN NUMBER, -- 不需要维护，如果是0，单位是分，GPRS费用.
                        v_applytime IN DATE) IS
    --不需要维护
    --不需要维护
    v_hmanage VARCHAR2(32);
    v_hregion VARCHAR2(32);
    v_cnt     NUMBER(10);
  BEGIN
    v_cnt := 0;
    SELECT COUNT(*)
      INTO v_cnt
      FROM mapping_list
     WHERE mapping_id = 10
       AND mapping_sour = v_imsi
       AND nvl(expiretime, to_date('20370101000000', 'yyyymmddhh24miss')) >
           v_applytime;
    IF v_cnt <> 1
    THEN
	dbms_output.put_line('32');
      raise_application_error(-20100,
                              '维护国家的imsi 前5位在值映射10里面没有维护，请维护');
    END IF;
    SELECT TRIM(substr(mapping_dest, 1, instr(mapping_dest, ',') - 1)),
           TRIM(substr(mapping_dest, instr(mapping_dest, ',') + 1))
      INTO v_hregion, v_hmanage
      FROM mapping_list
     WHERE mapping_id = 10
       AND mapping_sour = v_imsi
       AND nvl(expiretime, to_date('20370101000000', 'yyyymmddhh24miss')) >
           v_applytime;

    /*GPRS国际漫游费用,资费模式282*/
    IF v_gprsfee > 0
    THEN
      SELECT COUNT(1)
        INTO v_cnt
        FROM tariff_item a
       WHERE a.tariff_schema_id = 282
         AND a.tariff_criteria = v_imsi || ',';
		 dbms_output.put_line('33');
      IF v_cnt > 0
      THEN
        UPDATE tariff_item a
           SET expiretime = v_applytime
         WHERE a.tariff_schema_id = 282
           AND a.tariff_criteria = v_imsi || ',';
		   dbms_output.put_line('34');
      END IF;
      INSERT INTO tariff_item
        (tariff_schema_id,
         applytime,
         expiretime,
         tariff_criteria,
         subtariff_type,
         tariff_id,
         ratio,
         ratetype,
         param_string,
         precedence,
         expr_id,
         g_param,
         is_dynamic,
         g_criteria,
         billcode)
      VALUES
        (282,
         v_applytime,
         NULL,
         v_imsi || ',',
         1,
         178,
         v_gprsfee / 0.1,
         ' ',
         NULL,
         5,
         NULL,
         NULL,
         0,
         NULL,
         NULL);
    END IF;
    /*SMS国际漫游费用,资费模式57*/
    IF v_smsfee > 0
    THEN
      SELECT COUNT(1)
        INTO v_cnt
        FROM tariff_item a
       WHERE a.tariff_schema_id = 57
         AND a.tariff_criteria = v_hregion || ',' || v_hmanage || ',';
		 dbms_output.put_line('35');
      IF v_cnt > 0
      THEN
        UPDATE tariff_item a
           SET expiretime = v_applytime
         WHERE a.tariff_schema_id = 57
           AND a.tariff_criteria = v_hregion || ',' || v_hmanage || ',';
		   dbms_output.put_line('36');
      END IF;
      INSERT INTO tariff_item
        (tariff_schema_id,
         applytime,
         expiretime,
         tariff_criteria,
         subtariff_type,
         tariff_id,
         ratio,
         ratetype,
         param_string,
         precedence,
         expr_id,
         g_param,
         is_dynamic,
         g_criteria,
         billcode)
      VALUES
        (57,
         v_applytime,
         NULL,
         v_hregion || ',' || v_hmanage || ',',
         1,
         275,
         v_smsfee / 1,
         ' ',
         NULL,
         5,
         NULL,
         NULL,
         0,
         NULL,
         NULL);
    END IF;
    IF v_roamfee > 0
    THEN
      /*GSM国际漫游费用,资费模式49*/
      IF v_troamfee = 0 OR v_troamfee = v_roamfee
      THEN
        SELECT COUNT(1)
          INTO v_cnt
          FROM tariff_item a
         WHERE a.tariff_schema_id = 49
           AND a.tariff_criteria = v_hregion || ',' || v_hmanage;
		   dbms_output.put_line('37');
        IF v_cnt > 0
        THEN
          UPDATE tariff_item a
             SET expiretime = v_applytime
           WHERE a.tariff_schema_id = 49
             AND a.tariff_criteria = v_hregion || ',' || v_hmanage;
			 dbms_output.put_line('38');
        END IF;
        INSERT INTO tariff_item
          (tariff_schema_id,
           applytime,
           expiretime,
           tariff_criteria,
           subtariff_type,
           tariff_id,
           ratio,
           ratetype,
           param_string,
           precedence,
           expr_id,
           g_param,
           is_dynamic,
           g_criteria,
           billcode)
        VALUES
          (49,
           v_applytime,
           NULL,
           v_hregion || ',' || v_hmanage,
           1,
           206,
           v_roamfee / 1,
           ' ',
           NULL,
           5,
           NULL,
           NULL,
           0,
           NULL,
           NULL);

      ELSE
        /*有被叫漫游费用,维护资费模式3005*/
        INSERT INTO tariff_item
          (tariff_schema_id,
           applytime,
           expiretime,
           tariff_criteria,
           subtariff_type,
           tariff_id,
           ratio,
           ratetype,
           param_string,
           precedence,
           expr_id,
           g_param,
           is_dynamic,
           g_criteria,
           billcode)
        VALUES
          (3005,
           v_applytime,
           NULL,
           v_hregion || ',' || v_hmanage || ',O',
           1,
           3,
           v_roamfee / 0.1,
           ' ',
           '',
           1,
           NULL,
           '',
           0,
           '',
           '');

        INSERT INTO tariff_item
          (tariff_schema_id,
           applytime,
           expiretime,
           tariff_criteria,
           subtariff_type,
           tariff_id,
           ratio,
           ratetype,
           param_string,
           precedence,
           expr_id,
           g_param,
           is_dynamic,
           g_criteria,
           billcode)
        VALUES
          (3005,
           v_applytime,
           NULL,
           v_hregion || ',' || v_hmanage || ',T',
           1,
           3,
           v_troamfee / 0.1,
           ' ',
           '',
           1,
           NULL,
           '',
           0,
           '',
           '');
        INSERT INTO tariff_item
          (tariff_schema_id,
           applytime,
           expiretime,
           tariff_criteria,
           subtariff_type,
           tariff_id,
           ratio,
           ratetype,
           param_string,
           precedence,
           expr_id,
           g_param,
           is_dynamic,
           g_criteria,
           billcode)
        VALUES
          (49,
           v_applytime,
           NULL,
           v_hregion || ',' || v_hmanage,
           2,
           3005,
           1.00,
           ' ',
           '0005-.0706,0005-.0707,0005-.0701',
           5,
           NULL,
           '源计费事件.本方归属区号,源计费事件.本方运营商,源计费事件.话单类型',
           0,
           '',
           '');
		   dbms_output.put_line('39');
      END IF;
    END IF;
    /*GSM国际漫游长途费用,资费模式55*/
    /*拨打非本地长途*/
    IF v_lfee > 0
    THEN
      SELECT COUNT(1)
        INTO v_cnt
        FROM tariff_item a
       WHERE a.tariff_schema_id = 55
         AND a.tariff_criteria = v_hregion || ',' || v_hmanage || ',';
      IF v_cnt > 0
      THEN
        UPDATE tariff_item a
           SET expiretime = v_applytime
         WHERE a.tariff_schema_id = 55
           AND a.tariff_criteria = v_hregion || ',' || v_hmanage || ',';
		   dbms_output.put_line('40');
      END IF;
      INSERT INTO tariff_item
        (tariff_schema_id,
         applytime,
         expiretime,
         tariff_criteria,
         subtariff_type,
         tariff_id,
         ratio,
         ratetype,
         param_string,
         precedence,
         expr_id,
         g_param,
         is_dynamic,
         g_criteria,
         billcode)
      VALUES
        (55,
         v_applytime,
         NULL,
         v_hregion || ',' || v_hmanage || ',',
         1,
         284,
         v_lfee / 10 / 0.01,
         ' ',
         NULL,
         5,
         NULL,
         NULL,
         0,
         NULL,
         NULL);
    END IF;
    IF v_lfee2 > 0 AND v_lfee2 <> v_lfee
    THEN
      SELECT COUNT(1)
        INTO v_cnt
        FROM tariff_item a
       WHERE a.tariff_schema_id = 55
         AND a.tariff_criteria =
             v_hregion || ',' || v_hmanage || ',' || v_hregion;
      IF v_cnt > 0
      THEN
        UPDATE tariff_item a
           SET expiretime = v_applytime
         WHERE a.tariff_schema_id = 55
           AND a.tariff_criteria =
               v_hregion || ',' || v_hmanage || ',' || v_hregion;
			   dbms_output.put_line('41');
      END IF;
      INSERT INTO tariff_item
        (tariff_schema_id,
         applytime,
         expiretime,
         tariff_criteria,
         subtariff_type,
         tariff_id,
         ratio,
         ratetype,
         param_string,
         precedence,
         expr_id,
         g_param,
         is_dynamic,
         g_criteria,
         billcode)
      VALUES
        (55,
         v_applytime,
         NULL,
         v_hregion || ',' || v_hmanage || ',' || v_hregion,
         1,
         284,
         v_lfee2 / 10 / 0.01,
         ' ',
         NULL,
         5,
         NULL,
         NULL,
         0,
         NULL,
         NULL);
    END IF;
  END;
  PROCEDURE ap_gen_billcycle(v_event_id IN NUMBER) IS
    v_cnt NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_cnt
      FROM charging_cycle_schema a
     WHERE a.cycle_schema_id = 2
       AND a.charging_event_id = v_event_id;
    IF v_cnt <> 1
    THEN
	dbms_output.put_line('42');
      raise_application_error(-20100,
                              '请在帐期方案2里面维护数据，然后执行');
    END IF;
    FOR cc IN (SELECT * FROM custom_cycle_schema)
    LOOP
      INSERT INTO charging_cycle_schema
        (cycle_schema_id,
         charging_event_id,
         billing_cond,
         expr_id,
         g_billing_cond)
        SELECT cc.cycle_schema_id,
               v_event_id,
               billing_cond,
               expr_id,
               g_billing_cond
          FROM charging_cycle_schema a
         WHERE a.cycle_schema_id = 2
           AND a.charging_event_id = v_event_id;
		   dbms_output.put_line('43');
    END LOOP;

  END;

  /*拷贝帐务优惠*/
  PROCEDURE ap_copy_account_priv(from_plan_id NUMBER, to_plan_id NUMBER) IS
    v_num NUMBER;
  BEGIN
    INSERT INTO billdisc_def
      (tariffplan_id,
       planitem_id,
       disc_entity,
       disc_cond,
       g_disc_cond,
       disc_type,
       refitem_type,
       freerefitem_type,
       refitem_code,
       refitem_cycles,
       refitem_cycle_offset,
       refvalue_calc_type,
       refvalue_unit,
       disc_object_type,
       disc_item_code,
       start_cycle_offset,
       valid_cycle_type,
       valid_cycles,
       inuse,
       tariffplan_type,
       disc_item_type,
       freerefitem_min_usage,
       note)
      SELECT to_plan_id,
             planitem_id,
             disc_entity,
             disc_cond,
             g_disc_cond,
             disc_type,
             refitem_type,
             freerefitem_type,
             refitem_code,
             refitem_cycles,
             refitem_cycle_offset,
             refvalue_calc_type,
             refvalue_unit,
             disc_object_type,
             disc_item_code,
             start_cycle_offset,
             valid_cycle_type,
             valid_cycles,
             inuse,
             tariffplan_type,
             disc_item_type,
             freerefitem_min_usage,
             note
        FROM billdisc_def a
       WHERE a.tariffplan_id = from_plan_id;

    INSERT INTO discitem_def
      (tariffplan_id,
       planitem_id,
       discitem_id,
       min_refvalue,
       max_refvalue,
       calc_type,
       disc_value,
       max_disc_value,
       inuse,
       refvalue_add)
      SELECT to_plan_id,
             planitem_id,
             discitem_id,
             min_refvalue,
             max_refvalue,
             calc_type,
             disc_value,
             max_disc_value,
             inuse,
             refvalue_add
        FROM discitem_def a
       WHERE a.tariffplan_id = from_plan_id;

    INSERT INTO discvaluepercent_def
      (tariffplan_id, planitem_id, cond_expr, percent, g_cond_expr, seqno)
      SELECT to_plan_id,
             planitem_id,
             cond_expr,
             percent,
             g_cond_expr,
             seqno
        FROM discvaluepercent_def
       WHERE tariffplan_id = from_plan_id;

    INSERT INTO discrefitem_def
      (tariffplan_id, planitem_id, item_code, item_desc, inuse)
      SELECT to_plan_id, planitem_id, item_code, item_desc, inuse
        FROM discrefitem_def
       WHERE tariffplan_id = from_plan_id;

    /*配置批次定义*/
    /*
      SELECT COUNT(*)
        INTO v_num
        FROM disc_batch_list
       WHERE tariffplan_id = from_plan_id;

      IF v_num = 0
      THEN
        raise_application_error(-60427, '复制的帐务优惠在批次里面没有定义');
        ROLLBACK;
        RETURN;
      END IF;
    */
    /*加入批次定义*/
    /*
      INSERT INTO disc_batch_list
        (batch_id, tariffplan_id, start_cycle, end_cycle)
        SELECT batch_id,
               to_plan_id,
               to_number(to_char(SYSDATE, 'yyyymm')),
               end_cycle
          FROM disc_batch_list
         WHERE tariffplan_id = from_plan_id;
		 
    */
	dbms_output.put_line('44');
  END;


  PROCEDURE deep_copy_tariff_plan_vpn(a_from_plan NUMBER, a_to_plan NUMBER) AS
    v_tariff_name tariff_plan.tariff_plan_name%TYPE;
    v_schema_id   tariff_plan_item.tariff_schema_id%TYPE;
  BEGIN
    dbms_output.put_line('deep copy tariff_plan from: ' || a_from_plan ||
                         ' to: ' || a_to_plan);
    SELECT tariff_plan_name
      INTO v_tariff_name
      FROM tariff_plan_vpn
     WHERE tariff_plan_id = a_to_plan;
    FOR r IN (SELECT *
                FROM tariff_plan_item_vpn
               WHERE tariff_plan_id = a_from_plan)
    LOOP
      r.tariff_plan_id := a_to_plan;
      INSERT INTO tariff_plan_item_vpn VALUES r;
	  dbms_output.put_line('45');
    END LOOP;
    FOR r IN (SELECT DISTINCT tariff_schema_id
                FROM tariff_plan_item_vpn
               WHERE tariff_plan_id = a_to_plan)
    LOOP
      v_schema_id := deep_copy_tariff_schema(r.tariff_schema_id,
                                             v_tariff_name);
      UPDATE tariff_plan_item_vpn
         SET tariff_schema_id = v_schema_id
       WHERE tariff_plan_id = a_to_plan
         AND tariff_schema_id = r.tariff_schema_id;
		 dbms_output.put_line('46');
    END LOOP;
  END;
BEGIN
  -- Initialization
  --<Statement>;
  NULL;
  dbms_output.put_line('47');

END tu;
$$