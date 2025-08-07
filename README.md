# Identity Service Template

A powerful, domain-agnostic template for generating production-ready identity and access management services. This template enables rapid deployment of compliant, feature-rich identity services across healthcare, financial services, education, and other domains.

## Overview

The Identity Service Template is a code generation framework that creates fully-functional Spring Boot identity services based on configuration. It provides:

- **Domain-specific presets** for healthcare (HIPAA), financial (PCI-DSS/SOX), and education (FERPA)
- **Extensible profile system** with inheritance and polymorphism
- **Multi-provider authentication** (Auth0, Okta, Cognito, Keycloak)
- **Event-driven architecture** with Kafka integration
- **Compliance-by-design** with built-in audit trails and encryption
- **External system integration** (FHIR, LMS, KYC providers, etc.)

## Quick Start

### Using a Preset

```bash
# Healthcare identity service
./instantiate.sh --preset healthcare --output ../clinic-identity-service

# Financial services identity
./instantiate.sh --preset financial --output ../bank-identity-service

# Education identity service
./instantiate.sh --preset education --output ../school-identity-service
```

### Using Custom Configuration

```bash
./instantiate.sh --config my-config.yaml --output ../my-identity-service
```

## Architecture

### Core Components

The template provides these invariant components that all generated services inherit:

1. **BaseUser** - Core user entity with common attributes
2. **BaseProfile** - Extensible profile system with polymorphic support
3. **BaseRegistrationUseCase** - Template method pattern for registration workflows
4. **IdpProvider** - Interface for external identity provider integration
5. **EventPublisher** - Domain event publishing infrastructure

### Generated Components

Based on your configuration, the template generates:

- **Profile Entities** - Domain-specific user profiles (Patient, Student, Customer, etc.)
- **Use Cases** - Complete registration, authentication, and management use cases
- **REST Controllers** - OpenAPI-documented REST endpoints
- **Database Migrations** - Liquibase changesets for all entities
- **Integration Adapters** - Connectors for external systems

## Configuration Schema

### Service Definition

```yaml
service:
  name: "my-identity-service"
  package: "com.mycompany.identity"
  domain: "healthcare" # healthcare, financial, education, retail, generic
```

### Profile Types

Define custom profiles that extend the base system:

```yaml
profiles:
  - name: "Doctor"
    extends: "BaseProfile"
    attributes:
      - name: "licenseNumber"
        type: "String"
        required: true
        unique: true
    relationships:
      - type: "OneToMany"
        target: "Appointment"
    permissions:
      - "PRESCRIBE_MEDICATION"
      - "VIEW_PATIENT_RECORDS"
    requiresVerification: true
```

### Authentication Configuration

```yaml
authentication:
  providers:
    primary:
      type: "auth0" # auth0, okta, cognito, keycloak
      config:
        domain: "${AUTH0_DOMAIN}"
  methods:
    - type: "password"
    - type: "biometric"
    - type: "mfa"
  session:
    provider: "redis"
    timeout: 3600
```

### Integrations

```yaml
integrations:
  - name: "fhir"
    type: "fhir"
    config:
      url: "${FHIR_SERVER_URL}"
      version: "R4"
    eventMappings:
      - event: "PatientRegistered"
        action: "createFhirPatient"
```

### Compliance

```yaml
compliance:
  standards: ["HIPAA", "GDPR"]
  audit:
    enabled: true
    retention: 2555 # days
  encryption:
    atRest: true
    algorithm: "AES-256"
```

## Domain Presets

### Healthcare Preset

- **Profiles**: Patient, Physician, Nurse, Administrator
- **Integrations**: FHIR R4, HL7v2, Insurance APIs
- **Compliance**: HIPAA, HITECH, GDPR
- **Features**: Biometric auth, prescription management, appointment scheduling

### Financial Services Preset

- **Profiles**: Customer, Banker, ComplianceOfficer, Auditor
- **Integrations**: KYC providers, Credit bureaus, Fraud detection
- **Compliance**: PCI-DSS, SOX, AML/KYC, FATCA
- **Features**: Transaction approval, risk scoring, audit trails

### Education Preset

- **Profiles**: Student, Teacher, Parent, Administrator
- **Integrations**: LMS (Canvas/Blackboard), SIS, Google Workspace
- **Compliance**: FERPA, COPPA, IDEA
- **Features**: Grade management, parent portal, roster sync

## Generated Service Structure

