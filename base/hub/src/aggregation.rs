use crate::MqttConfig;
use anyhow::Result;
use chrono::{DateTime, Timelike, Utc};
use rumqttc::{AsyncClient, Event, Incoming, MqttOptions, QoS};
use serde::Deserialize;
use std::future::Future;
use std::time::Duration;

pub async fn aggregate_inverters<F, Fut>(mqtt_config: &MqttConfig, commit_fn: F) -> Result<()>
where
    F: Fn(InverterAgg15) -> Fut,
    Fut: Future<Output = Result<()>>,
{
    let mut mqttoptions =
        MqttOptions::new("rumqtt-sync", mqtt_config.host.as_str(), mqtt_config.port);
    mqttoptions.set_keep_alive(Duration::from_secs(5));
    if let (Some(user), Some(password)) =
        (mqtt_config.user.as_deref(), mqtt_config.password.as_deref())
    {
        mqttoptions.set_credentials(user, password);
    }

    let (client, mut eventloop) = AsyncClient::new(mqttoptions, 10);
    client.subscribe("posq/#", QoS::AtMostOnce).await?;

    let mut state: Option<InverterData> = None;
    loop {
        let notification = eventloop.poll().await;
        match notification {
            Ok(Event::Incoming(Incoming::Publish(p))) => {
                // println!("{}", String::from_utf8_lossy(&p.payload));
                match serde_json::from_slice::<InverterEvent>(&p.payload) {
                    Ok(ev) => {
                        let json_data = update_inverter_data(&mut state, ev.clone());
                        if let Err(e) = commit_fn(json_data).await {
                            eprintln!("err committing data: {e:?}");
                        }
                    }
                    Err(e) => eprintln!("err parsing event: {e:?}"),
                }
            }
            Ok(_) => {}
            Err(e) => eprintln!("err: {e:?}"),
        }
    }
}

#[derive(Deserialize, Debug, Clone, PartialEq)]
struct InverterEvent {
    ts: DateTime<Utc>,
    sn: String,
    pv_w: i32,
    loads_w: i32,
    grid_w: i32,
    battery_w: i32,
    battery_pct: f64,
}

#[derive(Debug, PartialEq, Clone)]
struct InverterData {
    last_event: InverterEvent,
    data_missing: bool, // true for the first quarter & when the gap between 2 samples exceeds 30s
    pv_ws: u32, // ws = watt seconds
    loads_ws: u32,
    load_produced_ws: u32, // If sources like plug in PV produce more than is consumed
    grid_import_ws: u32,
    grid_export_ws: u32,
    battery_charged_ws: u32,
    battery_discharged_ws: u32,
}

#[derive(Debug, PartialEq)]
pub struct InverterAgg15 {
    pub inverter_sn: String,
    pub started_at_s: i64,
    pub pv_ws: u32,
    pub loads_ws: u32,
    pub load_produced_ws: u32,
    pub grid_import_ws: u32,
    pub grid_export_ws: u32,
    pub battery_charged_ws: u32,
    pub battery_discharged_ws: u32,
    pub battery_soc_bp: i64,
    pub complete: bool,
    pub updated_at_s: i64,
}

// Returns the watt seconds for the given duration and previous and current watt values
fn calculate_ws(prev_w: i32, cur_w: i32, secs: i32) -> i32 {
    secs * (prev_w + cur_w) / 2
}

// Returns the DateTime at the beginning of the 15 minute interval, eg returns 16:45:00 for 16:59:03
fn floor_to_quarter(dt: DateTime<Utc>) -> DateTime<Utc> {
    dt.date_naive()
        .and_hms_opt(dt.hour(), dt.minute() - dt.minute() % 15, 0)
        .unwrap()
        .and_local_timezone(Utc)
        .unwrap()
}

