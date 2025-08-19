use bigdecimal::BigDecimal;
use std::str::FromStr;
mod electricity_price;
mod money;
use chrono::{DateTime, Duration, FixedOffset, Local};
use money::Money;
mod entsoe;
mod geocode;
mod sun;
mod sungrow;
mod weather;

use geocode::geocode_zip;
use geocode::Location;
use sun::sun_position;
use weather::fetch_weather;

#[derive(Debug)]
struct PriceEntry {
    start: DateTime<FixedOffset>,
    price: Money,
}

fn get_net_prices() -> Vec<PriceEntry> {
    [
        ("2025-07-26T00:00:00+02:00", "0.10676"),
        ("2025-07-26T01:00:00+02:00", "0.1007"),
        ("2025-07-26T02:00:00+02:00", "0.09773"),
        ("2025-07-26T03:00:00+02:00", "0.09665"),
        ("2025-07-26T04:00:00+02:00", "0.09645"),
        ("2025-07-26T05:00:00+02:00", "0.09489"),
        ("2025-07-26T06:00:00+02:00", "0.09713"),
        ("2025-07-26T07:00:00+02:00", "0.0961"),
        ("2025-07-26T08:00:00+02:00", "0.08786"),
        ("2025-07-26T09:00:00+02:00", "0.08585"),
        ("2025-07-26T10:00:00+02:00", "0.07959"),
        ("2025-07-26T11:00:00+02:00", "0.0752"),
        ("2025-07-26T12:00:00+02:00", "0.06642"),
        ("2025-07-26T13:00:00+02:00", "0.04987"),
        ("2025-07-26T14:00:00+02:00", "0.0509"),
        ("2025-07-26T15:00:00+02:00", "0.06807"),
        ("2025-07-26T16:00:00+02:00", "0.07481"),
        ("2025-07-26T17:00:00+02:00", "0.0842"),
        ("2025-07-26T18:00:00+02:00", "0.0982"),
        ("2025-07-26T19:00:00+02:00", "0.1066"),
        ("2025-07-26T20:00:00+02:00", "0.12195"),
        ("2025-07-26T21:00:00+02:00", "0.12707"),
        ("2025-07-26T22:00:00+02:00", "0.11672"),
        ("2025-07-26T23:00:00+02:00", "0.1088"),
    ]
    .iter()
    .map(|(date_str, price)| PriceEntry {
        start: DateTime::parse_from_rfc3339(&date_str).unwrap(),
        price: Money::new(BigDecimal::from_str(price).unwrap(), "EUR"),
    })
    .collect()
}

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
    let date = Local::now();
    let date_tz = date.with_timezone(date.offset());

    let net_prices = get_net_prices();

    let mut current_time = DateTime::parse_from_rfc3339("2025-07-26T00:00:00+02:00").unwrap();
    let end_time = DateTime::parse_from_rfc3339("2025-07-26T23:45:00+02:00").unwrap();
    let step = Duration::minutes(15);

    while current_time <= end_time {
        let entry = net_prices
            .iter()
            .rev()
            .find(|e| e.start <= current_time)
            .unwrap();
        let total = electricity_price::calculate(&entry.price, &current_time, &fees).unwrap();

        println!(
            "{}: {} net - {} gross",
            current_time.format("%H:%M"),
            entry.price.fmt_rounded(),
            total.fmt_rounded()
        );
        current_time = current_time.checked_add_signed(step).unwrap();
    }

    let entry = net_prices
        .iter()
        .rev()
        .find(|e| e.start <= date_tz)
        .unwrap();
    let breakdown = electricity_price::calculate_detailed(&entry.price, &date_tz, &fees).unwrap();
    println!("Net price for {}: {}", date_tz.format("%H:%M"), entry.price);
    for fee in breakdown.applied_fees.iter() {
        println!(
            "{}: {} {} = {}",
            fee.name, fee.rate, fee.pricing_type, fee.price
        );
    }
    println!("Gross price: {}", breakdown.total_price);

    entsoe::fetch().await?;

    Ok(())
}
