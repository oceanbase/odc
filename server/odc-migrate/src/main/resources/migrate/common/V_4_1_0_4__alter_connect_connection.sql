-- ssl config
alter table `connect_connection` add column is_ssl_enabled tinyint not null default 0 comment 'Flag bit used to indicate if ssl
connection enabled';

alter table `connect_connection` add column ssl_client_cert_object_id VARCHAR(64) default null comment 'ssl client certificate object id,
 references objectstorage_object_metadata.object_id';

alter table `connect_connection` add column ssl_client_key_object_id VARCHAR(64) default null comment 'ssl client private key object id,
references objectstorage_object_metadata.object_id';

alter table `connect_connection` add column ssl_ca_cert_object_id VARCHAR(64) default null comment 'ssl CA certificate object id,
references objectstorage_object_metadata.object_id';