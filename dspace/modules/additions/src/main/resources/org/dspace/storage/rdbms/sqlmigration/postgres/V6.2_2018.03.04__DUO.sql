-- add the extra configuration to the harvested collection table
--
alter table harvested_collection add column if not exists metadata_authority_type varchar;
alter table harvested_collection add column if not exists bundle_versioning_strategy varchar;
alter table harvested_collection add column if not exists workflow_process varchar;
alter table harvested_collection add column if not exists ingest_filter varchar;