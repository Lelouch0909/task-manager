CREATE TABLE notifications (
  id BINARY(16) PRIMARY KEY,
  owner_id BINARY(16) NOT NULL,
  task_id BINARY(16) NOT NULL,
  kind VARCHAR(30) NOT NULL,
  message VARCHAR(300) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  read_at DATETIME(6),
  FOREIGN KEY (owner_id) REFERENCES user_accounts(id),
  INDEX ix_notifications_owner_created (owner_id, created_at, id),
  INDEX ix_notifications_owner_unread (owner_id, read_at)
);
