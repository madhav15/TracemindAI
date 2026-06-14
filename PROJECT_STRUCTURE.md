# TracemindAI - Project Structure

## Overview
Production-quality Maven multi-module Spring Boot 3.x application with Java 21 targeting document processing operations.

**Group ID:** `com.tracemindai`  
**Version:** `1.0.0-SNAPSHOT`  
**Maven Packaging:** `pom` (parent) + individual modules

## Parent POM Configuration
- Java 21 (source/target/release)
- Spring Boot 3.3.0
- Spring Cloud 2023.0.0
- Centralized dependency management via BOMs
- Common plugins: maven-compiler-plugin, spring-boot-maven-plugin

## Module Structure

### 1. common-lib
**Artifact:** `common-lib`  
**Purpose:** Shared DTOs, events, constants, exceptions, and utilities used across all services.

**Package Structure:**
```
com.tracemindai.common
├── dto/
│   └── ApiResponse.java
├── event/
│   └── DomainEvent.java
├── constant/
│   └── ApplicationConstants.java
├── exception/
│   ├── ApplicationException.java
│   ├── ValidationException.java
│   └── ResourceNotFoundException.java
└── util/
    └── StringUtils.java
```

**Dependencies:** Jackson, Jakarta Validation, Hibernate Validator

---

### 2. file-upload-service
**Artifact:** `file-upload-service`  
**Purpose:** Handles file upload operations and validations.
**Base API Path:** `/api/v1/files`

**Package Structure:**
```
com.tracemindai.fileupload
├── config/
│   └── FileUploadConfig.java (configurable upload dir, max size, concurrency)
├── controller/
│   └── FileUploadController.java
├── service/
│   └── FileUploadService.java
├── repository/
│   └── FileRepository.java
├── entity/
│   └── FileMetadata.java
├── dto/
│   ├── FileUploadRequest.java
│   └── FileUploadResponse.java
├── exception/
│   └── FileUploadException.java
└── util/
    └── FileValidationUtil.java
```

**Dependencies:** common-lib, spring-boot-starter-validation, commons-io

---

### 3. pre-processor-service
**Artifact:** `pre-processor-service`  
**Purpose:** Document preprocessing and transformation service.
**Base API Path:** `/api/v1/preprocessing`

**Package Structure:**
```
com.tracemindai.preprocessor
├── config/
│   └── PreProcessorConfig.java (processing threads, timeout)
├── controller/
│   └── PreprocessorController.java
├── service/
│   └── PreprocessingService.java
├── repository/
│   └── ProcessingRepository.java
├── entity/
│   └── ProcessingJob.java
├── dto/
│   ├── PreprocessingRequest.java
│   └── PreprocessingResponse.java
├── exception/
│   └── PreprocessingException.java
└── util/
    └── ProcessingUtil.java
```

**Dependencies:** common-lib, spring-boot-starter-validation, commons-io

---

### 4. email-service
**Artifact:** `email-service`  
**Purpose:** Handles email sending and notifications.
**Base API Path:** `/api/v1/emails`

**Package Structure:**
```
com.tracemindai.email
├── config/
│   └── EmailConfig.java (sender email, retries, retry delay)
├── controller/
│   └── EmailController.java
├── service/
│   └── EmailService.java
├── repository/
│   └── EmailRepository.java
├── entity/
│   └── EmailLog.java
├── dto/
│   ├── EmailRequest.java
│   └── EmailResponse.java
├── exception/
│   └── EmailException.java
└── util/
    └── EmailValidationUtil.java
```

**Dependencies:** common-lib, spring-boot-starter-mail, spring-boot-starter-validation

---

### 5. print-service
**Artifact:** `print-service`  
**Purpose:** Handles document printing and formatting.
**Base API Path:** `/api/v1/print`

