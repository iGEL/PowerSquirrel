use crate::money::Money;
use bigdecimal::BigDecimal;
use chrono::{DateTime, Datelike, FixedOffset, NaiveDate, NaiveTime};
use serde::{Deserialize, Serialize};
use std::fs::File;
use std::io::BufReader;

#[derive(Debug, Serialize, PartialEq)]
pub struct PricingBreakdown {
    pub net_price: Money,
    pub applied_fees: Vec<AppliedFee>,
    pub total_price: Money,
}

#[derive(Debug, Serialize, PartialEq)]
pub struct AppliedFee {
    pub name: String,
    pub pricing_type: String,
    pub rate: BigDecimal,
    pub price: Money,
}

#[derive(Debug, Deserialize, Serialize)]
pub struct ElectricityFees {
    fees: Vec<FeePeriod>,
}

#[derive(Debug, Deserialize, Serialize)]
struct FeePeriod {
    valid_from: NaiveDate,
    fees: Vec<Fee>,
}

#[derive(Debug, Deserialize, Serialize)]
struct Fee {
    name: String,
    pricing: String,
    currency: String,
    schedule: FeeSchedule,
}

#[derive(Debug, Deserialize, Serialize)]
struct FeeSchedule {
    default: Vec<FeeScheduleEntry>,
    #[serde(skip_serializing_if = "Option::is_none")]
    monday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    tuesday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    wednesday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    thursday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    friday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    saturday: Option<Vec<FeeScheduleEntry>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    sunday: Option<Vec<FeeScheduleEntry>>,
}

#[derive(Debug, Deserialize, Serialize)]
struct FeeScheduleEntry {
    start: NaiveTime,
    price: BigDecimal,
}

pub fn parse(path: &str) -> Result<ElectricityFees, &'static str> {
    let file = File::open(path);
    if file.is_ok() {
        let reader = BufReader::new(file.unwrap());
        let fees = serde_json::from_reader(reader).expect("Failed to parse JSON");

        Ok(fees)
    } else {
        Err("Couldn't open path")
    }
}

pub fn calculate_detailed(
    net_price: &Money,
    date: &DateTime<FixedOffset>,
    fees: &ElectricityFees,
) -> Result<PricingBreakdown, Box<dyn std::error::Error>> {
    let applicable_fees = find_applicable_fees(fees, date.date_naive())?;

    let mut applied_fees = Vec::new();
    let mut net_total = net_price.amount.clone();

    // Apply per_kwh fees first
    for fee in &applicable_fees.fees {
        if fee.pricing == "per_kwh" {
            let rate = get_rate_for_time(&fee.schedule, date)?;
            let fee_price = Money::new(rate.clone(), &fee.currency);

            applied_fees.push(AppliedFee {
                name: fee.name.clone(),
                pricing_type: fee.pricing.clone(),
                rate,
                price: fee_price.clone(),
            });

            net_total += &fee_price.amount;
        }
    }

    // Apply percentage fees (like VAT) on the net + per_kwh fees
    for fee in &applicable_fees.fees {
        if fee.pricing == "percent" {
            let rate = get_rate_for_time(&fee.schedule, date)?;
            let fee_amount = &net_total * &rate / BigDecimal::from(100);
            let fee_price = Money::new(fee_amount.clone(), &fee.currency);

            applied_fees.push(AppliedFee {
                name: fee.name.clone(),
                pricing_type: fee.pricing.clone(),
                rate,
                price: fee_price.clone(),
            });

            net_total += &fee_amount;
        }
    }

    Ok(PricingBreakdown {
        net_price: net_price.clone(),
        applied_fees,
        total_price: Money::new(net_total, &net_price.currency),
    })
}

pub fn calculate(
    net_price: &Money,
    date: &DateTime<FixedOffset>,
    fees: &ElectricityFees,
) -> Result<Money, Box<dyn std::error::Error>> {
    let breakdown = calculate_detailed(net_price, date, fees)?;
    Ok(breakdown.total_price)
}

fn find_applicable_fees(
    fees: &ElectricityFees,
    date: NaiveDate,
) -> Result<&FeePeriod, &'static str> {
    fees.fees
        .iter()
        .filter(|period| period.valid_from <= date)
        .max_by_key(|period| period.valid_from)
        .ok_or("No applicable fee period found")
}

