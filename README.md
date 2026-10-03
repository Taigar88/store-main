# Store Application
The Store application keeps track of customers, orders and products in a database.

# Assumptions
This README assumes you're using a posix environment. It's possible to run this on Windows as well:
* Instead of `./gradlew` use `gradlew.bat`
* The syntax for creating the Docker container is different. You could also install PostgreSQL on bare metal if you prefer

# Prerequisites
This service assumes the presence of a postgresql 16.2 database server running on localhost:5433 (note the non-standard port)
It assumes a username and password `admin:admin` can be used.
It assumes there's already a database called `store`

You can start the PostgreSQL instance like this:
```shell
docker run -d \
  --name postgres \
  --restart always \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=store \
  -v postgres:/var/lib/postgresql/data \
  -p 5433:5432 \
  postgres:16.2 \
  postgres -c wal_level=logical
```

# Running the application
```shell
./gradlew bootRun
```

The application uses Liquibase to migrate the schema. Some sample data is provided. You can create more data by reading the documentation in utils/README.md

# Tests and formatting
```shell
./gradlew spotlessApply   # fix formatting
./gradlew build           # compile, format check and tests
```
The integration tests (`StoreApiIT`) start their own PostgreSQL with Testcontainers, so Docker must be running.
They also assert that the GET endpoints issue a fixed number of SQL statements regardless of row count.

# Data model
* An order has an ID, a description, the customer which made it, and one or more products.
* A customer has an ID, a name, and 0 or more orders.
* A product has an ID and a description. A product can appear in many orders (join table `order_product`).

# API
Three endpoints are provided; the full contract is in `OpenAPI.yaml`.

| Method | Path | Notes |
|---|---|---|
| GET | `/order` | All orders, with customer and products |
| GET | `/order/{id}` | One order, 404 if missing |
| POST | `/order` | Body: `description`, `customer.id`, `products[].id` (at least one). 400 if invalid, 404 if customer/product unknown |
| GET | `/customer` | All customers; optional `?name=` case-insensitive substring match |
| POST | `/customer` | Body: `name` |
| GET | `/products` | All products, each with the IDs of the orders containing it |
| GET | `/products/{id}` | One product, 404 if missing |
| POST | `/products` | Body: `description` |

The data model is circular, so to avoid serializer loops entities are mapped to DTOs which contain truncated versions of the dependent object (CustomerOrderDTO, OrderCustomerDTO, OrderProductDTO).

# Performance notes
The database is remote with high latency, so the cost of a request is dominated by the number of round trips.
* `GET /customer` and `GET /order` use entity graphs so each loads everything in one query (previously 1 + N).
* `GET /products` runs two queries in total: the products, and one query for every (product, order) pair.
* `spring.jpa.open-in-view=false`, so no connection is held during serialization and accidental lazy loads fail loudly.
* `hibernate.default_batch_fetch_size=100` batches any lazy load that slips through.
* Fixed-size, warm Hikari pool with `auto-commit=false` to avoid per-request connection setup and extra round trips.
* Indexes on `"order".customer_id` and `order_product.product_id`; the `order_product` primary key covers lookups by order.
* `LIKE '%term%'` customer search cannot use a B-tree index. For a large table add a `pg_trgm` GIN index on `lower(name)`.

# Decisions and assumptions
* Existing paths (`/order`, `/customer`) are kept; the OpenAPI spec is treated as the contract and only extended.
* Customer search is a case-insensitive substring match on the whole name.
* Write endpoints take request DTOs rather than JPA entities, so clients cannot set IDs or write nested objects.
* "An order contains 1 or more products" is enforced when creating an order. Orders that existed before products were introduced have none.
* The list endpoints are not paginated, to keep the response shape in the spec. Pagination would be the next step for large datasets.

# CI/CD
`.github/workflows/ci.yml` runs on every push and pull request:
1. `spotlessCheck` (formatting), compile, unit and integration tests
2. On `main`, builds the Docker image (`Dockerfile`, multi-stage, non-root) and pushes it to GitHub Container Registry

Run the image against your database with:
```shell
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5433/store \
  -e SPRING_DATASOURCE_USERNAME=admin -e SPRING_DATASOURCE_PASSWORD=admin \
  ghcr.io/<owner>/<repo>:latest
```
Further checks a production pipeline would add: dependency and image vulnerability scanning, and a smoke test against the built image.
