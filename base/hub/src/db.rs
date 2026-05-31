use crate::aggregation::InverterAgg15;
use anyhow::Result;
use serde::Deserialize;
use sqlx::SqlitePool;
use sqlx::sqlite::{SqliteConnectOptions, SqliteJournalMode, SqlitePoolOptions};
use std::str::FromStr;

static MIGRATOR: sqlx::migrate::Migrator = sqlx::migrate!("./migrations");

#[derive(Deserialize, Debug)]
pub struct DatabaseConfig {
    pub path: String,
}

pub async fn init(config: &DatabaseConfig) -> Result<SqlitePool> {
    let options = SqliteConnectOptions::from_str(&format!("sqlite://{}", config.path))?
        .create_if_missing(true)
        .journal_mode(SqliteJournalMode::Wal)
        .foreign_keys(true);

    let pool = SqlitePoolOptions::new()
        .max_connections(4)
        .connect_with(options)
        .await?;

    MIGRATOR.run(&pool).await?;

    Ok(pool)
}

pub async fn upsert_inverter_agg15(pool: &SqlitePool, data: &InverterAgg15) -> Result<()> {
    sqlx::query(
        r#"
        insert into inverter_agg15 (
            inverter_sn,
            started_at_s,
            pv_ws,
            loads_ws,
            load_produced_ws,
            grid_import_ws,
            grid_export_ws,
            battery_charged_ws,
            battery_discharged_ws,
            battery_soc_bp,
            complete,
            updated_at_s
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        on conflict (inverter_sn, started_at_s) do update set
            pv_ws = excluded.pv_ws,
            loads_ws = excluded.loads_ws,
            load_produced_ws = excluded.load_produced_ws,
            grid_import_ws = excluded.grid_import_ws,
            grid_export_ws = excluded.grid_export_ws,
            battery_charged_ws = excluded.battery_charged_ws,
            battery_discharged_ws = excluded.battery_discharged_ws,
            battery_soc_bp = excluded.battery_soc_bp,
            complete = excluded.complete,
            updated_at_s = excluded.updated_at_s
        "#,
    )
    .bind(&data.inverter_sn)
    .bind(data.started_at_s)
    .bind(i64::from(data.pv_ws))
    .bind(i64::from(data.loads_ws))
    .bind(i64::from(data.load_produced_ws))
    .bind(i64::from(data.grid_import_ws))
    .bind(i64::from(data.grid_export_ws))
    .bind(i64::from(data.battery_charged_ws))
    .bind(i64::from(data.battery_discharged_ws))
    .bind(data.battery_soc_bp)
    .bind(data.complete)
    .bind(data.updated_at_s)
    .execute(pool)
    .await?;

    Ok(())
}
