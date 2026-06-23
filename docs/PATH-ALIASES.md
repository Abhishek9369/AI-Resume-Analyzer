# Path Aliases Configuration

This document describes the path alias configuration for the React frontend application.

## Configured Path Aliases

The following path aliases are configured in both `tsconfig.json` and `vite.config.ts`:

| Alias | Resolves To | Usage |
|-------|-------------|-------|
| `@/*` | `./src/*` | General imports from src directory |
| `@components/*` | `./src/components/*` | React components |
| `@services/*` | `./src/services/*` | API and GraphQL services |
| `@store/*` | `./src/store/*` | Redux store, slices, and sagas |
| `@pages/*` | `./src/pages/*` | Page-level components |

## Usage Examples

### Before (Relative Paths)
```typescript
import { graphqlClient } from '../../services/graphql'
import { uploadResumes } from '../../services/api'
import { fetchCandidates } from '../slices/candidatesSlice'
import type { Candidate } from '../slices/candidatesSlice'
```

### After (Path Aliases)
```typescript
import { graphqlClient } from '@services/graphql'
import { uploadResumes } from '@services/api'
import { fetchCandidates } from '@store/slices/candidatesSlice'
import type { Candidate } from '@store/slices/candidatesSlice'
```

## Benefits

1. **Cleaner Imports**: No more counting `../../` levels
2. **Refactoring Safety**: Moving files doesn't break imports
3. **Better Readability**: Clear indication of where imports come from
4. **IDE Support**: Better autocomplete and navigation

## Configuration Files

### tsconfig.json
```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"],
      "@components/*": ["./src/components/*"],
      "@services/*": ["./src/services/*"],
      "@store/*": ["./src/store/*"],
      "@pages/*": ["./src/pages/*"]
    }
  }
}
```

### vite.config.ts
```typescript
export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@components': path.resolve(__dirname, './src/components'),
      '@services': path.resolve(__dirname, './src/services'),
      '@store': path.resolve(__dirname, './src/store'),
      '@pages': path.resolve(__dirname, './src/pages'),
    },
  },
})
```

## Import Patterns

### Components
```typescript
import { Header } from '@components/Header'
import { Button } from '@components/Button'
```

### Services
```typescript
import { graphqlClient } from '@services/graphql'
import { uploadResumes } from '@services/api'
```

### Store
```typescript
import { fetchCandidates } from '@store/slices/candidatesSlice'
import { RootState } from '@store'
import type { Candidate } from '@store/slices/candidatesSlice'
```

### Pages
```typescript
import Dashboard from '@pages/Dashboard/Dashboard'
import CandidateList from '@pages/CandidateList/CandidateList'
```

### General (using @/)
```typescript
import styles from '@/pages/Dashboard/Dashboard.module.css'
import { RootState } from '@/store'
```

## Notes

- Both TypeScript and Vite need to be configured for aliases to work properly
- TypeScript uses `paths` in tsconfig.json for type checking
- Vite uses `alias` in vite.config.ts for bundling
- The `@/` prefix is a general fallback for any src imports
- Specific aliases (`@components`, `@services`, etc.) provide better semantics

## Adding New Aliases

To add a new alias:

1. Add to `tsconfig.json` paths:
   ```json
   "@newalias/*": ["./src/newalias/*"]
   ```

2. Add to `vite.config.ts` alias:
   ```typescript
   '@newalias': path.resolve(__dirname, './src/newalias')
   ```

3. Restart your development server and TypeScript server in your IDE
