-- add the extra configuration to the harvested collection table
--
alter table harvested_collection add column metadata_authority_type varchar;
alter table harvested_collection add column bundle_versioning_strategy varchar;
alter table harvested_collection add column workflow_process varchar;
alter table harvested_collection add column ingest_filter varchar;