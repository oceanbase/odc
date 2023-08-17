DELIMITER $$
CREATE OR REPLACE package body g_core is

type ret_code_tbl_rt is table of varchar2(8);
type amount_tbl_rt is table of number(12, 2);
type bool_tbl_rt is table of boolean;

--prepaid data count
type state_domain_0_rt is record (charged number(10), uncharged number(10), ongoing number(10));
-- postpaid data count
type state_domain_1_rt is record (id raw(16), charged number(10), uncharged number(10));
type state_domain_1_aa is table of state_domain_1_rt index by varchar2(8);
-- prepaid amount sum
type state_domain_2_rt is record (charged number(12,2), ongoing number(12,2));
-- postpaid amount sum
type state_domain_3_rt is record (charged number(12,2));

-- service & rule data for START_TRANS process
type enter_context_rt is record (
	s_id mv_charge_core.s_id%TYPE,
	customer_id mv_charge_core.customer_id%TYPE,
	p_id mv_charge_core.p_id%TYPE,
	product_code mv_charge_core.product_code%TYPE,
	start_time mv_charge_core.start_time%TYPE,
	finish_time mv_charge_core.finish_time%TYPE,
	s_state mv_charge_core.s_state%TYPE,
	rule_plan mv_charge_core.rule_plan%TYPE,
	exists_gift_balance mv_charge_core.exists_gift_balance%TYPE,
	enable_local_gift mv_charge_core.enable_local_gift%TYPE,
	a_id mv_charge_core.a_id%TYPE,
	a_state mv_charge_core.a_state%TYPE,
	data_quanlity mv_charge_core.data_quanlity%TYPE,
	a_type mv_charge_core.a_type%TYPE,
	r_id mv_charge_core.r_id%TYPE,
	priority mv_charge_core.priority%TYPE,
	unit mv_charge_core.unit%TYPE,
	source mv_charge_core.source%TYPE,
	enter_policy mv_charge_core.enter_policy%TYPE,
	enter_conf mv_charge_core.enter_conf%TYPE,
	domains mv_charge_core.domains%TYPE,
	op_id mv_charge_core.op_id%TYPE,
	output_plan mv_charge_core.output_plan%TYPE
);
type enter_rule_context_rt is record (
	r_id g_charge_service_rule.recid%TYPE,
	priority g_charge_service_rule.priority%TYPE,
	unit g_charge_service_rule.balance_unit%TYPE,
	source g_charge_service_rule.balance_source%TYPE,
	enter_policy g_charge_service_rule.enter_policy%TYPE,
	enter_conf g_charge_service_rule.enter_conf%TYPE,
	domains g_charge_service_rule.domains%TYPE
);

-- service & rule data for FINISH_TRANS
type exit_context_rt is record (
	s_id mv_charge_core.s_id%TYPE,
	customer_id mv_charge_core.customer_id%TYPE,
	enable_local_gift mv_charge_core.enable_local_gift%TYPE,
	r_id mv_charge_core.r_id%TYPE,
	priority mv_charge_core.priority%TYPE,
	unit mv_charge_core.unit%TYPE,
	source mv_charge_core.source%TYPE,
	exit_policy mv_charge_core.exit_policy%TYPE,
	exit_conf mv_charge_core.exit_conf%TYPE,
	exit_ref mv_charge_core.exit_ref%TYPE,
	domains mv_charge_core.domains%TYPE,
	cc_id mv_charge_core.cc_id%TYPE,
	charge_condition mv_charge_core.charge_condition%TYPE
);
type exit_rule_context_rt is record (
	r_id g_charge_service_rule.recid%TYPE,
	priority g_charge_service_rule.priority%TYPE,
	unit g_charge_service_rule.balance_unit%TYPE,
	source g_charge_service_rule.balance_source%TYPE,
	exit_policy mv_charge_core.exit_policy%TYPE,
	exit_conf mv_charge_core.exit_conf%TYPE,
	exit_ref mv_charge_core.exit_ref%TYPE,
	domains mv_charge_core.domains%TYPE
);
type exit_rule_context_tbl_rt is table of exit_rule_context_rt;
type rc_exit_rule_context_map_aat is table of exit_rule_context_rt index by varchar2(8);

-- transaction & entry data for FINISH_TRANS
type trans_ongoing_rt is record (
	recid g_transaction_ongoing.recid%TYPE,
	seat_id g_transaction_ongoing.seat_id%TYPE,
	service_id g_transaction_ongoing.service_id%TYPE,
	request_time g_transaction_ongoing.request_time%TYPE,
	data_count g_transaction_ongoing.data_count%TYPE,
	start_time g_transaction_ongoing.start_time%TYPE,
	trans_state g_transaction_ongoing.trans_state%TYPE,
	hit_rule g_transaction_ongoing.hit_rule%TYPE,
	balance_unit g_transaction_ongoing.balance_unit%TYPE,
	balance_source g_transaction_ongoing.balance_source%TYPE,
	enter_amount_sum g_transaction_ongoing.enter_amount_sum%TYPE
);
type trans_ongoing_entry_rt is record (
	recid g_transaction_ongoing_entry.recid%TYPE,
	trans_id g_transaction_ongoing_entry.trans_id%TYPE,
	seq g_transaction_ongoing_entry.seq%TYPE,
	enter_amount g_transaction_ongoing_entry.enter_amount%TYPE
);
type trans_ongoing_entry_tbl_rt is table of trans_ongoing_entry_rt;

-- transaction & entry data for MOVE_TRANS
type trans_moving_rt is record (
	recid g_transaction_ongoing.recid%TYPE,
	seat_id g_transaction_ongoing.seat_id%TYPE,
	service_id g_transaction_ongoing.service_id%TYPE,
	request_time g_transaction_ongoing.request_time%TYPE,
	data_count g_transaction_ongoing.data_count%TYPE,
	start_time g_transaction_ongoing.start_time%TYPE,
	finish_time g_transaction_ongoing.finish_time%TYPE,
	trans_state g_transaction_ongoing.trans_state%TYPE,
	hit_rule g_transaction_ongoing.hit_rule%TYPE,
	balance_unit g_transaction_ongoing.balance_unit%TYPE,
	balance_source g_transaction_ongoing.balance_source%TYPE,
	enter_amount_sum g_transaction_ongoing.enter_amount_sum%TYPE,
	exit_amount_sum g_transaction_ongoing.exit_amount_sum%TYPE,
	charge_amount_sum g_transaction_ongoing.charge_amount_sum%TYPE,
	charge_data_count g_transaction_ongoing.charge_data_count%TYPE,
	call_type g_transaction_ongoing.call_type%TYPE,
	call_ip4_addr g_transaction_ongoing.call_ip4_addr%TYPE
);
type trans_moving_tbl_rt is table of trans_moving_rt;
type trans_entry_moving_rt is record (
	recid g_transaction_ongoing_entry.recid%TYPE,
	trans_id g_transaction_ongoing_entry.trans_id%TYPE,
	seq g_transaction_ongoing_entry.seq%TYPE,
	enter_amount g_transaction_ongoing_entry.enter_amount%TYPE,
	exit_amount g_transaction_ongoing_entry.exit_amount%TYPE,
	charge_amount g_transaction_ongoing_entry.charge_amount%TYPE,
	ret_code g_transaction_ongoing_entry.ret_code%TYPE,
	charge_condition g_transaction_ongoing_entry.charge_condition%TYPE
);
type trans_entry_moving_tbl_rt is table of trans_entry_moving_rt;
type retcode_map_aat is table of number index by varchar2(8);

