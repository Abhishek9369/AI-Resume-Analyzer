# Java 25 Compatibility Issues - Testing Framework

## Problem Summary

The project currently uses **Java 25**, but the testing framework (Mockito + ByteBuddy) **only officially supports up to Java 22**. This causes all Mockito-based unit tests to fail during initialization.

### Error Message
```
Java 25 (69) is not supported by the current version of Byte Buddy which officially supports Java 22 (66) 
- update Byte Buddy or set net.bytebuddy.experimental as a VM property
```

## Impact Assessment

### ❌ **Affected Tests (All Mockito-based)**
- **JobQueueControllerTest** (14 tests) - Uses `@WebMvcTest`
- **FileUploadServiceTest** (14 tests) - Uses `@ExtendWith(MockitoExtension.class)`
- **JobQueueServiceTest** (20+ tests) - Uses `@ExtendWith(MockitoExtension.class)`
- **ResumeJobProcessorTest** (10+ tests) - Uses `@ExtendWith(MockitoExtension.class)`
- **All existing unit tests in the project**

### ✅ **What Still Works**
- Application compilation
- Application runtime (Java 25 is fine for running the app)
- Integration tests that don't use Mockito
- Manual testing

## Solutions (Ranked by Recommendation)

### 🎯 **Solution 1: Downgrade to Java 21 LTS** (RECOMMENDED)

**Why this is best:**
- Java 21 is the current Long-Term Support (LTS) version
- Full compatibility with Spring Boot 3.2.2
- Full compatibility with Mockito and all testing frameworks
- Production-ready and widely adopted
- You're already using Spring Boot 3.2.2 which targets Java 21

**How to implement:**
1. Download and install **Java 21 LTS** (e.g., from Adoptium, Oracle, or BellSoft Liberica)
2. Update `JAVA_HOME` environment variable to point to Java 21
3. Update IDE (VS Code/IntelliJ) to use Java 21 for the project
4. Update `pom.xml` if it explicitly specifies Java version:
   ```xml
   <properties>
       <java.version>21</java.version>
       <maven.compiler.source>21</maven.compiler.source>
       <maven.compiler.target>21</maven.compiler.target>
   </properties>
   ```
5. Rebuild project: `mvn clean install`
6. Run tests: `mvn test`

**Pros:**
- ✅ Complete solution - fixes all tests
- ✅ LTS support until 2029
- ✅ No experimental flags or workarounds
- ✅ Best production support

**Cons:**
- ⚠️ Requires Java installation change
- ⚠️ May need to update CI/CD pipelines

---

### ⚙️ **Solution 2: Use Experimental ByteBuddy Flag**

**How to implement:**

Add VM argument when running tests:
```bash
-Dnet.bytebuddy.experimental=true
```

**In Maven:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0-M9</version>
    <configuration>
        <argLine>-Dnet.bytebuddy.experimental=true</argLine>
    </configuration>
</plugin>
```

**In IDE (VS Code with Java Test Runner):**
Add to `.vscode/settings.json`:
```json
{
    "java.test.config": {
        "vmArgs": ["-Dnet.bytebuddy.experimental=true"]
    }
}
```

**Pros:**
- ✅ No Java version change needed
- ✅ Quick to implement
- ✅ May work for simple tests

**Cons:**
- ⚠️ **Experimental** - not officially supported
- ⚠️ ByteBuddy explicitly states Java 25 is unsupported
- ⚠️ May have subtle bugs or unexpected behavior
- ⚠️ Not recommended for production testing
- ⚠️ Future Mockito/ByteBuddy updates may break this

---

### 🔄 **Solution 3: Upgrade ByteBuddy Explicitly**

**How to implement:**

Add explicit ByteBuddy dependency to `pom.xml`:
```xml
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy</artifactId>
    <version>1.15.10</version> <!-- Check for latest version with Java 25 support -->
    <scope>test</scope>
