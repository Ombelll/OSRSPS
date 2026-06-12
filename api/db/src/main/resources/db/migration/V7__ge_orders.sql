CREATE TABLE npc_ge_orders (
    id INTEGER PRIMARY KEY,
    owner TEXT NOT NULL,
    side TEXT NOT NULL CHECK (side IN ('Buy', 'Sell')),
    obj_id INTEGER NOT NULL,
    obj_name TEXT NOT NULL,
    price INTEGER NOT NULL,
    remaining INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_npc_ge_orders_owner ON npc_ge_orders(owner);
CREATE INDEX idx_npc_ge_orders_match ON npc_ge_orders(obj_id, side, price);

CREATE TABLE npc_ge_collect (
    owner TEXT PRIMARY KEY,
    coins INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE npc_ge_collect_items (
    owner TEXT NOT NULL,
    obj_id INTEGER NOT NULL,
    obj_name TEXT NOT NULL,
    count INTEGER NOT NULL,
    PRIMARY KEY (owner, obj_id),
    FOREIGN KEY (owner) REFERENCES npc_ge_collect(owner) ON DELETE CASCADE
);
