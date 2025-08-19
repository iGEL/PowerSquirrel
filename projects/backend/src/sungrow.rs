use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::fs::File;
use std::io::BufReader;

use anyhow::Result;

const BASE_URI: &str = "https://gateway.isolarcloud.eu";

#[derive(Debug, Deserialize, Serialize)]
struct Token {
    secret_key: String,
    appkey: String,
    access_token: String,
    refresh_token: String,
}

fn read_token() -> Result<Token, &'static str> {
    let file = File::open("resources/sungrow_token.json");
    if file.is_ok() {
        let reader = BufReader::new(file.unwrap());
        let fees = serde_json::from_reader(reader).expect("Failed to parse JSON");

        Ok(fees)
    } else {
        Err("Couldn't open path")
    }
}

pub async fn debug() -> Result<()> {
    let client = Client::new();
    let token: Token = read_token().unwrap();

    let body = json!({
        "appkey": token.appkey,
        "page": 1,
        "size": 10,
    });

    let res = client
        .post(format!(
            "{}/openapi/platform/queryPowerStationList",
            BASE_URI
        ))
        .header("Authorization", format!("Bearer {}", token.access_token))
        .header("x-access-key", token.secret_key)
        .json(&body)
        .send()
        .await?;

    let text = res.text().await?;
    println!("{}", text);

    Ok(())
}