</dependency>
```

**Status:** As of December 2024, ByteBuddy has experimental support for newer Java versions in bleeding-edge releases.

**Pros:**
- ✅ Addresses root cause
- ✅ Keeps Java 25

**Cons:**
- ⚠️ May cause version conflicts with Spring Boot's managed dependencies
- ⚠️ Requires monitoring ByteBuddy release notes
- ⚠️ Still experimental support for Java 25
- ⚠️ May break when Spring Boot updates its dependency versions

---

### 🛠️ **Solution 4: Rewrite Tests Without Mockito**

**How to implement:**

Replace `@WebMvcTest` with `@SpringBootTest`:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
class JobQueueControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private JobQueueRepository repository;
    
    @Test
    void testGetJobQueueHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/job-queue/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

For service tests, use test containers or in-memory databases instead of mocks.

**Pros:**
- ✅ More realistic integration testing
- ✅ No Mockito dependency
- ✅ Works with Java 25

**Cons:**
- ⚠️ Slower test execution
- ⚠️ Requires significant test rewriting
- ⚠️ Need test database setup
- ⚠️ More complex test setup

---

## Recommended Action Plan

### Immediate (Choose one):

**Option A - Production Ready (RECOMMENDED):**
1. ✅ Downgrade to Java 21 LTS
2. ✅ Run all tests: `mvn test`
3. ✅ Proceed with frontend validation

**Option B - Quick Experiment:**
1. ⚠️ Try experimental flag: `-Dnet.bytebuddy.experimental=true`
2. ⚠️ If tests pass, proceed cautiously
3. ⚠️ Plan migration to Java 21 later

### Why Java 21 Over Java 25?

| Factor | Java 21 | Java 25 |
|--------|---------|---------|
| **LTS Status** | ✅ LTS until 2029 | ❌ Not LTS (September 2025 release) |
| **Spring Boot Support** | ✅ Primary target | ⚠️ Cutting edge |
| **Testing Frameworks** | ✅ Full support | ❌ Limited/experimental |
| **Production Use** | ✅ Widely adopted | ⚠️ Early adopter |
| **Library Ecosystem** | ✅ Mature | ⚠️ Catching up |

## Testing the Fix

After switching to Java 21:

```bash
# Verify Java version
java -version

# Should show: openjdk version "21.0.x"

# Clean and rebuild
mvn clean install

# Run tests
mvn test

# Should see:
# Tests run: 58+, Failures: 0, Errors: 0, Skipped: 0
```

## Environment Variables (Windows)

To switch Java versions:

1. Install Java 21
2. Set system environment variable:
   ```
   JAVA_HOME=C:\Program Files\Java\jdk-21
   ```
3. Update PATH to include:
   ```
   %JAVA_HOME%\bin
   ```
4. Restart terminal/IDE
5. Verify: `java -version`

## IDE Configuration (VS Code)

Update `.vscode/settings.json`:
```json
{
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-21",
            "path": "C:\\Program Files\\Java\\jdk-21",
            "default": true
        }
    ]
}
```

## Maven Configuration

Ensure `pom.xml` specifies Java 21:
```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

## References

- **ByteBuddy Java Support:** https://github.com/raphw/byte-buddy/issues/1471
- **Spring Boot Java 21 Support:** https://spring.io/blog/2023/09/20/hello-java-21
- **Mockito Java Compatibility:** https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2#unmockable

## Current Test Status

| Test File | Tests | Status | Reason |
|-----------|-------|--------|--------|
| JobQueueServiceTest | 20+ | ❌ Failed | ByteBuddy incompatibility |
| ResumeJobProcessorTest | 10+ | ❌ Failed | ByteBuddy incompatibility |
| JobQueueControllerTest | 14 | ❌ Failed | ByteBuddy incompatibility |
| FileUploadServiceTest | 14 | ❌ Failed | ByteBuddy incompatibility |

**Total Impact:** 58+ tests blocked by Java 25 incompatibility

---

## Decision Required

**Question:** How would you like to proceed?

1. **Switch to Java 21 LTS** (recommended for production)
2. **Try experimental flag first** (quick test, less reliable)
3. **Keep Java 25 and rewrite tests** (most work)
4. **Other approach?**

Once you decide, I can help implement the chosen solution and get all tests passing.
