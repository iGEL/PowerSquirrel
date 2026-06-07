> [!WARNING]
> PowerSquirrel is under active development and is currently still incomplete.

## PowerSquirrel

![](./docs/sparky.svg)

PowerSquirrel (posq) is an intelligent power management system designed for private households and small offices. Its primary goal is to optimize electricity usage and storage, ensuring maximum comfort while minimizing costs and reliance on carbon-intensive energy sources.

The age of fixed electricity pricing is coming slowly to an end. Many renewable energy sources are more volatile than traditional sources, increasing the challenge to match supply and demand. Dynamic pricing encourages shifting demand to times of high supply when power is sold cheaper.
PowerSquirrel leverages household batteries and photovoltaic (PV) systems to optimize energy consumption in this new paradigm, offering smarter energy management that aligns with both financial and environmental goals.

## Key features

1. Energy Consumption Prediction  
   Forecast household or office electricity usage for the next 24 to 48 hours.
2. Solar Yield Prediction  
   Predict energy generation from photovoltaic systems based on weather forecasts and historical data
3. Smart Battery Management
   * Charge household batteries using grid electricity when solar yield is insufficient to meet predicted consumption.
   * Optimize charging times to benefit from dynamic electricity pricing.
4. Grid-supportive feed-in of PV electricity:  
   If a predicted surplus cannot be consumed or stored, shift the feed in-times to better match the grid's demand (periods with higher prices) provided the battery can be still fully charged with the remaining sunlight of the day.
5. Load Shifting for Cost Efficiency  
   Schedule and manage energy-intensive appliances to operate during periods of low-cost or high solar output.
   Examples include:
   * Electric Vehicles (EVs): Ensure vehicles are fully charged by a specified time, prioritizing cheaper energy availability.
   * Heat Pumps and Air Conditioning: Precharge thermal buffers during periods of low-cost or abundant energy.
   * Fridges and Freezers: Temporarily lower temperatures during low-cost periods, reducing energy use during higher-priced times
