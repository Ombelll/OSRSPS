CREATE TABLE social_friends (
    owner TEXT NOT NULL,
    friend TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (owner, friend)
);

CREATE TABLE social_clans (
    owner TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE social_clan_members (
    owner TEXT NOT NULL,
    member TEXT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (owner, member),
    FOREIGN KEY (owner) REFERENCES social_clans(owner) ON DELETE CASCADE
);
