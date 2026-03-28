CREATE UNIQUE INDEX IF NOT EXISTS ux_inventory_product_id
  ON inventory(product_id);

INSERT INTO inventory (product_id, quantity)
SELECT seed.product_id, seed.quantity
FROM (
  VALUES
    (1, 1000),
    (2, 1000),
    (3, 1000),
    (4, 1000),
    (5, 1000),
    (6, 1000)
) AS seed(product_id, quantity)
WHERE NOT EXISTS (
  SELECT 1
  FROM inventory i
  WHERE i.product_id = seed.product_id
);
