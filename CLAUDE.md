# CLAUDE.md - Identity Service Template

This template generates production-ready identity services for various domains (healthcare, financial, education).

## Template Overview

The Identity Service Template is a **code generation framework** that creates Spring Boot identity services based on YAML configuration. It provides domain-specific presets and supports custom configurations.

## How to Use This Template

### Quick Generation

```bash
# Use a preset
./instantiate.sh --preset healthcare --output ../my-health-identity

# Use custom config
./instantiate.sh --config custom.yaml --output ../my-identity
```

### Key Directories

- `microservices/identity-template/` - Core template files
- `configurations/` - Domain-specific presets
- `templates/` - Mustache templates for code generation
- `core/` - Invariant base components

## Template Components

### Core (Invariant)
- `BaseUser` - Foundation user entity
- `BaseProfile` - Polymorphic profile system
- `BaseRegistrationUseCase` - Registration workflow template
- `IdpProvider` - External IdP interface
- `EventPublisher` - Event streaming interface

### Generated
Based on configuration, generates:
- Profile entities (Patient, Student, Customer, etc.)
- Registration/authentication use cases
- REST controllers with OpenAPI
- Database migrations
- Integration adapters

## Configuration Structure

```yaml
service:
  name: "service-name"
  package: "com.company.identity"
  domain: "healthcare|financial|education"

profiles:
  - name: "ProfileName"
    extends: "BaseProfile"
    attributes: [...]
    permissions: [...]
    
authentication:
  providers: {...}
  methods: [...]
  
integrations:
  - name: "integration-name"
    type: "fhir|kafka|rest"
    config: {...}
    
compliance:
  standards: ["HIPAA", "GDPR", ...]
```

## Domain Presets

### Healthcare (`configurations/healthcare/`)
- Profiles: Patient, Physician, Nurse
- Integrations: FHIR, HL7
- Compliance: HIPAA, HITECH

### Financial (`configurations/financial/`)
- Profiles: Customer, Banker, Auditor
- Integrations: KYC, Credit bureaus
- Compliance: PCI-DSS, SOX, AML

### Education (`configurations/education/`)
- Profiles: Student, Teacher, Parent
- Integrations: LMS, SIS
- Compliance: FERPA, COPPA

## Extending the Template

### Adding New Domain Preset

1. Create `configurations/<domain>/<domain>-config.yaml`
2. Define domain-specific profiles and integrations
3. Add to instantiate.sh preset options

### Adding New Base Components

1. Add to `core/` directory
2. Update generated services to use new component
3. Document in template schema

### Custom Code Generation

1. Add Mustache templates in `templates/`
2. Update instantiation script to process templates
3. Map configuration to template variables

## Integration with Infrastructure

### Crossplane/OAM

When used with Crossplane compositions:

```yaml
- name: "identity-service"
  type: "spring-boot"
  template: "identity-service-template"
  config:
    preset: "healthcare"
```

### CI/CD Pipeline

1. Template instantiation during CI
2. Automated testing of generated code
3. Container build and push
4. Deployment via GitOps

## Generated Service Structure

```
output-directory/
├── src/main/java/<package>/
│   ├── core/          # Base components
│   ├── domain/        # Entities
│   ├── application/   # Use cases
│   ├── infrastructure/# Integrations
│   └── interfaces/    # REST APIs
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog/
├── pom.xml
└── README.md
```

## Key Features

### Multi-Domain Support
- Healthcare (FHIR, HL7)
- Financial (KYC, AML)
- Education (LMS, SIS)
- Extensible to any domain

### Security & Compliance
- Industry-specific compliance
- Encryption at rest/transit
- Audit trails
- GDPR support

### Authentication
- Multiple IdP support
- MFA/Biometric
- Session management
- SSO/SAML

### Event-Driven
- Kafka integration
- Domain events
- Event sourcing ready

## Testing Generated Services

Generated services include:
- Unit tests for use cases
- Integration tests
- API tests
- Security tests

Run tests:
```bash
cd generated-service/
./mvnw test
```

## Troubleshooting

### Generation Issues

1. **Missing Python/YAML**: Install `pip install pyyaml`
2. **Permission denied**: Run `chmod +x instantiate.sh`
3. **Config validation**: Check YAML syntax

### Generated Code Issues

1. **Package conflicts**: Verify package names in config
2. **Missing dependencies**: Run `mvn clean install`
3. **Database issues**: Check migration scripts

## Best Practices

1. **Start with a preset** then customize
2. **Review generated code** before deployment
3. **Keep configurations in version control**
4. **Test thoroughly** after generation
5. **Document customizations** in service README

## Template Maintenance

- Update Spring Boot version in base pom.xml
- Keep presets current with regulations
- Add new integration templates as needed
- Monitor security advisories

## Related Documentation

- [Identity Service README](../identity-service/README.md)
- [Chat Template](../chat-template/CLAUDE.md)
- [Crossplane Compositions](../health-service-idp/crossplane/)