# Infrastructure Integration Guide for Identity Service Template

This guide is for DevOps/Platform engineers integrating the Identity Service Template with OAM (Open Application Model) and Crossplane infrastructure.

## Table of Contents
1. [Overview](#overview)
2. [Template Structure](#template-structure)
3. [OAM Integration](#oam-integration)
4. [Crossplane Integration](#crossplane-integration)
5. [Secret Management](#secret-management)
6. [Environment Configuration](#environment-configuration)
7. [Deployment Workflow](#deployment-workflow)
8. [Monitoring & Observability](#monitoring--observability)
9. [Troubleshooting](#troubleshooting)

## Overview

The Identity Service Template generates Spring Boot applications that:
- Expect configuration via environment variables
- Support multiple deployment profiles (default, production, test)
- Integrate with PostgreSQL, Redis, and Kafka
- Require secrets for Auth0, database, and messaging infrastructure
- Provide health endpoints for Kubernetes probes

## Template Structure

```
identity-service-template/
├── microservices/identity-template/
│   ├── src/                          # Java source code templates
│   ├── configurations/               # Domain presets (healthcare, financial, education)
│   ├── k8s/                         # Kubernetes manifest templates
│   │   ├── configmap.yaml           # Non-sensitive configuration
│   │   ├── deployment.yaml          # Deployment manifest
│   │   └── secret.yaml              # Secret template (values injected by infrastructure)
│   ├── application*.yml             # Spring profiles
│   └── Dockerfile                   # Container build file
└── instantiate.sh                   # Template instantiation script
```

## OAM Integration

### 1. ComponentDefinition for Identity Service

Create an OAM ComponentDefinition similar to the rasa-chatbot example:

```yaml
apiVersion: core.oam.dev/v1beta1
kind: ComponentDefinition
metadata:
  name: identity-service
  annotations:
    definition.oam.dev/description: "Domain-specific identity and access management service"
spec:
  workload:
    definition:
      apiVersion: apps/v1
      kind: Deployment
  schematic:
    cue:
      template: |
        output: {
          apiVersion: "apps/v1"
          kind: "Deployment"
          metadata: {
            name: context.name
            namespace: context.namespace
          }
          spec: {
            replicas: parameter.replicas
            template: {
              spec: {
                containers: [{
                  name: context.name
                  image: parameter.image
                  
                  // Environment from ConfigMap and Secrets
                  envFrom: [
                    {configMapRef: name: context.name + "-config"},
                    {secretRef: name: context.name + "-secrets"}
                  ]
                  
                  env: [{
                    name: "SPRING_PROFILES_ACTIVE"
                    value: parameter.profile
                  }]
                  
                  ports: [{
                    containerPort: 8080
                    name: "http"
                  }]
                  
                  // Health checks
                  livenessProbe: {
                    httpGet: {
                      path: "/actuator/health/liveness"
                      port: 8080
                    }
                    initialDelaySeconds: 60
                  }
                  
                  readinessProbe: {
                    httpGet: {
                      path: "/actuator/health/readiness"
                      port: 8080
                    }
                    initialDelaySeconds: 30
                  }
                }]
              }
            }
          }
        }
        
        outputs: {
          "service": {
            apiVersion: "v1"
            kind: "Service"
            metadata: {
              name: context.name
              namespace: context.namespace
            }
            spec: {
              selector: {
                app: context.name
              }
              ports: [{
                port: 8080
                targetPort: 8080
              }]
            }
          }
        }
        
        parameter: {
          image: string
          replicas: *3 | int
          profile: *"production" | string
          domain: string  // healthcare, financial, education
          
          // Template generation parameters
          templateConfig?: {
            preset?: string     // Use preset configuration
            customConfig?: string  // Path to custom config
            repository?: string    // Generated service repository
          }
        }
```

### 2. Application Configuration

```yaml
apiVersion: core.oam.dev/v1beta1
kind: Application
metadata:
  name: clinic-identity-service
spec:
  components:
    - name: identity
      type: identity-service
      properties:
        image: "heathhealthregistry.azurecr.io/clinic-identity:latest"
        replicas: 3
        profile: "production"
        domain: "healthcare"
        templateConfig:
          preset: "healthcare"
          repository: "clinic-identity-service"
      traits:
        - type: ingress
          properties:
            domain: clinic-identity.health.com
            path: /
        - type: scaler
          properties:
            minReplicas: 2
            maxReplicas: 10
```

## Crossplane Integration

### 1. Composition for Identity Service

```yaml
apiVersion: apiextensions.crossplane.io/v1
kind: Composition
metadata:
  name: identity-service-composition
spec:
  compositeTypeRef:
    apiVersion: platform.example.org/v1alpha1
    kind: XIdentityService
  
  resources:
    # 1. Generate service from template
    - name: generate-service
      base:
        apiVersion: kubernetes.crossplane.io/v1alpha1
        kind: Object
        spec:
          forProvider:
            manifest:
              apiVersion: batch/v1
              kind: Job
              metadata:
                name: identity-service-generator
              spec:
                template:
                  spec:
                    containers:
                    - name: generator
                      image: identity-template-generator:latest
                      env:
                      - name: TEMPLATE_PRESET
                        value: # from composite
                      - name: SERVICE_NAME
                        value: # from composite
                      - name: OUTPUT_REPO
                        value: # from composite
      patches:
        - fromFieldPath: spec.domain
          toFieldPath: spec.forProvider.manifest.spec.template.spec.containers[0].env[0].value
    
    # 2. Create PostgreSQL Database
    - name: database
      base:
        apiVersion: database.azure.io/v1beta1
        kind: PostgreSQLServer
        spec:
          forProvider:
            location: eastus
            version: "14"
            sslEnforcement: Enabled
            storageProfile:
              storageMB: 51200
      patches:
        - fromFieldPath: spec.databaseSize
          toFieldPath: spec.forProvider.storageProfile.storageMB
    
    # 3. Create Redis Cache
    - name: redis
      base:
        apiVersion: cache.azure.io/v1beta1
        kind: Redis
        spec:
          forProvider:
            location: eastus
            sku:
              name: Premium
              family: P
              capacity: 1
      patches:
        - fromFieldPath: spec.cacheSize
          toFieldPath: spec.forProvider.sku.capacity
    
    # 4. Create Kafka Topics
    - name: kafka-topics
      base:
        apiVersion: kafka.crossplane.io/v1alpha1
        kind: Topic
        spec:
          forProvider:
            partitions: 3
            replicationFactor: 3
      patches:
        - fromFieldPath: metadata.name
          toFieldPath: spec.forProvider.name
          transforms:
            - type: string
              string:
                fmt: "%s-events"
    
    # 5. Create Kubernetes Secret
    - name: service-secrets
      base:
        apiVersion: kubernetes.crossplane.io/v1alpha1
        kind: Object
        spec:
          forProvider:
            manifest:
              apiVersion: v1
              kind: Secret
              metadata:
                name: # patched
                namespace: # patched
              stringData:
                DATABASE_URL: # from database connection
                DATABASE_USER: # from database
                DATABASE_PASSWORD: # from database secret
                REDIS_HOST: # from redis
                REDIS_PASSWORD: # from redis secret
                KAFKA_BOOTSTRAP_SERVERS: # from kafka
                AUTH0_CLIENT_ID: # from external secret
                AUTH0_CLIENT_SECRET: # from external secret
    
    # 6. Deploy Application
    - name: deployment
      base:
        apiVersion: apps/v1
        kind: Deployment
        spec:
          template:
            spec:
              containers:
              - name: identity-service
                envFrom:
                - secretRef:
                    name: # patched from secret
                env:
                - name: SPRING_PROFILES_ACTIVE
                  value: production
```

### 2. Composite Resource Definition (XRD)

```yaml
apiVersion: apiextensions.crossplane.io/v1
kind: CompositeResourceDefinition
metadata:
  name: xidentityservices.platform.example.org
spec:
  group: platform.example.org
  names:
    kind: XIdentityService
    plural: xidentityservices
  versions:
  - name: v1alpha1
    served: true
    referenceable: true
    schema:
      openAPIV3Schema:
        type: object
        properties:
          spec:
            type: object
            properties:
              domain:
                type: string
                enum: ["healthcare", "financial", "education"]
              preset:
                type: string
              serviceName:
                type: string
              databaseSize:
                type: integer
                default: 10240
              cacheSize:
                type: integer
                default: 1
              replicas:
                type: integer
                default: 3
              resources:
                type: object
                properties:
                  cpu:
                    type: string
                    default: "500m"
                  memory:
                    type: string
                    default: "1Gi"
```

### 3. Claim Example

```yaml
apiVersion: platform.example.org/v1alpha1
kind: IdentityService
metadata:
  name: clinic-identity
spec:
  domain: healthcare
  preset: healthcare
  serviceName: clinic-identity-service
  databaseSize: 20480
  cacheSize: 2
  replicas: 3
  resources:
    cpu: "1000m"
    memory: "2Gi"
```

## Secret Management

### Required Secrets Structure

The infrastructure must provide these secrets via environment variables:

```yaml
# Database (PostgreSQL)
DATABASE_URL: "jdbc:postgresql://host:port/database"
DATABASE_USER: "identity_user"
DATABASE_PASSWORD: "<generated-or-vault>"

# Redis
REDIS_HOST: "redis-cluster.namespace.svc.cluster.local"
REDIS_PORT: "6379"
REDIS_PASSWORD: "<generated-or-vault>"
REDIS_SSL_ENABLED: "true"

# Kafka
KAFKA_BOOTSTRAP_SERVERS: "broker1:9092,broker2:9092"
KAFKA_USERNAME: "identity-service"
KAFKA_PASSWORD: "<generated-or-vault>"
KAFKA_SECURITY_PROTOCOL: "SASL_SSL"

# Auth0 / Identity Provider
AUTH0_DOMAIN: "tenant.auth0.com"
AUTH0_CLIENT_ID: "<from-vault>"
AUTH0_CLIENT_SECRET: "<from-vault>"
AUTH0_AUDIENCE: "https://api.example.com"

# FHIR (Healthcare specific)
FHIR_SERVER_URL: "https://fhir.example.com/r4"
FHIR_USERNAME: "service-account"
FHIR_PASSWORD: "<from-vault>"
```

### Secret Creation Workflow

1. **Crossplane creates infrastructure** (Database, Redis, Kafka)
2. **Connection details extracted** from provisioned resources
3. **External secrets fetched** from vault (Auth0, FHIR)
4. **Kubernetes Secret created** with all values
5. **Deployment references** the secret

### Using External Secrets Operator (ESO)

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: identity-service-secrets
spec:
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: identity-service-secrets
  data:
  - secretKey: AUTH0_CLIENT_SECRET
    remoteRef:
      key: secret/identity-service
      property: auth0_client_secret
  - secretKey: DATABASE_PASSWORD
    remoteRef:
      key: secret/database
      property: password
```

## Environment Configuration

### ConfigMap for Non-Sensitive Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: identity-service-config
data:
  SPRING_PROFILES_ACTIVE: "production"
  SERVER_PORT: "8080"
  
  # Database Pool Configuration
  DB_POOL_MAX_SIZE: "20"
  DB_POOL_MIN_IDLE: "10"
  DB_CONNECTION_TIMEOUT: "30000"
  
  # Redis Configuration
  REDIS_TIMEOUT: "2000ms"
  REDIS_DATABASE: "0"
  
  # Kafka Configuration
  KAFKA_CONSUMER_GROUP_ID: "${service-name}"
  KAFKA_AUTO_OFFSET_RESET: "earliest"
  KAFKA_SESSION_TIMEOUT: "30000"
  
  # Monitoring
  TRACING_ENABLED: "true"
  TRACING_SAMPLING_RATE: "0.1"
  METRICS_ENABLED: "true"
```

### Profile Selection

- **Development**: `SPRING_PROFILES_ACTIVE=default`
- **Production**: `SPRING_PROFILES_ACTIVE=production`
- **Testing**: `SPRING_PROFILES_ACTIVE=test`

## Deployment Workflow

### 1. Template Instantiation

```bash
# In CI/CD pipeline or Argo Workflow
git clone https://github.com/org/identity-service-template
cd identity-service-template

# Generate service
./instantiate.sh \
  --preset healthcare \
  --output /tmp/generated-service

# Push to service repository
cd /tmp/generated-service
git init
git remote add origin https://github.com/org/clinic-identity-service
git push -u origin main
```

### 2. Container Build

```yaml
# Argo Workflow or GitHub Actions
apiVersion: argoproj.io/v1alpha1
kind: WorkflowTemplate
metadata:
  name: build-identity-service
spec:
  templates:
  - name: build
    container:
      image: gcr.io/kaniko-project/executor:latest
      args:
      - --dockerfile=Dockerfile
      - --context=git://github.com/org/{{service-name}}
      - --destination={{registry}}/{{service-name}}:{{version}}
```

### 3. Deployment via GitOps

```yaml
# ArgoCD Application
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: identity-service
spec:
  source:
    repoURL: https://github.com/org/identity-service-gitops
    path: deployments/production
    targetRevision: main
  destination:
    server: https://kubernetes.default.svc
    namespace: identity
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

## Monitoring & Observability

### Health Endpoints

The service exposes these endpoints for monitoring:

- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/info` - Application information

### Prometheus Scraping

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8080"
  prometheus.io/path: "/actuator/prometheus"
```

### Key Metrics to Monitor

```yaml
# Application metrics
- identity_service_registrations_total
- identity_service_authentications_total
- identity_service_authentication_failures_total
- identity_service_session_duration_seconds

# Infrastructure metrics
- database_connections_active
- redis_connection_pool_size
- kafka_producer_record_send_total
- http_server_requests_seconds
```

### Logging Configuration

```yaml
# CloudWatch (AWS)
CLOUDWATCH_ENABLED: "true"
CLOUDWATCH_LOG_GROUP: "/aws/ecs/identity-service"

# Azure Monitor
AZURE_MONITOR_ENABLED: "true"
AZURE_MONITOR_INSTRUMENTATION_KEY: "<key>"

# Google Cloud Logging
GCP_LOGGING_ENABLED: "true"
GCP_PROJECT_ID: "project-id"
```

## Troubleshooting

### Common Issues

#### 1. Service Fails to Start

```bash
# Check pod logs
kubectl logs -n identity deployment/identity-service

# Common causes:
- Missing required environment variables
- Database connection failure
- Invalid Auth0 configuration
```

#### 2. Database Connection Issues

```bash
# Verify secret exists
kubectl get secret identity-service-secrets -o yaml

# Test connection
kubectl run -it --rm debug --image=postgres:14 --restart=Never -- \
  psql "postgresql://user:pass@host:5432/db"
```

#### 3. Redis Connection Issues

```bash
# Test Redis connection
kubectl run -it --rm debug --image=redis:7 --restart=Never -- \
  redis-cli -h redis-host -p 6379 -a password ping
```

#### 4. Kafka Connection Issues

```bash
# Check Kafka connectivity
kubectl run -it --rm debug --image=confluentinc/cp-kafka:latest --restart=Never -- \
  kafka-broker-api-versions --bootstrap-server broker:9092
```

### Debug Mode

Enable debug logging:

```yaml
env:
- name: LOG_LEVEL
  value: DEBUG
- name: APP_LOG_LEVEL
  value: TRACE
```

### Validation Checklist

Before deployment:

- [ ] All required secrets are created
- [ ] Database is provisioned and accessible
- [ ] Redis is running and accessible
- [ ] Kafka topics are created
- [ ] Auth0 application is configured
- [ ] Network policies allow communication
- [ ] Resource limits are appropriate
- [ ] Health check endpoints are accessible

## Integration Examples

### Complete Crossplane + OAM Example

```yaml
# 1. Create XR Claim
apiVersion: platform.example.org/v1alpha1
kind: IdentityService
metadata:
  name: clinic-identity
spec:
  domain: healthcare
  preset: healthcare
  
---
# 2. OAM Application
apiVersion: core.oam.dev/v1beta1
kind: Application
metadata:
  name: clinic-identity-app
spec:
  components:
  - name: identity
    type: identity-service
    properties:
      image: "generated-from-claim"
      
---
# 3. GitOps Sync
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: clinic-identity-gitops
spec:
  source:
    path: clinic-identity
  syncPolicy:
    automated: {}
```

### Terraform Integration

```hcl
resource "kubernetes_secret" "identity_service" {
  metadata {
    name      = "identity-service-secrets"
    namespace = "identity"
  }
  
  data = {
    DATABASE_URL      = azurerm_postgresql_server.identity.connection_string
    DATABASE_PASSWORD = random_password.db_password.result
    REDIS_PASSWORD    = azurerm_redis_cache.identity.primary_access_key
    AUTH0_CLIENT_SECRET = data.vault_generic_secret.auth0.data["client_secret"]
  }
}
```

## Support & Contact

For infrastructure-specific questions:
- Review the template documentation in `/microservices/identity-template/README.md`
- Check the PROFILES.md for environment configuration details
- Examine k8s/ directory for Kubernetes manifest examples
- Contact the platform team for OAM/Crossplane specific issues

## Next Steps

1. Review domain-specific configurations in `configurations/`
2. Customize the template for your organization's needs
3. Set up CI/CD pipeline for automated generation
4. Configure monitoring and alerting
5. Implement backup and disaster recovery