CREATE TABLE tasks (
  id BINARY(16) PRIMARY KEY,
  owner_id BINARY(16) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description VARCHAR(5000),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  FOREIGN KEY (owner_id) REFERENCES user_accounts(id),
  INDEX ix_tasks_owner_created (owner_id, created_at, id),
  INDEX ix_tasks_owner_status (owner_id, status, created_at, id)
);
