
--
-- Create column `vanished` in oaiRecordSets which represents when
-- the record vanished from the set. This is linked to gone
-- for the transition period.
--

ALTER TABLE oairecordsets ADD COLUMN vanished TIMESTAMP WITH TIME ZONE;
UPDATE oairecordsets SET vanished=CURRENT_TIMESTAMP WHERE gone;
ALTER TABLE oairecordsets ALTER COLUMN gone DROP NOT NULL;
ALTER TABLE oairecordsets ALTER COLUMN gone DROP DEFAULT;

CREATE OR REPLACE FUNCTION ensure_vanished_and_gone_update() RETURNS trigger AS $$
    BEGIN
        IF NEW.gone IS NULL THEN
            NEW.gone = NEW.vanished IS NOT NULL;
        ELSE
            IF NEW.gone = OLD.gone AND NEW.vanished <> OLD.vanished THEN
                NEW.gone = NEW.vanished IS NOT NULL;
            ELSEIF NEW.gone <> OLD.gone AND NEW.vanished = OLD.vanished THEN
                IF NEW.gone THEN
                    NEW.vanished = CURRENT_TIMESTAMP;
                ELSE
                    NEW.vanished = NULL;
                END IF;
            END IF;

        END IF;
        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER oairecordsets_vanished_update BEFORE UPDATE ON oairecordsets
    FOR EACH ROW EXECUTE FUNCTION ensure_vanished_and_gone_update();

CREATE OR REPLACE FUNCTION ensure_vanished_and_gone_insert() RETURNS trigger AS $$
    BEGIN
        IF NEW.gone IS NULL THEN
            NEW.gone = NEW.vanished IS NOT NULL;
        ELSEIF NEW.vanished IS NULL AND NEW.gone THEN
            NEW.vanished = CURRENT_TIMESTAMP;
        END IF;
        RETURN NEW;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER oairecordsets_vanished_insert BEFORE INSERT ON oairecordsets
    FOR EACH ROW EXECUTE FUNCTION ensure_vanished_and_gone_insert();
