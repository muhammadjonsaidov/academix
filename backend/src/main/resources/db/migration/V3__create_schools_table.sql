-- academix_backend_tdd.md §4.1
CREATE TABLE schools (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    region VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    total_classes INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    subscribed_at TIMESTAMP,
    subscription_ends_at TIMESTAMP,
    admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    -- AI cost/budget, see academix_backend_tdd.md §7.4
    monthly_ai_call_limit INT NOT NULL DEFAULT 5000,
    current_month_ai_usage INT NOT NULL DEFAULT 0
);
