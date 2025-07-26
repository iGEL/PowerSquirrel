use bigdecimal::BigDecimal;
use std::str::FromStr;
mod electricity_price;
use chrono::Local;
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

    let fees = electricity_price::parse("resources/fees.json")?;
    println!("{:?}", fees);
    let net_price = electricity_price::Money::new(BigDecimal::from_str("0.06807").unwrap(), "EUR");
    let date = Local::now();
    let date_tz = date.with_timezone(date.offset());

    let breakdown = electricity_price::calculate_detailed(&net_price, &date_tz, &fees).unwrap();
    println!("Net price: {}", net_price);
    for fee in breakdown.applied_fees.iter() {
        println!(
            "{}: {} {} = {}",
            fee.name, fee.rate, fee.pricing_type, fee.price
        );
    }
    println!("Gross price: {}", breakdown.total_price);

    Ok(())
}
