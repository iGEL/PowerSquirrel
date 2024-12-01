use chrono::{TimeZone, Utc};

fn main() {
    let latitude = 52.5;
    let longitude = 13.5;
    let date_time = Utc.with_ymd_and_hms(2024, 12, 1, 10, 13, 1).unwrap();
    let pos = sun::pos(date_time.timestamp_millis(), latitude, longitude);
    println!(
        "Solar Altitude: {:.2}° Azimuth: {:.2}°",
        pos.altitude.to_degrees(),
        pos.azimuth.to_degrees()
    );
}