------------------------------ 30 limit
not_yet exception;
unknown_balance_source exception;
corrupted_global_balance exception;
corrupted_local_balance exception;
unknown_ruleplan exception;
unknown_valuepolicy exception;
corrupted_fixvaluepolicy exception;
corrupted_ldrvaluepolicy exception;
missing_rule_domain_0 exception;
missing_rule_domain_1 exception;
------------------------------ 30 limit
prany_missing_enter_policy exception;
unknown_ldrvaluepolicy_param exception;
missing_charge_service exception;
prany_missing_hit_rule exception;
missing_charge_condition exception;
unexpected_ongoing_entry exception;
poany_missing_exit_policy exception;
poany_evaluate_failed exception;
corrupted_mapvaluepolicy exception;
unknown_mapvaluepolicy_param exception;
------------------------------ 30 limit
missing_rule_domain_2 exception;
missing_rule_domain_3 exception;
missing_global_balance exception;
missing_local_balance exception;
insufficient_test_balance exception;
rc_missing_exit_ref exception;
rc_duplicate_exit_ref exception;

function parse_ldr_param (
	conf varchar2
) return varchar2
is
	i_dlr pls_integer;
begin
	i_dlr := instr(conf, '$');
	if i_dlr = 0 then
		raise corrupted_ldrvaluepolicy;
	end if;
	return substr(conf, 1, i_dlr - 1);
end;

procedure calc_amount_enter_all_fix (
	conf in varchar2,
	data_count in pls_integer,
	a_tbl out amount_tbl_rt,
	a_sum out number
) is
	i pls_integer;
	unit_value number(12,2);
begin
	begin
		select cast(conf as number(12,2)) into unit_value from dual;
	exception
		when others then raise corrupted_fixvaluepolicy;
	end;
	a_tbl := amount_tbl_rt();
	a_tbl.extend(data_count);
	a_sum := 0;
	i := 1;
	while i <= data_count loop
		a_tbl(i) := unit_value;
		i := i + 1;
	end loop;
	a_sum := unit_value * data_count;
end;

-- conf: 
-- data_count: total data count to calculate
-- base: value to determine step
-- offset: offset in conf of current step
-- filter: filter by ret_code_tbl 
-- a_i: index of data count, should reach data_count eventually
-- a_tbl: amount 
-- a_sum:
procedure calc_amount_all_ldr_dc (
	conf in varchar2,
	data_count in pls_integer,
	base in out pls_integer,
	offset in out pls_integer,
	filter in bool_tbl_rt,
	a_i in out pls_integer,
	a_tbl in out amount_tbl_rt,
	a_sum in out number
) is
	i_cln pls_integer;
	i_scln pls_integer;
	step_to pls_integer;
	unit_value number;
begin
	loop
		i_cln := instr(conf, ':', offset);
		begin
			select cast(substr(conf, offset, i_cln - offset) as number) into step_to from dual;
		exception
			when others then raise corrupted_ldrvaluepolicy;
		end;
		if step_to = -1 then
			begin
				select cast(substr(conf, i_cln + 1) as number) into unit_value from dual;
			exception
				when others then raise corrupted_ldrvaluepolicy;
			end;
		else
			i_scln := instr(conf, ';', i_cln + 1);
			begin
				select cast(substr(conf, i_cln + 1, i_scln - i_cln -1) as number) into unit_value from dual;
			exception
				when others then raise corrupted_ldrvaluepolicy;
			end;
		end if;
		
		if step_to = -1 then
			while a_i <= data_count loop
				if a_tbl is null then 
					a_tbl := amount_tbl_rt();
				end if;
				a_tbl.extend(1);
				if filter is null then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
				elsif filter(a_i) then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
				else
					a_tbl(a_i) := 0;
				end if;
				a_i := a_i + 1;
			end loop;
			exit;
		elsif base + 1 < step_to then
			loop
				if a_tbl is null then
					a_tbl := amount_tbl_rt();
				end if;
				a_tbl.extend(1);
				if filter is null then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
					base := base + 1;
				elsif filter(a_i) then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
					base := base + 1;
				else
					a_tbl(a_i) := 0;
				end if;
				if a_i = data_count then
					return;
				else
					a_i := a_i + 1;
					if base + 1 = step_to then
						exit;
					else
						continue;
					end if;
				end if;
			end loop;
		end if;
		offset := i_scln + 1;
	end loop;
end;

procedure calc_amount_all_ldr_as (
	conf in varchar2,
	data_count in pls_integer,
	base in out number,
	offset in out pls_integer,
	filter in bool_tbl_rt,
	a_i in out pls_integer,
	a_tbl in out amount_tbl_rt,
	a_sum in out number
) is
	i_cln pls_integer;
	i_scln pls_integer;
	step_to pls_integer;
	unit_value number;
begin
	loop
		i_cln := instr(conf, ':', offset);
		begin
			select cast(substr(conf, offset, i_cln - offset) as number) into step_to from dual;
		exception
			when others then raise corrupted_ldrvaluepolicy;
		end;
		if step_to = -1 then
			begin
				select cast(substr(conf, i_cln + 1) as number) into unit_value from dual;
			exception
				when others then raise corrupted_ldrvaluepolicy;
			end;
		else
			i_scln := instr(conf, ';', i_cln + 1);
			begin
				select cast(substr(conf, i_cln + 1, i_scln - i_cln -1) as number) into unit_value from dual;
			exception
				when others then raise corrupted_ldrvaluepolicy;
			end;
		end if;
		
		if step_to = -1 then
			while a_i <= data_count loop
				if a_tbl is null then 
					a_tbl := amount_tbl_rt();
				end if;
				a_tbl.extend(1);
				if filter is null then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
				elsif filter(a_i) then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
				else
					a_tbl(a_i) := 0;
				end if;
				a_i := a_i + 1;
			end loop;
			exit;
		elsif base < step_to then
			loop
				if a_tbl is null then
					a_tbl := amount_tbl_rt();
				end if;
				a_tbl.extend(1);
				if filter is null then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
					base := base + unit_value;
				elsif filter(a_i) then
					a_tbl(a_i) := unit_value;
					a_sum := a_sum + unit_value;
					base := base + unit_value;
				else
					a_tbl(a_i) := 0;
				end if;
				if a_i = data_count then
					return;
				else
					a_i := a_i + 1;
					if base >= step_to then
						exit;
					else
						continue;
					end if;
				end if;
			end loop;
		end if;
		offset := i_scln + 1;
	end loop;
