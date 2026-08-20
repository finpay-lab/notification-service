-- FP-19/48/59: notification schema. No shared DB (Rule 1).
CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(36) PRIMARY KEY,
    event_id        VARCHAR(72) NOT NULL,
    customer_id     VARCHAR(36) NOT NULL,
    channel         VARCHAR(8)  NOT NULL,
    subject         VARCHAR(512),
    body            TEXT,
    state           VARCHAR(16) NOT NULL DEFAULT 'RECEIVED',
    last_error      VARCHAR(512),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_notif_event ON notifications (event_id);
