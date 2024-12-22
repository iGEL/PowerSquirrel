use reqwest::Client;
use serde::Deserialize;

pub async fn geocode_zip(zip: &str, country: &str) -> Result<Location, Box<dyn std::error::Error>> {
    let url = format!("https://nominatim.openstreetmap.org/search.php?country={country}&postalcode={zip}&format=jsonv2");
    let client = Client::new();
    let response = client
        .get(&url)
        .header("User-Agent", "PowerSquirrel (igel@igels.net)")
        .send()
        .await?;

    if response.status().is_success() {
        let results: Vec<NominatimResult> = response.json().await?;
        if let Some(first_result) = results.into_iter().next() {
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
pub struct Location {
    pub lat: f64,
    pub lon: f64,
}

#[derive(Debug, Deserialize)]
struct NominatimResult {
    lat: String,
    lon: String,
}
