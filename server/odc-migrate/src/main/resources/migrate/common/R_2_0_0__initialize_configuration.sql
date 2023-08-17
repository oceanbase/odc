
-- v2.1.0
INSERT INTO `odc_configuration`(`key`, `value`, `cname`, `help`)
 VALUES ('oauth.account.domain', '#鉴权系统域名','', NULL) ON DUPLICATE KEY UPDATE `key`=`key`;

INSERT INTO `odc_configuration`(`key`, `value`, `cname`, `help`)
 VALUES ('apsara.aas.domain', '#oathBaseUrl','', NULL) ON DUPLICATE KEY UPDATE `key`=`key`;

INSERT INTO `odc_configuration`(`key`, `value`, `cname`, `help`)
 VALUES ('apsara.aas.access_id', '#appKey','', NULL) ON DUPLICATE KEY UPDATE `key`=`key`;

INSERT INTO `odc_configuration`(`key`, `value`, `cname`, `help`)
 VALUES ('apsara.aas.access_key', '#appSecret','', NULL) ON DUPLICATE KEY UPDATE `key`=`key`;
