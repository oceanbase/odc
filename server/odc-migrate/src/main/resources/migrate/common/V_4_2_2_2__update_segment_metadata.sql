--
-- Update sensitive algorithm segment metadata for license plate number masking algorithm
--
UPDATE
  `data_security_masking_algorithm_segment` AS `ds_mas`
SET
  `ds_mas`.`is_mask` = false
WHERE
  `ds_mas`.masking_algorithm_id IN (
    SELECT
      `ds_ma`.`id`
    FROM
      `data_security_masking_algorithm` AS `ds_ma`
    WHERE
      `ds_ma`.`name` = '${com.oceanbase.odc.builtin-resource.masking-algorithm.license-plate-number.name}'
  )
  AND `ds_mas`.ordinal = 2;
