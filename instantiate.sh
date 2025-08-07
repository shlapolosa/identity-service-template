#!/bin/bash

# Identity Service Template Instantiation Script
# Usage: ./instantiate.sh --config <config-file> --output <output-dir>

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
TEMPLATE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/microservices/identity-template"
OUTPUT_DIR=""
CONFIG_FILE=""
PRESET=""
VERBOSE=false

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to print usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
    --config <file>     Path to configuration YAML file
    --preset <name>     Use a preset configuration (healthcare, financial, education)
    --output <dir>      Output directory for generated service
    --verbose           Enable verbose output
    --help              Show this help message

Examples:
    # Use custom configuration
    $0 --config my-config.yaml --output ../my-identity-service

    # Use preset configuration
    $0 --preset healthcare --output ../healthcare-identity

EOF
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        --preset)
            PRESET="$2"
            shift 2
            ;;
        --output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            usage
            ;;
        *)
            print_message "$RED" "Unknown option: $1"
            usage
            ;;
    esac
done

# Validate inputs
if [[ -z "$OUTPUT_DIR" ]]; then
    print_message "$RED" "Error: Output directory is required"
    usage
fi

if [[ -z "$CONFIG_FILE" ]] && [[ -z "$PRESET" ]]; then
    print_message "$RED" "Error: Either --config or --preset must be specified"
    usage
fi

# Use preset if specified
if [[ -n "$PRESET" ]]; then
    CONFIG_FILE="$TEMPLATE_DIR/configurations/$PRESET/${PRESET}-config.yaml"
    if [[ ! -f "$CONFIG_FILE" ]]; then
        print_message "$RED" "Error: Preset '$PRESET' not found"
        print_message "$YELLOW" "Available presets: healthcare, financial, education"
        exit 1
    fi
    print_message "$GREEN" "Using preset: $PRESET"
fi

# Validate config file exists
if [[ ! -f "$CONFIG_FILE" ]]; then
    print_message "$RED" "Error: Configuration file not found: $CONFIG_FILE"
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

print_message "$BLUE" "========================================="
print_message "$BLUE" "Identity Service Template Generator"
print_message "$BLUE" "========================================="
print_message "$GREEN" "Template Dir: $TEMPLATE_DIR"
print_message "$GREEN" "Config File: $CONFIG_FILE"
print_message "$GREEN" "Output Dir: $OUTPUT_DIR"
print_message "$BLUE" "========================================="

# Function to extract value from YAML
get_yaml_value() {
    local file=$1
    local key=$2
    python3 -c "
import yaml
with open('$file', 'r') as f:
    data = yaml.safe_load(f)
keys = '$key'.split('.')
result = data
for k in keys:
    if k in result:
        result = result[k]
    else:
        print('')
        exit()
print(result if result is not None else '')
"
}

# Extract configuration values
SERVICE_NAME=$(get_yaml_value "$CONFIG_FILE" "service.name")
PACKAGE=$(get_yaml_value "$CONFIG_FILE" "service.package")
GROUP_ID=$(get_yaml_value "$CONFIG_FILE" "service.groupId")
ARTIFACT_ID=$(get_yaml_value "$CONFIG_FILE" "service.artifactId")
VERSION=$(get_yaml_value "$CONFIG_FILE" "service.version")
DOMAIN=$(get_yaml_value "$CONFIG_FILE" "service.domain")

print_message "$YELLOW" "Generating service: $SERVICE_NAME"

# Step 1: Copy base template structure
print_message "$BLUE" "Step 1: Copying base template..."
cp -r "$TEMPLATE_DIR/." "$OUTPUT_DIR/"

# Step 2: Process pom.xml
print_message "$BLUE" "Step 2: Processing pom.xml..."
sed -i.bak "s|{{groupId}}|$GROUP_ID|g" "$OUTPUT_DIR/pom.xml"
sed -i.bak "s|{{artifactId}}|$ARTIFACT_ID|g" "$OUTPUT_DIR/pom.xml"
sed -i.bak "s|{{version}}|$VERSION|g" "$OUTPUT_DIR/pom.xml"
sed -i.bak "s|{{serviceName}}|$SERVICE_NAME|g" "$OUTPUT_DIR/pom.xml"
sed -i.bak "s|{{domain}}|$DOMAIN|g" "$OUTPUT_DIR/pom.xml"
rm "$OUTPUT_DIR/pom.xml.bak"

