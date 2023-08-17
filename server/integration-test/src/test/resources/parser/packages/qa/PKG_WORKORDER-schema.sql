DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_WORKORDER" is

    procedure ap_workorder_to_tariff(
        a_workorder_id          in varchar2,    --work order id
        a_region                in number,      --region
        a_result                out number,     --1-success, -1-failure
        a_msg                   out varchar2    --result message
    )
    is
        v_count                 number;
        v_tariff_plan_id        number;
    begin
        --check whether exist tariff plan
        v_count := 0;
        select count(*) into v_count from tariff_plan_ucd
                            where workorder_id = a_workorder_id;

        if v_count = 0 then
            a_result := -1;
            a_msg := '工单【' || to_char(a_workorder_id) || '】无资费政策，配置不完整!';
			dbms_output.put_line('1');
            return;
        end if;

        --generate tariff plan
        for c1 in (select rowid,t.* from tariff_plan_ucd t
                            where workorder_id = a_workorder_id ) loop

            --获取资费政策编号
            ap_get_plan_id(v_tariff_plan_id, a_result, a_msg);

            if a_result = -1 then
			dbms_output.put_line('2');
                return;
            end if;

            --生成新的资费政策
            insert into tariff_plan
                   (tariff_plan_id,
                    tariff_plan_name,
                    precedence,
                    process_before,
                    process_after,
                    plantype,
                    note,
                    precedence_accdisc,
                    priceplantype,
                    region)
            values
                   (v_tariff_plan_id,               --资费政策标识
                    c1.tariff_plan_name,             --资费政策名称
                    c1.precedence,                   --优先级
                    c1.process_before,               --批价前处理方案
                    c1.process_after,                --批价后处理方案
                    c1.plantype,                     --计划类型
                    c1.note,                         --资费政策说明
                    c1.precedence_accdisc,           --帐务优惠优先级
                    c1.priceplantype,                --定价计划类型
                    a_region);                       --地区号，0表示全省


            --生成新的资费政策项
            ap_gen_tariff_plan_item(a_workorder_id, a_region, c1.tariff_plan_id, v_tariff_plan_id, a_result, a_msg);

            if a_result = -1 then
			dbms_output.put_line('3');
                return;
            end if;

            --tariff_plan_item_ucd
            update tariff_plan_item_ucd set tariff_plan_id = v_tariff_plan_id
                where workorder_id = a_workorder_id and  tariff_plan_id = c1.tariff_plan_id;

            --tariff_schema_ucd
            update tariff_schema_ucd set tariff_plan_id = v_tariff_plan_id
                where workorder_id = a_workorder_id and  tariff_plan_id = c1.tariff_plan_id;

            --tariff_item_ucd
            update tariff_item_ucd set tariff_plan_id = v_tariff_plan_id
                where workorder_id = a_workorder_id and  tariff_plan_id = c1.tariff_plan_id;


            --修改ucd表中资费政策编号
            update tariff_plan_ucd set tariff_plan_id = v_tariff_plan_id
                where rowid = c1.rowid;

             /*
             --优惠处理
             ap_audit_disc(a_workorder_id, c1.tariff_plan_id, a_result, a_msg);
             if a_result = -1 then
                 return;
             end if;
             */
        end loop;

        a_result := 1;
        a_msg := '生成资费政策成功!';
        return;
        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('4');
            return;
    end ap_workorder_to_tariff;

    --generate tariff plan item
    procedure ap_gen_tariff_plan_item(
        a_workorder_id          in  varchar2,   --work order id
        a_region                in  number,     --region
        a_tariff_plan_id        in  number,     --tariff plan id
        a_new_tariff_plan_id    in  number,     --new tariff plan id
        a_result                out number,     --1 - success, -1 - failure
        a_msg out varchar2                      --result message
    )
    is
        v_schema_id         tariff_schema.tariff_schema_id%type;
        v_plan_item_sn      tariff_plan_item.tariff_plan_item_sn%type;
        v_chargingcond      tariff_plan_item.charging_cond%type;
        v_gchargingcond     tariff_plan_item.g_charging_cond%type;
    begin

        --generate tariff plan item
        for c2 in (select a.*, a.rowid from tariff_plan_item_ucd a
                       where workorder_id = a_workorder_id
                             and tariff_plan_id = a_tariff_plan_id) loop
           --get max tariff schema
            v_schema_id := 0;
            select nvl(max(tariff_schema_id), 0) + 1 into v_schema_id  from tariff_schema;

            v_plan_item_sn := af_get_tariff_sn();

            --把业务表达式编号替换为表达式
            if c2.serv_id is null then
                v_chargingcond := '1';
                v_gchargingcond := '1';
				dbms_output.put_line('5');
            else
                v_chargingcond := chr(3) || '0015-.' || lpad(c2.serv_id, 4, 0) || chr(4);
                select '业务表达式.' || trim(t.serv_desc) into v_gchargingcond
                    from serv_expr t where t.serv_id = c2.serv_id and rownum = 1;
					dbms_output.put_line('6');
            end if;

            insert into tariff_plan_item
                (tariff_plan_id,
                 tariff_plan_item_sn,
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
            values
                (a_new_tariff_plan_id,          --资费政策ID
                 v_plan_item_sn,                --资费政策项序号
                 1,                             --计算类型，1-按实际类型，2-周期性，3-一次性
                 c2.fee_id,                     --要计算的费用ID
                 v_schema_id,                   --资费模式ID
                 c2.charging_event_id,          --要计算的计费事件
                 c2.precedence,                 --优先级
                 c2.applytime,                  --生效时间
                 c2.expiretime,                 --失效时间
                 c2.apply_method,               --生效失效判断方法
                 c2.discount_type,              --计算模式
                 c2.base_fee_id,                --基准费用
                 c2.discount_fee_id,            --优惠费用
                 v_chargingcond,                --计费条件
                 c2.switch_method,              --切换方式
                 c2.switch_unit,                --切换单位
                 v_gchargingcond,               --计费条件描述
                 c2.billcode,                   --账目编码
                 c2.select_method,              --批价原则
                 c2.tariff_name,                --资费政策项名称
                 c2.serv_id);                   --业务表达式ID

            --generate tariff schema
            ap_gen_tariff_schema(a_workorder_id, a_region, a_tariff_plan_id, c2.tariff_schema_sn, c2.charging_event_id,
                                 v_schema_id,  a_result, a_msg);

            if  a_result = -1 then
			dbms_output.put_line('7');
                return;
            end if;

            --update tariff_plan_item_ucd
            update tariff_plan_item_ucd
                set tariff_schema_id = v_schema_id,
                    tariff_plan_item_sn = v_plan_item_sn
                        where rowid = c2.rowid;
        end loop;

        a_result := 1;
        a_msg := '生成资费政策项成功!';
        return;
        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('8');
            return;
    end ap_gen_tariff_plan_item;

    --generate tariff schema
    procedure ap_gen_tariff_schema(
        a_workorder_id          in  varchar2,   --work order id
        a_region                in  number,     --region
        a_tariff_plan_id        in  number,     --tariff plan id
        a_tariff_schema_sn      in  number,     --tariff schema serial number
        a_charging_event_id     in  number,     --charging event id
        a_tariff_schema_id      in  number,     --tariff schema id
        a_result                out number,     --1-success, -1-failure
        a_msg                   out varchar2    --result message
    )
    is
       v_count         number(4);
    begin
        --check tariff schema
        select count(*) into v_count from tariff_schema_ucd
            where workorder_id = a_workorder_id
                and tariff_plan_id = a_tariff_plan_id
                and tariff_schema_sn = a_tariff_schema_sn;

        if v_count = 0 then
            a_result := - 1;
            a_msg := '没有找到资费模式序号为：' || to_char(a_tariff_schema_sn) || '的资费模式!';
            --a_msg := 'not found tariff_schema which tariff_schema_sn = ' || to_char(a_tariff_schema_sn) || '!';
			dbms_output.put_line('9');
            return;
        end if;

        --check tariff schema item
        select count(*) into v_count from tariff_item_ucd
            where workorder_id = a_workorder_id
                and tariff_plan_id = a_tariff_plan_id
                and tariff_schema_sn = a_tariff_schema_sn;

        if v_count = 0 then
            a_result := - 1;
            a_msg := '资费模式项不能为空。';
			dbms_output.put_line('10');
            return;
        end if;

        --generate tariff schema
        for c3 in ( select t.*, t.rowid  from tariff_schema_ucd t
                    where workorder_id = a_workorder_id
                      and tariff_plan_id = a_tariff_plan_id
                      and tariff_schema_sn = a_tariff_schema_sn) loop

            --insert into tariff_schema
            insert into tariff_schema
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
                 region)
            values
                (a_tariff_schema_id,            --资费模式号
                 c3.tariff_name,                --资费模式名称
                 c3.tariff_type,                --资费模式类型
                 nvl(c3.fieldcount,0),          --参数数量
                 c3.field_def,                  --参数定义
                 c3.match_order,                --匹配顺序
                 c3.match_type,                 --匹配方法
                 c3.apply_method,               --应用时间判断方法
                 c3.refid,                      --标记位置
                 c3.discount_fee_id,            --优惠费用标识
                 c3.g_field_def,                --中文参数定义
                 c3.round_method,               --取整方法
                 c3.round_scale,                --取整精度
                 c3.ref_offset,                 --标记偏移
                 a_charging_event_id,           --事件Id
                 c3.billcode_order,             --帐目编码取值顺序
                 a_region);                     --地区号，0表示全省

            --update tariff_schema_ucd
            update tariff_schema_ucd
                set tariff_schema_id = a_tariff_schema_id where rowid = c3.rowid;

            --generate tariff schema item
            ap_gen_tariff_schema_item(a_workorder_id, a_region, a_tariff_plan_id, a_tariff_schema_sn,
                                      a_charging_event_id, a_tariff_schema_id, a_result, a_msg);

            if a_result = -1 then
			dbms_output.put_line('11');
                return;
            end if;

        end loop;

        a_result := 1;
        a_msg := 'generate tariff schema success!';
        return;
        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('12');
            return;
    end ap_gen_tariff_schema;

    --generate tariff schema item
    procedure ap_gen_tariff_schema_item(
        a_workorder_id          in  varchar2,   --work order id
        a_region                in  number,     --region
        a_tariff_plan_id        in  number,     --tariff plan id
        a_tariff_schema_sn      in  number,     --tariff schema serial number
        a_charging_event_id     in  number,     --charging event id
        a_tariff_schema_id      in  number,     --tariff schema id
        a_result                out number,     --1-success, -1-failure
        a_msg                   out varchar2    --result message
    )
    is
        v_subschema_id          number(9);
        v_tariff_item_sn        varchar2(20);
    begin
        --generate tariff schema item
        for c4 in (select t.*, t.rowid from  tariff_item_ucd t
                       where workorder_id = a_workorder_id
                             and tariff_plan_id = a_tariff_plan_id
                             and tariff_schema_sn = a_tariff_schema_sn) loop

            v_tariff_item_sn := af_get_tariff_sn();

            case c4.subtariff_type
                when  1  then  --Charge Rate
                    if   c4.subtariff_schema_sn is null  then
                        a_result := -1;
                        a_msg := '费率代码不能为空!';
						dbms_output.put_line('13');
                        return;
                    end if;

                    update tariff_item_ucd set tariff_id = c4.subtariff_schema_sn
                        where rowid = c4.rowid ;
                    v_subschema_id := c4.subtariff_schema_sn;

                when  2  then  --Tariff
                    --generate tariff schema
                    v_subschema_id := 0;
                    select nvl(max(tariff_schema_id), 0) + 1 into v_subschema_id  from tariff_schema;

                    ap_gen_tariff_schema(a_workorder_id, a_region, a_tariff_plan_id, c4.subtariff_schema_sn,
                                        a_charging_event_id, v_subschema_id, a_result, a_msg);

                    if  a_result = -1 then
					dbms_output.put_line('14');
                        return;
                    end if;

                    --update tariff_item_ucd
                    update tariff_item_ucd set tariff_id = v_subschema_id where rowid = c4.rowid;

                else --3 Fee Item, 4-Average Charge Rate, 5-Max Usage Fee
                    update tariff_item_ucd set tariff_id = c4.subtariff_schema_sn
                        where rowid = c4.rowid ;
                    v_subschema_id := c4.subtariff_schema_sn;
					dbms_output.put_line('15');
            end case;

            insert into tariff_item
                  (tariff_schema_id,
                   tariff_item_sn,
                   tariff_item_name,
                   applytime,
                   expiretime,
                   tariff_criteria,
                   subtariff_type,
                   tariff_id,
                   ratio,
                   ratetype,
                   param_string,
                   precedence,
                   g_param,
                   g_criteria,
                   billcode)
            values
                   (a_tariff_schema_id,                 --资费模式号
                   v_tariff_item_sn,                    --资费模式项序号
                   c4.node_name,                        --资费模式项名称
                   c4.applytime,                        --生效时间
                   c4.expiretime,                       --失效时间
                   c4.tariff_criteria,                  --资费条件
                   c4.subtariff_type,                   --下级资费类型
                   v_subschema_id,                      --下级资费号
                   c4.ratio,                            --比率
                   nvl(c4.ratetype, 'c'),               --资费类型
                   c4.param_string,                     --资费参数串
                   c4.precedence,                       --优先级
                   c4.g_param,                          --资费参数串中文注释
                   c4.g_criteria,                       --资费条件中文注释
                   c4.billcode);                        --账目编码
        end loop;

        a_result := 1;
        a_msg := 'generate tariff schema item successed!';
        return;
        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('16');
            return;
    end ap_gen_tariff_schema_item;

    --generate ucd tariff plan
    procedure ap_gen_tariff_plan_ucd(
        a_workorder_id          in  varchar2, --work order id
        a_tariff_plan_id        in  number,   --tariff plan id
        a_new_tariff_plan_id    in  number,   --new tariff plan id
        a_source_type           in  number,   --0 from table of tariff_plan
                                              --1 from table of tariff_plan_ucd
        a_result                out number,   --1-success, -1-failurre
        a_msg                   out varchar2  --result message
    )
    is

    begin

        --check work order and tariff plan
        ap_check_workorder(a_workorder_id, a_new_tariff_plan_id, a_result, a_msg);

        if a_result = -1 then
		dbms_output.put_line('17');
            return;
        end if;

        --generate tariff plan ucd
        if a_source_type = 0 then
            insert into tariff_plan_ucd
              (workorder_id, tariff_plan_id, tariff_plan_name, optional, precedence, precedence_accdisc, process_before, process_after, plantype, priceplantype, note)
            select  a_workorder_id, a_new_tariff_plan_id, tariff_plan_name, 1, precedence, precedence_accdisc, process_before, process_after, plantype, priceplantype, note
                from tariff_plan where tariff_plan_id = a_tariff_plan_id;
				dbms_output.put_line('18');
        else
            insert into tariff_plan_ucd
              (workorder_id, tariff_plan_id, tariff_plan_name, optional, precedence, precedence_accdisc, process_before, process_after, plantype, priceplantype, note)
            select  a_workorder_id, a_new_tariff_plan_id, tariff_plan_name, optional, precedence, precedence_accdisc, process_before, process_after, plantype, priceplantype, note
                from tariff_plan_ucd where tariff_plan_id = a_tariff_plan_id;
				dbms_output.put_line('19');
        end if;

        a_result := 1;
        a_msg := 'generate tariff success!';
        return;

        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('20');
            return;
    end ap_gen_tariff_plan_ucd;

    --generate ucd tariff plan item
    procedure ap_gen_tariff_plan_item_ucd(
        a_workorder_id          in  varchar2, --work order id
        a_tariff_plan_id        in  number,   --tariff plan id
        a_new_tariff_plan_id    in  number,   --new tariff plan id
        a_tariff_plan_item_id   in  number,   --tariff paln item;
        a_result                out number,   --1-success,-1-failurre
        a_msg                   out varchar2  --result message
    )
    is
        v_schema_sn             number(4);
        v_tariff_schema_id      number(9);
        v_attr_class            number(4);
    begin

        --get schema_sn
        v_schema_sn := 0;
        select nvl(max(tariff_schema_sn), 0) + 1 into v_schema_sn from tariff_schema_ucd
            where workorder_id = a_workorder_id and tariff_plan_id = a_new_tariff_plan_id;

        --获取资费模式编号
        select tariff_schema_id into v_tariff_schema_id from tariff_plan_item
            where tariff_plan_id = a_tariff_plan_id
              and tariff_plan_item_sn = a_tariff_plan_item_id;

        --generate ucd tariff schema
        ap_gen_tariff_schema_ucd(a_workorder_id, a_tariff_plan_id, a_new_tariff_plan_id,
                                 v_schema_sn, v_tariff_schema_id, v_attr_class, a_result, a_msg);

         --generate ucd tariff plan item
        insert into tariff_plan_item_ucd
            (workorder_id,
             tariff_plan_id,
             tariff_schema_sn,
             tariff_name,
             fee_id,
             select_method,
             charging_event_id,
             precedence,
             applytime,
             expiretime,
             apply_method,
             discount_type,
             base_fee_id,
             discount_fee_id,
             charging_cond,
             g_charging_cond,
             switch_method,
             switch_unit,
             billcode,
             attr_class,
             serv_id)
        select a_workorder_id,                       --工单ID
               a_new_tariff_plan_id,                 --资费政策ID
               v_schema_sn,                          --资费模式序号
               tariff_plan_item_name,                --资费政策项名称
               fee_id,                               --要计算的费用ID
               select_method,                        --批价原则(1-取较低的费用;2-无条件取本费用;3-有条件取本费用)
               charging_event_id,                    --要计算的计费事件
               precedence,                           --优先级
               applytime,                            --生效时间
               expiretime,                           --失效时间
               apply_method,                         --生效失效判断方法(1-按当前时间匹配，2-按计费事件发生时间匹配，3-忽略时间匹配)
               discount_type,                        --计算模式
               base_fee_id,                          --基准费用
               discount_fee_id,                      --优惠费用
               charging_cond,                        --计费条件
               g_charging_cond,                      --计费条件描述
               switch_method,                        --切换方式(1-按开始时间，2-跨边界的一个时间单位向前靠，3-跨边界的一个时间单位向后靠，4-跨边界的一个时间单位按比例分配)
               switch_unit,                          --切换单位
               billcode,                             --账目编码
               v_attr_class,                         --计价属性（0－正常属性，11－时长,19－字节数,37－计费字节数)
               serv_id                               --业务表达式ID
        from tariff_plan_item
             where  tariff_plan_id = a_tariff_plan_id
               and  tariff_plan_item_sn = a_tariff_plan_item_id;

        if a_result = -1 then
		dbms_output.put_line('21');
            return;
        end if;

        a_result := 1;
        a_msg := 'generate tariff plan item success!';
        return;
      exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('22');
            return;
    end ap_gen_tariff_plan_item_ucd;

    --generate ucd tariff schema
    procedure ap_gen_tariff_schema_ucd(
        a_workorder_id          in  varchar2,   --work order id
        a_tariff_plan_id        in  number,     --tariff plan id
        a_new_tariff_plan_id    in  number,     --new tariff plan id
        a_tariff_schema_sn      in  number,     --tariff schema serial number
        a_tariff_schema_id      in  number,     --tariff schema id
        a_attr_class            out number,     --attr class
        a_result                out number,     --1-success,-1-failurre
        a_msg out varchar2                      --result message
    )
    is
       v_subschema_num          number(3);
       v_is_combination         number(1);
       v_attr_class             number(2);
       v_unittype               number(2);
       v_units                  number(10,3);
       v_currency               number(9,3);
       v_subtariff_schema_sn    number(4);
       v_tariff_type            number(2);
       v_rate_code              number(8);
    begin

        a_attr_class := 0;
        select tariff_type into v_tariff_type from tariff_schema
             where tariff_schema_id = a_tariff_schema_id;

        if v_tariff_type = 3 then
            a_result := -1;
            a_msg := '系统暂不支持参数映射资费模式!';
			dbms_output.put_line('23');
            return;
        end if;

        v_subschema_num := 0;
        select count(*) into  v_subschema_num from tariff_item
            where tariff_schema_id = a_tariff_schema_id and subtariff_type = 2;

        --下级资费类型包含资费模式时，按组合资费政策导入
        if v_subschema_num > 0 then
            v_is_combination := 1;
			dbms_output.put_line('24');
        else
            v_is_combination := 0;
			dbms_output.put_line('25');
        end if;

        --generate tariff schema
        insert into tariff_schema_ucd
            (workorder_id,
             tariff_plan_id,
             tariff_schema_sn,
             tariff_name,
             tariff_type,
             is_combination,
             field_def,
             fieldcount,
             match_order,
             match_type,
             apply_method,
             refid,
             ref_offset,
             discount_fee_id,
             g_field_def,
             round_method,
             round_scale,
             billcode_order)
        select a_workorder_id,
             a_new_tariff_plan_id,
             a_tariff_schema_sn,
             tariff_name,
             tariff_type,
             v_is_combination,
             field_def,
             fieldcount,
             match_order,
             match_type,
             apply_method,
             refid,
             ref_offset,
             discount_fee_id,
             g_field_def,
             round_method,
             round_scale,
             billcode_order
        from tariff_schema where tariff_schema_id = a_tariff_schema_id;

        --generate tariff schema item
        for c4 in (select t.*, t.rowid from  tariff_item t
                       where tariff_schema_id = a_tariff_schema_id) loop
            v_rate_code := null;
            v_attr_class := null;
            v_unittype := null;
            v_units := null;
            v_currency := null;

            case c4.subtariff_type
                when  1  then  --Charge Rate
                    select attr_class, unittype, units, currency
                        into v_attr_class, v_unittype, v_units, v_currency
                        from charge_rate where ratecode = c4.tariff_id;
                     v_subtariff_schema_sn := c4.tariff_id;
                     v_currency := v_currency * c4.ratio;
                     a_attr_class := v_attr_class;
                     v_rate_code := c4.tariff_id;
					 dbms_output.put_line('26');
                when  2  then  --Tariff schema
                    select nvl(max(tariff_schema_sn), 0) + 1 into v_subtariff_schema_sn from tariff_schema_ucd
                        where workorder_id = a_workorder_id and tariff_plan_id = a_new_tariff_plan_id;

                    --generate sub ucd tariff schema
                    ap_gen_tariff_schema_ucd(a_workorder_id, a_tariff_plan_id, a_new_tariff_plan_id,
                                 v_subtariff_schema_sn, c4.tariff_id, a_attr_class, a_result, a_msg);
