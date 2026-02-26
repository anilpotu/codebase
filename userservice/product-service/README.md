# Product Service

A business microservice demonstrating public endpoints, Redis caching, and mixed security (public GET, authenticated POST/PUT/DELETE).

## Features

- **Public GET Endpoints**: Product listing and search available without authentication
- **Authenticated Mutations**: POST/PUT/DELETE operations require ADMIN role
- **Redis Caching**: Caching for improved performance
- **H2 Database**: In-memory database with sample data
- **Service Discovery**: Eureka client for service registration
- **Spring Security**: OAuth2 Resource Server with JWT

## Port

- **8083**

## Endpoints

### Public Endpoints (No Authentication Required)

- `GET /products` - List all products
- `GET /products/{id}` - Get product by ID
- `GET /products/search?name={name}` - Search products by name
- `GET /products/category/{category}` - Get products by category

### Protected Endpoints (ADMIN Role Required)

- `POST /products` - Create new product
- `PUT /products/{id}` - Update product
- `DELETE /products/{id}` - Delete product

## Configuration

### Database

- H2 in-memory database
- Console available at: http://localhost:8083/h2-console
- JDBC URL: `jdbc:h2:mem:productdb`
- Username: `sa`
- Password: (empty)

### Redis

- Host: localhost
- Port: 6379
- Cache TTL: 10 minutes

### Security

- OAuth2 Resource Server
- JWT token validation
- Realm roles extraction from Keycloak

## Sample Data

The service includes 17 sample products across 6 categories:
- Electronics
- Books
- Clothing
- Home & Garden
- Sports
- Toys

## Running the Service

```bash
mvn spring-boot:run
```

## Testing

### Get All Products (Public)
```bash
curl http://localhost:8083/products
```

### Search Products (Public)
```bash
curl http://localhost:8083/products/search?name=laptop
```

### Create Product (Requires Admin Token)
```bash
curl -X POST http://localhost:8083/products \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Product",
    "description": "Product description",
    "price": 99.99,
    "stockQuantity": 100,
    "category": "Electronics"
  }'
```

## Dependencies

- Spring Boot Web
- Spring Security
- Spring Data JPA
- Spring Data Redis
- Spring Cloud Config
- Spring Cloud Eureka
- H2 Database
- Lombok
- Common Library
