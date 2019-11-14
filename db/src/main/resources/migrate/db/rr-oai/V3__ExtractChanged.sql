CREATE TABLE t AS  SELECT REGEXP_REPLACE(pid, '^([^:]*):', '\1-') AS pid, changed, setspec, gone FROM oairecords JOIN oairecordsets USING(pid);

DROP TABLE oairecordsets;
ALTER TABLE t RENAME TO oairecordsets;
UPDATE oairecords SET pid=REGEXP_REPLACE(pid, '^([^:]*):', '\1-');

ALTER TABLE oairecordsets ADD CONSTRAINT oairecordsets_pk PRIMARY KEY (pid, setSpec);
ALTER TABLE oairecordsets ADD CONSTRAINT oairecordsets_pid_fk FOREIGN KEY (pid) REFERENCES oairecords (pid);
ALTER TABLE oairecordsets ADD CONSTRAINT oairecordsets_setspec_fk FOREIGN KEY (setSpec) REFERENCES oaisets (setSpec);
ALTER TABLE oairecordsets ALTER COLUMN changed SET NOT NULL;
ALTER TABLE oairecordsets ALTER COLUMN changed SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE oairecordsets ALTER COLUMN gone SET NOT NULL;
ALTER TABLE oairecordsets ALTER COLUMN gone SET DEFAULT FALSE;

ALTER TABLE oairecords DROP COLUMN changed;
DROP TABLE version;