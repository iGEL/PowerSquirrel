use bigdecimal::BigDecimal;
use serde::{Deserialize, Serialize};
use std::fmt;

#[derive(Clone, Debug, Deserialize, Serialize, PartialEq)]
pub struct Money {
    pub amount: BigDecimal,
    pub currency: String,
}
impl Money {
    pub fn new(amount: BigDecimal, currency: &str) -> Self {
        Money {
            amount,
            currency: String::from(currency),
        }
    }
    pub fn fmt_rounded(&self) -> String {
        let rounded = self.amount.round(2);
        format!("{:.2} {}", rounded, self.currency)
    }
}
impl fmt::Display for Money {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} {}", self.amount, self.currency)
    }
}
