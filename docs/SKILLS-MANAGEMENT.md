# Skills Management Feature

## Overview
This feature implements a comprehensive skills management system with autocomplete and badge-based UI for job requirements. The system maintains a master table of skills and provides a GraphQL API for skill search and selection.

## Features Implemented

### Backend Changes

1. **New Entity: Skill**
   - Location: `src/main/java/io/subbu/ai/firedrill/entities/Skill.java`
   - Fields: id, name, category, description, isActive, createdAt, updatedAt
   - Many-to-many relationship with JobRequirement via join table

2. **Repository: SkillRepository**
   - Location: `src/main/java/io/subbu/ai/firedrill/repos/SkillRepository.java`
   - Methods:
     - `findByIsActive()` - Find all active skills
     - `findByNameIgnoreCase()` - Find skill by exact name
     - `searchByName()` - Search skills with autocomplete (case-insensitive)
     - `findByCategory()` - Find skills by category
     - `findAllCategories()` - Get all unique categories

3. **GraphQL Resolver: SkillResolver**
   - Location: `src/main/java/io/subbu/ai/firedrill/resolver/SkillResolver.java`
   - **Queries:**
     - `skill(id)` - Get skill by ID
     - `allSkills()` - Get all skills
     - `activeSkills()` - Get all active skills
     - `searchSkills(name)` - Search skills by name pattern
     - `skillsByCategory(category)` - Get skills by category
     - `skillCategories()` - Get all unique categories
   - **Mutations:**
     - `createSkill(name, category, description)` - Create new skill
     - `updateSkill(id, name, category, description, isActive)` - Update skill
     - `deleteSkill(id)` - Delete skill

4. **Updated JobRequirement Entity**
   - Added `@ManyToMany` relationship with Skills
   - Join table: `job_requirement_skills`
   - Added both `requiredSkills` (text field for backward compatibility) and `skills` (collection)

5. **Updated JobRequirementResolver**
   - Location: `src/main/java/io/subbu/ai/firedrill/resolver/JobRequirementResolver.java`
   - Modified `createJobRequirement()` to accept `skillIds` parameter
   - Modified `updateJobRequirement()` to accept `skillIds` parameter
   - Automatically loads and associates skills from the master table

6. **GraphQL Schema Updates**
   - Location: `src/main/resources/graphql/schema.graphqls`
   - Added `Skill` type with all fields
   - Added skill queries and mutations
   - Updated `JobRequirement` type to include `skills` field
   - Updated mutations to accept individual parameters instead of input objects

### Frontend Changes

1. **SkillsInput Component**
   - Location: `src/main/frontend/src/components/SkillsInput/`
   - Features:
     - Real-time autocomplete search as you type
     - Badge-based skill display with gradient styling
     - Keyboard navigation (Arrow keys, Enter, Escape)
     - Category display for each skill (optional)
     - Remove skills by clicking × button
     - Prevents duplicate selections
     - Responsive design with smooth animations

2. **Updated JobRequirements Page**
   - Location: `src/main/frontend/src/pages/JobRequirements/JobRequirements.tsx`
   - **Changes:**
     - Integrated SkillsInput component in create/edit form
     - Replaced text area with interactive skill selector
     - Displays selected skills as badges in job cards
     - Handles skill search via GraphQL API
     - Submits `skillIds` array when creating/updating jobs
   - **Features:**
     - Real-time skill search with debouncing
     - Visual separation of skills by category
     - Fallback to `requiredSkills` text for backward compatibility

3. **GraphQL Service Updates**
   - Location: `src/main/frontend/src/services/graphql.ts`
   - **Added Queries:**
     - `SEARCH_SKILLS` - Search skills by name pattern
     - `GET_ACTIVE_SKILLS` - Get all active skills
     - `CREATE_SKILL` - Create new skill (admin feature)
   - **Updated Queries:**
     - `GET_ALL_JOBS` - Now includes skills with id, name, category
     - `CREATE_JOB` - Accepts skillIds parameter
     - `UPDATE_JOB` - Accepts skillIds parameter

4. **Redux State Updates**
   - Location: `src/main/frontend/src/store/slices/jobsSlice.ts`
   - Updated `JobRequirement` interface to include `skills?: Skill[]`
   - Added `Skill` interface with id, name, category fields