end;

-- all data use same rule to calculate amount
procedure calc_amount_enter_all (
	policy in varchar2,
	conf in varchar2,
	data_count in pls_integer,
	state_domain_0 in state_domain_0_rt,
	state_domain_2 in state_domain_2_rt,
	a_tbl out amount_tbl_rt,
	a_sum out number
) is
begin
	if policy = 'FIX' then
		calc_amount_enter_all_fix(conf, data_count, a_tbl, a_sum);
	elsif policy = 'LDR_DC' then
		declare
			param_name varchar2(8);
			base pls_integer;
			offset pls_integer;
			a_i pls_integer;
		begin
			param_name := parse_ldr_param(conf);
			offset := length(param_name) + 2;
			a_i := 1;
			a_sum := 0;
			if param_name = 'ODC' then
				base := state_domain_0.charged + state_domain_0.ongoing;
				calc_amount_all_ldr_dc(conf, data_count, base, offset, null, a_i, a_tbl, a_sum);
			else
				raise unknown_ldrvaluepolicy_param;
			end if;
		end;
	elsif policy = 'LDR_AS' then
		declare
			param_name varchar2(8);
			base pls_integer;
			offset pls_integer;
			a_i pls_integer;
		begin
			param_name := parse_ldr_param(conf);
			offset := length(param_name) + 2;
			a_i := 1;
			a_sum := 0;
			if param_name = 'OAS' then
				base := state_domain_2.charged + state_domain_2.ongoing;
				calc_amount_all_ldr_as(conf, data_count, base, offset, null, a_i, a_tbl, a_sum);
			else
				raise unknown_ldrvaluepolicy_param;
			end if;
		end;
	else
		raise unknown_valuepolicy;
	end if;
end;

procedure try_lock_balance_local (
	p_service_id in raw,
	p_balance_unit in varchar2,
	p_balance_amount in number,
	eval_okay out boolean
) is
begin
	update g_charge_service_balance t
		set locked_amount = t.locked_amount + p_balance_amount
		where t.service_id = p_service_id and t.balance_unit = p_balance_unit
			and t.limit_amount - t.locked_amount - t.consumed_amount - p_balance_amount >= 0;
	if SQL%ROWCOUNT = 0 then 
		eval_okay := FALSE;
	elsif SQL%ROWCOUNT = 1 then
		eval_okay := TRUE;
	else
		raise corrupted_local_balance;
	end if;
end;

procedure try_lock_balance_global (
	p_customer_id in raw,
	p_balance_unit in varchar2,
	p_balance_amount in number,
	okay out boolean
) is
begin
	update g_customer_balance t
		set locked_amount = t.locked_amount + p_balance_amount, available_amount = t.available_amount - p_balance_amount
		where t.customer_id = p_customer_id	and t.balance_unit = p_balance_unit
			and t.available_amount - p_balance_amount >= 0;
	if SQL%ROWCOUNT = 0 then 
		okay := FALSE;
	elsif SQL%ROWCOUNT = 1 then
		okay := TRUE;
	else
		raise corrupted_global_balance;
	end if;
end;

procedure evaluate_enter_rule_prany (
	ctx in enter_context_rt,
	r in enter_rule_context_rt,
	p_data_count in pls_integer,
	eval_okay out boolean,
	using_rule out pls_integer,
	using_source out varchar2,
	using_unit out varchar2,
	amount_tbl out amount_tbl_rt,
	amount_sum out number
) is
	cursor sd0_c (p_rule_id raw) 
		is select charged_data_count, uncharged_data_count, ongoing_data_count
		from g_charge_rule_state_0 where recid = p_rule_id for update;
	cursor sd2_c (p_rule_id raw)
		is select charged_amount_sum, ongoing_amount_sum 
		from g_charge_rule_state_2 where recid = p_rule_id for update;
	state_domain_0 state_domain_0_rt;
	state_domain_2 state_domain_2_rt;
begin
	if r.enter_policy is null then
		raise prany_missing_enter_policy;
	end if;
	
	if utl_raw.bit_and(r.domains, hextoraw('01000000')) = hextoraw('01000000') then
		open sd0_c (r.r_id);
		fetch sd0_c into state_domain_0;
		if sd0_c%NOTFOUND then
			raise missing_rule_domain_0;
		end if;
	end if;

	if utl_raw.bit_and(r.domains, hextoraw('04000000')) = hextoraw('04000000') then
		open sd2_c (r.r_id);
		fetch sd2_c into state_domain_2;
		if sd2_c%NOTFOUND then
			raise missing_rule_domain_2;
		end if;
	end if;
	
	-- to ensures that cursor will be closed correctly
	begin
		calc_amount_enter_all(r.enter_policy, r.enter_conf, p_data_count,
			state_domain_0, state_domain_2, amount_tbl, amount_sum);
		
		if r.source = 'LOCAL' then
			try_lock_balance_local(ctx.s_id, r.unit, amount_sum, eval_okay);
		elsif r.source = 'GLOBAL' then
			try_lock_balance_global(ctx.customer_id, r.unit, amount_sum, eval_okay);
		elsif r.source = 'NONE' then
			eval_okay := TRUE;
		else
			raise unknown_balance_source;
		end if;
		
		if eval_okay then
			if utl_raw.bit_and(r.domains, hextoraw('01000000')) = hextoraw('01000000') then
				update g_charge_rule_state_0 set ongoing_data_count = ongoing_data_count + p_data_count
					where current of sd0_c;
				close sd0_c;
			end if;
			
			if utl_raw.bit_and(r.domains, hextoraw('04000000')) = hextoraw('04000000') then
				update g_charge_rule_state_2 set ongoing_amount_sum = ongoing_amount_sum + amount_sum
					where current of sd2_c;
				close sd2_c;
			end if;
			
			using_rule := r.priority;
			using_unit := r.unit;
			using_source := r.source;
		elsif r.source = 'LOCAL' and r.unit = 'TEST' then
			raise insufficient_test_balance; -- XXX not quite reasonable through raising exception
		end if;
	exception when others then
		if sd0_c%ISOPEN then close sd0_c; end if;
		if sd2_c%ISOPEN then close sd2_c; end if;
		raise;
	end;
	
	if sd0_c%ISOPEN then close sd0_c; end if;
	if sd2_c%ISOPEN then close sd2_c; end if;
