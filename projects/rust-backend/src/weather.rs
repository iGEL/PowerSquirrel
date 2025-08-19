use crate::Location;
use chrono::{DateTime, TimeZone, Utc};
use reqwest::Client;
use serde::Deserialize;
use std::env;

#[derive(Debug)]
pub struct Weather {
    pub time: DateTime<Utc>,
    pub temp: f64,
    pub clouds: u8,
}

#[derive(Debug, Deserialize)]
struct IntWeather {
    dt: i64,
    temp: f64,
    clouds: u8,
}

#[derive(Debug, Deserialize)]
struct IntResponse {
    hourly: Vec<IntWeather>,
}

fn int_to_weather(int: IntWeather) -> Weather {
    return Weather {
        time: Utc.timestamp_opt(int.dt, 0).unwrap(),
        temp: int.temp,
        clouds: int.clouds,
    };
}

pub async fn fetch_weather(
    location: &Location,
) -> Result<Vec<Weather>, Box<dyn std::error::Error>> {
    let api_key = match env::var("OPENWEATHERMAP_APIKEY") {
        Ok(key) => key,
        Err(_e) => return Err("OPENWEATHERMAP_APIKEY is not present".into()),
    };
    let url = format!(
        "https://api.openweathermap.org/data/3.0/onecall?lat={}&lon={}&exclude=minutely,daily,alerts&units=metric&appid={api_key}",
        location.lat,
        location.lon
);
    let client = Client::new();
    let response = client
        .get(&url)
        .header("User-Agent", "PowerSquirrel (igel@igels.net)")
        .send()
        .await?;

    if response.status().is_success() {
        let result: IntResponse = response.json().await?;
        let res: Vec<Weather> = result.hourly.into_iter().map(int_to_weather).collect();
        return Ok(res);
    } else {
        return Err(format!("Request failed with status {}", response.status()).into());
    }
}