fn get_rate_for_time(
    schedule: &FeeSchedule,
    datetime: &DateTime<FixedOffset>,
) -> Result<BigDecimal, &'static str> {
    let day_schedule = match datetime.weekday() {
        chrono::Weekday::Mon => schedule.monday.as_ref(),
        chrono::Weekday::Tue => schedule.tuesday.as_ref(),
        chrono::Weekday::Wed => schedule.wednesday.as_ref(),
        chrono::Weekday::Thu => schedule.thursday.as_ref(),
        chrono::Weekday::Fri => schedule.friday.as_ref(),
        chrono::Weekday::Sat => schedule.saturday.as_ref(),
        chrono::Weekday::Sun => schedule.sunday.as_ref(),
    }
    .unwrap_or(&schedule.default);

    let time = datetime.time();
    let applicable_entry = day_schedule
        .iter()
        .filter(|entry| entry.start <= time)
        .max_by_key(|entry| entry.start)
        .ok_or("No applicable rate found for time")?;

    Ok(applicable_entry.price.clone())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::str::FromStr;

    fn create_test_fees() -> ElectricityFees {
        ElectricityFees {
            fees: vec![FeePeriod {
                valid_from: NaiveDate::from_ymd_opt(2025, 1, 1).unwrap(),
                fees: vec![
                    Fee {
                        name: "Netzentgelte".to_string(),
                        pricing: "per_kwh".to_string(),
                        currency: "EUR".to_string(),
                        schedule: FeeSchedule {
                            default: vec![FeeScheduleEntry {
                                start: NaiveTime::from_hms_opt(0, 0, 0).unwrap(),
                                price: BigDecimal::from_str("0.1186").unwrap(),
                            }],
                            monday: None,
                            tuesday: None,
                            wednesday: None,
                            thursday: None,
                            friday: None,
                            saturday: None,
                            sunday: None,
                        },
                    },
                    Fee {
                        name: "Mehrwertsteuer".to_string(),
                        pricing: "percent".to_string(),
                        currency: "EUR".to_string(),
                        schedule: FeeSchedule {
                            default: vec![FeeScheduleEntry {
                                start: NaiveTime::from_hms_opt(0, 0, 0).unwrap(),
                                price: BigDecimal::from_str("19").unwrap(),
                            }],
                            monday: None,
                            tuesday: None,
                            wednesday: None,
                            thursday: None,
                            friday: None,
                            saturday: None,
                            sunday: None,
                        },
                    },
                ],
            }],
        }
    }

    #[test]
    fn basic_pricing_with_fees() {
        let net_price = Money::new(BigDecimal::from_str("1.00").unwrap(), "EUR");
        let date = DateTime::parse_from_rfc3339("2025-04-26T18:44:00+02:00").unwrap();
        let fees = create_test_fees();

        let breakdown = calculate_detailed(&net_price, &date, &fees).unwrap();

        assert_eq!(
            breakdown.net_price.amount,
            BigDecimal::from_str("1.00").unwrap()
        );
        assert_eq!(breakdown.applied_fees.len(), 2);

        // Check Netzentgelte fee
        let network_fee = &breakdown.applied_fees[0];
        assert_eq!(network_fee.name, "Netzentgelte");
        assert_eq!(network_fee.pricing_type, "per_kwh");
        assert_eq!(network_fee.rate, BigDecimal::from_str("0.1186").unwrap());
        assert_eq!(
            network_fee.price.amount,
            BigDecimal::from_str("0.1186").unwrap()
        );

        // Check VAT fee (19% on 1.00 + 0.1186 = 19% on 1.1186)
        let vat_fee = &breakdown.applied_fees[1];
        assert_eq!(vat_fee.name, "Mehrwertsteuer");
        assert_eq!(vat_fee.pricing_type, "percent");
        assert_eq!(vat_fee.rate, BigDecimal::from_str("19").unwrap());

        // Total should be 1.00 + 0.1186 + 19% of 1.1186 = 1.00 + 0.1186 + 0.212534 = 1.331134
        let expected_total = BigDecimal::from_str("1.331134").unwrap();
        assert_eq!(breakdown.total_price.amount, expected_total);
    }

    #[test]
    fn calculate_simple_wrapper() {
        let net_price = Money::new(BigDecimal::from_str("1.00").unwrap(), "EUR");
        let date = DateTime::parse_from_rfc3339("2025-04-26T18:44:00+02:00").unwrap();
        let fees = create_test_fees();

        let total = calculate(&net_price, &date, &fees).unwrap();

        let expected_total = BigDecimal::from_str("1.331134").unwrap();
        assert_eq!(total.amount, expected_total);
        assert_eq!(total.currency, "EUR");
    }

    #[test]
    fn time_based_pricing() {
        let mut fees = create_test_fees();
        // Update network fee to have time-based pricing
        fees.fees[0].fees[0].schedule.default = vec![
            FeeScheduleEntry {
                start: NaiveTime::from_hms_opt(0, 0, 0).unwrap(),
                price: BigDecimal::from_str("0.05").unwrap(),
            },
            FeeScheduleEntry {
                start: NaiveTime::from_hms_opt(8, 0, 0).unwrap(),
                price: BigDecimal::from_str("0.15").unwrap(),
            },
            FeeScheduleEntry {
                start: NaiveTime::from_hms_opt(20, 0, 0).unwrap(),
                price: BigDecimal::from_str("0.05").unwrap(),
            },
        ];

        let net_price = Money::new(BigDecimal::from_str("1.00").unwrap(), "EUR");

        // Test during peak hours (10:00)
        let peak_date = DateTime::parse_from_rfc3339("2025-04-26T10:00:00+02:00").unwrap();
        let peak_breakdown = calculate_detailed(&net_price, &peak_date, &fees).unwrap();
        let peak_network_fee = &peak_breakdown.applied_fees[0];
        assert_eq!(peak_network_fee.rate, BigDecimal::from_str("0.15").unwrap());

        // Test during off-peak hours (22:00)
        let offpeak_date = DateTime::parse_from_rfc3339("2025-04-26T22:00:00+02:00").unwrap();
        let offpeak_breakdown = calculate_detailed(&net_price, &offpeak_date, &fees).unwrap();
        let offpeak_network_fee = &offpeak_breakdown.applied_fees[0];
        assert_eq!(
            offpeak_network_fee.rate,
            BigDecimal::from_str("0.05").unwrap()
        );
    }
}