# Step 3: Process Spring profiles
print_message "$BLUE" "Step 3: Processing Spring profiles..."
# Update service name in profile files
find "$OUTPUT_DIR/src/main/resources" -name "application*.yml" -type f -exec sed -i.bak "s|healthcare-identity-service|$SERVICE_NAME|g" {} \;
find "$OUTPUT_DIR/src/main/resources" -name "*.bak" -type f -delete

# Update package name in profiles
find "$OUTPUT_DIR/src/main/resources" -name "application*.yml" -type f -exec sed -i.bak "s|com.healthcare.identity|$PACKAGE|g" {} \;
find "$OUTPUT_DIR/src/main/resources" -name "*.bak" -type f -delete

# Update Kubernetes manifests
if [ -d "$OUTPUT_DIR/k8s" ]; then
    find "$OUTPUT_DIR/k8s" -name "*.yaml" -type f -exec sed -i.bak "s|healthcare-identity-service|$SERVICE_NAME|g" {} \;
    find "$OUTPUT_DIR/k8s" -name "*.yaml" -type f -exec sed -i.bak "s|healthcare|$DOMAIN|g" {} \;
    find "$OUTPUT_DIR/k8s" -name "*.bak" -type f -delete
fi

# Generate base application.yml if it doesn't exist
if [ ! -f "$OUTPUT_DIR/src/main/resources/application.yml" ]; then
cat > "$OUTPUT_DIR/src/main/resources/application.yml" << EOF
spring:
  application:
    name: $SERVICE_NAME
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:default}
  
  datasource:
    url: \${DATABASE_URL:jdbc:postgresql://localhost:5432/${SERVICE_NAME//-/_}}
    username: \${DATABASE_USER:postgres}
    password: \${DATABASE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
  
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
  
  redis:
    host: \${REDIS_HOST:localhost}
    port: \${REDIS_PORT:6379}
    password: \${REDIS_PASSWORD:}
    timeout: 2000ms
  
  kafka:
    bootstrap-servers: \${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: $SERVICE_NAME
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

server:
  port: \${SERVER_PORT:8080}
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: INFO
    $PACKAGE: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Domain-specific configuration
domain:
  type: $DOMAIN
EOF
fi

# Step 4: Generate Main Application class
print_message "$BLUE" "Step 4: Generating Main Application class..."
PACKAGE_PATH=$(echo "$PACKAGE" | tr '.' '/')
mkdir -p "$OUTPUT_DIR/src/main/java/$PACKAGE_PATH"

cat > "$OUTPUT_DIR/src/main/java/$PACKAGE_PATH/Application.java" << EOF
package $PACKAGE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
@EnableAsync
@EnableTransactionManagement
@Modulith(systemName = "$SERVICE_NAME")
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
EOF

# Step 5: Process profiles from configuration
print_message "$BLUE" "Step 5: Generating profile entities..."

# This would normally call a Java-based generator or Python script
# For now, we'll create a simple example
python3 << EOF
import yaml
import os
from pathlib import Path

with open('$CONFIG_FILE', 'r') as f:
    config = yaml.safe_load(f)

package = config['service']['package']
package_path = package.replace('.', '/')
profiles_dir = Path('$OUTPUT_DIR/src/main/java').joinpath(package_path, 'domain', 'profiles')
profiles_dir.mkdir(parents=True, exist_ok=True)

# Generate profile entities
for profile in config.get('profiles', []):
    profile_name = profile['name']
    extends = profile.get('extends', 'BaseProfile')
    
    # Create basic profile class (simplified version)
    profile_file = profiles_dir / f"{profile_name}.java"
    
    with open(profile_file, 'w') as pf:
        pf.write(f"package {package}.domain.profiles;\n\n")
        pf.write(f"import {package}.core.domain.{extends};\n")
        pf.write("import jakarta.persistence.*;\n")
        pf.write("import lombok.*;\n\n")
        pf.write(f"@Entity\n")
        pf.write(f"@Table(name = \"{profile.get('tableName', profile_name.lower() + 's')}\")\n")
        pf.write(f"@DiscriminatorValue(\"{profile.get('discriminatorValue', profile_name.upper())}\")\n")
        pf.write("@Data\n")
        pf.write("@EqualsAndHashCode(callSuper = true)\n")
        pf.write("@SuperBuilder\n")
        pf.write("@NoArgsConstructor\n")
        pf.write(f"public class {profile_name} extends {extends} {{\n\n")
        
        # Add attributes
        for attr in profile.get('attributes', []):
            nullable = "false" if attr.get('required', False) else "true"
            unique = ", unique = true" if attr.get('unique', False) else ""
            pf.write(f"    @Column(name = \"{attr.get('columnName', attr['name'])}\", nullable = {nullable}{unique})\n")
            pf.write(f"    private {attr['type']} {attr['name']};\n\n")
        
        pf.write("    @Override\n")
        pf.write("    public String getProfileIdentifier() {\n")
        pf.write(f"        return \"{profile.get('discriminatorValue', profile_name.upper())}-\" + getId();\n")
        pf.write("    }\n\n")
        
        pf.write("    @Override\n")
        pf.write("    public boolean requiresVerification() {\n")
        pf.write(f"        return {str(profile.get('requiresVerification', False)).lower()};\n")
        pf.write("    }\n")
        pf.write("}\n")
    
    print(f"Generated profile: {profile_name}")

print("Profile generation complete!")
EOF

# Step 6: Move core components to correct package
print_message "$BLUE" "Step 6: Organizing core components..."
mkdir -p "$OUTPUT_DIR/src/main/java/$PACKAGE_PATH/core"
mv "$OUTPUT_DIR/src/main/java/com/template/identity/core/"* "$OUTPUT_DIR/src/main/java/$PACKAGE_PATH/core/" 2>/dev/null || true

# Update package names in core files
find "$OUTPUT_DIR/src/main/java/$PACKAGE_PATH/core" -name "*.java" -type f -exec sed -i.bak "s|com.template.identity|$PACKAGE|g" {} \;
find "$OUTPUT_DIR/src/main/java/$PACKAGE_PATH" -name "*.bak" -type f -delete

# Step 7: Generate Liquibase migrations
print_message "$BLUE" "Step 7: Generating database migrations..."
mkdir -p "$OUTPUT_DIR/src/main/resources/db/changelog"

cat > "$OUTPUT_DIR/src/main/resources/db/changelog/db.changelog-master.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    
    <include file="db/changelog/001-create-base-tables.xml"/>
    <include file="db/changelog/002-create-profile-tables.xml"/>
    <include file="db/changelog/003-create-indexes.xml"/>
</databaseChangeLog>
EOF

# Step 8: Clean up template files
print_message "$BLUE" "Step 8: Cleaning up..."
rm -rf "$OUTPUT_DIR/templates"
rm -rf "$OUTPUT_DIR/configurations"
rm -rf "$OUTPUT_DIR/generator"
rm -f "$OUTPUT_DIR/config-schema.yaml"
rm -f "$OUTPUT_DIR/instantiate.sh"
rm -rf "$OUTPUT_DIR/src/main/java/com/template"

# Step 9: Generate README
print_message "$BLUE" "Step 9: Generating README..."
cat > "$OUTPUT_DIR/README.md" << EOF
# $SERVICE_NAME

Generated from Identity Service Template for $DOMAIN domain.

## Quick Start

1. Set up required services:
\`\`\`bash
docker-compose up -d postgres redis kafka
\`\`\`

2. Configure environment variables:
\`\`\`bash
export AUTH0_DOMAIN=your-domain.auth0.com
export AUTH0_CLIENT_ID=your-client-id
export AUTH0_CLIENT_SECRET=your-client-secret
\`\`\`

3. Run the application:
\`\`\`bash
./mvnw spring-boot:run
\`\`\`

## Configuration

See \`src/main/resources/application.yml\` for configuration options.

## API Documentation

Once running, visit http://localhost:8080/swagger-ui.html

## Generated from Template

This service was generated from the identity-service-template with the following configuration:
- Domain: $DOMAIN
- Package: $PACKAGE
- Version: $VERSION
EOF

print_message "$GREEN" "========================================="
print_message "$GREEN" "✅ Service generation complete!"
print_message "$GREEN" "Generated service at: $OUTPUT_DIR"
print_message "$GREEN" "========================================="
print_message "$YELLOW" "Next steps:"
print_message "$YELLOW" "  1. cd $OUTPUT_DIR"
print_message "$YELLOW" "  2. Review and customize the generated code"
print_message "$YELLOW" "  3. Set up your database and external services"
print_message "$YELLOW" "  4. Run: ./mvnw spring-boot:run"