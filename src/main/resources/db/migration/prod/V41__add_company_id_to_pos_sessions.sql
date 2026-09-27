ALTER TABLE pos_sessions ADD COLUMN company_id UUID REFERENCES companies (id);

CREATE INDEX idx_pos_sessions_company_id ON pos_sessions (company_id);
