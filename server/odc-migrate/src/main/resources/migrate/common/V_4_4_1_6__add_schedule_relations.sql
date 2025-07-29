-- Create schedule_relations table
CREATE TABLE schedule_relations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL COMMENT 'Parent schedule ID',
    child_id BIGINT NOT NULL COMMENT 'Child schedule ID',
    schedule_type VARCHAR(30) NOT NULL COMMENT 'Schedule type',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Table to store parent-child relationships in the schedule_schedule table';

-- Create index on parent_id
CREATE INDEX idx_schedule_relations_parent_id ON schedule_relations (parent_id);