// Updates the given aggregated inverter data with the given event. Every 15 minutes, the data is
// reset. Returns the aggregate row to store in SQLite.
fn update_inverter_data(state: &mut Option<InverterData>, ev: InverterEvent) -> InverterAgg15 {
    const MINUTES_PER_QUARTER: i64 = 15; // To be able to change it for manual testing
    const DIVISOR: i64 = MINUTES_PER_QUARTER * 60;
    let mut is_complete = false;
    let data_to_return = match state {
        None => {
            *state = Some(InverterData {
                data_missing: true,
                pv_ws: 0,
                loads_ws: 0,
                load_produced_ws: 0,
                grid_import_ws: 0,
                grid_export_ws: 0,
                battery_charged_ws: 0,
                battery_discharged_ws: 0,
                last_event: ev.clone(),
            });
            state.clone().unwrap()
        }
        Some(s) => {
            let secs = ev.ts.signed_duration_since(s.last_event.ts).num_seconds() as i32;

            if secs > 30 {
                s.data_missing = true;
            } else if secs > 0 {
                let pv_ws = calculate_ws(s.last_event.pv_w, ev.pv_w, secs);
                let loads_ws = calculate_ws(s.last_event.loads_w, ev.loads_w, secs);
                let grid_ws = calculate_ws(s.last_event.grid_w, ev.grid_w, secs);
                let battery_ws = calculate_ws(s.last_event.battery_w, ev.battery_w, secs);
                if pv_ws > 0 {
                    s.pv_ws += pv_ws as u32;
                }
                if loads_ws > 0 {
                    s.loads_ws += loads_ws as u32;
                } else {
                    s.load_produced_ws += loads_ws.abs() as u32;
                }
                if grid_ws > 0 {
                    s.grid_import_ws += grid_ws as u32;
                } else {
                    s.grid_export_ws += grid_ws.abs() as u32;
                }
                if battery_ws > 0 {
                    s.battery_discharged_ws += battery_ws as u32;
                } else {
                    s.battery_charged_ws += battery_ws.abs() as u32;
                }
            }
            let data_to_return = s.clone();
            // Start of new 15 minutes
            if ev.ts.timestamp() / DIVISOR > s.last_event.ts.timestamp() / DIVISOR {
                is_complete = true;

                s.data_missing = false;
                s.pv_ws = 0;
                s.loads_ws = 0;
                s.load_produced_ws = 0;
                s.grid_import_ws = 0;
                s.grid_export_ws = 0;
                s.battery_charged_ws = 0;
                s.battery_discharged_ws = 0;
            }
            s.last_event = ev.clone();
            data_to_return
        }
    };
    InverterAgg15 {
        inverter_sn: ev.sn,
        started_at_s: floor_to_quarter(data_to_return.last_event.ts).timestamp(),
        pv_ws: data_to_return.pv_ws,
        loads_ws: data_to_return.loads_ws,
        load_produced_ws: data_to_return.load_produced_ws,
        grid_import_ws: data_to_return.grid_import_ws,
        grid_export_ws: data_to_return.grid_export_ws,
        battery_charged_ws: data_to_return.battery_charged_ws,
        battery_discharged_ws: data_to_return.battery_discharged_ws,
        battery_soc_bp: (ev.battery_pct * 100.0).round() as i64,
        complete: (is_complete && !data_to_return.data_missing),
        updated_at_s: ev.ts.timestamp(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::{TimeZone, Utc};

    #[test]
    fn update_inverter_data_test() {
        let mut state: Option<InverterData> = None;
        let e1 = InverterEvent {
            ts: Utc.with_ymd_and_hms(2025, 12, 13, 14, 59, 25).unwrap(),
            sn: "1234".to_string(),
            pv_w: 200,
            loads_w: 400,
            grid_w: 100,
            battery_w: 100,
            battery_pct: 20.1,
        };

        let json_data = update_inverter_data(&mut state, e1.clone());
        assert_eq!(
            json_data,
            InverterAgg15 {
                inverter_sn: "1234".to_string(),
                started_at_s: Utc
                    .with_ymd_and_hms(2025, 12, 13, 14, 45, 0)
                    .unwrap()
                    .timestamp(),
                pv_ws: 0,
                loads_ws: 0,
                load_produced_ws: 0,
                grid_import_ws: 0,
                grid_export_ws: 0,
                battery_charged_ws: 0,
                battery_discharged_ws: 0,
                battery_soc_bp: 2010,
                complete: false,
                updated_at_s: e1.ts.timestamp(),
            }
        );

        let expected_inverter_data = Some(InverterData {
            data_missing: true,
            last_event: e1,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 0,
            grid_import_ws: 0,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 0,
        });
        assert_eq!(state, expected_inverter_data);

        // Expect the current event updated within the same quarter hour
        let e2 = InverterEvent {
            ts: Utc.with_ymd_and_hms(2025, 12, 13, 14, 59, 45).unwrap(),
            sn: "1234".to_string(),
            pv_w: 200,
            loads_w: 500,
            grid_w: 200,
            battery_w: 100,
            battery_pct: 20.1,
        };

        let json_data = update_inverter_data(&mut state, e2.clone());
        assert_eq!(
            json_data,
            InverterAgg15 {
                inverter_sn: "1234".to_string(),
                started_at_s: Utc
                    .with_ymd_and_hms(2025, 12, 13, 14, 45, 0)
                    .unwrap()
                    .timestamp(),
                pv_ws: 4000,
                loads_ws: 9000,
                load_produced_ws: 0,
                grid_import_ws: 3000,
                grid_export_ws: 0,
                battery_charged_ws: 0,
                battery_discharged_ws: 2000,
                battery_soc_bp: 2010,
                complete: false,
                updated_at_s: e2.ts.timestamp(),
            }
        );

        let expected_inverter_data = Some(InverterData {
            last_event: e2,
            data_missing: true,
            pv_ws: 4000,
            loads_ws: 9000,
            load_produced_ws: 0,
            grid_import_ws: 3000,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 2000,
        });
        assert_eq!(state, expected_inverter_data);

        // New quarter hour: Expect updated json, but reset state
        let e3 = InverterEvent {
            ts: Utc.with_ymd_and_hms(2025, 12, 13, 15, 0, 5).unwrap(),
            sn: "1234".to_string(),
            pv_w: 0,
            loads_w: -600,
            grid_w: -500,
            battery_w: -100,
            battery_pct: 20.1,
        };

        let json_data = update_inverter_data(&mut state, e3.clone());
        assert_eq!(
            json_data,
            InverterAgg15 {
                inverter_sn: "1234".to_string(),
                started_at_s: Utc
                    .with_ymd_and_hms(2025, 12, 13, 14, 45, 0)
                    .unwrap()
                    .timestamp(),
                pv_ws: 6000,
                loads_ws: 9000,
                load_produced_ws: 1000,
                grid_import_ws: 3000,
                grid_export_ws: 3000,
                battery_charged_ws: 0,
                battery_discharged_ws: 2000,
                battery_soc_bp: 2010,
                complete: false,
                updated_at_s: e3.ts.timestamp(),
            }
        );

        let expected_inverter_data = Some(InverterData {
            last_event: e3,
            data_missing: false,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 0,
            grid_import_ws: 0,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 0,
        });
        assert_eq!(state, expected_inverter_data);

        // Another event in the new quarter, JSON & state are equal
        let e4 = InverterEvent {
            ts: Utc.with_ymd_and_hms(2025, 12, 13, 15, 0, 25).unwrap(),
            sn: "1234".to_string(),
            pv_w: 0,
            loads_w: -600,
            grid_w: -500,
            battery_w: -100,
            battery_pct: 20.1,
        };

        let json_data = update_inverter_data(&mut state, e4.clone());
        assert_eq!(
            json_data,
            InverterAgg15 {
                inverter_sn: "1234".to_string(),
                started_at_s: Utc
                    .with_ymd_and_hms(2025, 12, 13, 15, 0, 0)
                    .unwrap()
                    .timestamp(),
                pv_ws: 0,
                loads_ws: 0,
                load_produced_ws: 12000,
                grid_import_ws: 0,
                grid_export_ws: 10000,
                battery_charged_ws: 2000,
                battery_discharged_ws: 0,
                battery_soc_bp: 2010,
                complete: false,
                updated_at_s: e4.ts.timestamp(),
            }
        );

        let expected_inverter_data = Some(InverterData {
            last_event: e4,
            data_missing: false,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 12000,
            grid_import_ws: 0,
            grid_export_ws: 10000,
            battery_charged_ws: 2000,
            battery_discharged_ws: 0,
        });
        assert_eq!(state, expected_inverter_data);
    }

    #[test]
    fn no_missing_data_and_full_15_minutes_is_complete() {
        let mut state = Some(InverterData {
            last_event: event_at(18, 59, 50),
            data_missing: false,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 0,
            grid_import_ws: 0,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 0,
        });

        assert!(update_inverter_data(&mut state, event_at(19, 0, 2)).complete);
    }

    #[test]
    fn no_missing_data_but_not_full_15_minutes_is_not_complete() {
        let mut state = Some(InverterData {
            last_event: event_at(18, 59, 50),
            data_missing: false,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 0,
            grid_import_ws: 0,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 0,
        });

        let actual = update_inverter_data(&mut state, event_at(18, 59, 52)).complete;
        assert!(!actual);
    }

    #[test]
    fn full_15_minutes_but_missing_data_is_not_complete() {
        let mut state = Some(InverterData {
            last_event: event_at(18, 59, 50),
            data_missing: true,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 0,
            grid_import_ws: 0,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 0,
        });

        let actual = update_inverter_data(&mut state, event_at(19, 0, 2)).complete;
        assert!(!actual);
    }

    #[test]
    fn no_data_missing_for_up_to_30_secs() {
        let mut state = Some(InverterData {
            last_event: event_at(19, 50, 50),
            data_missing: false,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 0,
            grid_import_ws: 0,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 0,
        });

        update_inverter_data(&mut state, event_at(19, 51, 20));
        assert!(!state.unwrap().data_missing);
    }

    #[test]
    fn more_than_30_secs_between_data_missing() {
        let mut state = Some(InverterData {
            last_event: event_at(19, 50, 50),
            data_missing: false,
            pv_ws: 0,
            loads_ws: 0,
            load_produced_ws: 0,
            grid_import_ws: 0,
            grid_export_ws: 0,
            battery_charged_ws: 0,
            battery_discharged_ws: 0,
        });

        update_inverter_data(&mut state, event_at(19, 51, 21));
        assert!(state.unwrap().data_missing);
    }

    fn event_at(hour: u32, min: u32, sec: u32) -> InverterEvent {
        InverterEvent {
            ts: Utc.with_ymd_and_hms(2026, 1, 2, hour, min, sec).unwrap(),
            sn: "1234".to_string(),
            pv_w: 0,
            loads_w: 0,
            grid_w: 0,
            battery_w: 0,
            battery_pct: 0.0,
        }
    }
}
