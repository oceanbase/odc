create table TRIGGER_TABLE (col varchar2(20));
/

create or replace trigger TRIGGER_TEST before insert on TRIGGER_TABLE for each row begin select 1+1 from dual;end;
/

create or replace trigger INVALID_TRIGGER before insert on TRIGGER_TABLE for each row begin select 1+1 from invalid_dual;end;
