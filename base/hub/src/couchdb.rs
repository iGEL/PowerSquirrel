use anyhow::Result;
use reqwest::{Client, StatusCode};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use tokio::sync::Mutex;

pub fn init(config: CouchDBConfig) -> CouchDB {
    CouchDB {
        base_uri: config.base_uri,
        http_client: Client::new(),
        revs: Mutex::new(HashMap::<String, String>::new()),
    }
}

#[derive(Deserialize, Debug)]
pub struct CouchDBConfig {
    base_uri: String,
}

pub struct CouchDB {
    http_client: reqwest::Client,
    base_uri: String,
    revs: Mutex<HashMap<String, String>>,
}

impl CouchDB {
    pub async fn write(&self, db: String, id: String, doc: &impl Serialize) -> Result<()> {
        let cur_rev = {
            let guard = self.revs.lock().await;
            guard.get(&id).cloned()
        };

        let new_rev = int_write(
            self.http_client.clone(),
            format!("{}/{}/{}", self.base_uri, db, id),
            &doc,
            cur_rev,
        )
        .await?;
        let mut guard = self.revs.lock().await;
        guard.insert(id, new_rev);
        Ok(())
    }
}

#[derive(Deserialize)]
struct CouchDBResponse {
    #[serde(rename = "_rev", alias = "rev")]
    rev: String,
}

async fn int_write(
    http_client: reqwest::Client,
    uri: String,
    doc: &impl Serialize,
    rev: Option<String>,
) -> Result<String> {
    let mut cur_rev = rev;
    let mut retries = 3;
    loop {
        let mut req = http_client.put(uri.clone()).json(&doc);

        if let Some(rev) = cur_rev.clone() {
            req = req.query(&[("rev", rev)]);
        }

        let resp = req.send().await?;

        match resp.status() {
            StatusCode::OK | StatusCode::CREATED | StatusCode::ACCEPTED => {
                let body = resp.json::<CouchDBResponse>().await?;
                return Ok(body.rev);
            }
            StatusCode::CONFLICT => {
                if retries == 0 {
                    return Err(anyhow::anyhow!("Couldn't fetch a current rev"));
                } else {
                    let refresh_resp = http_client.get(uri.clone()).send().await?;
                    let body = refresh_resp.json::<CouchDBResponse>().await?;
                    cur_rev = Some(body.rev);
                    retries -= 1;
                }
            }
            status => {
                return Err(anyhow::anyhow!("Unexpected response {}", status));
            }
        }
    }
}
