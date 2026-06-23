# Logo Design - Resume Analyzer

## Overview

The Resume Analyzer logo is a professional SVG design that represents the core functionality of the application: AI-powered resume analysis and candidate matching.

## Design Elements

### Main Components

1. **Document/Resume Shape** (Center)
   - Navy blue document icon (`#1e3a8a`)
   - Represents resume/CV processing
   - Contains horizontal lines simulating text content
   - Rounded corners for modern aesthetic

2. **AI Neural Network** (Bottom Right)
   - Green neural network nodes (`#10b981`, `#34d399`)
   - Interconnected nodes representing AI intelligence
   - Central node with connections showing data processing
   - Golden sparkle indicating AI insights (`#fbbf24`)

3. **Success Checkmark** (Top Left)
   - Green checkmark badge (`#10b981`)
   - Represents successful candidate matching
   - White check icon for clarity

4. **Decorative Elements**
   - Subtle circuit pattern for tech theme
   - Light blue accent elements
   - Background circle with low opacity

### Color Palette

| Color | Hex | Purpose |
|-------|-----|---------|
| Navy Blue | `#1e3a8a` | Primary - Document, professionalism |
| Sky Blue | `#3b82f6`, `#60a5fa` | Accent - Technology, trust |
| Green | `#10b981`, `#34d399` | Success - AI matching, positive results |
| Gold | `#fbbf24` | Highlight - Intelligence, insight |
| White | `#ffffff` | Contrast - Clarity |

## Technical Specifications

- **Format:** SVG (Scalable Vector Graphics)
- **Dimensions:** 200x200 viewBox
- **File Size:** ~2.8 KB
- **Location:** `src/main/frontend/public/logo.svg`
- **Usage:** Favicon and application branding

## File Locations

### Source
```
src/main/frontend/public/logo.svg
```

### Build Output
```
src/main/frontend/dist/logo.svg           # Frontend build
target/classes/static/logo.svg            # Maven build
/app/app.jar/BOOT-INF/classes/static/     # Docker container (inside JAR)
```

## Access URLs

- **Development:** http://localhost:8080/logo.svg
- **Production (HTTPS):** https://localhost/logo.svg
- **Favicon:** Automatically loaded in browser tab

## Usage in Application

### HTML Reference
```html
<link rel="icon" type="image/svg+xml" href="/logo.svg" />
```

Located in: `src/main/frontend/index.html`

### Direct Access
The logo is served as a static resource by Spring Boot and is accessible at the root path `/logo.svg`.

## Build Process

### Frontend Build
```bash
cd src/main/frontend
yarn build
# Copies logo.svg from public/ to dist/
```

### Maven Build
```bash
mvn clean package
# Copies frontend dist/ to target/classes/static/
```

### Docker Build
```bash
cd docker
docker-compose build app
# Includes logo in Spring Boot JAR
```

## Customization

To customize the logo:

1. **Edit the SVG file:**
   ```bash
   # Open in any text editor or SVG editor
   code src/main/frontend/public/logo.svg
   ```

2. **Rebuild frontend:**
   ```bash
   cd src/main/frontend
   yarn build
   ```

3. **Rebuild Docker container:**
   ```bash
   cd docker
   docker-compose build --no-cache app
   docker-compose up -d app
   ```

## Design Rationale

### Why These Elements?

1. **Document Icon**
   - Primary function: Resume processing
   - Immediately recognizable
   - Professional appearance

2. **Neural Network**
   - Represents AI/ML capabilities
   - Modern tech aesthetic
   - Shows intelligent processing

3. **Checkmark**
   - Success indicator
   - Positive user experience
   - Quality assurance

4. **Color Choices**
   - Blue: Trust, professionalism, technology
   - Green: Success, growth, positive outcomes
   - Gold: Innovation, intelligence, premium service

### Target Audience
- **HR Professionals:** Professional, trustworthy branding
- **Recruiters:** Modern, efficient technology
- **Technical Users:** AI/ML capabilities highlighted

## Browser Compatibility

The SVG logo works across all modern browsers:
- ✅ Chrome/Edge (Chromium)
- ✅ Firefox
- ✅ Safari
- ✅ Opera
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

### Favicon Support
- **Modern browsers:** Native SVG favicon support
- **Older browsers:** Automatic fallback to rendered icon
- **High DPI displays:** Perfect clarity at any scale

## Accessibility

- **Semantic SVG:** Proper structure for screen readers
- **High contrast:** Meets WCAG 2.1 Level AA standards
- **Scalable:** Clear at all sizes (16px to 512px)

## Alternative Formats

If needed, you can generate additional formats:

### PNG Export
```bash
# Using Inkscape (install first)
inkscape logo.svg -w 512 -h 512 -o logo-512.png

# Or use online converter:
# https://cloudconvert.com/svg-to-png
```

### ICO Format (for older browsers)
```bash
# Using ImageMagick
convert logo.svg -background none -define icon:auto-resize=16,32,48,64,256 favicon.ico
```

## Version History

- **v1.0** (Feb 16, 2026) - Initial design
  - Document-centric design
  - AI neural network elements
  - Success checkmark
  - Professional color scheme

## Credits

- **Design:** AI-generated for Resume Analyzer project
- **Created:** February 16, 2026
- **Updated:** February 16, 2026
- **License:** Part of Resume Analyzer application

---

**Current Status:** ✅ Deployed and active  
**Location:** https://localhost/logo.svg  
**Favicon:** Active in browser tabs
