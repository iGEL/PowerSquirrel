# PowerSquirrel Base Hub

This is the program to run in your basement. Its main tasks are:

* Collect and aggregate data from the connectors via MQTT
* Predict solar yield and consumption
* Command the devices like the inverters & heat pumps for the most cost effective electricity usage
* Serve the frontend via HTTP

It's supposed to run in docker-compose on a Raspberry Pi.

## Database

The hub uses SQLite as its local relational database. The hub is an
unattended appliance running on customer hardware, so the local database should
avoid extra services, credentials, network dependencies, and manual upgrade
steps where possible. SQLite keeps the local deployment to an application plus
a database file, while the cloud can still use hosted Postgres.

Some data is authoritative on the hub and some data is authoritative in the
cloud. Sync code must model that explicitly instead of relying on database
replication semantics. Synced objects should carry enough metadata to determine
their authority, version, update time, deletion state, and sync state.

Use these storage conventions for SQLite columns:

* UUIDs are stored as canonical text.
* Timestamps are stored as Unix epoch seconds in UTC using integer columns.
* Durations are stored as integers, usually seconds.
* Power and energy values are stored as integers.
* Money is stored as two columns: integer cents and text currency.
* Percentages are stored as integers basis points (1% = 100 bp)

Column names should include unit suffixes so units remain visible in queries
and schema reviews. Examples:

* `created_at_s`
* `duration_s`
* `power_w`
* `energy_wh`
* `energy_ws`
* `price_cents`
* `currency`
* `percent_bp`

SQL keywords should be written lower case, eg. `create table` instead of
`CREATE TABLE`.

Telemetry tables should be indexed for common time range filters. For example,
15-minute measurements should have an index or primary key that starts with the
series identity, such as device and metric, followed by the timestamp column.

## Build instructions

docker buildx build --platform linux/arm64/v8 -t posq/hub:arm64 --load .
docker save posq/hub -o hub.ta
scp ...
ssh ...
docker load -i hub.tar
