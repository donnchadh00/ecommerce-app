INSERT INTO product (name, description, price, quantity)
SELECT * FROM (
  VALUES
    ('Mechanical Keyboard', 'Hot-swappable 75% keyboard with tactile switches', 129.99, 25),
    ('Ultrawide Monitor', '34-inch curved monitor for productivity and gaming', 499.00, 12),
    ('USB-C Dock', 'Dual-display dock with power delivery and Ethernet', 189.50, 18),
    ('Noise-Cancelling Headphones', 'Wireless over-ear headphones with ANC', 249.90, 20),
    ('Ergonomic Mouse', 'Vertical mouse designed for long work sessions', 79.00, 30),
    ('Laptop Stand', 'Adjustable aluminum stand for desk setups', 59.99, 40)
) AS seed(name, description, price, quantity)
WHERE NOT EXISTS (
  SELECT 1 FROM product p WHERE p.name = seed.name
);
