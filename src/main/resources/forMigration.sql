CREATE TYPE birthday_status AS ENUM ('PLANNED', 'CELEBRATED', 'MISSED');


CREATE TABLE BIRTHDAYS (
                           uuid UUID PRIMARY KEY,
                           celebrant_uuid UUID UNIQUE,
                           birth_date DATE NOT NULL,
                           status birthday_status NOT NULL,

                           CONSTRAINT fk_celebrant FOREIGN KEY (celebrant_uuid) REFERENCES CELEBRANTS(uuid) ON DELETE CASCADE
);