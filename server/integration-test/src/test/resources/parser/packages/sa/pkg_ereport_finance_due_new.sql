create or replace PACKAGE BODY pkg_ereport_finance_due_new AS
  /******************************************************************************
   --应收保费长短期包体
  --modified by liuyifu 修改汇率取值方法，废弃原来从旧表取汇率，更新从新表取汇率
  ******************************************************************************/
  procedure pro_finance_duepremium_short(parameters in varchar2,
                                         Re_Cursor  out T_CURSOR) is
    v_cursor T_CURSOR;
    --过滤参数
    p_user_code       varchar2(20);
    p_department_code varchar2(10);
    p_filter_date1    varchar2(10);
    p_filter_date2    varchar2(10);
    p_filter_date3    varchar2(10);
    p_plan_code       varchar2(20);
    p_emp_code        varchar2(30);
    p_sale_group      varchar2(20);
    p_client_type     varchar2(2);
    p_sale_channel    varchar2(10);
    p_currency_code   varchar2(2);
    p_rateh           varchar2(10);
    p_rateu           varchar2(10);
    --汇率值变量
    v_rateh number(8, 4);
    v_rateu number(8, 4);
    --记录统计变量
    v_count_limit number(10);
    v_count       number default 0;
    --查询记录的某些字段的辅助信息变量
    v_department_name varchar2(240);
    v_emp_name        varchar2(20);
    v_client_name     varchar2(20);
    v_salechnl_name   varchar2(20);
    v_salegroup_name  varchar2(100);
    v_plan_name       varchar2(40);
    v_currency_name   varchar2(20);
    v_duepremium      number(16, 2);
    v_duepremium1     number(16, 2);
    v_duepremium_sum  number(24, 6);
    v_user_loginId    varchar2(100);
    --与老系统对应的查询游标（实收日期为空的数据）
    cursor c_kcolduecursor is
      select CDNO, --分公司机构
             CPLYNO, --保单号
             CEDRNO, --批单号
             NTERMNO, --交费期别
             DPLYSTR, --保单责任起期，即保险起期
             DPLYEND, --保单责任止期，即保险止期
             CPAYNME, --缴费人名称
             CACCNO, --记帐编号，即应收凭证号
             DACCDTE, --记帐日期，即应收制证日期
             NPRMDUE, --应收保费，即金额
             CINSCDE, --险种代码
             substr(C_MAGIC_SET, 2, 1) kclientcde, --客户，即业务类型，个体客户：1 ？团体客户：2
             substr(C_MAGIC_SET, 1, 1) ksalechnl, --渠道代码，指销售渠道
             CPARNO, --团队代码
             CEMPCDE, --业务员代码
             CCURNO, --币种
             CANCEL, --失效标志,
             dfcd, -- 核保日期
             to_date(p_filter_date3, 'yyyy-mm-dd') -
             to_date(to_char(dpayend, 'yyyy-mm-dd'), 'yyyy-mm-dd') kaccountdays, --帐龄
             DCALDTE --结算日期
        from kcoldue
       where CDNO like (p_department_code || '%')
         and DECODE(DCOLDTE, NULL, 1, 0) = 1
         and greatest(DPLYSTR, DFCD) >=
             to_date(p_filter_date1, 'yyyy-mm-dd')
         and greatest(DPLYSTR, DFCD) <=
             to_date(p_filter_date2 || ' ' || '235959',
                     'yyyy-mm-dd hh24miss')
         and CPLYNO not in (select CPLYNO from NO_STAT_KCOLDUE)
         and CINSCDE <> 'A24'
         and nvl(CANCEL, 'N') <> 'Y'
         and nvl(HIDE_FLAG, 'N') <> 'Y';
    --与老系统对应的查询游标（截至日期后的数据）
    cursor c_kcolduecursor2 is
      select CDNO, --分公司机构
             CPLYNO, --保单号
             CEDRNO, --批单号
             NTERMNO, --交费期别
             DPLYSTR, --保单责任起期，即保险起期
             DPLYEND, --保单责任止期，即保险止期
             CPAYNME, --缴费人名称
             CACCNO, --记帐编号，即应收凭证号
             DACCDTE, --记帐日期，即应收制证日期
             NPRMDUE, --应收保费，即金额
             CINSCDE, --险种代码
             substr(C_MAGIC_SET, 2, 1) kclientcde, --客户，即业务类型，个体客户：1 ？团体客户：2
             substr(C_MAGIC_SET, 1, 1) ksalechnl, --渠道代码，指销售渠道
             CPARNO, --团队代码
             CEMPCDE, --业务员代码
             CCURNO, --币种
             CANCEL, --失效标志,
             dfcd, --核保日期
             to_date(p_filter_date3, 'yyyy-mm-dd') -
             to_date(to_char(dpayend, 'yyyy-mm-dd'), 'yyyy-mm-dd') kaccountdays, --帐龄
             DCALDTE --结算日期
        from kcoldue
       where CDNO like (p_department_code || '%')
         and DCOLDTE > to_date(p_filter_date3 || ' ' || '235959',
                               'yyyy-mm-dd hh24miss')
         and greatest(DPLYSTR, DFCD) >=
             to_date(p_filter_date1, 'yyyy-mm-dd')
         and greatest(DPLYSTR, DFCD) <=
             to_date(p_filter_date2 || ' ' || '235959',
                     'yyyy-mm-dd hh24miss')
         and CPLYNO not in (select CPLYNO from NO_STAT_KCOLDUE)
         and CINSCDE <> 'A24'
         and nvl(CANCEL, 'N') <> 'Y'
         and nvl(HIDE_FLAG, 'N') <> 'Y';
    v_kcoldue c_kcolduecursor%rowtype;
    --对新系统对应的查询游标（实收凭证号为空的数据）
    cursor c_premiumcursor is
      select a.FINANCE_DEPARTMENT_CODE,
             a.POLICY_NO,
             a.ENDORSE_NO,
             a.TERM_NO,
             a.INSURANCE_BEGIN_TIME,
             a.INSURANCE_END_TIME,
             a.INSURED_NAME,
             a.DUE_VOUCHER_NO,
             a.DUE_VOUCHER_DATE,
             b.PREMIUM_AMOUNT,
             b.PLAN_CODE,
             a.CLIENT_ATTRIBUTE,
             a.CHANNEL_SOURCE_CODE,
             a.GROUP_CODE,
             a.SALE_AGENT_CODE,
             a.CURRENCY_CODE,
             a.DISABLE_FLAG,
             a.UNDERWRITE_TIME,
             to_date(p_filter_date3, 'yyyy-mm-dd') -
             to_date(to_char(PAYMENT_END_DATE, 'yyyy-mm-dd'), 'yyyy-mm-dd') as kaccountdays,
             a.SETTLE_DATE
        from premium_info a, premium_plan b
       where FINANCE_DEPARTMENT_CODE like (p_department_code || '%')
         and DECODE(ACTUAL_VOUCHER_NO, NULL, 1, 0) = 1
         and greatest(INSURANCE_BEGIN_TIME, UNDERWRITE_TIME) >=
             to_date(p_filter_date1, 'yyyy-mm-dd')
         and greatest(INSURANCE_BEGIN_TIME, UNDERWRITE_TIME) <=
             to_date(p_filter_date2 || ' ' || '235959',
                     'yyyy-mm-dd hh24miss')
         and a.RECEIPT_NO = b.RECEIPT_NO
         and b.PLAN_CODE <> 'A24'
         and nvl(a.DISABLE_FLAG, 'N') <> 'Y';
    --对新系统对应的查询游标（截至日期后的数据）
    cursor c_premiumcursor2 is
      select a.FINANCE_DEPARTMENT_CODE,
             a.POLICY_NO,
             a.ENDORSE_NO,
             a.TERM_NO,
             a.INSURANCE_BEGIN_TIME,
             a.INSURANCE_END_TIME,
             a.INSURED_NAME,
             a.DUE_VOUCHER_NO,
             a.DUE_VOUCHER_DATE,
             b.PREMIUM_AMOUNT,
             b.PLAN_CODE,
             a.CLIENT_ATTRIBUTE,
             a.CHANNEL_SOURCE_CODE,
             a.GROUP_CODE,
             a.SALE_AGENT_CODE,
             a.CURRENCY_CODE,
             a.DISABLE_FLAG,
             a.UNDERWRITE_TIME,
             to_date(p_filter_date3, 'yyyy-mm-dd') -
             to_date(to_char(PAYMENT_END_DATE, 'yyyy-mm-dd'), 'yyyy-mm-dd') as kaccountdays,
             a.SETTLE_DATE
        from premium_info a, premium_plan b
       where FINANCE_DEPARTMENT_CODE like (p_department_code || '%')
         and ACTUAL_VOUCHER_DATE >
             to_date(p_filter_date3 || ' ' || '235959',
                     'yyyy-mm-dd hh24miss')
         and greatest(INSURANCE_BEGIN_TIME, UNDERWRITE_TIME) >=
             to_date(p_filter_date1, 'yyyy-mm-dd')
         and greatest(INSURANCE_BEGIN_TIME, UNDERWRITE_TIME) <=
             to_date(p_filter_date2 || ' ' || '235959',
                     'yyyy-mm-dd hh24miss')
         and a.RECEIPT_NO = b.RECEIPT_NO
         and b.PLAN_CODE <> 'A24'
         and nvl(a.DISABLE_FLAG, 'N') <> 'Y';
    v_premium c_premiumcursor%rowtype;
  BEGIN
    --分解通过reportnet提示页面获取的参数信息
    p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                       'userName_epcis');
    p_department_code := pkg_ereport.getParameterValue(parameters,
                                                       'finance_department_code_epcis');
    p_filter_date1    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'mustneed_settleDate1_epcis');
    p_filter_date2    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'mustneed_settleDate2_epcis');
    p_filter_date3    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'mustneed_settleDate3_epcis');
    p_plan_code       := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'plan_code_epcis');
    p_emp_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleAgent_code_epcis');
    p_sale_group      := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleGroup_code_epcis');
    p_client_type     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'businessType_epcis');
    p_sale_channel    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'channel_code_epcis');
    p_currency_code   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'currency_code_epcis');
    p_rateh           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateH_epcis');
    p_rateu           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateU_epcis');
    if p_department_code = substr(p_department_code, 1, 2) || '9999' then
      p_department_code := substr(p_department_code, 1, 2);
    elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
      p_department_code := substr(p_department_code, 1, 4);
    end if;
    --若用户没有在提示页面输入汇率，则从系统表中取汇率值
    /*if p_rateh is null then
        select rate into v_rateh from cur_dtl where ENDDAT is null and CNO = '02' and CCNO = '01' and rownum <= 1;
    else
        v_rateh := to_number(p_rateh);
    end if;
    if p_rateu is null then
        select rate into v_rateu from cur_dtl where ENDDAT is null and CNO = '03' and CCNO = '01' and rownum <= 1;
    else
        v_rateu := to_number(p_rateu);
    end if;*/
    if p_rateh is null then
      v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
    else
      v_rateh := to_number(p_rateh);
    end if;
    if p_rateu is null then
      v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
    else
      v_rateu := to_number(p_rateu);
    end if;
    --根据报表运行的当前时间获取记录条数限制值：8 － 20 点之间，10001，夜间，为65000
    --应用户要求，白天应收短期表放宽直接设定为10001
    if to_char(sysdate, 'hh24') > 8 and to_char(sysdate, 'hh24') < 20 then
      v_count_limit := 10001;
    else
      v_count_limit := 65000;
    end if;
    --根据当前时间组成此次会话的用户id
    v_user_loginId := p_user_code || '-' ||
                      to_char(sysdate, 'yyyymmdd hh24miss');
    v_count        := 0;
    open c_kcolduecursor;
    loop
      <<l_continue_old>>
      fetch c_kcolduecursor
        into v_kcoldue;
      exit when c_kcolduecursor%notfound;
      --先过滤，后取相关辅助信息。
      select sum(NPRMDUE)
        into v_duepremium
        from kcoldue
       where cplyno = v_kcoldue.cplyno;
      begin
        select sum(due_amount)
          into v_duepremium1
          from premium_info
         where policy_no = v_kcoldue.cplyno;
      exception
        when no_data_found then
          v_duepremium1 := 0;
      end;
      v_duepremium := v_duepremium + v_duepremium1;
      if v_duepremium = 0 then
        goto l_continue_old;
      end if;
      --过滤险种
      if p_plan_code is not null then
        if p_plan_code <> v_kcoldue.CINSCDE then
          goto l_continue_old;
        end if;
      end if;
      --过滤业务员
      if p_emp_code is not null then
        if p_emp_code <> nvl(v_kcoldue.CEMPCDE, 'NULL') then
          goto l_continue_old;
        end if;
      end if;
      --过滤团队
      if p_sale_group is not null then
        if p_sale_group <> nvl(v_kcoldue.CPARNO, 'NULL') then
          goto l_continue_old;
        end if;
      end if;
      --过滤客户
      if p_client_type is not null then
        if p_client_type <> nvl(v_kcoldue.kclientcde, 'NULL') then
          goto l_continue_old;
        end if;
      end if;
      --过滤渠道
      if p_sale_channel is not null then
        if p_sale_channel <> nvl(v_kcoldue.ksalechnl, 'NULL') then
          goto l_continue_old;
        end if;
      end if;
      --过滤币种
      if p_currency_code is not null then
        if p_currency_code <> v_kcoldue.CCURNO then
          goto l_continue_old;
        end if;
      end if;
      --取公司机构的名称  (注意：因为表kcoldue中有些字段允许为空，故应考虑到变量初始赋值)
      begin
        select description
          into v_department_name
          from institutions
         where flex_value = v_kcoldue.CDNO;
      exception
        when others then
          v_department_name := '';
      end;
      --取险种名称
      begin
        select plan_chinese_name
          into v_plan_name
          from plan_define
         where plan_code = v_kcoldue.CINSCDE;
      exception
        when others then
          v_plan_name := '';
      end;
      --取币种名称
      begin
        select currency_chinese_name
          into v_currency_name
          from currency_define
         where currency_code = v_kcoldue.CCURNO;
      exception
        when others then
          v_currency_name := '';
      end;
      --取业务员姓名
      begin
        if v_kcoldue.CEMPCDE is null then
          v_emp_name := '';
        else
          select CEMPCNM
            into v_emp_name
            from kempcde
           where CEMPCDE = v_kcoldue.CEMPCDE;
        end if;
      exception
        when others then
          v_emp_name := '';
      end;
      --取客户名称，即为个体客户或团体客户
      v_client_name := '';
      if v_kcoldue.kclientcde = '1' then
        v_client_name := '个体';
      elsif v_kcoldue.kclientcde = '2' then
        v_client_name := '团体';
      end if;
      --取销售渠道名称
      begin
        select BNOCNM
          into v_salechnl_name
          from business_source
         where BNO = v_kcoldue.ksalechnl;
      exception
        when others then
          v_salechnl_name := '';
      end;
      --取团队名称
      begin
        if v_kcoldue.CPARNO is null then
          v_salegroup_name := '';
        else
          select CGRPCNM
            into v_salegroup_name
            from kgrpcde
           where CGRPCDE = v_kcoldue.CPARNO;
        end if;
      exception
        when others then
          v_salegroup_name := '';
      end;
      --人民币折算
      if v_kcoldue.CCURNO = '02' then
        v_duepremium_sum := v_rateh * (v_kcoldue.NPRMDUE);
      elsif v_kcoldue.CCURNO = '03' then
        v_duepremium_sum := v_rateu * (v_kcoldue.NPRMDUE);
      else
        v_duepremium_sum := v_kcoldue.NPRMDUE;
      end if;
      --将符合条件的记录插入到temporary_table中
      insert into epcisacct.tmp_finance_duepremium_short
        (DEPARTMENT_CODE,
         DEPARTMENT_NAME,
         POLICY_NO,
         ENDORSE_NO,
         DUE_PREMIUM,
         INSURANCE_BEGIN_DATE,
         INSURANCE_END_DATE,
         INSURED_PERSON,
         DUE_VOUCHER_NO,
         DUE_VOUCHER_DATE,
         ACCOUNT_DAYS,
         PLAN_NAME,
         CLIENT_NAME,
         SALE_CHANNEL,
         SALE_GROUP,
         EMP_NAME,
         CURRENCY,
         DUE_PREMIUM_SUM,
         CANCEL_FLAG,
         underwrite_time,
         USER_LOGINID,
         NOTICE_NO,
         SETTLE_DATE)
      values
        (v_kcoldue.CDNO,
         v_department_name,
         v_kcoldue.CPLYNO,
         v_kcoldue.CEDRNO,
         v_kcoldue.NPRMDUE,
         v_kcoldue.DPLYSTR,
         v_kcoldue.DPLYEND,
         v_kcoldue.CPAYNME,
         v_kcoldue.CACCNO,
         v_kcoldue.DACCDTE,
         v_kcoldue.kaccountdays,
         v_plan_name,
         v_client_name,
         v_salechnl_name,
         v_kcoldue.CPARNO || '-' || v_salegroup_name,
         v_kcoldue.CEMPCDE || '-' || v_emp_name,
         v_currency_name,
         v_duepremium_sum,
         v_kcoldue.CANCEL,
         v_kcoldue.dfcd,
         v_user_loginID,
         v_kcoldue.ntermno,
         v_kcoldue.dcaldte);
      v_count := v_count + 1;
      if v_count_limit is not null then
        if v_count >= v_count_limit then
          close c_kcolduecursor;
          goto l_next1;
        end if;
      end if;
    end loop;
    close c_kcolduecursor;
    <<l_next1>>
    v_count := 0;
    open c_kcolduecursor2;
    loop
      <<l_continue_old2>>
      fetch c_kcolduecursor2
        into v_kcoldue;
      exit when c_kcolduecursor2%notfound;
      --先过滤，后取相关辅助信息。
      select sum(NPRMDUE)
        into v_duepremium
        from kcoldue
       where cplyno = v_kcoldue.cplyno;
      begin
        select sum(due_amount)
          into v_duepremium1
          from premium_info
         where policy_no = v_kcoldue.cplyno;
      exception
        when no_data_found then
          v_duepremium1 := 0;
      end;
      v_duepremium := v_duepremium + v_duepremium1;
      if v_duepremium = 0 then
        goto l_continue_old2;
      end if;
      --过滤险种
      if p_plan_code is not null then
        if p_plan_code <> v_kcoldue.CINSCDE then
          goto l_continue_old2;
        end if;
      end if;
      --过滤业务员
      if p_emp_code is not null then
        if p_emp_code <> nvl(v_kcoldue.CEMPCDE, 'NULL') then
          goto l_continue_old2;
        end if;
      end if;
      --过滤团队
      if p_sale_group is not null then
        if p_sale_group <> nvl(v_kcoldue.CPARNO, 'NULL') then
          goto l_continue_old2;
        end if;
      end if;
      --过滤客户
      if p_client_type is not null then
        if p_client_type <> nvl(v_kcoldue.kclientcde, 'NULL') then
          goto l_continue_old2;
        end if;
      end if;
      --过滤渠道
      if p_sale_channel is not null then
        if p_sale_channel <> nvl(v_kcoldue.ksalechnl, 'NULL') then
          goto l_continue_old2;
        end if;
      end if;
      --过滤币种
      if p_currency_code is not null then
        if p_currency_code <> v_kcoldue.CCURNO then
          goto l_continue_old2;
        end if;
      end if;
      --取公司机构的名称  (注意：因为表kcoldue中有些字段允许为空，故应考虑到变量初始赋值)
      begin
        select description
          into v_department_name
          from institutions
         where flex_value = v_kcoldue.CDNO;
      exception
        when others then
          v_department_name := '';
      end;
      --取险种名称
      begin
        select plan_chinese_name
          into v_plan_name
          from plan_define
         where plan_code = v_kcoldue.CINSCDE;
      exception
        when others then
          v_plan_name := '';
      end;
      --取币种名称
      begin
        select currency_chinese_name
          into v_currency_name
          from currency_define
         where currency_code = v_kcoldue.CCURNO;
      exception
        when others then
          v_currency_name := '';
      end;
      --取业务员姓名
      begin
        if v_kcoldue.CEMPCDE is null then
          v_emp_name := '';
        else
          select CEMPCNM
            into v_emp_name
            from kempcde
           where CEMPCDE = v_kcoldue.CEMPCDE;
        end if;
      exception
        when others then
          v_emp_name := '';
      end;
      --取客户名称，即为个体客户或团体客户
      v_client_name := '';
      if v_kcoldue.kclientcde = '1' then
        v_client_name := '个体';
      elsif v_kcoldue.kclientcde = '2' then
        v_client_name := '团体';
      end if;
      --取销售渠道名称
      begin
        select BNOCNM
          into v_salechnl_name
          from business_source
         where BNO = v_kcoldue.ksalechnl;
      exception
        when others then
          v_salechnl_name := '';
      end;
      --取团队名称
      begin
        if v_kcoldue.CPARNO is null then
          v_salegroup_name := '';
        else
          select CGRPCNM
            into v_salegroup_name
            from kgrpcde
           where CGRPCDE = v_kcoldue.CPARNO;
        end if;
      exception
        when others then
          v_salegroup_name := '';
      end;
      --人民币折算
      if v_kcoldue.CCURNO = '02' then
        v_duepremium_sum := v_rateh * (v_kcoldue.NPRMDUE);
      elsif v_kcoldue.CCURNO = '03' then
        v_duepremium_sum := v_rateu * (v_kcoldue.NPRMDUE);
      else
        v_duepremium_sum := v_kcoldue.NPRMDUE;
      end if;
      --将符合条件的记录插入到temporary_table中
      insert into epcisacct.tmp_finance_duepremium_short
        (DEPARTMENT_CODE,
         DEPARTMENT_NAME,
         POLICY_NO,
         ENDORSE_NO,
         DUE_PREMIUM,
         INSURANCE_BEGIN_DATE,
         INSURANCE_END_DATE,
         INSURED_PERSON,
         DUE_VOUCHER_NO,
         DUE_VOUCHER_DATE,
         ACCOUNT_DAYS,
         PLAN_NAME,
         CLIENT_NAME,
         SALE_CHANNEL,
         SALE_GROUP,
         EMP_NAME,
         CURRENCY,
         DUE_PREMIUM_SUM,
         CANCEL_FLAG,
         underwrite_time,
         USER_LOGINID,
         NOTICE_NO,
         SETTLE_DATE)
      values
        (v_kcoldue.CDNO,
         v_department_name,
         v_kcoldue.CPLYNO,
         v_kcoldue.CEDRNO,
         v_kcoldue.NPRMDUE,
         v_kcoldue.DPLYSTR,
         v_kcoldue.DPLYEND,
         v_kcoldue.CPAYNME,
         v_kcoldue.CACCNO,
         v_kcoldue.DACCDTE,
         v_kcoldue.kaccountdays,
         v_plan_name,
         v_client_name,
         v_salechnl_name,
         v_kcoldue.CPARNO || '-' || v_salegroup_name,
         v_kcoldue.CEMPCDE || '-' || v_emp_name,
         v_currency_name,
         v_duepremium_sum,
         v_kcoldue.CANCEL,
         v_kcoldue.dfcd,
         v_user_loginID,
         v_kcoldue.ntermno,
         v_kcoldue.dcaldte);
      v_count := v_count + 1;
      if v_count_limit is not null then
        if v_count >= v_count_limit then
          close c_kcolduecursor2;
          goto l_next2;
        end if;
      end if;
    end loop;
    close c_kcolduecursor2;
    <<l_next2>>
    v_count := 0;
    open c_premiumcursor;
    loop
      <<l_continue_new>>
      fetch c_premiumcursor
        into v_premium;
      exit when c_premiumcursor%notfound;
      --先过滤，后取相关辅助信息。
      select sum(DUE_AMOUNT)
        into v_duepremium
        from premium_info
       where policy_no = v_premium.policy_no;
      begin
        select sum(NPRMDUE)
          into v_duepremium
          from kcoldue
         where cplyno = v_premium.policy_no;
      exception
        when no_data_found then
          v_duepremium1 := 0;
      end;
      v_duepremium := v_duepremium + v_duepremium1;
      if v_duepremium = 0 then
        goto l_continue_new;
      end if;
      --过滤险种
      if p_plan_code is not null then
        if p_plan_code <> v_premium.PLAN_CODE then
          goto l_continue_new;
        end if;
      end if;
      --过滤团队
      if p_sale_group is not null then
        if p_sale_group <> nvl(v_premium.GROUP_CODE, 'NULL') then
          goto l_continue_new;
        end if;
      end if;
      --过滤客户
      if p_client_type is not null then
        if p_client_type = '1' then
          --个体
          if '1' <> nvl(v_premium.CLIENT_ATTRIBUTE, 'NULL') then
            goto l_continue_new;
          end if;
        end if;
        if p_client_type = '2' then
          --团体
          if ('0' <> nvl(v_premium.CLIENT_ATTRIBUTE, 'NULL')) and
             ('2' <> nvl(v_premium.CLIENT_ATTRIBUTE, 'NULL')) then
            goto l_continue_new;
          end if;
        end if;
      end if;
      --过滤渠道
      if p_sale_channel is not null then
        if p_sale_channel <> v_premium.CHANNEL_SOURCE_CODE then
          goto l_continue_new;
        end if;
      end if;
      --过滤币种
      if p_currency_code is not null then
        if p_currency_code <> v_premium.CURRENCY_CODE then
          goto l_continue_new;
        end if;
      end if;
      --过滤业务员
      if p_emp_code is not null then
        if p_emp_code <> nvl(v_premium.SALE_AGENT_CODE, 'NULL') then
          goto l_continue_new;
        end if;
      end if;
      --取公司机构的名称  (注意：因为表premium_info 中有些字段允许为空，故应考虑到变量初始赋值)
      begin
        select description
          into v_department_name
          from institutions
         where flex_value = v_premium.FINANCE_DEPARTMENT_CODE;
      exception
        when others then
          v_department_name := '';
      end;
      --取险种名称
      begin
        select plan_chinese_name
          into v_plan_name
          from plan_define
         where plan_code = v_premium.PLAN_CODE;
      exception
        when others then
          v_plan_name := '';
      end;
      --取币种名称
      begin
        select currency_chinese_name
          into v_currency_name
          from currency_define
         where currency_code = v_premium.CURRENCY_CODE;
      exception
        when others then
          v_currency_name := '';
      end;
      --取业务员姓名
      begin
        v_emp_name := '';
        if v_premium.SALE_AGENT_CODE is not null then
          select CEMPCNM
            into v_emp_name
            from kempcde
           where CEMPCDE = v_premium.SALE_AGENT_CODE;
        end if;
      exception
        when others then
          v_emp_name := '';
      end;
      --取团队名称
      begin
        if v_premium.GROUP_CODE is null then
          v_salegroup_name := '';
        else
          select CGRPCNM
            into v_salegroup_name
            from kgrpcde
           where CGRPCDE = v_premium.GROUP_CODE;
        end if;
      exception
        when others then
          v_salegroup_name := '';
      end;
      --取客户名称，即为个体客户或团体客户，为1 时表示个体，0 和2 都为团体
      begin
        v_client_name := '';
        if v_premium.CLIENT_ATTRIBUTE = '1' then
          v_client_name := '个体';
        elsif v_premium.CLIENT_ATTRIBUTE = '0' then
          v_client_name := '团体';
        elsif v_premium.CLIENT_ATTRIBUTE = '2' then
          v_client_name := '团体';
        end if;
      end;
      --取销售渠道名称
      begin
        select BNOCNM
          into v_salechnl_name
          from BUSINESS_SOURCE
         where BNO = v_premium.CHANNEL_SOURCE_CODE;
      exception
        when others then
          v_salechnl_name := '';
      end;
      --人民币折算
      if v_premium.CURRENCY_CODE = '02' then
        v_duepremium_sum := v_rateh * (v_premium.PREMIUM_AMOUNT);
      elsif v_premium.CURRENCY_CODE = '03' then
        v_duepremium_sum := v_rateu * (v_premium.PREMIUM_AMOUNT);
      else
        v_duepremium_sum := v_premium.PREMIUM_AMOUNT;
      end if;
      --将符合条件的记录插入到temporary_table中
      insert into epcisacct.tmp_finance_duepremium_short
        (DEPARTMENT_CODE,
         DEPARTMENT_NAME,
         POLICY_NO,
         ENDORSE_NO,
         DUE_PREMIUM,
         INSURANCE_BEGIN_DATE,
         INSURANCE_END_DATE,
         INSURED_PERSON,
         DUE_VOUCHER_NO,
         DUE_VOUCHER_DATE,
         ACCOUNT_DAYS,
         PLAN_NAME,
         CLIENT_NAME,
         SALE_CHANNEL,
         SALE_GROUP,
         EMP_NAME,
         CURRENCY,
         DUE_PREMIUM_SUM,
         CANCEL_FLAG,
         underwrite_time,
         USER_LOGINID,
         NOTICE_NO,
         SETTLE_DATE)
      values
        (v_premium.FINANCE_DEPARTMENT_CODE,
         v_department_name,
         v_premium.POLICY_NO,
         v_premium.ENDORSE_NO,
         v_premium.PREMIUM_AMOUNT,
         v_premium.INSURANCE_BEGIN_TIME,
         v_premium.INSURANCE_END_TIME,
         v_premium.INSURED_NAME,
         v_premium.DUE_VOUCHER_NO,
         v_premium.DUE_VOUCHER_DATE,
         v_premium.kaccountdays,
         v_plan_name,
         v_client_name,
         v_salechnl_name,
         v_premium.GROUP_CODE || '-' || v_salegroup_name,
         v_premium.SALE_AGENT_CODE || '-' || v_emp_name,
         v_currency_name,
         v_duepremium_sum,
         v_premium.DISABLE_FLAG,
         v_premium.underwrite_time,
         v_user_loginID,
         v_premium.term_no,
         v_premium.settle_date);
      v_count := v_count + 1;
      if v_count_limit is not null then
        if v_count >= v_count_limit then
          close c_premiumcursor;
          goto l_next3;
        end if;
      end if;
    end loop;
    close c_premiumcursor;
    <<l_next3>>
    v_count := 0;
    open c_premiumcursor2;
    loop
      <<l_continue_new2>>
      fetch c_premiumcursor2
        into v_premium;
      exit when c_premiumcursor2%notfound;
      --先过滤，后取相关辅助信息。
      select sum(DUE_AMOUNT)
        into v_duepremium
        from premium_info
       where policy_no = v_premium.policy_no;
      begin
        select sum(NPRMDUE)
          into v_duepremium
          from kcoldue
         where cplyno = v_premium.policy_no;
      exception
        when no_data_found then
          v_duepremium1 := 0;
      end;
      v_duepremium := v_duepremium + v_duepremium1;
      if v_duepremium = 0 then
        goto l_continue_new2;
      end if;
      --过滤险种
      if p_plan_code is not null then
        if p_plan_code <> v_premium.PLAN_CODE then
          goto l_continue_new2;
        end if;
      end if;
      --过滤团队
      if p_sale_group is not null then
        if p_sale_group <> nvl(v_premium.GROUP_CODE, 'NULL') then
          goto l_continue_new2;
        end if;
      end if;
      --过滤客户
      if p_client_type is not null then
        if p_client_type = '1' then
          --个体
          if '1' <> nvl(v_premium.CLIENT_ATTRIBUTE, 'NULL') then
            goto l_continue_new2;
          end if;
        end if;
        if p_client_type = '2' then
          --团体
          if ('0' <> nvl(v_premium.CLIENT_ATTRIBUTE, 'NULL')) and
             ('2' <> nvl(v_premium.CLIENT_ATTRIBUTE, 'NULL')) then
            goto l_continue_new2;
          end if;
        end if;
      end if;
      --过滤渠道
      if p_sale_channel is not null then
        if p_sale_channel <> v_premium.CHANNEL_SOURCE_CODE then
          goto l_continue_new2;
        end if;
      end if;
      --过滤币种
      if p_currency_code is not null then
        if p_currency_code <> v_premium.CURRENCY_CODE then
          goto l_continue_new2;
        end if;
      end if;
      --过滤业务员
      if p_emp_code is not null then
        if p_emp_code <> nvl(v_premium.SALE_AGENT_CODE, 'NULL') then
          goto l_continue_new2;
        end if;
      end if;
      --取公司机构的名称  (注意：因为表premium_info 中有些字段允许为空，故应考虑到变量初始赋值)
      begin
        select description
          into v_department_name
          from institutions
         where flex_value = v_premium.FINANCE_DEPARTMENT_CODE;
      exception
        when others then
          v_department_name := '';
      end;
      --取险种名称
      begin
        select plan_chinese_name
          into v_plan_name
          from plan_define
         where plan_code = v_premium.PLAN_CODE;
      exception
        when others then
          v_plan_name := '';
      end;
      --取币种名称
      begin
        select currency_chinese_name
          into v_currency_name
          from currency_define
         where currency_code = v_premium.CURRENCY_CODE;
      exception
        when others then
          v_currency_name := '';
      end;
      --取业务员姓名
      begin
        v_emp_name := '';
        if v_premium.SALE_AGENT_CODE is not null then
          select CEMPCNM
            into v_emp_name
            from kempcde
           where CEMPCDE = v_premium.SALE_AGENT_CODE;
        end if;
      exception
        when others then
          v_emp_name := '';
      end;
      --取团队名称
      begin
        if v_premium.GROUP_CODE is null then
          v_salegroup_name := '';
        else
          select CGRPCNM
            into v_salegroup_name
            from kgrpcde
           where CGRPCDE = v_premium.GROUP_CODE;
        end if;
      exception
        when others then
          v_salegroup_name := '';
      end;
      --取客户名称，即为个体客户或团体客户，为1 时表示个体，0 和2 都为团体
      begin
        v_client_name := '';
        if v_premium.CLIENT_ATTRIBUTE = '1' then
          v_client_name := '个体';
        elsif v_premium.CLIENT_ATTRIBUTE = '0' then
          v_client_name := '团体';
        elsif v_premium.CLIENT_ATTRIBUTE = '2' then
          v_client_name := '团体';
        end if;
      end;
      --取销售渠道名称
      begin
        select BNOCNM
          into v_salechnl_name
          from BUSINESS_SOURCE
         where BNO = v_premium.CHANNEL_SOURCE_CODE;
      exception
        when others then
          v_salechnl_name := '';
      end;
      --人民币折算
      if v_premium.CURRENCY_CODE = '02' then
        v_duepremium_sum := v_rateh * (v_premium.PREMIUM_AMOUNT);
      elsif v_premium.CURRENCY_CODE = '03' then
        v_duepremium_sum := v_rateu * (v_premium.PREMIUM_AMOUNT);
      else
        v_duepremium_sum := v_premium.PREMIUM_AMOUNT;
      end if;
      --将符合条件的记录插入到temporary_table中
      insert into epcisacct.tmp_finance_duepremium_short
        (DEPARTMENT_CODE,
         DEPARTMENT_NAME,
         POLICY_NO,
         ENDORSE_NO,
         DUE_PREMIUM,
         INSURANCE_BEGIN_DATE,
         INSURANCE_END_DATE,
         INSURED_PERSON,
         DUE_VOUCHER_NO,
         DUE_VOUCHER_DATE,
         ACCOUNT_DAYS,
         PLAN_NAME,
         CLIENT_NAME,
         SALE_CHANNEL,
         SALE_GROUP,
         EMP_NAME,
         CURRENCY,
         DUE_PREMIUM_SUM,
         CANCEL_FLAG,
         underwrite_time,
         USER_LOGINID,
         NOTICE_NO,
         SETTLE_DATE)
      values
        (v_premium.FINANCE_DEPARTMENT_CODE,
         v_department_name,
         v_premium.POLICY_NO,
         v_premium.ENDORSE_NO,
         v_premium.PREMIUM_AMOUNT,
         v_premium.INSURANCE_BEGIN_TIME,
         v_premium.INSURANCE_END_TIME,
         v_premium.INSURED_NAME,
         v_premium.DUE_VOUCHER_NO,
         v_premium.DUE_VOUCHER_DATE,
         v_premium.kaccountdays,
         v_plan_name,
         v_client_name,
         v_salechnl_name,
         v_premium.GROUP_CODE || '-' || v_salegroup_name,
         v_premium.SALE_AGENT_CODE || '-' || v_emp_name,
         v_currency_name,
         v_duepremium_sum,
         v_premium.DISABLE_FLAG,
         v_premium.underwrite_time,
         v_user_loginID,
         v_premium.term_no,
         v_premium.settle_date);
      v_count := v_count + 1;
      if v_count_limit is not null then
        if v_count >= v_count_limit then
          close c_premiumcursor2;
          goto l_result;
        end if;
      end if;
    end loop;
    close c_premiumcursor2;
    <<l_result>>
  --删除累积应收为0 的记录
    delete from epcisacct.tmp_finance_duepremium_short a
     where POLICY_NO in (select POLICY_NO
                           from epcisacct.tmp_finance_duepremium_short b
                          group by b.POLICY_NO
                         having sum(b.DUE_PREMIUM) = 0);
    --  -- 删除注销累积应收为 0 的纪录
    --  delete from epcisacct.tmp_finance_duepremium_short a
    --        where POLICY_NO in (
    --                      select POLICY_NO from epcisacct.tmp_finance_duepremium_short b
    --                          where b.CANCEL_flag = 'Y' group by b.POLICY_NO having sum(b.DUE_PREMIUM) = 0
    --                           ) and a.CANCEL_flag = 'Y';
    open v_cursor for
      select DEPARTMENT_CODE,
             DEPARTMENT_NAME,
             POLICY_NO,
             ENDORSE_NO,
             DUE_PREMIUM,
             INSURANCE_BEGIN_DATE,
             INSURANCE_END_DATE,
             INSURED_PERSON,
             DUE_VOUCHER_NO,
             DUE_VOUCHER_DATE,
             ACCOUNT_DAYS,
             PLAN_NAME,
             CLIENT_NAME,
             SALE_CHANNEL,
             SALE_GROUP,
             EMP_NAME,
             CURRENCY,
             DUE_PREMIUM_SUM,
             CANCEL_FLAG,
             underwrite_time,
             NOTICE_NO,
             SETTLE_DATE
        from epcisacct.tmp_finance_duepremium_short
       where USER_LOGINID = v_user_loginID
         and rownum <= 65000;
    Re_Cursor := v_cursor;
    --删除当前用户 的本次操作
    --  delete TMP_FINANCE_DUEPREMIUM_SHORT where USER_LOGINID = v_user_loginID;
  exception
    when others then
      rollback;
  END pro_finance_duepremium_short;

  procedure pro_finance_duepremium_long
  is
   --v_count integer;
   v_flage integer;
   v_ErrMsg     varchar2(500);
   v_ErrCodeVal number  :=0;

  -- type p_department_code is table of tmp_finance_duepremium_long_n.department_code%type index by binary_integer;

   type p_department_code is table of tmp_finance_duepremium_long_o.department_code%type index by binary_integer;
   type p_department_name is table of tmp_finance_duepremium_long_o.department_name%type index by binary_integer;
   type p_policy_no is table of tmp_finance_duepremium_long_o.policy_no%type index by binary_integer;
   type p_endorse_no is table of tmp_finance_duepremium_long_o.endorse_no%type index by binary_integer;
   type p_notice_no is table of tmp_finance_duepremium_long_o.notice_no%type index by binary_integer;
   type p_due_premium is table of tmp_finance_duepremium_long_o.due_premium%type index by binary_integer;
   type p_insurance_begin_date is table of tmp_finance_duepremium_long_o.insurance_begin_date%type index by binary_integer;
   type p_insurance_end_date is table of tmp_finance_duepremium_long_o.insurance_end_date%type index by binary_integer;
   type p_insured_person is table of tmp_finance_duepremium_long_o.insured_person%type index by binary_integer;
   type p_due_voucher_no is table of tmp_finance_duepremium_long_o.due_voucher_no%type index by binary_integer;
   type p_due_voucher_date is table of tmp_finance_duepremium_long_o.due_voucher_date%type index by binary_integer;
   type p_account_days is table of tmp_finance_duepremium_long_o.account_days%type index by binary_integer;
   type p_plan_code is table of tmp_finance_duepremium_long_o.plan_code%type index by binary_integer;
   type p_plan_name is table of tmp_finance_duepremium_long_o.plan_name%type index by binary_integer;
   type p_currency_code is table of tmp_finance_duepremium_long_o.currency_code%type index by binary_integer;
   type p_currency_name is table of tmp_finance_duepremium_long_o.currency_name%type index by binary_integer;
   type p_client_code is table of tmp_finance_duepremium_long_o.client_code%type index by binary_integer;
   type p_client_name is table of tmp_finance_duepremium_long_o.client_name%type index by binary_integer;
   type p_sale_agent_code is table of tmp_finance_duepremium_long_o.sale_agent_code%type index by binary_integer;
   type p_sale_agent_name is table of tmp_finance_duepremium_long_o.sale_agent_name%type index by binary_integer;
   type p_sale_channel_code is table of tmp_finance_duepremium_long_o.sale_channel_code%type index by binary_integer;
   type p_sale_channel_name is table of tmp_finance_duepremium_long_o.sale_channel_name%type index by binary_integer;
   type p_group_code is table of tmp_finance_duepremium_long_o.group_code%type index by binary_integer;
   type p_group_name is table of tmp_finance_duepremium_long_o.group_name%type index by binary_integer;
   type p_due_premium_sum is table of tmp_finance_duepremium_long_o.due_premium_sum%type index by binary_integer;
   type p_cancel_flag is table of tmp_finance_duepremium_long_o.cancel_flag%type index by binary_integer;
   type p_underwrite_time is table of tmp_finance_duepremium_long_o.underwrite_time%type index by binary_integer;
   type p_settle_date is table of tmp_finance_duepremium_long_o.settle_date%type index by binary_integer;
   type p_receipt_no is table of tmp_finance_duepremium_long_o.receipt_no%type index by binary_integer;
   type p_agent_code is table of tmp_finance_duepremium_long_o.agent_code%type index by binary_integer;
   type p_agent_chinese_name is table of tmp_finance_duepremium_long_o.agent_chinese_name%type index by binary_integer;
   type p_payment_end_date is table of tmp_finance_duepremium_long_o.payment_end_date%type index by binary_integer;
   type p_account_month is table of tmp_finance_duepremium_long_o.account_month%type index by binary_integer;
   type p_due_tax is table of tmp_finance_duepremium_long_o.due_tax%type index by binary_integer;
   type p_total_amount is table of tmp_finance_duepremium_long_o.total_amount%type index by binary_integer;

   v_department_code p_department_code;
   v_department_name p_department_name;
   v_policy_no p_policy_no;
   v_endorse_no p_endorse_no;
   v_notice_no p_notice_no;
   v_due_premium p_due_premium;
   v_insurance_begin_date p_insurance_begin_date;
   v_insurance_end_date p_insurance_end_date;
   v_insured_person p_insured_person;
   v_due_voucher_no p_due_voucher_no;
   v_due_voucher_date p_due_voucher_date;
   v_account_days p_account_days;
   v_plan_code p_plan_code;
   v_plan_name p_plan_name;
   v_currency_code p_currency_code;
   v_currency_name p_currency_name;
   v_client_code p_client_code;
   v_client_name p_client_name;
   v_sale_agent_code p_sale_agent_code;
   v_sale_agent_name p_sale_agent_name;
   v_sale_channel_code p_sale_channel_code;
   v_sale_channel_name p_sale_channel_name;
   v_group_code p_group_code;
   v_group_name p_group_name;
   v_due_premium_sum p_due_premium_sum;
   v_cancel_flag p_cancel_flag;
   v_underwrite_time p_underwrite_time;
   v_settle_date p_settle_date;
   v_receipt_no p_receipt_no;
   v_agent_code p_agent_code;
   v_agent_chinese_name p_agent_chinese_name;
   v_payment_end_date p_payment_end_date;
   v_account_month p_account_month;
   v_due_tax p_due_tax;
   v_total_amount p_total_amount;

   v_policy_no_n p_policy_no;

   cursor c_cur is
          select department_code, department_name, policy_no, endorse_no, notice_no, due_premium, insurance_begin_date, insurance_end_date,
                 insured_person, due_voucher_no, due_voucher_date, account_days, plan_code, plan_name, currency_code, currency_name,
                 client_code, client_name, sale_agent_code, sale_agent_name, sale_channel_code, sale_channel_name, group_code,
                 group_name, due_premium_sum, cancel_flag, underwrite_time, settle_date, receipt_no, agent_code, agent_chinese_name,
                 payment_end_date, account_month,due_tax,total_amount
            from tmp_finance_duepremium_long_o a;

 cursor c_n is
        select policy_no from tmp_finance_duepremium_long_n b
         group by b.policy_no having sum (b.due_premium) = 0;

  begin

 --调用处理数据
  pro_finance_duepremium_long3('01');
  pro_finance_duepremium_long3('02');
  pro_finance_duepremium_long3('03');
  pro_finance_duepremium_long3('04');
  pro_finance_duepremium_long3('05');
  pro_finance_duepremium_long3('06');
  pro_finance_duepremium_long3('07');
  pro_finance_duepremium_long3('08');
  pro_finance_duepremium_long3('09');
  pro_finance_duepremium_long3('10');
  pro_finance_duepremium_long3('11');
  pro_finance_duepremium_long3('12');
  pro_finance_duepremium_long3('13');
  pro_finance_duepremium_long3('14');
  pro_finance_duepremium_long3('15');
  pro_finance_duepremium_long3('16');
  pro_finance_duepremium_long3('17');
  pro_finance_duepremium_long3('18');
  pro_finance_duepremium_long3('19');
  pro_finance_duepremium_long3('20');
  pro_finance_duepremium_long3('21');
  pro_finance_duepremium_long3('22');
  pro_finance_duepremium_long3('23');
  pro_finance_duepremium_long3('24');
  pro_finance_duepremium_long3('25');
  pro_finance_duepremium_long3('26');
  pro_finance_duepremium_long3('27');
  pro_finance_duepremium_long3('28');
  pro_finance_duepremium_long3('29');
  pro_finance_duepremium_long3('30');
  pro_finance_duepremium_long3('31');
  pro_finance_duepremium_long3('32');
  pro_finance_duepremium_long3('33');
  pro_finance_duepremium_long3('34');
  pro_finance_duepremium_long3('35');
  pro_finance_duepremium_long3('36');
  pro_finance_duepremium_long3('41');
  pro_finance_duepremium_long3('42');
  pro_finance_duepremium_long3('43');
  pro_finance_duepremium_long3('44');
  pro_finance_duepremium_long3('45');
  pro_finance_duepremium_long3('46');
  pro_finance_duepremium_long3('47');
  pro_finance_duepremium_long3('48');
  pro_finance_duepremium_long3('49');
  pro_finance_duepremium_long3('91');
 insert into  epcis_job_log values (sysdate, 0,1, 0, '暂无','pro_finance_duepremium_long 调用处理 k 表完成',sys_guid());
      commit;
  --处理 p表数据
  pro_finance_duepremium_long4;
 insert into  epcis_job_log values (sysdate, 0,2, 0, '暂无','pro_finance_duepremium_long 调用处理 p 表完成',sys_guid());
      commit;
  --此处调用需要tmp_finance_duepremium_long_o和tmp_finance_duepremium_long_n都建立且授权
  pro_finance_duepremium_long2(1, v_flage);
  --1失败、0成功
  if v_flage = 0 then
      insert into  epcis_job_log values (sysdate, 0,3, 0, '暂无','pro_finance_duepremium_long 调用处理 pro_finance_duepremium_long2 表完成',sys_guid());
      commit;
  else
      insert into  epcis_job_log values (sysdate, 0,31, 0, '失败','pro_finance_duepremium_long 调用处理 pro_finance_duepremium_long2 表失败，处理异常结束',sys_guid());
      commit;
     goto end_long;
  end if;
--预计insert 560万,初始化的时候使用一次(可以放到第一次初始化的时候执行)
  open c_cur;
  loop
  fetch c_cur bulk collect into v_department_code, v_department_name, v_policy_no, v_endorse_no, v_notice_no,
                                v_due_premium, v_insurance_begin_date, v_insurance_end_date, v_insured_person,
                                v_due_voucher_no, v_due_voucher_date, v_account_days, v_plan_code, v_plan_name,
                                v_currency_code, v_currency_name, v_client_code, v_client_name, v_sale_agent_code,
                                v_sale_agent_name, v_sale_channel_code, v_sale_channel_name, v_group_code, v_group_name,
                                v_due_premium_sum, v_cancel_flag, v_underwrite_time, v_settle_date, v_receipt_no, v_agent_code,
                                v_agent_chinese_name, v_payment_end_date, v_account_month, v_due_tax, v_total_amount  limit 30000;

  exit when not v_department_code.exists(1);

  forall u_cur in v_department_code.first  ..  v_department_code.last
      insert into tmp_finance_duepremium_long_n
                 (department_code, department_name, policy_no, endorse_no, notice_no, due_premium,
                  insurance_begin_date, insurance_end_date, insured_person, due_voucher_no, due_voucher_date,
                  account_days, plan_code, plan_name, currency_code, currency_name, client_code, client_name,
                  sale_agent_code, sale_agent_name, sale_channel_code, sale_channel_name, group_code, group_name,
                  due_premium_sum, cancel_flag, underwrite_time, settle_date, receipt_no, agent_code, agent_chinese_name,
                  payment_end_date, account_month, due_tax, total_amount)
           values(v_department_code(u_cur),v_department_name(u_cur),v_policy_no(u_cur),v_endorse_no(u_cur),v_notice_no(u_cur),v_due_premium(u_cur),
                  v_insurance_begin_date(u_cur),v_insurance_end_date(u_cur),v_insured_person(u_cur),v_due_voucher_no(u_cur),v_due_voucher_date(u_cur),
                  v_account_days(u_cur),v_plan_code(u_cur),v_plan_name(u_cur),v_currency_code(u_cur),v_currency_name(u_cur),v_client_code(u_cur),v_client_name(u_cur),
                  v_sale_agent_code(u_cur),v_sale_agent_name(u_cur),v_sale_channel_code(u_cur),v_sale_channel_name(u_cur),v_group_code(u_cur),v_group_name(u_cur),
                  v_due_premium_sum(u_cur),v_cancel_flag(u_cur),v_underwrite_time(u_cur),v_settle_date(u_cur),v_receipt_no(u_cur),v_agent_code(u_cur),v_agent_chinese_name(u_cur),
                  v_payment_end_date(u_cur),v_account_month(u_cur), v_due_tax(u_cur), v_total_amount(u_cur));
   commit;

  end loop;
-------------------------------------------------------------------------------
commit;
 insert into  epcis_job_log values (sysdate, 0,4, 0, '暂无','pro_finance_duepremium_long 调用处理 insert tmp_finance_duepremium_long_n 表完成',sys_guid());
      commit;
--------只初始化的时候使用一次------------------
  --删除累积应收为0 的记录
/*  delete \*+ direct *\ from tmp_finance_duepremium_long_n a
   where policy_no in (select policy_no from tmp_finance_duepremium_long_n b
                                      group by b.policy_no having sum (b.due_premium) = 0);
  commit;
*/

  --删除累积应收为0 的记录,替换上面注释语句
  open c_n;
  loop
  fetch c_n bulk collect into v_policy_no_n limit 30000;

     exit when not v_policy_no_n.exists(1);

     forall i in v_policy_no_n.first  ..  v_policy_no_n.last
        delete /*+ direct */ from tmp_finance_duepremium_long_n a
         where policy_no = v_policy_no_n(i);
     commit;
  end loop;

  -- 删除注销累积应收为 0 的纪录
--  delete /*+ direct */  from tmp_finance_duepremium_long_n a
--        where policy_no in (select policy_no from tmp_finance_duepremium_long_n b
--                                          where  b.cancel_flag = 'Y' group by b.policy_no  having sum(b.due_premium) = 0
--                                         )
--            and cancel_flag = 'Y';
--  commit;

   insert into  epcis_job_log values (sysdate, 0,5, 0, '暂无','pro_finance_duepremium_long 调用处理完成',sys_guid());
   commit;
   <<end_long>>
   null;
   exception
     when others then
          v_ErrCodeVal := sqlcode;
          v_ErrMsg := substr('获取pro_finance_duepremium_long过程出错'|| sqlerrm, 1, 500);
          insert into  epcis_job_log values (sysdate, 0,0, v_ErrCodeVal, v_ErrMsg,'pro_finance_duepremium_long 调用处理失败',sys_guid());
          commit;
  end pro_finance_duepremium_long;


--2009-01-16 liuyifu 修改账龄为动态，放在最后查询返回时动态计算得到,增加缴费止期字段和月帐龄字段
  procedure pro_finance_duepremium_long3(department in varchar2)
    is
    --查询记录的某些字段的辅助信息变量
    v_department_name    varchar2(240);
    v_emp_name           varchar2(20);
    v_client_name        varchar2(20);
    v_salechnl_name      varchar2(20);
    v_salegroup_name     varchar2(100);
    v_plan_name          varchar2(40);
    v_currency_name      varchar2(20);
    --v_duepremium         number(16,2);
    v_duepremium_sum     number(24,6);
  --汇率值变量
  v_rateh            number(8,4);
  v_rateu            number(8,4);
    --针对新系统的通知单号变量
    --v_notice_no       varchar2(40);
    --v_collect_pay_no  varchar2(40);
  --控制commit的参数变量
  v_count           number(8) default 0;
    --与老系统对应的查询游标（收费日期为空）
    cursor c_kcolduecursor is
        select cdno,                                --分公司机构
                   cplyno,                              --保单号
                   cedrno,                              --批单号
                   ntermno,                             --交费期别
                   dplystr,                             --保单责任起期，即保险起期
                   dplyend,                             --保单责任止期，即保险止期
                   cpaynme,                             --缴费人名称，暂时作为被保险人
                   caccno,                              --记帐编号，即应收凭证号
                   daccdte,                             --记帐日期，即应收制证日期
                   nprmdue,                             --应收保费，即金额
                   cinscde,                             --险种代码
                   substr(c_magic_set,2,1) kclientcde,  --客户代码，即业务类型，个体客户：1 ；团体客户：2
                   substr(c_magic_set,1,1) ksalechnl,   --渠道代码，指销售渠道
                   cparno,                              --团队代码
                   cempcde,                             --业务员代码
                   ccurno ,                             --币种
                   cancel ,                             --失效标志
                   dfcd,                                --核保日期
                   '0' as kaccountdays,                 --日帐龄，修改为动态，放在最后查询返回时动态计算得到
                   '0' as kaccountmonth,                --月帐龄，修改为动态，放在最后查询返回时动态计算得到
                   dcaldte,                             --结算日期
                   crctno,   --收据号
                   cagtcde agent_code,
                   (select agent_chinese_name  from agent_define where agent_code = t.cagtcde) agent_chinese_name,
                   dpayend                              --缴费止期
         from kcoldue t /*partition (varpartition) */
       where dcoldte is null --decode(dcoldte,null,1,0)=1      --实收制证日期is null
           and caccno is not null            --记帐编号
           and cdno like department||'%'        --分公司机构
           and greatest(dplystr, dfcd) between to_date('19990101','yyyymmdd') and last_day(trunc(sysdate,'dd'))+1 --交费起期/创建日期
--         and cplyno not in (select cplyno from no_stat_kcoldue)
           and cinscde <> 'A24'
           and nvl(hide_flag, 'N') <> 'Y'
           and daccdte >= to_date('19990101','yyyymmdd');
    --与老系统对应的查询游标（截至日期后的数据）
    cursor c_kcolduecursor2 is
        select cdno,                                --分公司机构
                   cplyno,                              --保单号
                   cedrno,                              --批单号
                   ntermno,                             --交费期别
                   dplystr,                             --保单责任起期，即保险起期
                   dplyend,                             --保单责任止期，即保险止期
                   cpaynme,                             --缴费人名称，暂时作为被保险人
                   caccno,                              --记帐编号，即应收凭证号
                   daccdte,                             --记帐日期，即应收制证日期
                   nprmdue,                             --应收保费，即金额
                   cinscde,                             --险种代码
                   substr(c_magic_set,2,1) kclientcde,  --客户代码，即业务类型，个体客户：1 ；团体客户：2
                   substr(c_magic_set,1,1) ksalechnl,   --渠道代码，指销售渠道
                   cparno,                              --团队代码
                   cempcde,                             --业务员代码
                   ccurno ,                             --币种
                   cancel ,                             --失效标志
                   dfcd,                                --核保日期
                   '0' as kaccountdays,                 --帐龄，修改为动态，放在最后查询返回时动态计算得到
                   '0' as kaccountmonth,                --月帐龄，修改为动态，放在最后查询返回时动态计算得到
                   dcaldte,                             --结算日期
                   crctno,   --收据号
                   cagtcde agent_code,
                   (select agent_chinese_name  from agent_define where agent_code = t.cagtcde) agent_chinese_name,
                   dpayend                              --缴费止期
        from kcoldue t /*partition (varpartition)*/
      where dcoldte > trunc(sysdate)+1  --实收制证日期
          and caccno is not null
          and cdno like department||'%'
          and greatest(dplystr, dfcd) between to_date('19990101','yyyymmdd') and last_day(trunc(sysdate,'dd'))+1
--         and cplyno not in (select cplyno from no_stat_kcoldue)
          and cinscde <> 'A24'
          and nvl(hide_flag, 'N') <> 'Y'
          and daccdte >= to_date('19990101','yyyymmdd')
          and 1 = 2 ;  --对于长期应收，不存在 dcoldte > trunc(sysdate)+1 的情况，所以此sql可以省略
    --v_kcoldue c_kcolduecursor%rowtype;
    begin
  --分别取港币和美元的汇率
  v_rateh := pkg_general_tools.get_exchange_rate('02','01',sysdate);
  v_rateu := pkg_general_tools.get_exchange_rate('03','01',sysdate);
  v_count := 0;
  for v_kcoldue in c_kcolduecursor loop
      --取公司机构的名称  (注意：因为表kcoldue中有些字段允许为空，故应考虑到变量初始赋值；同时考虑select取值失败的情况)
    begin
          select description into v_department_name from institutions where flex_value = v_kcoldue.cdno;
    exception
        when others then
            v_department_name := '';
      end;
      --取险种名称
      begin
        select plan_chinese_name into v_plan_name from plan_define where plan_code = v_kcoldue.cinscde;
    exception
        when others then
            v_plan_name := '';
      end;
      --取币种名称
      begin
          select currency_chinese_name into v_currency_name from currency_define where currency_code = v_kcoldue.ccurno;
      exception
        when others then
            v_currency_name := '';
      end;
    --取业务员姓名
      begin
          if v_kcoldue.cempcde is null then
            v_emp_name := '';
        else
              select cempcnm into v_emp_name from kempcde where cempcde = v_kcoldue.cempcde;
        end if;
    exception
        when others then
            v_emp_name := '';
      end;
      --取客户名称，即为个体客户或团体客户
      v_client_name := '';
      if v_kcoldue.kclientcde = '1' then
          v_client_name := '个体';
      elsif v_kcoldue.kclientcde = '2' then
        v_client_name := '团体';
      end if;
      --取销售渠道名称
      begin
            select bnocnm into v_salechnl_name from business_source where bno = v_kcoldue.ksalechnl;
      exception
        when others then
            v_salechnl_name := '';
      end;
    --取团队名称
      begin
          if v_kcoldue.cparno is null then
            v_salegroup_name := '';
        else
            select cgrpcnm into v_salegroup_name from kgrpcde where cgrpcde = v_kcoldue.cparno;
        end if;
    exception
        when others then
            v_salegroup_name := '';
      end;
      --人民币折算
      if v_kcoldue.ccurno = '02' then
          v_duepremium_sum := v_rateh *(v_kcoldue.nprmdue) ;
      elsif v_kcoldue.ccurno = '03' then
    v_duepremium_sum := v_rateu * (v_kcoldue.nprmdue);
      else
    v_duepremium_sum := v_kcoldue.nprmdue;
      end if;
      --将符合条件的记录插入到temporary_table中
      insert  into tmp_finance_duepremium_long_o(
                                                department_code,
                                                department_name,
                                                policy_no,
                                                endorse_no,
                                                due_premium,
                                                insurance_begin_date,
                                                insurance_end_date,
                                                insured_person,
                                                due_voucher_no,
                                                due_voucher_date,
                                                account_days,
                                                plan_code,
                                                plan_name,
                                                currency_code,
                                                currency_name,
                                                client_code,
                                                client_name,
                                                sale_agent_code,
                                                sale_agent_name,
                                                sale_channel_code,
                                                sale_channel_name,
                                                group_code,
                                                group_name,
                                                due_premium_sum,
                                                cancel_flag,
                                                underwrite_time,
                                                notice_no,
                                                settle_date,
                                                receipt_no,
                                                agent_code,
                                                agent_chinese_name,
                                                payment_end_date,
                                                account_month,
                                                due_tax,
                                                total_amount
                                    )
                               values(
                                                v_kcoldue.cdno,
                                                v_department_name,
                                                v_kcoldue.cplyno,
                                                v_kcoldue.cedrno,
                                                v_kcoldue.nprmdue,
                                                v_kcoldue.dplystr,
                                                v_kcoldue.dplyend,
                                                v_kcoldue.cpaynme,
                                                v_kcoldue.caccno,
                                                v_kcoldue.daccdte,
                                                v_kcoldue.kaccountdays,
                                                v_kcoldue.cinscde,
                                                v_plan_name,
                                                v_kcoldue.ccurno,
                                                v_currency_name,
                                                decode(v_kcoldue.kclientcde,'0','2',v_kcoldue.kclientcde),
                                                v_client_name,
                                                v_kcoldue.cempcde,
                                                v_emp_name,
                                                v_kcoldue.ksalechnl,
                                                v_salechnl_name,
                                                v_kcoldue.cparno,
                                                v_salegroup_name,
                                                v_duepremium_sum,
                                                v_kcoldue.cancel,
                                                v_kcoldue.dfcd,
                                                v_kcoldue.ntermno,
                                                v_kcoldue.dcaldte,
                                                v_kcoldue.crctno,
                                                v_kcoldue.agent_code,
                                                v_kcoldue.agent_chinese_name,
                                                v_kcoldue.dpayend,
                                                v_kcoldue.kaccountmonth,
                                                0,
                                                v_kcoldue.nprmdue
                                               );
      v_count := v_count + 1;
    if v_count >= 5000 then
       commit;
       v_count := 0;
    end if;
  end loop;
  -- close c_kcolduecursor;
  commit;
  v_count := 0;
  for v_kcoldue in c_kcolduecursor2 loop
      --取公司机构的名称  (注意：因为表kcoldue中有些字段允许为空，故应考虑到变量初始赋值；同时考虑select取值失败的情况)
    begin
          select description into v_department_name from institutions where flex_value = v_kcoldue.cdno;
    exception
        when others then
            v_department_name := '';
      end;
      --取险种名称
      begin
          select plan_chinese_name into v_plan_name from plan_define where plan_code = v_kcoldue.cinscde;
      exception
        when others then
            v_plan_name := '';
      end;
    --取币种名称
      begin
          select currency_chinese_name into v_currency_name from currency_define where currency_code = v_kcoldue.ccurno;
      exception
        when others then
            v_currency_name := '';
      end;
    --取业务员姓名
      begin
          if v_kcoldue.cempcde is null then
            v_emp_name := '';
        else
              select cempcnm into v_emp_name from kempcde where cempcde = v_kcoldue.cempcde;
        end if;
    exception
        when others then
            v_emp_name := '';
      end;
      --取客户名称，即为个体客户或团体客户
      v_client_name := '';
      if v_kcoldue.kclientcde = '1' then
          v_client_name := '个体';
      elsif v_kcoldue.kclientcde = '2' then
        v_client_name := '团体';
      end if;
      --取销售渠道名称
      begin
            select bnocnm into v_salechnl_name from business_source where bno = v_kcoldue.ksalechnl;
      exception
        when others then
            v_salechnl_name := '';
      end;
    --取团队名称
      begin
          if v_kcoldue.cparno is null then
            v_salegroup_name := '';
        else
          select cgrpcnm into v_salegroup_name from kgrpcde where cgrpcde = v_kcoldue.cparno;
        end if;
    exception
        when others then
            v_salegroup_name := '';
      end;
      --人民币折算
      if v_kcoldue.ccurno = '02' then
          v_duepremium_sum := v_rateh * (v_kcoldue.nprmdue);
      elsif v_kcoldue.ccurno = '03' then
    v_duepremium_sum := v_rateu * (v_kcoldue.nprmdue);
      else
    v_duepremium_sum := v_kcoldue.nprmdue;
      end if;
      --将符合条件的记录插入到temporary_table中
      insert  into tmp_finance_duepremium_long_o(
                                        department_code,
                                        department_name,
                                        policy_no,
                                        endorse_no,
                                        due_premium,
                                        insurance_begin_date,
                                        insurance_end_date,
                                        insured_person,
                                        due_voucher_no,
                                        due_voucher_date,
                                        account_days,
                                        plan_code,
                                        plan_name,
                                        currency_code,
                                        currency_name,
                                        client_code,
                                        client_name,
                                        sale_agent_code,
                                        sale_agent_name,
                                        sale_channel_code,
                                        sale_channel_name,
                                        group_code,
                                        group_name,
                                        due_premium_sum,
                                        cancel_flag,
                                        underwrite_time,
                                        notice_no,
                                        settle_date,
                                        receipt_no,
                                        agent_code,
                                        agent_chinese_name,
                                        payment_end_date,
                                        account_month,
                                        due_tax,
                                        total_amount
                                    )
                               values(
                                       v_kcoldue.cdno,
                                       v_department_name,
                                       v_kcoldue.cplyno,
                                       v_kcoldue.cedrno,
                                       v_kcoldue.nprmdue,
                                       v_kcoldue.dplystr,
                                       v_kcoldue.dplyend,
                                       v_kcoldue.cpaynme,
                                       v_kcoldue.caccno,
                                       v_kcoldue.daccdte,
                                       v_kcoldue.kaccountdays,
                                       v_kcoldue.cinscde,
                                       v_plan_name,
                                       v_kcoldue.ccurno,
                                       v_currency_name,
                                       decode(v_kcoldue.kclientcde,'0','2',v_kcoldue.kclientcde),
                                       v_client_name,
                                       v_kcoldue.cempcde,
                                       v_emp_name,
                                       v_kcoldue.ksalechnl,
                                       v_salechnl_name,
                                       v_kcoldue.cparno,
                                       v_salegroup_name,
                                       v_duepremium_sum,
                                       v_kcoldue.cancel,
                                       v_kcoldue.dfcd,
                                       v_kcoldue.ntermno,
                                       v_kcoldue.dcaldte,
                                       v_kcoldue.crctno,
                                       v_kcoldue.agent_code,
                                       v_kcoldue.agent_chinese_name,
                                       v_kcoldue.dpayend,
                                       v_kcoldue.kaccountmonth,
                                       0,
                                        v_kcoldue.nprmdue
                                     );
      v_count := v_count + 1;
    if v_count >= 5000 then
       commit;
       v_count := 0;
    end if;
  end loop;
  commit;
    end pro_finance_duepremium_long3;


--2009-01-16 liuyifu 修改账龄为动态，放在最后查询返回时动态计算得到,增加缴费止期字段和月帐龄字段
  procedure pro_finance_duepremium_long4
    is
    --查询记录的某些字段的辅助信息变量
    v_department_name    varchar2(240);
    v_emp_name           varchar2(20);
    v_client_name        varchar2(20);
    v_salechnl_name      varchar2(20);
    v_salegroup_name     varchar2(100);
    v_plan_name          varchar2(40);
    v_currency_name      varchar2(20);
     v_duepremium_sum     number(24,6);
  --汇率值变量
  v_rateh            number(8,4);
  v_rateu            number(8,4);
    --针对新系统的通知单号变量
    --v_notice_no       varchar2(40);
    --v_collect_pay_no  varchar2(40);
  --控制commit的参数变量
  v_count           number(8) default 0;
  --对新系统对应的查询游标（实收凭证号为空）
    cursor c_premiumcursor is
        select a.finance_department_code ,
               a.policy_no  ,
               a.endorse_no  ,
               a.term_no ,
               a.insurance_begin_time ,
               a.insurance_end_time ,
               a.insured_name ,
               a.due_voucher_no ,
               a.due_voucher_date ,
               b.premium_amount ,
               b.plan_code ,
               a.client_attribute,
               a.channel_source_code ,
               a.group_code ,
               a.sale_agent_code,
               a.currency_code ,
               a.disable_flag ,
               a.underwrite_time   ,
               '0'   as kaccountdays,   --日帐龄，修改为动态，放在最后查询返回时动态计算得到
               '0'   as kaccountmonth,  --月帐龄，修改为动态，放在最后查询返回时动态计算得到
               a.settle_date,
               a.receipt_no,  --收据号
               a.agent_code,
               (select c.agent_chinese_name from agent_define c where c.agent_code = a.agent_code) agent_chinese_name,
               payment_end_date,
               nvl((select c.amount from premium_tax_plan c where c.receipt_no=b.receipt_no and c.plan_code=b.plan_code),0) due_tax
          from premium_info a, premium_plan b
        where actual_voucher_no is null
            and due_voucher_no is not null
            and greatest(insurance_begin_time, underwrite_time) between to_date('19990101','yyyy-mm-dd')  and last_day(trunc(sysdate,'dd'))+1
            and a.receipt_no = b.receipt_no
            and b.plan_code <> 'A24'
            and a.due_voucher_date >= to_date('19990101','yyyy-mm-dd');
    --对新系统对应的查询游标（截至日期后的数据）
    cursor c_premiumcursor2 is
        select a.finance_department_code as finance_department_code,
               a.policy_no as policy_no,
               a.endorse_no as endorse_no,
               a.term_no as term_no,
               a.insurance_begin_time as insurance_begin_time,
               a.insurance_end_time as insurance_end_time,
               a.insured_name as insured_name,
               a.due_voucher_no as due_voucher_no,
               a.due_voucher_date as due_voucher_date,
               b.premium_amount as premium_amount,
               b.plan_code as plan_code,
               a.client_attribute as client_attribute,
               a.channel_source_code as channel_source_code,
               a.group_code as group_code,
               a.sale_agent_code as sale_agent_code,
               a.currency_code as currency_code,
               a.disable_flag as disable_flag,
               a.underwrite_time as underwrite_time ,
               '0'   as kaccountdays,   --日帐龄，修改为动态，放在最后查询返回时动态计算得到
               '0'   as kaccountmonth,  --月帐龄，修改为动态，放在最后查询返回时动态计算得到
               a.settle_date,
               a.receipt_no,  --收据号
               a.agent_code,
               (select c.agent_chinese_name from agent_define c where c.agent_code = a.agent_code) agent_chinese_name,
               payment_end_date,
               nvl((select c.amount from premium_tax_plan c where c.receipt_no=b.receipt_no and c.plan_code=b.plan_code),0) due_tax
         from premium_info a, premium_plan b
       where actual_voucher_date > trunc(sysdate)+1
           and due_voucher_no is not null
           and greatest(insurance_begin_time, underwrite_time)  between to_date('19990101','yyyy-mm-dd') and last_day(trunc(sysdate,'dd'))+1
           and a.receipt_no = b.receipt_no
           and b.plan_code <> 'A24'
           and a.due_voucher_date >= to_date('19990101','yyyy-mm-dd')
           and 1 = 2 ;  --对于长期应收，不存在 actual_voucher_date > trunc(sysdate)+1  的情况，所以此sql可以省略
      --v_premium c_premiumcursor%rowtype;
    begin
  --分别取港币和美元的汇率
  v_rateh := pkg_general_tools.get_exchange_rate('02','01',sysdate);
  v_rateu := pkg_general_tools.get_exchange_rate('03','01',sysdate);
  v_count := 0;
    for v_premium in c_premiumcursor loop
      --取公司机构的名称  (注意：因为表premium_info 中有些字段允许为空，故应考虑到变量初始赋值)
    begin
          select description into v_department_name from institutions where flex_value = v_premium.FINANCE_DEPARTMENT_CODE;
    exception
        when others then
            v_department_name := '';
      end;
        --取险种名称
        begin
            select plan_chinese_name into v_plan_name from plan_define where plan_code = v_premium.plan_code;
        exception
        when others then
            v_plan_name := '';
      end;
    --取币种名称
        begin
            select currency_chinese_name into v_currency_name from currency_define where currency_code = v_premium.currency_code;
        exception
        when others then
            v_currency_name := '';
      end;
    --取业务员姓名
        begin
      v_emp_name := '';
            if v_premium.sale_agent_code is not null then
                select cempcnm into v_emp_name from kempcde where cempcde = v_premium.sale_agent_code;
            end if;
        exception
            when others then
                v_emp_name := '';
        end;
      --取团队名称
      begin
          if v_premium.group_code is null then
          v_salegroup_name := '';
        else
        select cgrpcnm into v_salegroup_name from kgrpcde where cgrpcde = v_premium.group_code;
        end if;
      exception
      when others then
          v_salegroup_name := '';
    end;
        --取客户名称，即为个体客户或团体客户，为1 时表示个体，0 和2 都为团体
      begin
            v_client_name := '';
            if v_premium.client_attribute = '1' then
                v_client_name := '个体';
            elsif v_premium.client_attribute = '0' then
                v_client_name := '团体';
      elsif v_premium.client_attribute = '2' then
                v_client_name := '团体';
            end if;
      end;
        --取销售渠道名称
        begin
            select bnocnm into v_salechnl_name from business_source where bno = v_premium.channel_source_code;
    exception
        when others then
            v_salechnl_name := '';
      end;
        --人民币折算
        if v_premium.currency_code = '02' then
            v_duepremium_sum := v_rateh * (v_premium.premium_amount);
        elsif v_premium.currency_code = '03' then
            v_duepremium_sum := v_rateu * (v_premium.premium_amount);
      else
          v_duepremium_sum := v_premium.premium_amount;
        end if;
      --将符合条件的记录插入到temporary_table中
      insert into tmp_finance_duepremium_long_o(
                              department_code,
                              department_name,
                              policy_no,
                              endorse_no,
                              due_premium,
                              insurance_begin_date,
                              insurance_end_date,
                              insured_person,
                              due_voucher_no,
                              due_voucher_date,
                              account_days,
                              plan_code,
                              plan_name,
                              currency_code,
                              currency_name,
                              client_code,
                              client_name,
                              sale_agent_code,
                              sale_agent_name,
                              sale_channel_code,
                              sale_channel_name,
                              group_code,
                              group_name,
                              due_premium_sum,
                              cancel_flag,
                              underwrite_time,
                              notice_no,
                              settle_date,
                              receipt_no,
                              agent_code,
                              agent_chinese_name,
                              payment_end_date,
                              account_month,
                              due_tax,
                              total_amount
                             )
                       values(
                               v_premium.finance_department_code,
                               v_department_name,
                               v_premium.policy_no,
                               v_premium.endorse_no,
                               v_premium.premium_amount,
                               v_premium.insurance_begin_time,
                               v_premium.insurance_end_time,
                               v_premium.insured_name,
                               v_premium.due_voucher_no,
                               v_premium.due_voucher_date,
                               v_premium.kaccountdays,
                               v_premium.plan_code,
                               v_plan_name,
                               v_premium.currency_code,
                               v_currency_name,
                               decode(v_premium.client_attribute,'0','2',v_premium.client_attribute),
                               v_client_name,
                               v_premium.sale_agent_code,
                               v_emp_name,
                               v_premium.channel_source_code,
                               v_salechnl_name,
                               v_premium.group_code,
                               v_salegroup_name,
                               v_duepremium_sum,
                               v_premium.disable_flag,
                               v_premium.underwrite_time,
                               v_premium.term_no,
                               v_premium.settle_date,
                               v_premium.receipt_no,
                               v_premium.agent_code,
                               v_premium.agent_chinese_name,
                               v_premium.payment_end_date,
                               v_premium.kaccountmonth,
                               v_premium.due_tax,
                               v_premium.due_tax+v_premium.premium_amount
                              );
        v_count := v_count + 1;
    if v_count >= 5000 then
       commit;
       v_count := 0;
    end if;
  end loop;
  commit;
  v_count := 0;
    for v_premium in c_premiumcursor2 loop
      --取公司机构的名称  (注意：因为表premium_info 中有些字段允许为空，故应考虑到变量初始赋值)
    begin
          select description into v_department_name from institutions where flex_value = v_premium.finance_department_code;
    exception
        when others then
            v_department_name := '';
      end;
        --取险种名称
        begin
            select plan_chinese_name into v_plan_name from plan_define where plan_code = v_premium.plan_code;
        exception
        when others then
            v_plan_name := '';
      end;
    --取币种名称
        begin
            select currency_chinese_name into v_currency_name from currency_define where currency_code = v_premium.currency_code;
        exception
        when others then
            v_currency_name := '';
      end;
    --取业务员姓名
        begin
      v_emp_name := '';
            if v_premium.sale_agent_code is not null then
                select cempcnm into v_emp_name from kempcde where cempcde = v_premium.sale_agent_code;
            end if;
        exception
            when others then
                v_emp_name := '';
        end;
      --取团队名称
      begin
          if v_premium.group_code is null then
          v_salegroup_name := '';
        else
        select cgrpcnm into v_salegroup_name from kgrpcde where cgrpcde = v_premium.group_code;
        end if;
      exception
      when others then
          v_salegroup_name := '';
    end;
        --取客户名称，即为个体客户或团体客户，为1 时表示个体，0 和2 都为团体
      begin
            v_client_name := '';
            if v_premium.client_attribute = '1' then
                v_client_name := '个体';
            elsif v_premium.client_attribute = '0' then
                v_client_name := '团体';
      elsif v_premium.client_attribute = '2' then
                v_client_name := '团体';
            end if;
      end;
        --取销售渠道名称
        begin
            select bnocnm into v_salechnl_name from business_source where bno = v_premium.channel_source_code;
    exception
        when others then
            v_salechnl_name := '';
      end;
        --人民币折算
        if v_premium.currency_code = '02' then
            v_duepremium_sum := v_rateh * (v_premium.premium_amount);
        elsif v_premium.currency_code = '03' then
            v_duepremium_sum := v_rateu * (v_premium.premium_amount);
      else
          v_duepremium_sum := v_premium.premium_amount;
        end if;
      --将符合条件的记录插入到temporary_table中
      insert into tmp_finance_duepremium_long_o(
                              department_code,
                              department_name,
                              policy_no,
                              endorse_no,
                              due_premium,
                              insurance_begin_date,
                              insurance_end_date,
                              insured_person,
                              due_voucher_no,
                              due_voucher_date,
                              account_days,
                              plan_code,
                              plan_name,
                              currency_code,
                              currency_name,
                              client_code,
                              client_name,
                              sale_agent_code,
                              sale_agent_name,
                              sale_channel_code,
                              sale_channel_name,
                              group_code,
                              group_name,
                              due_premium_sum,
                              cancel_flag,
                              underwrite_time,
                              notice_no,
                              settle_date,
                              receipt_no,
                              agent_code,
                              agent_chinese_name,
                              payment_end_date,
                              account_month,
                              due_tax,
                              total_amount
                             )
                       values(v_premium.finance_department_code,
                              v_department_name,
                              v_premium.policy_no,
                              v_premium.endorse_no,
                              v_premium.premium_amount,
                              v_premium.insurance_begin_time,
                              v_premium.insurance_end_time,
                              v_premium.insured_name,
                              v_premium.due_voucher_no,
                              v_premium.due_voucher_date,
                              v_premium.kaccountdays,
                              v_premium.plan_code,
                              v_plan_name,
                              v_premium.currency_code,
                              v_currency_name,
                              decode(v_premium.client_attribute,'0','2',v_premium.client_attribute),
                              v_client_name,
                              v_premium.sale_agent_code,
                              v_emp_name,
                              v_premium.channel_source_code,
                              v_salechnl_name,
                              v_premium.group_code,
                              v_salegroup_name,
                              v_duepremium_sum,
                              v_premium.disable_flag,
                              v_premium.underwrite_time,
                              v_premium.term_no,
                              v_premium.settle_date,
                              v_premium.receipt_no,
                              v_premium.agent_code,
                              v_premium.agent_chinese_name,
                              v_premium.payment_end_date,
                              v_premium.kaccountmonth,
                              v_premium.due_tax,
                              v_premium.due_tax+v_premium.premium_amount
                             );
        v_count := v_count + 1;
    if v_count >= 5000 then
       commit;
       v_count := 0;
    end if;
  end loop;
  -- close c_premiumcursor2;
  commit;
    end pro_finance_duepremium_long4;


--------------------------------------------------------------------------
--created by liuyifu 2008-10-10
--pro_finance_duepremium_long2主要处理内容如下：
--1、批单对应的原始保单的结算日期不在99年1月1日（含）以后的数据
--2、删除tmp_finance_duepremium_long_n表累积应收为0 的记录
--modified by liuyifu 20090310修改保单有批单的情况，如果批单对应的保单结算日期在1999年1月1日之前的，这样的数据是整个保单（原始保单和所有批单）都不显示
--------------------------------------------------------------------------
   procedure  pro_finance_duepremium_long2 (n_flag in number, out_flag out number)
  is
      v_count number;
      i_count number;
      i_count1 number;
      v_ErrMsg     varchar2(500);
      v_ErrCodeVal number  :=0;

     type p_policy_no  is table of tmp_finance_duepremium_long_n.policy_no%type index by binary_integer;
     type p_policy_no1 is table of tmp_finance_duepremium_long_o.policy_no%type index by binary_integer;
     v_policy_no_n p_policy_no;
     v_policy_no_n1 p_policy_no1;

      --这里可以分第一次初始化和每天调度，可借用调用时间减少循环运算
     --总应收数据，包括金额累加为0 的数据
      cursor c_loging is select t.policy_no
                                   from tmp_finance_duepremium_long_o t
                                 where t.endorse_no is not null
                                  group by t.policy_no;
      --每天更新数据
      cursor c_loging1 is select t.policy_no
                                    from tmp_k_p_update_data_everyday t where t.endorse_no is not null
                                   group by t.policy_no;

      cursor c_n is select policy_no from tmp_finance_duepremium_long_n b
                     group by b.policy_no having sum (b.due_premium) = 0;

  begin
    --删除批单对应的保单的结算日期在99年1月1日（含）以后
    out_flag := 0 ; --成功
    v_count := 0;
    i_count := 0;
    i_count1:= 0;
    --判断是否是初始化1－代表初始化，2－代表非初始化
     if n_flag = 2 then
        for tmp_rec1 in c_loging1 loop
           begin
             select count(*) into i_count from premium_info t
              where t.policy_no = tmp_rec1.policy_no
                and t.endorse_no is null
                and t.settle_date >= to_date('1999-01-01','yyyy-mm-dd')
                and rownum =1;
              if i_count = 0 or i_count is null then
                 select count(*) into i_count from kcoldue tr
                  where tr.cplyno = tmp_rec1.policy_no
                    and tr.cedrno is null
                    and dcaldte >= to_date('1999-01-01','yyyy-mm-dd')
                    and rownum =1;
              end if;
             exception
                  when no_data_found then
                        i_count := 0;
                  when others then
                        i_count := 0;
             end;
               if i_count = 0 then
                   --删除批单对应的原始保单不符合应收条件数据，删除整个批单对应的保单。
                   --modified by liuyifu 20090310修改保单有批单的情况，如果批单对应的保单结算日期在1999年1月1日之前的，这样的数据是整个保单（原始保单和所有批单）都不显示
                   delete /*+ direct */ from tmp_finance_duepremium_long_o a
                  where a.policy_no = tmp_rec1.policy_no;

                  --同步删除tmp_finance_duepremium_long_n保证两边数据一致
                  delete tmp_finance_duepremium_long_n a
                 where a.policy_no = tmp_rec1.policy_no;

                  v_count := v_count + 1;
                  if v_count >= 5000 then
                      commit;
                      v_count := 0;
                  end if;
               end if;
        end loop;
     else
        open c_loging;
        loop
        fetch c_loging bulk collect into v_policy_no_n1
        limit 30000;
            exit when not v_policy_no_n1.exists(1);

            <<tmp_rec_loop>>
            for tmp_rec in v_policy_no_n1.first  ..  v_policy_no_n1.last loop
              begin
               select count(*) into i_count from premium_info t
                where t.policy_no = v_policy_no_n1(tmp_rec)
                  and t.endorse_no is null
                  and t.settle_date >= to_date('1999-01-01','yyyy-mm-dd')
                  and rownum =1;

               if i_count = 0 or i_count is null then
                  select count(*) into i_count from kcoldue tr
                   where tr.cplyno = v_policy_no_n1(tmp_rec)
                     and tr.cedrno is null
                     and dcaldte >= to_date('1999-01-01','yyyy-mm-dd')
                     and rownum =1;
               end if;
             exception
                  when no_data_found then
                        i_count := 0;
                  when others then
                        i_count := 0;
             end;

            i_count1 := i_count1 + 1;
            if i_count1 = 1 then
            insert into  epcis_job_log values (sysdate, 2, 11, 0, '速度监视','第一条'||to_char(sysdate,'yyyy-mm-dd hh24:mi:ss'),sys_guid());
            commit;
            end if;

            if i_count1 >= 10000 then
            insert into  epcis_job_log values (sysdate, 2, 11, 0, '速度监视',to_char(i_count1)||'---'||to_char(sysdate,'yyyy-mm-dd hh24:mi:ss'),sys_guid());
            commit;
            i_count1 := 0;
            end if;

            if i_count = 0 then
            --删除批单对应的原始保单不符合应收条件数据，删除整个批单对应的保单。
            --modified by liuyifu 20090310修改保单有批单的情况，如果批单对应的保单结算日期在1999年1月1日之前的，这样的数据是整个保单（原始保单和所有批单）都不显示
                 delete /*+ direct */ from tmp_finance_duepremium_long_o a
                 where a.policy_no = v_policy_no_n1(tmp_rec);

               --同步删除tmp_finance_duepremium_long_n保证两边数据一致
                  delete /*+ direct */ from tmp_finance_duepremium_long_n a
                 where a.policy_no = v_policy_no_n1(tmp_rec);

               v_count := v_count + 1;
              if v_count >= 5000 then
                  insert into  epcis_job_log values (sysdate, 2, 12, 0, 'delete 速度监视',to_char(v_count)||'---'||to_char(sysdate,'yyyy-mm-dd hh24:mi:ss'),sys_guid());
                  commit;
                  v_count := 0;
              end if;
            end if;
            end loop tmp_rec_loop;

            commit;
        end loop;
     end if;
  commit;
     insert into  epcis_job_log values (sysdate, 5, 21, 0, '暂无','pro_finance_duepremium_long2 删除批单对应的原始保单不符合应收条件数据完成',sys_guid());
      commit;
  --删除累积应收为0 的记录
/*  delete \*+ direct *\ from tmp_finance_duepremium_long_n a
   where policy_no in (select policy_no from tmp_finance_duepremium_long_n b
                                      group by b.policy_no having sum (b.due_premium) = 0);
  commit;
*/
  --删除累积应收为0 的记录,替换上面注释语句
  open c_n;
  loop
  fetch c_n bulk collect into v_policy_no_n limit 30000;

     exit when not v_policy_no_n.exists(1);

     forall i in v_policy_no_n.first .. v_policy_no_n.last
        delete /*+ direct */ from tmp_finance_duepremium_long_n a
         where policy_no = v_policy_no_n(i);
     commit;
  end loop;

  -- 删除注销累积应收为 0 的纪录
--  delete /*+ direct */  from tmp_finance_duepremium_long_n a
--        where policy_no in (select policy_no from tmp_finance_duepremium_long_n b
--                                          where  b.cancel_flag = 'Y' group by b.policy_no  having sum(b.due_premium) = 0
--                                         )
--            and cancel_flag = 'Y';
--  commit;

     insert into  epcis_job_log values (sysdate, 6, 22, 0, '暂无','pro_finance_duepremium_long2 删除tmp_finance_duepremium_long_n表累积应收为0 的记录完成',sys_guid());
      commit;
--至此数据更新完毕，可以提供给用户使用
   exception
     when others then
          v_ErrCodeVal := sqlcode;
          v_ErrMsg := substr('获取pro_finance_duepremium_long2过程出错'|| sqlerrm, 1, 500);
          insert into  epcis_job_log values (sysdate, 22,2,v_ErrCodeVal, v_ErrMsg,'pro_finance_duepremium_long2 执行失败!',sys_guid());
          commit;
  end pro_finance_duepremium_long2;


---------------------------------------------------------------------------
--created by liuyifu 2008-10-10
--pro_finance_duepremium_long5处理内容如下：
  --1、处理每天财务更新的k表和p表数据，增量更新到 tmp_finance_duepremium_long_n 和tmp_finance_duepremium_long_o表
  --2、对两表作增加、修改、删除以满足每天变化，还需要保证tmp_finance_duepremium_long_n表的数据完整（对tmp_finance_duepremium_long_o
  -- 3、表的更新数据全量都必须更新到tmp_finance_duepremium_long_n里）
  --4、将每天更新的且符合条件的插入tmp_finance_duepremium_long_n表数据到tmp_finance_duepremium_long_o找保单匹配的完整数据在插入tmp_finance_duepremium_long_n表
  --5、每天运行
  --注：tmp_k_p_update_data_everyday包括了每天k、p表的更新数据，
  --         由ETL完成,ETL实现job是:PDBC2_BASE/Jobs/PDBC/CTL/SEQ/SeqKP.dsx ，调用job是：PDBC2_BASE/Jobs/PDBC/CTL/SEQ/SeqK.dsx
  --         每天调度时间预计设置在 6点，但是必须在财务“制证”完成后才可执行。
  --2009-01-16 liuyifu，增加缴费止期字段和月帐龄字段
---------------------------------------------------------------------------
  procedure pro_finance_duepremium_long5
  is
    out_flag number;
    v_ErrMsg     varchar2(500);
    v_ErrCodeVal number := 0;
    i_count      number := 0;
    i_count1     number := 0;

    --不符合要求数据
    cursor c_tfd_long_o is
    select t.receipt_no, t.plan_code from tmp_k_p_update_data_everyday t where t.isdata = 2;

    type v_receipt_no is table of tmp_k_p_update_data_everyday.receipt_no%type index by binary_integer;
    type v_plan_code is table of tmp_k_p_update_data_everyday.plan_code%type index by binary_integer;
    v_receipt_no_n v_receipt_no;
    v_plan_code_n  v_plan_code;

  begin
  --在tmp_k_p_update_data_everyday表数据输入完成后调用此过程
  --更新k、p表数据到参照表tmp_finance_duepremium_long_n，tmp_finance_duepremium_long_o，
  --tmp_k_p_update_data_everyday表记录了k、p表当天更新的数据
  --更新tmp_finance_duepremium_long_n表
    for i in (select a.flex_value as department_code
                 from institutions a,
                      local_info b
                where a.enable_flag = 'Y'
                  and a.branch_control = b.branch_control) loop

       merge into tmp_finance_duepremium_long_n t1
        using (select /*+ index(a ix_tmpkp_inup_isdata_depcode)*/ * from tmp_k_p_update_data_everyday a where a.isdata =1 and a.department_code = i.department_code) t2  --是否符合应收条件，符合应收条件记录 1，不符合应收条件记录 2
        on (t1.receipt_no  = t2.receipt_no and t1.plan_code = t2.plan_code)
        when matched then
          update  set t1.department_code=t2.department_code,t1.department_name=t2.department_name,t1.policy_no=t2.policy_no,
                              t1.endorse_no=t2.endorse_no,t1.notice_no=t2.notice_no,t1.due_premium=t2.due_premium,
                              t1.insurance_begin_date=t2.insurance_begin_date,t1.insurance_end_date=t2.insurance_end_date,t1.insured_person=t2.insured_person,
                              t1.due_voucher_no=t2.due_voucher_no,t1.due_voucher_date=t2.due_voucher_date,t1.account_days=t2.account_days,
                              t1.plan_name=t2.plan_name,t1.currency_code=t2.currency_code,
                              t1.currency_name=t2.currency_name,t1.client_code=t2.client_code,t1.client_name=t2.client_name,
                              t1.sale_agent_code=t2.sale_agent_code,t1.sale_agent_name=t2.sale_agent_name,t1.sale_channel_code=t2.sale_channel_code,
                              t1.sale_channel_name=t2.sale_channel_name,t1.group_code=t2.group_code,t1.group_name=t2.group_name,
                              t1.due_premium_sum=t2.due_premium_sum,t1.cancel_flag=t2.cancel_flag,t1.underwrite_time=t2.underwrite_time,
                              t1.settle_date=t2.settle_date,t1.agent_code=t2.agent_code,t1.agent_chinese_name=t2.agent_chinese_name,
                              t1.payment_end_date=t2.payment_end_date, t1.account_month=t2.account_month, t1.due_tax=t2.due_tax, t1.total_amount=t2.total_amount
        when not matched then
          insert(department_code,  department_name,  policy_no,  endorse_no,  notice_no,
                      due_premium, insurance_begin_date, insurance_end_date,  insured_person, due_voucher_no,
                      due_voucher_date,  account_days, plan_code,  plan_name, currency_code,
                      currency_name,  client_code,  client_name,  sale_agent_code,  sale_agent_name,
                      sale_channel_code,  sale_channel_name,  group_code, group_name, due_premium_sum,
                      cancel_flag,  underwrite_time,  settle_date,  receipt_no, agent_code, agent_chinese_name,
                      payment_end_date, account_month, due_tax, total_amount
                   )
           values(t2.department_code,  t2.department_name,  t2.policy_no,  t2.endorse_no,  t2.notice_no,
                       t2.due_premium, t2.insurance_begin_date, t2.insurance_end_date,  t2.insured_person, t2.due_voucher_no,
                       t2.due_voucher_date,  t2.account_days, t2.plan_code,  t2.plan_name, t2.currency_code,
                       t2.currency_name,  t2.client_code,  t2.client_name,  t2.sale_agent_code,  t2.sale_agent_name,
                       t2.sale_channel_code,  t2.sale_channel_name,  t2.group_code, t2.group_name, t2.due_premium_sum,
                       t2.cancel_flag,  t2.underwrite_time,  t2.settle_date,  t2.receipt_no, t2.agent_code, t2.agent_chinese_name,
                       t2.payment_end_date, t2.account_month, t2.due_tax, t2.total_amount
                     ) ;
      commit;
      --更新tmp_finance_duepremium_long_o表
      merge into tmp_finance_duepremium_long_o t1
        using (select  /*+ index(a ix_tmpkp_inup_isdata_depcode)*/ * from tmp_k_p_update_data_everyday a where a.isdata =1 and a.department_code = i.department_code) t2  --是否符合应收条件，符合应收条件记录 1，不符合应收条件记录 2
        on (t1.receipt_no  = t2.receipt_no and t1.plan_code = t2.plan_code)
        when matched then
            update  set t1.department_code=t2.department_code,t1.department_name=t2.department_name,t1.policy_no=t2.policy_no,
                                t1.endorse_no=t2.endorse_no,t1.notice_no=t2.notice_no,t1.due_premium=t2.due_premium,
                                t1.insurance_begin_date=t2.insurance_begin_date,t1.insurance_end_date=t2.insurance_end_date,t1.insured_person=t2.insured_person,
                                t1.due_voucher_no=t2.due_voucher_no,t1.due_voucher_date=t2.due_voucher_date,t1.account_days=t2.account_days,
                                t1.plan_name=t2.plan_name,t1.currency_code=t2.currency_code,
                                t1.currency_name=t2.currency_name,t1.client_code=t2.client_code,t1.client_name=t2.client_name,
                                t1.sale_agent_code=t2.sale_agent_code,t1.sale_agent_name=t2.sale_agent_name,t1.sale_channel_code=t2.sale_channel_code,
                                t1.sale_channel_name=t2.sale_channel_name,t1.group_code=t2.group_code,t1.group_name=t2.group_name,
                                t1.due_premium_sum=t2.due_premium_sum,t1.cancel_flag=t2.cancel_flag,t1.underwrite_time=t2.underwrite_time,
                                t1.settle_date=t2.settle_date,t1.agent_code=t2.agent_code,t1.agent_chinese_name=t2.agent_chinese_name,
                                t1.payment_end_date=t2.payment_end_date, t1.account_month=t2.account_month, t1.due_tax=t2.due_tax, t1.total_amount=t2.total_amount
          when not matched then
            insert(department_code,  department_name,  policy_no,  endorse_no,  notice_no,
                        due_premium, insurance_begin_date, insurance_end_date,  insured_person, due_voucher_no,
                        due_voucher_date,  account_days, plan_code,  plan_name, currency_code,
                        currency_name,  client_code,  client_name,  sale_agent_code,  sale_agent_name,
                        sale_channel_code,  sale_channel_name,  group_code, group_name, due_premium_sum,
                        cancel_flag,  underwrite_time,  settle_date,  receipt_no, agent_code, agent_chinese_name,
                        payment_end_date, account_month, due_tax, total_amount
                     )
             values(t2.department_code,  t2.department_name,  t2.policy_no,  t2.endorse_no,  t2.notice_no,
                         t2.due_premium, t2.insurance_begin_date, t2.insurance_end_date,  t2.insured_person, t2.due_voucher_no,
                         t2.due_voucher_date,  t2.account_days, t2.plan_code,  t2.plan_name, t2.currency_code,
                         t2.currency_name,  t2.client_code,  t2.client_name,  t2.sale_agent_code,  t2.sale_agent_name,
                         t2.sale_channel_code,  t2.sale_channel_name,  t2.group_code, t2.group_name, t2.due_premium_sum,
                         t2.cancel_flag,  t2.underwrite_time,  t2.settle_date,  t2.receipt_no, t2.agent_code, t2.agent_chinese_name,
                         t2.payment_end_date, t2.account_month, t2.due_tax, t2.total_amount
                       ) ;
      commit;
    end loop;
     insert into  epcis_job_log values (sysdate, 1, 5, 0, '暂无','pro_finance_duepremium_long5更新tmp_finance_duepremium_long_n表完成',sys_guid());
     insert into  epcis_job_log values (sysdate, 2, 5, 0, '暂无','pro_finance_duepremium_long5更新tmp_finance_duepremium_long_o表完成',sys_guid());
    commit;
--删除不符合要求数据
--(此处删除k、p表需要同步，不能分开执行不然会有误删问题)

    open c_tfd_long_o;
    loop
      fetch c_tfd_long_o bulk collect
        into v_receipt_no_n, v_plan_code_n limit 5000;
      exit when not v_receipt_no_n.exists(1);
      forall i in v_receipt_no_n.first .. v_receipt_no_n.last
        delete /*+ direct */
        from tmp_finance_duepremium_long_o w
         where w.receipt_no = v_receipt_no_n(i)
           and w.plan_code = v_plan_code_n(i);
      commit;
      forall i in v_receipt_no_n.first .. v_receipt_no_n.last
        delete /*+ direct */
        from tmp_finance_duepremium_long_n w
         where w.receipt_no = v_receipt_no_n(i)
           and w.plan_code = v_plan_code_n(i);
      commit;
    end loop;
    close c_tfd_long_o;

     insert into  epcis_job_log values (sysdate, 3, 5, 0, '暂无','pro_finance_duepremium_long5删除不符合要求数据完成',sys_guid());
      commit;
--保证mp_finance_duepremium_long_n 表是 mp_finance_duepremium_long表的真子集(会很影响性能,可以不用验证)
/*     for i in (select a.receipt_no, a.plan_code from tmp_finance_duepremium_long_n a) loop
          select count(*) into i_count from tmp_finance_duepremium_long_o b
          where b.receipt_no = i.receipt_no and b.plan_code = i.plan_code;
          if i_count = 0 then
              update tmp_finance_duepremium_long_n b set b.client_code = '9'
              where b.receipt_no = i.receipt_no and b.plan_code = i.plan_code;
              commit;
           end if;
     end loop;
         insert into  epcis_job_log values (sysdate, 3, 51, 0, '暂无','pro_finance_duepremium_long5保证long_n 表是long表的真子集完成',sys_guid());
      commit;*/
--将每天更新的且符合条件的插入tmp_finance_duepremium_long_n表数据到tmp_finance_duepremium_long_o找保单匹配的完整数据在插入tmp_finance_duepremium_long_n表
--当天k,p表更新的数据,且符合应收条件的数据,必须在将每天更新（增、删、改）的数据完全放入tmp_finance_duepremium_long_n表和tmp_finance_duepremium_long_o表
    --for c_t1 in (select distinct tt.policy_no from tmp_k_p_update_data_everyday  tt where tt.isdata = 1) loop --1/2的情况都需要处理 ex-liukailin001 modify2010-12-2
    for c_t1 in (select distinct tt.policy_no from tmp_k_p_update_data_everyday  tt ) loop
      <<c_t2_loop>>
      for c_t2 in (select t.* from tmp_finance_duepremium_long_o t where t.policy_no = c_t1.policy_no) loop
         begin
           select count(*) into i_count1 from tmp_finance_duepremium_long_n t1
            where t1.policy_no = c_t2.policy_no and t1.receipt_no = c_t2.receipt_no and t1.plan_code=c_t2.plan_code;
         exception
            when no_data_found then
               i_count1 := 0;
            when others then
               i_count1 := 0;
         end;
         if i_count1 > 0 then
           update tmp_finance_duepremium_long_n t1
              set t1.department_code=c_t2.department_code,t1.department_name=c_t2.department_name,
                  t1.endorse_no=c_t2.endorse_no,t1.notice_no=c_t2.notice_no,t1.due_premium=c_t2.due_premium,
                  t1.insurance_begin_date=c_t2.insurance_begin_date,t1.insurance_end_date=c_t2.insurance_end_date,
                  t1.insured_person=c_t2.insured_person, t1.due_voucher_no=c_t2.due_voucher_no,t1.due_voucher_date=c_t2.due_voucher_date,
                  t1.account_days=c_t2.account_days, t1.plan_name=c_t2.plan_name,t1.currency_code=c_t2.currency_code,
                  t1.currency_name=c_t2.currency_name,t1.client_code=c_t2.client_code,t1.client_name=c_t2.client_name,
                  t1.sale_agent_code=c_t2.sale_agent_code,t1.sale_agent_name=c_t2.sale_agent_name,t1.sale_channel_code=c_t2.sale_channel_code,
                  t1.sale_channel_name=c_t2.sale_channel_name,t1.group_code=c_t2.group_code,t1.group_name=c_t2.group_name,
                  t1.due_premium_sum=c_t2.due_premium_sum,t1.cancel_flag=c_t2.cancel_flag,t1.underwrite_time=c_t2.underwrite_time,
                  t1.settle_date=c_t2.settle_date,t1.agent_code=c_t2.agent_code,t1.agent_chinese_name=c_t2.agent_chinese_name,
                  t1.payment_end_date=c_t2.payment_end_date, t1.account_month=c_t2.account_month, t1.due_tax=c_t2.due_tax, t1.total_amount=c_t2.total_amount
            where t1.policy_no = c_t2.policy_no and t1.receipt_no = c_t2.receipt_no and t1.plan_code=c_t2.plan_code;
         elsif i_count1 = 0 then
            insert into tmp_finance_duepremium_long_n
                  (department_code,  department_name,  policy_no,  endorse_no,  notice_no,
                   due_premium, insurance_begin_date, insurance_end_date,  insured_person, due_voucher_no,
                   due_voucher_date,  account_days, plan_code,  plan_name, currency_code,
                   currency_name,  client_code,  client_name,  sale_agent_code,  sale_agent_name,
                   sale_channel_code,  sale_channel_name,  group_code, group_name, due_premium_sum,
                   cancel_flag,  underwrite_time,  settle_date,  receipt_no, agent_code, agent_chinese_name,
                   payment_end_date, account_month, due_tax, total_amount)
            values(c_t2.department_code,  c_t2.department_name,  c_t2.policy_no,  c_t2.endorse_no,  c_t2.notice_no,
                   c_t2.due_premium, c_t2.insurance_begin_date, c_t2.insurance_end_date,  c_t2.insured_person, c_t2.due_voucher_no,
                   c_t2.due_voucher_date,  c_t2.account_days, c_t2.plan_code,  c_t2.plan_name, c_t2.currency_code,
                   c_t2.currency_name,  c_t2.client_code,  c_t2.client_name,  c_t2.sale_agent_code,  c_t2.sale_agent_name,
                   c_t2.sale_channel_code,  c_t2.sale_channel_name,  c_t2.group_code, c_t2.group_name, c_t2.due_premium_sum,
                   c_t2.cancel_flag,  c_t2.underwrite_time,  c_t2.settle_date,  c_t2.receipt_no, c_t2.agent_code, c_t2.agent_chinese_name,
                   c_t2.payment_end_date, c_t2.account_month, c_t2.due_tax, c_t2.total_amount);
         end if;
      end loop c_t2_loop;
      i_count := i_count + 1;
      if i_count >= 5000 then
        commit;
        i_count := 0;
      end if;
    end loop;
  commit;

     insert into  epcis_job_log values (sysdate, 4, 5, 0, '暂无','pro_finance_duepremium_long5回查匹配的完整数据在插入tmp_finance_duepremium_long_n表完成',sys_guid());
      commit;
--回查tmp_finance_duepremium_long_n表将批批单对应的原始保单的结算日期不在99年1月1日（含）以后，删除保单累加为0的数据
    pro_finance_duepremium_long2(2,out_flag);
    --0成功，1失败
    if out_flag = 0 then
        insert into  epcis_job_log values (sysdate, 7, 5, 0, '暂无','pro_finance_duepremium_long5调用pro_finance_duepremium_long2 完成',sys_guid());
        commit;
    else
        insert into  epcis_job_log values (sysdate, 7, 51, 0, '失败','pro_finance_duepremium_long5调用pro_finance_duepremium_long2 失败',sys_guid());
        commit;
    end if;
    insert into  epcis_job_log values (sysdate, 8, 5, 0, '暂无','pro_finance_duepremium_long5调用完成',sys_guid());
    commit;

    --删除一正一负金额相等的两条数据 ex-zhaominzhi001
    i_count := 0;
    i_count1 := 0;
    for c_del in (select t.policy_no,
                         t.insurance_begin_date,
                         t.insurance_end_date,
                         t.payment_end_date,
                         t.total_amount
                    from tmp_k_p_update_data_everyday t
                   where t.due_premium < 0) loop
      --删除正数
      select count(1)
        into i_count1
        from tmp_finance_duepremium_long_n n
       where n.policy_no = c_del.policy_no
         and n.insurance_begin_date = c_del.insurance_begin_date
         and n.insurance_end_date = c_del.insurance_end_date
         and n.payment_end_date = c_del.payment_end_date
         and n.total_amount = -c_del.total_amount
         and exists (select 1
                from tmp_finance_duepremium_long_n n
               where n.policy_no = c_del.policy_no
                 and n.insurance_begin_date = c_del.insurance_begin_date
                 and n.insurance_end_date = c_del.insurance_end_date
                 and n.payment_end_date = c_del.payment_end_date
                 and n.total_amount = c_del.total_amount
                 and rownum = 1)
         and rownum = 1;
      if i_count1 = 1 then
        --删除正数
        delete tmp_finance_duepremium_long_n n
         where n.policy_no = c_del.policy_no
           and n.insurance_begin_date = c_del.insurance_begin_date
           and n.insurance_end_date = c_del.insurance_end_date
           and n.payment_end_date = c_del.payment_end_date
           and n.total_amount = -c_del.total_amount
           and rownum = 1;
        --删除负数
        delete tmp_finance_duepremium_long_n n
         where n.policy_no = c_del.policy_no
           and n.insurance_begin_date = c_del.insurance_begin_date
           and n.insurance_end_date = c_del.insurance_end_date
           and n.payment_end_date = c_del.payment_end_date
           and n.total_amount = c_del.total_amount
           and rownum = 1;

        --5000 commit
        i_count := i_count + 1;
        if i_count >= 5000 then
          i_count := 0;
          commit;
        end if;

      end if;
    end loop;
    commit;

   exception
       when others then
          v_ErrCodeVal := sqlcode;
          v_ErrMsg := substr('获取pro_finance_duepremium_long5过程出错'|| sqlerrm, 1, 500);
          insert into  epcis_job_log values (sysdate, 55, 5, v_ErrCodeVal, v_ErrMsg,'pro_finance_duepremium_long5执行失败！',sys_guid());
           commit;
    end pro_finance_duepremium_long5;
  --2010-12-2 ex-liukailin001 增加一个procedure 用来补充计算，增加时间范围。
  procedure pro_finance_duepremium_long6(p_begin_date  in   varchar2, --开始补运行时间
                                         p_end_date    in   varchar2, --结束补运行时间
                                         p_result_code out  varchar2, --返回结果N失败Y成功
                                         p_result_msg  out  varchar2 --返回错误信息
                                         ) is
      v_flag  varchar2(20);
  begin
      p_result_code :='Y';
      insert into  epcis_job_log values (sysdate, 0, 5, 0, '应收补运行','开始时间:'||p_begin_date||';结束时间:'||p_end_date,sys_guid());
      v_flag :='3';
      for i in 1 .. 2000 loop
          delete from tmp_k_p_update_data_everyday
           where rownum <= 20000;
          --if sql%rowcount = 0 then modiby by ex-zengjiu001 2011-7-19 PKG RULE 4801
              --exit;
          --end if;
          exit when sql%rowcount = 0;
          commit;
      end loop;
      v_flag :='4';
      insert into tmp_k_p_update_data_everyday
          (department_code,
           department_name,
           policy_no,
           endorse_no,
           notice_no,
           due_premium,
           insurance_begin_date,
           insurance_end_date,
           insured_person,
           due_voucher_no,
           due_voucher_date,
           account_days,
           plan_code,
           plan_name,
           currency_code,
           currency_name,
           client_code,
           client_name,
           sale_agent_code,
           sale_agent_name,
           sale_channel_code,
           sale_channel_name,
           group_code,
           group_name,
           due_premium_sum,
           cancel_flag,
           underwrite_time,
           settle_date,
           receipt_no,
           iskorptab,
           isdata,
           ods_updated_date,
           business_source,
           agent_code,
           agent_chinese_name,
           payment_end_date,
           account_month,
           due_tax,
           total_amount)
         select /*+ index(b PK_PREMIUM_PLAN)*/
           finance_department_code,
           (select description
              from institutions
             where flex_value = a.finance_department_code) v_department_name,
           policy_no,
           endorse_no,
           term_no, --期别
           premium_amount,
           insurance_begin_time,
           insurance_end_time,
           insured_name,
           due_voucher_no,
           due_voucher_date,
           '0' as kaccountdays, --帐龄
           plan_code,
           (select plan_chinese_name
              from plan_define
             where plan_code = b.plan_code) v_plan_name,
           currency_code,
           (select currency_chinese_name
              from currency_define
             where currency_code = a.currency_code) v_currency_name,
           client_attribute,
           case
               when a.client_attribute = '1' then
                '个体'
               else
                '团体'
           end v_client_name,
           sale_agent_code,
           (select employee_name
              from epcisbase.sas_employee
             where employee_code = sale_agent_code
               and rownum = 1) sale_agent_name,
           channel_source_code,
           (select bnocnm
              from business_source
             where bno = a.channel_source_code
               and rownum = 1) v_salechnl_name,
           group_code,
           (select group_name
              from sas_group t_salegrp
             where group_code = a.group_code
               and rownum = 1) group_name,
           case a.currency_code
               when '02' then
                (select exchange_rate
                   from exchange_rate
                  where effective_date <= sysdate
                    and (invalidate_date is null or
                        sysdate <= invalidate_date)
                    and currency1_code = '02'
                    and currency2_code = '01') * b.premium_amount
               when '03' then
                (select exchange_rate
                   from exchange_rate
                  where effective_date <= sysdate
                    and (invalidate_date is null or
                        sysdate <= invalidate_date)
                    and currency1_code = '03'
                    and currency2_code = '01') * b.premium_amount
               else
                b.premium_amount
           end due_premium_sum,
           disable_flag,
           underwrite_time,
           settle_date,
           a.receipt_no, --收据号
           'p' as iskorptab, --数据来源说明p->premium_info,k->kcoldue
           case
               when (actual_voucher_no is null and
                    due_voucher_no is not null and
                    greatest(insurance_begin_time, underwrite_time) between
                    to_date('19990101', 'yyyy-mm-dd') and
                    last_day(trunc(sysdate, 'dd')) + 1 and
                    a.receipt_no = b.receipt_no and b.plan_code <> 'A24' and
                    a.due_voucher_date >=
                    to_date('19990101', 'yyyy-mm-dd') and
                    nvl(cancel_after_verification, 'N') = 'N') or
                    (actual_voucher_date > trunc(sysdate) + 1 and
                    due_voucher_no is not null and
                    greatest(insurance_begin_time, underwrite_time) between
                    to_date('19990101', 'yyyy-mm-dd') and
                    last_day(trunc(sysdate, 'dd')) + 1 and
                    a.receipt_no = b.receipt_no and b.plan_code <> 'A24' and
                    a.due_voucher_date >=
                    to_date('19990101', 'yyyy-mm-dd') and
                    nvl(cancel_after_verification, 'N') = 'N') then
                1 --符合应收条件记录 1
               else
                2 --不符合应收条件记录 2
           end isdata, --是否符合应收条件
           sysdate ods_updated_date,
           a.business_source,
           a.agent_code,
           (select agent_chinese_name
              from agent_define
             where agent_code = a.agent_code) as agent_chinese_name,
           payment_end_date,
           '0' account_month,
               (select t.amount
          from epcisacct.premium_tax_plan t
         where t.receipt_no = b.receipt_no
           and t.plan_code = b.plan_code) due_tax,
       (b.premium_amount +nvl((select t.amount
          from epcisacct.premium_tax_plan t
         where t.receipt_no = b.receipt_no
           and t.plan_code = b.plan_code),0)) total_amount
            from premium_info a, premium_plan b
           where a.updated_date >= to_date(p_begin_date, 'yyyy-mm-dd')
             and a.updated_date <= to_date(p_end_date, 'yyyy-mm-dd') + 1 - 1/24/3600
             and a.receipt_no = b.receipt_no
             and b.plan_code <> 'A24';
          commit;
          v_flag :='5';
          insert into tmp_k_p_update_data_everyday
              (department_code,
               department_name,
               policy_no,
               endorse_no,
               notice_no,
               due_premium,
               insurance_begin_date,
               insurance_end_date,
               insured_person,
               due_voucher_no,
               due_voucher_date,
               account_days,
               plan_code,
               plan_name,
               currency_code,
               currency_name,
               client_code,
               client_name,
               sale_agent_code,
               sale_agent_name,
               sale_channel_code,
               sale_channel_name,
               group_code,
               group_name,
               due_premium_sum,
               cancel_flag,
               underwrite_time,
               settle_date,
               receipt_no,
               iskorptab,
               isdata,
               ods_updated_date,
               business_source,
               agent_code,
               agent_chinese_name,
               payment_end_date,
               account_month,
               due_tax,
               total_amount)
              select cdno department_code,
                     (select description
                        from institutions
                       where flex_value = t.cdno) department_name,
                     cplyno policy_no,
                     cedrno endorse_no,
                     ntermno notice_no,
                     nprmdue due_premium,
                     dplystr insurance_begin_date,
                     dplyend insurance_end_date,
                     cpaynme insured_person,
                     caccno due_voucher_no,
                     daccdte due_voucher_date,
                     '0' as kaccountdays, --日帐龄，在返回数据时计算
                     cinscde plan_code,
                     (select plan_chinese_name
                        from plan_define
                       where plan_code = t.cinscde) plan_name,
                     ccurno currency_code,
                     (select currency_chinese_name
                        from currency_define
                       where currency_code = t.ccurno) currency_name,
                     substr(c_magic_set, 2, 1) client_code,
                     case
                         when substr(t.c_magic_set, 2, 1) = '1' then
                          '个体'
                         else
                          '团体'
                     end client_name,
                     cempcde sale_agent_code,
                     (select employee_name
                        from epcisbase.sas_employee
                       where employee_code = t.cempcde
                         and rownum = 1) sale_agent_name,
                     substr(c_magic_set, 1, 1) sale_channel_code,
                     (select bnocnm
                        from business_source
                       where bno = substr(t.c_magic_set, 1, 1)
                         and rownum = 1) sale_channel_name,
                     cparno group_code,
                     (select group_name
                        from sas_group t_salegrp
                       where group_code = t.cparno
                         and rownum = 1) group_name,
                     case t.ccurno
                         when '02' then
                          (select exchange_rate
                             from exchange_rate
                            where effective_date <= sysdate
                              and (invalidate_date is null or
                                  sysdate <= invalidate_date)
                              and currency1_code = '02'
                              and currency2_code = '01') * t.nprmdue
                         when '03' then
                          (select exchange_rate
                             from exchange_rate
                            where effective_date <= sysdate
                              and (invalidate_date is null or
                                  sysdate <= invalidate_date)
                              and currency1_code = '03'
                              and currency2_code = '01') * t.nprmdue
                         else
                          t.nprmdue
                     end due_premium_sum,
                     cancel cancel_flag,
                     dfcd underwrite_time,
                     dcaldte settle_date,
                     crctno receipt_no,
                     'k' as iskorptab,
                     case
                         when (dcoldte is null and caccno is not null and
                              greatest(dplystr, dfcd) between
                              to_date('19990101', 'yyyymmdd') and
                              last_day(trunc(sysdate, 'dd')) + 1 --交费起期/创建日期
                              and cinscde <> 'A24' and
                              nvl(hide_flag, 'N') <> 'Y' and
                              daccdte >= to_date('19990101', 'yyyymmdd') and
                              nvl(cancel_after_verification, 'N') = 'N') or
                              (dcoldte > trunc(sysdate) + 1 and
                              caccno is not null and
                              greatest(dplystr, dfcd) between
                              to_date('19990101', 'yyyymmdd') and
                              last_day(trunc(sysdate, 'dd')) + 1 and
                              cinscde <> 'A24' and nvl(hide_flag, 'N') <> 'Y' and
                              daccdte >= to_date('19990101', 'yyyymmdd') and
                              nvl(cancel_after_verification, 'N') = 'N') then
                          1
                         else
                          2
                     end isdata,
                     sysdate ods_updated_date,
                     csource business_source,
                     t.cagtcde as agent_code,
                     (select agent_chinese_name
                        from agent_define
                       where agent_code = t.cagtcde) as agent_chinese_name,
                     dpayend,
                     '0' account_month, --月帐龄，在返回数据时计算
                     0,
                     nprmdue due_premium
                from kcoldue t
               where t.dlcd >= to_date(p_begin_date, 'yyyy-mm-dd')
                 and t.dlcd <= to_date(p_end_date, 'yyyy-mm-dd') + 1 - 1 / 24 / 3600
                 and t.cinscde <> 'A24';
              commit;
              pro_finance_duepremium_long5;

      exception
          when others then
              p_result_code :='N';
              if v_flag ='3' then
                  p_result_msg:='删除EVERY_DAY表错误';
              elsif v_flag ='4' then
                  p_result_msg :='抽取p表数据错误';
              elsif v_flag = '5' then
                  p_result_msg := '抽取k表错误';
              end if;
              p_result_msg :=substr('发生错误:'||p_result_msg||sqlerrm,1,200);
              insert into  epcis_job_log values (sysdate, 0, 5, 0, '应收补运行',p_result_msg,sys_guid());
              commit;
  end pro_finance_duepremium_long6;
  ------------------------------------------------------------------------------------------------------
   --清空每天更新的临时表数据,在每天插入数据前情况，由 ETL 调用执行
   -----------------------------------------------------------------------------------------------------
  procedure pro_truncate_table
  is
  v_count number;
  --v_sql Varchar2(2000);
  begin
    --清空每天更新的临时表数据,此清空数据的操作必须在早上8:30前执行，否则会报错
    --如果上班时间执行不了使用下面的这个报错方式使用DELETE的方式删除。
    begin
        --epcisrpt_ddl_prc.truncate_table('TMP_K_P_UPDATE_DATA_EVERYDAY');
        appmgr.pkg_truncate.truncate_table('EPCISACCT','TMP_K_P_UPDATE_DATA_EVERYDAY', null);
        --v_sql := 'begin appmgr.pkg_truncate.truncate_table(''EPCISACCT'',''TMP_K_P_UPDATE_DATA_EVERYDAY'', null); end;';
        --Execute Immediate v_sql;
    exception
        when others then
            for i in 1 .. 1000 loop
                delete from tmp_k_p_update_data_everyday a
                 where rownum <20001;
                v_count := sql%rowcount;
                commit;
                exit when v_count=0;
            end loop;
    end;
    insert into  epcis_job_log values (sysdate, 0, 0, 0, '暂无','pro_truncate_table执行完成',sys_guid());
    commit;
  end pro_truncate_table;


  ------------------------------------------------------------------------------------------------------
   --报表页面调用pkg，直接返回数据给用户
   --2009-01-16 liuyifu，增加缴费止期字段和月账龄字段，修改稿账龄分月账龄和日账龄
   -----------------------------------------------------------------------------------------------------
  procedure pro_finance_duepremium_long_re(parameters in varchar2,
                                           re_cursor  out t_cursor) is
    v_cursor      t_cursor;
    filter        varchar2(1800); --过滤
    sel           varchar2(3600); --选择项
    sql_statement varchar2(5400); --Select语句
    --过滤参数
    p_user_code       varchar2(20);
    p_department_code varchar2(10);
    p_plan_code       varchar2(20);
    p_plan_class_code varchar2(20);
    p_emp_code        varchar2(30);
    p_sale_group      varchar2(20);
    p_client_type     varchar2(2);
    p_sale_channel    varchar2(10);
    p_currency_code   varchar2(2);
    p_rateh           varchar2(10);
    p_rateu           varchar2(10);
    --汇率值变量
    v_rateh number(8, 4);
    v_rateu number(8, 4);
    data_not_prepared exception;
    --用户登录参数
    v_user_loginId varchar2(100);
    counterlimit   number(10); --报表查询记录条数限制
    v_ErrMsg       varchar2(500);
  begin
    --先判断能否从临时表TMP_FINANCE_DUEPREMIUM_LONG_O 中取数
    /*  select parameter_value into v_fetch_flag from control_parameter where parameter_name = 'duepremium_long_flag';
    if v_fetch_flag = '0' then
        raise data_not_prepared;
    end if;*/
    --报表查询记录条数限制,直接控制到excel最大接受值
    --  counterlimit := pkg_ereport.getcountlimit;
    --晚上开放至20万
    If to_char(Sysdate, 'hh24') < '21' And to_char(Sysdate, 'hh24') > '07' Then
      counterlimit := 65000;
    Else
      counterlimit := 200000;
    End If;
    /*机构：明细三级机构财务代码，如010100、010200等
    起止日期：保批单保险起期与核保日期的最大值所在范围(长期没有起止日期)
    截止日期：实收凭证日期
    团队：团队代码
    业务员：业务员代码
    险种：分车、财、意三大险种，三大险种下可以继续细分明细险种
    客户：分个人客户、团体客户
    渠道：分综合开拓、传统渠道、新渠道三大渠道，三大渠道下可细分明细渠道
    币种：人民币、港币、美元*/
    --分解通过reportnet提示页面获取的参数信息
    p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                       'userName_epcis'); --执行用户
    p_department_code := pkg_ereport.getParameterValue(parameters,
                                                       'finance_department_code_epcis'); --机构
    p_plan_class_code := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'planClass_code_epcis'); --险种大类
    p_plan_code       := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'plan_code_epcis'); --险种
    p_emp_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleAgent_code_epcis'); --业务员
    p_sale_group      := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleGroup_code_epcis'); --团队
    p_client_type     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'businessType_epcis'); --客户类型
    p_sale_channel    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'channel_code_epcis'); --渠道
    p_currency_code   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'currency_code_epcis'); --币种
    p_rateh           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateH_epcis'); --港币汇率值
    p_rateu           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateU_epcis'); --美元汇率值
    v_user_loginId    := p_user_code || '-' ||
                         to_char(sysdate, 'yyyymmdd hh24miss'); --根据当前时间组成此次会话的用户id
    --财务机构如果是汇总机构则做相应处理
    if p_department_code = substr(p_department_code, 1, 2) || '9999' then
      p_department_code := substr(p_department_code, 1, 2);
    elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
      p_department_code := substr(p_department_code, 1, 4);
    end if;
    --若用户没有在提示页面输入汇率，则从系统表中取汇率值
    if p_rateh is null then
      v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
    else
      v_rateh := to_number(p_rateh);
    end if;
    if p_rateu is null then
      v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
    else
      v_rateu := to_number(p_rateu);
    end if;
    --组织filter过滤
    filter := '';
    --机构（必选）
    filter := filter || ' where department_code like (''' ||
              p_department_code || '''||''%'') ';
    --险种
    if p_plan_code is not null then
      filter := filter || ' and plan_code = ''' || p_plan_code || ''' ';
    elsif p_plan_class_code is not null then
      filter := filter ||
                ' and plan_code in (select distinct b.plan_code
                            from ereport_plan_map_info b,
                                 plan_define c
                           where c.plan_code=b.plan_code
                             and b.ereport_plan_class_code = ''' ||
                p_plan_class_code || ''') ';
    end if;
    --业务员
    if p_emp_code is not null then
      filter := filter || ' and sale_agent_code =''' || p_emp_code || ''' ';
    end if;
    --团队
    if p_sale_group is not null then
      filter := filter || ' and group_code = ''' || p_sale_group || ''' ';
    end if;
    --客户
    if p_client_type is not null then
      filter := filter ||
                ' and decode(client_code, ''0'', ''2'', client_code) =''' ||
                p_client_type || ''' ';
    end if;
    --渠道
    if p_sale_channel is not null then
      filter := filter -- || ' and sale_channel_code ='''|| p_sale_channel ||''' '
                || ' and sale_channel_code in (Select a.business_source_code || a.business_source_detail_code ||
           a.channel_source_code || nvl(a.channel_source_detail_code, ''0'')
           From chg_channel_contrast a
           Where a.exploit_source_code = ''' ||
                p_sale_channel || ''' Union All Select ''' ||
                p_sale_channel || ''' From dual) ';
    end if;
    --币种
    if p_currency_code is not null then
      filter := filter || ' and currency_code = ''' || p_currency_code ||
                ''' ';
    end if;
    --增加每次返回条数限制
    filter := filter || ' and rownum <= ' || counterlimit;
    sel    := ' insert into EPCISACCT.tmp_finance_duepremium_short ' ||
              ' (department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date,insured_person, ' ||
              ' due_voucher_no,due_voucher_date,account_days,account_month,plan_name,client_name, ' ||
              ' sale_channel,sale_group,emp_name, ' ||
              ' currency,due_premium_sum,cancel_flag, ' ||
              ' underwrite_time,user_loginid,notice_no,settle_date,
              receipt_no, plan_code, agent_code,agent_chinese_name,payment_end_date) ' ||
              ' select department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date, insured_person, ' ||
              ' due_voucher_no,due_voucher_date,
              to_char(round(sysdate - greatest(payment_end_date ,underwrite_time),0)) as account_day,
              case when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 3 then ''3个月以内''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 3
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 6 then ''3到6个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 6
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 12 then ''6到12个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 12 then ''12个月以上''
               end account_month, plan_name,client_code,  ' ||
              ' nvl(sale_channel_name, (Select b.bnocnm From chg_channel_contrast a, business_source b ' ||
              ' Where b.bno = a.exploit_source_code ' ||
              ' And a.business_source_code || a.business_source_detail_code || a.channel_source_code || ' ||
              ' nvl(a.channel_source_detail_code, ''0'') = sale_channel_code)), ' ||
              ' group_code|| ''-'' ||group_name,sale_agent_code|| ''-'' ||sale_agent_name, ' ||
              ' currency_name,decode(currency_code, ''02'', due_premium * ''' ||
              v_rateh || ''', ''03'', due_premium * ''' || v_rateu ||
              ''', due_premium),cancel_flag , ' || ' underwrite_time,''' ||
              v_user_loginid || ''',notice_no,settle_date, ' ||
              ' receipt_no, plan_code, agent_code, agent_chinese_name, payment_end_date' ||
              ' from tmp_finance_duepremium_long_n c_u';

    /* 取代理人名称字段
    ||' case when (select ''x'' from premium_info a where receipt_no = c_u.receipt_no) is null
                       then (select (select c.agent_chinese_name
                                       from agent_define c where c.agent_code = a.cagtcde) agent_chinese_name
                               from ep_kcoldue a where crctno = c_u.receipt_no )
                       else (select (select c.agent_chinese_name
                                       from agent_define c where c.agent_code = a.agent_code) agent_chinese_name
                               from premium_info a where receipt_no = c_u.receipt_no)
                  end as agent_name,*/

    --sql组合
    sql_statement := sel || filter;
    execute immediate sql_statement;

    --打开游标返回
    open v_cursor for
      select department_code,
             department_name,
             policy_no,
             endorse_no,
             due_premium,
             insurance_begin_date,
             insurance_end_date,
             insured_person,
             due_voucher_no,
             due_voucher_date,
             account_days,
             account_month,
             plan_name,
             client_name,
             sale_channel,
             sale_group,
             emp_name,
             currency,
             due_premium_sum,
             cancel_flag,
             underwrite_time,
             notice_no,
             settle_date,
             agent_chinese_name,
             receipt_no,
             plan_code,
             agent_code,
             payment_end_date
        from EPCISACCT.tmp_finance_duepremium_short tmp
       Where rownum <= counterlimit
       order by policy_no;
    Re_Cursor := v_cursor;
  exception
    when data_not_prepared then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
    when others then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
      rollback;
      --raise;
  end pro_finance_duepremium_long_re;

------------------------------------------------------------------------------------------------------
--坏账报表页面调用pkg，直接返回数据给用户
--2009-06-18 wangyanhui006
-----------------------------------------------------------------------------------------------------

  procedure pro_finance_cancel_after_veri(parameters in varchar2,
                                          re_cursor  out t_cursor) is
    v_cursor          t_cursor;
    filter            varchar2(1800); --过滤
    sel               varchar2(3600); --选择项
    sql_statement     varchar2(3600); --Select语句
    p_user_code       varchar2(20);
    p_department_code varchar2(10);
    p_plan_code       varchar2(20);
    p_plan_class_code varchar2(20);
    p_emp_code        varchar2(30);
    p_sale_group      varchar2(20);
    p_client_type     varchar2(2);
    p_sale_channel    varchar2(10);
    p_currency_code   varchar2(2);
    p_rateh           varchar2(10);
    p_rateu           varchar2(10);
    v_rateh           number(8, 4);
    v_rateu           number(8, 4);
    data_not_prepared exception;
    v_user_loginId varchar2(100);
    counterlimit   number(10); --报表查询记录条数限制
    v_ErrMsg       varchar2(500);
  begin
    counterlimit := 65000;

    p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                       'userName_epcis'); --执行用户
    p_department_code := pkg_ereport.getParameterValue(parameters,
                                                       'finance_department_code_epcis'); --机构
    p_plan_class_code := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'planClass_code_epcis'); --险种大类
    p_plan_code       := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'plan_code_epcis'); --险种
    p_emp_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleAgent_code_epcis'); --业务员
    p_sale_group      := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleGroup_code_epcis'); --团队
    p_client_type     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'businessType_epcis'); --客户类型
    p_sale_channel    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'channel_code_epcis'); --渠道
    p_currency_code   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'currency_code_epcis'); --币种
    p_rateh           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateH_epcis'); --港币汇率值
    p_rateu           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateU_epcis'); --美元汇率值
    v_user_loginId    := p_user_code || '-' ||
                         to_char(sysdate, 'yyyymmdd hh24miss'); --根据当前时间组成此次会话的用户id
    --财务机构如果是汇总机构则做相应处理
    if p_department_code = substr(p_department_code, 1, 2) || '9999' then
      p_department_code := substr(p_department_code, 1, 2);
    elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
      p_department_code := substr(p_department_code, 1, 4);
    end if;
    --若用户没有在提示页面输入汇率，则从系统表中取汇率值
    if p_rateh is null then
      v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
    else
      v_rateh := to_number(p_rateh);
    end if;
    if p_rateu is null then
      v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
    else
      v_rateu := to_number(p_rateu);
    end if;
    --组织filter过滤
    filter := '';
    --机构（必选）
    filter := filter || ' where department_code like (''' ||
              p_department_code || '''||''%'') ';
    --险种
    if p_plan_code is not null then
      filter := filter || ' and plan_code = ''' || p_plan_code || ''' ';
    elsif p_plan_class_code is not null then
      filter := filter ||
                ' and plan_code in (select distinct b.plan_code
                                                                            from ereport_plan_map_info b,
                                                                                 plan_define c
                                                                           where c.plan_code=b.plan_code
                                                                             and b.ereport_plan_class_code = ''' ||
                p_plan_class_code || ''') ';
    end if;
    --业务员
    if p_emp_code is not null then
      filter := filter || ' and sale_agent_code =''' || p_emp_code || ''' ';
    end if;
    --团队
    if p_sale_group is not null then
      filter := filter || ' and group_code = ''' || p_sale_group || ''' ';
    end if;
    --客户
    if p_client_type is not null then
      filter := filter ||
                ' and decode(client_code, ''0'', ''2'', client_code) =''' ||
                p_client_type || ''' ';
    end if;
    --渠道
    if p_sale_channel is not null then
      filter := filter || ' and sale_channel_code =''' || p_sale_channel ||
                ''' ';
    end if;
    --币种
    if p_currency_code is not null then
      filter := filter || ' and currency_code = ''' || p_currency_code ||
                ''' ';
    end if;
    --增加每次返回条数限制
    filter := filter || ' and rownum <= ' || counterlimit;
    sel    := ' insert into EPCISACCT.tmp_finance_duepremium_short ' ||
              ' (department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date,insured_person, ' ||
              ' due_voucher_no,due_voucher_date,account_days,account_month,plan_name,client_name, ' ||
              ' sale_channel,sale_group,emp_name, ' ||
              ' currency,due_premium_sum,cancel_flag, ' ||
              ' underwrite_time,user_loginid,notice_no,settle_date,
              receipt_no, plan_code, agent_code,agent_chinese_name,payment_end_date) ' ||
              ' select department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date, insured_person, ' ||
              ' due_voucher_no,due_voucher_date,
              to_char(round(sysdate - greatest(payment_end_date ,underwrite_time),0)) as account_day,
              case when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 3 then ''3个月以内''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 3
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 6 then ''3到6个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 6
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 12 then ''6到12个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 12 then ''12个月以上''
               end account_month, plan_name,client_code,  ' ||
              ' sale_channel_name, group_code|| ''-'' ||group_name,sale_agent_code|| ''-'' ||sale_agent_name, ' ||
              ' currency_name,decode(currency_code, ''02'', due_premium * ''' ||
              v_rateh || ''', ''03'', due_premium * ''' || v_rateu ||
              ''', due_premium),cancel_flag , ' || ' underwrite_time,''' ||
              v_user_loginid || ''',notice_no,settle_date, ' ||
              ' receipt_no, plan_code, agent_code, agent_chinese_name, payment_end_date' ||
              ' from tmp_finance_duepremium_long_n c_u';

    sql_statement := sel || filter;
    execute immediate sql_statement;

    --打开游标返回
    open v_cursor for
      select department_code,
             department_name,
             policy_no,
             endorse_no,
             due_premium,
             insurance_begin_date,
             insurance_end_date,
             insured_person,
             (select p.journal_sequence
                from prop_journal_line p
               where p.receipt_no = tmp.receipt_no
                 and rownum = 1) as cancel_voucher_no,
             (select p.effective_date
                from prop_journal_line p
               where p.receipt_no = tmp.receipt_no
                 and rownum = 1) as cancel_voucher_date,
             account_days,
             account_month,
             plan_name,
             client_name,
             sale_channel,
             sale_group,
             emp_name,
             currency,
             due_premium_sum,
             cancel_flag,
             underwrite_time,
             notice_no,
             settle_date,
             agent_chinese_name,
             receipt_no,
             plan_code,
             agent_code,
             payment_end_date
        from EPCISACCT.tmp_finance_duepremium_short tmp
       where tmp.user_loginid = v_user_loginid
         and rownum <= counterlimit
         and exists (select (1)
                from premium_info pre
               where pre.receipt_no = tmp.receipt_no
                 and pre.cancel_after_verification = 'Y'
                 and pre.due_voucher_no is not null
                 and rownum = 1
              union all
              select (1)
                from kcoldue due
               where due.crctno = tmp.receipt_no
                 and due.cancel_after_verification = 'Y'
                 and due.caccno is not null
                 and rownum = 1)
         and exists (select (1)
                from prop_journal_line prop
               where prop.receipt_no = tmp.receipt_no
                 and voucher_type = '03'
                 and rownum = 1)
       order by policy_no;
    Re_Cursor := v_cursor;
  exception
    when data_not_prepared then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
    when others then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
      rollback;
  end pro_finance_cancel_after_veri;

------------------------------------------------------------------------------------------------------
--应收停单及预警清单页面调用pkg，直接返回数据给用户
--2009-07-15 by wangyanhui006
-----------------------------------------------------------------------------------------------------

  procedure pro_finance_emp_stop_business(parameters in varchar2,
                                          re_cursor  out t_cursor) is
    v_cursor          t_cursor;
    filter            varchar2(1800); --过滤
    sel               varchar2(10000); --选择项
    sql_statement     varchar2(10000); --Select语句
    p_department_code varchar2(10);
    p_emp_code        varchar2(30);
    data_not_prepared exception;
    counterlimit number(10); --报表查询记录条数限制
    p_flag       number(1); --定义查询应收停单清单（1）还是查询应收停单预警清单（2）
    v_ErrMsg     varchar2(500);
    --汇率值变量
    v_rateh number(8, 4);
    v_rateu number(8, 4);
  begin
    counterlimit := 65000;
    -- para:financeDepartments=040000^saleAgent_code_epcis=100012^listType_epcis=1^
    p_department_code := pkg_ereport.getParameterValue(parameters,
                                                       'financeDepartments'); --机构
    p_emp_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleAgent_code_epcis'); --业务员
    p_flag            := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'listType_epcis'); --清单类型
    --财务机构如果是汇总机构则做相应处理
    if p_department_code = substr(p_department_code, 1, 2) || '9999' then
      p_department_code := substr(p_department_code, 1, 2);
    elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
      p_department_code := substr(p_department_code, 1, 4);
    end if;
    v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
    v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);

    --组织filter过滤
    filter := '';
    --机构（必选）

    filter := filter || ' and substr(a.department_code, 1, 3) in' ||
              ' (select distinct substr(df.department_code, 1, 3)' ||
              '  from department_finance           df,' ||
              '        sas_due_supvz_set due' ||
              '   where due.department_code = substr(df.department_code, 1, 3)' ||
              '    and df.finance_department_code like  :p1 ||''%'')';
    filter := filter || ' and b.department_code like  :p2 ||''%''';
    --业务员(非必选)
    if p_emp_code is not null then
      filter := filter || ' and b.SALE_AGENT_CODE =''' || p_emp_code || '''';
    end if;

    --增加每次返回条数限制
    filter := filter || ' and rownum <=  :p3 ';
    if (p_flag = 1) then
      --应收停单清单

      sel := 'insert into FINANCE_DUE_STOP_POLICY_TMP' ||
             ' (EMPLOYEE_CODE,EMPLOYEE_NAME,DEPARTMENT_CODE,DEPARTMENT_NAME,POLICY_NO,ENDORSE_NO,TERM_NO,DUE_PREMIUM,' ||
             ' DUE_PREMIUM_SUM,INSURANCE_BEGIN_DATE,INSURANCE_END_DATE,' ||
             ' INSURED_PERSON,CURRENCY_CODE,CURRENCY_NAME,EMPLOYEE_CHANNEL,PLAN_CLASS_CODE,CLIENT_CODE,AGE_DAY,INVOICE_AMOUNT_SUM,INVOICE_RMB_SUM)' ||
             ' SELECT ' ||
             ' A.EMPLOYEE_CODE,a.employee_name,b.department_code,b.department_name,b.policy_no,b.endorse_no,' ||
             ' b.notice_no,b.due_premium,b.due_premium_sum,b.insurance_begin_date,b.insurance_end_date,' ||
             ' b.insured_person,b.currency_code,b.currency_name,' ||
             ' NVL(A.EMPLOYEE_CHANNEL, A.EMPLOYEE_TYPE) EMPLOYEE_CHANNEL,'
            --||' DECODE(C.PLAN_CLASS_CODE, ''A'', ''A'', ''B'', ''A'', ''C'', ''B'', ''C'') PLAN_CLASS_CODE, '
             || 'B.PLAN_CODE PLAN_CLASS_CODE, ' ||
             ' DECODE(B.CLIENT_CODE, ''1'', ''1'', ''2''),' ||
             ' ROUND(SYSDATE - GREATEST(PAYMENT_END_DATE, UNDERWRITE_TIME), 0), ' ||
             '(nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = b.receipt_no
                     and p.plan_code = b.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = b.receipt_no
                               and ptp.plan_code = b.plan_code),
                            0)),
             decode(currency_code, ''02'', :p4, ''03'', :p5, ''1'')
             * (nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = b.receipt_no
                     and p.plan_code = b.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = b.receipt_no
                               and ptp.plan_code = b.plan_code),
                            0)) ' ||
             ' FROM epcisbase.SAS_EMPLOYEE A, TMP_FINANCE_DUEPREMIUM_LONG_N B '
            --||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
            --||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
             ||
             ' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE and b.policy_no in (' ||
             ' SELECT ' || 'b.policy_no' ||
             ' FROM epcisbase.SAS_EMPLOYEE A, TMP_FINANCE_DUEPREMIUM_LONG_N B '
            --||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
            --||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
             || ' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE ' ||
             ' AND A.LEAVE_DATE IS NULL AND B.INSURANCE_BEGIN_DATE >= TO_DATE(''2009-1-1'', ''yyyy-mm-dd'')' ||
             '   AND ROUND(SYSDATE - GREATEST(PAYMENT_END_DATE, UNDERWRITE_TIME), 0) >=' ||
             '       (select se.use_age ' ||
             '          from sas_due_supvz_set se ' ||
             '        where se.effective_date < sysdate + 1 ' ||
             '          and (se.invalidate_date is null or se.invalidate_date > sysdate) ' ||
             '          and se.channel_code = a.employee_channel ' ||
             '          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';
      sel := sel || filter ||
             'group by b.policy_no having sum(b.due_premium)>0)' ||
             ' AND ROUND(SYSDATE - GREATEST(PAYMENT_END_DATE, UNDERWRITE_TIME), 0) >=' ||
             '       (select se.use_age ' ||
             '          from sas_due_supvz_set se ' ||
             '        where se.effective_date < sysdate + 1 ' ||
             '          and (se.invalidate_date is null or se.invalidate_date > sysdate) ' ||
             '          and se.channel_code = a.employee_channel ' ||
             '          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';

    elsif (p_flag = 2) then
      --应收停单预警清单
      sel := 'insert into FINANCE_DUE_STOP_POLICY_TMP' ||
             ' (EMPLOYEE_CODE,EMPLOYEE_NAME,DEPARTMENT_CODE,DEPARTMENT_NAME,POLICY_NO,ENDORSE_NO,TERM_NO,DUE_PREMIUM,' ||
             ' DUE_PREMIUM_SUM,INSURANCE_BEGIN_DATE,INSURANCE_END_DATE,' ||
             ' INSURED_PERSON,CURRENCY_CODE,CURRENCY_NAME,EMPLOYEE_CHANNEL,PLAN_CLASS_CODE,CLIENT_CODE,AGE_DAY,INVOICE_AMOUNT_SUM,INVOICE_RMB_SUM)' ||
             ' SELECT ' ||
             ' A.EMPLOYEE_CODE,a.employee_name,b.department_code,b.department_name,b.policy_no,b.endorse_no,' ||
             ' b.notice_no,b.due_premium,b.due_premium_sum,b.insurance_begin_date,b.insurance_end_date,' ||
             ' b.insured_person,b.currency_code,b.currency_name,' ||
             ' NVL(A.EMPLOYEE_CHANNEL, A.EMPLOYEE_TYPE) EMPLOYEE_CHANNEL,'
            --||' DECODE(C.PLAN_CLASS_CODE, ''A'', ''A'', ''B'', ''A'', ''C'', ''B'', ''C'') PLAN_CLASS_CODE, '
             || 'B.PLAN_CODE PLAN_CLASS_CODE, ' ||
             ' DECODE(B.CLIENT_CODE, ''1'', ''1'', ''2''),' ||
             ' ROUND(SYSDATE - GREATEST(PAYMENT_END_DATE, UNDERWRITE_TIME), 0), ' ||
            '(nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = b.receipt_no
                     and p.plan_code = b.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = b.receipt_no
                               and ptp.plan_code = b.plan_code),
                            0)),
             decode(currency_code, ''02'', :p4, ''03'', :p5, ''1'')
             * (nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = b.receipt_no
                     and p.plan_code = b.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = b.receipt_no
                               and ptp.plan_code = b.plan_code),
                            0)) ' ||
             ' FROM epcisbase.SAS_EMPLOYEE A, TMP_FINANCE_DUEPREMIUM_LONG_N B '
            --||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
            --||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
             ||
             ' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE and b.policy_no in (' ||
             ' SELECT ' || 'b.policy_no' ||
             ' FROM epcisbase.SAS_EMPLOYEE A, TMP_FINANCE_DUEPREMIUM_LONG_N B '
            -- ||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
            --||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
             || ' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE ' ||
             ' AND A.LEAVE_DATE IS NULL AND B.INSURANCE_BEGIN_DATE >= TO_DATE(''2009-1-1'', ''yyyy-mm-dd'')' ||
             '   AND ROUND(SYSDATE - GREATEST(PAYMENT_END_DATE, UNDERWRITE_TIME), 0) between' ||
             '       (select se.use_age ' ||
             '          from sas_due_supvz_set se ' ||
             '        where se.effective_date < sysdate + 1 ' ||
             '          and (se.invalidate_date is null or se.invalidate_date > sysdate) ' ||
             '          and se.channel_code = a.employee_channel ' ||
             '          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))-15 and ' ||
             '       (select se.use_age ' ||
             '          from sas_due_supvz_set se ' ||
             '        where se.effective_date < sysdate + 1 ' ||
             '          and (se.invalidate_date is null or se.invalidate_date > sysdate) ' ||
             '          and se.channel_code = a.employee_channel ' ||
             '          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';
      sel := sel || filter ||
             'group by b.policy_no having sum(b.due_premium)>0)' ||
             ' AND ROUND(SYSDATE - GREATEST(PAYMENT_END_DATE, UNDERWRITE_TIME), 0) between' ||
             '       (select se.use_age ' ||
             '          from sas_due_supvz_set se ' ||
             '        where se.effective_date < sysdate + 1 ' ||
             '          and (se.invalidate_date is null or se.invalidate_date > sysdate) ' ||
             '          and se.channel_code = a.employee_channel ' ||
             '          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))-15 and ' ||
             '       (select se.use_age ' ||
             '          from sas_due_supvz_set se ' ||
             '        where se.effective_date < sysdate + 1 ' ||
             '          and (se.invalidate_date is null or se.invalidate_date > sysdate) ' ||
             '          and se.channel_code = a.employee_channel ' ||
             '          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';

    end if;

    sql_statement := sel;

    execute immediate sql_statement using v_rateh,v_rateu,p_department_code,p_department_code,counterlimit;

    --打开游标返回
    open v_cursor for
      select EMPLOYEE_CODE as 业务员代码,
             EMPLOYEE_NAME as 业务员名称,
             DEPARTMENT_CODE as 机构代码,
             DEPARTMENT_NAME as 机构名称,
             POLICY_NO as 保单号,
             ENDORSE_NO as 批单号,
             TERM_NO as 期次,
             DUE_PREMIUM as "金额(不含税)",
             DUE_PREMIUM_SUM as "折合人民币(不含税)",
             INVOICE_AMOUNT_SUM as "金额(含税)",
             INVOICE_RMB_SUM as "折合人民币(含税)",
             INSURANCE_BEGIN_DATE as 保险起期,
             INSURANCE_END_DATE as 保险止期,
             INSURED_PERSON as 被保险人,
             CURRENCY_CODE as 币种编码,
             CURRENCY_NAME as 币种,
             EMPLOYEE_CHANNEL as 渠道,
             decode(EMPLOYEE_CHANNEL,
                    'DS',
                    '直销',
                    'CS',
                    '综合开拓',
                    'AS',
                    '车行',
                    'BS',
                    '重点客户',
                    'ES',
                    '代理',
                    'FS',
                    '非重客经纪',
                    'GS',
                    '混合业务渠道',
                    'HS',
                    '新渠道',
                    'IS',
                    '银保渠道',
                    'JS',
                    '零售渠道',
                    'KS',
                    '移动渠道',
                    EMPLOYEE_CHANNEL) as 渠道名称,
             PLAN_CLASS_CODE as 险种,
             (select p.produce
                from produce_map p
               where p.ins_sort = tmp.plan_class_code
                 And rownum = 1) as 险种名称,
             CLIENT_CODE as 客户代码,
             AGE_DAY as 帐龄
        from FINANCE_DUE_STOP_POLICY_TMP tmp
       where rownum <= counterlimit
       order by EMPLOYEE_CODE;
    Re_Cursor := v_cursor;
  exception
    when data_not_prepared then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
    when others then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
      rollback;
  end pro_finance_emp_stop_business;

  ------------------------------------------------------------------------------------------------------
   --数据初始化时调用，由ETL调用，调用过程在ETL的 REPORT/Jobs/PDBC2/JOB/01Finance/P_PRO_JOB 下
   -----------------------------------------------------------------------------------------------------
    procedure pro_job is
        v_sid        number;
        v_serial#    number;
        v_ErrCodeVal number  :=0;
        v_ErrMsg     varchar2(500);
        v_begin_time date;
        v_end_time   date;
        v_timetest varchar2(2);
      --v_fetch_flag number:=0;
    begin
      select sysdate into v_begin_time from dual;
      select substr(to_char(sysdate, 'yyyy-mm-dd'), instr(to_char(sysdate, 'yyyy-mm-dd'), '-', -1)+1) into v_timetest from dual;
       v_sid:='';
       v_serial#:='';

      insert into  epcis_job_log values (sysdate, v_sid, v_serial#, 0, '暂无', '财务应收长期报表job开始运行',sys_guid());
      commit;
      begin
           insert into  epcis_job_log values (sysdate, v_sid, v_serial#, 0, '暂无', '初始化，清除TMP_FINANCE_DUEPREMIUM_LONG_N表和TMP_FINANCE_DUEPREMIUM_LONG_O表数据',sys_guid());
           commit;
           --执行此pkg必须在早上8点30分之前执行，否则会异常
           --epcisrpt_ddl_prc.truncate_table('TMP_FINANCE_DUEPREMIUM_LONG_N');
           --epcisrpt_ddl_prc.truncate_table('TMP_FINANCE_DUEPREMIUM_LONG_O');
           --历史程序，这两个不能做truancate操作，2021-10-8 zhaominzhi692
      exception
            when others then
                 v_errcodeval := sqlcode;
                 v_errmsg := substr('删除表tmp_finance_duepremium_long_n或tmp_finance_duepremium_long_o表数据失败'|| sqlerrm, 1, 500);
                insert into  epcis_job_log values (sysdate, v_sid, v_serial#, v_errcodeval, v_errmsg, '财务应收长期报表job，当前执行步骤为truncate表tmp_finance_duepremium_long_o',sys_guid());
                commit;
                goto l_over;
      end;

       begin
        insert into  epcis_job_log values (sysdate, v_sid, v_serial#, 0, '暂无', 'pkg_ereport_finance_due_new.pro_finance_duepremium_long开始运行',sys_guid());
        commit;
            pkg_ereport_finance_due_new.pro_finance_duepremium_long;
            select sysdate into v_end_time from dual;
            if v_end_time-v_begin_time > 1/3 then
                insert into epcis_job_log
                    values (sysdate, v_sid, v_serial#, 0,
                            '存储过程pkg_ereport_finance_due_new.pro_finance_duepremium_long执行时间过长，超过8小时' ,
                            '财务应收长期报表job，当前执行步骤为运行pkg_ereport_finance_due_new.pro_finance_duepremium_long，以获取所有符合应收逻辑的数据记录。',sys_guid());
                commit;
                goto l_over;
            end if;
        exception
            when others then
                v_ErrCodeVal := sqlcode;
                v_ErrMsg := substr('获取符合应收逻辑的数据记录过程出错'|| sqlerrm, 1, 500);
                insert into  epcis_job_log
                    values (sysdate, v_sid, v_serial#, v_ErrCodeVal, v_ErrMsg, '财务应收长期报表job，当前执行步骤为运行pkg_ereport_finance_due_new.pro_finance_duepremium_long，以获取所有符合应收逻辑的数据记录。',sys_guid());
                commit;
                goto l_over;
        end;
      <<l_over>>
      if v_ErrCodeVal = 0 then
            insert into  epcis_job_log
                values (sysdate, v_sid, v_serial#, 0, '无错误信息', '财务应收长期报表报表job运行成功结束',sys_guid());
            commit;
      else
          insert into  epcis_job_log
                values (sysdate, v_sid, v_serial#, 0, '存在错误信息，请查看之前日志记录', '财务应收长期报表报表job运行成功失败',sys_guid());
            commit;
        end if;
      <<pro_job_end>>
        null;
    end pro_job;
    ----------------------------------------------------------------
    --创建：远期应收清单 liuyifu 20090531
    --需求见：[SR_PA00670582－远期应收清单]说明书
    ----------------------------------------------------------------
    procedure pro_finance_duepremium_foresee(parameters in varchar2,
                                             re_cursor  out t_cursor) is
      v_cursor t_cursor;
      --filter        varchar2 (1800);              --过滤
      --sel           varchar2 (3600);              --选择项
      --sql_statement     varchar2 (3600);             --Select语句
      --过滤参数
      p_user_code       varchar2(20);
      p_department_code varchar2(10);
      p_plan_code       varchar2(20);
      p_plan_class_code varchar2(20);
      p_emp_code        varchar2(30);
      p_sale_group      varchar2(20);
      p_client_type     varchar2(2);
      p_sale_channel    varchar2(10);
      p_currency_code   varchar2(2);
      p_rateh           varchar2(10);
      p_rateu           varchar2(10);
      --汇率值变量
      v_rateh number(8, 4);
      v_rateu number(8, 4);
      data_not_prepared exception;
      --用户登录参数
      v_user_loginId   varchar2(100);
      counterlimit     number(10); --报表查询记录条数限制
      v_ErrMsg         varchar2(500);
      v_count          number;
      v_count1         number;
      v_premium_amount number(16, 2);

      --预收信息参数
      v_voucher_no           varchar2(30); --预收凭证号
      v_voucher_craeted_date date; --预收凭证日期
      v_precol_amount        number(16, 2); --预收金额

      --取远期数据游标
      cursor c_foresee is
        select a.crctno receipt_no,
               a.cdno finance_department_code,
               (select description
                  from institutions t
                 where t.flex_value = a.cdno) finance_department_name, --机构名称,
               a.CPARNO group_code, --团队代码
               (select cgrpcnm from kgrpcde where cgrpcde = a.CPARNO) group_name, --团队名称
               a.CEMPCDE sale_agent_code, -- 业务员
               (select employee_name
                  from epcisbase.sas_employee
                 where employee_code = a.CEMPCDE) sale_agent_name, --业务员名称,
               a.cplyno policy_no, --保单号
               a.cedrno endorse_no, -- 批单号
               a.CPAYNME insured_name, -- 被保险人
               a.NPRMDUE premium_amount, -- 金额,
               a.DPLYSTR insurance_begin_time, -- 保险起期
               a.DPLYEND insurance_end_time, -- 保险止期
               a.DFCD underwrite_time, --核保日期,
               a.DPAYEND payment_end_date, -- 缴费止期,
               a.DCALDTE settle_date, --  结算日期,
               a.NTERMNO term_no, -- 期数
               to_char(round(sysdate - greatest(a.DPAYEND, a.DFCD), 0)) as account_day, --账龄(日),
               case
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) <= 3 then
                  '3个月以内'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) > 3 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) <= 6 then
                  '3到6个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) > 6 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) <= 12 then
                  '6到12个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) > 12 then
                  '12个月以上'
               end account_month, --账龄(月),
               a.CINSCDE plan_code, --险种
               (select t.plan_chinese_name
                  from plan_define t
                 where t.plan_code = a.CINSCDE) plan_name, --险种名称
               '' as channel_source_code, -- 渠道代码
               '' as channel_name, --渠道名称,
               '' as client_attribute, --客户类型编码
               '' as client_name, --客户类型名称
               (select agent_chinese_name
                  from agent_define t
                 where t.agent_code = a.CAGTCDE) agent_name, --代理人名称,
               a.CCURNO currency_code, --币种代码
               (select currency_chinese_name
                  from currency_define
                 where currency_code = a.CCURNO) currency_name --币种名称,
          from kcoldue a
         where a.cdno like (p_department_code)
           and a.CACCNO is null --应收凭证号
           and a.ACTUAL_VOUCHER_NO is null -- 实收凭证号
           and greatest(a.DPLYSTR, a.DFCD) >=
               add_months(trunc(sysdate, 'mm'), 1)
        union all
        select /*+index(a IDX_PREMIUMINFO_FUN4)*/a.receipt_no,
               a.finance_department_code, -- 机构代码,
               (select description
                  from institutions t
                 where t.flex_value = a.finance_department_code) finance_department_name, --机构名称,
               a.group_code, --团队代码
               (select cgrpcnm from kgrpcde where cgrpcde = a.group_code) group_name, --团队名称
               a.sale_agent_code, -- 业务员
               (select employee_name
                  from epcisbase.sas_employee
                 where employee_code = a.sale_agent_code) sale_agent_name, --业务员名称,
               a.policy_no, --保单号,
               a.endorse_no, -- 批单号,
               a.insured_name, -- 被保险人,
               b.premium_amount, -- 金额,
               --最后预收凭证号,
               --最后预收凭证日期,
               --a.precol_amount, -- 预收金额,
               a.insurance_begin_time, -- 保险起期,
               a.insurance_end_time, -- 保险止期,
               a.underwrite_time, -- 核保日期,
               a.payment_end_date, -- 缴费止期,
               a.settle_date, -- 结算日期,
               a.term_no, -- 期数,
               to_char(round(sysdate -
                             greatest(a.payment_end_date, a.underwrite_time),
                             0)) as account_day, --账龄(日),
               case
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) <= 3 then
                  '3个月以内'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) > 3 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) <= 6 then
                  '3到6个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) > 6 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) <= 12 then
                  '6到12个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) > 12 then
                  '12个月以上'
               end account_month, --账龄(月),
               b.plan_code, --险种
               (select t.plan_chinese_name
                  from plan_define t
                 where t.plan_code = b.plan_code) plan_name, --险种名称
               a.channel_source_code, -- 渠道代码
               (select bnocnm
                  from business_source
                 where bno = a.channel_source_code) channel_name, --渠道名称,
               -- a.client_attribute, --客户类型编码
               decode(a.client_attribute, '0', '2', a.client_attribute) client_attribute, --客户类型编码
               decode(a.client_attribute, '1', '个体', '团体') client_name, --客户类型名称,
               (select agent_chinese_name
                  from agent_define t
                 where t.agent_code = a.agent_code) agent_name, --代理人名称,
               a.currency_code, --币种代码
               (select currency_chinese_name
                  from currency_define
                 where currency_code = a.currency_code) currency_name --币种名称,
          from premium_info a, premium_plan b
         where a.finance_department_code like (p_department_code)
           and a.due_voucher_no is null --应收凭证号
           and a.actual_voucher_no is null -- 实收凭证号
           and greatest(a.insurance_begin_time, a.underwrite_time) >=
               add_months(trunc(sysdate, 'mm'), 1)
           and nvl(a.disable_flag, 'N') <> 'Y' --是否失效
           and a.receipt_no = b.receipt_no;
      --                  and a.precol_amount != 0;   --未全额预收,解释：该笔应收没有收钱即系统里没有预收

      --v_foresee c_foresee%rowtype;

    begin

      --报表查询记录条数限制,直接控制到excel最大接受值
      --  counterlimit := pkg_ereport.getcountlimit;
      counterlimit := 65000;

      --分解通过reportnet提示页面获取的参数信息
      p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                         'userName_epcis'); --执行用户
      p_department_code := pkg_ereport.getParameterValue(parameters,
                                                         'finance_department_code_epcis'); --机构
      p_plan_class_code := pkg_ereport.getparametervalue(parameters,
                                                         'planClass_code_epcis'); --险种大类
      p_plan_code       := pkg_ereport.getparametervalue(parameters,
                                                         'plan_code_epcis'); --险种
      p_emp_code        := pkg_ereport.getparametervalue(parameters,
                                                         'saleAgent_code_epcis'); --业务员
      p_sale_group      := pkg_ereport.getparametervalue(parameters,
                                                         'saleGroup_code_epcis'); --团队
      p_client_type     := pkg_ereport.getparametervalue(parameters,
                                                         'businessType_epcis'); --客户类型
      p_sale_channel    := pkg_ereport.getparametervalue(parameters,
                                                         'channel_code_epcis'); --渠道
      p_currency_code   := pkg_ereport.getparametervalue(parameters,
                                                         'currency_code_epcis'); --币种
      p_rateh           := pkg_ereport.getparametervalue(parameters,
                                                         'rateH_epcis'); --港币汇率值
      p_rateu           := pkg_ereport.getparametervalue(parameters,
                                                         'rateU_epcis'); --美元汇率值
      v_user_loginId    := p_user_code || '-' ||
                           to_char(sysdate, 'yyyymmdd hh24miss'); --根据当前时间组成此次会话的用户id

      --财务机构如果是汇总机构则做相应处理
      if p_department_code = substr(p_department_code, 1, 2) || '9999' then
        p_department_code := substr(p_department_code, 1, 2) || '%';
      elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
        p_department_code := substr(p_department_code, 1, 4) || '%';
      end if;

      --若用户没有在提示页面输入汇率，则从系统表中取汇率值
      if p_rateh is null then
        v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
      else
        v_rateh := to_number(p_rateh);
      end if;

      if p_rateu is null then
        v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
      else
        v_rateu := to_number(p_rateu);
      end if;

      --游标开始循环取数据
      for v_foresee in c_foresee loop

        --过滤险种大类
        if p_plan_class_code is not null then
          select count(*)
            into v_count
            from ereport_plan_map_info b, plan_define c
           where c.plan_code = b.plan_code
             and b.ereport_plan_class_code = p_plan_class_code
             and b.plan_code = v_foresee.plan_code;

          if v_count = 0 then
            goto begin_loop;
          end if;
        end if;

        --过滤险种
        if p_plan_code is not null then
          if p_plan_code != v_foresee.plan_code then
            goto begin_loop;
          end if;
        end if;

        --过滤业务员
        if p_emp_code is not null then
          if p_emp_code != v_foresee.sale_agent_code then
            goto begin_loop;
          end if;
        end if;

        --过滤团队
        if p_sale_group is not null then
          if p_sale_group != v_foresee.group_code then
            goto begin_loop;
          end if;
        end if;

        --过滤客户类型
        if p_client_type is not null then
          if p_client_type != v_foresee.client_attribute then
            goto begin_loop;
          end if;
        end if;
        --过滤渠道
        if p_sale_channel is not null then
          if p_sale_channel != v_foresee.channel_source_code then
            goto begin_loop;
          end if;
        end if;
        --过滤币种
        if p_currency_code is not null then
          if p_currency_code != v_foresee.currency_code then
            goto begin_loop;
          end if;
        end if;

        --从老表中取预收数据
        begin
          select sum(due_amount), --预收金额
                 max(voucher_no), --预收凭证号最后一次
                 max(voucher_date) --预收凭证日期 最后一次
            into v_precol_amount, v_voucher_no, v_voucher_craeted_date
            from (select t.cvouno_precol   voucher_no, --预收凭证号
                         t.dcoldte         voucher_date, --预收凭证日期
                         t6.premium_amount due_amount --预收金额
                    from kprecol t, premium_info t1, premium_plan t6
                   where t.cplyno = t1.policy_no
                     and t.cedrno = t1.endorse_no
                     and t.ntermno = t1.term_no
                     and t.cvouno_precol is not null --已经生成预收凭证
                     and t6.receipt_no = t1.receipt_no
                     and t.cinscde <> 'A24'
                     and t.cdno like (v_foresee.finance_department_code)
                     and t1.policy_no = v_foresee.policy_no
                     and t.cplyno = v_foresee.policy_no
                     and nvl(t.cedrno, 'a') = nvl(v_foresee.endorse_no, 'a')
                     and nvl(t1.endorse_no, 'a') =
                         nvl(v_foresee.endorse_no, 'a')
                  union all
                  -- 从新表中取数据保单
                  --取2121000000 科目（02 产险保费  0A 意健险保费  14 产险退保费 1E 意健险退保费），
                  select t3.voucher_no voucher_no, --预收凭证号
                         t3.voucher_date voucher_craeted_date, --预收凭证日期
                         decode(t2.collect_pay_item,
                                '14',
                                -amount,
                                '1E',
                                -amount,
                                amount) precol_amount
                  --t6.premium_amount precol_amount --预收金额
                    from collection_notice         t3,
                         collect_pay_info          t2,
                         business_receipt_relation t4,
                         premium_info              t1,
                         premium_plan              t6
                   where t3.notice_status = '81'
                     and t3.voucher_no is not null --预收凭证号
                     and t3.notice_no = t2.notice_no
                     and t2.collect_pay_item in ('02', '0A', '14', '1E')
                     and t2.collect_pay_no = t4.collect_pay_no
                     and t1.receipt_no = t4.receipt_no
                        --           and t4.policy_no = t1.policy_no
                        --           and nvl(t4.endorse_no, 'a') = nvl(t1.endorse_no, 'a')
                        --           and t4.term_no = t1.term_no
                     and t1.receipt_no = t6.receipt_no
                     and t6.plan_code <> 'A24'
                     and t3.account_department_code like
                         (v_foresee.finance_department_code)
                     and t1.policy_no = v_foresee.policy_no
                     and nvl(t1.endorse_no, 'a') =
                         nvl(v_foresee.endorse_no, 'a')
                  union all
                  --从支付通知单取退保的预收
                  select t3.voucher_no voucher_no, --预收凭证号
                         t3.voucher_date voucher_craeted_date, --预收凭证日期
                         decode(t2.collect_pay_item,
                                '14',
                                -amount,
                                '1E',
                                -amount,
                                amount) precol_amount
                  --t6.premium_amount precol_amount --预收金额
                    from payment_notice            t3,
                         collect_pay_info          t2,
                         business_receipt_relation t4,
                         premium_info              t1,
                         premium_plan              t6
                   where t3.notice_status = '81'
                     and t3.voucher_no is not null --预收凭证号
                     and t3.notice_no = t2.notice_no
                     and t2.collect_pay_item in ('02', '0A', '14', '1E')
                     and t2.collect_pay_no = t4.collect_pay_no
                     and t1.receipt_no = t4.receipt_no
                        --                     and t4.policy_no = t1.policy_no
                        --                     and nvl(t4.endorse_no, 'a') = nvl(t1.endorse_no, 'a')
                        --                     and t4.term_no = t1.term_no
                     and t1.receipt_no = t6.receipt_no
                     and t6.plan_code <> 'A24'
                     and t3.account_department_code like
                         (v_foresee.finance_department_code)
                     and t1.policy_no = v_foresee.policy_no
                     and nvl(t1.endorse_no, 'a') =
                         nvl(v_foresee.endorse_no, 'a'));
        exception
          when no_data_found then
            v_precol_amount        := 0;
            v_voucher_no           := null;
            v_voucher_craeted_date := null;
            --该笔应收没有对应的全额预收。全额预收的解释：该笔应收没有收钱即系统里没有预收
            --或者该笔应收收到的钱小于应收金额即系统里的预收金额之和小于应收金额
            goto begin_loop;
          when others then
            v_precol_amount        := 0;
            v_voucher_no           := null;
            v_voucher_craeted_date := null;
            --该笔应收没有对应的全额预收。全额预收的解释：该笔应收没有收钱即系统里没有预收
            --或者该笔应收收到的钱小于应收金额即系统里的预收金额之和小于应收金额
            goto begin_loop;
        end;

        --“系统中该笔应收金额>预收金额之和”这以保单，批单来算 ?,取以保批单汇总金额的应收金额
        --该笔应收没有对应的全额预收。全额预收的解释：该笔应收没有收钱即系统里没有预收
        --或者该笔应收收到的钱小于应收金额即系统里的预收金额之和小于应收金额
        begin
          select sum(b.premium_amount)
            into v_premium_amount
            from premium_info a, premium_plan b
           where a.finance_department_code like (p_department_code)
             and a.due_voucher_no is null --应收凭证号
             and a.actual_voucher_no is null -- 实收凭证号
             and greatest(a.insurance_begin_time, a.underwrite_time) >=
                 add_months(trunc(sysdate, 'mm'), 1)
             and nvl(a.disable_flag, 'N') <> 'Y' --是否失效
             and a.receipt_no = b.receipt_no
             and a.policy_no = v_foresee.policy_no
             and nvl(a.endorse_no, 'a') = nvl(v_foresee.endorse_no, 'a');
        exception
          when no_data_found then
            v_premium_amount := 0;
          when others then
            v_premium_amount := 0;
        end;

        if v_premium_amount > 0 and v_premium_amount <= v_precol_amount then
          goto begin_loop;
        elsif v_premium_amount < 0 and v_premium_amount >= v_precol_amount then
          goto begin_loop;
        end if;

        --将数据写入临时表
        insert into EPCISACCT.tmp_finance_duepremium_short
          (department_code,
           department_name,
           sale_group,
           emp_name,
           policy_no,
           endorse_no,
           insured_person,
           due_premium,
           due_voucher_no,
           due_voucher_date,
           due_premium_sum, --预收替代字段
           insurance_begin_date,
           insurance_end_date,
           underwrite_time,
           payment_end_date,
           settle_date,
           notice_no,
           account_days,
           account_month,
           plan_name,
           sale_channel,
           client_name,
           agent_chinese_name,
           currency,
           user_loginid)
        values
          (v_foresee.finance_department_code, -- 机构代码,
           v_foresee.finance_department_name, --机构名称,
           v_foresee.group_name, --团队名称
           v_foresee.sale_agent_name, --业务员名称,
           v_foresee.policy_no, --保单号,
           v_foresee.endorse_no, -- 批单号,
           v_foresee.insured_name, -- 被保险人,
           v_foresee.premium_amount, -- 金额,
           v_voucher_no, --最后预收凭证号,
           v_voucher_craeted_date, --最后预收凭证日期,
           v_precol_amount, -- 预收金额,
           v_foresee.insurance_begin_time, -- 保险起期,
           v_foresee.insurance_end_time, -- 保险止期,
           v_foresee.underwrite_time, -- 核保日期,
           v_foresee.payment_end_date, -- 缴费止期,
           v_foresee.settle_date, -- 结算日期,
           v_foresee.term_no, -- 期数,
           v_foresee.account_day, --账龄(日),
           v_foresee.account_month, --账龄(月),
           v_foresee.plan_name, --险种名称
           v_foresee.channel_name, --渠道名称,
           v_foresee.client_name, --客户类型名称,
           v_foresee.agent_name, --代理人名称,
           v_foresee.currency_name, --币种名称
           v_foresee.currency_code --币种代码
           );

        v_count1 := v_count1 + 1;
        if v_count1 >= counterlimit then
          goto end_loop;
        end if;

        --下一循环，放在这里
        <<begin_loop>>
        null;

      end loop;

      <<end_loop>>

      --返回结果集
      open v_cursor for
        select department_code,
               department_name,
               sale_group,
               emp_name,
               policy_no,
               endorse_no,
               insured_person,
               due_premium,
               due_voucher_no,
               due_voucher_date,
               due_premium_sum, --预收替代字段
               insurance_begin_date,
               insurance_end_date,
               underwrite_time,
               payment_end_date,
               settle_date,
               notice_no,
               account_days,
               account_month,
               plan_name,
               sale_channel,
               client_name,
               agent_chinese_name,
               currency,
               decode(user_loginid,
                      '02',
                      due_premium * v_rateh,
                      '03',
                      due_premium * v_rateu,
                      due_premium) as rmb_due_premium
          from EPCISACCT.tmp_finance_duepremium_short
         order by policy_no, endorse_no;

      Re_Cursor := v_cursor;

    exception
      when data_not_prepared then
        v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
        raise_application_error(-20001, v_ErrMsg);
      when others then
        v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
        raise_application_error(-20001, v_ErrMsg);
        rollback;
    end pro_finance_duepremium_foresee;

    ----------------------------------------------------------------
    --创建：远期未全额预收停单 ex-fanjianguo001 20100512
    --需求见：[SR_PA01029466－远期远期未全额预收停单财务关联需求]说明书
    ----------------------------------------------------------------
    procedure pro_finance_notcol_foresee(parameters in varchar2,
                                             re_cursor  out t_cursor) is
      v_cursor t_cursor;
      --filter        varchar2 (1800);              --过滤
      --sel           varchar2 (3600);              --选择项
      --sql_statement     varchar2 (3600);          --Select语句
      --过滤参数
      p_user_code       varchar2(20);
      p_department_code varchar2(10);
      p_plan_code       varchar2(20);
      p_plan_class_code varchar2(20);
      p_emp_code        varchar2(30);
      p_sale_group      varchar2(20);
      p_client_type     varchar2(2);
      p_sale_channel    varchar2(10);
      p_currency_code   varchar2(2);
      p_rateh           varchar2(10);
      p_rateu           varchar2(10);
      --汇率值变量
      v_rateh number(8, 4);
      v_rateu number(8, 4);
      data_not_prepared exception;
      --用户登录参数
      v_user_loginId   varchar2(100);
      counterlimit     number(10); --报表查询记录条数限制
      v_ErrMsg         varchar2(500);
      v_count          number;
      v_count1         number;
      v_premium_amount number(16, 2);

      --预收信息参数
      v_voucher_no           varchar2(30); --预收凭证号
      v_voucher_craeted_date date; --预收凭证日期
      v_precol_amount        number(16, 2); --预收金额

      --取远期数据游标
      cursor c_foresee is
      select a.crctno receipt_no,
               a.cdno finance_department_code,
               (select description
                  from institutions t
                 where t.flex_value = a.cdno) finance_department_name, --机构名称,
               a.CPARNO group_code, --团队代码
               (select cgrpcnm from kgrpcde where cgrpcde = a.CPARNO) group_name, --团队名称
               a.CEMPCDE sale_agent_code, -- 业务员代码
               (select employee_name
                  from epcisbase.sas_employee
                 where employee_code = a.CEMPCDE) sale_agent_name, --业务员名称,
               a.cplyno policy_no, --保单号
               a.cedrno endorse_no, -- 批单号
               a.CPAYNME insured_name, -- 被保险人
               a.NPRMDUE premium_amount, -- 金额,（应收保费）
               a.DPLYSTR insurance_begin_time, -- 保险起期
               a.DPLYEND insurance_end_time, -- 保险止期
               a.DFCD underwrite_time, --核保日期,
               a.DPAYEND payment_end_date, -- 缴费止期,
               a.DCALDTE settle_date, --  结算日期,
               a.NTERMNO term_no, -- 期数
               to_char(round(sysdate - greatest(a.DPAYEND, a.DFCD), 0)) as account_day, --账龄(日),
               case
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) <= 3 then
                  '3个月以内'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) > 3 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) <= 6 then
                  '3到6个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) > 6 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) <= 12 then
                  '6到12个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.DPAYEND,
                                                              a.DFCD)),
                                      3)) > 12 then
                  '12个月以上'
               end account_month, --账龄(月),
               a.CINSCDE plan_code, --险种
               (select t.plan_chinese_name
                  from plan_define t
                 where t.plan_code = a.CINSCDE) plan_name, --险种名称
               '' as channel_source_code, -- 渠道代码
               '' as channel_name, --渠道名称,
               '' as client_attribute, --客户类型编码
               '' as client_name, --客户类型名称
               (select agent_chinese_name
                  from agent_define t
                 where t.agent_code = a.CAGTCDE) agent_name, --代理人名称,
               a.CCURNO currency_code, --币种代码
               (select currency_chinese_name
                  from currency_define
                 where currency_code = a.CCURNO) currency_name --币种名称,
                         -----------增加业务员表--------------
          from kcoldue a,epcisbase.sas_employee b
         where a.cempcde=b.employee_code---------增加关联关系------------
           and a.cdno like (p_department_code)
           and a.CACCNO is null --应收凭证号
           and a.ACTUAL_VOUCHER_NO is null -- 实收凭证号
           and greatest(a.DPLYSTR, a.DFCD) >=
               add_months(trunc(sysdate, 'mm'), 1)
------------------------------增加代码begin-------------------------------------------------------------------------
           and round(sysdate - greatest(a.DPAYEND, a.DFCD), 0)>=
           (select se.use_age
                  from sas_due_supvz_set se
                 where se.effective_date < sysdate + 1
                   and (se.invalidate_date is null or
                       se.invalidate_date > sysdate)
                   and se.channel_code = b.employee_channel --case update by chenqinghai
                 /*nvl(b.employee_channel,  b.employee_type) WHEN 'A' THEN 'DS' WHEN 'DS' THEN 'DS' WHEN 'I' THEN 'AS' WHEN 'AS' THEN 'AS' WHEN 'ES' THEN 'AS' WHEN 'F' THEN 'CS' WHEN 'CS' THEN 'CS' WHEN 'V' THEN 'BS' WHEN 'BS' THEN 'BS' WHEN 'FS' THEN 'FS' WHEN 'L' THEN 'IS' WHEN 'IS' THEN 'IS' WHEN 'G' THEN 'HS' WHEN 'H' THEN 'HS' WHEN 'HS' THEN 'HS' WHEN 'GS' THEN 'GS' WHEN 'JS' THEN 'JS' ELSE 'X' END*/  and se.department_code = (select substr(emp.department_code,
                                                                                                                                                                                                                                                                                                                                                                                                                   1,
                                                                                                                                                                                                                                                                                                                                                                                                                   3)
                                                                                                                                                                                                                                                                                                                                                                                                       from epcisbase.sas_employee emp
                                                                                                                                                                                                                                                                                                                                                                                                      where emp.employee_code =
                                                                                                                                                                                                                                                                                                                                                                                                            a.cempcde
                                                                                                                                                                                                                                                                                                                                                                                                        and rownum = 1) and se.client_code = decode(DECODE(SUBSTR(a.c_magic_set, 2, 1),'2', '0',SUBSTR(a.c_magic_set, 2, 1)), '1', '1', '2'))
------------------------------增加代码end-------------------------------------------------------------------------
        union all
        select a.receipt_no,
               a.finance_department_code, -- 机构代码,
               (select description
                  from institutions t
                 where t.flex_value = a.finance_department_code) finance_department_name, --机构名称,
               a.group_code, --团队代码
               (select cgrpcnm from kgrpcde where cgrpcde = a.group_code) group_name, --团队名称
               a.sale_agent_code, -- 业务员
               (select employee_name
                  from epcisbase.sas_employee
                 where employee_code = a.sale_agent_code) sale_agent_name, --业务员名称,
               a.policy_no, --保单号,
               a.endorse_no, -- 批单号,
               a.insured_name, -- 被保险人,
               b.premium_amount, -- 金额,(保费金额)
               --最后预收凭证号,
               --最后预收凭证日期,
               --a.precol_amount, -- 预收金额,
               a.insurance_begin_time, -- 保险起期,
               a.insurance_end_time, -- 保险止期,
               a.underwrite_time, -- 核保日期,
               a.payment_end_date, -- 缴费止期,
               a.settle_date, -- 结算日期,
               a.term_no, -- 期数,
               to_char(round(sysdate -
                             greatest(a.payment_end_date, a.underwrite_time),
                             0)) as account_day, --账龄(日),
               case
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) <= 3 then
                  '3个月以内'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) > 3 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) <= 6 then
                  '3到6个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) > 6 and
                      to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) <= 12 then
                  '6到12个月'
                 when to_number(round(months_between(sysdate,
                                                     greatest(a.payment_end_date,
                                                              a.underwrite_time)),
                                      3)) > 12 then
                  '12个月以上'
               end account_month, --账龄(月),
               b.plan_code, --险种
               (select t.plan_chinese_name
                  from plan_define t
                 where t.plan_code = b.plan_code) plan_name, --险种名称
               a.channel_source_code, -- 渠道代码
               (select bnocnm
                  from business_source
                 where bno = a.channel_source_code) channel_name, --渠道名称,
               -- a.client_attribute, --客户类型编码
               decode(a.client_attribute, '0', '2', a.client_attribute) client_attribute, --客户类型编码
               decode(a.client_attribute, '1', '个体', '团体') client_name, --客户类型名称,
               (select agent_chinese_name
                  from agent_define t
                 where t.agent_code = a.agent_code) agent_name, --代理人名称,
               a.currency_code, --币种代码
               (select currency_chinese_name
                  from currency_define
                 where currency_code = a.currency_code) currency_name --币种名称,
                                              -------增加业务员表--------------
          from premium_info a, premium_plan b,epcisbase.sas_employee c
         where a.sale_agent_code=c.employee_code--------增加关联关系-----------
           and a.finance_department_code like (p_department_code)
           and a.due_voucher_no is null --应收凭证号
           and a.actual_voucher_no is null -- 实收凭证号
           and greatest(a.insurance_begin_time, a.underwrite_time) >=
               add_months(trunc(sysdate, 'mm'), 1)
           and nvl(a.disable_flag, 'N') <> 'Y' --是否失效
           and a.receipt_no = b.receipt_no
------------------------------增加代码begin-------------------------------------------------------------------------
           and round(sysdate -greatest(a.payment_end_date, a.underwrite_time),0)>=
             (select se.use_age
                  from sas_due_supvz_set se
                 where se.effective_date < sysdate + 1
                   and (se.invalidate_date is null or
                       se.invalidate_date > sysdate)
                   and se.channel_code = c.employee_channel --case update by chenqinghai
                 /*nvl(c.employee_channel,  c.employee_type) WHEN 'A' THEN 'DS' WHEN 'DS' THEN 'DS' WHEN 'I' THEN 'AS' WHEN 'AS' THEN 'AS' WHEN 'ES' THEN 'AS' WHEN 'F' THEN 'CS' WHEN 'CS' THEN 'CS' WHEN 'V' THEN 'BS' WHEN 'BS' THEN 'BS' WHEN 'FS' THEN 'FS' WHEN 'L' THEN 'IS' WHEN 'IS' THEN 'IS' WHEN 'G' THEN 'HS' WHEN 'H' THEN 'HS' WHEN 'HS' THEN 'HS' WHEN 'GS' THEN 'GS' WHEN 'JS' THEN 'JS' ELSE 'X' END*/  and se.department_code = (select substr(emp.department_code,
                                                                                                                                                                                                                                                                                                                                                                                                                   1,
                                                                                                                                                                                                                                                                                                                                                                                                                   3)
                                                                                                                                                                                                                                                                                                                                                                                                       from epcisbase.sas_employee emp
                                                                                                                                                                                                                                                                                                                                                                                                      where emp.employee_code =
                                                                                                                                                                                                                                                                                                                                                                                                           a.sale_agent_code
                                                                                                                                                                                                                                                                                                                                                                                                        and rownum = 1) and se.client_code = decode(a.client_attribute, '1', '1', '2'));
------------------------------增加代码end-------------------------------------------------------------------------

      --v_foresee c_foresee%rowtype;
    begin

      --报表查询记录条数限制,直接控制到excel最大接受值
      --  counterlimit := pkg_ereport.getcountlimit;
      counterlimit := 65000;

      --分解通过reportnet提示页面获取的参数信息
      p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                         'userName_epcis'); --执行用户
      p_department_code := pkg_ereport.getParameterValue(parameters,
                                                         'financeDepartments'); --机构
      p_plan_class_code := pkg_ereport.getparametervalue(parameters,
                                                         'planClass_code_epcis'); --险种大类
      p_plan_code       := pkg_ereport.getparametervalue(parameters,
                                                         'plan_code_epcis'); --险种
      p_emp_code        := pkg_ereport.getparametervalue(parameters,
                                                         'saleAgent_code'); --业务员
      p_sale_group      := pkg_ereport.getparametervalue(parameters,
                                                         'saleGroup_code_epcis'); --团队
      p_client_type     := pkg_ereport.getparametervalue(parameters,
                                                         'businessType_epcis'); --客户类型
      p_sale_channel    := pkg_ereport.getparametervalue(parameters,
                                                         'channel_code_epcis'); --渠道
      p_currency_code   := pkg_ereport.getparametervalue(parameters,
                                                         'currency_code_epcis'); --币种
      p_rateh           := pkg_ereport.getparametervalue(parameters,
                                                         'rateH_epcis'); --港币汇率值
      p_rateu           := pkg_ereport.getparametervalue(parameters,
                                                         'rateU_epcis'); --美元汇率值
      v_user_loginId    := p_user_code || '-' ||
                           to_char(sysdate, 'yyyymmdd hh24miss'); --根据当前时间组成此次会话的用户id

      --财务机构如果是汇总机构则做相应处理
      if p_department_code = substr(p_department_code, 1, 2) || '9999' then
        p_department_code := substr(p_department_code, 1, 2) || '%';
      elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
        p_department_code := substr(p_department_code, 1, 4) || '%';
      end if;

      --若用户没有在提示页面输入汇率，则从系统表中取汇率值
      if p_rateh is null then
        v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
      else
        v_rateh := to_number(p_rateh);
      end if;

      if p_rateu is null then
        v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
      else
        v_rateu := to_number(p_rateu);
      end if;

      --游标开始循环取数据
      for v_foresee in c_foresee loop

        --过滤险种大类
        if p_plan_class_code is not null then
          select count(*)
            into v_count
            from ereport_plan_map_info b, plan_define c
           where c.plan_code = b.plan_code
             and b.ereport_plan_class_code = p_plan_class_code
             and b.plan_code = v_foresee.plan_code;

          if v_count = 0 then
            goto begin_loop;
          end if;
        end if;

        --过滤险种
        if p_plan_code is not null then
          if p_plan_code != v_foresee.plan_code then
            goto begin_loop;
          end if;
        end if;

        --过滤业务员
        if p_emp_code is not null then
          if p_emp_code != v_foresee.sale_agent_code then
            goto begin_loop;
          end if;
        end if;

        --过滤团队
        if p_sale_group is not null then
          if p_sale_group != v_foresee.group_code then
            goto begin_loop;
          end if;
        end if;

        --过滤客户类型
        if p_client_type is not null then
          if p_client_type != v_foresee.client_attribute then
            goto begin_loop;
          end if;
        end if;
        --过滤渠道
        if p_sale_channel is not null then
          if p_sale_channel != v_foresee.channel_source_code then
            goto begin_loop;
          end if;
        end if;
        --过滤币种
        if p_currency_code is not null then
          if p_currency_code != v_foresee.currency_code then
            goto begin_loop;
          end if;
        end if;

        --从老表中取预收数据
        begin
          select sum(due_amount), --预收金额
                 max(voucher_no), --预收凭证号最后一次
                 max(voucher_date) --预收凭证日期 最后一次
            into v_precol_amount, v_voucher_no, v_voucher_craeted_date
            from (select t.cvouno_precol   voucher_no, --预收凭证号
                         t.dcoldte         voucher_date, --预收凭证日期
                         t6.premium_amount due_amount --预收金额
                    from kprecol t, premium_info t1, premium_plan t6
                   where t.cplyno = t1.policy_no
                     and t.cedrno = t1.endorse_no
                     and t.ntermno = t1.term_no
                     and t.cvouno_precol is not null --已经生成预收凭证
                     and t6.receipt_no = t1.receipt_no
                     and t.cinscde <> 'A24'
                     and t.cdno like (v_foresee.finance_department_code)
                     and t1.policy_no = v_foresee.policy_no
                     and t.cplyno = v_foresee.policy_no
                     and nvl(t.cedrno, 'a') = nvl(v_foresee.endorse_no, 'a')
                     and nvl(t1.endorse_no, 'a') =
                         nvl(v_foresee.endorse_no, 'a')
                  union all
                  -- 从新表中取数据保单
                  --取2121000000 科目（02 产险保费  0A 意健险保费  14 产险退保费 1E 意健险退保费），
                  select t3.voucher_no voucher_no, --预收凭证号
                         t3.voucher_date voucher_craeted_date, --预收凭证日期
                         decode(t2.collect_pay_item,
                                '14',
                                -amount,
                                '1E',
                                -amount,
                                amount) precol_amount
                  --t6.premium_amount precol_amount --预收金额
                    from collection_notice         t3,
                         collect_pay_info          t2,
                         business_receipt_relation t4,
                         premium_info              t1,
                         premium_plan              t6
                   where t3.notice_status = '81'
                     and t3.voucher_no is not null --预收凭证号
                     and t3.notice_no = t2.notice_no
                     and t2.collect_pay_item in ('02', '0A', '14', '1E')
                     and t2.collect_pay_no = t4.collect_pay_no
                     and t1.receipt_no = t4.receipt_no
                        --           and t4.policy_no = t1.policy_no
                        --           and nvl(t4.endorse_no, 'a') = nvl(t1.endorse_no, 'a')
                        --           and t4.term_no = t1.term_no
                     and t1.receipt_no = t6.receipt_no
                     and t6.plan_code <> 'A24'
                     and t3.account_department_code like
                         (v_foresee.finance_department_code)
                     and t1.policy_no = v_foresee.policy_no
                     and nvl(t1.endorse_no, 'a') =
                         nvl(v_foresee.endorse_no, 'a')
                  union all
                  --从支付通知单取退保的预收
                  select t3.voucher_no voucher_no, --预收凭证号
                         t3.voucher_date voucher_craeted_date, --预收凭证日期
                         decode(t2.collect_pay_item,
                                '14',
                                -amount,
                                '1E',
                                -amount,
                                amount) precol_amount
                  --t6.premium_amount precol_amount --预收金额
                    from payment_notice            t3,
                         collect_pay_info          t2,
                         business_receipt_relation t4,
                         premium_info              t1,
                         premium_plan              t6
                   where t3.notice_status = '81'
                     and t3.voucher_no is not null --预收凭证号
                     and t3.notice_no = t2.notice_no
                     and t2.collect_pay_item in ('02', '0A', '14', '1E')
                     and t2.collect_pay_no = t4.collect_pay_no
                     and t1.receipt_no = t4.receipt_no
                        --                     and t4.policy_no = t1.policy_no
                        --                     and nvl(t4.endorse_no, 'a') = nvl(t1.endorse_no, 'a')
                        --                     and t4.term_no = t1.term_no
                     and t1.receipt_no = t6.receipt_no
                     and t6.plan_code <> 'A24'
                     and t3.account_department_code like
                         (v_foresee.finance_department_code)
                     and t1.policy_no = v_foresee.policy_no
                     and nvl(t1.endorse_no, 'a') =
                         nvl(v_foresee.endorse_no, 'a'));
        exception
          when no_data_found then
            v_precol_amount        := 0;
            v_voucher_no           := null;
            v_voucher_craeted_date := null;
            --该笔应收没有对应的全额预收。全额预收的解释：该笔应收没有收钱即系统里没有预收
            --或者该笔应收收到的钱小于应收金额即系统里的预收金额之和小于应收金额
            goto begin_loop;
          when others then
            v_precol_amount        := 0;
            v_voucher_no           := null;
            v_voucher_craeted_date := null;
            --该笔应收没有对应的全额预收。全额预收的解释：该笔应收没有收钱即系统里没有预收
            --或者该笔应收收到的钱小于应收金额即系统里的预收金额之和小于应收金额
            goto begin_loop;
        end;

        --“系统中该笔应收金额>预收金额之和”这以保单，批单来算 ?,取以保批单汇总金额的应收金额
        --该笔应收没有对应的全额预收。全额预收的解释：该笔应收没有收钱即系统里没有预收
        --或者该笔应收收到的钱小于应收金额即系统里的预收金额之和小于应收金额
        begin
          select sum(b.premium_amount)
            into v_premium_amount
            from premium_info a, premium_plan b
           where a.finance_department_code like (p_department_code)
             and a.due_voucher_no is null --应收凭证号
             and a.actual_voucher_no is null -- 实收凭证号
             and greatest(a.insurance_begin_time, a.underwrite_time) >=
                 add_months(trunc(sysdate, 'mm'), 1)
             and nvl(a.disable_flag, 'N') <> 'Y' --是否失效
             and a.receipt_no = b.receipt_no
             and a.policy_no = v_foresee.policy_no
             and nvl(a.endorse_no, 'a') = nvl(v_foresee.endorse_no, 'a');
        exception
          when no_data_found then
            v_premium_amount := 0;
          when others then
            v_premium_amount := 0;
        end;

        if v_premium_amount > 0 and v_premium_amount <= v_precol_amount then
          goto begin_loop;
        elsif v_premium_amount < 0 and v_premium_amount >= v_precol_amount then
          goto begin_loop;
        end if;

        --将数据写入临时表
        insert into EPCISACCT.tmp_finance_duepremium_short
          (department_code,
           department_name,
           sale_group,
           emp_name,
           policy_no,
           endorse_no,
           insured_person,
           due_premium,
           due_voucher_no,
           due_voucher_date,
           due_premium_sum, --预收替代字段
           insurance_begin_date,
           insurance_end_date,
           underwrite_time,
           payment_end_date,
           settle_date,
           notice_no,
           account_days,
           account_month,
           plan_name,
           sale_channel,
           client_name,
           agent_chinese_name,
           currency,
           user_loginid)
        values
          (v_foresee.finance_department_code, -- 机构代码,
           v_foresee.finance_department_name, --机构名称,
           v_foresee.group_name, --团队名称
           v_foresee.sale_agent_name, --业务员名称,
           v_foresee.policy_no, --保单号,
           v_foresee.endorse_no, -- 批单号,
           v_foresee.insured_name, -- 被保险人,
           v_foresee.premium_amount, -- 金额,
           v_voucher_no, --最后预收凭证号,
           v_voucher_craeted_date, --最后预收凭证日期,
           v_precol_amount, -- 预收金额,
           v_foresee.insurance_begin_time, -- 保险起期,
           v_foresee.insurance_end_time, -- 保险止期,
           v_foresee.underwrite_time, -- 核保日期,
           v_foresee.payment_end_date, -- 缴费止期,
           v_foresee.settle_date, -- 结算日期,
           v_foresee.term_no, -- 期数,
           v_foresee.account_day, --账龄(日),
           v_foresee.account_month, --账龄(月),
           v_foresee.plan_name, --险种名称
           v_foresee.channel_name, --渠道名称,
           v_foresee.client_name, --客户类型名称,
           v_foresee.agent_name, --代理人名称,
           v_foresee.currency_name, --币种名称
           v_foresee.currency_code --币种代码
           );

        v_count1 := v_count1 + 1;
        if v_count1 >= counterlimit then
          goto end_loop;
        end if;

        --下一循环，放在这里
        <<begin_loop>>
        null;

      end loop;

      <<end_loop>>

      --返回结果集
      open v_cursor for
        select department_code 机构代码,
               department_name 机构名称,
               sale_group 团队代码,
               emp_name 业务员名称,
               policy_no 保单号,
               endorse_no 批单号,
               insured_person 被保人,
               due_premium 应收保费,
               due_voucher_no 应收凭证号,
               due_voucher_date 应收凭证日期,
               due_premium_sum 预收金额,
               insurance_begin_date 保险起期,
               insurance_end_date 保险止期,
               underwrite_time 核保日期,
               payment_end_date 缴费止期,
               settle_date 结算日期,
               notice_no 通知单号,
               account_days 账龄日,
               account_month 账龄月,
               plan_name 险种,
               sale_channel 销售渠道,
               client_name 客户名称,
               agent_chinese_name 代理人名称,
               currency 币种,
               decode(user_loginid,
                      '02',
                      due_premium * v_rateh,
                      '03',
                      due_premium * v_rateu,
                      due_premium) 折合人民币
          from EPCISACCT.tmp_finance_duepremium_short
         order by policy_no, endorse_no;

      Re_Cursor := v_cursor;

    exception
      when data_not_prepared then
        v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
        raise_application_error(-20001, v_ErrMsg);
      when others then
        v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
        raise_application_error(-20001, v_ErrMsg);
        rollback;
    end pro_finance_notcol_foresee;

    ----------------------------------------------------------------
    --创建：远期未全额预收停单 ex-zhongyibiao001 2010-06-01
    --需求见：[SR_PA01084958—应收清单（月结）]说明书
    ----------------------------------------------------------------
      --应用清单长期表(月结)
procedure pro_finance_due_balance(parameters in varchar2,
                                  re_cursor  out t_cursor) as

  v_cursor t_cursor; --返回值游标

  p_department_code   varchar2(10); --机构编码
  p_plan_class_code   varchar2(10); --险种大类代码
  p_plan_code         VARCHAR2(11); --险种代码
  p_group_code        VARCHAR2(11); --团队代码
  p_saler_code        VARCHAR2(10); --业务员
  p_client_attribute  varchar2(2); --客户类型
  p_channel_five_code varchar2(10); --渠道
  p_currency_code     varchar2(2); --币种
  p_user_code         varchar2(20); --执行用户
  p_month             varchar2(10); --期间
  p_approach          varchar2(1);   --1-监管口径 2-考核口径
  p_group_include     varchar2(1);   --包含/不包含 团队
  p_agent_include     varchar2(1);   --包含/不包含 业务员
  p_channel_include   varchar2(1);   --包含/不包含 渠道

  --v_plan_class_code varchar2(3);--险种大类的具体值
  --v_plan_class      varchar2(3);--险种大类,用作和传入的参数比较

  --记录统计变量n
  v_count_limit number(10);
  v_count       number default 0;

  --用户登录参数
  v_user_loginid varchar2(100);

  v_channel_five_code           varchar2(10); --渠道代码,用作和传入的参数比较
  v_channel_eight_code          Varchar2(10);

  V_POLICY_NO_LOG       finance_due_balance_tmp.POLICY_NO%TYPE; --2011-06-10 add by ex-zengjiu001 记入异常日志
  v_finance_depart_name varchar2(240); --add by ex-hejin001 20120702 财务机构名称
  v_special_code  varchar2(50);
  --查询游标
  cursor cur_finance_due_balance is
    select h.finance_department_code, --机构编码
           h.department_code, --业务机构代码
           h.department_name, --机构名称
           h.policy_no, --保单号
           '' as begin_date, --起止日期
           '' as end_date, --截止日期
           h.endorse_no, --批单号
           h.notice_no, --交费期别(期次)
           h.due_premium, --预收金额
           h.insurance_begin_date, --保险起期
           h.insurance_end_date, --保险止期
           h.insured_person, --被保险人名称
           h.due_voucher_no, --应收凭证号
           h.due_voucher_date, --应收制证日期
           h.plan_code, --险种
           replace(t.plan_description, '汇总', '') as plan_class_code, --险种大类 2010-9-6 ex-liukailin001 add
           --plan_class.plan_class_code as plan_class_code, --险种大类 2010-9-6 ex-liukailin001 add
           nvl(h.plan_name,
               (select a.pro_descr
                  from produce_map a
                 where a.c_ins_no = h.plan_code
                   and rownum = 1)) as plan_name, --险种名称
           h.currency_code, --币种
           decode(h.currency_code,
                  '02',
                  pkg_general_tools.get_exchange_rate('02', '01', sysdate),
                  '03',
                  pkg_general_tools.get_exchange_rate('03', '01', sysdate),
                  1) as exchange_rate, --汇率
           h.currency_name, --币种名称
           h.client_code, --客户性质 0 团体 1 个人
           h.client_name, --客户性质名称
           h.sale_agent_code, --业务员代码
           h.sale_agent_name, --业务员名称
           h.sale_channel_code, --渠道
           h.sale_channel_name, --渠道名称
           h.group_code, --团队代码
           h.group_name, --团队名称
           h.due_premium_sum, --人民币折算
           h.cancel_flag, --失效标志
           h.underwrite_time, --承保时间 如果是批单，则记录批改核保时间
           h.settle_date, --结算日期
           h.receipt_no, --收据号
           h.agent_code, --代理人码
           nvl(h.agent_chinese_name,
               (select a.broker_chn_name
                  from broker_info a
                 where a.broker_code in
                       (select b.broker_code
                          from commission_info b
                         where b.policy_no = h.policy_no)
                   and rownum = 1)) as agent_chinese_name, -- 代理人或经纪人,
           sign(Sum(h.due_premium) Over(Partition By h.policy_no)) as receivable_type, -- 应收保费类型
           h.business_source_code, --业务来源代码
           h.business_source_detail_code, --业务来源细分编码
           h.channel_source_code, --渠道编码
           h.channel_source_detail_code, --渠道细分编码
           h.payment_end_date, --缴费止期
           h.account_days, --日账龄
           case
             when nvl(months_between(h.run_time,
                                     trunc(greatest(underwrite_time,
                                                    payment_end_date))),
                      0) > 12 then
              '12个月以上'
             when nvl(months_between(h.run_time,
                                     trunc(greatest(underwrite_time,
                                                    payment_end_date))),
                      0) > 6 then
              '6到12个月'
             when nvl(months_between(h.run_time,
                                     trunc(greatest(underwrite_time,
                                                    payment_end_date))),
                      0) > 3 then
              '3到6个月'
             else
              '3个月以内'
           end as account_month,
           nvl((select fd.applicant_name
                 from tmp_finance_duepremium_long_n fd
                where fd.receipt_no = h.receipt_no
                  and fd.plan_code = h.plan_code
                  and rownum = 1),
               (select o.client_name
                  from epcisacct.opera_type_temp o
                 where o.policy_no = h.policy_no
                   and nvl(o.opera_type,'null') not in ('0M', '0N', '0O','1K','1L','1M')
                   and rownum = 1)) applicant_name,
           h.tax_amount tax_amount,
           h.total_amount total_amount
      from his_finance_duepremium_long_n h, --2010-9-6 ex-liukailin001 add begin
           (select plan_code, plan_class, decode(plan_class, 'A', '车险', 'B', '财产险', 'C', '意健险', '') plan_description
              from (Select Distinct t.c_ins_no plan_code,
                                    decode(t.plan_class,
                                           '01',
                                           'A',
                                           '02',
                                           'B',
                                           '03',
                                           'C',
                                           'B') plan_class
                      From produce_map t)
             where plan_class = p_plan_class_code
                or p_plan_class_code is null
               and p_approach = '2'
         union
         select plan_code, plan_class, plan_description from
         (select distinct p.c_ins_no plan_code, f.plan_code plan_class, f.plan_description
          from finance_plan_relation fr, finance_plan_class f, produce_map p
         where fr.parent_plan_code = f.plan_code
           and p.produce = fr.child_plan_code) t where p_approach = '1' and  ((t.plan_class = p_plan_class_code and p_plan_class_code is not null)
           or (p_plan_class_code is  null and t.plan_class in ('099998', '099997', '900002', '900009', '900001', '900008', '900005', '900011',
             '900007', '900003', '900010', '900004', '900006')))) t
    --2010-9-6 ex-liukailin001 add end
    --where h.finance_department_code like (p_department_code) 2010-9-6 ex-liukailin001 modify begin
     where h.plan_code = t.plan_code(+)
       and t.plan_code is not null
--       and h.finance_department_code like (p_department_code)
       and ((h.finance_department_code like (p_department_code) and not exists --原来归属机构剔除部分特殊机构，如青岛（119999）需要剔除青岛烟台支公司（111500）
            (select 1
                from nps_paramter_define n
               where n.class_code = 'spec2ndFinanceDept'
                 and n.department_code = h.finance_department_code
                 and n.department_code like (decode(length(p_department_code),6,'null',1,'null',p_department_code))))
            or
             (exists (select 1
                         from nps_paramter_define np --特殊二级机构需要，如青岛部分机构(11xxxx)归属山东（369999）了，也需要取出来
                        where np.class_code = 'spec2ndFinanceDept'
                          and np.department_code = h.finance_department_code
                          and np.value_code = v_special_code)))
          --2010-9-6 ex-liukailin001 modify end
       and h.run_time = last_day(to_date(p_month, 'yyyy-mm')) /* and
              greatest(insurance_begin_date,underwrite_time) between to_date('19990101', 'yyyymmdd') and
               last_day(trunc(sysdate, 'dd')) + 1 and
              h.plan_code <> 'A24'*/
       and (p_group_code is null or p_group_code is not null and (h.group_code = p_group_code and p_group_include is null
         or h.group_code <> p_group_code and p_group_include = '0'))
       and (p_client_attribute is null or (p_client_attribute = '0' and h.client_code in ('0','2')) or (p_client_attribute = '1' and h.client_code in ('1') ))
       and (p_currency_code is null or p_currency_code is not null and p_currency_code = h.currency_code)
       and (p_saler_code is null or p_saler_code is not null and (h.sale_agent_code = p_saler_code and p_agent_include is null
         or h.sale_agent_code <> p_saler_code and p_agent_include = '0'))
       and (p_plan_code is null or p_plan_code is not null and h.plan_code = p_plan_code)
    ;
  v_finance_due_balance cur_finance_due_balance%rowtype;
begin

  --根据报表运行的当前时间获取记录条数限制值：8 － 20 点之间，5001（新老系统各5001），夜间，为null
  --应用户要求，白天实收短期表放宽直接设定为10001
  if to_char(sysdate, 'hh24') > 7 and to_char(sysdate, 'hh24') < 21 then
    v_count_limit := 65000;
  else
    --v_count_limit := 200000; --晚上开放至20万
    v_count_limit := 500000; --晚上开放至50万
  end if;

  --分解通过reportnet提示页面获取的参数信息
  p_user_code         := pkg_ereport.getParametervalue(parameters,
                                                       'um_code');
  p_department_code   := pkg_ereport.getParameterValue(parameters,
                                                       'financeDepartments'); --机构
  p_plan_class_code   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'plan_class_code'); --险种大类
  p_plan_code         := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'plan_code'); --险种
  p_saler_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'sale_agent_code'); --业务员
  p_group_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'group_code'); --团队
  p_client_attribute  := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'client_attribute'); --客户类型
  p_channel_five_code := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'sale_channel_code'); --渠道
  p_currency_code     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'currency_code'); --币种
  p_month             := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'p_month'); --期间
  p_month             := trim(p_month); --去掉前后空格
  p_approach          := '2';/*pkg_ereport.getparametervalue(PARAMETERS,
                                                       'approach');*/
  p_group_include     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'group_include');
  p_agent_include     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'agent_include');
  p_channel_include   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'channel_include');
 v_special_code := p_department_code;
  --财务机构如果是汇总机构则做相应处理
  if p_department_code = substr(p_department_code, 1, 2) || '9999' then
    p_department_code := substr(p_department_code, 1, 2) || '%';
  elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
    p_department_code := substr(p_department_code, 1, 4) || '%';
  end if;

  --根据当前时间组成此次会话的用户id
  v_user_loginId := p_user_code || '-' ||
                    to_char(sysdate, 'yyyymmdd hh24miss');
  v_count        := 0;
  open cur_finance_due_balance;
  loop
    <<l_continue_fetch>>
    fetch cur_finance_due_balance
      into v_finance_due_balance;
    exit when cur_finance_due_balance%notfound;

    --过滤险种
    --if p_plan_class_code is not null then
      /* --2010-9-6 ex-liukailin001 险种大类提前到外层SQL处理 delete begin
      begin
        v_plan_class_code := '';
        v_plan_class := '';

        --如果是意健险险种，则用标准为1类的险种设置信息，否则用标准为2类的险种设置信息
        if p_plan_class_code = 'C' then
          select PLAN_CLASS_CODE
            into v_plan_class_code
            from plan_class_relation
           where plan_class_code in
                 (select p.plan_class_code
                    from plan_class p
                   where p.plan_class_standard = 1)
             and plan_code = v_finance_due_balance.plan_code
             and rownum = 1;
        else
          select PLAN_CLASS_CODE
            into v_plan_class_code
            from plan_class_relation
           where plan_class_code in
                 (select p.plan_class_code
                    from plan_class p
                   where p.plan_class_standard = 2)
             and plan_code = v_finance_due_balance.plan_code
             and rownum = 1;
        end if;

        if v_plan_class_code = 'SC' then
           v_plan_class := 'A';
        elsif (v_plan_class_code = 'J' or v_plan_class_code = 'K')  then
           v_plan_class := 'C';
        else
            v_plan_class := 'B';
        end if;

        exception
        when others then
           v_plan_class_code := '';
           v_plan_class := '';
      end;
      */  --2010-9-6 ex-liukailin001 delete end
      --过滤险种大类
      --if p_plan_class_code <> v_plan_class then --2010-9-6 ex-liukailin modify
      /*begin
        select f.plan_code
          into v_plan_class
          from finance_plan_relation fr, finance_plan_class f, produce_map p
         where fr.parent_plan_code = f.plan_code
           and f.plan_type = '02'
           and p.produce = fr.child_plan_code
           and p.c_ins_no = v_finance_due_balance.plan_code
           AND ROWNUM = 1;
      exception
        when others then
          v_plan_class       := '';
      end;

      if p_approach = '1' then
        if p_plan_class_code is null then
          if nvl(v_plan_class, 'null') not in ('099998', '099997', '900002', '900009', '900001', '900008', '900005', '900011',
             '900007', '900003', '900010', '900004', '900006') then
            goto l_continue_fetch;
          end if;
        else
          if p_plan_class_code <> nvl(v_plan_class, 'null') then
            goto l_continue_fetch;
          end if;
        end if;
      elsif p_approach = '2' then
        if p_plan_class_code is null then
          if nvl(v_plan_class, 'null') not in ('991000', '991100', '990100', '990700', '990400', '990500', '990800', '990200',
             '991300', '990300', '990900', '990600', '991200') then
            goto l_continue_fetch;
          end if;
        else
          if p_plan_class_code <> nvl(v_plan_class, 'null') then
            goto l_continue_fetch;
          end if;
        end if;
      end if;*/

      --过滤险种明细
      /*if p_plan_code is not null then
        if p_plan_code <> nvl(v_finance_due_balance.plan_code, 'null') then
          goto l_continue_fetch;
        end if;
      end if;*/
    --end if;

    --过滤业务员
    if p_saler_code is not null then
      if p_agent_include = '0' then
        if p_saler_code = nvl(v_finance_due_balance.sale_agent_code, 'null') then
          goto l_continue_fetch;
        end if;
      else
        if p_saler_code <> nvl(v_finance_due_balance.sale_agent_code, 'null') then
          goto l_continue_fetch;
        end if;
      end if;
    end if;
    --过滤团队
    /*if p_group_code is not null then
      if p_group_include = '0' then
        if p_group_code = nvl(v_finance_due_balance.group_code, 'null') then
          goto l_continue_fetch;
        end if;
      else
        if p_group_code <> nvl(v_finance_due_balance.group_code, 'null') then
          goto l_continue_fetch;
        end if;
      end if;
    end if;*/
    --过滤客户
    /*if p_client_attribute is not null then
      if p_client_attribute <>
         nvl(v_finance_due_balance.client_code, 'null') then
        goto l_continue_fetch;
      end if;
    end if;*/

    --过滤币种
    /*if p_currency_code is not null then
      if p_currency_code <>
         nvl(v_finance_due_balance.currency_code, 'null') then
        goto l_continue_fetch;
      end if;
    end if;*/

    --过滤渠道  按照5大渠道直接过滤，5大渠道包括：混合渠道 车行渠道 综合开拓 新渠道 重点客户
    begin
      SELECT b.value_chinese_name, a.channel_eight_role_code
        INTO v_channel_five_code, v_channel_eight_code
        FROM channel_info a, common_parameter b
       WHERE a.channel_eight_role_code = b.value_code
         AND business_source_code =
             v_finance_due_balance.business_source_code
         AND business_source_detail_code =
             v_finance_due_balance.business_source_detail_code
         AND channel_source_code =
             v_finance_due_balance.channel_source_code
         AND nvl(trim(channel_source_detail_code), 'null') =
             nvl(trim(v_finance_due_balance.channel_source_detail_code),
                 'null')
         AND b.collection_code = 'QDDM03'
         AND b.department_code = '2'; -- added by ex-hejin001 20120502

    exception
      when others then
        v_channel_five_code  := '';
        v_channel_eight_code := 'Null';
    end;

    if p_channel_five_code is not null then
      if p_channel_include = '0' then
        if p_channel_five_code = v_channel_eight_code then
          goto l_continue_fetch;
        end if;
      else
        if p_channel_five_code <> v_channel_eight_code then
          goto l_continue_fetch;
        end if;
      end if;
    end if;

    V_POLICY_NO_LOG := v_finance_due_balance.policy_no;

    <<get_finance_depart_name_1>>
  /* add by ex-hejin001 20120702 取财务机构名称*/
    begin
      select a.description
        into v_finance_depart_name
        from institutions a
       where a.flex_value = v_finance_due_balance.finance_department_code;
    exception
      when others then
        v_finance_depart_name := '';
    end get_finance_depart_name_1;

    --将符合条件的记录插入到temporary_table中
    insert into finance_due_balance_tmp
      (department_code,
       department_name,
       begin_date,
       end_date,
       exchange_rate,
       policy_no,
       endorse_no,
       insured_person,
       group_name,
       sale_agent_name,
       due_premium,
       due_voucher_no,
       due_voucher_date,
       insurance_begin_date,
       insurance_end_date,
       underwrite_time,
       payment_end_date,
       settle_date,
       notice_no,
       account_month,
       account_days,
       plan_name,
       sale_channel_name,
       client_name,
       currency_name,
       due_premium_sum,
       user_loginid,
       finance_department_code, --add by ex-hejin001 20120702 财务机构代码
       finance_department_name, --add by ex-hejin001 20120702 财务机构名称
       application_name, --add by ex-hejin001 20120702 投保人
       AGENT_CHINESE_NAME, --代理人名称
       plan_class_code, --险种类别(A 车 B 财 C 意 )
       receivable_type, --应收保费类型
       tax_amount, --应收增值税额
       total_amount, --合计
       plan_code  --险种
       )
    values
      (v_finance_due_balance.department_code,
       v_finance_due_balance.department_name,
       v_finance_due_balance.begin_date,
       v_finance_due_balance.end_date,
       v_finance_due_balance.exchange_rate,
       v_finance_due_balance.policy_no,
       v_finance_due_balance.endorse_no,
       v_finance_due_balance.insured_person,
       v_finance_due_balance.group_name,
       v_finance_due_balance.sale_agent_name,
       v_finance_due_balance.due_premium,
       v_finance_due_balance.due_voucher_no,
       v_finance_due_balance.due_voucher_date,
       v_finance_due_balance.insurance_begin_date,
       v_finance_due_balance.insurance_end_date,
       v_finance_due_balance.underwrite_time,
       v_finance_due_balance.payment_end_date,
       v_finance_due_balance.settle_date,
       v_finance_due_balance.notice_no,
       v_finance_due_balance.account_month,
       v_finance_due_balance.account_days,
       v_finance_due_balance.plan_name,
       v_channel_five_code,
       v_finance_due_balance.client_name,
       v_finance_due_balance.currency_name,
       v_finance_due_balance.due_premium *
       v_finance_due_balance.exchange_rate, --modify by ex-fanjianguo001 2010-11-30
       v_user_loginId,
       v_finance_due_balance.finance_department_code,
       v_finance_depart_name,
       v_finance_due_balance.applicant_name,
       v_finance_due_balance.Agent_Chinese_Name,
       /*decode(v_finance_due_balance.plan_class_code,
              'A',
              '车险',
              'B',
              '财产险',
              'C',
              '意健险',
              ''),*/
       v_finance_due_balance.plan_class_code,
       decode(v_finance_due_balance.receivable_type,
              '1',
              '正数应收',
              '-1',
              '负数应收',
              '正数应收'),
       v_finance_due_balance.tax_amount,
       v_finance_due_balance.total_amount,
       v_finance_due_balance.plan_code);
    v_count := v_count + 1;
    if v_count_limit is not null then
      if v_count >= v_count_limit then
        close cur_finance_due_balance;
        goto l_result;
      end if;
    end if;

  end loop;
  close cur_finance_due_balance;
  <<l_result>>

  open v_cursor for
    select '''' || department_code as 业务机构代码,
           department_name as 业务机构名称,
           exchange_rate as 汇率,
           '''' || policy_no as 保单号,
           '''' || endorse_no as 批单号,
           application_name as 投保人, --change position ex-zhaominzh001 2012/9/17
           insured_person as 被保险人,
           --group_name as 团队,
           sale_agent_name as 业务员,
           due_premium as 金额,
           due_voucher_no as 应收凭证号,
           due_voucher_date as 应收凭证日期,
           insurance_begin_date as 保险起期,
           insurance_end_date as 保险止期,
           underwrite_time as 核保日期,
           payment_end_date as 缴费止期,
           settle_date as 结算日期,
           notice_no as 期次,
           account_month as 账龄,
           account_days as 账龄a,
           plan_name as 险种,
           sale_channel_name as 渠道,
           client_name as 客户,
           currency_name as 币种,
           cast(due_premium_sum as number(16,2)) as 折合人民币,
           cast(a.due_premium *
           (Select e.exchange_rate
              From exchange_rate e
             Where e.currency1_code =
                   decode(a.currency_name, '港币', '02', '美元', '03', '01')
               and e.currency2_code = '01'
               and last_day(to_date(p_month, 'yyyy-mm')) >= e.effective_date
               and last_day(to_date(p_month, 'yyyy-mm')) <=
                   nvl(e.invalidate_date, to_date('9999', 'yyyy'))) as number(16,2)) "折算人民币（考核）",
           '''' || finance_department_code as 财务机构代码,
           finance_department_name as 财务机构名称,
           replace(plan_class_code, '汇总', '') as 险种大类,
           receivable_type as 应收保费类型,
           AGENT_CHINESE_NAME as 代理人或经纪人,
           a.tax_amount as 应收增值税额,
           a.tax_amount *
           (Select e.exchange_rate
              From exchange_rate e
             Where e.currency1_code =
                   decode(a.currency_name, '港币', '02', '美元', '03', '01')
               and e.currency2_code = '01'
               and last_day(to_date(p_month, 'yyyy-mm')) >= e.effective_date
               and last_day(to_date(p_month, 'yyyy-mm')) <=
                   nvl(e.invalidate_date, to_date('9999', 'yyyy'))) "应收增值税额(人民币)",
           decode(nvl(a.total_amount,0), 0, due_premium, a.total_amount) 合计,
           cast(decode(nvl(a.total_amount,0) *
                  (Select e.exchange_rate
                     From exchange_rate e
                    Where e.currency1_code = decode(a.currency_name,
                                                    '港币',
                                                    '02',
                                                    '美元',
                                                    '03',
                                                    '01')
                      and e.currency2_code = '01'
                      and last_day(to_date(p_month, 'yyyy-mm')) >=
                          e.effective_date
                      and last_day(to_date(p_month, 'yyyy-mm')) <=
                          nvl(e.invalidate_date, to_date('9999', 'yyyy'))),
                  0,
                  due_premium_sum,
                  nvl(a.total_amount,0) *
                  (Select e.exchange_rate
                     From exchange_rate e
                    Where e.currency1_code = decode(a.currency_name,
                                                    '港币',
                                                    '02',
                                                    '美元',
                                                    '03',
                                                    '01')
                      and e.currency2_code = '01'
                      and last_day(to_date(p_month, 'yyyy-mm')) >=
                          e.effective_date
                      and last_day(to_date(p_month, 'yyyy-mm')) <=
                          nvl(e.invalidate_date, to_date('9999', 'yyyy')))) as number(16,2)) "合计（人民币）",
           cast(decode(nvl(a.total_amount,0) *
                  (Select e.exchange_rate
                    From exchange_rate e
                   Where e.currency1_code = decode(a.currency_name, '港币', '02', '美元', '03', '01')
                     And e.currency2_code = '01'
                     And Sysdate >= e.effective_date
                     And Sysdate <= nvl(e.invalidate_date, to_date('9999', 'yyyy'))),
                  0,
                  due_premium_sum,
                  nvl(a.total_amount,0) *
                  (Select e.exchange_rate
                    From exchange_rate e
                   Where e.currency1_code = decode(a.currency_name, '港币', '02', '美元', '03', '01')
                     And e.currency2_code = '01'
                     And Sysdate >= e.effective_date
                     And Sysdate <= nvl(e.invalidate_date, to_date('9999', 'yyyy')))) as number(16,2)) "合计人民币金额（当期汇率）" ,
           replace((select f.plan_description from finance_plan_class f where f.plan_type='02' and f.plan_code in
             (select fr.parent_plan_code from finance_plan_relation fr where fr.child_plan_code in
             (select p.produce from produce_map p where p.c_ins_no =a.plan_code))),'汇总','') 明细险种大类
      from finance_due_balance_tmp a
     where user_loginid = v_user_loginid
       and rownum <= v_count_limit;
  re_cursor := v_cursor;

exception
  when others then
    pkg_finance_exception_log.process_exception_log(null,
                                                    V_POLICY_NO_LOG,
                                                    'pkg_ereport_finance_due_new',
                                                    'pro_finance_due_balance',
                                                    substr(sqlerrm, 1, 200));
    rollback;
end pro_finance_due_balance;

-- Author : ex-zhongyibiao001
-- Created : 2010-07-12 14:16:36
-- Purpose : 清空每天更新的临时表数据,在每天插入数据前情况，由 ETL 调用执行
procedure pro_truncate_foresee_everyday is
v_count number;
--v_sql Varchar(2000);
begin
  --清空每天更新的临时表数据,此清空数据的操作必须在早上8:30前执行，否则会报错
  begin
      --epcisrpt_ddl_prc.truncate_table('FIN_NOTCOL_FORESEE_EVERYDAY');
      appmgr.pkg_truncate.truncate_table('EPCISACCT','FIN_NOTCOL_FORESEE_EVERYDAY', null);
      --v_sql := 'begin appmgr.pkg_truncate.truncate_table(''EPCISACCT'',''FIN_NOTCOL_FORESEE_EVERYDAY'', null); end;';
      --Execute Immediate v_sql;
  exception
      when others then
          for i in 1 .. 1000 loop
              delete from fin_notcol_foresee_everyday
               where rownum < 20001;
              v_count := sql%rowcount;
              commit;
              exit when v_count = 0;
          end loop;
  end;
  insert into epcis_job_log
  values
    (sysdate, 0, 0, 0, '暂无', 'pro_truncate_table执行完成',sys_guid());
  commit;
end pro_truncate_foresee_everyday;

-- Author : ex-zhongyibiao001
-- Created : 2010-07-07 15:18:36
-- Purpose :  远期未全额预收数据
procedure pro_finance_foresee_long is

  v_ErrMsg     varchar2(500);
  v_ErrCodeVal number := 0;
  i_count      number;
  v_count      number;

  cursor c_foresee_everyday_policy_no is
    select t.policy_no
      from fin_notcol_foresee_everyday t
     where t.endorse_no is not null
     group by t.policy_no;

  cursor c_foresee_duepremium_policy_no is
    select policy_no
      from fin_notcol_foresee_duepremium b
     group by b.policy_no
    having sum(b.due_premium) = 0;
  type t_foresee_duepremium_policy_no is table of fin_notcol_foresee_duepremium.policy_no%type index by binary_integer;
  --type t_foresee_duepremium_policy_no is table of fin_notcol_foresee_duepremium.policy_no%type;

  v_foresee_duepremium_policy_no t_foresee_duepremium_policy_no;

begin
  --在fin_notcol_foresee_everyday表数据输入完成后调用此过程
  --更新k、p表数据到参照表fin_notcol_foresee_duepremium
  --fin_notcol_foresee_everyday表记录了k、p表当天更新的数据
  merge into fin_notcol_foresee_duepremium t1
  using (select * from fin_notcol_foresee_everyday where isdata = 1) t2 --是否符合远期条件，符合远期条件记录 1，不符合远期条件记录 2
  on (t1.receipt_no = t2.receipt_no and t1.plan_code = t2.plan_code)
  when matched then
    update
       set t1.created_date         = t2.created_date,
           t1.created_by           = t2.created_by,
           t1.updated_date         = t2.updated_date,
           t1.updated_by           = t2.updated_by,
           t1.department_code      = t2.department_code,
           t1.department_name      = t2.department_name,
           t1.policy_no            = t2.policy_no,
           t1.endorse_no           = t2.endorse_no,
           t1.notice_no            = t2.notice_no,
           t1.precol_amount        = t2.precol_amount,
           t1.due_premium          = t2.due_premium,
           t1.insurance_begin_date = t2.insurance_begin_date,
           t1.insurance_end_date   = t2.insurance_end_date,
           t1.insured_person       = t2.insured_person,
           t1.due_voucher_no       = t2.due_voucher_no,
           t1.due_voucher_date     = t2.due_voucher_date,
           t1.plan_name            = t2.plan_name,
           t1.currency_code        = t2.currency_code,
           t1.currency_name        = t2.currency_name,
           t1.client_code          = t2.client_code,
           t1.client_name          = t2.client_name,
           t1.sale_agent_code      = t2.sale_agent_code,
           t1.sale_agent_name      = t2.sale_agent_name,
           t1.sale_channel_code    = t2.sale_channel_code,
           t1.sale_channel_name    = t2.sale_channel_name,
           t1.group_code           = t2.group_code,
           t1.group_name           = t2.group_name,
           t1.precol_amount_sum    = t2.precol_amount_sum,
           t1.due_premium_sum      = t2.due_premium_sum,
           t1.cancel_flag          = t2.cancel_flag,
           t1.underwrite_time      = t2.underwrite_time,
           t1.settle_date          = t2.settle_date,
           t1.agent_code           = t2.agent_code,
           t1.agent_chinese_name   = t2.agent_chinese_name,
           t1.payment_end_date     = t2.payment_end_date,
           t1.account_days         = t2.account_days,
           t1.account_month        = t2.account_month
  when not matched then
    insert
      (t1.created_date,
       t1.created_by,
       t1.updated_date,
       t1.updated_by,
       t1.department_code,
       t1.department_name,
       t1.policy_no,
       t1.endorse_no,
       t1.notice_no,
       t1.precol_amount,
       t1.due_premium,
       t1.insurance_begin_date,
       t1.insurance_end_date,
       t1.insured_person,
       t1.due_voucher_no,
       t1.due_voucher_date,
       t1.plan_code,
       t1.plan_name,
       t1.currency_code,
       t1.currency_name,
       t1.client_code,
       t1.client_name,
       t1.sale_agent_code,
       t1.sale_agent_name,
       t1.sale_channel_code,
       t1.sale_channel_name,
       t1.group_code,
       t1.group_name,
       t1.precol_amount_sum,
       t1.due_premium_sum,
       t1.cancel_flag,
       t1.underwrite_time,
       t1.settle_date,
       t1.receipt_no,
       t1.agent_code,
       t1.agent_chinese_name,
       t1.payment_end_date,
       t1.account_days,
       t1.account_month)
    values
      (t2.created_date,
       t2.created_by,
       t2.updated_date,
       t2.updated_by,
       t2.department_code,
       t2.department_name,
       t2.policy_no,
       t2.endorse_no,
       t2.notice_no,
       t2.precol_amount,
       t2.due_premium,
       t2.insurance_begin_date,
       t2.insurance_end_date,
       t2.insured_person,
       t2.due_voucher_no,
       t2.due_voucher_date,
       t2.plan_code,
       t2.plan_name,
       t2.currency_code,
       t2.currency_name,
       t2.client_code,
       t2.client_name,
       t2.sale_agent_code,
       t2.sale_agent_name,
       t2.sale_channel_code,
       t2.sale_channel_name,
       t2.group_code,
       t2.group_name,
       t2.precol_amount_sum,
       t2.due_premium_sum,
       t2.cancel_flag,
       t2.underwrite_time,
       t2.settle_date,
       t2.receipt_no,
       t2.agent_code,
       t2.agent_chinese_name,
       t2.payment_end_date,
       t2.account_days,
       t2.account_month);
  commit;
  insert into epcis_job_log
    (input_time, sid, serial#, error_code_no, error_message, error_comment)
  values
    (sysdate,
     1,
     5,
     0,
     '暂无',
     'pro_finance_foresee_long更新fin_notcol_foresee_duepremium表完成');
  commit;

  --删除不符合要求的远期未全额预收数据
  --1 保险起期、核保时间最大值不在下个月
  --2 预收金额等于应收金额的保批单数据
  --3 应收核销的保批单数据
  delete /*+ direct */
  from fin_notcol_foresee_duepremium w
   where exists (select t.policy_no
            from fin_notcol_foresee_everyday t
           where t.receipt_no = w.receipt_no
             and t.plan_code = w.plan_code
             and t.isdata = 2);
  commit;
  insert into epcis_job_log
    (input_time, sid, serial#, error_code_no, error_message, error_comment)
  values
    (sysdate,
     3,
     5,
     0,
     '暂无',
     'pro_finance_foresee_long删除不符合要求数据完成');
  commit;

  --回查fin_notcol_foresee_duepremium表将批批单对应的原始保单的结算日期不在99年1月1日（含）以后，删除保单累加为0的数据
  for tmp_rec in c_foresee_everyday_policy_no loop
    begin
      select count(*)
        into i_count
        from premium_info t
       where t.policy_no = tmp_rec.policy_no
         and t.endorse_no is null
         and t.settle_date >= to_date('1999-01-01', 'yyyy-mm-dd')
         and rownum = 1;
      if i_count = 0 or i_count is null then
        select count(*)
          into i_count
          from kcoldue tr
         where tr.cplyno = tmp_rec.policy_no
           and tr.cedrno is null
           and dcaldte >= to_date('1999-01-01', 'yyyy-mm-dd')
           and rownum = 1;
      end if;
    exception
      when no_data_found then
        i_count := 0;
      when others then
        i_count := 0;
    end;

    if i_count = 0 then
      delete fin_notcol_foresee_duepremium a
       where a.policy_no = tmp_rec.policy_no;
      v_count := v_count + 1;
      if v_count >= 5000 then
        commit;
        v_count := 0;
      end if;
    end if;
  end loop;
  insert into epcis_job_log
    (input_time, sid, serial#, error_code_no, error_message, error_comment)
  values
    (sysdate,
     5,
     21,
     0,
     '暂无',
     'pro_finance_foresee_long 删除批单对应的原始保单不符合应收条件数据完成');
  commit;

  insert into epcis_job_log
  values
    (sysdate, 8, 5, 0, '暂无', 'pro_finance_foresee_long调用完成',sys_guid());
  commit;

  -- 删除注销累积应收为 0 的纪录
  open c_foresee_duepremium_policy_no;
  loop
    fetch c_foresee_duepremium_policy_no bulk collect
      into v_foresee_duepremium_policy_no limit 30000;
    exit when not v_foresee_duepremium_policy_no.exists(1);
    --exit when  v_foresee_duepremium_policy_no.count = 0;

    forall i in v_foresee_duepremium_policy_no.first .. v_foresee_duepremium_policy_no.last
      delete /*+ direct */
      from fin_notcol_foresee_duepremium a
       where policy_no = v_foresee_duepremium_policy_no(i);
    commit;
  end loop;

  insert into epcis_job_log
    (input_time, sid, serial#, error_code_no, error_message, error_comment)
  values
    (sysdate,
     6,
     22,
     0,
     '暂无',
     'pro_finance_foresee_long 删除fin_notcol_foresee_duepremium表累积应收为0的记录完成');
  commit;
exception
  when others then
    if c_foresee_everyday_policy_no%ISOPEN then
      close c_foresee_everyday_policy_no;
    end if;

    if c_foresee_duepremium_policy_no%ISOPEN then
      close c_foresee_duepremium_policy_no;
    end if;

    v_ErrCodeVal := sqlcode;
    v_ErrMsg     := substr('获取pro_finance_foresee_long过程出错' || sqlerrm,
                           1,
                           500);
    insert into epcis_job_log
    values
      (sysdate,
       55,
       5,
       v_ErrCodeVal,
       v_ErrMsg,
       'pro_finance_foresee_long执行失败！',sys_guid());
    commit;
end pro_finance_foresee_long;

procedure  pro_finance_dueforesee_stop(parameters in varchar2, re_cursor out t_cursor)
  is
      v_cursor             t_cursor;
      filter               varchar2 (1800);              --过滤
      sel                   varchar2 (10000);              --选择项
      sql_statement         varchar2 (10000);              --Select语句
      p_department_code    varchar2(10);
      p_emp_code           varchar2(30);
      data_not_prepared     exception;
      counterlimit         number(10);  --报表查询记录条数限制
      p_flag               number(1);--定义查询应收停单清单（1）还是查询应收停单预警清单（2）
      v_ErrMsg             varchar2(500);
      p_plancode           varchar2(20);
      p_plan_class_code    varchar2(20);
      p_filter_plan        varchar2(1000);
      --v_count             number;
      --v_str             varchar2(1000);
  begin
  --根据报表运行的当前时间获取记录条数限制值
    if to_char(sysdate, 'hh24') > 7 and to_char(sysdate, 'hh24') < 21 then
      counterlimit := 65000;
    else
      counterlimit := 200000;
    end if;
-- para:financeDepartments=040000^saleAgent_code_epcis=100012^listType_epcis=1^
  p_department_code := pkg_ereport.getParameterValue(parameters,'financeDepartments'); --机构
  p_emp_code := pkg_ereport.getparametervalue(PARAMETERS, 'saleAgent_code_epcis'); --业务员
  p_flag := pkg_ereport.getparametervalue(PARAMETERS, 'listType_epcis'); --清单类型
  p_plancode:=pkg_ereport.getparametervalue(PARAMETERS, 'plan_code');--险种明细
  p_plan_class_code:=pkg_ereport.getparametervalue(PARAMETERS, 'planClasses');--险种大类
--财务机构如果是汇总机构则做相应处理
  if p_department_code = substr(p_department_code,1,2) || '9999' then
      p_department_code := substr(p_department_code, 1,2);
  elsif p_department_code = substr(p_department_code,1,4) || '99' then
      p_department_code := substr(p_department_code, 1,4);
  end if;
  --组织filter过滤
  filter := '';
  p_filter_plan:='';
  --机构（必选）

   filter := filter ||' and substr(a.department_code, 1, 3) in'
         ||' (select distinct substr(df.department_code, 1, 3)'
         ||'  from department_finance           df,'
         ||'        sas_due_supvz_set due'
         ||'   where due.department_code = substr(df.department_code, 1, 3)'
         ||'    and df.finance_department_code like '''|| p_department_code ||'''||''%'')';
  filter := filter ||' and b.department_code like ''' || p_department_code ||'''||''%''';
  --业务员(非必选)
  if p_emp_code is not null then
      filter := filter || ' and b.SALE_AGENT_CODE =''' || p_emp_code ||'''';
  end if;

--险种大类（非必选）
p_filter_plan:=p_filter_plan /*||' select plan_code,plan_class_code'
             ||'  from (select distinct plan_code, ''A''plan_class_code'
             ||'       from plan_class_relation'
             ||'      where plan_class_code = ''SC''   union all '
             ||'    select distinct plan_code, ''B'' plan_class_code'
             ||'      from plan_class_relation'
             ||'     where plan_class_code in (select plan_class_code'
             ||'                               from plan_class'
             ||'                              where plan_class_standard = ''2''  and '
             ||'                              plan_class_code <> ''SC'' ) union all '
             ||'  select distinct plan_code, ''C'' plan_class_code'
             ||'   from plan_class_relation'
             ||'  where plan_class_code in (''J'', ''K''))'*/
             ||' Select plan_code, plan_class_code from (Select Distinct t.c_ins_no plan_code, '
             ||' decode(t.plan_class, ''01'', ''A'', ''02'', ''B'', ''03'', ''C'', ''B'') plan_class_code '
             ||' From produce_map t) ';
       if p_plan_class_code is not null then
        p_filter_plan:=p_filter_plan ||'  where  plan_class_code = '''|| p_plan_class_code||'''' ;
       end if;
/*if p_plan_class_code is not null then
    if p_plan_class_code='A'  then--车险
     p_filter_plan:=' SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T  where plan_class_code = ''SC'' AND INVALIDATE_DATE IS NULL ';
    elsif  p_plan_class_code='B' then--财产险
      p_filter_plan:=' SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T  where plan_class_code in (select plan_class_code '
                      ||' from plan_class  where plan_class_standard = ''2'' and plan_class_code <> ''SC'') AND INVALIDATE_DATE IS NULL  ';
    else--意健险
       p_filter_plan:=' SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T  where plan_class_code in (''J'', ''K'')  AND INVALIDATE_DATE IS NULL  ';
       end if;
 else
     p_filter_plan:='SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION  T '
       ||' WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52''';

end if;*/
--险种（非必选）
if p_plancode is not null then
 p_filter_plan:=p_filter_plan||' and PLAN_CODE='''||p_plancode||'''';
end if;

  --增加每次返回条数限制
  filter := filter || ' and rownum <= '|| counterlimit ;

if(p_flag=1) then --应收、远期停单清单

 sel := 'insert into FINANCE_DUE_STOP_POLICY_TMP'
  ||' (EMPLOYEE_CODE,EMPLOYEE_NAME,DEPARTMENT_CODE,DEPARTMENT_NAME,POLICY_NO,ENDORSE_NO,TERM_NO,DUE_PREMIUM,'
  ||' DUE_PREMIUM_SUM,INSURANCE_BEGIN_DATE,INSURANCE_END_DATE,'
  ||' INSURED_PERSON,CURRENCY_CODE,CURRENCY_NAME,EMPLOYEE_CHANNEL,PLAN_CLASS_CODE,CLIENT_CODE,AGE_DAY)'
  ||' SELECT '
  ||' A.EMPLOYEE_CODE,a.employee_name,b.department_code,b.department_name,b.policy_no,b.endorse_no,'
  ||' b.notice_no,b.due_premium,b.due_premium_sum,b.insurance_begin_date,b.insurance_end_date,'
  ||' b.insured_person,b.currency_code,b.currency_name,'
  ||' NVL(A.EMPLOYEE_CHANNEL, A.EMPLOYEE_TYPE) EMPLOYEE_CHANNEL,'
  ||' C.PLAN_CLASS_CODE, '
  ||' DECODE(B.CLIENT_CODE, ''1'', ''1'', ''2''),'
  ||'B.account_days '
  ||' FROM epcisbase.SAS_EMPLOYEE A, vw_foresee_duepremium B, ( ';
  sel := sel||p_filter_plan ||')  C '
 --||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
--||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
  ||' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE AND B.PLAN_CODE = C.PLAN_CODE and b.policy_no in ('
  ||' SELECT '
  ||'b.policy_no'
  ||' FROM epcisbase.SAS_EMPLOYEE A, vw_foresee_duepremium B, ( ';
  sel:=sel||p_filter_plan||') C'
 -- ||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
 -- ||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
  ||' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE AND B.PLAN_CODE = C.PLAN_CODE '
  ||' AND A.LEAVE_DATE IS NULL AND B.INSURANCE_BEGIN_DATE >= TO_DATE(''2009-1-1'', ''yyyy-mm-dd'')'
  ||'   AND B.account_days >='
  ||'       (select se.use_age '
  ||'          from sas_due_supvz_set se '
  ||'        where se.effective_date < sysdate + 1 '
  ||'          and (se.invalidate_date is null or se.invalidate_date > sysdate) '
  ||'          and se.channel_code = a.employee_channel '
  ||'          and se.department_code = (select substr(emp.department_code,1,3) from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';
 sel := sel || filter||'group by b.policy_no having sum(b.due_premium)>0)'||' AND B.account_days >='
  ||'       (select se.use_age '
  ||'          from sas_due_supvz_set se '
  ||'        where se.effective_date < sysdate + 1 '
  ||'          and (se.invalidate_date is null or se.invalidate_date > sysdate) '
  ||'          and se.channel_code = a.employee_channel '
  ||'          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';

  elsif(p_flag=2) then --应收、远期停单预警清单
      sel := 'insert into FINANCE_DUE_STOP_POLICY_TMP'
  ||' (EMPLOYEE_CODE,EMPLOYEE_NAME,DEPARTMENT_CODE,DEPARTMENT_NAME,POLICY_NO,ENDORSE_NO,TERM_NO,DUE_PREMIUM,'
  ||' DUE_PREMIUM_SUM,INSURANCE_BEGIN_DATE,INSURANCE_END_DATE,'
  ||' INSURED_PERSON,CURRENCY_CODE,CURRENCY_NAME,EMPLOYEE_CHANNEL,PLAN_CLASS_CODE,CLIENT_CODE,AGE_DAY)'
  ||' SELECT '
  ||' A.EMPLOYEE_CODE,a.employee_name,b.department_code,b.department_name,b.policy_no,b.endorse_no,'
  ||' b.notice_no,b.due_premium,b.due_premium_sum,b.insurance_begin_date,b.insurance_end_date,'
  ||' b.insured_person,b.currency_code,b.currency_name,'
  ||' NVL(A.EMPLOYEE_CHANNEL, A.EMPLOYEE_TYPE) EMPLOYEE_CHANNEL,'
  ||'  C.PLAN_CLASS_CODE, '
  ||' DECODE(B.CLIENT_CODE, ''1'', ''1'', ''2''),'
  ||' B.account_days '
  ||' FROM epcisbase.SAS_EMPLOYEE A, vw_foresee_duepremium B,( ';
    sel:=sel||p_filter_plan||') C'
  --||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
 --||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
  ||' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE AND B.PLAN_CODE = C.PLAN_CODE and b.policy_no in ('
  ||' SELECT '
  ||'b.policy_no'
  ||' FROM epcisbase.SAS_EMPLOYEE A, vw_foresee_duepremium B,( ';
    sel:=sel||p_filter_plan||') C'
  --||' (SELECT PLAN_CODE,PLAN_CLASS_CODE FROM PLAN_CLASS_RELATION T'
  --||'    WHERE PLAN_CLASS_CODE IN (''A'', ''B'', ''C'', ''J'', ''K'') AND INVALIDATE_DATE IS NULL AND PLAN_CODE <> ''A52'') C '
  ||' WHERE A.EMPLOYEE_CODE = B.SALE_AGENT_CODE AND B.PLAN_CODE = C.PLAN_CODE '
  ||' AND A.LEAVE_DATE IS NULL AND B.INSURANCE_BEGIN_DATE >= TO_DATE(''2009-1-1'', ''yyyy-mm-dd'')'
  ||'   AND B.account_days between'
  ||'       (select se.use_age '
  ||'          from sas_due_supvz_set se '
  ||'        where se.effective_date < sysdate + 1 '
  ||'          and (se.invalidate_date is null or se.invalidate_date > sysdate) '
  ||'          and se.channel_code = a.employee_channel '
  ||'          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))-15 and '
  ||'       (select se.use_age '
  ||'          from sas_due_supvz_set se '
  ||'        where se.effective_date < sysdate + 1 '
  ||'          and (se.invalidate_date is null or se.invalidate_date > sysdate) '
  ||'          and se.channel_code = a.employee_channel '
  ||'          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';

 sel := sel || filter||'group by b.policy_no having sum(b.due_premium)>0)'||' AND B.account_days between'
  ||'       (select se.use_age '
  ||'          from sas_due_supvz_set se '
  ||'        where se.effective_date < sysdate + 1 '
  ||'          and (se.invalidate_date is null or se.invalidate_date > sysdate) '
  ||'          and se.channel_code = a.employee_channel '
  ||'          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))-15 and '
  ||'       (select se.use_age '
  ||'          from sas_due_supvz_set se '
  ||'        where se.effective_date < sysdate + 1 '
  ||'          and (se.invalidate_date is null or se.invalidate_date > sysdate) '
  ||'          and se.channel_code = a.employee_channel '
  ||'          and se.department_code = (select substr(emp.department_code,1,3)from epcisbase.sas_employee emp where emp.employee_code = b.sale_agent_code and rownum = 1) and se.client_code = decode(b.client_code, ''1'', ''1'', ''2''))';

  end if;

/*  v_count := 0;

  while v_count < length(sel) loop
     if v_count + 250 > length(sel) then
        v_str := substr(sel,v_count,length(sel));
     else
     v_str := substr(sel,v_count,250);
     end if;
     v_count:= v_count+250;
  end loop;*/



  sql_statement := sel ;
  execute immediate sql_statement;

--打开游标返回
open v_cursor for
  select EMPLOYEE_CODE AS 业务员代码 ,
         EMPLOYEE_NAME AS   业务员名称,
         DEPARTMENT_CODE AS 机构代码 ,
         DEPARTMENT_NAME AS  机构名称,
         POLICY_NO AS  保单号,
         ENDORSE_NO AS 批单号,
         TERM_NO AS 期次,
         DUE_PREMIUM AS 金额,
         DUE_PREMIUM_SUM AS  折合人民币,
         INSURANCE_BEGIN_DATE AS 保险起期,
         INSURANCE_END_DATE AS 保险止期,
         INSURED_PERSON AS  被保险人,
         CURRENCY_CODE AS 币种编码,
         CURRENCY_NAME AS 币种,
         EMPLOYEE_CHANNEL AS 渠道,
         decode(EMPLOYEE_CHANNEL, 'DS', '直销', 'CS', '综合开拓', 'AS', '车行', 'BS', '重点客户', 'ES',
                '代理', 'FS', '非重客经纪', 'GS', '混合业务渠道', 'HS', '新渠道', 'IS', '银保渠道', 'JS',
                '零售渠道', EMPLOYEE_CHANNEL) as 渠道名称,
         PLAN_CLASS_CODE AS 险种,
         /*(select p.plan_class_name
            from plan_class p
           where p.plan_class_code = tmp.plan_class_code
             and rownum =1)*/
         decode(PLAN_CLASS_CODE,'A','车险','B','财产险','C','意健险', PLAN_CLASS_CODE) as 险种名称,
         CLIENT_CODE AS 客户代码,
         AGE_DAY AS 帐龄
    from FINANCE_DUE_STOP_POLICY_TMP tmp--EPCISACCT.tmp_finance_duepremium_short
   where rownum <= counterlimit
   order by EMPLOYEE_CODE;
  Re_Cursor := v_cursor;
  exception
    when data_not_prepared then
      v_ErrMsg := substr('获取数据过程出错! '|| sqlerrm, 1, 500);
       raise_application_error(-20001, v_ErrMsg);
      when others then
         v_ErrMsg := substr('获取数据过程出错! '|| sqlerrm, 1, 500);
         raise_application_error(-20001, v_ErrMsg);
         rollback;
    end  pro_finance_dueforesee_stop;
 /*********************************************
 --应收计算优化多进程计算机构初始化2011-12-15
 **********************************************/
Procedure pro_finance_duep_department Is
  v_list      epciscde.T_VARCHAR;
  v_big_count Number;
Begin
  --防止有主键冲突,首先更新RUN_DATE=CREATED_DATE
  Update etl_department_run a
  Set    a.run_date = created_date
  Where  a.run_date = trunc(Sysdate)
  And    a.use_project = 'ysjs';

  --取数据控制判断需要进行大机构拆分, 设计一个超大值即可作为开关使用
  Begin
    Select to_number(t.value_code) Into v_big_count From common_parameter t Where t.collection_code = 'YSJS_BIG';
  Exception
    When Others Then
      --默认100万
      v_big_count := 1000000;
  End;

  Select department_code
  Bulk   Collect
  Into   v_list
  From   tmp_k_p_update_data_everyday a
  Group  By a.department_code
  Having Count(1) > v_big_count;

  Insert Into etl_department_run
    (created_date, created_by, updated_date, updated_by, run_date, department_code, flag, begin_time, end_time, error_msg, use_project)
    Select Distinct Sysdate,
                    'system',
                    Sysdate,
                    'system',
                    trunc(Sysdate),
                    department_code || '-' || substr(policy_no, -1),
                    'W',
                    Null,
                    Null,
                    Null,
                    'ysjs'
    From   tmp_k_p_update_data_everyday a
    Where  Exists (Select 1 From Table(v_list) Where column_value = a.department_code);

  --插入新的计算机构
  Insert Into etl_department_run
    (created_date, created_by, updated_date, updated_by, run_date, department_code, flag, begin_time, end_time, error_msg, use_project)
    Select Distinct Sysdate, 'system', Sysdate, 'system', trunc(Sysdate), department_code, 'W', Null, Null, Null, 'ysjs'
    From   tmp_k_p_update_data_everyday a
    Where  Not Exists (Select 1 From Table(v_list) Where column_value = a.department_code);
  Commit;

  --20121011 CODEXPERT注释 caopengfei105
  Insert Into etl_department_run_log
    (created_date,
     created_by,
     updated_date,
     updated_by,
     run_date,
     department_code,
     flag,
     begin_time,
     end_time,
     error_msg,
     use_project,
     id_etl_department_run_log)
    Select created_date,
           created_by,
           updated_date,
           updated_by,
           run_date,
           department_code,
           flag,
           begin_time,
           end_time,
           error_msg,
           use_project,
           sys_guid()
    From   etl_department_run a
    Where  a.run_date <= add_months(trunc(Sysdate), -1)
    And    a.use_project = 'ysjs';
  Delete From etl_department_run a
  Where  a.run_date <= add_months(trunc(Sysdate), -1)
  And    a.use_project = 'ysjs';
  Commit;
End pro_finance_duep_department;
  /*******************************************
  --应收计算优化多进程计算优化2011-12-15
    工作原理:1、分机构多进程MERGE N/O表,更新插入应收记录
             2、分机构多进程删除N/O表转实收的记录
             3、分机构多进程从O表拉回应收记录MERGE N表
             4、分机构多进程删除1999-01-01的记录
             5、删除保单合并金额为0的记录
  --其中第4、5步放long2处理即可,无需多进程处理
  ********************************************/
  procedure pro_finance_duep_more_course is
    v_count          number;
    v_department_cnt number;
    v_modify_cnt1    number;
    v_modify_cnt2    number;
    v_delete_cnt     number;
    v_ErrMsg         varchar2(500);
    v_ErrCodeVal     number := 0;
    i_count          number := 0;
    i_count1         number := 0;

    v_department_code tmp_k_p_update_data_everyday.department_code%type;
    --不符合要求数据
    cursor c_tfd_long_o(p_department_code varchar2, p_tail_str Varchar2) is
      select /*+ index(t IX_KP_EVERYDATE_DP_PLAST)*/
       t.receipt_no, t.plan_code
        from tmp_k_p_update_data_everyday t
       where t.isdata = 2
         And substr(POLICY_NO,-1) Like p_tail_str
         and t.department_code = p_department_code;

     cursor c_o_to_n_long(p_department_code varchar2, p_tail_str Varchar2) is
      select /*+ index(t IX_KP_EVERYDATE_DP_PLAST)*/
       t.policy_no
        from tmp_k_p_update_data_everyday t
      where   substr(POLICY_NO,-1) Like p_tail_str
         and t.department_code = p_department_code;
         --实收的保单也要取出来
    /*   where t.isdata = 1
         And substr(POLICY_NO,-1) Like p_tail_str
         and t.department_code = p_department_code;*/

    type v_receipt_no is table of tmp_k_p_update_data_everyday.receipt_no%type index by binary_integer;
    type v_plan_code is table of tmp_k_p_update_data_everyday.plan_code%type index by binary_integer;
    v_receipt_no_n v_receipt_no;
    v_plan_code_n  v_plan_code;

    --拆分大机构计算
    v_big_string Varchar(20);
    v_split_pos Number;
    v_tail_string Varchar(10) := '%';
    type v_policy_list is table of tmp_k_p_update_data_everyday.policy_no%type index by binary_integer;
    v_policy_list_n  v_policy_list;
  begin
    select count(1)
      into v_count
      from etl_department_run a
     where a.run_date = trunc(sysdate)
       and a.use_project = 'ysjs';
    for cur_merge in 1 .. v_count loop
      update etl_department_run a
         set a.flag = 'R', a.begin_time = sysdate, a.updated_date = sysdate
       where a.run_date = trunc(sysdate)
         and a.use_project = 'ysjs'
         and a.flag = 'W'
         and rownum = 1 return department_code Into v_big_string;
      -- v_department_code;
      v_department_cnt := sql%rowcount;
      exit when v_department_cnt = 0;
      commit;

      --拆分大机构计算
      v_split_pos := instr(v_big_string, '-');
      If v_split_pos <> 0 Then
        v_department_code := substr(v_big_string, 1, v_split_pos - 1);
        v_tail_string := substr(v_big_string, v_split_pos + 1);
      Else
        v_department_code := v_big_string;
        v_tail_string := '%';
      End If;

      begin
        Merge /*+ index(t1 IX_TMPFINANCEDUELONGN_RECEIPT)*/ into tmp_finance_duepremium_long_n t1
        using (select /*+ index(a IX_KP_EVERYDATE_DP_PLAST)*/
                a.*,
                nvl((select o.client_name
                      from epcisacct.opera_type_temp o
                     where o.policy_no = a.policy_no
                       and nvl(o.opera_type,'null') not in ('0M', '0N', '0O','1K','1L','1M')
                       and rownum = 1),
                    epciscde.pkg_finance_pub_tools.getApplicantByPolicyEndorse(a.policy_no,
                                                                               a.endorse_no)) as applicant_name
                 from tmp_k_p_update_data_everyday a
                where a.isdata = 1
                  And substr(POLICY_NO,-1) Like v_tail_string
                  and a.department_code = v_department_code) t2 --是否符合应收条件，符合应收条件记录 1，不符合应收条件记录 2
        on (t1.receipt_no = t2.receipt_no and t1.plan_code = t2.plan_code)
        when matched then
          update
             set t1.department_code      = t2.department_code,
                 t1.department_name      = t2.department_name,
                 t1.policy_no            = t2.policy_no,
                 t1.endorse_no           = t2.endorse_no,
                 t1.notice_no            = t2.notice_no,
                 t1.due_premium          = t2.due_premium,
                 t1.insurance_begin_date = t2.insurance_begin_date,
                 t1.insurance_end_date   = t2.insurance_end_date,
                 t1.insured_person       = t2.insured_person,
                 t1.due_voucher_no       = t2.due_voucher_no,
                 t1.due_voucher_date     = t2.due_voucher_date,
                 t1.account_days         = t2.account_days,
                 t1.plan_name            = t2.plan_name,
                 t1.currency_code        = t2.currency_code,
                 t1.currency_name        = t2.currency_name,
                 t1.client_code          = t2.client_code,
                 t1.client_name          = t2.client_name,
                 t1.sale_agent_code      = t2.sale_agent_code,
                 t1.sale_agent_name      = t2.sale_agent_name,
                 t1.sale_channel_code    = t2.sale_channel_code,
                 t1.sale_channel_name    = t2.sale_channel_name,
                 t1.group_code           = t2.group_code,
                 t1.group_name           = t2.group_name,
                 t1.due_premium_sum      = t2.due_premium_sum,
                 t1.cancel_flag          = t2.cancel_flag,
                 t1.underwrite_time      = t2.underwrite_time,
                 t1.settle_date          = t2.settle_date,
                 t1.agent_code           = t2.agent_code,
                 t1.agent_chinese_name   = t2.agent_chinese_name,
                 t1.payment_end_date     = t2.payment_end_date,
                 t1.account_month        = t2.account_month,
                 t1.busi_department_code = t2.busi_department_code,
                 t1.business_source_code = substr(t2.channel_source_code , 1, 1),
                 t1.business_source_detail_code = substr(t2.channel_source_code , 2, 1),
                 t1.channel_source_code = substr(t2.channel_source_code , 3, 1),
                 t1.channel_source_detail_code = DECODE(substr(t2.channel_source_code , 4, 1),'0','', Substr(t2.channel_source_code , 4, 1)),
                 t1.applicant_name = t2.applicant_name,
                 t1.due_tax    = t2.due_tax,
                 t1.total_amount = t2.total_amount
        when not matched then
          insert
            (department_code,
             department_name,
             policy_no,
             endorse_no,
             notice_no,
             due_premium,
             insurance_begin_date,
             insurance_end_date,
             insured_person,
             due_voucher_no,
             due_voucher_date,
             account_days,
             plan_code,
             plan_name,
             currency_code,
             currency_name,
             client_code,
             client_name,
             sale_agent_code,
             sale_agent_name,
             sale_channel_code,
             sale_channel_name,
             group_code,
             group_name,
             due_premium_sum,
             cancel_flag,
             underwrite_time,
             settle_date,
             receipt_no,
             agent_code,
             agent_chinese_name,
             payment_end_date,
             account_month,
             busi_department_code,
             business_source_code,
             business_source_detail_code,
             channel_source_code,
             channel_source_detail_code,
             applicant_name,
             due_tax,
             total_amount)
          values
            (t2.department_code,
             t2.department_name,
             t2.policy_no,
             t2.endorse_no,
             t2.notice_no,
             t2.due_premium,
             t2.insurance_begin_date,
             t2.insurance_end_date,
             t2.insured_person,
             t2.due_voucher_no,
             t2.due_voucher_date,
             t2.account_days,
             t2.plan_code,
             t2.plan_name,
             t2.currency_code,
             t2.currency_name,
             t2.client_code,
             t2.client_name,
             t2.sale_agent_code,
             t2.sale_agent_name,
             t2.sale_channel_code,
             t2.sale_channel_name,
             t2.group_code,
             t2.group_name,
             t2.due_premium_sum,
             t2.cancel_flag,
             t2.underwrite_time,
             t2.settle_date,
             t2.receipt_no,
             t2.agent_code,
             t2.agent_chinese_name,
             t2.payment_end_date,
             t2.account_month,
             t2.busi_department_code,
             substr(t2.channel_source_code , 1, 1),
             substr(t2.channel_source_code , 2, 1),
             substr(t2.channel_source_code , 3, 1),
              DECODE(substr(t2.channel_source_code , 4, 1),'0','', Substr(t2.channel_source_code , 4, 1)),
             t2.applicant_name,
             t2.due_tax,
             t2.total_amount);
        v_modify_cnt1 := sql%rowcount;
        commit;
        insert into epcis_job_log
        values
          (sysdate,
           1,
           5,
           0,
           '暂无',
           'pro_finance_duepremium_long5更新tmp_finance_duepremium_long_n表完成v_modify_cnt1' || ';' ||v_big_string
           ,sys_guid());
     --更新tmp_finance_duepremium_long_o表
        merge into tmp_finance_duepremium_long_o t1
       using (select /*+ index(a IX_KP_EVERYDATE_DP_PLAST)*/
             a.*,
             nvl((select o.client_name
                   from epcisacct.opera_type_temp o
                  where o.policy_no = a.policy_no
                    and nvl(o.opera_type,'null') not in ('0M', '0N', '0O','1K','1L','1M')
                    and rownum = 1),
                 epciscde.pkg_finance_pub_tools.getApplicantByPolicyEndorse(a.policy_no,
                                                                               a.endorse_no)) as applicant_name
              from tmp_k_p_update_data_everyday a
             where a.isdata = 1
               And substr(POLICY_NO,-1) Like v_tail_string
               and a.department_code = v_department_code) t2 --是否符合应收条件，符合应收条件记录 1，不符合应收条件记录 2
       on (t1.receipt_no = t2.receipt_no and t1.plan_code = t2.plan_code)
        when matched then
          update
             set t1.department_code      = t2.department_code,
                 t1.department_name      = t2.department_name,
                 t1.policy_no            = t2.policy_no,
                 t1.endorse_no           = t2.endorse_no,
                 t1.notice_no            = t2.notice_no,
                 t1.due_premium          = t2.due_premium,
                 t1.insurance_begin_date = t2.insurance_begin_date,
                 t1.insurance_end_date   = t2.insurance_end_date,
                 t1.insured_person       = t2.insured_person,
                 t1.due_voucher_no       = t2.due_voucher_no,
                 t1.due_voucher_date     = t2.due_voucher_date,
                 t1.account_days         = t2.account_days,
                 t1.plan_name            = t2.plan_name,
                 t1.currency_code        = t2.currency_code,
                 t1.currency_name        = t2.currency_name,
                 t1.client_code          = t2.client_code,
                 t1.client_name          = t2.client_name,
                 t1.sale_agent_code      = t2.sale_agent_code,
                 t1.sale_agent_name      = t2.sale_agent_name,
                 t1.sale_channel_code    = t2.sale_channel_code,
                 t1.sale_channel_name    = t2.sale_channel_name,
                 t1.group_code           = t2.group_code,
                 t1.group_name           = t2.group_name,
                 t1.due_premium_sum      = t2.due_premium_sum,
                 t1.cancel_flag          = t2.cancel_flag,
                 t1.underwrite_time      = t2.underwrite_time,
                 t1.settle_date          = t2.settle_date,
                 t1.agent_code           = t2.agent_code,
                 t1.agent_chinese_name   = t2.agent_chinese_name,
                 t1.payment_end_date     = t2.payment_end_date,
                 t1.account_month        = t2.account_month,
                 t1.busi_department_code = t2.busi_department_code,
                 t1.business_source_code = substr(t2.channel_source_code , 1, 1),
                 t1.business_source_detail_code = substr(t2.channel_source_code , 2, 1),
                 t1.channel_source_code = substr(t2.channel_source_code , 3, 1),
                 t1.channel_source_detail_code =  DECODE(substr(t2.channel_source_code , 4, 1),'0','', Substr(t2.channel_source_code , 4, 1)),
                 t1.applicant_name = t2.applicant_name,
                 t1.due_tax = t2.due_tax,
                 t1.total_amount = t2.total_amount
        when not matched then
          insert
            (department_code,
             department_name,
             policy_no,
             endorse_no,
             notice_no,
             due_premium,
             insurance_begin_date,
             insurance_end_date,
             insured_person,
             due_voucher_no,
             due_voucher_date,
             account_days,
             plan_code,
             plan_name,
             currency_code,
             currency_name,
             client_code,
             client_name,
             sale_agent_code,
             sale_agent_name,
             sale_channel_code,
             sale_channel_name,
             group_code,
             group_name,
             due_premium_sum,
             cancel_flag,
             underwrite_time,
             settle_date,
             receipt_no,
             agent_code,
             agent_chinese_name,
             payment_end_date,
             account_month,
             busi_department_code,
             business_source_code,
             business_source_detail_code,
             channel_source_code,
             channel_source_detail_code,
             applicant_name,
             due_tax,
             total_amount)
          values
            (t2.department_code,
             t2.department_name,
             t2.policy_no,
             t2.endorse_no,
             t2.notice_no,
             t2.due_premium,
             t2.insurance_begin_date,
             t2.insurance_end_date,
             t2.insured_person,
             t2.due_voucher_no,
             t2.due_voucher_date,
             t2.account_days,
             t2.plan_code,
             t2.plan_name,
             t2.currency_code,
             t2.currency_name,
             t2.client_code,
             t2.client_name,
             t2.sale_agent_code,
             t2.sale_agent_name,
             t2.sale_channel_code,
             t2.sale_channel_name,
             t2.group_code,
             t2.group_name,
             t2.due_premium_sum,
             t2.cancel_flag,
             t2.underwrite_time,
             t2.settle_date,
             t2.receipt_no,
             t2.agent_code,
             t2.agent_chinese_name,
             t2.payment_end_date,
             t2.account_month,
             t2.busi_department_code,
             substr(t2.channel_source_code , 1, 1),
             substr(t2.channel_source_code , 2, 1),
             substr(t2.channel_source_code , 3, 1),
             DECODE(substr(t2.channel_source_code , 4, 1),'0','', Substr(t2.channel_source_code , 4, 1)),
             t2.applicant_name,
             t2.due_tax,
             t2.total_amount);
        v_modify_cnt2 := sql%rowcount;
        commit;
        insert into epcis_job_log
        values
          (sysdate,
           2,
           5,
           0,
           '暂无',
           'pro_finance_duepremium_long5更新tmp_finance_duepremium_long_o表完成v_modify_cnt2' || ';' ||v_big_string
           ,sys_guid());

        --删除不符合要求数据
        --(此处删除k、p表需要同步，不能分开执行不然会有误删问题)
        open c_tfd_long_o(v_department_code, v_tail_string);
        <<c_tfd_long_o_loop>>
        loop
          fetch c_tfd_long_o bulk collect
            into v_receipt_no_n, v_plan_code_n limit 5000;
          exit c_tfd_long_o_loop when not v_receipt_no_n.exists(1);

   forall i in v_receipt_no_n.first .. v_receipt_no_n.last
            delete /*+ direct */
            from tmp_finance_duepremium_long_o w
             where w.receipt_no = v_receipt_no_n(i)
               and w.plan_code = v_plan_code_n(i);
          v_delete_cnt := sql%rowcount + nvl(v_delete_cnt, 0);
          commit;
          forall i in v_receipt_no_n.first .. v_receipt_no_n.last
            delete /*+ direct */
            from tmp_finance_duepremium_long_n w
             where w.receipt_no = v_receipt_no_n(i)
               and w.plan_code = v_plan_code_n(i);
          v_delete_cnt := sql%rowcount + nvl(v_delete_cnt, 0);
          commit;
        end loop c_tfd_long_o_loop;
        close c_tfd_long_o;

        insert into epcis_job_log
        values
          (sysdate,
           3,
           5,
           0,
           '暂无',
           'pro_finance_duepremium_long5删除不符合要求数据完成' || v_big_string || ';' ||v_delete_cnt
           ,sys_guid());
        commit;

        --将每天更新的且符合条件的插入tmp_finance_duepremium_long_n表数据到tmp_finance_duepremium_long_o找保单匹配的完整数据在插入tmp_finance_duepremium_long_n表
        --当天k,p表更新的数据,且符合应收条件的数据,必须在将每天更新（增、删、改）的数据完全放入tmp_finance_duepremium_long_n表和tmp_finance_duepremium_long_o表
        --for c_t1 in (select distinct tt.policy_no from tmp_k_p_update_data_everyday  tt where tt.isdata = 1) loop --1/2的情况都需要处理 ex-liukailin001 modify2010-12-2
      /* <<loop_2>>
        for c_t1 in (select distinct tt.policy_no
                       from tmp_k_p_update_data_everyday tt
                      where tt.department_code = v_department_code
                        And isdata = 1
                        And substr(POLICY_NO,-1) Like v_tail_string) loop
          <<c_t2_loop>>
          for c_t2 in (select t.*
                         from tmp_finance_duepremium_long_o t
                        where t.policy_no = c_t1.policy_no) loop
            <<begin_end>>
            begin
              select count(*)
                into i_count1
                from tmp_finance_duepremium_long_n t1
               where t1.policy_no = c_t2.policy_no
                 and t1.receipt_no = c_t2.receipt_no
                 and t1.plan_code = c_t2.plan_code;
            exception
              when no_data_found then
                i_count1 := 0;
              when others then
                i_count1 := 0;
            end begin_end;
            if i_count1 > 0 then
              update tmp_finance_duepremium_long_n t1
                 set t1.department_code      = c_t2.department_code,
                     t1.department_name      = c_t2.department_name,
                     t1.endorse_no           = c_t2.endorse_no,
                     t1.notice_no            = c_t2.notice_no,
                     t1.due_premium          = c_t2.due_premium,
                     t1.insurance_begin_date = c_t2.insurance_begin_date,
                     t1.insurance_end_date   = c_t2.insurance_end_date,
                     t1.insured_person       = c_t2.insured_person,
                     t1.due_voucher_no       = c_t2.due_voucher_no,
                     t1.due_voucher_date     = c_t2.due_voucher_date,
                     t1.account_days         = c_t2.account_days,
                     t1.plan_name            = c_t2.plan_name,
                     t1.currency_code        = c_t2.currency_code,
                     t1.currency_name        = c_t2.currency_name,
                     t1.client_code          = c_t2.client_code,
                     t1.client_name          = c_t2.client_name,
                     t1.sale_agent_code      = c_t2.sale_agent_code,
                     t1.sale_agent_name      = c_t2.sale_agent_name,
                     t1.sale_channel_code    = c_t2.sale_channel_code,
                     t1.sale_channel_name    = c_t2.sale_channel_name,
                     t1.group_code           = c_t2.group_code,
                     t1.group_name           = c_t2.group_name,
                     t1.due_premium_sum      = c_t2.due_premium_sum,
                     t1.cancel_flag          = c_t2.cancel_flag,
                     t1.underwrite_time      = c_t2.underwrite_time,
                     t1.settle_date          = c_t2.settle_date,
                     t1.agent_code           = c_t2.agent_code,
                     t1.agent_chinese_name   = c_t2.agent_chinese_name,
                     t1.payment_end_date     = c_t2.payment_end_date,
                     t1.account_month        = c_t2.account_month,
                     t1.busi_department_code = NVL(c_t2.busi_department_code, t1.busi_department_code),
                     t1.business_source_code = NVL(c_t2.business_source_code, t1.business_source_code),
                     t1.business_source_detail_code = NVL(c_t2.business_source_detail_code, t1.business_source_detail_code),
                     t1.channel_source_code = NVL(c_t2.channel_source_code, t1.channel_source_code),
                     t1.channel_source_detail_code = NVL(c_t2.channel_source_detail_code, t1.channel_source_detail_code),
                     t1.due_tax = c_t2.due_tax,
                     t1.total_amount = c_t2.total_amount
               where t1.policy_no = c_t2.policy_no
                 and t1.receipt_no = c_t2.receipt_no
                 and t1.plan_code = c_t2.plan_code;
            elsif i_count1 = 0 then
              insert into tmp_finance_duepremium_long_n
                (department_code,
                 department_name,
                 policy_no,
                 endorse_no,
                 notice_no,
                 due_premium,
                 insurance_begin_date,
                 insurance_end_date,
                 insured_person,
                 due_voucher_no,
                 due_voucher_date,
                 account_days,
                 plan_code,
                 plan_name,
                 currency_code,
                 currency_name,
                 client_code,
                 client_name,
                 sale_agent_code,
                 sale_agent_name,
                 sale_channel_code,
                 sale_channel_name,
                 group_code,
                 group_name,
                 due_premium_sum,
                 cancel_flag,
                 underwrite_time,
                 settle_date,
                 receipt_no,
                 agent_code,
                 agent_chinese_name,
                 payment_end_date,
                 account_month,
                 busi_department_code,
                 business_source_code,
                 business_source_detail_code,
                 channel_source_code,
                 channel_source_detail_code,
                 due_tax,
                 total_amount)
              values
                (c_t2.department_code,
                 c_t2.department_name,
                 c_t2.policy_no,
                 c_t2.endorse_no,
                 c_t2.notice_no,
                 c_t2.due_premium,
                 c_t2.insurance_begin_date,
                 c_t2.insurance_end_date,
                 c_t2.insured_person,
                 c_t2.due_voucher_no,
                 c_t2.due_voucher_date,
                 c_t2.account_days,
                 c_t2.plan_code,
                 c_t2.plan_name,
                 c_t2.currency_code,
                 c_t2.currency_name,
                 c_t2.client_code,
                 c_t2.client_name,
                 c_t2.sale_agent_code,
                 c_t2.sale_agent_name,
                 c_t2.sale_channel_code,
                 c_t2.sale_channel_name,
                 c_t2.group_code,
                 c_t2.group_name,
                 c_t2.due_premium_sum,
                 c_t2.cancel_flag,
                 c_t2.underwrite_time,
                 c_t2.settle_date,
                 c_t2.receipt_no,
                 c_t2.agent_code,
                 c_t2.agent_chinese_name,
                 c_t2.payment_end_date,
                 c_t2.account_month,
                 c_t2.busi_department_code,
                 c_t2.business_source_code,
                 c_t2.business_source_detail_code,
                 c_t2.channel_source_code,
                 c_t2.channel_source_detail_code,
                 c_t2.due_tax,
                 c_t2.total_amount);
            end if;
          end loop c_t2_loop;
          i_count := i_count + 1;
          if i_count >= 5000 then
            commit;
            i_count := 0;
          end if;
        end loop loop_2;
        commit;*/

        --o表回写n表改造
        Open c_o_to_n_long (v_department_code, v_tail_string);
        <<o_to_n_loop>>
        Loop
          Fetch c_o_to_n_long Bulk Collect
            Into v_policy_list_n Limit 5000;
          Exit o_to_n_loop When Not v_policy_list_n.exists(1);

         For i In v_policy_list_n.first .. v_policy_list_n.last loop
            Merge Into tmp_finance_duepremium_long_n t1
            Using (Select a.* From tmp_finance_duepremium_long_o a Where a.policy_no = v_policy_list_n(i)) t2
            On (t1.policy_no = t2.policy_no And t1.receipt_no = t2.receipt_no And t1.plan_code = t2.plan_code)
            When Matched Then
              Update
              Set    t1.department_code             = t2.department_code,
                     t1.department_name             = t2.department_name,
                     t1.endorse_no                  = t2.endorse_no,
                     t1.notice_no                   = t2.notice_no,
                     t1.due_premium                 = t2.due_premium,
                     t1.insurance_begin_date        = t2.insurance_begin_date,
                     t1.insurance_end_date          = t2.insurance_end_date,
                     t1.insured_person              = t2.insured_person,
                     t1.due_voucher_no              = t2.due_voucher_no,
                     t1.due_voucher_date            = t2.due_voucher_date,
                     t1.account_days                = t2.account_days,
                     t1.plan_name                   = t2.plan_name,
                     t1.currency_code               = t2.currency_code,
                     t1.currency_name               = t2.currency_name,
                     t1.client_code                 = t2.client_code,
                     t1.client_name                 = t2.client_name,
                     t1.sale_agent_code             = t2.sale_agent_code,
                     t1.sale_agent_name             = t2.sale_agent_name,
                     t1.sale_channel_code           = t2.sale_channel_code,
                     t1.sale_channel_name           = t2.sale_channel_name,
                     t1.group_code                  = t2.group_code,
                     t1.group_name                  = t2.group_name,
                     t1.due_premium_sum             = t2.due_premium_sum,
                     t1.cancel_flag                 = t2.cancel_flag,
                     t1.underwrite_time             = t2.underwrite_time,
                     t1.settle_date                 = t2.settle_date,
                     t1.agent_code                  = t2.agent_code,
                     t1.agent_chinese_name          = t2.agent_chinese_name,
                     t1.payment_end_date            = t2.payment_end_date,
                     t1.account_month               = t2.account_month,
                     t1.busi_department_code        = NVL(t2.busi_department_code, t1.busi_department_code),
                     t1.business_source_code        = NVL(t2.business_source_code, t1.business_source_code),
                     t1.business_source_detail_code = NVL(t2.business_source_detail_code, t1.business_source_detail_code),
                     t1.channel_source_code         = NVL(t2.channel_source_code, t1.channel_source_code),
                     t1.channel_source_detail_code  = NVL(t2.channel_source_detail_code, t1.channel_source_detail_code),
                     t1.applicant_name              = nvl(t2.applicant_name, t1.applicant_name),
                     t1.due_tax                     = t2.due_tax,
                     t1.total_amount                = t2.total_amount
            When Not Matched Then
              Insert
                (department_code,
                 department_name,
                 policy_no,
                 endorse_no,
                 notice_no,
                 due_premium,
                 insurance_begin_date,
                 insurance_end_date,
                 insured_person,
                 due_voucher_no,
                 due_voucher_date,
                 account_days,
                 plan_code,
                 plan_name,
                 currency_code,
                 currency_name,
                 client_code,
                 client_name,
                 sale_agent_code,
                 sale_agent_name,
                 sale_channel_code,
                 sale_channel_name,
                 group_code,
                 group_name,
                 due_premium_sum,
                 cancel_flag,
                 underwrite_time,
                 settle_date,
                 receipt_no,
                 agent_code,
                 agent_chinese_name,
                 payment_end_date,
                 account_month,
                 busi_department_code,
                 business_source_code,
                 business_source_detail_code,
                 channel_source_code,
                 channel_source_detail_code,
                 applicant_name,
                 due_tax,
                 total_amount)
              Values
                (t2.department_code,
                 t2.department_name,
                 t2.policy_no,
                 t2.endorse_no,
                 t2.notice_no,
                 t2.due_premium,
                 t2.insurance_begin_date,
                 t2.insurance_end_date,
                 t2.insured_person,
                 t2.due_voucher_no,
                 t2.due_voucher_date,
                 t2.account_days,
                 t2.plan_code,
                 t2.plan_name,
                 t2.currency_code,
                 t2.currency_name,
                 t2.client_code,
                 t2.client_name,
                 t2.sale_agent_code,
                 t2.sale_agent_name,
                 t2.sale_channel_code,
                 t2.sale_channel_name,
                 t2.group_code,
                 t2.group_name,
                 t2.due_premium_sum,
                 t2.cancel_flag,
                 t2.underwrite_time,
                 t2.settle_date,
                 t2.receipt_no,
                 t2.agent_code,
                 t2.agent_chinese_name,
                 t2.payment_end_date,
                 t2.account_month,
                 t2.busi_department_code,
                 t2.business_source_code,
                 t2.business_source_detail_code,
                 t2.channel_source_code,
                 t2.channel_source_detail_code,
                 t2.applicant_name,
                 t2.due_tax,
                 t2.total_amount);
        end loop;
        Commit;
        End Loop o_to_n_loop;
        Close c_o_to_n_long;
        Insert Into epcis_job_log
        Values
          (Sysdate,
           4,
           5,
           0,
           '暂无',
           'pro_finance_duepremium_long5回查匹配的完整数据在插入tmp_finance_duepremium_long_n表完成' || v_big_string,sys_guid());
        Commit;


        --删除一正一负金额相等的两条数据 ex-zhaominzhi001
        i_count := 0;
        i_count1 := 0;
        for c_out in (select distinct tt.policy_no
                       from tmp_k_p_update_data_everyday tt
                      where tt.department_code = v_department_code
                         --And isdata = 1 --删除应收标记
                        And substr(POLICY_NO,-1) Like v_tail_string) loop
          <<detail_loop>>
          for c_del in (select t.policy_no,
                               t.insurance_begin_date,
                               t.insurance_end_date,
                               t.payment_end_date,
                               t.total_amount
                          from tmp_finance_duepremium_long_n t
                         where t.policy_no = c_out.policy_no
                           and t.due_premium < 0) loop
            --删除正数
            select count(1)
              into i_count1
              from tmp_finance_duepremium_long_n n
             where n.policy_no = c_del.policy_no
               and n.insurance_begin_date = c_del.insurance_begin_date
               and n.insurance_end_date = c_del.insurance_end_date
               and n.payment_end_date = c_del.payment_end_date
               and n.total_amount = -c_del.total_amount
               and exists (select 1
                      from tmp_finance_duepremium_long_n m
                     where m.policy_no = c_del.policy_no
                       and m.insurance_begin_date = c_del.insurance_begin_date
                       and m.insurance_end_date = c_del.insurance_end_date
                       and m.payment_end_date = c_del.payment_end_date
                       and m.total_amount = c_del.total_amount
                       and rownum = 1)
               and rownum = 1;
            if i_count1 = 1 then
              --删除正数
              delete tmp_finance_duepremium_long_n n
               where n.policy_no = c_del.policy_no
                 and n.insurance_begin_date = c_del.insurance_begin_date
                 and n.insurance_end_date = c_del.insurance_end_date
                 and n.payment_end_date = c_del.payment_end_date
                 and n.total_amount = -c_del.total_amount
                 and rownum = 1;
              --删除负数
              delete tmp_finance_duepremium_long_n n
               where n.policy_no = c_del.policy_no
                 and n.insurance_begin_date = c_del.insurance_begin_date
                 and n.insurance_end_date = c_del.insurance_end_date
                 and n.payment_end_date = c_del.payment_end_date
                 and n.total_amount = c_del.total_amount
                 and rownum = 1;

              --5000 commit
              i_count := i_count + 1;
              if i_count >= 5000 then
                i_count := 0;
                commit;
              end if;

            end if;
          end loop detail_loop;
        end loop;
        commit;

        update etl_department_run a
           set a.flag = 'S', a.end_time = sysdate, a.updated_date = sysdate
         where a.run_date = trunc(sysdate)
           and a.use_project = 'ysjs'
           and a.department_code = v_big_string;
        commit;
      exception
        when others then
          v_errcodeval := sqlcode;
          v_errmsg     := substr(sqlerrm,1,200);
          update etl_department_run a
             set a.flag         = 'E',
                 a.end_time     = sysdate,
                 a.updated_date = sysdate,
                 a.error_msg    = v_errcodeval||';'||v_errmsg
           where a.run_date = trunc(sysdate)
             and a.use_project = 'ysjs'
             and a.department_code = v_big_string;
          commit;
      end;
    end loop;
  exception
    when others then
      v_ErrCodeVal := sqlcode;
      v_ErrMsg     := substr('获取pro_finance_duepremium_long5过程出错2' ||sqlerrm,1,500);
      insert into epcis_job_log
      values
        (sysdate,
         55,
         5,
         v_ErrCodeVal,
         v_ErrMsg,
         'pro_finance_duepremium_long5执行失败！',sys_guid());
      commit;
  end pro_finance_duep_more_course;
   /*******************************************
  --应收计算优化多进程计算优化2011-12-15
    工作原理:1、分机构多进程删除1999-01-01的记录
             2、删除保单合并金额为0的记录
  ********************************************/
  procedure pro_finance_duep_del is
    v_count      number;
    i_count      number;
    i_count1     number;
    v_ErrMsg     varchar2(500);
    v_ErrCodeVal number := 0;

    type p_policy_no is table of tmp_finance_duepremium_long_n.policy_no%type index by binary_integer;
    v_policy_no_n  p_policy_no;

    --用于删除每天1999-01-01之前的保单
    cursor c_loging1 is
      select t.policy_no
        from tmp_k_p_update_data_everyday t
       where t.endorse_no is not null
       group by t.policy_no;
    --用于删除合并金额为0的保单
    cursor c_n is
      select policy_no
        from tmp_finance_duepremium_long_n b
       group by b.policy_no
      having sum(b.total_amount) = 0;

  begin
    --删除批单对应的保单的结算日期在99年1月1日（含）以后
    v_count  := 0;
    i_count  := 0;
    i_count1 := 0;
    --判断是否是初始化1－代表初始化，2－代表非初始化
    for tmp_rec1 in c_loging1 loop
      begin
        select count(*)
          into i_count
          from premium_info t
         where t.policy_no = tmp_rec1.policy_no
           and t.endorse_no is null
           and t.settle_date >= to_date('1999-01-01', 'yyyy-mm-dd')
           and rownum = 1;
        if i_count = 0 or i_count is null then
          select count(*)
            into i_count
            from kcoldue tr
           where tr.cplyno = tmp_rec1.policy_no
             and tr.cedrno is null
             and dcaldte >= to_date('1999-01-01', 'yyyy-mm-dd')
             and rownum = 1;
        end if;
      exception
        when no_data_found then
          i_count := 0;
        when others then
          i_count := 0;
      end;
      if i_count = 0 then
        --删除批单对应的原始保单不符合应收条件数据，删除整个批单对应的保单。
        --modified by liuyifu 20090310修改保单有批单的情况，如果批单对应的保单结算日期在1999年1月1日之前的，
        --这样的数据是整个保单（原始保单和所有批单）都不显示
        delete /*+ direct */
          from tmp_finance_duepremium_long_o a
         where a.policy_no = tmp_rec1.policy_no;

        --同步删除tmp_finance_duepremium_long_n保证两边数据一致
        delete tmp_finance_duepremium_long_n a
         where a.policy_no = tmp_rec1.policy_no;

        v_count := v_count + 1;
        if v_count >= 2000 then
          commit;
          v_count := 0;
        end if;
      end if;
    end loop;

    commit;

    insert into epcis_job_log
    values (sysdate, 5, 21, 0, '暂无', 'pro_finance_duepremium_long2 删除批单对应的原始保单不符合应收条件数据完成',sys_guid());
    commit;

    --删除累积应收为0 的记录,替换上面注释语句
    open c_n;
    loop
      fetch c_n bulk collect
        into v_policy_no_n limit 2000;

      exit when not v_policy_no_n.exists(1);

      forall i in v_policy_no_n.first .. v_policy_no_n.last
        delete /*+ direct */
          from tmp_finance_duepremium_long_n a
         where policy_no = v_policy_no_n(i);
      commit;
    end loop;

    insert into epcis_job_log
    values (sysdate, 6, 22, 0, '暂无', 'pro_finance_duepremium_long2 删除tmp_finance_duepremium_long_n表累积应收为0 的记录完成',sys_guid());
    commit;
    update etl_department_run a
       set a.run_date = a.created_date
     where a.run_date=trunc(sysdate)
       and a.use_project ='ysjs';
    insert into etl_department_run
          (created_date, created_by, updated_date, updated_by, run_date, department_code, flag, begin_time, end_time, error_msg, use_project)
    values(sysdate,'system',sysdate,'system',trunc(sysdate),'delete','S',sysdate,sysdate,'删除数据执行成功','ysjs');
    commit;
    --至此数据更新完毕，可以提供给用户使用
  exception
    when others then
      v_ErrCodeVal := sqlcode;
      v_ErrMsg     := substr('获取pro_finance_duepremium_long2过程出错' || sqlerrm, 1, 500);
      insert into epcis_job_log
      values (sysdate,  22, 2, v_ErrCodeVal, v_ErrMsg, 'pro_finance_duepremium_long2 执行失败!',sys_guid());
      update etl_department_run a
         set a.run_date = a.created_date
       where a.run_date=trunc(sysdate)
         and a.use_project ='ysjs';
      insert into etl_department_run
             (created_date, created_by, updated_date, updated_by, run_date, department_code, flag, begin_time, end_time, error_msg, use_project)
      values(sysdate,'system',sysdate,'system',trunc(sysdate),'delete','E',sysdate,sysdate,substr('删除数据执行失败'||v_ErrCodeVal,1,200),'ysjs');
      commit;
  end pro_finance_duep_del;

--------------------------------------------------------------------------
--created by ex-huangzhiyun001 2012-06-21
--应收保费清单（短期）新   旧清单迁移
--------------------------------------------------------------------------
procedure finance_duepremium_short_new(parameters in varchar2,
                                       re_cursor  out t_cursor) is
    v_cursor t_cursor;
    --过滤参数
    p_user_code       varchar2(20);
    p_department_code varchar2(10);
    p_filter_date1    varchar2(10);
    p_filter_date2    varchar2(10);
    p_filter_date3    varchar2(10);
    p_plan_code       varchar2(20);
    p_emp_code        varchar2(30);
    p_sale_group      varchar2(20);
    p_client_type     varchar2(2);
    p_sale_channel    varchar2(10);
    p_currency_code   varchar2(2);
    p_rateh           varchar2(10);
    p_rateu           varchar2(10);
    --汇率值变量
    v_rateh number(8, 4);
    v_rateu number(8, 4);
    --记录统计变量
    v_count_limit     number(10);
    v_user_loginId    varchar2(100);
    v_k_str           varchar2(3999);
    v_k_str1          varchar2(3999);
    v_k_str2          varchar2(3999);
    v_p_str           varchar2(3999);
    v_p_str1          varchar2(3999);
    v_p_str2          varchar2(3999);
    filter_k          varchar2(2000);
    filter_p          varchar2(2000);
    v_partition       varchar2(100);
    p_plan_class_code varchar2(20);
    i_count           number;
    --v_count  number;
    cursor c_loging is
        select t.policy_no,
               t.endorse_no,
               t.plan_code,
               t.notice_no,
               t.receipt_no
          from epcisacct.tmp_finance_duepremium_short t
         where t.endorse_no is not null;
begin
    --_oLstChoicesmustneed_departmentCode_epcis=010100^_oChoicemustneed_settleDate1_epcis=2008-11-05^txtDatemustneed_settleDate1_epcis=2008-11-5^_oChoicemustneed_settleDate2_epcis=2008-11-05^txtDatemustneed_settleDate2_epcis=2008-11-5^_oChoicemustneed_settleDate3_epcis=2008-11-05^txtDatemustneed_settleDate3_epcis=2008-11-5^_oLstChoicesplanClassCode_epcis=010^_oLstChoicesplanCode_epcis=B01^_textEditBoxsalegroup_epcis=123213^_textEditBoxsaleAgent_code_epcis=4242^_oLstChoicesbusinessType_epcis=1^_oLstChoiceschannel_code_epcis=A^_oLstChoicescurrency_code_epcis=01^_textEditBoxrateH_epcis=0.5^_textEditBoxrateU_epcis=0.2^userName_epcis=weile^
    --分解通过reportnet提示页面获取的参数信息
    p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                       'userName_epcis'); --执行用户
    p_department_code := pkg_ereport.getParameterValue(parameters,
                                                       'finance_department_code_epcis'); --机构
    p_filter_date1    := pkg_ereport.getparametervalue(parameters,
                                                       'mustneed_settleDate1_epcis'); --起始时间
    p_filter_date2    := pkg_ereport.getparametervalue(parameters,
                                                       'mustneed_settleDate2_epcis'); --终止时间
    p_filter_date3    := pkg_ereport.getparametervalue(parameters,
                                                       'mustneed_settleDate3_epcis'); --截止时间
    p_plan_class_code := pkg_ereport.getparametervalue(parameters,
                                                       'planClass_code_epcis'); --险种大类
    p_plan_code       := pkg_ereport.getparametervalue(parameters,
                                                       'plan_code_epcis'); --险种明细
    p_emp_code        := pkg_ereport.getparametervalue(parameters,
                                                       'saleAgent_code_epcis'); --业务员
    p_sale_group      := pkg_ereport.getparametervalue(parameters,
                                                       'saleGroup_code_epcis'); --团队
    p_client_type     := pkg_ereport.getparametervalue(parameters,
                                                       'businessType_epcis'); --客户类型
    p_sale_channel    := pkg_ereport.getparametervalue(parameters,
                                                       'channel_code_epcis'); --渠道
    p_currency_code   := pkg_ereport.getparametervalue(parameters,
                                                       'currency_code_epcis'); --币种
    /* p_rateh := pkg_ereport.getparametervalue(parameters, 'rateH_epcis');  --港币汇率值
    p_rateu := pkg_ereport.getparametervalue(parameters, 'rateU_epcis');  --美元汇率值
    if p_department_code = substr(p_department_code,1,2) || '9999' then
        p_department_code := substr(p_department_code, 1,2);
    elsif p_department_code = substr(p_department_code,1,4) || '99' then
        p_department_code := substr(p_department_code, 1,4);
    end if;*/
    if p_rateh is null then
        v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
    else
        v_rateh := to_number(p_rateh);
    end if;
    if p_rateu is null then
        v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
    else
        v_rateu := to_number(p_rateu);
    end if;
    --根据报表运行的当前时间获取记录条数限制值：8 － 20 点之间，65000，夜间，为200000
    --应用户要求，白天应收短期表放宽直接设定为10001
    if to_char(sysdate, 'hh24') > 7 and to_char(sysdate, 'hh24') < 21 then
        v_count_limit := 65000;
    else
        v_count_limit := 200000;
    end if;

    --根据当前时间组成此次会话的用户id
    v_user_loginId := p_user_code || '-' ||
                      to_char(sysdate, 'yyyymmdd hh24miss');
    --v_count := 0;
    --依据用户输入的机构区分在k表所在的分区
    v_partition := case when substr(p_department_code, 1, 2) < '02' then 'KCOLDUE201'
    when substr(p_department_code, 1, 2) < '03' then 'KCOLDUE202'
    when substr(p_department_code, 1, 2) < '04' then 'KCOLDUE203'
    when substr(p_department_code, 1, 2) < '05' then 'KCOLDUE204'
    when substr(p_department_code, 1, 2) < '06' then 'KCOLDUE205'
    when substr(p_department_code, 1, 2) < '07' then 'KCOLDUE206'
    when substr(p_department_code, 1, 2) < '08' then 'KCOLDUE207'
    when substr(p_department_code, 1, 2) < '09' then 'KCOLDUE208'
    when substr(p_department_code, 1, 2) < '10' then 'KCOLDUE209'
    when substr(p_department_code, 1, 2) < '11' then 'KCOLDUE210'
    when substr(p_department_code, 1, 2) < '12' then 'KCOLDUE211'
    when substr(p_department_code, 1, 2) < '13' then 'KCOLDUE212'
    when substr(p_department_code, 1, 2) < '14' then 'KCOLDUE213'
    when substr(p_department_code, 1, 2) < '15' then 'KCOLDUE214'
    when substr(p_department_code, 1, 2) < '16' then 'KCOLDUE215'
    when substr(p_department_code, 1, 2) < '17' then 'KCOLDUE216'
    when substr(p_department_code, 1, 2) < '18' then 'KCOLDUE217'
    when substr(p_department_code, 1, 2) < '19' then 'KCOLDUE218'
    when substr(p_department_code, 1, 2) < '20' then 'KCOLDUE219'
    when substr(p_department_code, 1, 2) < '21' then 'KCOLDUE220'
    when substr(p_department_code, 1, 2) < '22' then 'KCOLDUE221'
    when substr(p_department_code, 1, 2) < '23' then 'KCOLDUE222'
    when substr(p_department_code, 1, 2) < '24' then 'KCOLDUE223'
    when substr(p_department_code, 1, 2) < '25' then 'KCOLDUE224'
    when substr(p_department_code, 1, 2) < '26' then 'KCOLDUE225'
    when substr(p_department_code, 1, 2) < '27' then 'KCOLDUE226'
    when substr(p_department_code, 1, 2) < '28' then 'KCOLDUE227'
    when substr(p_department_code, 1, 2) < '29' then 'KCOLDUE228'
    when substr(p_department_code, 1, 2) < '30' then 'KCOLDUE229'
    when substr(p_department_code, 1, 2) < '31' then 'KCOLDUE230'
    when substr(p_department_code, 1, 2) < '32' then 'KCOLDUE231'
    when substr(p_department_code, 1, 2) < '33' then 'KCOLDUE232'
    when substr(p_department_code, 1, 2) < '34' then 'KCOLDUE233'
    when substr(p_department_code, 1, 2) < '35' then 'KCOLDUE234'
    when substr(p_department_code, 1, 2) < '36' then 'KCOLDUE235'
    when substr(p_department_code, 1, 2) < '37' then 'KCOLDUE236'
    when substr(p_department_code, 1, 2) < '42' then 'KCOLDUE241'
    when substr(p_department_code, 1, 2) < '43' then 'KCOLDUE242'
    when substr(p_department_code, 1, 2) < '44' then 'KCOLDUE243'
    when substr(p_department_code, 1, 2) < '45' then 'KCOLDUE244'
    when substr(p_department_code, 1, 2) < '46' then 'KCOLDUE245'
    when substr(p_department_code, 1, 2) < '47' then 'KCOLDUE246'
    when substr(p_department_code, 1, 2) < '48' then 'KCOLDUE247'
    when substr(p_department_code, 1, 2) < '49' then 'KCOLDUE248'
    else 'KCOLDUEMAX' end;
    -------------------------------------------------------
    --对于k表则直接将查询后的数据写入到临时表中
    v_k_str  := 'insert into epcisacct.tmp_finance_duepremium_short(
                            department_code,
                           department_name,
                           policy_no,
                           endorse_no,
                           due_premium,
                           insurance_begin_date,
                           insurance_end_date,
                           insured_person,
                           due_voucher_no,
                           due_voucher_date,
                           account_month,
                           account_days,
                           plan_name,
                           client_name,
                           sale_channel,
                           sale_group,
                           emp_name,
                           currency,
                           due_premium_sum,
                           cancel_flag,
                           underwrite_time,
                           user_loginid,
                           notice_no,
                           settle_date,
                           receipt_no,
                           plan_code,
                           agent_code,
                           agent_chinese_name,
                           payment_end_date,
                           applicant_name
                           )
              select /*+index(t IDX_KCOLDUE_FUN1)*/ t.cdno,
                     (select description from institutions
                       where flex_value = t.cdno) as v_department_name,
                     t.cplyno,
                     t.cedrno,
                     t.nprmdue,
                     t.dplystr,
                     t.dplyend,
                     t.cpaynme,
                     t.caccno,
                     t.daccdte,
                     to_char(round(months_between(to_date(''' ||
                p_filter_date3 ||
                ''', ''yyyy-mm-dd''), greatest(dpayend,daccdte)),3)) kaccountmonth, --月帐龄
                     to_char(round(to_date(''' ||
                p_filter_date3 ||
                ''', ''yyyy-mm-dd'') - greatest(dpayend,daccdte),3)) kaccountdays, --日帐龄
                     (select plan_chinese_name from plan_define
                       where plan_code = t.cinscde) as  v_plan_name,
                     case when substr(t.c_magic_set,2,1) = ''1'' then ''个体'' else ''团体'' end as v_client_name,
                     (select bnocnm from business_source where bno = substr(t.c_magic_set,1,1) and rownum = 1) as  v_salechnl_name,
                     t.cparno || ''-'' || ( select cgrpcnm from kgrpcde where cgrpcde = t.cparno and rownum = 1),
                     t.cempcde || ''-'' || (select cempcnm from kempcde where cempcde = t.cempcde and rownum = 1) as emp_n_c,
                     (select currency_chinese_name from currency_define
                       where currency_code = t.ccurno) as v_currency_name,
                     case when t.ccurno = ''02'' then ' ||
                v_rateh ||
                ' * t.nprmdue
                          when t.ccurno = ''03'' then ' ||
                v_rateu || ' * t.nprmdue
                        else t.nprmdue
                     end as v_duepremium_sum,
                     t.cancel,
                     t.dfcd,''' || v_user_loginID || ''',
                     t.ntermno,
                     t.dcaldte,
                     t.crctno receipt_no,
                     t.cinscde plan_code,
                     nvl(t.cagtcde,t.cbrkcde) agent_code,
                     nvl((select c.agent_chinese_name
                        from agent_define c
                       where c.agent_code = t.cagtcde),
                       (select c.broker_chn_name
                        from broker_info c
                       where c.broker_code = t.cbrkcde)) agent_chinese_name,
                     dpayend as payment_end_date,
                     null
              from kcoldue partition (' || v_partition ||
                ') t ';
    v_k_str1 := 'where cdno like ''' || p_department_code || '%''
                  and dcoldte > to_date(''' ||
                p_filter_date3 || ' ' ||
                '235959'', ''yyyy-mm-dd hh24miss'')
                  and greatest(dplystr, dfcd) >= to_date(''' ||
                p_filter_date1 ||
                ''',''yyyy-mm-dd'')
                  and greatest(dplystr, dfcd) <= to_date(''' ||
                p_filter_date2 || ' ' ||
                '235959'', ''yyyy-mm-dd hh24miss'')
                  and cinscde <> ''A24''
                  and nvl(hide_flag, ''N'') <> ''Y''
                  and daccdte >= to_date(''19990101'',''yyyymmdd'')
                  and rownum<='||v_count_limit||' ';
    v_k_str2 := 'where cdno like ''' || p_department_code || '%''
                  and dcoldte is null        --实收制证日期
                  and caccno is not null            --记帐编号
                  and greatest(dplystr, dfcd) >= to_date(''' ||
                p_filter_date1 ||
                ''',''yyyy-mm-dd'')
                  and greatest(dplystr, dfcd) <= to_date(''' ||
                p_filter_date2 || ' ' ||
                '235959'', ''yyyy-mm-dd hh24miss'')
                  and cinscde <> ''A24''
                  and nvl(hide_flag, ''N'') <> ''Y''
                  and daccdte >= to_date(''19990101'',''yyyymmdd'')
                  and rownum<='||v_count_limit||' ';
    --组织filter过滤
    filter_k := ' ';
    --险种
    if p_plan_code is not null then
        filter_k := filter_k || ' and cinscde  = ''' || p_plan_code ||
                    ''' ';
    elsif p_plan_class_code is not null then
        filter_k := filter_k ||
                    ' and cinscde in (select distinct b.plan_code
                                                 from ereport_plan_map_info b,
                                                      plan_define c
                                                where c.plan_code=b.plan_code
                                                  and b.ereport_plan_class_code = ''' ||
                    p_plan_class_code || ''') ';
    end if;
    --业务员
    if p_emp_code is not null then
        filter_k := filter_k || ' and cempcde  =''' || p_emp_code || ''' ';
    end if;
    --团队
    if p_sale_group is not null then
        filter_k := filter_k || ' and cparno  = ''' || p_sale_group ||
                    ''' ';
    end if;
    --客户
    if p_client_type is not null then
        filter_k := filter_k ||
                    ' and decode(substr(c_magic_set,2,1), ''0'', ''2'', substr(c_magic_set,2,1)) =''' ||
                    p_client_type || ''' ';
    end if;
    --渠道
    if p_sale_channel is not null then
        filter_k := filter_k || ' and substr(c_magic_set,1,1) =''' ||
                    p_sale_channel || ''' ';
    end if;
    --币种
    if p_currency_code is not null then
        filter_k := filter_k || ' and ccurno  = ''' || p_currency_code ||
                    ''' ';
    end if;
    --对p表数据直接写入临时表
    v_p_str  := 'insert into epcisacct.tmp_finance_duepremium_short(
                           department_code,
                           department_name,
                           policy_no,
                           endorse_no,
                           due_premium,
                           insurance_begin_date,
                           insurance_end_date,
                           insured_person,
                           due_voucher_no,
                           due_voucher_date,
                           account_month,
                           account_days,
                           plan_name,
                           client_name,
                           sale_channel,
                           sale_group,
                           emp_name,
                           currency,
                           due_premium_sum,
                           cancel_flag,
                           underwrite_time,
                           user_loginid,
                           notice_no,
                           settle_date,
                           receipt_no,
                           plan_code,
                           agent_code,
                           agent_chinese_name,
                           payment_end_date,
                           applicant_name
                           )
               select a.finance_department_code,
                      (select description from institutions
                        where flex_value = a.finance_department_code and rownum = 1) as v_department_name,
                      a.policy_no,
                      a.endorse_no,
                      b.premium_amount,
                      a.insurance_begin_time,
                      a.insurance_end_time,
                      a.insured_name,
                      a.due_voucher_no,
                      a.due_voucher_date,
                      to_char(round(months_between(to_date(''' ||
                p_filter_date3 ||
                ''', ''yyyy-mm-dd''), greatest(payment_end_date ,underwrite_time)),3)) as kaccountmonth,  --月帐龄
                      to_char(round(to_date(''' ||
                p_filter_date3 ||
                ''', ''yyyy-mm-dd'') - greatest(payment_end_date ,underwrite_time),3)) as kaccountdays,   --日帐龄
                      (select plan_chinese_name from plan_define
                        where plan_code = b.plan_code) as  v_plan_name,
                      case when a.client_attribute = ''1'' then ''个体'' else ''团体'' end as v_client_name,
                      (select bnocnm from business_source where bno = a.channel_source_code and rownum = 1) as v_salechnl_name,
                      a.group_code || ''-'' || (select cgrpcnm from kgrpcde where cgrpcde = a.group_code and rownum=1),
                      a.sale_agent_code || ''-'' || (select cempcnm from kempcde where cempcde = a.sale_agent_code and rownum=1),
                      (select currency_chinese_name from currency_define
                        where currency_code = a.currency_code) as v_currency_name,
                      case when a.currency_code = ''02'' then ' ||
                v_rateh ||
                ' * b.premium_amount
                           when a.currency_code = ''03'' then ' ||
                v_rateu || ' * b.premium_amount
                           else b.premium_amount
                      end as v_duepremium_sum,
                      a.disable_flag,
                      a.underwrite_time,''' ||
                v_user_loginid || ''',
                      a.term_no,
                      a.settle_date,
                      a.receipt_no,
                      b.plan_code,


                      nvl(a.agent_code,a.broker_code),
                      nvl((select c.agent_chinese_name
                          from agent_define c
                         where c.agent_code = a.agent_code),
                         (select t.broker_chn_name
                          from broker_info t
                         where t.broker_code = a.broker_code)) agent_chinese_name,
                      payment_end_date,
                      null
                 from premium_info a, premium_plan b ';
    v_p_str1 := 'where finance_department_code like ''' ||
                p_department_code || '%''
                  and actual_voucher_no is null
                  and due_voucher_no is not null
                  and greatest(insurance_begin_time,underwrite_time)  >= to_date(''' ||
                p_filter_date1 ||
                ''',''yyyy-mm-dd'')
                  and greatest(insurance_begin_time,underwrite_time)  <= to_date(''' ||
                p_filter_date2 || ' ' ||
                '235959'', ''yyyy-mm-dd hh24miss'')
                  and a.receipt_no = b.receipt_no
                  and b.plan_code <> ''A24''
                  and a.due_voucher_date >= to_date(''19990101'',''yyyy-mm-dd'')
                  and rownum<='||v_count_limit||' ';
    v_p_str2 := 'where finance_department_code like ''' ||
                p_department_code || '%''
                  and actual_voucher_date > to_date(''' ||
                p_filter_date3 || ' ' ||
                '235959'', ''yyyy-mm-dd hh24miss'')
                  and greatest(insurance_begin_time,underwrite_time)  >= to_date(''' ||
                p_filter_date1 ||
                ''',''yyyy-mm-dd'')
                  and greatest(insurance_begin_time,underwrite_time)  <= to_date(''' ||
                p_filter_date2 || ' ' ||
                '235959'',''yyyy-mm-dd hh24miss'')
                  and a.receipt_no = b.receipt_no
                  and b.plan_code <> ''A24''
                  and a.due_voucher_date >= to_date(''19990101'',''yyyy-mm-dd'')
                  and rownum<='||v_count_limit||' ';
    --组织filter过滤
    filter_p := ' ';
    --险种
    if p_plan_code is not null then
        filter_p := filter_p || ' and b.plan_code  = ''' || p_plan_code ||
                    ''' ';
    elsif p_plan_class_code is not null then
        filter_p := filter_p ||
                    ' and b.plan_code in (select distinct b.plan_code
                                                 from ereport_plan_map_info b,
                                                      plan_define c
                                                where c.plan_code=b.plan_code
                                                  and b.ereport_plan_class_code = ''' ||
                    p_plan_class_code || ''') ';
    end if;
    --业务员
    if p_emp_code is not null then
        filter_p := filter_p || ' and a.sale_agent_code  =''' || p_emp_code ||
                    ''' ';
    end if;
    --团队
    if p_sale_group is not null then
        filter_p := filter_p || ' and a.group_code  = ''' || p_sale_group ||
                    ''' ';
    end if;
    --客户
    if p_client_type is not null then
        filter_p := filter_p ||
                    ' and decode(a.client_attribute, ''0'', ''2'', a.client_attribute) =''' ||
                    p_client_type || ''' ';
    end if;
    --渠道
    if p_sale_channel is not null then
        filter_p := filter_p || ' and a.channel_source_code =''' ||
                    p_sale_channel || ''' ';
    end if;
    --币种
    if p_currency_code is not null then
        filter_p := filter_p || ' and a.currency_code  = ''' ||
                    p_currency_code || ''' ';
    end if;
    execute immediate v_k_str || v_k_str1 || filter_k;
    execute immediate v_k_str || v_k_str2 || filter_k;
    execute immediate v_p_str || v_p_str1 || filter_p;
    execute immediate v_p_str || v_p_str2 || filter_p;

    --删除批单对应的原始保单的结算日期不在99年1月1日（含）以后的数据
    i_count := 0;
    for tmp_rec in c_loging loop
        begin
            select count(*)
              into i_count
              from premium_info t
             where t.policy_no = tmp_rec.policy_no
               and t.endorse_no is null
               and t.settle_date >= to_date('1999-01-01', 'yyyy-mm-dd')
               and rownum = 1;
            if i_count = 0 or i_count is null then
                select count(*)
                  into i_count
                  from kcoldue tr
                 where tr.cplyno = tmp_rec.policy_no
                   and tr.cedrno is null
                   and dcaldte >= to_date('1999-01-01', 'yyyy-mm-dd')
                   and rownum = 1;
            end if;
        exception
            when no_data_found then
                i_count := 0;
            when others then
                i_count := 0;
        end;
        if i_count = 0 then
            --删除批单对应的原始保单不符合应收条件数据，删除这个批单。
            ----modified by liuyifu 20090310修改保单有批单的情况，如果批单对应的保单结算日期在1999年1月1日之前的，这样的数据是整个保单（原始保单和所有批单）都不显示
            delete /*+ direct */
            from epcisacct.tmp_finance_duepremium_short a
             where a.policy_no = tmp_rec.policy_no;
            /*             and a.plan_code = tmp_rec.plan_code
            and nvl(trim(a.endorse_no),'null') = nvl(trim(tmp_rec.endorse_no),'null')
            and a.notice_no = tmp_rec.notice_no
            and a.receipt_no = tmp_rec.receipt_no;*/
        end if;
    end loop;
    --删除累积应收为0 的记录
    delete from epcisacct.tmp_finance_duepremium_short a
     where policy_no in (select policy_no
                           from epcisacct.tmp_finance_duepremium_short b
                          group by b.policy_no
                         having sum(b.due_premium) = 0);
    --  -- 删除注销累积应收为 0 的纪录
    --  delete from epcisacct.tmp_finance_duepremium_short a
    --        where POLICY_NO in (
    --                      select POLICY_NO from epcisacct.tmp_finance_duepremium_short b
    --                          where b.CANCEL_flag = 'Y' group by b.POLICY_NO having sum(b.DUE_PREMIUM) = 0
    --                           ) and a.CANCEL_flag = 'Y';
    open v_cursor for
        select t.department_code as 机构代码,
               t.department_name as 机构名称,
               t.sale_group      as 团队,
               t.emp_name        as 业务员,

               t.policy_no as 保单号,
               t.endorse_no as 批单号,
               t.insured_person as 被保险人,
               t.due_premium as 金额,
               t.due_voucher_no as 应收凭证号,
               t.due_voucher_date as 应收凭证日期,
               t.insurance_begin_date as 保险起期,
               t.insurance_end_date as 保险止期,
               t.underwrite_time as 核保日期,
               t.payment_end_date as 缴费止期,
               t.settle_date as 结算日期,
               t.notice_no as 期次,
               case
                   when to_number(t.account_month) <= 3 then
                    '3个月以内'
                   when to_number(t.account_month) > 3 and
                        to_number(t.account_month) <= 6 then
                    '3到6个月'
                   when to_number(t.account_month) > 6 and
                        to_number(t.account_month) <= 12 then
                    '6到12个月'
                   when to_number(t.account_month) > 12 then
                    '12个月以上'
               end as 账龄,
               t.account_days as 账龄a,
               t.plan_name as 险种,
               t.sale_channel as 渠道,
               t.client_name as 客户,
               --t.agent_chinese_name as 代理人或经纪人,
               nvl(t.agent_chinese_name,(select a.broker_chn_name
                from broker_info a
               where a.broker_code in
                     (select b.broker_code from commission_info b
                     where b.policy_no = t.policy_no)
                and rownum = 1)) as 代理人或经纪人,
               t.currency as 币种,
               t.due_premium_sum as 折合人民币,
               --cancel_flag,
               --receipt_no,
               --plan_code,
               -- agent_code,
               t.applicant_name as 投保人
          from epcisacct.tmp_finance_duepremium_short t
         where rownum <= v_count_limit;
    re_cursor := v_cursor;
exception
    when others then
        rollback;
end finance_duepremium_short_new;

------------------------------------------------------------------------------------------------------
--created by ex-huangzhiyun001 2012-06-21
--应收保费清单（长期）  旧清单迁移
-----------------------------------------------------------------------------------------------------
  procedure finance_duepremium_long_re_new(parameters in varchar2,
                                           re_cursor  out t_cursor) is
    v_cursor      t_cursor;
    filter        varchar2(3600); --过滤
    sel           varchar2(5000); --选择项
    sql_statement varchar2(8600); --Select语句
    --过滤参数
    p_user_code       varchar2(20);
    p_department_code varchar2(10);
    p_plan_code       varchar2(20);
    p_plan_class_code varchar2(20);
    p_emp_code        varchar2(30);
    p_sale_group      varchar2(20);
    p_client_type     varchar2(2);
    p_sale_channel    varchar2(10);
    p_currency_code   varchar2(2);
    p_rateh           varchar2(10);
    p_rateu           varchar2(10);
    p_approach          varchar2(1);   --1-监管口径 2-考核口径
    p_group_include     varchar2(1);   --包含/不包含 团队
    p_agent_include     varchar2(1);   --包含/不包含 业务员
    p_channel_include   varchar2(1);   --包含/不包含 渠道

    --汇率值变量
    v_rateh number(8, 4);
    v_rateu number(8, 4);
    data_not_prepared exception;
    --用户登录参数
    v_user_loginId varchar2(100);
    counterlimit   number(10); --报表查询记录条数限制
    v_ErrMsg       varchar2(500);
    v_sql_plan_class          varchar2(500); --查询的险种大类
    v_special_code varchar2(50);
  begin
    --先判断能否从临时表TMP_FINANCE_DUEPREMIUM_LONG_O 中取数
    /*  select parameter_value into v_fetch_flag from control_parameter where parameter_name = 'duepremium_long_flag';
    if v_fetch_flag = '0' then
        raise data_not_prepared;
    end if;*/
    --报表查询记录条数限制,直接控制到excel最大接受值
    --  counterlimit := pkg_ereport.getcountlimit;
    --晚上开放至20万
    If to_char(Sysdate, 'hh24') < '21' And to_char(Sysdate, 'hh24') > '07' Then
      counterlimit := 65000;
    Else
    --counterlimit := 200000;
      counterlimit := 500000;
    End If;

    /*机构：明细三级机构财务代码，如010100、010200等
    起止日期：保批单保险起期与核保日期的最大值所在范围(长期没有起止日期)
    截止日期：实收凭证日期
    团队：团队代码
    业务员：业务员代码
    险种：分车、财、意三大险种，三大险种下可以继续细分明细险种
    客户：分个人客户、团体客户
    渠道：分综合开拓、传统渠道、新渠道三大渠道，三大渠道下可细分明细渠道
    币种：人民币、港币、美元*/
    --分解通过reportnet提示页面获取的参数信息
    p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                       'userName_epcis'); --执行用户
    p_department_code := pkg_ereport.getParameterValue(parameters,
                                                       'finance_department_code_epcis'); --机构
    p_plan_class_code := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'planClass_code_epcis'); --险种大类
    p_plan_code       := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'plan_code_epcis'); --险种
    p_emp_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleAgent_code_epcis'); --业务员
    p_sale_group      := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleGroup_code_epcis'); --团队
    p_client_type     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'businessType_epcis'); --客户类型
    p_client_type     := trim(p_client_type);
    p_sale_channel    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'channel_code_epcis'); --渠道
    p_currency_code   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'currency_code_epcis'); --币种
    p_rateh           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateH_epcis'); --港币汇率值
    p_rateu           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateU_epcis'); --美元汇率值
    v_user_loginId    := p_user_code || '-' ||
                         to_char(sysdate, 'yyyymmdd hh24miss'); --根据当前时间组成此次会话的用户id
    p_approach          := '2';/*pkg_ereport.getparametervalue(PARAMETERS,
                                                       'approach');*/
    p_group_include     := pkg_ereport.getparametervalue(PARAMETERS,
                                                         'group_include');
    p_agent_include     := pkg_ereport.getparametervalue(PARAMETERS,
                                                         'agent_include');
    p_channel_include   := pkg_ereport.getparametervalue(PARAMETERS,
                                                         'channel_include');
     v_special_code :=   p_department_code;
    --财务机构如果是汇总机构则做相应处理
    if p_department_code = substr(p_department_code, 1, 2) || '9999' then
      p_department_code := substr(p_department_code, 1, 2);
    elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
      p_department_code := substr(p_department_code, 1, 4);
    end if;
    --若用户没有在提示页面输入汇率，则从系统表中取汇率值
    if p_rateh is null then
      v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
    else
      v_rateh := to_number(p_rateh);
    end if;
    if p_rateu is null then
      v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
    else
      v_rateu := to_number(p_rateu);
    end if;
    --组织filter过滤
    filter := '';
    --机构（必选）
    filter := filter || ' where ((department_code like (:p7 ||''%'') and department_code not in (select distinct n.department_code from nps_paramter_define n where n.class_code = ''spec2ndFinanceDept''  and n.department_code like (decode(length(:p8),6,''null'',1,''null'',:p9||''%'')))) or department_code in ( select np.department_code from nps_paramter_define np where np.class_code = ''spec2ndFinanceDept''and np.value_code = (:p10)))';


    --险种
    v_sql_plan_class := ' and exists (select 1
          from finance_plan_relation fr, finance_plan_class f, produce_map p
         where fr.parent_plan_code = f.plan_code
           and p.produce = fr.child_plan_code
           and p.c_ins_no = c_u.plan_code';
    if p_approach = '1' then
      filter := filter || v_sql_plan_class || ' and f.plan_type = ''01''';
      if p_plan_class_code is null then
        filter := filter ||
           ' and f.plan_code in (''099998'', ''099997'', ''900002'', ''900009'', ''900001'', ''900008'', ''900005'', ''900011'',' ||
           '''900007'', ''900003'', ''900010'', ''900004'', ''900006'')) ';
      else
        filter := filter || ' and  f.plan_code = ''' || p_plan_class_code || ''') ';
      end if;
    elsif p_approach = '2' then
      filter := filter || v_sql_plan_class || ') ';
      if p_plan_class_code is not null then
        --filter := filter || v_sql_plan_class || ' and  f.plan_code = ''' || p_plan_class_code || ''') ';
        filter := filter ||
          ' and exists (select 1 from produce_map t where t.c_ins_no = plan_code' ||
          ' and decode(t.plan_class, ''01'', ''A'', ''02'', ''B'', ''03'', ''C'', ''B'') = ''' ||
            p_plan_class_code || ''') ';

      end if;
    end if;
    if p_plan_code is not null then
      filter := filter || ' and plan_code = ''' || p_plan_code || ''' ';
    --elsif p_plan_class_code is not null then
    --  filter := filter ||
               /*   ' and plan_code in (Select p1.plan_code
                                                                                                                            From plan_class_relation p1, plan_define p2
                                                                                                                           Where plan_class_code In (Select p.plan_class_code From plan_class p Where p.plan_class_standard = 1)
                                                                                                                             And plan_class_code In (''J'', ''K'') And p1.plan_code = p2.plan_code
                                                                                                                             And ''C'' ='''|| p_plan_class_code || '''
                                                                                                                          Union
                                                                                                                          Select p1.plan_code
                                                                                                                            From plan_class_relation p1, plan_define p2
                                                                                                                           Where plan_class_code In (Select p.plan_class_code From plan_class p Where p.plan_class_standard = 2)
                                                                                                                             And plan_class_code Not In (''SC'', ''SJ'', ''SK'') And p1.plan_code = p2.plan_code
                                                                                                                             And ''B'' = '''|| p_plan_class_code || '''
                                                                                                                          Union
                                                                                                                          Select p1.plan_code
                                                                                                                            From plan_class_relation p1, plan_define p2
                                                                                                                           Where plan_class_code In (Select p.plan_class_code From plan_class p Where p.plan_class_standard = 2)
                                                                                                                             And plan_class_code In (''SC'')  And p1.plan_code = p2.plan_code
                                                                                                                             And ''A'' = '''|| p_plan_class_code || ''') ';*/
    --            ' and plan_code in (Select t.c_ins_no From produce_map t where ' ||
    --            ' decode(t.plan_class, ''01'', ''A'', ''02'', ''B'', ''03'', ''C'', ''B'') = ''' ||
    --            p_plan_class_code || ''') ';
    end if;
    --业务员
    if p_agent_include = '0' then
      if p_emp_code is not null then
        filter := filter || ' and nvl(sale_agent_code, ''null'') <>''' || p_emp_code || ''' ';
      end if;
    else
      if p_emp_code is not null then
        filter := filter || ' and nvl(sale_agent_code, ''null'') =''' || p_emp_code || ''' ';
      end if;
    end if;
    --团队
--    if p_group_include = '0' then
--      if p_sale_group is not null then
--        filter := filter || ' and nvl(group_code, ''null'') <> ''' || p_sale_group || ''' ';
--      end if;
--    else
--      if p_sale_group is not null then
--        filter := filter || ' and nvl(group_code, ''null'') = ''' || p_sale_group || ''' ';
--      end if;
--    end if;
    --客户
    if p_client_type is not null then
      filter := filter ||
                ' and decode(client_code, ''2'', ''0'', client_code) =''' ||
                p_client_type || ''' ';
    end if;
    --渠道
    if p_channel_include = '0' then
      if p_sale_channel is not null then
        filter := filter --|| ' and sale_channel_code =''' || p_sale_channel ||
                  || ' and nvl(sale_channel_code, ''null'') not In (Select b.flex_value ' ||
                  '  From channel_info b Where b.channel_eight_role_code = ''' ||
                  p_sale_channel || ''') ';
      end if;
    else
      if p_sale_channel is not null then
        filter := filter --|| ' and sale_channel_code =''' || p_sale_channel ||
                  || ' and nvl(sale_channel_code, ''null'') In (Select b.flex_value ' ||
                  '  From channel_info b Where b.channel_eight_role_code = ''' ||
                  p_sale_channel || ''') ';
      end if;
    end if;
    --币种
    if p_currency_code is not null then
      filter := filter || ' and currency_code = ''' || p_currency_code ||
                ''' ';
    end if;
    --增加每次返回条数限制
    filter := filter || ' and rownum <= ' || counterlimit;
    sel    := ' insert into tmp_finance_duepremium_short ' ||
              ' (department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date,insured_person, ' ||
              ' due_voucher_no,due_voucher_date,account_days,account_month,plan_name,client_name, ' ||
              ' sale_channel,sale_group,emp_name, ' ||
              ' currency,due_premium_sum,cancel_flag, ' ||
              ' underwrite_time,notice_no,settle_date,
              receipt_no, plan_code, agent_code,agent_chinese_name,payment_end_date,applicant_name, plan_class_code,receivable_type, ' ||
              'invoice_amount,invoice_amount_sum,invoice_rmb,invoice_rmb_sum) ' ||
              ' select department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date, insured_person, ' ||
              ' due_voucher_no,due_voucher_date,
              to_char(round(sysdate - greatest(payment_end_date ,underwrite_time),0)) as account_day,
              case when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 3 then ''3个月以内''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 3
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 6 then ''3到6个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 6
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 12 then ''6到12个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 12 then ''12个月以上''
               end account_month, nvl(plan_name, (select pro_descr from produce_map where c_ins_no=plan_code)),CLIENT_NAME,  ' ||
              ' sale_channel_code, group_code|| ''-'' ||group_name,sale_agent_code|| ''-'' ||sale_agent_name, ' ||
              ' currency_name,decode(currency_code, ''02'', due_premium * :p1, ''03'', due_premium * :p2
              , due_premium),cancel_flag , ' || ' underwrite_time,notice_no,settle_date, ' ||
              ' receipt_no, plan_code, agent_code, agent_chinese_name, payment_end_date,' ||
             /* '  case--按批单号去视图查投保
                                                                                                                     when endorse_no is not null then
                                                                                                                    (select APPLICANTNAME
                                                                                                                     from odsdata.get_applicant_name_vw
                                                                                                                    where POLICYNO = endorse_no
                                                                                                                    and rownum=1)
                                                                                                                   else
                                                                                                                   (select APPLICANTNAME
                                                                                                                     from odsdata.get_applicant_name_vw
                                                                                                                    where POLICYNO = policy_no
                                                                                                                    and rownum=1)
                                                                                                                      end,   '||*/
              'epciscde.pkg_finance_pub_tools.getApplicantByPolicyEndorse(policy_no,endorse_no), --投保人
               (Select Distinct t.plan_class plan_class_code
                  From produce_map t
                 where t.c_ins_no = c_u.plan_code),  ' ||
              'sign(Sum(c_u.due_premium) Over(Partition By c_u.policy_no)),  ' ||
              'nvl((select ptp.amount
                   from premium_tax_plan ptp
                  where ptp.receipt_no = c_u.receipt_no
                    and ptp.plan_code = c_u.plan_code),
                 0),
             (nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = c_u.receipt_no
                     and p.plan_code = c_u.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = c_u.receipt_no
                               and ptp.plan_code = c_u.plan_code),
                            0)),' ||
              'decode(currency_code, ''02'', :p3, ''03'', :p4, ''1'')
              * nvl((select ptp.amount
                   from premium_tax_plan ptp
                  where ptp.receipt_no = c_u.receipt_no
                    and ptp.plan_code = c_u.plan_code),
                 0),
             decode(currency_code, ''02'', :p5, ''03'', :p6, ''1'')
             * (nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = c_u.receipt_no
                     and p.plan_code = c_u.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = c_u.receipt_no
                               and ptp.plan_code = c_u.plan_code),
                            0)) ' ||
              '  from EPCISACCT.tmp_finance_duepremium_long_n c_u';

    /* 取代理人名称字段
    ||' case when (select ''x'' from premium_info a where receipt_no = c_u.receipt_no) is null
                       then (select (select c.agent_chinese_name
                                       from agent_define c where c.agent_code = a.cagtcde) agent_chinese_name
                               from ep_kcoldue a where crctno = c_u.receipt_no )
                       else (select (select c.agent_chinese_name
                                       from agent_define c where c.agent_code = a.agent_code) agent_chinese_name
                               from premium_info a where receipt_no = c_u.receipt_no)
                  end as agent_name,*/

    --sql组合
    sql_statement := sel || filter;

    execute immediate sql_statement using v_rateh,v_rateu,v_rateh,v_rateu,v_rateh,v_rateu,p_department_code,p_department_code,p_department_code,v_special_code;

    --insert into test1(runtime,sqlmess) values(sysdate,sql_statement);

    --打开游标返回
    open v_cursor for
      Select department_code      As 机构代码,
             department_name      As 机构名称,
             -- sale_group           As 团队,
             emp_name             As 业务员,
             policy_no            As 保单号,
             endorse_no           As 批单号,
             applicant_name       As 投保人,
             insured_person       As 被保险人,
             due_premium          As 金额,
             due_voucher_no       As 应收凭证号,
             due_voucher_date     As 应收凭证日期,
             insurance_begin_date As 保险起期,
             insurance_end_date   As 保险止期,
             underwrite_time      As 核保日期,
             payment_end_date     As 缴费止期,
             settle_date          As 结算日期,
             notice_no            As 期次,
             account_month        As 账龄,
             account_days         As 账龄a,

             nvl(plan_name,
                 (Select a.pro_descr
                    From produce_map a
                   Where a.c_ins_no = tmp.plan_code
                     And rownum = 1)) As 险种,
             Case
               When length(tmp.sale_channel) = 1 Then
                (select bnocnm
                   from business_source
                  where bno = tmp.sale_channel
                    and rownum = 1)
               Else
                (Select c.value_chinese_name
                   From channel_info a, common_parameter c
                  Where a.channel_eight_role_code = c.value_code
                    And c.collection_code = 'QDDM03'
                    And a.flex_value = tmp.sale_channel
                    And rownum = 1)
             End As 渠道,
             client_name As 客户,
             --agent_chinese_name as 代理人或经纪人,
             nvl(agent_chinese_name,
                 (Select a.broker_chn_name
                    From broker_info a
                   Where a.broker_code = (Select b.broker_code
                                            From commission_info b
                                           Where b.policy_no = tmp.policy_no
                                             And rownum = 1)
                     And rownum = 1)) As 代理人或经纪人,
             currency As 币种,
             due_premium_sum As 折合人民币,
              case
                 when p_approach = '1' then
                  (select f.plan_description
                    from finance_plan_relation fr,
                         finance_plan_class    f,
                         produce_map           p
                   where fr.parent_plan_code = f.plan_code
                     and p.produce = fr.child_plan_code
                     and p.c_ins_no = tmp.plan_code
                     and f.plan_type = '01'
                     and rownum = 1)
                 else
                  (select decode(p.plan_class,
                                  '01',
                                  '车险',
                                  '02',
                                  '财产险',
                                  '03',
                                  '意健险',
                                  '财产险')
                     from produce_map p
                    where p.c_ins_no = tmp.plan_code)
               end as 险种大类,
             /*decode(plan_class_code,
                    '01',
                    '车险',
                    '02',
                    '财产险',
                    '03',
                    '意健险',
                    '财产险') As 险种大类,*/
             decode(receivable_type,
                    '1',
                    '正数应收',
                    '-1',
                    '负数应收',
                    '正数应收') As 应收保费类型,
             tmp.invoice_amount as 应收增值税额,
             tmp.invoice_rmb as "应收增值税额（人民币）",
             decode(tmp.invoice_amount_sum,0,due_premium,tmp.invoice_amount_sum) as 合计,
             decode(tmp.invoice_rmb_sum,0,due_premium_sum,tmp.invoice_rmb_sum) as "合计（人民币）",
             replace((select f.plan_description from finance_plan_class f where f.plan_type='02' and f.plan_code in
             (select fr.parent_plan_code from finance_plan_relation fr where fr.child_plan_code in
             (select p.produce from produce_map p where p.c_ins_no =tmp.plan_code))),'汇总','') 明细险种大类
      --cancel_flag,
      --receipt_no,
      -- plan_code,
      --agent_code,

        From tmp_finance_duepremium_short tmp
       Where rownum <= counterlimit
       Order By policy_no;
    Re_Cursor := v_cursor;
  exception
    when data_not_prepared then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
    when others then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
      rollback;
      --raise;
  end finance_duepremium_long_re_new;

------------------------------------------------------------------------------------------------------
--created by ex-huangzhiyun001 2012-06-21
--应收保费清单（长期）  旧清单迁移 货运险清单
-----------------------------------------------------------------------------------------------------
  procedure finance_duepremium_long_hy_new(parameters in varchar2,
                                           re_cursor  out t_cursor) is
    v_cursor      t_cursor;
    filter        varchar2(3600); --过滤
    sel           varchar2(5000); --选择项
    sql_statement varchar2(8600); --Select语句
    --过滤参数
    p_user_code       varchar2(20);
    p_department_code varchar2(10);
    p_plan_code       varchar2(20);
    p_plan_class_code varchar2(20);
    p_emp_code        varchar2(30);
    p_sale_group      varchar2(20);
    p_client_type     varchar2(2);
    p_sale_channel    varchar2(10);
    p_currency_code   varchar2(2);
    p_rateh           varchar2(10);
    p_rateu           varchar2(10);
    p_approach          varchar2(1);   --1-监管口径 2-考核口径
    p_group_include     varchar2(1);   --包含/不包含 团队
    p_agent_include     varchar2(1);   --包含/不包含 业务员
    p_channel_include   varchar2(1);   --包含/不包含 渠道

    --汇率值变量
    v_rateh number(8, 4);
    v_rateu number(8, 4);
    data_not_prepared exception;
    --用户登录参数
    v_user_loginId varchar2(100);
    counterlimit   number(10); --报表查询记录条数限制
    v_ErrMsg       varchar2(500);
    v_sql_plan_class          varchar2(500); --查询的险种大类
  begin
    --先判断能否从临时表TMP_FINANCE_DUEPREMIUM_LONG_O 中取数
    /*  select parameter_value into v_fetch_flag from control_parameter where parameter_name = 'duepremium_long_flag';
    if v_fetch_flag = '0' then
        raise data_not_prepared;
    end if;*/
    --报表查询记录条数限制,直接控制到excel最大接受值
    --  counterlimit := pkg_ereport.getcountlimit;
    --晚上开放至20万
    If to_char(Sysdate, 'hh24') < '21' And to_char(Sysdate, 'hh24') > '07' Then
      counterlimit := 65000;
    Else
    --counterlimit := 200000;
      counterlimit := 500000;
    End If;

    /*机构：明细三级机构财务代码，如010100、010200等
    起止日期：保批单保险起期与核保日期的最大值所在范围(长期没有起止日期)
    截止日期：实收凭证日期
    团队：团队代码
    业务员：业务员代码
    险种：分车、财、意三大险种，三大险种下可以继续细分明细险种
    客户：分个人客户、团体客户
    渠道：分综合开拓、传统渠道、新渠道三大渠道，三大渠道下可细分明细渠道
    币种：人民币、港币、美元*/
    --分解通过reportnet提示页面获取的参数信息
    p_user_code       := pkg_ereport.getParametervalue(parameters,
                                                       'userName_epcis'); --执行用户
    p_department_code := pkg_ereport.getParameterValue(parameters,
                                                       'finance_department_code_epcis'); --机构
    p_plan_class_code := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'planClass_code_epcis'); --险种大类
    p_plan_code       := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'plan_code_epcis'); --险种
    p_emp_code        := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleAgent_code_epcis'); --业务员
    p_sale_group      := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'saleGroup_code_epcis'); --团队
    p_client_type     := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'businessType_epcis'); --客户类型
    p_client_type     := trim(p_client_type);
    p_sale_channel    := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'channel_code_epcis'); --渠道
    p_currency_code   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'currency_code_epcis'); --币种
    p_rateh           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateH_epcis'); --港币汇率值
    p_rateu           := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'rateU_epcis'); --美元汇率值
    v_user_loginId    := p_user_code || '-' ||
                         to_char(sysdate, 'yyyymmdd hh24miss'); --根据当前时间组成此次会话的用户id
    p_approach          := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'approach');
    p_group_include     := pkg_ereport.getparametervalue(PARAMETERS,
                                                         'group_include');
    p_agent_include     := pkg_ereport.getparametervalue(PARAMETERS,
                                                         'agent_include');
    p_channel_include   := pkg_ereport.getparametervalue(PARAMETERS,
                                                         'channel_include');
    --财务机构如果是汇总机构则做相应处理
    if p_department_code = substr(p_department_code, 1, 2) || '9999' then
      p_department_code := substr(p_department_code, 1, 2);
    elsif p_department_code = substr(p_department_code, 1, 4) || '99' then
      p_department_code := substr(p_department_code, 1, 4);
    end if;
    --若用户没有在提示页面输入汇率，则从系统表中取汇率值
    if p_rateh is null then
      v_rateh := pkg_general_tools.get_exchange_rate('02', '01', sysdate);
    else
      v_rateh := to_number(p_rateh);
    end if;
    if p_rateu is null then
      v_rateu := pkg_general_tools.get_exchange_rate('03', '01', sysdate);
    else
      v_rateu := to_number(p_rateu);
    end if;
    --组织filter过滤
    filter := '';
    --机构（必选）
    filter := filter || ' where department_code like (:p7 ||''%'') ';
    --险种
    v_sql_plan_class := ' and exists (select 1
          from finance_plan_relation fr, finance_plan_class f, produce_map p
         where fr.parent_plan_code = f.plan_code
           and p.produce = fr.child_plan_code
           and p.c_ins_no = c_u.plan_code';
    if p_approach = '1' then
      filter := filter || v_sql_plan_class || ' and f.plan_type = ''01''';
      filter := filter ||
           ' and f.plan_code in (''990200'')) ';
    elsif p_approach = '2' then
      filter := filter || v_sql_plan_class ||
           ' and f.plan_code in ( ''990200'') and f.plan_type = ''02'') ';
    else
       filter := filter || v_sql_plan_class ||
           ' and f.plan_code in (''990200'')) ';
    end if;

    --业务员
--    if p_agent_include = '0' then
--      if p_emp_code is not null then
--        filter := filter || ' and nvl(sale_agent_code, ''null'') <>''' || p_emp_code || ''' ';
--      end if;
--    else
--      if p_emp_code is not null then
--        filter := filter || ' and nvl(sale_agent_code, ''null'') =''' || p_emp_code || ''' ';
--      end if;
--    end if;
    --团队
--    if p_group_include = '0' then
--      if p_sale_group is not null then
--        filter := filter || ' and nvl(group_code, ''null'') <> ''' || p_sale_group || ''' ';
--      end if;
--    else
--      if p_sale_group is not null then
--        filter := filter || ' and nvl(group_code, ''null'') = ''' || p_sale_group || ''' ';
--      end if;
--    end if;
    --客户
    if p_client_type is not null then
      filter := filter ||
                ' and decode(client_code, ''2'', ''0'', client_code) =''' ||
                p_client_type || ''' ';
    end if;
    --渠道
    if p_channel_include = '0' then
      if p_sale_channel is not null then
        filter := filter --|| ' and sale_channel_code =''' || p_sale_channel ||
                  || ' and nvl(sale_channel_code, ''null'') not In (Select b.flex_value ' ||
                  '  From channel_info b Where b.channel_eight_role_code = ''' ||
                  p_sale_channel || ''') ';
      end if;
    else
      if p_sale_channel is not null then
        filter := filter --|| ' and sale_channel_code =''' || p_sale_channel ||
                  || ' and nvl(sale_channel_code, ''null'') In (Select b.flex_value ' ||
                  '  From channel_info b Where b.channel_eight_role_code = ''' ||
                  p_sale_channel || ''') ';
      end if;
    end if;
    --币种
    if p_currency_code is not null then
      filter := filter || ' and currency_code = ''' || p_currency_code ||
                ''' ';
    end if;
    --增加每次返回条数限制
    filter := filter || ' and rownum <= ' || counterlimit;
    sel    := ' insert into tmp_finance_duepremium_short ' ||
              ' (department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date,insured_person, ' ||
              ' due_voucher_no,due_voucher_date,account_days,account_month,plan_name,client_name, ' ||
              ' sale_channel,sale_group,emp_name, ' ||
              ' currency,due_premium_sum,cancel_flag, ' ||
              ' underwrite_time,notice_no,settle_date,
              receipt_no, plan_code, agent_code,agent_chinese_name,payment_end_date,applicant_name, plan_class_code,receivable_type, ' ||
              'invoice_amount,invoice_amount_sum,invoice_rmb,invoice_rmb_sum) ' ||
              ' select department_code,department_name,policy_no,endorse_no,due_premium,insurance_begin_date, ' ||
              ' insurance_end_date, insured_person, ' ||
              ' due_voucher_no,due_voucher_date,
              to_char(round(sysdate - greatest(payment_end_date ,underwrite_time),0)) as account_day,
              case when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 3 then ''3个月以内''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 3
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 6 then ''3到6个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 6
                    and to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) <= 12 then ''6到12个月''
                   when to_number(round(months_between(sysdate, greatest(payment_end_date ,underwrite_time)),3)) > 12 then ''12个月以上''
               end account_month, nvl(plan_name, (select pro_descr from produce_map where c_ins_no=plan_code)),CLIENT_NAME,  ' ||
              ' sale_channel_code, group_code|| ''-'' ||group_name,sale_agent_code|| ''-'' ||sale_agent_name, ' ||
              ' currency_name,decode(currency_code, ''02'', due_premium * :p1, ''03'', due_premium * :p2
              , due_premium),cancel_flag , ' || ' underwrite_time,notice_no,settle_date, ' ||
              ' receipt_no, plan_code, agent_code, agent_chinese_name, payment_end_date,' ||
             /* '  case--按批单号去视图查投保
                                                                                                                     when endorse_no is not null then
                                                                                                                    (select APPLICANTNAME
                                                                                                                     from odsdata.get_applicant_name_vw
                                                                                                                    where POLICYNO = endorse_no
                                                                                                                    and rownum=1)
                                                                                                                   else
                                                                                                                   (select APPLICANTNAME
                                                                                                                     from odsdata.get_applicant_name_vw
                                                                                                                    where POLICYNO = policy_no
                                                                                                                    and rownum=1)
                                                                                                                      end,   '||*/
              'epciscde.pkg_finance_pub_tools.getApplicantByPolicyEndorse(policy_no,endorse_no), --投保人
               (Select Distinct t.plan_class plan_class_code
                  From produce_map t
                 where t.c_ins_no = c_u.plan_code),  ' ||
              'sign(Sum(c_u.due_premium) Over(Partition By c_u.policy_no)),  ' ||
              'nvl((select ptp.amount
                   from premium_tax_plan ptp
                  where ptp.receipt_no = c_u.receipt_no
                    and ptp.plan_code = c_u.plan_code),
                 0),
             (nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = c_u.receipt_no
                     and p.plan_code = c_u.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = c_u.receipt_no
                               and ptp.plan_code = c_u.plan_code),
                            0)),' ||
              'decode(currency_code, ''02'', :p3, ''03'', :p4, ''1'')
              * nvl((select ptp.amount
                   from premium_tax_plan ptp
                  where ptp.receipt_no = c_u.receipt_no
                    and ptp.plan_code = c_u.plan_code),
                 0),
             decode(currency_code, ''02'', :p5, ''03'', :p6, ''1'')
             * (nvl((select nvl(p.premium_amount, 0)
                    from premium_plan p
                   where p.receipt_no = c_u.receipt_no
                     and p.plan_code = c_u.plan_code),
                  0) + nvl((select ptp.amount
                              from premium_tax_plan ptp
                             where ptp.receipt_no = c_u.receipt_no
                               and ptp.plan_code = c_u.plan_code),
                            0)) ' ||
              '  from EPCISACCT.tmp_finance_duepremium_long_n c_u';

    /* 取代理人名称字段
    ||' case when (select ''x'' from premium_info a where receipt_no = c_u.receipt_no) is null
                       then (select (select c.agent_chinese_name
                                       from agent_define c where c.agent_code = a.cagtcde) agent_chinese_name
                               from ep_kcoldue a where crctno = c_u.receipt_no )
                       else (select (select c.agent_chinese_name
                                       from agent_define c where c.agent_code = a.agent_code) agent_chinese_name
                               from premium_info a where receipt_no = c_u.receipt_no)
                  end as agent_name,*/

    --sql组合
    sql_statement := sel || filter;

    execute immediate sql_statement using v_rateh,v_rateu,v_rateh,v_rateu,v_rateh,v_rateu,p_department_code;

    --insert into test1(runtime,sqlmess) values(sysdate,sql_statement);

    --打开游标返回
    open v_cursor for
        SELECT 机构代码,
               机构名称,
               -- 团队,
               -- 业务员,
               保单号,
               批单号,
               投保人,
               被保险人,
               sum(金额) as 金额,
               应收凭证号,
               应收凭证日期,
               保险起期,
               保险止期,
               核保日期,
               缴费止期,
               结算日期,
               期次,
               账龄,
               账龄a,
               险种,
               渠道,
               客户,
               代理人或经纪人,
               币种,
               sum(折合人民币) as 折合人民币,
               险种大类,
               应收保费类型,
               sum(应收增值税额) as 应收增值税额,
               sum(应收增值税额1) as "应收增值税额（人民币）",
               sum(合计)  as合计,
               sum(合计1) as "合计（人民币）",
               产品名称,
               总保额,
               运单号
          FROM (Select department_code      As 机构代码,
                     department_name      As 机构名称,
                     -- sale_group           As 团队,
                     -- emp_name             As 业务员,
                     policy_no            As 保单号,
                     endorse_no           As 批单号,
                     applicant_name       As 投保人,
                     insured_person       As 被保险人,
                     due_premium          As 金额,
                     due_voucher_no       As 应收凭证号,
                     due_voucher_date     As 应收凭证日期,
                     insurance_begin_date As 保险起期,
                     insurance_end_date   As 保险止期,
                     underwrite_time      As 核保日期,
                     payment_end_date     As 缴费止期,
                     settle_date          As 结算日期,
                     notice_no            As 期次,
                     account_month        As 账龄,
                     account_days         As 账龄a,

                    /*(SELECT p.marketproduct_name
                            FROM marketproduct_info p
                           where p.marketproduct_code = (SELECT PRODUCT_CODE
                                                          -- FROM PLY_BASE_INFO p
                                                          where p.policy_no = tmp.policy_no
                                                            and rownum = 1)
                             and p.product_class = '09'
                             And p.status = '1'
                             and rownum = 1) As 险种,*/
                     '' 险种,
                     Case
                       When length(tmp.sale_channel) = 1 Then
                        (select bnocnm
                           from business_source
                          where bno = tmp.sale_channel
                            and rownum = 1)
                       Else
                        (Select c.value_chinese_name
                           From channel_info a, common_parameter c
                          Where a.channel_eight_role_code = c.value_code
                            And c.collection_code = 'QDDM03'
                            And a.flex_value = tmp.sale_channel
                            And rownum = 1)
                     End As 渠道,
                     client_name As 客户,
                     --agent_chinese_name as 代理人或经纪人,
                     nvl(agent_chinese_name,
                         (Select a.broker_chn_name
                            From broker_info a
                           Where a.broker_code = (Select b.broker_code
                                                    From commission_info b
                                                   Where b.policy_no = tmp.policy_no
                                                     And rownum = 1)
                             And rownum = 1)) As 代理人或经纪人,
                     currency As 币种,
                     due_premium_sum As 折合人民币,
                     replace(nvl2(p_plan_class_code,
                          (select distinct f.plan_description
                             from finance_plan_relation fr,
                                  finance_plan_class    f,
                                  produce_map           p
                            where fr.parent_plan_code = f.plan_code
                              and p.produce = fr.child_plan_code
                              and f.plan_code = p_plan_class_code),
                          nvl2(p_approach,
                               (select f.plan_description
                                  from finance_plan_relation fr,
                                       finance_plan_class    f,
                                       produce_map           p
                                 where fr.parent_plan_code = f.plan_code
                                   and p.produce = fr.child_plan_code
                                   and p.c_ins_no = tmp.plan_code
                                   and f.plan_type = '0' || p_approach),
                               (select f.plan_description
                                  from finance_plan_relation fr,
                                       finance_plan_class    f,
                                       produce_map           p
                                 where fr.parent_plan_code = f.plan_code
                                   and p.produce = fr.child_plan_code
                                   and p.c_ins_no = tmp.plan_code
                                   and rownum = 1))), '汇总', '') As 险种大类,
                     /*decode(plan_class_code,
                            '01',
                            '车险',
                            '02',
                            '财产险',
                            '03',
                            '意健险',
                            '财产险') As 险种大类,*/
                     decode(receivable_type,
                            '1',
                            '正数应收',
                            '-1',
                            '负数应收',
                            '正数应收') As 应收保费类型,
                     tmp.invoice_amount as 应收增值税额,
                     tmp.invoice_rmb as "应收增值税额1",
                     decode(tmp.invoice_amount_sum,0,due_premium,tmp.invoice_amount_sum) as 合计,
                     decode(tmp.invoice_rmb_sum,0,due_premium_sum,tmp.invoice_rmb_sum) as "合计1",
                      /* (SELECT PRODUCT_CODE
                          FROM PLY_BASE_INFO p
                         where tmp.POLICY_NO = p.policy_no
                           and rownum = 1) 产品名称,
                       (SELECT TOTAL_INSURED_AMOUNT
                          FROM PLY_BASE_INFO p
                         where tmp.POLICY_NO = p.policy_no
                           and rownum = 1) 总保额,*/
                           '' 产品名称,
                           0 总保额,
                           ''运单号/*,
                       (SELECT substr(to_char(risk_detail),
                                      decode(instr(to_char(risk_detail),
                                                   '"productSerialNumber"'),
                                             0,
                                             length(risk_detail),
                                             instr(to_char(risk_detail),
                                                   '"productSerialNumber"')) + 23,
                                      20)
                          FROM Ply_risk_property p
                         where p.policy_no = t.POLICY_NO
                           and rownum = 1)*/
                      --cancel_flag,
                      --receipt_no,
                      -- plan_code,
                      --agent_code,
                From tmp_finance_duepremium_short tmp
               Where rownum <= counterlimit)t
               group by 机构代码,
                        机构名称,
                        -- 团队,
                        -- 业务员,
                        保单号,
                        批单号,
                        投保人,
                        被保险人,
                        应收凭证号,
                        应收凭证日期,
                        保险起期,
                        保险止期,
                        核保日期,
                        缴费止期,
                        结算日期,
                        期次,
                        账龄,
                        账龄a,
                        险种,
                        渠道,
                        客户,
                        代理人或经纪人,
                        币种,
                        险种大类,
                        应收保费类型,
                        产品名称,
                        总保额,
                        运单号
               Order By 保单号;
    Re_Cursor := v_cursor;
  exception
    when data_not_prepared then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
    when others then
      v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
      rollback;
      --raise;
  end finance_duepremium_long_hy_new;

/*****************************************
add by ex-zhangyou001 on 2014-04-18
应收计算数据准备部分，将原ETL同步KP表数据等部分在该pro执行。并由财务系统Quartz调用
工作原理：1、调用PKG删除每日应收临时表
            pkg_ereport_finance_due_new.pro_truncate_table();
         2、 同步kcoldue表到每日应收临时表
         3、同步premium_info表到每日应收临时表
         4、调PRO插入部门到任务表中
          pkg_ereport_finance_due_new.PRO_FINANCE_DUEP_DEPARTMENT();
********************************************/
procedure pro_finance_due_seqkp_head is
   start_time  date;
   end_time  date;
   v_ErrMsg         varchar2(500);
   v_ErrCodeVal     number := 0;
begin
    /*获取配置的开始时间,格式为yyyy-mm-dd hh24:mi:ss
     默认开始时间为昨天0点，到当天8点，如运行时间为2014-03-05
    开始时间为2014-03-04 00:00:00
    结束时间为2014-03-05 08:00:00 */
   begin
     select  to_date(t.item_value,'yyyy-mm-dd hh24:mi:ss') into start_time  from epcisacct.finance_data_dictionary t
      where t.item_class= 'pro_due_parameter'  and t.item_name='start_time'
      and  t.disable_flag ='N';
    select  to_date(t.item_value,'yyyy-mm-dd hh24:mi:ss') into end_time  from epcisacct.finance_data_dictionary t
      where t.item_class= 'pro_due_parameter'  and t.item_name='end_time'
      and  t.disable_flag ='N';
   exception when others then
        select trunc(sysdate) -1 into start_time  from dual;
        select trunc(sysdate) +1/24   into end_time  from dual;
   end;
   /*1、调用PKG删除每日应收临时表
            pkg_ereport_finance_due_new.pro_truncate_table();*/
   pkg_ereport_finance_due_new.pro_truncate_table();

   /* 2、 同步kcoldue表到每日应收临时表 */
        merge into TMP_K_P_UPDATE_DATA_EVERYDAY t1
        using (select cdno department_code,
                      (select description
                         from institutions
                        where flex_value = t.cdno) department_name,
                      cplyno policy_no,
                      cedrno endorse_no,
                      ntermno notice_no,
                      nprmdue due_premium,
                      dplystr insurance_begin_date,
                      dplyend insurance_end_date,
                      cpaynme insured_person,
                      caccno due_voucher_no,
                      daccdte due_voucher_date,
                      '0' as kaccountdays, --日帐龄，在返回数据时计算
                      cinscde plan_code,
                      (select plan_chinese_name
                         from plan_define
                        where plan_code = t.cinscde) plan_name,
                      ccurno currency_code,
                      (select currency_chinese_name
                         from currency_define
                        where currency_code = t.ccurno) currency_name,
                      substr(c_magic_set, 2, 1) client_code,
                      case
                        when substr(t.c_magic_set, 2, 1) = '1' then
                         '个体'
                        else
                         '团体'
                      end client_name,
                      cempcde sale_agent_code,
                      (select employee_name
                         from epcisbase.sas_employee
                        where employee_code = t.cempcde
                          and rownum = 1) sale_agent_name,
                      substr(c_magic_set, 1, 1) sale_channel_code,
                      (select bnocnm
                         from business_source
                        where bno = substr(t.c_magic_set, 1, 1)
                          and rownum = 1) sale_channel_name,
                      cparno group_code,
                      (select group_name
                         from epcisbase.sas_group t_salegrp
                        where group_code = t.cparno
                          and rownum = 1) group_name,
                      case t.ccurno
                        when '02' then
                         (select exchange_rate
                            from exchange_rate
                           where effective_date <= sysdate
                             and (invalidate_date is null or
                                 sysdate <= invalidate_date)
                             and currency1_code = '02'
                             and currency2_code = '01') * t.nprmdue
                        when '03' then
                         (select exchange_rate
                            from exchange_rate
                           where effective_date <= sysdate
                             and (invalidate_date is null or
                                 sysdate <= invalidate_date)
                             and currency1_code = '03'
                             and currency2_code = '01') * t.nprmdue
                        else
                         t.nprmdue
                      end due_premium_sum,
                      cancel cancel_flag,
                      dfcd      underwrite_time,
                      dcaldte   settle_date,
                      crctno receipt_no,
                      'k' as iskorptab,
                      case
                        when (dcoldte is null and caccno is not null and
                             greatest(dplystr, dfcd) between
                             to_date('19990101', 'yyyymmdd') and
                             last_day(trunc(sysdate, 'dd')) + 1 --交费起期/创建日期
                             and cinscde <> 'A24' and
                             nvl(hide_flag, 'N') <> 'Y' and
                             daccdte >= to_date('19990101', 'yyyymmdd') and
                             nvl(cancel_after_verification, 'N') = 'N') or
                             (dcoldte > trunc(sysdate) + 1 and
                             caccno is not null and
                             greatest(dplystr, dfcd) between
                             to_date('19990101', 'yyyymmdd') and
                             last_day(trunc(sysdate, 'dd')) + 1 and
                             cinscde <> 'A24' and nvl(hide_flag, 'N') <> 'Y' and
                             daccdte >= to_date('19990101', 'yyyymmdd') and
                             nvl(cancel_after_verification, 'N') = 'N')  then
                         1
                        else
                         2
                      end isdata,
                      sysdate ods_updated_date,
                      csource business_source,
                     /* t.cagtcde as agent_code,
                      (select agent_chinese_name
                         from agent_define
                        where agent_code = t.cagtcde) as agent_chinese_name,*/
                        nvl(t.cagtcde, t.cbrkcde) agent_code,--代理人代码或经纪人代码
                        nvl((select b.agent_chinese_name
                              from agent_define b
                             where b.agent_code = t.cagtcde),
                            (select b.broker_chn_abbr_name
                                from broker_info b
                               where b.broker_code = t.cbrkcde)) agent_chinese_name,--代理人名称或经纪人名称
                       dpayend  PAYMENT_END_DATE,
                      '0' account_month, --月帐龄，在返回数据时计算
                      t.cdptcde busi_department_code,
                      epciscde.pkg_finance_pub_tools.getFourChannelCode(t.cplyno,Null) channel_source_code
                 from epcisacct.kcoldue t
                where t.dlcd >= start_time
                  and t.dlcd < end_time
                  and t.cinscde <> 'A24') t2
        on (t1.receipt_no = t2.receipt_no and t1.plan_code = t2.plan_code)
        when matched then
          update
             set t1.DEPARTMENT_CODE      = t2.DEPARTMENT_CODE,
                 t1.DEPARTMENT_NAME      = t2.DEPARTMENT_NAME,
                 t1.POLICY_NO            = t2.POLICY_NO,
                 t1.ENDORSE_NO           = t2.ENDORSE_NO,
                 t1.NOTICE_NO            = t2.NOTICE_NO,
                 t1.DUE_PREMIUM          = t2.DUE_PREMIUM,
                 t1.INSURANCE_BEGIN_DATE = t2.INSURANCE_BEGIN_DATE,
                 t1.INSURANCE_END_DATE   = t2.INSURANCE_END_DATE,
                 t1.INSURED_PERSON       = t2.INSURED_PERSON,
                 t1.DUE_VOUCHER_NO       = t2.DUE_VOUCHER_NO,
                 t1.DUE_VOUCHER_DATE     = t2.DUE_VOUCHER_DATE,
                 t1.ACCOUNT_DAYS         = t2.KACCOUNTDAYS,
                 t1.PLAN_NAME            = t2.PLAN_NAME,
                 t1.CURRENCY_CODE        = t2.CURRENCY_CODE,
                 t1.CURRENCY_NAME        = t2.CURRENCY_NAME,
                 t1.CLIENT_CODE          = t2.CLIENT_CODE,
                 t1.CLIENT_NAME          = t2.CLIENT_NAME,
                 t1.SALE_AGENT_CODE      = t2.SALE_AGENT_CODE,
                 t1.SALE_AGENT_NAME      = t2.SALE_AGENT_NAME,
                 t1.SALE_CHANNEL_CODE    = t2.SALE_CHANNEL_CODE,
                 t1.SALE_CHANNEL_NAME    = t2.SALE_CHANNEL_NAME,
                 t1.GROUP_CODE           = t2.GROUP_CODE,
                 t1.GROUP_NAME           = t2.GROUP_NAME,
                 t1.DUE_PREMIUM_SUM      = t2.DUE_PREMIUM_SUM,
                 t1.CANCEL_FLAG          = t2.CANCEL_FLAG,
                 t1.UNDERWRITE_TIME      = t2.UNDERWRITE_TIME,
                 t1.SETTLE_DATE          = t2.SETTLE_DATE,
                 t1.ISKORPTAB            = t2.ISKORPTAB,
                 t1.ISDATA               = t2.ISDATA,
                 t1.ODS_UPDATED_DATE     = t2.ODS_UPDATED_DATE,
                 t1.BUSINESS_SOURCE      = t2.BUSINESS_SOURCE,
                 t1.AGENT_CODE           = t2.AGENT_CODE,
                 t1.AGENT_CHINESE_NAME   = t2.AGENT_CHINESE_NAME,
                 t1.PAYMENT_END_DATE     = t2.PAYMENT_END_DATE,
                 t1.ACCOUNT_MONTH        = t2.ACCOUNT_MONTH,
                 t1.busi_department_code = t2.busi_department_code,
                 t1.channel_source_code  = t2.channel_source_code,
                 t1.due_tax  = 0,
                 t1.total_amount         = t2.DUE_PREMIUM
        when not matched then
          insert
            (DEPARTMENT_CODE,
             DEPARTMENT_NAME,
             POLICY_NO,
             ENDORSE_NO,
             NOTICE_NO,
             DUE_PREMIUM,
             INSURANCE_BEGIN_DATE,
             INSURANCE_END_DATE,
             INSURED_PERSON,
             DUE_VOUCHER_NO,
             DUE_VOUCHER_DATE,
             ACCOUNT_DAYS,
             PLAN_CODE,
             PLAN_NAME,
             CURRENCY_CODE,
             CURRENCY_NAME,
             CLIENT_CODE,
             CLIENT_NAME,
             SALE_AGENT_CODE,
             SALE_AGENT_NAME,
             SALE_CHANNEL_CODE,
             SALE_CHANNEL_NAME,
             GROUP_CODE,
             GROUP_NAME,
             DUE_PREMIUM_SUM,
             CANCEL_FLAG,
             UNDERWRITE_TIME,
             SETTLE_DATE,
             RECEIPT_NO,
             ISKORPTAB,
             ISDATA,
             ODS_UPDATED_DATE,
             BUSINESS_SOURCE,
             AGENT_CODE,
             AGENT_CHINESE_NAME,
             PAYMENT_END_DATE,
             ACCOUNT_MONTH,
             busi_department_code,
             channel_source_code,
             due_tax,
             total_amount)
          values
            (t2.DEPARTMENT_CODE,
             t2.DEPARTMENT_NAME,
             t2.POLICY_NO,
             t2.ENDORSE_NO,
             t2.NOTICE_NO,
             t2.DUE_PREMIUM,
             t2.INSURANCE_BEGIN_DATE,
             t2.INSURANCE_END_DATE,
             t2.INSURED_PERSON,
             t2.DUE_VOUCHER_NO,
             t2.DUE_VOUCHER_DATE,
             t2.KACCOUNTDAYS,
             t2.PLAN_CODE,
             t2.PLAN_NAME,
             t2.CURRENCY_CODE,
             t2.CURRENCY_NAME,
             t2.CLIENT_CODE,
             t2.CLIENT_NAME,
             t2.SALE_AGENT_CODE,
             t2.SALE_AGENT_NAME,
             t2.SALE_CHANNEL_CODE,
             t2.SALE_CHANNEL_NAME,
             t2.GROUP_CODE,
             t2.GROUP_NAME,
             t2.DUE_PREMIUM_SUM,
             t2.CANCEL_FLAG,
             t2.UNDERWRITE_TIME,
             t2.SETTLE_DATE,
             t2.RECEIPT_NO,
             t2.ISKORPTAB,
             t2.ISDATA,
             t2.ODS_UPDATED_DATE,
             t2.BUSINESS_SOURCE,
             t2.AGENT_CODE,
             t2.AGENT_CHINESE_NAME,
             t2.PAYMENT_END_DATE,
             t2.ACCOUNT_MONTH,
             t2.busi_department_code,
             t2.channel_source_code,
             0,
             t2.DUE_PREMIUM);
        commit;
       /* 3、同步premium_info表到每日应收临时表*/
      merge into TMP_K_P_UPDATE_DATA_EVERYDAY t1
      using (select /*+use_nl(a,b) index(b PK_PREMIUM_PLAN)*/
              finance_department_code,
              (select description
                 from institutions
                where flex_value = a.finance_department_code) v_department_name,
              policy_no,
              endorse_no,
              term_no, --期别
              premium_amount,
              insurance_begin_time,
              insurance_end_time,
              insured_name,
              due_voucher_no,
              due_voucher_date,
              '0' as kaccountdays, --帐龄
              plan_code,
              (select plan_chinese_name
                 from plan_define
                where plan_code = b.plan_code) v_plan_name,
              currency_code,
              (select currency_chinese_name
                 from currency_define
                where currency_code = a.currency_code) v_currency_name,
              client_attribute,
              case
                when a.client_attribute = '1' then
                 '个体'
                else
                 '团体'
              end v_client_name,
              sale_agent_code,
              (select employee_name
                 from epcisbase.sas_employee
                where employee_code = sale_agent_code
                  and rownum = 1) sale_agent_name,
              --channel_source_code,
              (select bnocnm
                 from business_source
                where bno = a.channel_source_code
                  and rownum = 1) v_salechnl_name,
              group_code,
              (select group_name
                 from epcisbase.sas_group t_salegrp
                where group_code = a.group_code
                  and rownum = 1) group_name,
              case a.currency_code
                when '02' then
                 (select exchange_rate
                    from exchange_rate
                   where effective_date <= sysdate
                     and (invalidate_date is null or sysdate <= invalidate_date)
                     and currency1_code = '02'
                     and currency2_code = '01') * b.premium_amount
                when '03' then
                 (select exchange_rate
                    from exchange_rate
                   where effective_date <= sysdate
                     and (invalidate_date is null or sysdate <= invalidate_date)
                     and currency1_code = '03'
                     and currency2_code = '01') * b.premium_amount
                else
                 b.premium_amount
              end due_premium_sum,
              disable_flag,
              underwrite_time,
              settle_date,
              a.receipt_no, --收据号
              'p' as iskorptab, --数据来源说明p->premium_info,k->kcoldue
              case
                when (actual_voucher_no is null and due_voucher_no is not null and
                     greatest(insurance_begin_time, underwrite_time) between
                     to_date('19990101', 'yyyy-mm-dd') and
                     last_day(trunc(sysdate, 'dd')) + 1 and
                     (trunc(a.CONFIRM_DATE) <> trunc(sysdate,'mm')-1 or a.CONFIRM_DATE is null) and --ex-chenbaiwen001 2020-9-18 add
                     a.receipt_no = b.receipt_no and b.plan_code <> 'A24' and
                     a.due_voucher_date >= to_date('19990101', 'yyyy-mm-dd') and
                     nvl(cancel_after_verification, 'N') = 'N') or
                     (actual_voucher_date > trunc(sysdate) + 1 and
                     due_voucher_no is not null and
                     greatest(insurance_begin_time, underwrite_time) between
                     to_date('19990101', 'yyyy-mm-dd') and
                     last_day(trunc(sysdate, 'dd')) + 1 and
                     (trunc(a.CONFIRM_DATE) <> trunc(sysdate,'mm')-1 or a.CONFIRM_DATE is null) and --ex-chenbaiwen001 2020-9-18 add
                     a.receipt_no = b.receipt_no and b.plan_code <> 'A24' and
                     a.due_voucher_date >= to_date('19990101', 'yyyy-mm-dd') and
                     nvl(cancel_after_verification, 'N') = 'N') then
                 1 --符合应收条件记录 1
                else
                 2 --不符合应收条件记录 2
              end isdata, --是否符合应收条件
              sysdate  ods_updated_date,
              a.business_source,
              a.agent_code,
              (select agent_chinese_name
                 from agent_define
                where agent_code = a.agent_code) as agent_chinese_name,
               PAYMENT_END_DATE,
              '0' account_month,
              a.department_code busi_department_code,
              Case
                When length(a.channel_source_code) = 4 Then
                  a.channel_source_code
                Else
                  epciscde.pkg_finance_pub_tools.getFourChannelCode(a.policy_no,Null)
              End channel_source_code,
              nvl((select c.amount from epcisacct.premium_tax_plan c where c.receipt_no=b.receipt_no and c.plan_code = b.plan_code),0) due_tax
               from epcisacct.premium_info a, epcisacct.premium_plan b
              where a.updated_date >= start_time
                and a.updated_date < end_time
                --and a.CONFIRM_DATE is null       --过滤掉已经预收的数据  ex-chenbaiwen001 2020-9-2
                and a.receipt_no = b.receipt_no
                and nvl(a.remark, 'null') <> '不计应收'
                and b.plan_code <> 'A24') t2
      on (t1.receipt_no = t2.receipt_no and t1.plan_code = t2.plan_code)
      when matched then
        update
           set t1.DEPARTMENT_CODE      = t2.FINANCE_DEPARTMENT_CODE,
               t1.DEPARTMENT_NAME      = t2.V_DEPARTMENT_NAME,
               t1.POLICY_NO            = t2.POLICY_NO,
               t1.ENDORSE_NO           = t2.ENDORSE_NO,
               t1.NOTICE_NO            = t2.TERM_NO,
               t1.DUE_PREMIUM          = t2.PREMIUM_AMOUNT,
               t1.INSURANCE_BEGIN_DATE = t2.INSURANCE_BEGIN_TIME,
               t1.INSURANCE_END_DATE   = t2.INSURANCE_END_TIME,
               t1.INSURED_PERSON       = t2.INSURED_NAME,
               t1.DUE_VOUCHER_NO       = t2.DUE_VOUCHER_NO,
               t1.DUE_VOUCHER_DATE     = t2.DUE_VOUCHER_DATE,
               t1.ACCOUNT_DAYS         = t2.KACCOUNTDAYS,
               t1.PLAN_NAME            = t2.V_PLAN_NAME,
               t1.CURRENCY_CODE        = t2.CURRENCY_CODE,
               t1.CURRENCY_NAME        = t2.V_CURRENCY_NAME,
               t1.CLIENT_CODE          = t2.CLIENT_ATTRIBUTE,
               t1.CLIENT_NAME          = t2.V_CLIENT_NAME,
               t1.SALE_AGENT_CODE      = t2.SALE_AGENT_CODE,
               t1.SALE_AGENT_NAME      = t2.SALE_AGENT_NAME,
               t1.SALE_CHANNEL_CODE    = t2.CHANNEL_SOURCE_CODE,
               t1.SALE_CHANNEL_NAME    = t2.V_SALECHNL_NAME,
               t1.GROUP_CODE           = t2.GROUP_CODE,
               t1.GROUP_NAME           = t2.GROUP_NAME,
               t1.DUE_PREMIUM_SUM      = t2.DUE_PREMIUM_SUM,
               t1.CANCEL_FLAG          = t2.DISABLE_FLAG,
               t1.UNDERWRITE_TIME      = t2.UNDERWRITE_TIME,
               t1.SETTLE_DATE          = t2.SETTLE_DATE,
               t1.ISKORPTAB            = t2.ISKORPTAB,
               t1.ISDATA               = t2.ISDATA,
               t1.ODS_UPDATED_DATE     = t2.ODS_UPDATED_DATE,
               t1.BUSINESS_SOURCE      = t2.BUSINESS_SOURCE,
               t1.AGENT_CODE           = t2.AGENT_CODE,
               t1.AGENT_CHINESE_NAME   = t2.AGENT_CHINESE_NAME,
               t1.PAYMENT_END_DATE     = t2.PAYMENT_END_DATE,
               t1.ACCOUNT_MONTH        = t2.ACCOUNT_MONTH,
               t1.busi_department_code = t2.busi_department_code,
               t1.channel_source_code  = t2.channel_source_code,
               t1.due_tax              = t2.due_tax,
               t1.total_amount         = t2.PREMIUM_AMOUNT+t2.due_tax
      when not matched then
        insert
          (DEPARTMENT_CODE,
           DEPARTMENT_NAME,
           POLICY_NO,
           ENDORSE_NO,
           NOTICE_NO,
           DUE_PREMIUM,
           INSURANCE_BEGIN_DATE,
           INSURANCE_END_DATE,
           INSURED_PERSON,
           DUE_VOUCHER_NO,
           DUE_VOUCHER_DATE,
           ACCOUNT_DAYS,
           PLAN_CODE,
           PLAN_NAME,
           CURRENCY_CODE,
           CURRENCY_NAME,
           CLIENT_CODE,
           CLIENT_NAME,
           SALE_AGENT_CODE,
           SALE_AGENT_NAME,
           SALE_CHANNEL_CODE,
           SALE_CHANNEL_NAME,
           GROUP_CODE,
           GROUP_NAME,
           DUE_PREMIUM_SUM,
           CANCEL_FLAG,
           UNDERWRITE_TIME,
           SETTLE_DATE,
           RECEIPT_NO,
           ISKORPTAB,
           ISDATA,
           ODS_UPDATED_DATE,
           BUSINESS_SOURCE,
           AGENT_CODE,
           AGENT_CHINESE_NAME,
           PAYMENT_END_DATE,
           ACCOUNT_MONTH,
           busi_department_code,
           channel_source_code,
           due_tax,
           total_amount)
        values
          (t2.FINANCE_DEPARTMENT_CODE,
            t2.V_DEPARTMENT_NAME,
            t2.POLICY_NO,
            t2.ENDORSE_NO,
            t2.TERM_NO,
            t2.PREMIUM_AMOUNT,
            t2.INSURANCE_BEGIN_TIME,
            t2.INSURANCE_END_TIME,
            t2.INSURED_NAME,
            t2.DUE_VOUCHER_NO,
            t2.DUE_VOUCHER_DATE,
            t2.KACCOUNTDAYS,
            t2.PLAN_CODE,
            t2.V_PLAN_NAME,
            t2.CURRENCY_CODE,
            t2.V_CURRENCY_NAME,
            t2.CLIENT_ATTRIBUTE,
            t2.V_CLIENT_NAME,
            t2.SALE_AGENT_CODE,
            t2.SALE_AGENT_NAME,
            t2.CHANNEL_SOURCE_CODE,
            t2.V_SALECHNL_NAME,
            t2.GROUP_CODE,
            t2.GROUP_NAME,
            t2.DUE_PREMIUM_SUM,
            t2.DISABLE_FLAG,
            t2.UNDERWRITE_TIME,
            t2.SETTLE_DATE,
            t2.RECEIPT_NO,
            t2.ISKORPTAB,
            t2.ISDATA,
            t2.ODS_UPDATED_DATE,
            t2.BUSINESS_SOURCE,
            t2.AGENT_CODE,
            t2.AGENT_CHINESE_NAME,
            t2.PAYMENT_END_DATE,
            t2.ACCOUNT_MONTH,
            t2.busi_department_code,
            t2.channel_source_code,
            t2.due_tax,
             t2.PREMIUM_AMOUNT+t2.due_tax
        );
        commit;
        /*  4、调PRO插入部门到任务表中*/
     pkg_ereport_finance_due_new.PRO_FINANCE_DUEP_DEPARTMENT();
exception
    when others then
      v_ErrCodeVal := sqlcode;
      v_ErrMsg     := substr('执行pro_finance_due_seqkp_head过程出错' ||sqlerrm,1,500);
      insert into epcis_job_log
      values
        (sysdate,
         '',
         '',
         v_ErrCodeVal,
         v_ErrMsg,
         'pro_finance_due_seqkp_head执行失败！',sys_guid());
      commit;
end pro_finance_due_seqkp_head;

/*******************************************
 add by ex-zhangyou001 on 2014-04-18
远期应收（预收）计算,将原ETL预收计算所有逻辑在该部分执行。并由财务系统Quartz调用
工作原理：1、调用pro删除每日预收临时表
             pkg_ereport_finance_due_new.pro_truncate_foresee_everyday();
         2、同步kcoldue表到每日预收临时表
         3、同步premium_info表到每日预收临时表
         4、 调用远期应收计算pro
pkg_ereport_finance_due_new.pro_finance_foresee_long();
********************************************/
procedure pro_finance_foresee_seqkp is
   start_time  date;
   end_time  date;
   v_ErrMsg         varchar2(500);
   v_ErrCodeVal     number := 0;
   v_switch         varchar2(1);
begin
    /*获取配置的开始时间,格式为yyyy-mm-dd hh24:mi:ss
     默认开始时间为昨天0点，到当天8点，如运行时间为2014-03-05
    开始时间为2014-03-04 00:00:00
    结束时间为2014-03-05 08:00:00 */
   begin
     select  to_date(t.item_value,'yyyy-mm-dd hh24:mi:ss') into start_time  from epcisacct.finance_data_dictionary t
      where t.item_class= 'pro_due_parameter'  and t.item_name='start_time'
      and  t.disable_flag ='N';
    select  to_date(t.item_value,'yyyy-mm-dd hh24:mi:ss') into end_time  from epcisacct.finance_data_dictionary t
      where t.item_class= 'pro_due_parameter'  and t.item_name='end_time'
      and  t.disable_flag ='N';
   exception when others then
        select trunc(sysdate) -1 into start_time  from dual;
        select trunc(sysdate) +1/24   into end_time  from dual;
   end;

   begin
     select  VALUE_CODE into v_switch from epcisacct.nps_paramter_define t
      where t.CLASS_CODE= 'due_foresee_asyn_switch';
   exception when others then
        v_switch := 'N';
   end;

   IF v_switch = 'Y' THEN
   /*1、调用pro删除每日预收临时表*/
    pkg_ereport_finance_due_new.pro_truncate_foresee_everyday();

   /* 2、 同步kcoldue表到每日预收临时表*/
    merge into  FIN_NOTCOL_FORESEE_EVERYDAY   t1
    using (select sysdate  as created_date,
                  'system' as created_use,
                   sysdate  as updated_date,
                  'sysdate' as updated_by,
                  cdno department_code,
                  (select description from institutions where flex_value = t.cdno) department_name,
                  cplyno policy_no,
                  cedrno endorse_no,
                  ntermno notice_no,
                  precol_amount,
                  nprmdue due_premium,
                  dplystr  insurance_begin_date,
                  dplyend   insurance_end_date,
                  cpaynme insured_person,
                  caccno due_voucher_no,
                  daccdte  due_voucher_date,
                  cinscde plan_code,
                  (select plan_chinese_name
                     from plan_define
                    where plan_code = t.cinscde) plan_name,
                  ccurno currency_code,
                  (select currency_chinese_name
                     from currency_define
                    where currency_code = t.ccurno) currency_name,
                  substr(c_magic_set, 2, 1) client_code,
                  case
                    when substr(t.c_magic_set, 2, 1) = '1' then
                     '个体'
                    else
                     '团体'
                  end client_name,
                  cempcde sale_agent_code,
                  (select employee_name
                     from epcisbase.sas_employee
                    where employee_code = t.cempcde
                      and rownum = 1) sale_agent_name,
                  substr(c_magic_set, 1, 1) sale_channel_code,
                  (select bnocnm
                     from business_source
                    where bno = substr(t.c_magic_set, 1, 1)
                      and rownum = 1) sale_channel_name,
                  cparno group_code,
                  (select group_name
                     from epcisbase.sas_group t_salegrp
                    where group_code = t.cparno
                      and rownum = 1) group_name,
                  case t.ccurno
                    when '02' then
                     (select exchange_rate
                        from exchange_rate
                       where effective_date <= sysdate
                         and (invalidate_date is null or
                             sysdate <= invalidate_date)
                         and currency1_code = '02'
                         and currency2_code = '01') * t.precol_amount
                    when '03' then
                     (select exchange_rate
                        from exchange_rate
                       where effective_date <= sysdate
                         and (invalidate_date is null or
                             sysdate <= invalidate_date)
                         and currency1_code = '03'
                         and currency2_code = '01') * t.precol_amount
                    else
                     t.precol_amount
                  end precol_amount_sum,
                  case t.ccurno
                    when '02' then
                     (select exchange_rate
                        from exchange_rate
                       where effective_date <= sysdate
                         and (invalidate_date is null or
                             sysdate <= invalidate_date)
                         and currency1_code = '02'
                         and currency2_code = '01') * t.nprmdue
                    when '03' then
                     (select exchange_rate
                        from exchange_rate
                       where effective_date <= sysdate
                         and (invalidate_date is null or
                             sysdate <= invalidate_date)
                         and currency1_code = '03'
                         and currency2_code = '01') * t.nprmdue
                    else
                     t.nprmdue
                  end due_premium_sum,
                  cancel cancel_flag,
                  dfcd  underwrite_time,
                  dcaldte  settle_date,
                  crctno receipt_no,
                  'k' as iskorptab,
                  case
                    when greatest(t.dplystr, t.dfcd) >=
                         last_day(trunc(sysdate, 'mm')) + 1 and
                         nvl(t.nprmdue, 0) <> nvl(t.precol_amount, 0) and
                         nvl(t.cancel_after_verification, 'N') = 'N' then
                     1
                    else
                     2
                  end isdata,
                  sysdate  ods_updated_date,
                  csource business_source,
                  /*t.cagtcde as agent_code,
                  (select agent_chinese_name
                     from agent_define
                    where agent_code = t.cagtcde) as agent_chinese_name,*/
                   nvl(t.cagtcde, t.cbrkcde) agent_code,--代理人代码或经纪人代码
                   nvl((select b.agent_chinese_name
                        from agent_define b
                       where b.agent_code = t.cagtcde),
                      (select b.broker_chn_abbr_name
                          from broker_info b
                         where b.broker_code = t.cbrkcde)) agent_chinese_name,--代理人名称或经纪人名称
                   dpayend   payment_end_date,
                  '0' account_days,
                  '0' account_month
             from epcisacct.kcoldue t
            where t.dlcd >= start_time
              and t.dlcd < end_time
              and t.cinscde <> 'A24') t2
    on (t1.receipt_no = t2.receipt_no and t1.plan_code = t2.plan_code)
    when matched then
      update
         set t1.CREATED_DATE = t2.CREATED_DATE  ,
             t1.CREATED_BY = t2.CREATED_USE  ,
             t1.UPDATED_DATE = t2.UPDATED_DATE  ,
             t1.UPDATED_BY = t2.UPDATED_BY  ,
             t1.DEPARTMENT_CODE = t2.DEPARTMENT_CODE  ,
             t1.DEPARTMENT_NAME = t2.DEPARTMENT_NAME  ,
             t1.POLICY_NO = t2.POLICY_NO  ,
             t1.ENDORSE_NO = t2.ENDORSE_NO  ,
             t1.NOTICE_NO = t2.NOTICE_NO  ,
             t1.PRECOL_AMOUNT = t2.PRECOL_AMOUNT  ,
             t1.DUE_PREMIUM = t2.DUE_PREMIUM  ,
             t1.INSURANCE_BEGIN_DATE = t2.INSURANCE_BEGIN_DATE  ,
             t1.INSURANCE_END_DATE = t2.INSURANCE_END_DATE  ,
             t1.INSURED_PERSON = t2.INSURED_PERSON  ,
             t1.DUE_VOUCHER_NO = t2.DUE_VOUCHER_NO  ,
             t1.DUE_VOUCHER_DATE = t2.DUE_VOUCHER_DATE  ,
             t1.PLAN_NAME = t2.PLAN_NAME  ,
             t1.CURRENCY_CODE = t2.CURRENCY_CODE  ,
             t1.CURRENCY_NAME = t2.CURRENCY_NAME  ,
             t1.CLIENT_CODE = t2.CLIENT_CODE  ,
             t1.CLIENT_NAME = t2.CLIENT_NAME  ,
             t1.SALE_AGENT_CODE = t2.SALE_AGENT_CODE  ,
             t1.SALE_AGENT_NAME = t2.SALE_AGENT_NAME  ,
             t1.SALE_CHANNEL_CODE = t2.SALE_CHANNEL_CODE  ,
             t1.SALE_CHANNEL_NAME = t2.SALE_CHANNEL_NAME  ,
             t1.GROUP_CODE = t2.GROUP_CODE  ,
             t1.GROUP_NAME = t2.GROUP_NAME  ,
             t1.PRECOL_AMOUNT_SUM = t2.PRECOL_AMOUNT_SUM  ,
             t1.DUE_PREMIUM_SUM = t2.DUE_PREMIUM_SUM  ,
             t1.CANCEL_FLAG = t2.CANCEL_FLAG  ,
             t1.UNDERWRITE_TIME = t2.UNDERWRITE_TIME  ,
             t1.SETTLE_DATE = t2.SETTLE_DATE  ,
             t1.ISKORPTAB = t2.ISKORPTAB ,
             t1.ISDATA = t2.ISDATA  ,
             t1.ODS_UPDATED_DATE = t2.ODS_UPDATED_DATE  ,
             t1.BUSINESS_SOURCE = t2.BUSINESS_SOURCE  ,
             t1.AGENT_CODE = t2.AGENT_CODE  ,
             t1.AGENT_CHINESE_NAME = t2.AGENT_CHINESE_NAME  ,
             t1.PAYMENT_END_DATE = t2.PAYMENT_END_DATE  ,
             t1.ACCOUNT_DAYS = t2.ACCOUNT_DAYS  ,
             t1.ACCOUNT_MONTH = t2.ACCOUNT_MONTH
    when not matched then
      insert
        (CREATED_DATE,
         CREATED_BY,
         UPDATED_DATE,
         UPDATED_BY,
         DEPARTMENT_CODE,
         DEPARTMENT_NAME,
         POLICY_NO,
         ENDORSE_NO,
         NOTICE_NO,
         PRECOL_AMOUNT,
         DUE_PREMIUM,
         INSURANCE_BEGIN_DATE,
         INSURANCE_END_DATE,
         INSURED_PERSON,
         DUE_VOUCHER_NO,
         DUE_VOUCHER_DATE,
         PLAN_CODE,
         PLAN_NAME,
         CURRENCY_CODE,
         CURRENCY_NAME,
         CLIENT_CODE,
         CLIENT_NAME,
         SALE_AGENT_CODE,
         SALE_AGENT_NAME,
         SALE_CHANNEL_CODE,
         SALE_CHANNEL_NAME,
         GROUP_CODE,
         GROUP_NAME,
         PRECOL_AMOUNT_SUM,
         DUE_PREMIUM_SUM,
         CANCEL_FLAG,
         UNDERWRITE_TIME,
         SETTLE_DATE,
         RECEIPT_NO,
         ISKORPTAB,
         ISDATA,
         ODS_UPDATED_DATE,
         BUSINESS_SOURCE,
         AGENT_CODE,
         AGENT_CHINESE_NAME,
         PAYMENT_END_DATE,
         ACCOUNT_DAYS,
         ACCOUNT_MONTH)
      values
        (t2.CREATED_DATE,
         t2.CREATED_USE,
         t2.UPDATED_DATE,
         t2.UPDATED_BY,
         t2.DEPARTMENT_CODE,
         t2.DEPARTMENT_NAME,
         t2.POLICY_NO,
         t2.ENDORSE_NO,
         t2.NOTICE_NO,
         t2.PRECOL_AMOUNT,
         t2.DUE_PREMIUM,
         t2.INSURANCE_BEGIN_DATE,
         t2.INSURANCE_END_DATE,
         t2.INSURED_PERSON,
         t2.DUE_VOUCHER_NO,
         t2.DUE_VOUCHER_DATE,
         t2.PLAN_CODE,
         t2.PLAN_NAME,
         t2.CURRENCY_CODE,
         t2.CURRENCY_NAME,
         t2.CLIENT_CODE,
         t2.CLIENT_NAME,
         t2.SALE_AGENT_CODE,
         t2.SALE_AGENT_NAME,
         t2.SALE_CHANNEL_CODE,
         t2.SALE_CHANNEL_NAME,
         t2.GROUP_CODE,
         t2.GROUP_NAME,
         t2.PRECOL_AMOUNT_SUM,
         t2.DUE_PREMIUM_SUM,
         t2.CANCEL_FLAG,
         t2.UNDERWRITE_TIME,
         t2.SETTLE_DATE,
         t2.RECEIPT_NO,
         t2.ISKORPTAB,
         t2.ISDATA,
         t2.ODS_UPDATED_DATE,
         t2.BUSINESS_SOURCE,
         t2.AGENT_CODE,
         t2.AGENT_CHINESE_NAME,
         t2.PAYMENT_END_DATE,
         t2.ACCOUNT_DAYS,
         t2.ACCOUNT_MONTH);
        commit;
      /* 3、同步premium_info表到每日预收临时表*/
    merge into FIN_NOTCOL_FORESEE_EVERYDAY t1
    using (select /*+ index(b PK_PREMIUM_PLAN)*/
            sysdate as created_date,
            'system' as created_use,
            sysdate as updated_date,
            'sysdate' as updated_by,
            finance_department_code department_code,
            (select description
               from institutions
              where flex_value = a.finance_department_code) department_name,
            policy_no,
            endorse_no,
            term_no notice_no,
            due_amount due_premium,
            precol_amount,
            insurance_begin_time,
           insurance_end_time,
            insured_name,
            due_voucher_no,
            due_voucher_date,
            plan_code,
            (select plan_chinese_name
               from plan_define
              where plan_code = b.plan_code) plan_name,
            currency_code,
            (select currency_chinese_name
               from currency_define
              where currency_code = a.currency_code) currency_name,
            client_attribute,
            case
              when a.client_attribute = '1' then
               '个体'
              else
               '团体'
            end client_name,
            sale_agent_code,
            (select employee_name
               from epcisbase.sas_employee
              where employee_code = sale_agent_code
                and rownum = 1) sale_agent_name,
            channel_source_code sale_channel_code,
            (select bnocnm
               from business_source
              where bno = a.channel_source_code
                and rownum = 1) sale_channel_name,
            group_code,
            (select group_name
               from epcisbase.sas_group t_salegrp
              where group_code = a.group_code
                and rownum = 1) group_name,
            case a.currency_code
              when '02' then
               (select exchange_rate
                  from exchange_rate
                 where effective_date <= sysdate
                   and (invalidate_date is null or sysdate <= invalidate_date)
                   and currency1_code = '02'
                   and currency2_code = '01') * a.precol_amount
              when '03' then
               (select exchange_rate
                  from exchange_rate
                 where effective_date <= sysdate
                   and (invalidate_date is null or sysdate <= invalidate_date)
                   and currency1_code = '03'
                   and currency2_code = '01') * a.precol_amount
              else
               a.precol_amount
            end precol_amount_sum,
            case a.currency_code
              when '02' then
               (select exchange_rate
                  from exchange_rate
                 where effective_date <= sysdate
                   and (invalidate_date is null or sysdate <= invalidate_date)
                   and currency1_code = '02'
                   and currency2_code = '01') * a.due_amount
              when '03' then
               (select exchange_rate
                  from exchange_rate
                 where effective_date <= sysdate
                   and (invalidate_date is null or sysdate <= invalidate_date)
                   and currency1_code = '03'
                   and currency2_code = '01') * a.due_amount
              else
               a.due_amount
            end due_premium_sum,
            disable_flag cancel_flag,
            underwrite_time,
            settle_date,
            a.receipt_no,
            'p' as iskorptab,
            case
              when greatest(a.underwrite_time, a.insurance_begin_time) >=
                   last_day(trunc(sysdate, 'mm')) + 1 and
                   a.due_amount <> a.precol_amount and
                   nvl(a.cancel_after_verification, 'N') = 'N' then
               1
              else
               2
            end isdata,
            sysdate  ods_updated_date,
            a.business_source,
            a.agent_code,
            (select agent_chinese_name
               from agent_define
              where agent_code = a.agent_code) as agent_chinese_name,
             payment_end_date,
            '0' account_days,
            '0' account_month
             from epcisacct.premium_info a, epcisacct.premium_plan b
            where a.updated_date >= start_time
              and a.updated_date < end_time
              and a.receipt_no = b.receipt_no
              and nvl(a.remark, 'null') <> '不计应收'
              and b.plan_code <> 'A24') t2
    on (t1.receipt_no = t2.receipt_no and t1.plan_code = t2.plan_code)
    when matched then
      update
         set t1.CREATED_DATE         = t2.CREATED_DATE,
             t1.CREATED_BY           = t2.CREATED_USE,
             t1.UPDATED_DATE         = t2.UPDATED_DATE,
             t1.UPDATED_BY           = t2.UPDATED_BY,
             t1.DEPARTMENT_CODE      = t2.DEPARTMENT_CODE,
             t1.DEPARTMENT_NAME      = t2.DEPARTMENT_NAME,
             t1.POLICY_NO            = t2.POLICY_NO,
             t1.ENDORSE_NO           = t2.ENDORSE_NO,
             t1.NOTICE_NO            = t2.NOTICE_NO,
             t1.DUE_PREMIUM          = t2.DUE_PREMIUM,
             t1.PRECOL_AMOUNT        = t2.PRECOL_AMOUNT,
             t1.INSURANCE_BEGIN_DATE = t2.INSURANCE_BEGIN_TIME,
             t1.INSURANCE_END_DATE   = t2.INSURANCE_END_TIME,
             t1.INSURED_PERSON       = t2.INSURED_NAME,
             t1.DUE_VOUCHER_NO       = t2.DUE_VOUCHER_NO,
             t1.DUE_VOUCHER_DATE     = t2.DUE_VOUCHER_DATE,
             t1.PLAN_NAME            = t2.PLAN_NAME,
             t1.CURRENCY_CODE        = t2.CURRENCY_CODE,
             t1.CURRENCY_NAME        = t2.CURRENCY_NAME,
             t1.CLIENT_CODE          = t2.CLIENT_ATTRIBUTE,
             t1.CLIENT_NAME          = t2.CLIENT_NAME,
             t1.SALE_AGENT_CODE      = t2.SALE_AGENT_CODE,
             t1.SALE_AGENT_NAME      = t2.SALE_AGENT_NAME,
             t1.SALE_CHANNEL_CODE    = t2.SALE_CHANNEL_CODE,
             t1.SALE_CHANNEL_NAME    = t2.SALE_CHANNEL_NAME,
             t1.GROUP_CODE           = t2.GROUP_CODE,
             t1.GROUP_NAME           = t2.GROUP_NAME,
             t1.PRECOL_AMOUNT_SUM    = t2.PRECOL_AMOUNT_SUM,
             t1.DUE_PREMIUM_SUM      = t2.DUE_PREMIUM_SUM,
             t1.CANCEL_FLAG          = t2.CANCEL_FLAG,
             t1.UNDERWRITE_TIME      = t2.UNDERWRITE_TIME,
             t1.SETTLE_DATE          = t2.SETTLE_DATE,
             t1.ISKORPTAB            = t2.ISKORPTAB,
             t1.ISDATA               = t2.ISDATA,
             t1.ODS_UPDATED_DATE     = t2.ODS_UPDATED_DATE,
             t1.BUSINESS_SOURCE      = t2.BUSINESS_SOURCE,
             t1.AGENT_CODE           = t2.AGENT_CODE,
             t1.AGENT_CHINESE_NAME   = t2.AGENT_CHINESE_NAME,
             t1.PAYMENT_END_DATE     = t2.PAYMENT_END_DATE,
             t1.ACCOUNT_DAYS         = t2.ACCOUNT_DAYS,
             t1.ACCOUNT_MONTH        = t2.ACCOUNT_MONTH
    when not matched then
      insert
        (CREATED_DATE,
         CREATED_BY,
         UPDATED_DATE,
         UPDATED_BY,
         DEPARTMENT_CODE,
         DEPARTMENT_NAME,
         POLICY_NO,
         ENDORSE_NO,
         NOTICE_NO,
         DUE_PREMIUM,
         PRECOL_AMOUNT,
         INSURANCE_BEGIN_DATE,
         INSURANCE_END_DATE,
         INSURED_PERSON,
         DUE_VOUCHER_NO,
         DUE_VOUCHER_DATE,
         PLAN_CODE,
         PLAN_NAME,
         CURRENCY_CODE,
         CURRENCY_NAME,
         CLIENT_CODE,
         CLIENT_NAME,
         SALE_AGENT_CODE,
         SALE_AGENT_NAME,
         SALE_CHANNEL_CODE,
         SALE_CHANNEL_NAME,
         GROUP_CODE,
         GROUP_NAME,
         PRECOL_AMOUNT_SUM,
         DUE_PREMIUM_SUM,
         CANCEL_FLAG,
         UNDERWRITE_TIME,
         SETTLE_DATE,
         RECEIPT_NO,
         ISKORPTAB,
         ISDATA,
         ODS_UPDATED_DATE,
         BUSINESS_SOURCE,
         AGENT_CODE,
         AGENT_CHINESE_NAME,
         PAYMENT_END_DATE,
         ACCOUNT_DAYS,
         ACCOUNT_MONTH)
      values
        (t2.CREATED_DATE,
         t2.CREATED_USE,
         t2.UPDATED_DATE,
         t2.UPDATED_BY,
         t2.DEPARTMENT_CODE,
         t2.DEPARTMENT_NAME,
         t2.POLICY_NO,
         t2.ENDORSE_NO,
         t2.NOTICE_NO,
         t2.DUE_PREMIUM,
         t2.PRECOL_AMOUNT,
         t2.INSURANCE_BEGIN_TIME,
         t2.INSURANCE_END_TIME,
         t2.INSURED_NAME,
         t2.DUE_VOUCHER_NO,
         t2.DUE_VOUCHER_DATE,
         t2.PLAN_CODE,
         t2.PLAN_NAME,
         t2.CURRENCY_CODE,
         t2.CURRENCY_NAME,
         t2.CLIENT_ATTRIBUTE,
         t2.CLIENT_NAME,
         t2.SALE_AGENT_CODE,
         t2.SALE_AGENT_NAME,
         t2.SALE_CHANNEL_CODE,
         t2.SALE_CHANNEL_NAME,
         t2.GROUP_CODE,
         t2.GROUP_NAME,
         t2.PRECOL_AMOUNT_SUM,
         t2.DUE_PREMIUM_SUM,
         t2.CANCEL_FLAG,
         t2.UNDERWRITE_TIME,
         t2.SETTLE_DATE,
         t2.RECEIPT_NO,
         t2.ISKORPTAB,
         t2.ISDATA,
         t2.ODS_UPDATED_DATE,
         t2.BUSINESS_SOURCE,
         t2.AGENT_CODE,
         t2.AGENT_CHINESE_NAME,
         t2.PAYMENT_END_DATE,
         t2.ACCOUNT_DAYS,
         t2.ACCOUNT_MONTH);
        commit;
        /*  4、调用远期应收计算pro中*/
         pkg_ereport_finance_due_new.pro_finance_foresee_long();
     END IF;
exception
    when others then
      v_ErrCodeVal := sqlcode;
      v_ErrMsg     := substr('执行pro_finance_foresee_seqkp过程出错' ||sqlerrm,1,500);
      insert into epcis_job_log
      values
        (sysdate,
         '',
         '',
         v_ErrCodeVal,
         v_ErrMsg,
         'pro_finance_foresee_seqkp执行失败！',sys_guid());
      commit;

end pro_finance_foresee_seqkp;

procedure pro_finance_incidental_pay(parameters in varchar2,
                                  re_cursor  out t_cursor) as


  v_cursor t_cursor; --返回值游标
  p_department_code   varchar2(10); --机构编码
  p_sub_month             varchar2(10); --提交日期
  p_pay_month             varchar2(10); --支付日期
  V_POLICY_NO_LOG       varchar2(500); --2011-06-10 add by ex-zengjiu001 记入异常日志
  v_sub_runTime  varchar2(25);
  v_pay_runTime  varchar2(25);
  /*p_sub_begin_epcis      varchar2(20);
  p_sub_end_epcis      varchar2(20);
  p_pay_begin_epcis      varchar2(20);
  p_pay_end_epcis      varchar2(20);*/
  v_ErrMsg             varchar2(500);

begin

  --分解通过reportnet提示页面获取的参数信息
  p_department_code         := pkg_ereport.getParametervalue(parameters,
                                                       'financeDepartments');
  /*p_sub_begin_epcis  := pkg_ereport.getParameterValue(parameters,
                                                       'sub_begin_epcis'); --提交日期
  p_sub_end_epcis   := pkg_ereport.getParameterValue(parameters,
                                                       'sub_end_epcis'); --提交日期

  p_pay_begin_epcis   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'pay_begin_epcis'); --支付日期

  p_pay_end_epcis   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'pay_end_epcis'); --支付日期*/

    v_sub_runTime   := pkg_ereport.getParameterValue(parameters,
                                                       'sub_runTime'); --提交日期

  v_pay_runTime   := pkg_ereport.getparametervalue(PARAMETERS,
                                                       'pay_runTime'); --支付日期

/* if p_sub_begin_epcis is not null then
   if p_sub_end_epcis is null or p_sub_end_epcis='' then
      raise_application_error(-20010, '选择提单起期但是没有选择提单止期.');
   end if;
 end if;

  if p_pay_begin_epcis is not null then
   if p_pay_end_epcis is null or p_sub_end_epcis='' then
      raise_application_error(-20010, '选择支付起期但是没有选择支付止期.');
   end if;
 end if;*/

  if v_sub_runTime is null or v_sub_runTime ='' then
   if v_pay_runTime is null or v_pay_runTime='' then
      raise_application_error(-20010, '不能同时不录入提单日期和支付日期');
   end if;
  end if;

  --财务机构如果是汇总机构则做相应处理
  if p_department_code='999999' then
    p_department_code:='%';
  elsif p_department_code = substr(p_department_code, 1, 2) || '9999' then
    p_department_code := substr(p_department_code, 1, 2) || '%';

  end if;


  V_POLICY_NO_LOG:=p_department_code||','||v_sub_runTime||','||v_pay_runTime;

  if v_sub_runTime  is null then
     if v_pay_runTime is null then
         open v_cursor for
           select  ''''||w.FINANCE_DEPARTMENT_CODE  财务机构,
         ''''||w.ACCOUNT_MANAGE_NO  杂费编码,
         ''''||(select ar.cancel_manage_no from account_notice_relation ar where ar.account_manage_no=w.account_manage_no and rownum =1)   冲销台账编码,
         ''''||w.economic_matters_id||(select em.economic_matters_name from economic_matter em where em.economic_matters_id =w.economic_matters_id) 杂费付款经济事项,
         ''''||(select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no and b.notice_no is null) 付款通知单号,
          ''''||decode(w.ACCOUNT_MANAGE_STATUS,'01','暂存','02','已提交','03','已复核','04','已制证','05','已作废','06','已复审','其他') 杂费状态,
           ''''||decode((select notice_status from payment_notice where notice_no in  (select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no
           and b.notice_no is null)),'51','已提交','54','已复核','62','已抽档','72','支付成功','81','已制证','42','暂存','其他') 付款状态,
         ''''||w.submit_date  提交时间,
         ''''||p.voucher_no  实付凭证号,
          ''''||(SELECT M.SUBJECT_NO
                 FROM ECONOMIC_MATTER M
                WHERE M.ECONOMIC_MATTERS_ID = w.economic_matters_id) 科目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='142'),'0000') 业务段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='141'),'000000') 产品段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 成本中心,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='143'),'000000') 子目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 关联方,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='138'),'0000')  弹性域客户段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='131'),'0000') 与外部单位往来,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='136'),'0000') 责任部门及责任人,
         ''''||(select a.date_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='101')  预计清理日期,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='145'),'***') 凭证摘要,
         ''''||(p.client_name)  收款人账户名称,
          ''''||(substr(p.client_bank_account,0,(length(p.client_bank_account)-8))||'****'||substr(p.client_bank_account,-4))  收款人银行账号,
         ''''||w.currency_code   币种,
         ''''||to_char(abs(nvl(w.AMOUNT,0)),'fm999999999999990.00') 金额,
          ''''||(p.client_certificate_type)  证件类型,
           case  when p.client_certificate_type ='01'  then  ''''||(substr(p.client_certificate_no,0,(length(p.client_certificate_no)-12))||'********'||substr(p.client_certificate_no,-4)) else p.client_certificate_no end   证件号码,
          ''''||(p.province_name)  银行省份,
         ''''||(p.city_name)  银行城市,
         ''''|| (p.client_bank_code)  银行大类,
         ''''|| (p.client_bank_name)  开户行明细,
         ''''|| (p.bank_account_attribute)  账户类型,
         ''''||w.remark  付款备注
        from account_manage w ,business_receipt_relation b, collect_pay_info c ,payment_notice p
        where w.DATA_TYPE='6'
        and w.data_source='12'
        and w.finance_department_code like (p_department_code)
        and  b.policy_no = w.account_manage_no
        and b.collect_pay_no = c.collect_pay_no
        and c.notice_no =p.notice_no
        and rownum < 65000
        order by  w.ACCOUNT_MANAGE_NO;

       re_cursor := v_cursor;

     else
       open v_cursor for
         select  ''''||w.FINANCE_DEPARTMENT_CODE  财务机构,
         ''''||w.ACCOUNT_MANAGE_NO  杂费编码,
         ''''||(select ar.cancel_manage_no from account_notice_relation ar where ar.account_manage_no=w.account_manage_no and rownum =1)   冲销台账编码,
         ''''||w.economic_matters_id||(select em.economic_matters_name from economic_matter em where em.economic_matters_id =w.economic_matters_id) 杂费付款经济事项,
         ''''||(select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no and b.notice_no is null) 付款通知单号,
          ''''||decode(w.ACCOUNT_MANAGE_STATUS,'01','暂存','02','已提交','03','已复核','04','已制证','05','已作废','06','已复审','其他') 杂费状态,
          ''''||decode((select notice_status from payment_notice where notice_no in  (select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no
           and b.notice_no is null)),'51','已提交','54','已复核','62','已抽档','72','支付成功','81','已制证','42','暂存','其他') 付款状态,
         ''''||w.submit_date  提交时间,
         ''''||p.voucher_no  实付凭证号,
         ''''||(SELECT M.SUBJECT_NO
                 FROM ECONOMIC_MATTER M
                WHERE M.ECONOMIC_MATTERS_ID = w.economic_matters_id) 科目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='142'),'0000') 业务段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='141'),'000000') 产品段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 成本中心,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='143'),'000000') 子目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 关联方,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='138'),'0000')  弹性域客户段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='131'),'0000') 与外部单位往来,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='136'),'0000') 责任部门及责任人,
         ''''||(select a.date_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='101')  预计清理日期,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='145'),'***') 凭证摘要,
         ''''||(p.client_name)  收款人账户名称,
           ''''||(substr(p.client_bank_account,0,(length(p.client_bank_account)-8))||'****'||substr(p.client_bank_account,-4))  收款人银行账号,
         ''''||w.currency_code   币种,
         ''''||to_char(abs(nvl(w.AMOUNT,0)),'fm999999999999990.00') 金额,
          ''''||(p.client_certificate_type)  证件类型,
           case  when p.client_certificate_type ='01'  then  ''''||(substr(p.client_certificate_no,0,(length(p.client_certificate_no)-12))||'********'||substr(p.client_certificate_no,-4)) else p.client_certificate_no end   证件号码,
          ''''||(p.province_name)  银行省份,
         ''''||(p.city_name)  银行城市,
         ''''|| (p.client_bank_code)  银行大类,
         ''''|| (p.client_bank_name)  开户行明细,
         ''''|| (p.bank_account_attribute)  账户类型,
         ''''||w.remark  付款备注
         from account_manage w ,business_receipt_relation b, collect_pay_info c ,payment_notice p
        where w.DATA_TYPE='6'
        and w.data_source='12'
        and w.finance_department_code like (p_department_code)
        and  b.policy_no = w.account_manage_no
        and b.collect_pay_no = c.collect_pay_no
        and c.notice_no =p.notice_no
        and p.pay_date>= to_date(v_pay_runTime,'yyyymm')
        and p.pay_date<  TRUNC(last_day(to_date(v_pay_runTime, 'yyyymm')) + 1, 'MM')
        and p.notice_status in ('72','81')
        and rownum < 65000
        order by  w.ACCOUNT_MANAGE_NO;

       re_cursor := v_cursor;


     end if;

  else


   if v_pay_runTime is null then
         open v_cursor for
         select  ''''||w.FINANCE_DEPARTMENT_CODE  财务机构,
         ''''||w.ACCOUNT_MANAGE_NO  杂费编码,
         ''''||(select ar.cancel_manage_no from account_notice_relation ar where ar.account_manage_no=w.account_manage_no and rownum =1)   冲销台账编码,
         ''''||w.economic_matters_id||(select em.economic_matters_name from economic_matter em where em.economic_matters_id =w.economic_matters_id) 杂费付款经济事项,
         ''''||(select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no and b.notice_no is null) 付款通知单号,
          ''''||decode(w.ACCOUNT_MANAGE_STATUS,'01','暂存','02','已提交','03','已复核','04','已制证','05','已作废','06','已复审','其他') 杂费状态,
          ''''||decode((select notice_status from payment_notice where notice_no in  (select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no
           and b.notice_no is null)),'51','已提交','54','已复核','62','已抽档','72','支付成功','81','已制证','42','暂存','其他') 付款状态,
         ''''||w.submit_date  提交时间,
         ''''||p.voucher_no  实付凭证号,
          ''''||(SELECT M.SUBJECT_NO
                 FROM ECONOMIC_MATTER M
                WHERE M.ECONOMIC_MATTERS_ID = w.economic_matters_id) 科目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='142'),'0000') 业务段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='141'),'000000') 产品段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 成本中心,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='143'),'000000') 子目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 关联方,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='138'),'0000')  弹性域客户段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='131'),'0000') 与外部单位往来,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='136'),'0000') 责任部门及责任人,
         ''''||(select a.date_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='101')  预计清理日期,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='145'),'***') 凭证摘要,
         ''''||(p.client_name)  收款人账户名称,
          ''''||(substr(p.client_bank_account,0,(length(p.client_bank_account)-8))||'****'||substr(p.client_bank_account,-4))  收款人银行账号,
         ''''||w.currency_code   币种,
         ''''||to_char(abs(nvl(w.AMOUNT,0)),'fm999999999999990.00') 金额,
          ''''||(p.client_certificate_type)  证件类型,
           case  when p.client_certificate_type ='01'  then  ''''||(substr(p.client_certificate_no,0,(length(p.client_certificate_no)-12))||'********'||substr(p.client_certificate_no,-4)) else p.client_certificate_no end   证件号码,
          ''''||(p.province_name)  银行省份,
         ''''||(p.city_name)  银行城市,
         ''''|| (p.client_bank_code)  银行大类,
         ''''|| (p.client_bank_name)  开户行明细,
         ''''|| (p.bank_account_attribute)  账户类型,
         ''''||w.remark  付款备注
         from account_manage w ,business_receipt_relation b, collect_pay_info c ,payment_notice p
        where w.DATA_TYPE='6'
        and w.data_source='12'
        and w.finance_department_code like (p_department_code)
        and b.policy_no = w.account_manage_no
        and b.collect_pay_no = c.collect_pay_no
        and c.notice_no =p.notice_no
        and w.submit_date>= to_date(v_sub_runTime,'yyyymm')
        and w.submit_date< TRUNC(last_day(to_date(v_sub_runTime, 'yyyymm')) + 1, 'MM')
        and rownum < 65000
        order by w.ACCOUNT_MANAGE_NO;

       re_cursor := v_cursor;

   else

   open v_cursor for
          select  ''''||w.FINANCE_DEPARTMENT_CODE  财务机构,
         ''''||w.ACCOUNT_MANAGE_NO  杂费编码,
         ''''||(select ar.cancel_manage_no from account_notice_relation ar where ar.account_manage_no=w.account_manage_no and rownum =1)   冲销台账编码,
         ''''||w.economic_matters_id||(select em.economic_matters_name from economic_matter em where em.economic_matters_id =w.economic_matters_id) 杂费付款经济事项,
         ''''||(select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no and b.notice_no is null) 付款通知单号,
          ''''||decode(w.ACCOUNT_MANAGE_STATUS,'01','暂存','02','已提交','03','已复核','04','已制证','05','已作废','06','已复审','其他') 杂费状态,
          ''''||decode((select notice_status from payment_notice where notice_no in  (select max(c.notice_no) keep(dense_rank first order by b.updated_date desc)
          from business_receipt_relation b, collect_pay_info c
         where b.collect_pay_no = c.collect_pay_no
           and b.policy_no = w.account_manage_no
           and b.notice_no is null)),'51','已提交','54','已复核','62','已抽档','72','支付成功','81','已制证','42','暂存','其他') 付款状态,
         ''''||w.submit_date  提交时间,
         ''''||p.voucher_no  实付凭证号,
          ''''||(SELECT M.SUBJECT_NO
                 FROM ECONOMIC_MATTER M
                WHERE M.ECONOMIC_MATTERS_ID = w.economic_matters_id) 科目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='142'),'0000') 业务段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='141'),'000000') 产品段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 成本中心,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='143'),'000000') 子目,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='140'),'0000') 关联方,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='138'),'0000')  弹性域客户段,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='131'),'0000') 与外部单位往来,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='136'),'0000') 责任部门及责任人,
         ''''||(select a.date_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='101')  预计清理日期,
         ''''||nvl((select a.var_value from account_attribute_relation a where a.account_manage_no =w.account_manage_no and a.economic_attribute_id ='145'),'***') 凭证摘要,
         ''''||(p.client_name)  收款人账户名称,
           ''''||(substr(p.client_bank_account,0,(length(p.client_bank_account)-8))||'****'||substr(p.client_bank_account,-4))  收款人银行账号,
         ''''||w.currency_code   币种,
         ''''||to_char(abs(nvl(w.AMOUNT,0)),'fm999999999999990.00') 金额,
          ''''||(p.client_certificate_type)  证件类型,
           case  when p.client_certificate_type ='01'  then  ''''||(substr(p.client_certificate_no,0,(length(p.client_certificate_no)-12))||'********'||substr(p.client_certificate_no,-4)) else p.client_certificate_no end   证件号码,
          ''''||(p.province_name)  银行省份,
         ''''||(p.city_name)  银行城市,
         ''''|| (p.client_bank_code)  银行大类,
         ''''|| (p.client_bank_name)  开户行明细,
         ''''|| (p.bank_account_attribute)  账户类型,
         ''''||w.remark  付款备注
         from account_manage w ,business_receipt_relation b, collect_pay_info c ,payment_notice p
        where w.DATA_TYPE='6'
        and w.data_source='12'
        and w.finance_department_code like (p_department_code)
       and  b.policy_no = w.account_manage_no
        and b.collect_pay_no = c.collect_pay_no
        and c.notice_no =p.notice_no
        and w.submit_date>= to_date(v_sub_runTime,'yyyymm')
        and w.submit_date< TRUNC(last_day(to_date(v_sub_runTime, 'yyyymm')) + 1, 'MM')
        and p.pay_date>= to_date(v_pay_runTime,'yyyymm')
        and p.pay_date<  TRUNC(last_day(to_date(v_pay_runTime, 'yyyymm')) + 1, 'MM')
        and p.notice_status in ('72','81')
        and rownum < 65000
        order by  w.ACCOUNT_MANAGE_NO;

       re_cursor := v_cursor;


   end if;


  end if;


exception
     when others then
      pkg_finance_exception_log.process_exception_log(null,
                                                    V_POLICY_NO_LOG,
                                                    'pkg_ereport_finance_due_new',
                                                    'pro_finance_incidental_pay',
                                                    substr(sqlerrm, 1, 200));
     v_ErrMsg := substr('获取数据过程出错! ' || sqlerrm, 1, 500);
      raise_application_error(-20001, v_ErrMsg);
     rollback;
end pro_finance_incidental_pay;

--每日定时删除不应该存在应收的数据
procedure del_incorrect_duepremium_long Is
  v_due_amount   number;
  v_total_amount number;
  v_n_dueamount  number;
  --删除前一天的数据
  p_voucher_date date := trunc(Sysdate) - 1;

  cursor t_policy_no is
    SELECT a.policy_no
      FROM tmp_finance_duepremium_long_n a
     WHERE greatest (a.UNDERWRITE_TIME,a.insurance_begin_date) = p_voucher_date
       and a.TOTAL_AMOUNT < 0
     group by a.policy_no;

begin

  v_due_amount   := -1;
  v_total_amount := -1;
  v_n_dueamount  := -1;
  for cur in t_policy_no loop
    begin
      select sum(p.due_amount), sum(p.total_amount)
        into v_due_amount, v_total_amount
        from premium_info p
       where p.policy_no = cur.policy_no
         and p.DISABLE_FLAG = 'Y'
       group by p.policy_no;
    exception
      when no_data_found then
        v_due_amount   := 0;
        v_total_amount := 0;
      when others then
        v_due_amount   := -1;
        v_total_amount := -1;
    end;

    --如果符合删除条件
    if v_due_amount = 0 and v_total_amount = 0 then

      --单独的负数应收不能删除 ，所以需要继续筛选一遍

      SELECT sum(a.DUE_PREMIUM)
        into v_n_dueamount
        FROM tmp_finance_duepremium_long_n a, premium_info p
       WHERE a.RECEIPT_NO = p.RECEIPT_NO
         and p.policy_no = cur.policy_no
         and p.DISABLE_FLAG = 'Y';

    end if;

    --删除符合条件的数据
    if v_n_dueamount = 0 then
      for c_re in (select a.RECEIPT_NO
                     from premium_info a
                    where a.policy_no = cur.policy_no
                      and a.DISABLE_FLAG = 'Y') loop
        delete from tmp_finance_duepremium_long_n a
         where a.RECEIPT_NO = c_re.RECEIPT_NO;
        delete from tmp_finance_duepremium_long_o a
         where a.RECEIPT_NO = c_re.RECEIPT_NO;

      end loop;
    end if;

    v_due_amount   := -1;
    v_total_amount := -1;
    v_n_dueamount  := -1;
  end loop;
  commit;
exception
  when others then
    If t_policy_no%Isopen Then
      Close t_policy_no;
    End If;
    Raise;

end del_incorrect_duepremium_long;

END pkg_ereport_finance_due_new
