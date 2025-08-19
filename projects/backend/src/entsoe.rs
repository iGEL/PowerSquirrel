use chrono::{Datelike, Utc};
use quick_xml::events::Event;
use quick_xml::Reader;
use reqwest::Client;
use std::env;

pub async fn fetch() -> anyhow::Result<()> {
    let token = env::var("ENTSOE_TOKEN")?;

    // Get today's and tomorrow's dates in UTC for the query
    let today = Utc::now().date_naive();
    let tomorrow = today.succ_opt().unwrap();

    let start = format!("{}{:02}{:02}0000", today.year(), today.month(), today.day());
    let end = format!(
        "{}{:02}{:02}0000",
        tomorrow.year(),
        tomorrow.month(),
        tomorrow.day()
    );

    // DE-LU bidding zone EIC code
    let domain = "10Y1001A1001A83F";

    // Build the URL according to ENTSO-E API guide (§3.1 + Appendix A)
    let url = format!(
        "https://web-api.tp.entsoe.eu/api?securityToken={}&documentType=A44&in_Domain={}&out_Domain={}&periodStart={}&periodEnd={}",
        token, domain, domain, start, end
    );

    println!("Fetching: {}", url);

    let client = Client::new();
    let response = client.get(&url).send().await?.bytes().await?;

    // Parse XML to extract <price.amount> values
    let mut reader = Reader::from_reader(response.as_ref());
    reader.config_mut().trim_text(true);

    let mut buf = Vec::new();
    let mut prices = Vec::new();

    loop {
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(ref e)) if e.name().as_ref() == b"price.amount" => {
                if let Ok(Event::Text(text)) = reader.read_event_into(&mut buf) {
                    let price = String::from_utf8_lossy(text.as_ref()).into_owned();
                    prices.push(price);
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => return Err(e.into()),
            _ => {}
        }
        buf.clear();
    }

    println!("Prices (€/MWh): {:?}", prices);

    Ok(())
}
