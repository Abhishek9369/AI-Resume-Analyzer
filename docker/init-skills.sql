-- Skills Master Data
-- Insert initial set of commonly used technical skills

-- Programming Languages
INSERT INTO skills (id, name, category, description, is_active, created_at, updated_at) VALUES
(gen_random_uuid(), 'Java', 'Programming Language', 'Object-oriented programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Python', 'Programming Language', 'High-level programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'JavaScript', 'Programming Language', 'Web programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'TypeScript', 'Programming Language', 'Typed superset of JavaScript', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'C#', 'Programming Language', '.NET programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'C++', 'Programming Language', 'Systems programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Go', 'Programming Language', 'Google programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Kotlin', 'Programming Language', 'Modern JVM language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Scala', 'Programming Language', 'Functional JVM language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Ruby', 'Programming Language', 'Dynamic programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'PHP', 'Programming Language', 'Server-side scripting language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Swift', 'Programming Language', 'Apple programming language', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Frameworks
INSERT INTO skills (id, name, category, description, is_active, created_at, updated_at) VALUES
(gen_random_uuid(), 'Spring Boot', 'Framework', 'Java application framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Spring Framework', 'Framework', 'Java enterprise framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'React', 'Framework', 'JavaScript UI library', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Angular', 'Framework', 'TypeScript web framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Vue.js', 'Framework', 'Progressive JavaScript framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Node.js', 'Framework', 'JavaScript runtime', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Express.js', 'Framework', 'Node.js web framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Django', 'Framework', 'Python web framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Flask', 'Framework', 'Python micro-framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), '.NET Core', 'Framework', 'Microsoft application framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'ASP.NET', 'Framework', 'Microsoft web framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Hibernate', 'Framework', 'Java ORM framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'JPA', 'Framework', 'Java Persistence API', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Databases
INSERT INTO skills (id, name, category, description, is_active, created_at, updated_at) VALUES
(gen_random_uuid(), 'PostgreSQL', 'Database', 'Relational database', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'MySQL', 'Database', 'Relational database', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'MongoDB', 'Database', 'NoSQL document database', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Oracle Database', 'Database', 'Enterprise relational database', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Microsoft SQL Server', 'Database', 'Microsoft relational database', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Redis', 'Database', 'In-memory data store', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Cassandra', 'Database', 'Distributed NoSQL database', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'DynamoDB', 'Database', 'AWS NoSQL database', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Elasticsearch', 'Database', 'Search and analytics engine', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Cloud Platforms
INSERT INTO skills (id, name, category, description, is_active, created_at, updated_at) VALUES
(gen_random_uuid(), 'AWS', 'Cloud Platform', 'Amazon Web Services', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Azure', 'Cloud Platform', 'Microsoft cloud platform', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Google Cloud Platform', 'Cloud Platform', 'Google cloud services', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Docker', 'Cloud Platform', 'Container platform', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Kubernetes', 'Cloud Platform', 'Container orchestration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Terraform', 'Cloud Platform', 'Infrastructure as Code', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Tools & Technologies
INSERT INTO skills (id, name, category, description, is_active, created_at, updated_at) VALUES
(gen_random_uuid(), 'Git', 'Tool', 'Version control system', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Jenkins', 'Tool', 'CI/CD automation', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Maven', 'Tool', 'Java build tool', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Gradle', 'Tool', 'Build automation tool', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'GraphQL', 'Tool', 'Query language for APIs', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'REST API', 'Tool', 'RESTful web services', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Microservices', 'Architecture', 'Microservices architecture', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'JUnit', 'Tool', 'Java testing framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Mockito', 'Tool', 'Java mocking framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Agile', 'Methodology', 'Agile development methodology', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Scrum', 'Methodology', 'Scrum framework', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Web Technologies
INSERT INTO skills (id, name, category, description, is_active, created_at, updated_at) VALUES
(gen_random_uuid(), 'HTML5', 'Web Technology', 'Latest HTML standard', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'CSS3', 'Web Technology', 'Latest CSS standard', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'SASS', 'Web Technology', 'CSS preprocessor', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Webpack', 'Web Technology', 'Module bundler', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Vite', 'Web Technology', 'Frontend build tool', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Redux', 'Web Technology', 'State management library', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
