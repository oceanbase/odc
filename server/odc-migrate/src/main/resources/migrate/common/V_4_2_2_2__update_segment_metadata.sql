--
-- Update sensitive algorithm segment metadata for license plate number masking algorithm
--
UPDATE
  `data_security_masking_algorithm_segment`
SET
  `is_mask` = false
WHERE
  `masking_algorithm_id` IN (
    SELECT
      `id`
    FROM
      `data_security_masking_algorithm`
    WHERE
      `name` = '${com.oceanbase.odc.builtin-resource.masking-algorithm.license-plate-number.name}'
  )
  AND `ordinal` = 2;
