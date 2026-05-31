create table inverter_agg15 (
    inverter_sn text not null,
    started_at_s integer not null,

    pv_ws integer not null default 0,
    loads_ws integer not null default 0,
    load_produced_ws integer not null default 0,
    grid_import_ws integer not null default 0,
    grid_export_ws integer not null default 0,
    battery_charged_ws integer not null default 0,
    battery_discharged_ws integer not null default 0,

    battery_soc_bp integer not null,
    complete integer not null default 0 check (complete in (0, 1)),

    updated_at_s integer not null,

    primary key (inverter_sn, started_at_s)
) strict;

create index inverter_agg15_started_at_idx
on inverter_agg15 (started_at_s);

create index inverter_agg15_complete_started_at_idx
on inverter_agg15 (complete, started_at_s);