dbms_output.put_line('27');
                    if a_result = -1 then
					dbms_output.put_line('28');
                        return;
                    end if;

                else --3 Fee Item, 4-Average Charge Rate,5-Max Usage Fee
                    v_subtariff_schema_sn := c4.tariff_id;
					dbms_output.put_line('29');
            end case;

            insert into tariff_item_ucd
                (workorder_id,
                 tariff_plan_id,
                 tariff_schema_sn,
                 tariff_schema_id,
                 applytime,
                 expiretime,
                 tariff_criteria,
                 subtariff_type,
                 subtariff_schema_sn,
                 tariff_id,
                 param_string,
                 g_param,
                 ratetype,
                 precedence,
                 billcode,
                 g_criteria,
                 node_name,
                 ratio,
                 attr_class,
                 currency,
                 unittype,
                 units)
            values
                 (a_workorder_id,                     --工单ID
                  a_new_tariff_plan_id,               --资费政策ID
                  a_tariff_schema_sn,                 --资费模式序号
                  null,                               --资费模式号
                  c4.applytime,                       --生效时间
                  c4.expiretime,                      --失效时间
                  c4.tariff_criteria,                 --资费定义条件
                  c4.subtariff_type,                  --下级资费类型(1-费率，2-资费模式，3-费用项，4-当前费用项,5-封顶费用项)
                  v_subtariff_schema_sn,              --下级资费模式序号
                  c4.tariff_id,                       --下级资费号(对应正式库下级资费号)
                  c4.param_string,                    --参数串
                  c4.g_param,                         --参数串描述
                  c4.ratetype,                        --资费标记符
                  c4.precedence,                      --优先级
                  c4.billcode,                        --账目编码
                  c4.g_criteria,                      --资费定义条件描述
                  c4.tariff_item_name,                --节点名称
                  c4.ratio,                           --比率
                  v_attr_class,                       --计价属性（0－正常属性，11－时长,19－字节数,37－计费字节数)
                  v_currency,                         --金额
                  v_unittype,                         --单位种类
                  v_units);                           --单位
        end loop;

        a_result := 1;
        a_msg := 'generate ucd tariff schema success!';
        return;

        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('30');
            return;
    end ap_gen_tariff_schema_ucd;

    --copy ucd tariff plan item
    procedure ap_cp_tariff_plan_item_ucd(
        a_workorder_id          in  varchar2, --work order id
        a_new_workorder_id      in  varchar2, --new work order id
        a_tariff_plan_id        in  number,   --tariff plan id
        a_new_tariff_plan_id    in  number,   --new tariff plan id
        a_tariff_schema_sn      in  number,   --tariff schema serial number;
        a_result                out number,   --1-success, -1-failurre
        a_msg                   out varchar2  --result message
    )
    is
        v_new_schema_sn             number(9);
    begin
        --get new schema serial number
        v_new_schema_sn := 0;
        select nvl(max(tariff_schema_sn), 0) + 1 into v_new_schema_sn from tariff_schema_ucd
            where workorder_id = a_new_workorder_id and tariff_plan_id = a_new_tariff_plan_id;

        --copy ucd tariff plan item
        insert into tariff_plan_item_ucd
            (workorder_id,
             tariff_plan_id,
             tariff_schema_sn,
             fee_id,
             select_method,
             charging_event_id,
             precedence,
             applytime,
             expiretime,
             apply_method,
             discount_type,
             base_fee_id,
             discount_fee_id,
             charging_cond,
             g_charging_cond,
             switch_method,
             switch_unit,
             billcode,
             tariff_name,
             attr_class,
             serv_id)
        select a_new_workorder_id,
               a_new_tariff_plan_id,
               v_new_schema_sn,
               fee_id,
               select_method,
               charging_event_id,
               precedence,
               applytime,
               expiretime,
               apply_method,
               discount_type,
               base_fee_id,
               discount_fee_id,
               charging_cond,
               g_charging_cond,
               switch_method,
               switch_unit,
               billcode,
               tariff_name,
               attr_class,
               serv_id
        from tariff_plan_item_ucd  where workorder_id = a_workorder_id
              and tariff_plan_id = a_tariff_plan_id and tariff_schema_sn = a_tariff_schema_sn;

        --copy ucd tariff schema
        ap_cp_tariff_schema_ucd(a_workorder_id, a_new_workorder_id, a_tariff_plan_id, a_new_tariff_plan_id,
                                a_tariff_schema_sn, v_new_schema_sn, a_result, a_msg);

        if a_result = -1 then
		dbms_output.put_line('31');
            return;
        end if;

        a_result := 1;
        a_msg := 'copy tariff success!';
        return;
      exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('32');
            return;
    end ap_cp_tariff_plan_item_ucd;

    --copy ucd tariff schema
    procedure ap_cp_tariff_schema_ucd(
        a_workorder_id          in  varchar2,   --old work order id
        a_new_workorder_id      in  varchar2,   --new work order id
        a_tariff_plan_id        in  number,     --old tariff plan id
        a_new_tariff_plan_id    in  number,     --new tariff plan id
        a_tariff_schema_sn      in  number,     --old tariff schema serial number
        a_new_tariff_schema_sn  in  number,     --new tariff schema serial number
        a_result                out number,     --1-success, -1-failurre
        a_msg                   out varchar2    --result message
    )
    is
       v_sub_schema_sn   number(4);
    begin

        --copy tariff schema
        insert into tariff_schema_ucd
            (workorder_id,
             tariff_plan_id,
             tariff_schema_sn,
             tariff_name,
             tariff_type,
             is_combination,
             field_def,
             fieldcount,
             match_order,
             match_type,
             apply_method,
             refid,
             ref_offset,
             discount_fee_id,
             g_field_def,
             round_method,
             round_scale,
             billcode_order)
        select a_new_workorder_id,
               a_new_tariff_plan_id,
               a_new_tariff_schema_sn,
               tariff_name,
               tariff_type,
               is_combination,
               field_def,
               fieldcount,
               match_order,
               match_type,
               apply_method,
               refid,
               ref_offset,
               discount_fee_id,
               g_field_def,
               round_method,
               round_scale,
               billcode_order
        from tariff_schema_ucd  where workorder_id = a_workorder_id
                and tariff_plan_id = a_tariff_plan_id and tariff_schema_sn = a_tariff_schema_sn;

        --copy tariff schema item
        for c4 in (select t.*, t.rowid from  tariff_item_ucd t  where workorder_id = a_workorder_id
                        and tariff_plan_id = a_tariff_plan_id and tariff_schema_sn = a_tariff_schema_sn) loop

            if c4.subtariff_type = 2 then --Tariff schema

                select nvl(max(tariff_schema_sn), 0) + 1 into v_sub_schema_sn from tariff_schema_ucd
                    where workorder_id = a_new_workorder_id and tariff_plan_id = a_new_tariff_plan_id;

                --generate sub ucd tariff schema
                ap_cp_tariff_schema_ucd(a_workorder_id, a_new_workorder_id, a_tariff_plan_id, a_new_tariff_plan_id,
                             c4.subtariff_schema_sn, v_sub_schema_sn, a_result, a_msg);
