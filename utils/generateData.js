const { faker } = require('@faker-js/faker');

const N = 100; // Number of customers
const M = 10_000; // Number of orders
const P = 50; // Number of products
const MAX_PRODUCTS_PER_ORDER = 3;

// Double single quotes so names like O'Connor do not break the SQL
const esc = (s) => s.replace(/'/g, "''");

// Generate customers
for (let i = 1; i <= N; i++) {
    console.log(`INSERT INTO customer (id, name) VALUES (${i}, '${esc(faker.name.fullName())}');`);
}

// Generate products
for (let i = 1; i <= P; i++) {
    console.log(`INSERT INTO product (id, description) VALUES (${i}, '${esc(faker.commerce.productName())}');`);
}

// Generate orders, each with 1 to MAX_PRODUCTS_PER_ORDER distinct products
for (let i = 1; i <= M; i++) {
    const customerId = Math.ceil(Math.random() * N);
    console.log(
        `INSERT INTO "order" (id, description, customer_id) VALUES (${i}, '${esc(faker.commerce.productName())}', ${customerId});`
    );

    const productCount = 1 + Math.floor(Math.random() * MAX_PRODUCTS_PER_ORDER);
    const productIds = new Set();
    while (productIds.size < productCount) {
        productIds.add(Math.ceil(Math.random() * P));
    }
    for (const productId of productIds) {
        console.log(`INSERT INTO order_product (order_id, product_id) VALUES (${i}, ${productId});`);
    }
}

// The inserts above use explicit ids, so move each sequence past them.
// Without this, the next POST would try id 1 and fail with a duplicate key error.
console.log(`SELECT setval(pg_get_serial_sequence('customer', 'id'), (SELECT MAX(id) FROM customer));`);
console.log(`SELECT setval(pg_get_serial_sequence('product', 'id'), (SELECT MAX(id) FROM product));`);
console.log(`SELECT setval(pg_get_serial_sequence('"order"', 'id'), (SELECT MAX(id) FROM "order"));`);