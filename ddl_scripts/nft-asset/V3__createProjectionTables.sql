-- DROP TABLE IF EXISTS nft_asset.pekko_projection_offset_store

CREATE TABLE IF NOT EXISTS nft_asset.pekko_projection_offset_store(
  projection_name VARCHAR(255) NOT NULL,
  projection_key VARCHAR(255) NOT NULL,
  current_offset VARCHAR(255) NOT NULL,
  manifest VARCHAR(4) NOT NULL,
  mergeable BOOLEAN NOT NULL,
  last_updated BIGINT NOT NULL,

  PRIMARY KEY(projection_name, projection_key)
);

CREATE INDEX IF NOT EXISTS projection_name_index ON nft_asset.pekko_projection_offset_store(projection_name);

CREATE TABLE IF NOT EXISTS nft_asset.pekko_projection_management (
  projection_name VARCHAR(255) NOT NULL,
  projection_key VARCHAR(255) NOT NULL,
  paused BOOLEAN NOT NULL,
  last_updated BIGINT NOT NULL,
  PRIMARY KEY(projection_name, projection_key)
);
