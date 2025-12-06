# Sungrow Inverter Connector

This is a PowerSquirrel connector that collects the required data from Sungrow inverters and publishes them as JSON via mqtt. It can be configured via the config.json, see [this example](./config.example.json).

It might be useful to start out with `cp config.example.json config.json`.

## Example MQTT message (version 0.1)

The topic for the MQTT messages is `posq/inverter/sungrow/<serial_number>`.

```json
{
  "ts": "2025-11-14T16:15:10Z", // Timestamp of the data sample
  "sn": "A0123456789", // Serial number of the inverter
  "pv_w": 1200, // Current solar yield in watts
  "pv_today_wh": 3000, // Today's solar yield in watt hours
  "pv_to_load_today_wh": 200, // How many watt hours were used by load directly from solar panels
  "pv_to_battery_today_wh": 200, // How many watt hours were used to charge the battery from solar panels
  "pv_to_grid_today_wh": 200, // How many watt hours were fed into the grid from solar panels
  "loads_w": 1200, // Current load in watts
  "grid_w": 1000, // Current grid usage, negative values represent feed in. In watts
  "grid_export_today_wh": 1000, // Today's grid feed-in in watt hours
  "grid_import_today_wh": 1000, // Today's grid usage in watt hours
  "battery_w": 200, // Current charging load in watts, negative values represent discharging
  "battery_pct": 73.8, // Current battery level in percent
  "battery_charged_today_wh": 23, // How many watt hours were charged today
  "battery_discharged_today_wh": 2222, // How many watt hours were discharged from the battery today
  "battery_status": "charging", // "discharging", "hold"
  "battery_charge_discharge_w": 6250, // The allowed power in watts to charge/discharge the battery
}
```

With `--print`, the connector prints this JSON to the terminal and exits, without publishing to MQTT.

## Writing values

My notes how charge/discharge the battery:

We only need the `External EMS mode` if we want to force charging/discharging or holding the battery. If we want to charge the battery if the pv yield is higher than load usage or we want the load to use battery if the yield isn't high enough, it's probably smarter to switch back to `Self-consumption mode`.

1. Set the `EMS mode selection` (Register 13050 / Address 13049) to 3 (`External EMS mode`)
2. Start setting the `External EMS heartbeat` (Register 13080 / Address 13079).
  * Setting the value is sending the "I'm still alive" message
  * The value in seconds is the timeout after which the inverter returns to `Self-consumption mode` for the `EMS mode selection`.
  * You need to continously write the same value to the address


### Force charging

1. Enable `External EMS mode` & start sending the heartbeat as described above
2. Set `Charge/discharge command` (Register 13051 / Address 13050) to `0xAA` (Charge)
3. Set `Charge/discharge power` (Register 13052 / Address 13051) to the amount in watt you want to allow for charging
4. You can set `Max. SOC` (Register 13058 / Address 13057) to a value between 50 and 100% (The unit is 0.1%, so 751 are 75.1%). The interter will stop charging if the SOC reaches or rises above this level.
5. You can set `Max. Charging Power` (Register 33047 / Address 33046) to limit the charging power (The unit is 0.01kw, so 244 are 2.44kw)

### Force hold

1. Enable `External EMS mode` & start sending the heartbeat as described above
2. Set `Charge/discharge command` (Register 13051 / Address 13050) to `0xCC` (Hold)

Alternativelly you can set the `Charge/discharge power` (Register 13052 / Address 13051) 0, that's what the 1komma5 Heartbeat is doing.

### Force discharging

1. Enable `External EMS mode` & start sending the heartbeat as described above
2. Set `Charge/discharge command` (Register 13051 / Address 13050) to `0xBB` (discharge)
3. Set `Charge/discharge power` (Register 13052 / Address 13051) to the amount in watt you want to allow for discharging
4. You can set `Min. SOC` (Register 13059 / Address 13058) to a value between 0 and 50% (The unit is 0.1%, so 204 are 20.4%). The Inverter will stop discharging if the SOC reaches or falls below this level.
5. You can set `Max. Discharging Power` (Register 33048 / Address 33047) to limit the discharging power (The unit is 0.01kw, so 432 are 4.32kw)
