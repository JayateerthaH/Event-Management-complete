# CampusEvents - Authentication App

A modern React authentication application built with Vite, featuring email/password authentication and Google OAuth integration.

## Features

- ✨ Modern UI with split-panel design
- 🔐 Email/Password authentication
- 🌐 Google OAuth integration
- 🎨 Role-based signup (Student, Organizer, Admin)
- 📱 Responsive design
- 🔒 JWT token-based authentication
- ⚡ Built with Vite for fast development

## Tech Stack

- **React 19** - UI library
- **React Router** - Navigation
- **Vite** - Build tool
- **Google OAuth** - Social authentication
- **JWT Decode** - Token handling

## Prerequisites

- Node.js (v16 or higher)
- npm, yarn, or pnpm
- Backend API running (default: http://localhost:8080)

## Setup

1. **Clone and install dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Configure environment variables:**
   ```bash
   cp .env.example .env
   ```
   
   Update `.env` with your values:
   ```env
   VITE_API_URL=http://localhost:8080
   VITE_GOOGLE_CLIENT_ID=your_google_client_id_here
   ```

3. **Start the development server:**
   ```bash
   npm run dev
   ```

4. **Open your browser:**
   Navigate to http://localhost:5173

## API Integration

### Backend Endpoints

The app expects these endpoints from your backend:

#### Signup
- **Endpoint:** `POST /auth/signup`
- **Request Body:**
  ```json
  {
    "username": "user@example.com",
    "password": "SecurePass123",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["STUDENT"]
  }
  ```
- **Response:**
  ```json
  {
    "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 123
  }
  ```

#### Login
- **Endpoint:** `POST /auth/login`
- **Request Body:**
  ```json
  {
    "username": "user@example.com",
    "password": "SecurePass123"
  }
  ```
- **Response:**
  ```json
  {
    "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 123
  }
  ```

### Role Types

The signup form supports three role types:
- `STUDENT` - Default role for students
- `ORGANIZER` - For event organizers
- `ADMIN` - For administrators

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   └── ProtectedRoute.jsx    # Route protection
│   ├── context/
│   │   └── AuthContext.jsx        # Auth state management
│   ├── pages/
│   │   ├── Login.jsx              # Login page
│   │   ├── Signup.jsx             # Signup page
│   │   └── Dashboard.jsx          # Protected dashboard
│   ├── styles/
│   │   ├── Auth.css               # Auth pages styles
│   │   └── Dashboard.css          # Dashboard styles
│   ├── App.jsx                    # Main app component
│   └── main.jsx                   # Entry point
├── .env.example                   # Environment template
└── package.json
```

## Available Scripts

```bash
npm run dev      # Start development server
npm run build    # Build for production
npm run preview  # Preview production build
npm run lint     # Run ESLint
```

## Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API
4. Go to Credentials → Create OAuth 2.0 Client ID
5. Add authorized JavaScript origins:
   - http://localhost:5173 (development)
   - Your production URL
6. Copy the Client ID to your `.env` file

## Authentication Flow

1. **Signup:**
   - User fills form with email, password, name, and role
   - Form validates input (email format, password strength)
   - Sends POST request to `/auth/signup`
   - Receives JWT token and user ID
   - Stores token in localStorage
   - Redirects to dashboard

2. **Login:**
   - User enters email and password
   - Sends POST request to `/auth/login`
   - Receives JWT token and user ID
   - Stores token in localStorage
   - Redirects to dashboard

3. **Protected Routes:**
   - `ProtectedRoute` component checks for valid user/token
   - Redirects to login if not authenticated

## Customization

### Update API URL
Change the backend URL in `.env`:
```env
VITE_API_URL=https://your-api-domain.com
```

### Modify Theme Colors
Edit CSS variables in `Auth.css`:
- Primary gradient: `#667eea` to `#764ba2`
- Dark background: `#1e2530`
- Component background: `#2b3544`

## Build for Production

```bash
npm run build
```

The built files will be in the `dist/` directory, ready to deploy to any static hosting service.

## Troubleshooting

**CORS Issues:**
- Ensure your backend allows requests from your frontend origin
- Check backend CORS configuration

**Google OAuth not working:**
- Verify Client ID is correct in `.env`
- Check authorized origins in Google Console
- Ensure you're using the correct domain (localhost vs 127.0.0.1)

**API connection fails:**
- Verify backend is running
- Check `VITE_API_URL` in `.env`
- Inspect network tab in browser DevTools

## License

MIT

