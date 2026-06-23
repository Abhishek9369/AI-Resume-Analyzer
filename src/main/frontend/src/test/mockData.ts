export const mockCandidate = {
  id: '550e8400-e29b-41d4-a716-446655440000',
  name: 'John Doe',
  email: 'john.doe@example.com',
  mobile: '+1234567890',
  skills: 'Java, Spring Boot, React, PostgreSQL',
  experience: 8,
  education: "Master's Degree in Computer Science",
  currentCompany: 'Tech Corp',
  summary: 'Senior Software Engineer with 8 years of experience in E-commerce and Cloud Computing',
  createdAt: '2024-01-15T10:30:00Z',
};

export const mockCandidates = [
  mockCandidate,
  {
    id: '661e9511-f39c-52e5-b827-557766551111',
    name: 'Jane Smith',
    email: 'jane.smith@example.com',
    mobile: '+1987654321',
    skills: 'Python, Django, React, MongoDB',
    experience: 5,
    education: "Bachelor's Degree in Engineering",
    currentCompany: 'Healthcare Inc',
    summary: 'Full Stack Developer with 5 years of experience in Healthcare and Data Analytics',
    createdAt: '2024-01-16T14:20:00Z',
  },
];

export const mockJobRequirement = {
  id: '772fa622-g4ad-63f6-c938-668877662222',
  title: 'Senior Java Developer',
  description: 'Looking for experienced Java developer',
  requiredSkills: 'Java, Spring Boot, Microservices, Docker, Kubernetes',
  minExperienceYears: 5,
  maxExperienceYears: 10,
  requiredEducation: "Master's Degree in Computer Science",
  domainRequirements: 'E-commerce, Fintech',
  isActive: true,
  createdAt: '2024-01-10T09:00:00Z',
};

export const mockJobRequirements = [
  mockJobRequirement,
  {
    id: '883gb733-h5be-74g7-d049-779988773333',
    title: 'Python Backend Engineer',
    description: 'Seeking Python specialist for backend development',
    requiredSkills: 'Python, FastAPI, PostgreSQL, Redis, Docker',
    minExperienceYears: 3,
    maxExperienceYears: 7,
    requiredEducation: "Bachelor's Degree",
    domainRequirements: 'Healthcare, ML/AI',
    isActive: true,
    createdAt: '2024-01-12T11:00:00Z',
  },
];

export const mockCandidateMatch = {
  id: '994hc844-i6cf-85h8-e15a-88aa99884444',
  candidateId: mockCandidate.id,
  jobRequirementId: mockJobRequirement.id,
  overallScore: 85.5,
  matchScore: 85.5,
  skillsScore: 90.0,
  experienceScore: 85.0,
  educationScore: 80.0,
  domainScore: 87.0,
  explanation: 'Excellent match with strong technical skills and relevant domain knowledge',
  isShortlisted: true,
  isSelected: false,
  createdAt: '2024-01-17T15:45:00Z',
};

export const mockProcessTracker = {
  id: 'aa5id955-j7dg-96i9-f26b-99bb00995555',
  status: 'COMPLETED' as const,
  totalFiles: 1,
  processedFiles: 1,
  failedFiles: 0,
  message: 'Resume processed successfully',
  uploadedFilename: 'resume_john_doe.pdf',
  createdAt: '2024-01-15T10:25:00Z',
  updatedAt: '2024-01-15T10:30:00Z',
  completedAt: '2024-01-15T10:30:00Z',
};

export const mockUploadResponse = {
  trackerId: mockProcessTracker.id,
  message: 'File uploaded successfully',
};

