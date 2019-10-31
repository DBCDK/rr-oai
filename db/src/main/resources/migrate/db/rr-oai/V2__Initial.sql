
-- Typo
UPDATE oaisets SET setName='Nationalbibliografi', description='Materialer udgivet i Danmark - Offentligt tilgængeligt.' WHERE setSpec = 'nat';

-- Faster default
ALTER TABLE oairecords ALTER COLUMN changed SET DEFAULT current_timestamp;

-- Same timestamp issue when harvesting
CREATE INDEX oairecords_changedpid ON oairecords(changed, pid);



