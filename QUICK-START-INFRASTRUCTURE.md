# Quick Start Guide - Infrastructure Integration

## 1-Minute Overview

The Identity Service Template generates Spring Boot services that need:
- **PostgreSQL** database
- **Redis** for sessions
- **Kafka** for events
- **Auth0** for authentication
- **Secrets** injected via environment variables

## Minimal Deployment

### Step 1: Generate Service
```bash
./instantiate.sh --preset healthcare --output ../my-identity-service
```

### Step 2: Create Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: identity-service-secrets
stringData:
  DATABASE_URL: "jdbc:postgresql://postgres:5432/identity_db"
  DATABASE_USER: "identity_user"
  DATABASE_PASSWORD: "changeme"
  REDIS_HOST: "redis"
  REDIS_PASSWORD: "changeme"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
  AUTH0_DOMAIN: "your-tenant.auth0.com"
  AUTH0_CLIENT_ID: "your-client-id"
  AUTH0_CLIENT_SECRET: "your-secret"
```

### Step 3: Deploy
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: identity-service
spec:
  template:
    spec:
      containers:
      - name: identity-service
        image: your-registry/identity-service:latest
        envFrom:
        - secretRef:
            name: identity-service-secrets
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
```

## Required Environment Variables

### Minimal Set (Service Will Start)
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/identity_db
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
SPRING_PROFILES_ACTIVE=production
```

### Full Production Set
```bash
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/identity_db
DATABASE_USER=identity_user
DATABASE_PASSWORD=<secret>

# Redis
REDIS_HOST=redis-cluster
REDIS_PORT=6379
REDIS_PASSWORD=<secret>

# Kafka
KAFKA_BOOTSTRAP_SERVERS=broker1:9092,broker2:9092
KAFKA_USERNAME=identity-service
KAFKA_PASSWORD=<secret>

# Auth0
AUTH0_DOMAIN=tenant.auth0.com
AUTH0_CLIENT_ID=<client-id>
AUTH0_CLIENT_SECRET=<secret>
AUTH0_AUDIENCE=https://api.example.com

# Profile
SPRING_PROFILES_ACTIVE=production
```

## OAM Quick Setup

```yaml
apiVersion: core.oam.dev/v1beta1
kind: ComponentDefinition
metadata:
  name: identity-service
spec:
  workload:
    definition:
      apiVersion: apps/v1
      kind: Deployment
  schematic:
    cue:
      template: |
        output: {
          spec: {
            template: {
              spec: {
                containers: [{
                  image: parameter.image
                  envFrom: [
                    {secretRef: name: context.name + "-secrets"}
                  ]
                  env: [{
                    name: "SPRING_PROFILES_ACTIVE"
                    value: "production"
                  }]
                }]
              }
            }
          }
        }
        parameter: {
          image: string
        }
```

## Crossplane Quick Setup

```yaml
apiVersion: apiextensions.crossplane.io/v1
kind: Composition
metadata:
  name: identity-service-minimal
spec:
  resources:
    # PostgreSQL
    - name: database
      base:
        apiVersion: database.azure.io/v1beta1
        kind: PostgreSQLServer
    
    # Redis
    - name: cache
      base:
        apiVersion: cache.azure.io/v1beta1
        kind: Redis
    
    # Secret with connection info
    - name: secret
      base:
        apiVersion: kubernetes.crossplane.io/v1alpha1
        kind: Object
        spec:
          forProvider:
            manifest:
              apiVersion: v1
              kind: Secret
              stringData:
                DATABASE_URL: # from database
                REDIS_HOST: # from redis
```

## Health Check Endpoints

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
```

## Quick Debugging

### Check if service is running
```bash
kubectl logs deployment/identity-service | grep "Started Application"
```

### Check health
```bash
kubectl exec deployment/identity-service -- curl localhost:8080/actuator/health
```

### Common issues
1. **Port 8080 already in use**: Another service is running
2. **Database connection failed**: Check DATABASE_URL format
3. **Redis connection failed**: Check REDIS_HOST
4. **Missing environment variable**: Check secrets are mounted

## Template Parameters

### Using Presets
- `healthcare`: HIPAA-compliant with FHIR integration
- `financial`: PCI-DSS compliant with KYC
- `education`: FERPA-compliant with LMS

### Custom Configuration
Create a YAML file with:
```yaml
service:
  name: "my-identity-service"
  package: "com.company.identity"
  domain: "healthcare"

profiles:
  - name: "Customer"
    attributes:
      - name: "accountNumber"
        type: "String"
```

## Container Image

### Build
```bash
docker build -t identity-service:latest .
```

### Run Locally
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=default \
  identity-service:latest
```

## GitOps Integration

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: identity-service
spec:
  source:
    repoURL: https://github.com/org/identity-service
    path: k8s/
  destination:
    namespace: identity
  syncPolicy:
    automated:
      prune: true
```

## Support

- Infrastructure Guide: [README-INFRASTRUCTURE.md](README-INFRASTRUCTURE.md)
- Profile Configuration: [PROFILES.md](microservices/identity-template/PROFILES.md)
- Template Documentation: [README.md](README.md)

## Checklist

Before deploying:
- [ ] Database provisioned
- [ ] Redis available
- [ ] Kafka topics created (optional)
- [ ] Auth0 app configured
- [ ] Secrets created in Kubernetes
- [ ] Image built and pushed
- [ ] Health checks configured
- [ ] Monitoring enabled