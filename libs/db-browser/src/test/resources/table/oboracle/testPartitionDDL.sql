CREATE TABLE "part_hash" (
"C1" INTEGER NOT NULL
)  PARTITION BY HASH("C1")
PARTITIONS 5;
COMMENT ON TABLE "part_hash" IS 'this is a comment';
