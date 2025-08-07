# Spring Profiles Configuration Guide

This identity service uses Spring Profiles to manage different configurations for various environments. The infrastructure automatically injects secrets and configuration via environment variables.

## Available Profiles

### 1. **default** (Development)
- **Purpose**: Local development with minimal dependencies
- **Database**: H2 in-memory
- **Redis**: Optional (localhost)
- **Kafka**: Optional (localhost)
- **Security**: Simplified (bypass Auth0)
- **Usage**: `./mvnw spring-boot:run` or `SPRING_PROFILES_ACTIVE=default`

### 2. **production**
- **Purpose**: Production deployment with full external services
- **Database**: PostgreSQL (credentials injected)
- **Redis**: Cluster mode (credentials injected)
- **Kafka**: SASL_SSL secured (credentials injected)
- **Security**: Full Auth0 integration
- **Usage**: Set by infrastructure via `SPRING_PROFILES_ACTIVE=production`

### 3. **test**
- **Purpose**: Automated testing in CI/CD
- **Database**: H2 in-memory (PostgreSQL mode)
- **Redis**: Disabled or embedded
- **Kafka**: Embedded or mocked
- **Security**: Mocked
- **Usage**: `./mvnw test` or `SPRING_PROFILES_ACTIVE=test`

## Environment Variables

### Database Configuration
```bash
# Production - Injected by Infrastructure
DATABASE_URL=jdbc:postgresql://host:port/database
DATABASE_USER=identity_user
DATABASE_PASSWORD=<injected-secret>

# Optional tuning
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=10
DB_CONNECTION_TIMEOUT=30000
```

### Redis Configuration
```bash
# Production - Injected by Infrastructure
REDIS_HOST=redis-master.namespace.svc.cluster.local
REDIS_PORT=6379
REDIS_PASSWORD=<injected-secret>
REDIS_DATABASE=0
REDIS_SSL_ENABLED=true

# Optional tuning
REDIS_TIMEOUT=2000ms
REDIS_CONNECT_TIMEOUT=10s
REDIS_POOL_MAX_ACTIVE=8
```

### Kafka Configuration
```bash
# Production - Injected by Infrastructure
KAFKA_BOOTSTRAP_SERVERS=broker1:9092,broker2:9092
KAFKA_USERNAME=identity-service
KAFKA_PASSWORD=<injected-secret>
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN

# Consumer configuration
KAFKA_CONSUMER_GROUP_ID=identity-service
KAFKA_AUTO_OFFSET_RESET=earliest
```

### Auth0 Configuration
```bash
# Production - Injected by Infrastructure
AUTH0_DOMAIN=your-tenant.auth0.com
AUTH0_CLIENT_ID=<injected-secret>
AUTH0_CLIENT_SECRET=<injected-secret>
AUTH0_AUDIENCE=https://your-api.com
AUTH0_CONNECTION=Username-Password-Authentication
```

### FHIR Server Configuration
```bash
# Production - Injected by Infrastructure
FHIR_SERVER_URL=https://fhir.healthcare.com/r4
FHIR_USERNAME=identity-service
FHIR_PASSWORD=<injected-secret>
FHIR_ENABLED=true
```

## Kubernetes Deployment

### Secret Creation by Infrastructure

The infrastructure (Crossplane/Terraform) creates secrets:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: identity-service-secrets
  namespace: healthcare
stringData:
  DATABASE_URL: "postgresql://..."
  DATABASE_USER: "identity_user"
  DATABASE_PASSWORD: "<generated>"
  REDIS_PASSWORD: "<generated>"
  KAFKA_PASSWORD: "<generated>"
  AUTH0_CLIENT_SECRET: "<from-vault>"
```

### Deployment Configuration

```yaml
spec:
  containers:
  - name: identity-service
    envFrom:
    - configMapRef:
        name: identity-service-config
    - secretRef:
        name: identity-service-secrets
    env:
    - name: SPRING_PROFILES_ACTIVE
      value: "production"
```

## Local Development

### Running with Default Profile
```bash
# Uses H2 in-memory database
./mvnw spring-boot:run
```

### Running with External Services
```bash
# Start dependencies
docker-compose up -d postgres redis kafka

# Run with environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/identity_db
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
export REDIS_HOST=localhost
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

./mvnw spring-boot:run --spring.profiles.active=production
```

### Using Docker Compose
```yaml
version: '3.8'
services:
  identity-service:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: production
      DATABASE_URL: jdbc:postgresql://postgres:5432/identity_db
      DATABASE_USER: postgres
      DATABASE_PASSWORD: postgres
      REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - postgres
      - redis
      - kafka
```

## Testing

### Unit Tests
```bash
# Uses test profile automatically
./mvnw test
```

### Integration Tests
```bash
# With real dependencies
SPRING_PROFILES_ACTIVE=test-integration ./mvnw test
```

### Security Tests
```bash
# With security enabled
SPRING_PROFILES_ACTIVE=test-security ./mvnw test
```

## Profile Hierarchy

```
application.yml (base configuration)
├── application-default.yml (development overrides)
├── application-production.yml (production overrides)
├── application-test.yml (test overrides)
│   ├── application-test-integration.yml
│   └── application-test-security.yml
```

## Monitoring & Observability

### Health Endpoints
- `/actuator/health` - Overall health
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe
- `/actuator/prometheus` - Prometheus metrics

### Environment Info
```bash
# Check active profile
curl http://localhost:8080/actuator/env | jq '.activeProfiles'

# Check configuration
curl http://localhost:8080/actuator/configprops
```

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check `DATABASE_URL` format
   - Verify network connectivity
   - Check credentials in secrets

2. **Redis Connection Failed**
   - Verify `REDIS_HOST` and `REDIS_PORT`
   - Check password if secured
   - Ensure Redis is running

3. **Kafka Connection Failed**
   - Check `KAFKA_BOOTSTRAP_SERVERS`
   - Verify SASL credentials
   - Check security protocol

4. **Auth0 Authentication Failed**
   - Verify domain and credentials
   - Check audience configuration
   - Ensure callback URLs are configured

### Debug Mode
```bash
# Enable debug logging
export LOG_LEVEL=DEBUG
export APP_LOG_LEVEL=DEBUG

# Or in application.yml
logging:
  level:
    root: DEBUG
    com.healthcare.identity: TRACE
```

## Best Practices

1. **Never commit secrets** - Use environment variables
2. **Use appropriate profiles** - Don't use `default` in production
3. **Validate configuration** - Check `/actuator/health` after deployment
4. **Monitor resources** - Watch connection pools and memory
5. **Rotate secrets regularly** - Update via infrastructure
6. **Test profile switching** - Ensure smooth transitions

## Cloud-Specific Configuration

### Azure
```bash
# Azure KeyVault integration
AZURE_KEYVAULT_ENABLED=true
AZURE_KEYVAULT_URI=https://your-kv.vault.azure.net/
AZURE_CLIENT_ID=<service-principal-id>
AZURE_CLIENT_SECRET=<service-principal-secret>
```

### AWS
```bash
# AWS Secrets Manager
AWS_SECRETS_ENABLED=true
AWS_SECRETS_REGION=us-east-1
AWS_SECRETS_PREFIX=identity-service/
```

### GCP
```bash
# Google Secret Manager
GCP_SECRETS_ENABLED=true
GCP_PROJECT_ID=your-project
GCP_SECRETS_PREFIX=identity-service-
```