5. **Styling**
   - Location: `src/main/frontend/src/components/SkillsInput/SkillsInput.module.css`
   - Gradient skill badges with purple/blue theme
   - Smooth fade-in animations
   - Hover effects on suggestions
   - Category pills with semi-transparent background

### Database

1. **Schema:**
   - `skills` table with columns: id, name, category, description, is_active, created_at, updated_at
   - `job_requirement_skills` join table with columns: job_requirement_id, skill_id

2. **Initial Skills Data:**
   - Location: `docker/init-skills.sql`
   - Pre-populated with 70+ commonly used technical skills
   - **Categories include:**
     - **Programming Languages:** Java, Python, JavaScript, TypeScript, C#, C++, Go, Kotlin, Scala, Ruby, PHP, Swift
     - **Frameworks:** Spring Boot, Spring Framework, React, Angular, Vue.js, Node.js, Express.js, Django, Flask, .NET Core, ASP.NET, Hibernate, JPA
     - **Databases:** PostgreSQL, MySQL, MongoDB, Oracle Database, Microsoft SQL Server, Redis, Cassandra, DynamoDB, Elasticsearch
     - **Cloud Platforms:** AWS, Azure, Google Cloud Platform, Docker, Kubernetes, Terraform
     - **Tools & Technologies:** Git, Jenkins, Maven, Gradle, GraphQL, REST API, Microservices, JUnit, Mockito, Agile, Scrum
     - **Web Technologies:** HTML5, CSS3, SASS, Webpack, Vite, Redux

3. **Docker Integration:**
   - Updated `docker/docker-compose.yml` to mount `init-skills.sql`
   - Skills data loaded automatically on first database initialization
   - Order: 01-init-db.sql → 02-init-skills.sql

## Usage

### Creating/Editing a Job Requirement

1. Navigate to Job Requirements page
2. Click "Create New Job" or "Edit" on an existing job
3. Fill in job details (title, experience range, education, etc.)
4. **In the "Required Skills" field:**
   - Start typing a skill name (minimum 2 characters)
   - Select from autocomplete suggestions that appear
   - Selected skills appear as colorful badges above the input
   - Skills show their category when available
   - Click the × on a badge to remove it
   - Continue adding more skills
5. Fill in other fields (education, domain, description)
6. Click "Create Job" or "Update Job"

### Viewing Job Requirements

- Skills are displayed as badges on job cards
- Each badge shows the skill name and category
- Badges use a gradient purple/blue theme
- Fallback to text display for jobs without structured skills

### GraphQL API Examples

**Search Skills:**
```graphql
query {
  searchSkills(name: "java") {
    id
    name
    category
    description
  }
}
```

**Create Job with Skills:**
```graphql
mutation {
  createJobRequirement(
    title: "Senior Java Developer"
    description: "We are looking for an experienced Java developer..."
    skillIds: ["uuid1", "uuid2", "uuid3"]
    minExperience: 5
    maxExperience: 10
    requiredEducation: "Bachelor's in Computer Science"
    domain: "Fintech"
  ) {
    id
    title
    skills {
      id
      name
      category
    }
    minExperience
    maxExperience
  }
}
```

**Update Job with Skills:**
```graphql
mutation {
  updateJobRequirement(
    id: "existing-job-uuid"
    skillIds: ["new-uuid1", "new-uuid2"]
    isActive: true
  ) {
    id
    title
    skills {
      id
      name
      category
    }
  }
}
```

**Get All Active Skills:**
```graphql
query {
  activeSkills {
    id
    name
    category
  }
}
```

## Database Schema

### Skills Table
```sql
CREATE TABLE skills (
    id UUID PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    category VARCHAR,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Join Table
```sql
CREATE TABLE job_requirement_skills (
    job_requirement_id UUID REFERENCES job_requirements(id),
    skill_id UUID REFERENCES skills(id),
    PRIMARY KEY (job_requirement_id, skill_id)
);
```

## Loading Initial Skills Data

To populate the database with initial skills:

```bash
# If using Docker
docker exec -i <postgres-container> psql -U <username> -d <database> < docker/init-skills.sql

# Or directly via psql
psql -h localhost -U username -d database -f docker/init-skills.sql
```

## Future Enhancements

1. Skill synonyms (e.g., "JS" → "JavaScript")
2. Skill proficiency levels (Beginner, Intermediate, Expert)
3. Auto-extraction of skills from job descriptions
4. Skill trending and analytics
5. Bulk import of skills from CSV
6. Skill suggestions based on job title/description