dbms_output.put_line('33');
                if a_result = -1 then
				dbms_output.put_line('34');
                    return;
                end if;
            else
                 v_sub_schema_sn := c4.subtariff_schema_sn;
				 dbms_output.put_line('35');
            end if;

            insert into tariff_item_ucd
                (workorder_id,
                 tariff_plan_id,
                 tariff_schema_sn,
                 tariff_schema_id,
                 applytime,
                 expiretime,
                 tariff_criteria,
                 subtariff_type,
                 subtariff_schema_sn,
                 tariff_id,
                 param_string,
                 g_param,
                 ratetype,
                 precedence,
                 billcode,
                 g_criteria,
                 attr_class,
                 currency,
                 unittype,
                 units,
                 node_name,
                 ratio)
            values
                (a_new_workorder_id,
                 a_new_tariff_plan_id,
                 a_new_tariff_schema_sn,
                 null,
                 c4.applytime,
                 c4.expiretime,
                 c4.tariff_criteria,
                 c4.subtariff_type,
                 v_sub_schema_sn,
                 c4.tariff_id,
                 c4.param_string,
                 c4.g_param,
                 c4.ratetype,
                 c4.precedence,
                 c4.billcode,
                 c4.g_criteria,
                 c4.attr_class,
                 c4.currency,
                 c4.unittype,
                 c4.units,
                 c4.node_name,
                 c4.ratio);
        end loop; --c4

        a_result := 1;
        a_msg := 'generate ucd tariff schema success!';
        return;

        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('36');
            return;
    end ap_cp_tariff_schema_ucd;


    --save ucd tariff schema
    procedure ap_save_tariff_schema_ucd(
        a_workorder_id	        in  varchar2,       --工单ID
        a_tariff_plan_id   	    in  number,         --资费政策ID
        a_tariff_schema_sn	    in  number,         --资费模式序号
        a_tariff_name 	        in  varchar2,       --资费模式名称
        a_tariff_type     	    in  number,         --资费模式类型
        a_is_combination	    in  number,         --是否组合资费模式
        a_field_def       	    in  varchar2,       --参数字段定义
        a_fieldcount      	    in  number,         --参数字段定义数
        a_match_order     	    in  number,         --参数匹配次序
        a_match_type      	    in  number,         --匹配类型
        a_apply_method    	    in  number,         --应用时间判断方法
        a_refid           	    in  number,         --资费标记位置
        a_ref_offset      	    in  number,         --资费标记偏移
        a_discount_fee_id 	    in  number,         --优惠费用
        a_g_field_def     	    in  varchar2,       --中文参数定义
        a_round_method    	    in  number,         --取整方法
        a_round_scale     	    in  number,         --取整精度
        a_billcode_order	    in  number,         --账目编码取值顺序
        a_rowid                 out varchar2,       --返回rowid
        a_result                out number,         --1-成功, -1-失败
        a_msg                   out varchar2        --结果信息
    )
    is
    begin
        --insert into tariff schema
        insert into tariff_schema_ucd
            (workorder_id,
             tariff_plan_id,
             tariff_schema_sn,
             tariff_name,
             tariff_type,
             is_combination,
             field_def,
             fieldcount,
             match_order,
             match_type,
             apply_method,
             refid,
             ref_offset,
             discount_fee_id,
             g_field_def,
             round_method,
             round_scale,
             billcode_order)
         values
           (a_workorder_id,
            a_tariff_plan_id,
            a_tariff_schema_sn,
            a_tariff_name,
            a_tariff_type,
            a_is_combination,
            a_field_def,
            a_fieldcount,
            a_match_order,
            a_match_type,
            a_apply_method,
            a_refid,
            a_ref_offset,
            a_discount_fee_id,
            a_g_field_def,
            a_round_method,
            a_round_scale,
            a_billcode_order)
            returning rowid into a_rowid;

        a_result := 1;
        a_msg := 'save ucd tariff schema success!';
		dbms_output.put_line('37');
        return;

        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('38');
            return;
    end ap_save_tariff_schema_ucd;

    --save ucd tariff item
    procedure ap_save_tariff_item(
        a_workorder_id	        in    varchar2,		    --工单ID
        a_tariff_plan_id   	    in    number,     	    --资费政策ID
        a_tariff_schema_sn	    in    number,	    	--资费模式序号
        a_applytime	            in    date,         	--生效时间
        a_expiretime	        in    date,         	--失效时间
        a_tariff_criteria 	    in    varchar2,		    --资费定义条件
        a_subtariff_type  	    in    number,    		--下级资费类型
        a_subtariff_schema_sn	in    number,		    --下级资费模式序号
        a_precedence      	    in    number,    		--优先级
        a_g_criteria      	    in    varchar2,		    --资费定义条件描述
        a_node_name	            in    varchar2,		    --节点名称
        a_ratio	                in    number,		    --比率
        a_attr_class	        in    number,     	    --计价属性
        a_rowid                 out   varchar2,         --返回rowid
        a_result                out   number,           --1-成功, -1-失败
        a_msg                   out   varchar2          --结果信息
    )
    is
    begin
        insert into tariff_item_ucd
                (workorder_id,
                 tariff_plan_id,
                 tariff_schema_sn,
                 applytime,
                 expiretime,
                 tariff_criteria,
                 subtariff_type,
                 subtariff_schema_sn,
                 precedence,
                 g_criteria,
                 attr_class,
                 node_name,
                 ratio)
            values
               (a_workorder_id,
                a_tariff_plan_id ,
                a_tariff_schema_sn,
                a_applytime,
                a_expiretime,
                a_tariff_criteria,
                a_subtariff_type,
                a_subtariff_schema_sn,
                a_precedence,
                a_g_criteria,
                a_attr_class,
                a_node_name,
                a_ratio)
                returning rowid into a_rowid;

        a_result := 1;
        a_msg := 'save tariff item successed!';
		dbms_output.put_line('39');
        return;
        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('40');
            return;
    end ap_save_tariff_item;

    --check work order and tariff plan
    procedure ap_check_workorder(
        a_workorder_id          in  varchar2, --work order id
        a_tariff_plan_id        in  number,   --tariff plan id
        a_result                out number,   --1-success, -1-failurre
        a_msg                   out varchar2  --result message
    )
    is
        v_count                 number;
    begin

        --check work order
        v_count := 0;
        select count(*) into v_count from work_order_def
                            where workorder_id = a_workorder_id;

        if v_count = 0 then
            a_result := -1;
            a_msg := '工单【' || to_char(a_workorder_id) || '】不存在。';
			dbms_output.put_line('41');
            return;
        end if;

        --check tariff plan
        v_count := 0;
        select count(*) into v_count from tariff_plan
                            where tariff_plan_id = a_tariff_plan_id;

        if v_count > 0 then
            a_result := -1;
            a_msg := '资费政策【' || to_char(a_tariff_plan_id) || '】已经存在。';
			dbms_output.put_line('42');
            return;
        end if;

        a_result := 1;
        return;

        exception when others then
            a_msg := sqlerrm;
            a_result := -1;
			dbms_output.put_line('43');
            return;
    end ap_check_workorder;

    procedure ap_get_workorder_id(
        a_region_id             in  number,    --region id
        a_workorder_id          out varchar2   -- work order id
    )
    is
        v_serial                number(4);
        v_tmpStr                varchar2(10);
        v_region                varchar2(10);

    begin
        select max(to_number(substr(nvl(workorder_id,'00000000000000'),12,3))) + 1 into v_serial from work_order_def
            where region = a_region_id and trunc(create_date) = trunc(to_date('20201023','yyyymmdd'));

        v_region := lpad(a_region_id, 3, '0');

        if v_serial is null  then
            a_workorder_id :=  v_region || to_char(to_date('20201023','yyyymmdd'), 'yyyymmdd') || '001';
			dbms_output.put_line('44');
        else
            v_tmpStr := lpad(v_serial, 3, '0');
            a_workorder_id :=  v_region || to_char(to_date('20201023','yyyymmdd'), 'yyyymmdd') || v_tmpStr;
			dbms_output.put_line('45');
        end if;
    end ap_get_workorder_id;

    --delete work order
    procedure ap_del_workorder(
        a_workorder_id          in varchar2,   -- work order id
        a_result                out number     --1-success, -1-failurre
    )
    is

    begin
        --work order
        delete from work_order_def where workorder_id = a_workorder_id;

        --tariff plan
        delete from tariff_plan_ucd where workorder_id = a_workorder_id;

        --tariff plan item
        delete from tariff_plan_item_ucd where workorder_id = a_workorder_id;

        --tariff schema
        delete from tariff_schema_ucd where workorder_id = a_workorder_id;

        --tariff item
        delete from tariff_item_ucd where workorder_id = a_workorder_id;

        --Billdisc_Def_Ucd
        delete from  Billdisc_Def_Ucd where workorder_id = a_workorder_id;

        --Discitem_Def_Ucd
        delete from Discitem_Def_Ucd where workorder_id = a_workorder_id;

        --Discrefitem_Def_Ucd
        delete from Discrefitem_Def_Ucd where workorder_id = a_workorder_id;

        --Discvaluepercent_Def_Ucd
        delete from Discvaluepercent_Def_Ucd where workorder_id = a_workorder_id;

        --Disc_Batch_List_Ucd
        delete from Disc_Batch_List_Ucd  where workorder_id = a_workorder_id;

        --work_order_process_log
        delete from work_order_process_log  where workorder_id = a_workorder_id;
        commit;
        a_result := 1;
		dbms_output.put_line('46');
        return;
        exception when others then
            rollback;
            a_result := -1;
			dbms_output.put_line('47');
    end ap_del_workorder;

    --delete tariff plan
    procedure ap_del_tariff_plan(
        a_workorder_id          in varchar2,   -- work order id
        a_plan_id               in number,     -- tariff plan id
        a_result                out number     --1-success, -1-failurre
    )
    is

    begin
        --tariff plan
        delete from tariff_plan_ucd
            where workorder_id = a_workorder_id and tariff_plan_id = a_plan_id;

        --tariff plan item
        delete from tariff_plan_item_ucd
            where workorder_id = a_workorder_id and tariff_plan_id = a_plan_id;

        --tariff schema
        delete from tariff_schema_ucd
            where workorder_id = a_workorder_id and tariff_plan_id = a_plan_id;

        --tariff item
        delete from tariff_item_ucd
            where workorder_id = a_workorder_id and tariff_plan_id = a_plan_id;

        --process log
        delete from work_order_process_log where workorder_id = a_workorder_id;

        --Billdisc_Def_Ucd
        delete from  Billdisc_Def_Ucd
            where workorder_id = a_workorder_id and  tariffplan_id = a_plan_id;

        --Discitem_Def_Ucd
        delete from Discitem_Def_Ucd
            where workorder_id = a_workorder_id and  tariffplan_id = a_plan_id;

        --Discrefitem_Def_Ucd
        delete from Discrefitem_Def_Ucd
            where workorder_id = a_workorder_id and  tariffplan_id = a_plan_id;

        --Discvaluepercent_Def_Ucd
        delete from Discvaluepercent_Def_Ucd
            where workorder_id = a_workorder_id and  tariffplan_id = a_plan_id;

        --Disc_Batch_List_Ucd
        delete from Disc_Batch_List_Ucd
            where workorder_id = a_workorder_id and  tariffplan_id = a_plan_id;
