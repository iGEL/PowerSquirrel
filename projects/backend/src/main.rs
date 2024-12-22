mod geocode;
mod sun;

use geocode::geocode_zip;
use geocode::Location;
use sun::sun_position;
use chrono::{TimeZone, Utc};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let zip = "12207";
    let country = "Germany";
    let date_time = Utc.with_ymd_and_hms(2024, 12, 1, 10, 13, 1).unwrap();
    let location = geocode_zip(zip, country).await?;
    let sun = sun_position(date_time, location);

    println!(
        "Solar Altitude: {:.2}° Azimuth: {:.2}°",
        sun.altitude,
        sun.azimuth
    );
    Ok(())
}
