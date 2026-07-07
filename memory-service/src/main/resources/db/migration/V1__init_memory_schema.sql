CREATE TABLE conversations (
    id uuid PRIMARY KEY,
    user_id varchar(128) NOT NULL,
    agent_id varchar(128) NOT NULL,
    title varchar(200),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_conversations_user_agent_updated
    ON conversations (user_id, agent_id, updated_at DESC);

CREATE TABLE conversation_messages (
    id uuid PRIMARY KEY,
    conversation_id uuid NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role varchar(32) NOT NULL,
    content text NOT NULL,
    model varchar(128),
    tool_name varchar(128),
    correlation_id varchar(128),
    prompt_tokens integer,
    completion_tokens integer,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_conversation_messages_role
        CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT', 'TOOL')),
    CONSTRAINT chk_conversation_messages_prompt_tokens
        CHECK (prompt_tokens IS NULL OR prompt_tokens >= 0),
    CONSTRAINT chk_conversation_messages_completion_tokens
        CHECK (completion_tokens IS NULL OR completion_tokens >= 0)
);

CREATE INDEX idx_conversation_messages_conversation_created
    ON conversation_messages (conversation_id, created_at DESC, id DESC);

CREATE INDEX idx_conversation_messages_correlation_id
    ON conversation_messages (correlation_id)
    WHERE correlation_id IS NOT NULL;

