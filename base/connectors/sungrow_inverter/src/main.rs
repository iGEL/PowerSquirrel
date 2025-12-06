use anyhow::{ensure, Context, Result};
use chrono::{SecondsFormat, Utc};
use clap::Parser;
use modbus::{tcp, Client};
use rumqttc::{Client as MqttClient, MqttOptions, QoS};
use serde::{Deserialize, Serialize};
use std::{
    fs,
    path::{Path, PathBuf},
    process, thread,
    time::Duration,
};

const BATTERY_IDLE_THRESHOLD_W: i64 = 30;

/// Collects a Sungrow inverter snapshot and prints the JSON payload that would be published.
#[derive(Parser, Debug)]
#[command(author, version, about)]
struct Cli {
    /// Path to the connector config JSON file
    #[arg(short, long, default_value = "config.json")]
    config: PathBuf,

    /// Print the payload once and exit instead of publishing to MQTT
    #[arg(long)]
    print: bool,
}

#[derive(Deserialize)]
struct Config {
    inverter: InverterConfig,
    mqtt: MqttConfig,
}

#[derive(Deserialize)]
struct InverterConfig {
    host: String,
    port: u16,
    unit: u8,
}

#[derive(Deserialize)]
struct MqttConfig {
    host: String,
    port: u16,
    #[serde(default)]
    user: Option<String>,
    #[serde(default)]
    password: Option<String>,
    interval_s: u64,
}

#[derive(Serialize)]
struct SamplePayload {
    ts: String,
    sn: String,
    pv_w: i64,
    pv_today_wh: u64,
    pv_to_load_today_wh: u64,
    pv_to_battery_today_wh: u64,
    pv_to_grid_today_wh: u64,
    loads_w: i64,
    grid_w: i64,
    grid_export_today_wh: u64,
    grid_import_today_wh: u64,
    battery_w: i64,
    battery_pct: f64,
    battery_charged_today_wh: u64,
    battery_discharged_today_wh: u64,
    battery_status: BatteryStatus,
    battery_charge_discharge_w: i64,
}

#[derive(Serialize)]
#[serde(rename_all = "lowercase")]
enum BatteryStatus {
    Charging,
    Discharging,
    Hold,
}

impl BatteryStatus {
    fn from_power(power_w: i64) -> Self {
        if power_w.abs() < BATTERY_IDLE_THRESHOLD_W {
            BatteryStatus::Hold
        } else if power_w > 0 {
            BatteryStatus::Charging
        } else {
            BatteryStatus::Discharging
        }
    }
}

fn main() -> Result<()> {
    let cli = Cli::parse();
    let config = load_config(&cli.config)?;

    if cli.print {
        run_print_mode(&config)?;
        return Ok(());
    }

    run_publish_mode(&config)?;

    Ok(())
}

fn load_config(path: &Path) -> Result<Config> {
    let contents = fs::read_to_string(path)
        .with_context(|| format!("failed to read config {}", path.display()))?;
    let cfg = serde_json::from_str(&contents)
        .with_context(|| format!("failed to parse config {}", path.display()))?;
    Ok(cfg)
}

fn run_print_mode(config: &Config) -> Result<()> {
    let mut ctx = connect_inverter(&config.inverter)?;
    let payload = collect_payload(&mut ctx)?;
    print_payload(&payload)
}

fn run_publish_mode(config: &Config) -> Result<()> {
    ensure!(
        config.mqtt.interval_s > 0,
        "mqtt.interval_s must be greater than zero"
    );

    let mut ctx = connect_inverter(&config.inverter)?;
    let mut mqtt_client = connect_mqtt(&config.mqtt)?;
    let interval = Duration::from_secs(config.mqtt.interval_s);

    loop {
        match collect_payload(&mut ctx) {
            Ok(payload) => {
                if let Err(err) = publish_payload(&mut mqtt_client, &payload) {
                    eprintln!("failed to publish MQTT payload: {err:#}");
                    mqtt_client = connect_mqtt(&config.mqtt)?;
                }
            }
            Err(err) => {
                eprintln!("failed to collect payload from inverter: {err:#}");
                ctx = connect_inverter(&config.inverter)?;
            }
        }

        thread::sleep(interval);
    }
}

fn print_payload(payload: &SamplePayload) -> Result<()> {
    serde_json::to_writer_pretty(std::io::stdout(), payload)?;
    println!();
    Ok(())
}

fn connect_inverter(cfg: &InverterConfig) -> Result<tcp::Transport> {
    let mut transport_cfg = tcp::Config::default();
    transport_cfg.tcp_port = cfg.port;
    transport_cfg.modbus_uid = cfg.unit;
    transport_cfg.tcp_connect_timeout = Some(Duration::from_secs(3));
    transport_cfg.tcp_read_timeout = Some(Duration::from_secs(3));
    transport_cfg.tcp_write_timeout = Some(Duration::from_secs(3));

    tcp::Transport::new_with_cfg(&cfg.host, transport_cfg)
        .with_context(|| format!("failed to connect to {}:{}", cfg.host, cfg.port))
}

