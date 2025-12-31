# PowerSquirrel Base Hub

This is the program to run in your basement. Its main tasks are:

* Collect and aggregate data from the connectors via MQTT
* Predict solar yield and consumption
* Command the devices like the inverters & heat pumps for the most cost effective electricity usage
* Serve the frontend via HTTP

It's supposed to run in docker-compose on a Raspberry Pi.
