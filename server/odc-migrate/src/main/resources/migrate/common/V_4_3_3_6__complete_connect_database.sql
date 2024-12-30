UPDATE connect_database AS cd
JOIN connect_connection AS cc ON cd.connection_id = cc.id
SET cd.connect_type = cc.type
WHERE cd.connect_type IS NULL AND cd.type = 'PHYSICAL';