fn connect_mqtt(cfg: &MqttConfig) -> Result<MqttClient> {
    let mut options = MqttOptions::new(
        format!("sungrow-inverter-{}", process::id()),
        cfg.host.clone(),
        cfg.port,
    );
    options.set_keep_alive(Duration::from_secs(30));
    if cfg.user.is_some() || cfg.password.is_some() {
        options.set_credentials(
            cfg.user.as_deref().unwrap_or_default(),
            cfg.password.as_deref().unwrap_or_default(),
        );
    }

    let (client, mut connection) = MqttClient::new(options, 10);
    thread::spawn(move || {
        for notification in connection.iter() {
            if let Err(err) = notification {
                eprintln!("MQTT connection error: {err}");
            }
        }
    });

    Ok(client)
}

fn collect_payload(ctx: &mut tcp::Transport) -> Result<SamplePayload> {
    let sn = read_serial_number(ctx)?;
    let pv_w = read_value(ctx, TOTAL_DC_POWER, "total DC power")?;
    let pv_today_wh =
        wh_from_tenths_kwh(read_value(ctx, DAILY_PV_GENERATION, "daily PV generation")?);
    let pv_to_load_today_wh = wh_from_tenths_kwh(read_value(
        ctx,
        DAILY_DIRECT_ENERGY_CONSUMPTION,
        "daily direct energy consumption",
    )?);
    let pv_to_battery_today_wh = wh_from_tenths_kwh(read_value(
        ctx,
        DAILY_BATTERY_CHARGE_FROM_PV,
        "daily battery charge from PV",
    )?);
    let pv_to_grid_today_wh = wh_from_tenths_kwh(read_value(
        ctx,
        DAILY_EXPORTED_ENERGY_FROM_PV,
        "daily exported energy from PV",
    )?);
    let loads_w = read_value(ctx, LOAD_POWER, "load power")?;
    let export_power_raw = read_value(ctx, EXPORT_POWER_RAW, "export power raw")?;
    let grid_w = -export_power_raw;
    let grid_export_today_wh = wh_from_tenths_kwh(read_value(
        ctx,
        DAILY_EXPORTED_ENERGY,
        "daily exported energy",
    )?);
    let grid_import_today_wh = wh_from_tenths_kwh(read_value(
        ctx,
        DAILY_IMPORTED_ENERGY,
        "daily imported energy",
    )?);
    let battery_w = read_value(ctx, BATTERY_POWER_RAW, "battery power raw")?;
    let battery_pct = percent_from_tenths(read_value(ctx, BATTERY_LEVEL, "battery level")?);
    let battery_charged_today_wh = wh_from_tenths_kwh(read_value(
        ctx,
        DAILY_BATTERY_CHARGE,
        "daily battery charge",
    )?);
    let battery_discharged_today_wh = wh_from_tenths_kwh(read_value(
        ctx,
        DAILY_BATTERY_DISCHARGE,
        "daily battery discharge",
    )?);
    let battery_charge_discharge_w = read_value(
        ctx,
        BATTERY_CHARGE_DISCHARGE_POWER_LIMIT,
        "battery charge/discharge power limit",
    )?;

    let battery_status = BatteryStatus::from_power(battery_w);

    Ok(SamplePayload {
        ts: Utc::now().to_rfc3339_opts(SecondsFormat::Secs, true),
        sn,
        pv_w,
        pv_today_wh,
        pv_to_load_today_wh,
        pv_to_battery_today_wh,
        pv_to_grid_today_wh,
        loads_w,
        grid_w,
        grid_export_today_wh,
        grid_import_today_wh,
        battery_w,
        battery_pct,
        battery_charged_today_wh,
        battery_discharged_today_wh,
        battery_status,
        battery_charge_discharge_w,
    })
}

fn publish_payload(client: &mut MqttClient, payload: &SamplePayload) -> Result<()> {
    let topic = format!("posq/inverter/sungrow/{}", payload.sn);
    let body = serde_json::to_vec(payload)?;
    client
        .publish(topic, QoS::AtLeastOnce, false, body)
        .context("MQTT publish failed")?;
    Ok(())
}

fn wh_from_tenths_kwh(raw_value: i64) -> u64 {
    if raw_value <= 0 {
        0
    } else {
        (raw_value as u64) * 100
    }
}

fn percent_from_tenths(raw_value: i64) -> f64 {
    (raw_value as f64) / 10.0
}

fn read_value(ctx: &mut tcp::Transport, register: Register, label: &str) -> Result<i64> {
    read_register(ctx, register).with_context(|| format!("failed to read {}", label))
}

