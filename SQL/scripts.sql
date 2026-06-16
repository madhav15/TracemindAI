CREATE TABLE tracemind.job (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(255) UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    total_records INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tracemind.record (
    id BIGSERIAL PRIMARY KEY,
    record_id VARCHAR(255) NOT NULL UNIQUE,
    job_id VARCHAR(255) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    name VARCHAR(255),
    mobile VARCHAR(20),
    email VARCHAR(255),
    communication_preference CHAR(1) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_job_record_job
        FOREIGN KEY (job_id)
        REFERENCES tracemind.job(job_id)
);


CREATE INDEX idx_record_job_id
ON tracemind.record(job_id);

CREATE INDEX idx_record_member_id
ON tracemind.record(member_id);

CREATE INDEX idx_record_status
ON tracemind.record(status);

CREATE INDEX idx_job_status
ON tracemind.job(status);



-- ######################
CREATE TABLE pre_processor_tracking (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    processing_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pre_processor_job_id
    ON pre_processor_tracking(job_id);

CREATE INDEX idx_pre_processor_record_id
    ON pre_processor_tracking(record_id);

CREATE INDEX idx_pre_processor_member_id
    ON pre_processor_tracking(member_id);

CREATE INDEX idx_pre_processor_status
    ON pre_processor_tracking(status);


-- #################

CREATE TABLE email_tracking (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_job_id
ON email_tracking(job_id);

CREATE INDEX idx_email_record_id
ON email_tracking(record_id);

CREATE INDEX idx_email_member_id
ON email_tracking(member_id);

CREATE INDEX idx_email_email
ON email_tracking(email);




CREATE TABLE dlt_events (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    job_id VARCHAR(100),
    record_id VARCHAR(100),
    member_id VARCHAR(100),
    error_type VARCHAR(50) NOT NULL,
    error_message TEXT,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- #########################

CREATE TABLE print_job (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);



-- ################################

CREATE TABLE archival_job (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


--
-- BEGIN;
--
-- TRUNCATE TABLE
--     archival_job,
--     print_job,
--     email_tracking,
--     pre_processor_tracking,
--     dlt_events,
--     record,
--     job
-- RESTART IDENTITY CASCADE;
--
-- COMMIT;

-- ########################################################

-- Vector DB

-- ########################################################

CREATE EXTENSION vector;

SELECT extname
FROM pg_extension;

CREATE TABLE knowledge_document (
    id BIGSERIAL PRIMARY KEY,

    document_name VARCHAR(255),

    chunk_id INTEGER,

    content TEXT,

    embedding VECTOR(1536),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX knowledge_embedding_idx
ON knowledge_document
USING ivfflat (embedding vector_cosine_ops);