end;

-- PR_ANY require each rule contains a enter policy
procedure evaluate_enter_plan_prany (
	ctx in enter_context_rt,
	p_data_count in pls_integer,
	eval_okay out boolean,
	using_rule out pls_integer,
	using_source out varchar2,
	using_unit out varchar2,
	amount_tbl out amount_tbl_rt,
	amount_sum out number
) is
	cursor c is 
		select
			r.recid rule_id,
			r.priority,
			r.balance_unit,
			r.balance_source,
			r.enter_policy,
			r.enter_conf,
			r.domains
		from 
			g_charge_service_rule r
		where
			r.service_id = ctx.s_id and priority > 0
		order by
			r.priority;
	r c%ROWTYPE;
	i pls_integer;
begin
	eval_okay := FALSE;
	
	-- try local gift
	if ctx.enable_local_gift = 1 then
		try_lock_balance_local(ctx.s_id, 'GIFT', p_data_count, eval_okay);
		if eval_okay then 
			using_rule := -99;
			using_source := 'LOCAL';
			using_unit := 'GIFT';
			amount_tbl.extend(p_data_count);
			i := 1;
			while i <= p_data_count loop
				amount_tbl(i) := 1;
			end loop;
			amount_sum := p_data_count;
			return;
		end if;
	end if;
	
	-- fisrt rule fetch from materialized view
	r.rule_id := ctx.r_id;
	r.priority := ctx.priority;
	r.balance_unit := ctx.unit;
	r.balance_source := ctx.source;
	r.enter_policy := ctx.enter_policy;
	r.enter_conf := ctx.enter_conf;
	r.domains := ctx.domains;
	
	evaluate_enter_rule_prany(ctx, r, p_data_count,
		eval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);
	
	if eval_okay then
		return;
	end if;
	
	-- using FOR/LOOP in case of exception
	for x in c loop
		r.rule_id := x.rule_id;
		r.priority := x.priority;
		r.balance_unit := x.balance_unit;
		r.balance_source := x.balance_source;
		r.enter_policy := x.enter_policy;
		r.enter_conf := x.enter_conf;
		r.domains := x.domains;
		
		evaluate_enter_rule_prany(ctx, r, p_data_count,
			eval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);
		
		if eval_okay then
			return;
		end if;
	end loop;
end;

procedure evaluate_enter_plan_prorc (
	ctx in enter_context_rt,
	p_data_count in pls_integer,
	eval_okay out boolean,
	using_rule out pls_integer,
	using_source out varchar2,
	using_unit out varchar2,
	amount_tbl out amount_tbl_rt,
	amount_sum out number
) is
begin
	eval_okay := FALSE;
	
	if ctx.enable_local_gift = 1 then
		try_lock_balance_local(ctx.s_id, 'GIFT', p_data_count, eval_okay);
		if eval_okay then 
			using_rule := -99;
			using_source := 'LOCAL';
			using_unit := 'GIFT';
			amount_tbl.extend(p_data_count);
			for i in amount_tbl.first .. amount_tbl.last loop
				amount_tbl(i) := 1;
			end loop;
			amount_sum := p_data_count;
			return;
		end if;
	end if;
	
	using_rule := -1;
	using_source := ctx.source;
	using_unit := ctx.unit;
	
	calc_amount_enter_all(ctx.enter_policy, ctx.enter_conf, p_data_count,
		null, null, amount_tbl, amount_sum);
		
	if ctx.source = 'LOCAL' then
		try_lock_balance_local(ctx.s_id, ctx.unit, amount_sum, eval_okay);
	elsif ctx.source = 'GLOBAL' then
		try_lock_balance_global(ctx.customer_id, ctx.unit, amount_sum, eval_okay);
	elsif ctx.source = 'NONE' then
		eval_okay := TRUE;
	else
		raise unknown_balance_source;
	end if;
end;

procedure start_trans_standard (
	ctx in enter_context_rt,
	p_request_time in timestamp,
	p_transaction_id in raw,
	p_seat_id in raw,
	p_data_count in pls_integer,
	p_call_type in varchar2,
	p_call_ip4_addr in varchar2,
	state_code out pls_integer
) is
	pragma autonomous_transaction;
	state_domain_0 state_domain_0_rt;
	state_domain_2 state_domain_2_rt;
begin
	if ctx.rule_plan = 'PR_ANY' then
		declare
			eval_okay boolean;
			using_rule pls_integer;
			using_source varchar2(8);
			using_unit varchar2(8);
			amount_tbl amount_tbl_rt;
			amount_sum number(12,2);
		begin
			evaluate_enter_plan_prany(ctx, p_data_count, 
				eval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);
			if eval_okay then
				insert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,
						trans_state, hit_rule, data_count, balance_source, balance_unit,
						enter_amount_sum, call_type, call_ip4_addr)
					values (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),
						1, using_rule, p_data_count, using_source, using_unit,
						amount_sum, p_call_type, p_call_ip4_addr);
				for i in amount_tbl.first .. amount_tbl.last loop
					insert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)
						values (sys_guid(), p_transaction_id, i, amount_tbl(i));
				end loop;
				commit; state_code := 0; return;
			else
				rollback; state_code := 202; return;
			end if;
		end;
	elsif ctx.rule_plan = 'PO_ANY' then
		declare
			i pls_integer;
		begin
			insert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,
					trans_state, hit_rule, data_count, balance_source, balance_unit, enter_amount_sum, call_type, call_ip4_addr)
				values (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),
					2, -1, p_data_count, 'TBD', 'TBD', 0, p_call_type, p_call_ip4_addr);
			i := 1;
			while i <= p_data_count loop
				insert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)
					values (sys_guid(), p_transaction_id, i, 0);
				i := i + 1;
			end loop;
		end;
		commit; state_code := 0; return;
	elsif ctx.rule_plan = 'PR_O_RC' then 
		declare
			eval_okay boolean;
			using_rule pls_integer;
			using_source varchar2(8);
			using_unit varchar2(8);
			amount_tbl amount_tbl_rt;
			amount_sum number(12,2);
		begin
			evaluate_enter_plan_prorc(ctx, p_data_count, 
				eval_okay, using_rule, using_source, using_unit, amount_tbl, amount_sum);
			if eval_okay then
				insert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,
						trans_state, hit_rule, data_count, balance_source, balance_unit,
						enter_amount_sum, call_type, call_ip4_addr)
					values (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),
						4, using_rule, p_data_count, using_source, using_unit,
						amount_sum, p_call_type, p_call_ip4_addr);
				for i in amount_tbl.first .. amount_tbl.last loop
					insert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)
						values (sys_guid(), p_transaction_id, i, amount_tbl(i));
				end loop;
				commit; state_code := 0; return;
			else
				rollback; state_code := 202; return;
			end if;
		end;
	elsif ctx.rule_plan = 'PO_O_RC' then 
		declare
			i pls_integer;
		begin
			insert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,
					trans_state, hit_rule, data_count, balance_source, balance_unit, enter_amount_sum, call_type, call_ip4_addr)
				values (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),
					5, -1, p_data_count, 'TBD', 'TBD', 0, p_call_type, p_call_ip4_addr);
			i := 1;
			while i <= p_data_count loop
				insert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)
					values (sys_guid(), p_transaction_id, i, 0);
				i := i + 1;
			end loop;
		end;
	else
		raise unknown_ruleplan;
	end if;
