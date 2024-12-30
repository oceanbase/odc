UPDATE connect_database
SET connect_type = (
    SELECT cc.type FROM connect_connection cc
    WHERE connect_database.connection_id = cc.id
  )
WHERE connect_type IS NULL AND type = 'PHYSICAL';