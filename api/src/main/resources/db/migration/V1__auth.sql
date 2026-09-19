CREATE TABLE user_accounts (
  id BINARY(16) PRIMARY KEY,
  display_name VARCHAR(100) NOT NULL,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL
);
CREATE TABLE auth_codes (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  purpose VARCHAR(20) NOT NULL,
  code_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  attempts INT NOT NULL,
  consumed BOOLEAN NOT NULL,
  last_sent_at DATETIME(6) NOT NULL,
  window_start DATETIME(6) NOT NULL,
  sends INT NOT NULL,
  UNIQUE KEY uq_code_purpose (user_id, purpose),
  FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);
CREATE TABLE auth_sessions (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked BOOLEAN NOT NULL,
  FOREIGN KEY (user_id) REFERENCES user_accounts(id),
  INDEX ix_sessions_user (user_id)
);
CREATE TABLE refresh_tokens (
  token_hash VARCHAR(64) PRIMARY KEY,
  session_id BINARY(16) NOT NULL,
  used BOOLEAN NOT NULL,
  FOREIGN KEY (session_id) REFERENCES auth_sessions(id)
);