exception
	when unknown_ruleplan then rollback; state_code := 532;
	when unknown_valuepolicy then rollback; state_code := 533;
	when corrupted_fixvaluepolicy then rollback; state_code := 534;
	when corrupted_ldrvaluepolicy then rollback; state_code := 535;
	when unknown_balance_source then rollback; state_code := 536;
	when corrupted_global_balance then rollback; state_code := 537;
	when corrupted_local_balance then rollback; state_code := 538;
	when missing_rule_domain_0 then rollback; state_code := 539;
	when prany_missing_enter_policy then rollback; state_code := 540;
	when unknown_ldrvaluepolicy_param then rollback; state_code := 541;
	when missing_rule_domain_2 then rollback; state_code := 549;
	when unknown_mapvaluepolicy_param then rollback; state_code := 553;
	when corrupted_mapvaluepolicy then rollback; state_code := 554;
	when insufficient_test_balance then rollback; state_code := 206;
	when not_yet then rollback; state_code := 1542;
	when others then rollback; state_code:= 1543;
end;

procedure start_trans_delegate (
	ctx in enter_context_rt,
	p_request_time in timestamp,
	p_transaction_id in raw,
	p_seat_id in raw,
	p_data_count in pls_integer,
	p_call_type in varchar2,
	p_call_ip4_addr in varchar2,
	state_code out pls_integer
) is
	pragma autonomous_transaction;
	i pls_integer;
begin
	insert into g_transaction_ongoing (recid, seat_id, service_id, request_time, start_time,
			trans_state, hit_rule, data_count, balance_source, balance_unit, enter_amount_sum, call_type, call_ip4_addr)
		values (p_transaction_id, p_seat_id, ctx.s_id, p_request_time, localtimestamp(3),
			3, -1, p_data_count, 'NONE', 'DELEGATE', p_data_count, p_call_type, p_call_ip4_addr);
	i := 1;
	while i <= p_data_count loop
		insert into g_transaction_ongoing_entry (recid, trans_id, seq, enter_amount)
			values (sys_guid(), p_transaction_id, i, 1);
		i := i + 1;
	end loop;
	state_code := 0;
	commit;
exception
	when others then rollback; state_code := 1543;
end;

procedure start_trans_ts (
	p_customer_id in raw,
	p_seat_id in raw,
	p_product_code in varchar2,
	p_data_count in pls_integer,
	p_mode in pls_integer,
	p_call_type in varchar2,
	p_call_ip4_addr in varchar2,
	p_request_time timestamp,
	state_code out pls_integer,
	transaction_id out raw,
	output_plan out varchar2,
	data_quanlity out varchar2
) is
	c_transaction_id raw(16);
	cursor c is
		select s_id, customer_id, p_id, product_code, start_time,
			finish_time, s_state, rule_plan, exists_gift_balance, enable_local_gift,
			a_id, a_state, data_quanlity, a_type, r_id, priority, unit, source,
			enter_policy, enter_conf, domains, op_id, output_plan
			from mv_charge_core 
			where customer_id = p_customer_id and product_code = p_product_code
				and p_request_time >= start_time and p_request_time < finish_time
			order by finish_time;
	ctx enter_context_rt;
begin
	
	if p_customer_id is null or p_seat_id is null or p_product_code is null then
		state_code := 101;
		return;
	end if;
	
	if p_data_count is null or p_data_count <= 0 then
		state_code := 101;
		return;
	end if;
	
	if p_mode <> 1 and p_mode <> 2 then
		state_code := 207;
		return;
	end if;
	
	state_code := 201;
	
	open c;
	begin
		loop
			fetch c into ctx;
			if c%NOTFOUND then
				rollback; close c; return;
			end if;
			
			if ctx.a_state <> 1 or ctx.s_state <> 1 then
				state_code := 209;
				continue;
			end if;
			
			if ctx.op_id is null then
				state_code := 543;
				continue;
			end if;
			
			select sys_guid() into transaction_id from dual;
		
			if p_mode = 1 then
				declare
					c_status pls_integer;
				begin
					select status into c_status from g_calltype_status s 
						where s.authr_id = ctx.a_id and s.calltype = p_call_type;
					if c_status = 1 and ctx.a_type = 7 or c_status = 2 and ctx.a_type = 1 then
						null;
					else
						state_code := 553; continue;
					end if;
				exception
					when no_data_found then state_code := 554; continue;
				end;
				start_trans_standard(ctx, p_request_time, transaction_id, 
					p_seat_id, p_data_count, p_call_type, p_call_ip4_addr,
					state_code);
			elsif p_mode = 2 then
				start_trans_delegate(ctx, p_request_time, transaction_id, 
					p_seat_id, p_data_count, p_call_type, p_call_ip4_addr,
					state_code);
			end if;
			
			if state_code = 0 then
				output_plan := ctx.output_plan;
				data_quanlity := ctx.data_quanlity;
				exit;
			end if;
		end loop;
	exception when others then
		if c%ISOPEN then close c; raise; end if;
	end;
	close c;
end;

procedure start_trans (
	p_customer_id in raw,
	p_seat_id in raw,
	p_product_code in varchar2,
	p_data_count in pls_integer,
	p_mode in pls_integer,
	p_call_type in varchar2,
	p_call_ip4_addr in varchar2,
	state_code out pls_integer,
	transaction_id out raw,
	output_plan out varchar2,
	data_quanlity out varchar2
) is
	c_request_time timestamp(3);
	c_transaction_id raw(16);
	cursor c is
		select s_id, customer_id, p_id, product_code, start_time,
			finish_time, s_state, rule_plan, exists_gift_balance, enable_local_gift,
			a_id, a_state, data_quanlity, a_type, r_id, priority, unit, source,
			enter_policy, enter_conf, domains, op_id, output_plan
			from mv_charge_core 
			where customer_id = p_customer_id and product_code = p_product_code
				and localtimestamp(3) >= start_time and localtimestamp(3) < finish_time
			order by finish_time;
	ctx enter_context_rt;
