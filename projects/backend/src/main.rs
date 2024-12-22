use chrono::{TimeZone, Utc};
use reqwest::Client;
use serde::Deserialize;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let zip = "12207";
    let country = "Germany";
    let result = geocode_zip(zip, country).await?;
    sun_position(result);
    Ok(())
}

async fn geocode_zip(zip: &str, country: &str) -> Result<Location, Box<dyn std::error::Error>> {
    let url = format!("https://nominatim.openstreetmap.org/search.php?country={country}&postalcode={zip}&format=jsonv2");
    let client = Client::new();
    let response = client
        .get(&url)
        .header("User-Agent", "PowerQuirrel (igel@igels.net)")
        .send()
        .await?;

    if response.status().is_success() {
        let results: Vec<NominatimResult> = response.json().await?;
        if let Some(first_result) = results.into_iter().next() {
            // Convert lat & lon from strings to f64
            let lat = first_result.lat.parse::<f64>()?;
            let lon = first_result.lon.parse::<f64>()?;
            return Ok(Location { lat, lon });
        } else {
            return Err("No results found in response".into());
        }
    } else {
        return Err(format!("Request failed with status {}", response.status()).into());
    }
}

#[derive(Debug, Deserialize)]
struct Location {
    lat: f64,
    lon: f64,
}

#[derive(Debug, Deserialize)]
struct NominatimResult {
    lat: String,
    lon: String,
}

fn sun_position(location: Location) {
    let date_time = Utc.with_ymd_and_hms(2024, 12, 1, 10, 13, 1).unwrap();
    let pos = sun::pos(date_time.timestamp_millis(), location.lat, location.lon);
    println!(
        "Solar Altitude: {:.2}° Azimuth: {:.2}°",
        pos.altitude.to_degrees(),
        pos.azimuth.to_degrees()
    );
}
