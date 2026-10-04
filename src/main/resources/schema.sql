CREATE TABLE IF NOT EXISTS employees (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(50)    NOT NULL,
    last_name     VARCHAR(50)    NOT NULL,
    email         VARCHAR(120)   NOT NULL UNIQUE,
    phone         VARCHAR(15),
    department    VARCHAR(50)    NOT NULL,
    designation   VARCHAR(80)    NOT NULL,
    salary        DECIMAL(12, 2) NOT NULL,
    joining_date  DATE           NOT NULL,
    active        BOOLEAN        NOT NULL DEFAULT TRUE
);
