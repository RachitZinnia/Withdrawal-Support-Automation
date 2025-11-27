# Withdrawal Support Automation - Frontend

Modern React frontend for the Withdrawal Support Automation system.

## 🏗️ Technology Stack

- **React**: 18.2.0
- **Vite**: 5.0.0 (Build tool)
- **Axios**: HTTP client for API calls
- **Lucide React**: Icon library

## 🎨 Features

- 🎯 One-click case processing
- 📊 Real-time processing results
- 📈 Visual statistics dashboard
- 📋 Detailed case breakdown table
- 🎨 Modern, responsive design
- 🌈 Beautiful gradient UI
- ⚡ Fast loading with Vite
- 📱 Mobile-friendly interface

## 🚀 Quick Start

### Prerequisites

- Node.js 18+ and npm

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

The application will be available at `http://localhost:3000`

## 📁 Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── Header.jsx           # Application header
│   │   ├── Header.css
│   │   ├── CaseMonitoring.jsx   # Main case processing component
│   │   └── CaseMonitoring.css
│   ├── App.jsx                  # Main app component
│   ├── App.css
│   ├── main.jsx                 # Entry point
│   └── index.css                # Global styles
├── index.html
├── package.json
└── vite.config.js
```

## 🎨 Component Overview

### Header Component
- Displays application branding
- Gradient background with purple theme
- Responsive design

### CaseMonitoring Component
Main component that handles:
- Case processing trigger
- Loading states
- Results display
- Error handling
- Statistics visualization
- Detailed case table

## 🔌 API Integration

The frontend communicates with the backend via Axios:

**Endpoint:** `POST /api/cases/process-dataentry-waiting`

**Configuration:**
```javascript
// Vite proxy configuration in vite.config.js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

## 🎨 Design System

### Color Palette

```css
--primary-color: #2563eb      /* Blue */
--success-color: #10b981      /* Green */
--warning-color: #f59e0b      /* Orange */
--danger-color: #ef4444       /* Red */
--gray-*: Various gray shades
```

### Gradients

```css
/* Header gradient */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* Button gradient */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### Shadows

```css
--shadow-sm: Small shadow
--shadow-md: Medium shadow
--shadow-lg: Large shadow
--shadow-xl: Extra large shadow
```

## 📊 UI Components

### Process Button
- Gradient background
- Loading state with spinning icon
- Hover effects with elevation
- Disabled state

### Statistics Cards
- Color-coded by status
- Icon indicators
- Hover animations
- Responsive grid layout

### Case Details Table
- Sortable columns
- Status badges
- Manual review indicators
- Responsive design
- Hover effects

### Status Badges
- `PENDING` - Gray
- `IN_PROGRESS` - Blue
- `COMPLETED` - Green
- `FAILED` - Red
- `MANUAL_REVIEW_REQUIRED` - Orange

## 📱 Responsive Design

### Breakpoints

- **Desktop**: > 1024px
  - 4-column stats grid
  - Full table view
  
- **Tablet**: 641px - 1024px
  - 2-column stats grid
  - Horizontal scroll for table
  
- **Mobile**: ≤ 640px
  - Single column layout
  - Stacked stats cards
  - Optimized table view

## 🎯 User Flow

1. **Initial State**
   - Display "Process Cases" button
   - Empty results section

2. **Processing**
   - Button shows loading state
   - Spinner animation
   - Disabled interaction

3. **Results Display**
   - Statistics cards with counts
   - Summary message
   - Detailed case table

4. **Error State**
   - Error alert banner
   - Error message display

## 🔧 Configuration

### Vite Configuration

```javascript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
```

### Environment Variables

Create `.env` file (optional):
```
VITE_API_URL=http://localhost:8080
```

## 🚀 Build & Deployment

### Development Build

```bash
npm run dev
```

### Production Build

```bash
npm run build
```

Output will be in `dist/` directory.

### Preview Production Build

```bash
npm run preview
```

### Deploy to Static Hosting

The `dist/` folder can be deployed to:
- Netlify
- Vercel
- AWS S3 + CloudFront
- GitHub Pages
- Any static hosting service

**Example: Netlify**
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy
netlify deploy --prod --dir=dist
```

## 🎨 Customization

### Changing Color Scheme

Edit `src/index.css`:
```css
:root {
  --primary-color: #your-color;
  --primary-hover: #your-hover-color;
}
```

### Changing Header Gradient

Edit `src/components/Header.css`:
```css
.header {
  background: linear-gradient(135deg, #your-start 0%, #your-end 100%);
}
```

### Adding New Features

1. Create component in `src/components/`
2. Import in `App.jsx`
3. Add styles in component CSS file

## 🧪 Testing

### Manual Testing Checklist

- [ ] Process button triggers API call
- [ ] Loading state displays correctly
- [ ] Results render properly
- [ ] Error states show alerts
- [ ] Table displays all case details
- [ ] Status badges show correct colors
- [ ] Responsive design works on mobile
- [ ] Manual review flags display

## 📈 Performance Optimization

- Vite for fast builds
- Code splitting
- Lazy loading (can be added)
- Optimized images
- Minified production build

## 🔍 Troubleshooting

### API Connection Issues

```javascript
// Check proxy configuration in vite.config.js
// Verify backend is running on port 8080
```

### Build Errors

```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Port Already in Use

```bash
# Change port in vite.config.js
server: {
  port: 3001  // Use different port
}
```

## 🎨 Icons

Using Lucide React for icons:
```javascript
import { Play, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
```

Browse icons: https://lucide.dev/icons

## 📝 Code Style

- Use functional components
- Hooks for state management
- CSS modules for styling
- Descriptive variable names
- Comments for complex logic

## 🔄 Future Enhancements

- [ ] Add authentication
- [ ] Implement real-time updates (WebSocket)
- [ ] Add filtering and sorting
- [ ] Export results to CSV/Excel
- [ ] Add date range filtering
- [ ] Implement pagination for large datasets
- [ ] Add dark mode
- [ ] Add charts and visualizations