**Package Structure:**
```
com.tracemindai.print
├── config/
│   └── PrintConfig.java (output dir, max concurrent prints, default page size)
├── controller/
│   └── PrintController.java
├── service/
│   └── PrintService.java
├── repository/
│   └── PrintJobRepository.java
├── entity/
│   └── PrintJob.java
├── dto/
│   ├── PrintRequest.java
│   └── PrintResponse.java
├── exception/
│   └── PrintException.java
└── util/
    └── PrintValidationUtil.java
```

**Dependencies:** common-lib, spring-boot-starter-validation, commons-io

---

### 6. archival-service
**Artifact:** `archival-service`  
**Purpose:** Handles document archival and retrieval.
**Base API Path:** `/api/v1/archives`

**Package Structure:**
```
com.tracemindai.archival
├── config/
│   └── ArchivalConfig.java (archive dir, retention days, max archive size)
├── controller/
│   └── ArchivalController.java
├── service/
│   └── ArchivalService.java
├── repository/
│   └── ArchiveRepository.java
├── entity/
│   └── Archive.java
├── dto/
│   ├── ArchiveRequest.java
│   └── ArchiveResponse.java
├── exception/
│   └── ArchivalException.java
└── util/
    └── ArchiveUtil.java
```

**Dependencies:** common-lib, spring-boot-starter-validation, commons-io

---

## Key Design Patterns

### 1. Module Independence
- Each service module is independently deployable
- All services inherit from parent POM
- All services depend on common-lib

### 2. Standard Package Structure (per service)
- **config/** - Configuration classes with @ConfigurationProperties
- **controller/** - REST endpoint definitions (empty placeholders)
- **service/** - Business logic containers
- **repository/** - Data access abstraction
- **entity/** - Domain models
- **dto/** - Request/Response objects with validation annotations
- **exception/** - Custom service-specific exceptions
- **util/** - Helper utilities

### 3. Exception Hierarchy
```
ApplicationException (base with errorCode, httpStatus)
├── ValidationException (BAD_REQUEST)
├── ResourceNotFoundException (NOT_FOUND)
├── FileUploadException (BAD_REQUEST)
├── PreprocessingException (INTERNAL_SERVER_ERROR)
├── EmailException (INTERNAL_SERVER_ERROR)
├── PrintException (INTERNAL_SERVER_ERROR)
└── ArchivalException (INTERNAL_SERVER_ERROR)
```

### 4. DTOs and Validation
- All DTOs use Lombok for boilerplate reduction
- Jakarta Validation annotations for input validation
- Immutable with @Builder pattern

### 5. Configuration Management
- Each service has a @ConfigurationProperties class
- Configurable via application.yml under `app.<service>` prefix

---

## Build Configuration

### Compiler Settings
- **Source:** Java 21
- **Target:** Java 21
- **Release:** 21
- **Encoding:** UTF-8

### Plugin Configuration
- **maven-compiler-plugin (3.11.0):** Java 21 compilation
- **spring-boot-maven-plugin:** Builds executable JARs, excludes Lombok from runtime

### Dependency Management
- Parent POM uses `<dependencyManagement>` for version control
- Spring Boot and Spring Cloud BOMs imported
- All module versions inherit from parent

---

## Notes

1. **No Business Logic Implemented:** All service methods are placeholder stubs
2. **No Database Configuration:** Repositories are interface stubs only
3. **No Kafka/Messaging:** Not included per requirements
4. **No Docker:** Not included per requirements
5. **No Tests:** Test skeleton not created per requirements
6. **No API Implementations:** Controller methods are empty shells

---

## Getting Started

### Build All Modules
```bash
mvn clean install
```

### Build Individual Module
```bash
mvn clean install -pl file-upload-service -am
```

### Run Tests (when added)
```bash
mvn test
```

### Package for Deployment
```bash
mvn clean package
```

---

## Dependency Tree
```
TracemindAI (pom)
├── common-lib
├── file-upload-service → common-lib
├── pre-processor-service → common-lib
├── email-service → common-lib
├── print-service → common-lib
└── archival-service → common-lib
```

All business services depend on common-lib for shared components.
