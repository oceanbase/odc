---
--- v3.2.3 patch1
---

-- add system configuration and version configuration for adapt alibaba JUSHITA
INSERT INTO config_system_configuration(`key`, `value`, `description`)
 VALUES('odc.cloud.export.disable.caller-bid-list','1869954032956259', '导出功能禁用的 CallerBid 列表，值为逗号分隔的列表，如 101,102') ON DUPLICATE KEY UPDATE `id`=`id`;
