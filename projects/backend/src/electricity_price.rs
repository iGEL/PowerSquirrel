use bigdecimal::BigDecimal;
use chrono::{NaiveDate, NaiveTime};
use serde::{Deserialize, Serialize};
use std::fs::File;
use std::io::BufReader;

#[derive(Debug, Deserialize, Serialize)]
pub struct ElectricityFees {
    fees: Vec<FeePeriod>,
}

#[derive(Debug, Deserialize, Serialize)]
struct FeePeriod {
    valid_from: NaiveDate,
    fees: Vec<Fee>,
}

#[derive(Debug, Deserialize, Serialize)]
struct Fee {
    name: String,
    pricing: String,
    currency: String,
    schedule: FeeSchedule,
}

#[derive(Debug, Deserialize, Serialize)]
struct FeeSchedule {
    default: Vec<FeeScheduleEntry>,
    #[serde(skip_serializing_if = "Option::is_none")]
    monday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    tuesday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    wednesday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    thursday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    friday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    saturday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    sunday: Option<Vec<FeeScheduleEntry>>,
}

#[derive(Debug, Deserialize, Serialize)]
struct FeeScheduleEntry {
    start: NaiveTime,
    price: BigDecimal,
}

pub fn parse(path: &str) -> Result<ElectricityFees, &'static str> {
    let file = File::open(path);
    if file.is_ok() {
        let reader = BufReader::new(file.unwrap());
        let fees = serde_json::from_reader(reader).expect("Failed to parse JSON");

        Ok(fees)
    } else {
        Err("Couldn't open path")
    }
}
