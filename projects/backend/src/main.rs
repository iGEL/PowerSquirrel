mod geocode;
mod sun;
mod weather;

use geocode::geocode_zip;
use geocode::Location;
use sun::sun_position;
use weather::fetch_weather;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let zip = "12207";
    let country = "Germany";
    let location = geocode_zip(zip, country).await?;
    let weather = fetch_weather(&location).await?;

    for hour in weather.iter() {
        let sun = sun_position(hour.time, &location);
        println!(
            "{} Temperature: {:.2}°, Clouds: {}% - Solar Altitude: {:.2}° Azimuth: {:.2}°",
            hour.time, hour.temp, hour.clouds, sun.altitude, sun.azimuth
        );
    }

    Ok(())
}