begin
	
	if p_customer_id is null or p_seat_id is null or p_product_code is null then
		state_code := 101;
		return;
	end if;
	
	if p_data_count is null or p_data_count <= 0 then
		state_code := 101;
		return;
	end if;
	
	if p_mode <> 1 and p_mode <> 2 then
		state_code := 207;
		return;
	end if;
	
	select localtimestamp(3) into c_request_time from dual;
	
	state_code := 201;
	
	open c;
	begin
		loop
			fetch c into ctx;
			if c%NOTFOUND then
				rollback; close c; return;
			end if;
			
			-- either authr unavailable or service unavailable
			if ctx.a_state <> 1 or ctx.s_state <> 1 then
				state_code := 209;
				continue;
			end if;
			
			if ctx.op_id is null then
				state_code := 543;
				continue;
			end if;
			
			declare
				c_status pls_integer;
			begin
				select status into c_status from g_calltype_status s 
					where s.authr_id = ctx.a_id and s.calltype = p_call_type;
				if c_status = 1 and ctx.a_type = 7 or c_status = 2 and ctx.a_type = 1 then
					null;
				else
					state_code := 553; continue;
				end if;
			exception
				when no_data_found then state_code := 554; continue;
			end;
			
			select sys_guid() into transaction_id from dual;
		
			if p_mode = 1 then
				start_trans_standard(ctx, c_request_time, transaction_id, 
					p_seat_id, p_data_count, p_call_type, p_call_ip4_addr,
					state_code);
			elsif p_mode = 2 then
				start_trans_delegate(ctx, c_request_time, transaction_id, 
					p_seat_id, p_data_count, p_call_type, p_call_ip4_addr,
					state_code);
			end if;
			
			if state_code = 0 then
				output_plan := ctx.output_plan;
				data_quanlity := ctx.data_quanlity;
				exit;
			end if;
		end loop;
	exception when others then
		if c%ISOPEN then close c; raise; end if;
	end;
	close c;
end;

procedure calc_amount_exit_all_fix (
	conf in varchar2,
	ret_code_tbl in ret_code_tbl_rt,
	whether_tbl in bool_tbl_rt,
	a_tbl out amount_tbl_rt,
	a_sum out number
) is
	i pls_integer;
	unit_value number(12,2);
begin
	begin
		select cast(conf as number(12,2)) into unit_value from dual;
	exception
		when others then raise corrupted_fixvaluepolicy;
	end;
	a_tbl := amount_tbl_rt();
	a_tbl.extend(ret_code_tbl.count);
	a_sum := 0;
	i := 1;
	while i <= ret_code_tbl.count loop
		if whether_tbl(i) then
			a_tbl(i) := unit_value;
			a_sum := a_sum + unit_value;
		else
			a_tbl(i) := 0;
		end if;
		i := i + 1;
	end loop;
end;

procedure calc_amount_exit_all_map (
	conf in varchar2,
	ret_code_tbl in ret_code_tbl_rt,
	whether_tbl in bool_tbl_rt,
	a_tbl out amount_tbl_rt,
	a_sum out number
) is
	i_lp pls_integer;
	i_rp pls_integer;
	param_name varchar2(8);
	offset pls_integer;
	
	ret_code varchar2(8);
	value_str varchar2(10);
	value_len pls_integer;
	i_value pls_integer;
	i_scln pls_integer;
	unit_value number(12,2);
begin
	i_lp := instr(conf, '(');
	if i_lp = 0 then
		raise corrupted_mapvaluepolicy;
	end if;
	param_name := substr(conf, 1, i_lp - 1);
	i_rp := instr(conf, ')', i_lp + 1);
	if i_rp = 0 then
		raise corrupted_mapvaluepolicy;
	end if;
	
	if param_name = 'RETCODE' then
		a_tbl := amount_tbl_rt();
		a_tbl.extend(ret_code_tbl.count);
		a_sum := 0;
		
		for i in ret_code_tbl.first .. ret_code_tbl.last loop
			if whether_tbl(i) then
				ret_code := ret_code_tbl(i);
				value_str := '['||ret_code||']';
				value_len := length(value_str);
				i_value := instr(conf, value_str, i_rp + 1);
				if i_value = 0 then 
					a_tbl(i) := 0;
				else
					i_scln := instr(conf, ';', i_value + value_len);
					if i_scln = 0 then
						raise corrupted_mapvaluepolicy;
					end if;
					begin
						select cast(substr(conf, i_value + value_len, i_scln - i_value - value_len) as number) into unit_value from dual;
					exception
						when others then raise corrupted_mapvaluepolicy;
					end;
					a_tbl(i) := unit_value;
					a_sum := a_sum + unit_value;
				end if;
			else
				a_tbl(i) := 0;
			end if;
		end loop;
	else
		raise unknown_mapvaluepolicy_param;
	end if;
end;

procedure calc_amount_exit_all (
	policy in varchar2,
	conf in varchar2,
	state_domain_0 in state_domain_0_rt,
	state_domain_1 in state_domain_1_rt,
	state_domain_2 in state_domain_2_rt,
	state_domain_3 in state_domain_3_rt,
	ret_code_tbl in ret_code_tbl_rt,
	whether_tbl in bool_tbl_rt,
	a_tbl out amount_tbl_rt,
	a_sum out number
) is
begin
	if policy = 'FIX' then
		calc_amount_exit_all_fix(conf, ret_code_tbl, whether_tbl, a_tbl, a_sum);
	elsif policy = 'LDR_DC' then
		declare
			param_name varchar2(8);
			base pls_integer;
			offset pls_integer;
			a_i pls_integer;
		begin
			param_name := parse_ldr_param(conf);
			offset := length(param_name) + 2;
			a_i := 1;
			a_sum := 0;
			if param_name = 'CDC' then
				base := state_domain_0.charged;
				calc_amount_all_ldr_dc(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);
			elsif param_name = 'CDC2' then
				base := state_domain_1.charged;
				calc_amount_all_ldr_dc(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);
			else
				raise unknown_ldrvaluepolicy_param;
			end if;
		end;
	elsif policy = 'LDR_AS' then
		declare
			param_name varchar2(8);
			base pls_integer;
			offset pls_integer;
			a_i pls_integer;
		begin
			param_name := parse_ldr_param(conf);
			offset := length(param_name) + 2;
			a_i := 1;
			a_sum := 0;
			if param_name = 'CAS' then
				base := state_domain_2.charged;
				calc_amount_all_ldr_as(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);
			elsif param_name = 'CAS2' then
				base := state_domain_3.charged;
				calc_amount_all_ldr_as(conf, ret_code_tbl.count, base, offset, whether_tbl, a_i, a_tbl, a_sum);
			else
				raise unknown_ldrvaluepolicy_param;
			end if;
		end;
	elsif policy = 'MAPPING' then
		calc_amount_exit_all_map(conf, ret_code_tbl, whether_tbl, a_tbl, a_sum);
	else
		raise unknown_valuepolicy;
	end if;
