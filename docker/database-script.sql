CREATE TABLE asset_state (
    asset_id                          text PRIMARY KEY,

    -- SPEC §4.1 counters, lifetime scope (§4.4 baselines + this run)
    lifetime_engine_hours             numeric NOT NULL,
    lifetime_total_working_hours      numeric NOT NULL,
    lifetime_transport_hours          numeric NOT NULL,
    lifetime_idle_hours               numeric NOT NULL,
    lifetime_loaded_hours             numeric NOT NULL,
    lifetime_high_load_hours          numeric NOT NULL,
    lifetime_temp_excursion_hours     numeric NOT NULL,
    lifetime_vibration_alert_hours    numeric NOT NULL,

    -- SPEC §4.1 counters, rolling 24 h scope (§4.3)
    rolling_24h_engine_hours          numeric NOT NULL,
    rolling_24h_total_working_hours   numeric NOT NULL,
    rolling_24h_transport_hours       numeric NOT NULL,
    rolling_24h_idle_hours            numeric NOT NULL,
    rolling_24h_loaded_hours          numeric NOT NULL,
    rolling_24h_high_load_hours       numeric NOT NULL,
    rolling_24h_temp_excursion_hours  numeric NOT NULL,
    rolling_24h_vibration_alert_hours numeric NOT NULL,

    -- SPEC §4.5
    window_coverage_hours             numeric NOT NULL,
    utilization_percent               numeric NULL,
    productive_share_percent          numeric NULL,

    -- SPEC §5.2, §5.3, §5.4, §5.5
    status                            text NOT NULL,
    live_condition                    text NOT NULL,
    recommendation                    text NOT NULL,
    priority_rank                     integer NOT NULL,

    -- SPEC §5.1 flags; NULL severity means the flag is inactive
    heavy_recent_use_severity         text NULL,
    heavy_recent_use_value            numeric NULL,
    heavy_recent_use_threshold        numeric NULL,
    thermal_load_severity             text NULL,
    thermal_load_value                numeric NULL,
    thermal_load_threshold            numeric NULL,
    mechanical_attention_severity     text NULL,
    mechanical_attention_value        numeric NULL,
    mechanical_attention_threshold    numeric NULL,
    high_cumulative_load_severity     text NULL,
    high_cumulative_load_value        numeric NULL,
    high_cumulative_load_threshold    numeric NULL,

    -- SPEC §3.1 most recent message; all NULL until the asset reports
    latest_timestamp                  timestamptz NULL,
    latest_pto_engaged                boolean NULL,
    latest_ground_speed_kmh           numeric NULL,
    latest_load_factor                numeric NULL,
    latest_temperature_c              numeric NULL,
    latest_vibration_level            text NULL,
    latest_working_status             text NULL,

    updated_at                        timestamptz NOT NULL
);

CREATE TABLE hourly_aggregate (
    asset_id              text NOT NULL,
    hour_start            timestamptz NOT NULL,
    engine_hours          numeric NOT NULL,
    total_working_hours   numeric NOT NULL,
    transport_hours       numeric NOT NULL,
    idle_hours            numeric NOT NULL,
    loaded_hours          numeric NOT NULL,
    high_load_hours       numeric NOT NULL,
    temp_excursion_hours  numeric NOT NULL,
    vibration_alert_hours numeric NOT NULL,
    PRIMARY KEY (asset_id, hour_start)
);

CREATE INDEX idx_hourly_aggregate_hour_start ON hourly_aggregate (hour_start);
