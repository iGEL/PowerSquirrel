use crate::Location;

pub fn sun_position(date_time: chrono::DateTime<chrono::Utc>, location: Location) -> SunPosition {
    let pos = sun::pos(date_time.timestamp_millis(), location.lat, location.lon);
    return SunPosition { altitude: pos.altitude.to_degrees(), azimuth: pos.azimuth.to_degrees() };
}


#[derive(Debug)]
pub struct SunPosition {
    pub altitude: f64,
    pub azimuth: f64,
}