end;

function calc_amount_one_ldr_dc (
	conf in varchar2,
	base in pls_integer,
	offset in out pls_integer
) return number
is
	i_cln pls_integer;
	i_scln pls_integer;
	step_to pls_integer;
	unit_value number;
begin
	loop
		i_cln := instr(conf, ':', offset);
		begin
			select cast(substr(conf, offset, i_cln - offset) as number) into step_to from dual;
		exception
			when others then raise corrupted_ldrvaluepolicy;
		end;
		if step_to = -1 then
			begin
				select cast(substr(conf, i_cln + 1) as number) into unit_value from dual;
			exception
				when others then raise corrupted_ldrvaluepolicy;
			end;
			return unit_value;
		else
			i_scln := instr(conf, ';', i_cln + 1);
			begin
				select cast(substr(conf, i_cln + 1, i_scln - i_cln -1) as number) into unit_value from dual;
			exception
				when others then raise corrupted_ldrvaluepolicy;
			end;
		end if;
		
		if base + 1 < step_to then
			return unit_value;
		end if;
		offset := i_scln + 1;
	end loop;
end;

procedure unlock_balance_local (
	p_service_id in raw,
	p_balance_unit in varchar2,
	unlock in number,
	charge in number
) is
begin
	update g_charge_service_balance t
		set locked_amount = locked_amount - unlock, consumed_amount = consumed_amount + charge
		where t.service_id = p_service_id and t.balance_unit = p_balance_unit;
	if SQL%ROWCOUNT <> 1 then
		raise missing_local_balance;
	end if;
end;

procedure unlock_balance_global (
	p_customer_id in raw,
	p_balance_unit in varchar2,
	unlock in number,
	charge in number
) is
begin
	update g_customer_balance t
		set locked_amount = locked_amount - unlock, available_amount = available_amount + unlock - charge
		where t.customer_id = p_customer_id and t.balance_unit = p_balance_unit;
	if SQL%ROWCOUNT <> 1 then
		raise missing_global_balance;
	end if;
end;

procedure unlock_balance (
	p_service_id in raw,
	p_balance_source in varchar2,
	p_balance_unit in varchar2,
	unlock in number,
	charge in number
) is
begin
	if p_balance_source = 'GLOBAL' then
		update g_customer_balance t
			set locked_amount = locked_amount - unlock,
				available_amount = available_amount + unlock - charge
			where
				t.customer_id = (select customer_id from g_charge_service t where t.recid = p_service_id)
				and t.balance_unit = p_balance_unit;
		if SQL%ROWCOUNT <> 1 then
			raise corrupted_global_balance;
		end if;
	elsif p_balance_source = 'LOCAL' then
		update g_charge_service_balance t
			set locked_amount = locked_amount - unlock,
				consumed_amount = consumed_amount + charge
			where
				t.service_id = p_service_id and t.balance_unit = p_balance_unit;
		if SQL%ROWCOUNT <> 1 then
			raise corrupted_local_balance;
		end if;
	else
		raise unknown_balance_source;
	end if;
end;

procedure try_consume_balance_local (
	p_service_id in raw,
	p_balance_unit in varchar2,
	amount in number,
	okay out boolean
) is
begin
	update g_charge_service_balance t
		set consumed_amount = consumed_amount + amount
		where t.service_id = p_service_id and t.balance_unit = p_balance_unit
			and t.limit_amount - t.locked_amount - t.consumed_amount - amount >= 0;
		if SQL%ROWCOUNT = 0 then
			okay := FALSE;
		elsif SQL%ROWCOUNT = 1 then
			okay := TRUE;
		else
			raise corrupted_local_balance;
		end if;
end;

procedure evaluate_exit_rule_poany (
	ctx in exit_context_rt,
	r in exit_rule_context_rt,
	trans in trans_ongoing_rt,
	ret_code_tbl in ret_code_tbl_rt,
	whether_tbl in bool_tbl_rt,
	whether_count in pls_integer,
	eval_okay out boolean,
	using_rule out pls_integer,
	using_unit out varchar2,
	using_source out varchar2,
	a_tbl out amount_tbl_rt,
	a_sum out number
) is
	cursor sd1_c (p_rule_id raw)
		is select recid, charged_data_count, uncharged_data_count
			from g_charge_rule_state_1 where recid = p_rule_id for update;
	cursor sd3_c (p_rule_id raw)
		is select charged_amount_sum
			from g_charge_rule_state_3 where recid = p_rule_id for update;
	state_domain_1 state_domain_1_rt;
	state_domain_3 state_domain_3_rt;
begin
	if r.exit_policy is null then
		raise poany_missing_exit_policy;
	end if;

	if utl_raw.bit_and(r.domains, hextoraw('02000000')) = hextoraw('02000000') then
		open sd1_c (r.r_id);
		fetch sd1_c into state_domain_1;
		if sd1_c%NOTFOUND then
			raise missing_rule_domain_1;
		end if;
	end if;

	if utl_raw.bit_and(r.domains, hextoraw('08000000')) = hextoraw('08000000') then
		open sd3_c (r.r_id);
		fetch sd3_c into state_domain_3;
		if sd3_c%NOTFOUND then
			raise missing_rule_domain_3;
		end if;
	end if;

	calc_amount_exit_all(r.exit_policy, r.exit_conf, null, state_domain_1, null, state_domain_3,
		ret_code_tbl, whether_tbl, a_tbl, a_sum);

	if r.source = 'NONE' then
		eval_okay := TRUE;
	elsif r.source = 'LOCAL' then
		try_consume_balance_local(ctx.s_id, r.unit, a_sum, eval_okay);
	elsif r.source = 'GLOBAL' then
		raise not_yet;
	else
		raise unknown_balance_source;
	end if;

	if eval_okay then
		using_rule := r.priority;
		using_unit := r.unit;
		using_source := r.source;

		if utl_raw.bit_and(r.domains, hextoraw('02000000')) = hextoraw('02000000') then
			update g_charge_rule_state_1
				set charged_data_count = charged_data_count + whether_count,
					uncharged_data_count = uncharged_data_count + trans.data_count - whether_count
				where current of sd1_c;
			close sd1_c;
		end if;

		if utl_raw.bit_and(r.domains, hextoraw('08000000')) = hextoraw('08000000') then
			update g_charge_rule_state_3
				set charged_amount_sum = charged_amount_sum + a_sum
				where current of sd3_c;
			close sd3_c;
		end if;
	end if;

	if sd1_c%ISOPEN then
		close sd1_c;
	end if;
	if sd3_c%ISOPEN then
		close sd3_c;
	end if;