```
my-identity-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/mycompany/identity/
│   │   │       ├── core/           # Base components
│   │   │       ├── domain/         # Entities and repositories
│   │   │       ├── application/    # Use cases and services
│   │   │       ├── infrastructure/ # External integrations
│   │   │       └── interfaces/     # REST controllers
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/changelog/       # Database migrations
│   └── test/
├── pom.xml
├── Dockerfile
└── README.md
```

## Features

### Registration Workflows

- Multi-step registration processes
- Email/phone verification
- KYC/identity verification
- Approval workflows
- External system synchronization

### Authentication

- Multiple authentication methods
- Multi-factor authentication (MFA)
- Session management with Redis
- Device fingerprinting
- Passwordless options

### Profile Management

- Polymorphic profile types
- Custom attributes and permissions
- Self-service vs admin-only fields
- Audit trail for all changes
- Bulk operations support

### Security & Compliance

- Role-based access control (RBAC)
- Attribute-based access control (ABAC)
- Data encryption at rest and in transit
- Comprehensive audit logging
- GDPR compliance (right to erasure, data portability)
- Industry-specific compliance (HIPAA, PCI-DSS, FERPA)

### Integrations

- External IdP (Auth0, Okta, Cognito)
- Healthcare (FHIR, HL7)
- Education (LMS, SIS)
- Financial (KYC, Credit bureaus)
- Messaging (Kafka, RabbitMQ)
- Observability (Prometheus, Jaeger)

## Customization

### Adding Custom Profiles

1. Define the profile in your configuration:

```yaml
profiles:
  - name: "CustomRole"
    extends: "BaseProfile"
    attributes:
      - name: "customField"
        type: "String"
```

2. Run the instantiation script
3. The generator creates all necessary classes and migrations

### Adding Integrations

1. Define the integration in configuration:

```yaml
integrations:
  - name: "custom-api"
    type: "rest"
    config:
      url: "${CUSTOM_API_URL}"
```

2. Implement the provider interface in the generated service
3. Map events to integration actions

### Extending Use Cases

Generated use cases follow the template method pattern, allowing easy extension:

```java
@Component
public class CustomRegistrationUseCase extends BaseRegistrationUseCase<CustomProfile> {
    @Override
    protected void postRegistration(CustomProfile profile) {
        // Add custom logic
    }
}
```

## Deployment

### Docker

```dockerfile
FROM openjdk:21-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes/OpenShift

The template includes Kubernetes manifests and supports:
- ConfigMaps for configuration
- Secrets for sensitive data
- Horizontal Pod Autoscaling
- Health checks and probes

### Cloud Platforms

- **AWS**: ECS/EKS deployment with Cognito integration
- **Azure**: AKS deployment with Azure AD integration
- **Google Cloud**: GKE deployment with Identity Platform

## Testing

Generated services include:

- Unit tests for all use cases
- Integration tests with embedded databases
- API tests with REST Assured
- Security tests for authentication/authorization
- Compliance tests for data handling

## Monitoring & Observability

- **Metrics**: Prometheus/Grafana dashboards
- **Tracing**: Distributed tracing with Jaeger/Zipkin
- **Logging**: Structured JSON logging
- **Health Checks**: Actuator endpoints
- **Alerts**: Configurable alerting rules

## Infrastructure Integration

**For DevOps/Platform Engineers**: See [README-INFRASTRUCTURE.md](README-INFRASTRUCTURE.md) for detailed instructions on:
- OAM ComponentDefinition integration
- Crossplane Composition setup
- Secret management and injection
- Kubernetes deployment patterns
- GitOps workflow integration

## Spring Profiles & Configuration

The generated services support multiple Spring profiles:
- **default**: Local development with H2 database
- **production**: Full external services with injected secrets
- **test**: CI/CD testing with mocked services

See [PROFILES.md](microservices/identity-template/PROFILES.md) for detailed configuration.

## Contributing

To add new domain presets or enhance the template:

1. Create a new configuration in `configurations/<domain>/`
2. Add domain-specific templates if needed
3. Update the instantiation script
4. Submit a pull request

## License

MIT License - See LICENSE file for details

## Support

For issues, questions, or contributions:
- GitHub Issues: [Create an issue](https://github.com/your-org/identity-service-template/issues)
- Documentation: [Wiki](https://github.com/your-org/identity-service-template/wiki)

## Roadmap

- [ ] GraphQL API generation
- [ ] Blockchain identity integration
- [ ] Biometric authentication templates
- [ ] Zero-trust architecture patterns
- [ ] Cloud-native service mesh integration
- [ ] AI-powered fraud detection
- [ ] Decentralized identity (DID) support