dbms_output.put_line('48');
        commit;
        a_result := 1;
        return;
        exception when others then
            rollback;
            a_result := -1;
			dbms_output.put_line('49');
    end ap_del_tariff_plan;

   --delete ucd tariff plan item
    procedure ap_del_plan_item_ucd(
        a_workorder_id          in varchar2,   -- work order id
        a_tariff_plan_id        in number,     -- tariff plan id
        a_tariff_schema_sn      in number,     -- tariff schema serial number
        a_result                out number     --1-success, -1-failurre
    )
    is

    begin
        --delete tariff plan item
        delete from tariff_plan_item_ucd a where workorder_id = a_workorder_id
            and tariff_plan_id = a_tariff_plan_id and tariff_schema_sn = a_tariff_schema_sn;

        --delete tariff schema
        ap_del_tariff_schema_ucd(a_workorder_id, a_tariff_plan_id, a_tariff_schema_sn, a_result);
		dbms_output.put_line('50');
        return;
        exception when others then
            a_result := -1;
    end ap_del_plan_item_ucd;

    --del ucd tariff schema
    procedure ap_del_tariff_schema_ucd(
        a_workorder_id          in  varchar2,   --work order id
        a_tariff_plan_id        in  number,     --tariff plan id
        a_tariff_schema_sn      in  number,     --tariff schema serial number
        a_result                out number      --1-success, -1-failurre
    )
    is

    begin
        --delete tariff schema item
        for c1 in (select t.*, t.rowid from  tariff_item_ucd t  where workorder_id = a_workorder_id
                        and tariff_plan_id = a_tariff_plan_id and tariff_schema_sn = a_tariff_schema_sn) loop

            if c1.subtariff_type = 2 then --Tariff schema

                --delete sub ucd tariff schema
                ap_del_tariff_schema_ucd(a_workorder_id, a_tariff_plan_id, c1.subtariff_schema_sn, a_result);

                if a_result = -1 then
				dbms_output.put_line('51');
                    return;
                end if;
             else
                 delete from tariff_item_ucd where rowid = c1.rowid ;
				 dbms_output.put_line('52');
             end if;
          end loop; --c1

        --delete tariff schema
        delete from tariff_schema_ucd where workorder_id = a_workorder_id
                and tariff_plan_id = a_tariff_plan_id and tariff_schema_sn = a_tariff_schema_sn;

        a_result := 1;
        return;
        exception when others then
            a_result := -1;
            return;
    end ap_del_tariff_schema_ucd;

    --add process log
    procedure ap_add_process_log(
        a_workorder_id          in varchar2,   -- work order id
        a_operator              in varchar2,   -- operator
        a_status                in number,     -- work order status
        a_reason                in varchar2,   -- reject reason
        a_result                out number     --1-success, -1-failurre
    )
    is
        v_order     number(4);
    begin
        --get process order
        select nvl(max(process_order),0) + 1  into v_order from work_order_process_log
            where workorder_id = a_workorder_id;

        --insert into  work_order_process_log
        insert into work_order_process_log
        (workorder_id, process_order, operator, processtime, status, description)
        values
        (a_workorder_id, v_order, a_operator, to_date('20201023','yyyymmdd'), a_status, a_reason);

        --update work order status
        update work_order_def set status = a_status where workorder_id = a_workorder_id;
        a_result := 1;
		dbms_output.put_line('53');
        return;

        exception when others then
            a_result := -1;
    end ap_add_process_log;

    --ap_judge_plan_id
    procedure ap_judge_plan_id(
        a_rowid                 in varchar2,   -- rowid
        a_plan_id               in number,     -- tariff plan id
        a_result                out number     --1-success, -1-failurre
    )
    is
        v_count     number(4);
    begin

        select count(*) into v_count from tariff_plan
            where tariff_plan_id = a_plan_id;

        if v_count > 0 then
            a_result := -1;
			dbms_output.put_line('54');
            return;
        end if;

        if a_rowid is null then
            select count(*) into v_count from tariff_plan_ucd
            where tariff_plan_id = a_plan_id;
			dbms_output.put_line('55');
        else
            select count(*) into v_count from tariff_plan_ucd
            where rowid <> a_rowid and tariff_plan_id = a_plan_id;
			dbms_output.put_line('56');
        end if;

        if v_count > 0 then
            a_result := -1;
			dbms_output.put_line('57');
            return;
        end if;
        a_result := 1;
        return;
    end ap_judge_plan_id;

    --update tarriff plan id
    procedure ap_update_plan_id(
        a_workorder_id          in varchar2,   -- work order id
        a_old_plan_id           in number,     -- old tariff plan id
        a_new_plan_id           in number,     -- new tariff plan id
        a_result                out number     --1-success, -1-failurre
    )
    is
    begin
        --tariff_plan_item_ucd
        update tariff_plan_item_ucd set tariff_plan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariff_plan_id = a_old_plan_id;

        --tariff_plan_item_ucd
        update tariff_schema_ucd set tariff_plan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariff_plan_id = a_old_plan_id;

        --tariff_item_ucd
        update tariff_item_ucd set tariff_plan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariff_plan_id = a_old_plan_id;

        --Billdisc_Def_Ucd
        update Billdisc_Def_Ucd set tariffplan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariffplan_id = a_old_plan_id;

        --Discitem_Def_Ucd
        update Discitem_Def_Ucd set tariffplan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariffplan_id = a_old_plan_id;

        --Discrefitem_Def_Ucd
        update Discrefitem_Def_Ucd set tariffplan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariffplan_id = a_old_plan_id;

        --Discvaluepercent_Def_Ucd
        update Discvaluepercent_Def_Ucd set tariffplan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariffplan_id = a_old_plan_id;

        --Disc_Batch_List_Ucd
        update Disc_Batch_List_Ucd set tariffplan_id = a_new_plan_id
            where workorder_id = a_workorder_id and  tariffplan_id = a_old_plan_id;

        a_result := 1;
		dbms_output.put_line('58');
        return;
        exception when others then
            a_result := -1;
    end ap_update_plan_id;

    --获取资费政策编号
    procedure ap_get_plan_id(
        a_plan_id               out number,    -- tariff plan id
        a_result                out number,    --1-success, -1-failurre
        a_msg                   out varchar2   --result message
    )
    is
        v_plan_id   number;
    begin

        v_plan_id := TARIFF_PLAN_ID_START + 1;

        --从资费政策表(tariff_plan)找一个未用的资费政策编号作为新的资费政策编号
        for c1 in (select tariff_plan_id from tariff_plan
                       where tariff_plan_id > TARIFF_PLAN_ID_START
                       order by tariff_plan_id) loop

           if  v_plan_id <> c1.tariff_plan_id  then
               a_result := 1;
               a_plan_id := v_plan_id;
			   dbms_output.put_line('59');
               return;
           else
               v_plan_id := v_plan_id + 1;
			   dbms_output.put_line('60');
           end if;
       end loop;

       if v_plan_id >= TARIFF_PLAN_ID_MAX then
            a_result := -1;
            a_msg := '资费政策编号已超过最大值【' ||TARIFF_PLAN_ID_MAX || '】,无法生成资费政策!' ;
			dbms_output.put_line('61');
            return;
       end if;
       a_plan_id := v_plan_id;
       a_result := 1;
       return;
    end ap_get_plan_id;

    procedure ap_get_schema_id(
        a_schema_id             out number,    -- tariff schema id
        a_result                out number,    --1-success, -1-failurre
        a_msg                   out varchar2  --result message
    )
    is
        v_schema_id   number;
    begin

        v_schema_id := TARIFF_SCHEMA_ID_START + 1;

        --从资费模式表(tariff_schema )找一个未用的资费模式编号作为新的资费模式编号
        for c1 in (select tariff_schema_id from tariff_schema
                       where tariff_schema_id > TARIFF_SCHEMA_ID_START
                       order by tariff_schema_id ) loop

           if  v_schema_id <> c1.tariff_schema_id  then
               a_result := 1;
               a_schema_id := v_schema_id;
			   dbms_output.put_line('62');
               return;
           else
               v_schema_id := v_schema_id + 1;
			   dbms_output.put_line('63');
           end if;
       end loop;

       if v_schema_id >= TARIFF_SCHEMA_ID_START then
            a_result := -1;
            a_msg := '资费模式编号已超过最大值【' ||TARIFF_SCHEMA_ID_START || '】,无法生成资费政策!' ;
			dbms_output.put_line('64');
            return;
       end if;
       a_schema_id := v_schema_id;
       a_result := 1;
       return;
    end ap_get_schema_id;
begin
  -- Initialization
   null;
end PKG_WORKORDER;
$$