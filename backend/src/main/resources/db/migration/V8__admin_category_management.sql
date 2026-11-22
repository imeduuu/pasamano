-- RF-CAT-02 y RF-ADM-05: auditoría de cambios administrativos.
CREATE TABLE audit_log (
    id uuid PRIMARY KEY,
    admin_id uuid NOT NULL,
    action varchar(80) NOT NULL,
    resource varchar(120) NOT NULL,
    reason varchar(500),
    occurred_at timestamptz NOT NULL
);

CREATE INDEX ix_audit_log_admin_date ON audit_log(admin_id, occurred_at DESC);
