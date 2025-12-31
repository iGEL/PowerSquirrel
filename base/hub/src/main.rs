mod aggregation;
use aggregation::aggregate_inverters;
use anyhow::{Context, Result};
use std::{fs, path::Path};
use serde::Deserialize;

#[allow(clippy::print_stdout)]
#[tokio::main]
async fn main() -> Result<()> {
    println!("PowerSquirrel base hub {} 🐿️", env!("CARGO_PKG_VERSION"));

    let config = load_config(Path::new("config.json"))?;

    aggregate_inverters(&config.mqtt).await
}

#[derive(Deserialize, Debug)]
struct Config {
    couchdb: CouchDBConfig,
    mqtt: MqttConfig,
}

#[derive(Deserialize, Debug)]
struct CouchDBConfig {
    base_uri: String,
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
