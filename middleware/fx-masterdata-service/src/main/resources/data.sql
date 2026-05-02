-- Currency seed (idempotent: skipped if code already present).
-- ISO 4217 codes derived from the requested countries:
--   USD, JPY, GBP (UK), INR, SGD (Singapore), CHF (Switzerland),
--   NZD (New Zealand), AUD (Australia), EUR (Europe / Netherlands / Belgium)
INSERT INTO currency (code, name, country, active)
SELECT 'USD', 'US Dollar', 'United States', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'USD');

INSERT INTO currency (code, name, country, active)
SELECT 'EUR', 'Euro', 'Eurozone', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'EUR');

INSERT INTO currency (code, name, country, active)
SELECT 'GBP', 'Pound Sterling', 'United Kingdom', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'GBP');

INSERT INTO currency (code, name, country, active)
SELECT 'JPY', 'Japanese Yen', 'Japan', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'JPY');

INSERT INTO currency (code, name, country, active)
SELECT 'INR', 'Indian Rupee', 'India', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'INR');

INSERT INTO currency (code, name, country, active)
SELECT 'SGD', 'Singapore Dollar', 'Singapore', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'SGD');

INSERT INTO currency (code, name, country, active)
SELECT 'CHF', 'Swiss Franc', 'Switzerland', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'CHF');

INSERT INTO currency (code, name, country, active)
SELECT 'NZD', 'New Zealand Dollar', 'New Zealand', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'NZD');

INSERT INTO currency (code, name, country, active)
SELECT 'AUD', 'Australian Dollar', 'Australia', TRUE
WHERE NOT EXISTS (SELECT 1 FROM currency WHERE code = 'AUD');