end;

procedure finish_trans_state_5 (
	ctx in exit_context_rt,
	trans in trans_ongoing_rt,
	entries in trans_ongoing_entry_tbl_rt,
	ret_code_tbl in ret_code_tbl_rt,
	whether_tbl in bool_tbl_rt,
	whether_count in pls_integer
) is
begin
	null;
end;

procedure evaluate_whether_charge (
	ctx in exit_context_rt,
	p_ret_code_tbl in ret_code_tbl_rt,
	whether_tbl out bool_tbl_rt,
	whether_count out pls_integer
) is
begin
	whether_tbl := bool_tbl_rt();
	whether_tbl.extend(p_ret_code_tbl.count);
	whether_count := 0;
	for i in p_ret_code_tbl.first .. p_ret_code_tbl.last loop
		if instr(ctx.charge_condition, '"'||p_ret_code_tbl(i)||'"') > 0 then
			whether_tbl(i) := TRUE;
			whether_count := whether_count + 1;
		else
			whether_tbl(i) := FALSE;
		end if;
	end loop;
end;

procedure split_ret_code (
	p_ret_code_concat in varchar2,
	ret_code_tbl out ret_code_tbl_rt
) is
	fr pls_integer;
	ci pls_integer;
	ti pls_integer;
	len constant pls_integer := length(p_ret_code_concat);
begin
	ret_code_tbl := ret_code_tbl_rt();
	fr := 1;
	ti := 1;
	loop
		ci := instr(p_ret_code_concat, ',', fr);
		if ci = 0 then
			ret_code_tbl.extend(1);
			ret_code_tbl(ti) := substr(p_ret_code_concat, fr);
			return;
		else
			ret_code_tbl.extend(1);
			ret_code_tbl(ti) := substr(p_ret_code_concat, fr, ci - fr);
			ti := ti + 1;
			fr := ci + 1;
		end if;
	end loop;
end;

procedure finish_trans (
	p_transaction_id in raw,
	p_ret_code_concat in varchar2
) is
	state_code pls_integer;
begin
	finish_trans_debug(p_transaction_id, p_ret_code_concat, state_code);
end;

procedure finish_trans_debug (
	p_transaction_id in raw,
	p_ret_code_concat in varchar2,
	state_code out pls_integer
) is
	pragma autonomous_transaction;
	ret_code_tbl ret_code_tbl_rt;
	ctx exit_context_rt;
	whether_tbl bool_tbl_rt;
	whether_count pls_integer;
	trans trans_ongoing_rt;
	entries trans_ongoing_entry_tbl_rt;
begin
	if p_transaction_id is null or p_ret_code_concat is null then
		state_code := 101; return;
	end if;

	begin
		split_ret_code(p_ret_code_concat, ret_code_tbl);
	exception
		when others then state_code := 101; return;
	end;

	begin
		select recid, seat_id, service_id, request_time, data_count,
				start_time, trans_state, hit_rule, balance_unit,
				balance_source, enter_amount_sum into trans
			from g_transaction_ongoing where recid = p_transaction_id for update; -- LOCK !
	exception
		when no_data_found then rollback; state_code := 203; return;
	end;

	if trans.data_count <> ret_code_tbl.count then
		rollback; state_code := 204; return;
	end if;

	if trans.trans_state < 1 or trans.trans_state > 5 then
		rollback; state_code := 205; return;
	end if;

	begin
		-- may not ordered by seq
		select recid, trans_id, seq, enter_amount
			bulk collect into entries
			from g_transaction_ongoing_entry where trans_id = p_transaction_id;
	exception
		when no_data_found then raise unexpected_ongoing_entry;
	end;

	if entries.count <> trans.data_count then
		raise unexpected_ongoing_entry;
	end if;

	begin
		select s_id, customer_id, enable_local_gift, r_id, priority, unit, source, exit_policy, exit_conf, exit_ref, domains, cc_id, charge_condition into ctx
			from mv_charge_core where s_id = trans.service_id;
	exception
		when no_data_found then raise missing_charge_service;
	end;

	evaluate_whether_charge(ctx, ret_code_tbl, whether_tbl, whether_count);

	if trans.trans_state = 1 then
		finish_trans_state_1(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);
		state_code := 0; commit;
	elsif trans.trans_state = 2 then
		finish_trans_state_2(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);
		state_code := 0; commit;
	elsif trans.trans_state = 3 then
		finish_trans_state_3(trans, entries, ret_code_tbl, whether_tbl, whether_count);
		state_code := 0; commit;
	elsif trans.trans_state = 4 then
		finish_trans_state_4(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);
		state_code := 0; commit;
	elsif trans.trans_state = 5 then
		finish_trans_state_5(ctx, trans, entries, ret_code_tbl, whether_tbl, whether_count);
		state_code := 0; commit;
	else
		state_code := 205; rollback; return;
	end if;
exception
	when unknown_ruleplan then rollback; state_code := 532;
	when unknown_valuepolicy then rollback; state_code := 533;
	when corrupted_fixvaluepolicy then rollback; state_code := 534;
	when corrupted_ldrvaluepolicy then rollback; state_code := 535;
	when unknown_balance_source then rollback; state_code := 536;
	when missing_rule_domain_0 then rollback; state_code := 539;
	when unknown_ldrvaluepolicy_param then rollback; state_code := 541;
	when prany_missing_hit_rule then rollback; state_code := 542;
	when missing_charge_condition then rollback; state_code := 543;
	when unexpected_ongoing_entry then rollback; state_code := 544;
	when poany_evaluate_failed then rollback; state_code := 545;
	when missing_charge_service then rollback; state_code := 546;
	when missing_rule_domain_1 then rollback; state_code := 547;
	when poany_missing_exit_policy then rollback; state_code := 548;
	when missing_rule_domain_2 then rollback; state_code := 549;
	when missing_rule_domain_3 then rollback; state_code := 550;
	when missing_global_balance then rollback; state_code := 551;
	when missing_local_balance then rollback; state_code := 552;
	when unknown_mapvaluepolicy_param then rollback; state_code := 553;
	when corrupted_mapvaluepolicy then rollback; state_code := 554;
	when rc_missing_exit_ref then rollback; state_code := 555;
	when rc_duplicate_exit_ref then rollback; state_code := 556;
	when not_yet then rollback; state_code := 1542;
	when others then rollback; state_code := 1543;
		dbms_output.put_line(SQLERRM); raise;
end;

end g_core;
$$