CREATE TABLE follow_up_tasks (
    id             UUID PRIMARY KEY,
    opportunity_id UUID REFERENCES opportunities (id),
    customer_id    UUID REFERENCES customers (id),
    due_date       DATE NOT NULL,
    owner          UUID NOT NULL,
    alert_channel  VARCHAR(10) NOT NULL CHECK (alert_channel IN ('APP', 'EMAIL')),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL,
    modified_at    TIMESTAMP NOT NULL,
    CONSTRAINT chk_follow_up_tasks_linked_target CHECK (opportunity_id IS NOT NULL OR customer_id IS NOT NULL)
);

CREATE INDEX idx_follow_up_tasks_owner ON follow_up_tasks (owner);
CREATE INDEX idx_follow_up_tasks_due_date ON follow_up_tasks (due_date);
CREATE INDEX idx_follow_up_tasks_opportunity_id ON follow_up_tasks (opportunity_id);
CREATE INDEX idx_follow_up_tasks_customer_id ON follow_up_tasks (customer_id);
