mod aggregation;
mod db;
use aggregation::aggregate_inverters;
use anyhow::{Context, Result};
use db::DatabaseConfig;
use serde::Deserialize;
use std::{fs, path::Path};

#[allow(clippy::print_stdout)]
#[tokio::main]
async fn main() -> Result<()> {
    println!("PowerSquirrel base hub {} 🐿️", env!("CARGO_PKG_VERSION"));

    let config = load_config(Path::new("config.json"))?;
    let database = db::init(&config.database).await?;
    let commit = |data: aggregation::InverterAgg15| {
        let database = database.clone();
        async move { db::upsert_inverter_agg15(&database, &data).await }
    };

    aggregate_inverters(&config.mqtt, commit).await
}

#[derive(Deserialize, Debug)]
struct Config {
    database: DatabaseConfig,
    mqtt: MqttConfig,
}

#[derive(Deserialize, Debug)]
pub struct MqttConfig {
    host: String,
    port: u16,
    #[serde(default)]
    user: Option<String>,
    #[serde(default)]
    password: Option<String>,
}

fn load_config(path: &Path) -> Result<Config> {
    let contents = fs::read_to_string(path)
        .with_context(|| format!("failed to read config {}", path.display()))?;
    let cfg = serde_json::from_str(&contents)
        .with_context(|| format!("failed to parse config {}", path.display()))?;
    Ok(cfg)
}