fn read_register(ctx: &mut tcp::Transport, register: Register) -> Result<i64> {
    let quantity = register.kind.word_count();
    let words = match register.space {
        RegisterSpace::Input => ctx.read_input_registers(register.address, quantity)?,
        RegisterSpace::Holding => ctx.read_holding_registers(register.address, quantity)?,
    };

    ensure!(
        words.len() == quantity as usize,
        "expected {} registers, device returned {}",
        quantity,
        words.len()
    );

    register.kind.decode(&words, register.swap_words)
}

fn read_serial_number(ctx: &mut tcp::Transport) -> Result<String> {
    let words = ctx
        .read_input_registers(SERIAL_NUMBER_ADDRESS, SERIAL_NUMBER_WORDS)
        .context("failed to read serial number")?;
    ensure!(
        words.len() == SERIAL_NUMBER_WORDS as usize,
        "expected {} registers for serial number, device returned {}",
        SERIAL_NUMBER_WORDS,
        words.len()
    );

    let mut bytes = Vec::with_capacity(words.len() * 2);
    for word in words {
        bytes.extend_from_slice(&word.to_be_bytes());
    }

    while bytes.last() == Some(&0) {
        bytes.pop();
    }

    String::from_utf8(bytes).context("serial number is not valid UTF-8")
}

#[derive(Clone, Copy)]
struct Register {
    address: u16,
    kind: RegisterKind,
    space: RegisterSpace,
    swap_words: bool,
}

impl Register {
    const fn input(address: u16, kind: RegisterKind, swap_words: bool) -> Self {
        Self {
            address,
            kind,
            space: RegisterSpace::Input,
            swap_words,
        }
    }

    const fn holding(address: u16, kind: RegisterKind, swap_words: bool) -> Self {
        Self {
            address,
            kind,
            space: RegisterSpace::Holding,
            swap_words,
        }
    }
}

#[derive(Clone, Copy)]
enum RegisterSpace {
    Input,
    Holding,
}

#[derive(Clone, Copy)]
enum RegisterKind {
    U16,
    I16,
    I32,
    U32,
}

impl RegisterKind {
    fn word_count(self) -> u16 {
        match self {
            RegisterKind::U16 | RegisterKind::I16 => 1,
            RegisterKind::I32 | RegisterKind::U32 => 2,
        }
    }

    fn decode(self, words: &[u16], swap_words: bool) -> Result<i64> {
        match self {
            RegisterKind::U16 => {
                ensure!(words.len() == 1, "expected 1 register, got {}", words.len());
                Ok(words[0] as i64)
            }
            RegisterKind::I16 => {
                ensure!(words.len() == 1, "expected 1 register, got {}", words.len());
                Ok(i16::from_be_bytes(words[0].to_be_bytes()) as i64)
            }
            RegisterKind::I32 => {
                let raw = words_to_u32(words, swap_words)?;
                Ok(i32::from_be_bytes(raw.to_be_bytes()) as i64)
            }
            RegisterKind::U32 => {
                let raw = words_to_u32(words, swap_words)?;
                Ok(raw as i64)
            }
        }
    }
}

fn words_to_u32(words: &[u16], swap_words: bool) -> Result<u32> {
    ensure!(
        words.len() == 2,
        "expected two registers, got {}",
        words.len()
    );
    let (hi, lo) = if swap_words {
        (words[1], words[0])
    } else {
        (words[0], words[1])
    };
    Ok(((hi as u32) << 16) | lo as u32)
}

const TOTAL_DC_POWER: Register = Register::input(5_016, RegisterKind::U32, true);
const SERIAL_NUMBER_ADDRESS: u16 = 4_989;
const SERIAL_NUMBER_WORDS: u16 = 10;
const DAILY_PV_GENERATION: Register = Register::input(13_001, RegisterKind::U16, false);
const DAILY_DIRECT_ENERGY_CONSUMPTION: Register = Register::input(13_016, RegisterKind::U16, false);
const DAILY_BATTERY_CHARGE_FROM_PV: Register = Register::input(13_011, RegisterKind::U16, false);
const DAILY_EXPORTED_ENERGY_FROM_PV: Register = Register::input(13_004, RegisterKind::U16, false);
const LOAD_POWER: Register = Register::input(13_007, RegisterKind::I32, true);
const EXPORT_POWER_RAW: Register = Register::input(13_009, RegisterKind::I32, true);
const DAILY_EXPORTED_ENERGY: Register = Register::input(13_044, RegisterKind::U16, false);
const DAILY_IMPORTED_ENERGY: Register = Register::input(13_035, RegisterKind::U16, false);
const BATTERY_POWER_RAW: Register = Register::input(13_021, RegisterKind::I16, false);
const BATTERY_LEVEL: Register = Register::input(13_022, RegisterKind::U16, false);
const DAILY_BATTERY_CHARGE: Register = Register::input(13_039, RegisterKind::U16, false);
const DAILY_BATTERY_DISCHARGE: Register = Register::input(13_025, RegisterKind::U16, false);
const BATTERY_CHARGE_DISCHARGE_POWER_LIMIT: Register =
    Register::holding(13_051, RegisterKind::U16, false);
