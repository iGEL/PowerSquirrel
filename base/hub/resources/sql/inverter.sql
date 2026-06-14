-- :name upsert-inverter-agg15! :! :n
-- :doc Upsert one 15-minute inverter aggregate row, keyed by (inverter_sn, started_at_s).
insert into inverter_agg15
(inverter_sn, started_at_s, pv_ws, loads_ws, load_produced_ws,
 grid_import_ws, grid_export_ws, battery_charged_ws, battery_discharged_ws,
 battery_soc_bp, complete, updated_at_s)
values
(:inverter-sn, :started-at-s, :pv-ws, :loads-ws, :load-produced-ws,
 :grid-import-ws, :grid-export-ws, :battery-charged-ws, :battery-discharged-ws,
 :battery-soc-bp, :complete, :updated-at-s)
on conflict (inverter_sn, started_at_s) do update set
  pv_ws = excluded.pv_ws,
  loads_ws = excluded.loads_ws,
  load_produced_ws = excluded.load_produced_ws,
  grid_import_ws = excluded.grid_import_ws,
  grid_export_ws = excluded.grid_export_ws,
  battery_charged_ws = excluded.battery_charged_ws,
  battery_discharged_ws = excluded.battery_discharged_ws,
  battery_soc_bp = excluded.battery_soc_bp,
  complete = excluded.complete,
  updated_at_s = excluded.updated_at_s
