DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_ROAM" IS

  PROCEDURE af_judge(v_date IN DATE) AS
    v_destfilename_out VARCHAR2(40);
    v_destfilename_now VARCHAR2(40);
    v_seq_out          NUMBER;
    v_seq_now          NUMBER;
    v_flag             NUMBER;
  
  BEGIN
  
    SELECT destfilename, flag
      INTO v_destfilename_out, v_flag
      FROM outer_log
     WHERE yearmonth = to_char(v_date, 'yyyymmdd');
    IF v_flag = 1
    THEN
	dbms_output.put_line('1');
      RETURN;
    ELSE
      v_seq_out := to_number(substr(v_destfilename_out, 7, 2));
      SELECT filename
        INTO v_destfilename_now
        FROM outertranslog
       WHERE yearmonth = to_char(to_date('20200924','yyyymmdd'), 'yyyymmdd')
         AND fileid = 8
         AND currentbit = 1;
      v_seq_now := to_number(substr(v_destfilename_now, 7, 2));
      IF v_seq_out = v_seq_now + 1
      THEN
        UPDATE outer_log
           SET flag = 1
         WHERE yearmonth = to_char(v_date, 'yyyymmdd');
        INSERT INTO gsm_roam_recu
          SELECT * FROM gsm_roam;
        COMMIT;
		dbms_output.put_line('2');
        RETURN;
      ELSIF v_seq_out > v_seq_now + 1
      THEN
	  dbms_output.put_line('3');
        RETURN;
      ELSE
        --表示有补传的现象产生
        --调用函数产生话单
        ap_gencdr(v_seq_now + 1);
        UPDATE outer_log
           SET flag = 1
         WHERE yearmonth = to_char(v_date, 'yyyymmdd');
        INSERT INTO gsm_roam_recu
          SELECT * FROM gsm_roam;
		  dbms_output.put_line('4');
        COMMIT;
        RETURN;
      
      END IF;
    END IF;
  END;
  PROCEDURE ap_gencdr(s_seq VARCHAR2 DEFAULT NULL) AS
    v_starttime DATE;
    v_day       VARCHAR2(8);
    v_seq       NUMBER;
    v_filename  VARCHAR2(40);
    l_sql       VARCHAR2(204);
    v_dur       NUMBER;
  BEGIN
    SELECT to_char(to_date('20200924','yyyymmdd'), 'yyyymmdd') INTO v_day FROM dual;
    l_sql := 'truncate table gsm_roam';
    DELETE FROM outer_log
     WHERE to_char(starttime, 'yyyymmdd') = to_char(to_date('20200924','yyyymmdd'), 'yyyymmdd');
    EXECUTE IMMEDIATE l_sql;
    IF s_seq IS NULL
	
    THEN
    
      SELECT ceil(dbms_random.VALUE(1, 48)) INTO v_seq FROM dual;
      --SELECT MOD(v_day, 48) + 1 INTO v_seq FROM dual;
	  dbms_output.put_line('5');
    ELSE
      v_seq := s_seq;
	  dbms_output.put_line('6');
    END IF;
  
    v_dur := ceil(dbms_random.VALUE(1, 300));
  
    SELECT to_date(v_day, 'yyyymmdd') + v_seq * 15 / 60 / 24 -
           v_dur * 1 / 60 / 60 / 24
      INTO v_starttime
      FROM dual;
  
    v_filename := REPLACE('D' || to_char(to_date('20200924','yyyymmdd'), 'mmdd') ||
                          to_char(v_seq, '000') || '.531',
                          ' ');
    INSERT INTO gsm_roam
      (cdrtype,
       roamtype,
       calltype,
       telnum,
       vregion,
       hregion,
       hmanage,
       othertelnum,
       othervregion,
       otherhregion,
       othermanage,
       othernettype,
       servermanage,
       imsi,
       msrn,
       intrunkgroup,
       outtrunkgroup,
       mscid,
       lac,
       cellid,
       starttime,
       duration,
       partialflag,
       servicetype,
       servicecode,
       rfpowercapability,
       otherlac,
       othercellid,
       localfee,
       roamfee,
       tollfee,
       ruraladdfee,
       tolladdfee,
       tolldiscount,
       billingcycle,
       sourfilename,
       subscriberid,
       accountid,
       destfilename,
       thirdtelnum,
       tariffflag,
       errorcode,
       specialtype,
       servicebasic,
       devicetype,
       trunkmanage,
       tollfee2,
       localfee_s,
       roamfee_s,
       tollfee_s,
       ruraladdfee_s,
       tolladdfee_s,
       tollfee2_s,
       processtime,
       total_free,
       imei,
       region,
       usertype,
       discount_info,
       ofee1,
       rollbackflag,
       oothertelnum,
       rothertelnum,
       res1)
    VALUES
      ('T',
       '21',
       '11',
       '13000000000',
       '531',
       '22',
       '1.22',
       '13000000000',
       '531',
       '531',
       '1.531',
       'G',
       'OTHER',
       '400000000000000',
       '8613741540518',
       'BSC7I',
       'BSC7O',
       '8613741540',
       '5311',
       '1E01',
       v_starttime,
       v_dur + 34,
       '0',
       '00',
       '00',
       '',
       'BADB4B',
       '',
       0.000,
       1.800,
       0.000,
       0.000,
       0.000,
       531.000,
       substr(to_char(v_starttime, 'yyyymm'), 1, 6),
       'JN09.20080602.TTFILE1090',
       '',
       '',
       NULL,
       '',
       '000000B00000000',
       '',
       '00',
       '00',
       2,
       '',
       0.000,
       0.000,
       1.800,
       0.000,
       0.000,
       0.000,
       0.000,
       to_date('02-06-2008 13:30:17', 'dd-mm-yyyy hh24:mi:ss'),
       'gotone',
       '111111111111111',
       '',
       '',
       '',
       0.00,
       '',
       '13000000000',
       '13000000000',
       '5');
  
    INSERT INTO outer_log
      (yearmonth, telnum, starttime, duration, destfilename, flag)
    VALUES
      (to_char(to_date('20200924','yyyymmdd'), 'yyyymmdd'),
       '13000000000',
       v_starttime,
       23,
       v_filename,
       0);
    COMMIT;
  END;
END pkg_roam;
$$