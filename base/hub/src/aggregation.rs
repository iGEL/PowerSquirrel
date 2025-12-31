use crate::MqttConfig;
use anyhow::Result;
use chrono::{DateTime, Timelike, Utc};
use rumqttc::{AsyncClient, Event, Incoming, MqttOptions, QoS};
use serde::Deserialize;
use serde::Serialize;
use std::time::Duration;

pub async fn aggregate_inverters(mqtt_config: &MqttConfig) -> Result<()> {
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
                        println!("{}", serde_json::to_string(&json_data).unwrap());
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

#[derive(Debug, PartialEq, Clone, Serialize)]
struct InverterData {
    #[serde(skip_serializing)]
    last_event: InverterEvent,
    #[serde(skip_serializing)]
    data_missing: bool, // true for the first quarter & when the gap between 2 samples exceeds 30s
    pv_ws: u32, // ws = watt seconds
    loads_ws: u32,
    load_produced_ws: u32, // If sources like plug in PV produce more than is consumed
    grid_import_ws: u32,
    grid_export_ws: u32,
    battery_charged_ws: u32,
    battery_discharged_ws: u32,
}

#[derive(Serialize)]
struct InverterDataJson {
    #[serde(rename = "_id")]
    id: String,
    #[serde(rename = "_rev", skip_serializing_if = "Option::is_none")]
    rev: Option<String>,
    #[serde(flatten)]
    base: InverterData,
    complete: bool,
    battery_pct: f64,
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

// Updates the given aggregated inverter dates with the given event. Every 15 minutes, the data is
// reset. Returns the InverterDataJson to store in couchdb
fn update_inverter_data(state: &mut Option<InverterData>, ev: InverterEvent) -> InverterDataJson {
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
    InverterDataJson {
        id: format!(
            "agg15:inverter:{}:{}",
            ev.sn,
            floor_to_quarter(data_to_return.last_event.ts).format("%Y-%m-%dT%H:%MZ")
        ),
        rev: None,
        complete: (is_complete && !data_to_return.data_missing),
        battery_pct: ev.battery_pct,
        base: data_to_return,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::{TimeZone, Utc};

    fn event(time: DateTime<Utc>) -> InverterEvent {
        InverterEvent {
            ts: time,
            grid_import_today_wh: 12,
            grid_export_today_wh: 1,
            pv_today_wh: 1,
            pv_to_load_today_wh: 1,
            pv_to_battery_today_wh: 1,
            pv_to_grid_today_wh: 1,
            battery_charged_today_wh: 1,
            battery_discharged_today_wh: 1,
        }
    }

    #[test]
    fn update_ref_state_test() {
        let mut state: Option<RefState> = None;
        let e1 = event(Utc.with_ymd_and_hms(2025, 12, 13, 13, 0, 30).unwrap());

        update_ref_state(&mut state, e1.clone());

        let expected = Some(RefState {
            current_event: e1.clone(),
            ref_event: e1.clone(),
        });
        assert_eq!(state, expected);

        // Expect the current event updated within the same quarter hour
        let e2 = event(Utc.with_ymd_and_hms(2025, 12, 13, 13, 14, 59).unwrap());

        update_ref_state(&mut state, e2.clone());

        let expected = Some(RefState {
            current_event: e2.clone(),
            ref_event: e1,
        });
        assert_eq!(state, expected);

        // Expect everything updated when the quarter changes
        let e3 = event(Utc.with_ymd_and_hms(2025, 12, 13, 13, 15, 0).unwrap());

        update_ref_state(&mut state, e3.clone());

        let expected = Some(RefState {
            current_event: e3,
            ref_event: e2,
        });
        assert_eq!(state, expected);
    }
}
