DELETE FROM fx_rate;
DELETE FROM account;

INSERT INTO account (id, name, currency, balance) VALUES (1, 'Alice', 'USD', 1000);
INSERT INTO account (id, name, currency, balance) VALUES (2, 'Bob', 'JPN', 500);

INSERT INTO fx_rate (id, from_currency, to_currency, rate) VALUES (1, 'USD', 'AUD', 2.0);