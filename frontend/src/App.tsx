import { CssBaseline, ThemeProvider } from '@mui/material';
import { Navigate, Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import type { JSX } from 'react/jsx-runtime';
import Layout from './components/Layout';
import { AuthProvider, useAuth } from './context/AuthContext';
import AddProject from './pages/AddProject';
import AdminPanel from './pages/AdminPanel';
import Dashboard from './pages/Dashboard';
import LanguagesPage from './pages/LanguagesPage';
import Login from './pages/Login';
import ProjectDetails from './pages/ProjectDetails';
import Register from './pages/Register';
import Team from './pages/Team';
import { darkTechTheme } from './theme'; // Файл темы, который мы делали ранее

// Защита маршрутов
const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
};

const AdminRoute = ({ children }: { children: JSX.Element }) => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
};

const AdminOrManagerRoute = ({ children }: { children: JSX.Element }) => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== 'ADMIN' && user.role !== 'MANAGER') return <Navigate to="/" replace />;
  return children;
};


function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      
      {/* Все защищенные страницы оборачиваем в Layout */}
      <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />
        <Route path="add" element={<AddProject />} />
        <Route path="project/:id" element={<ProjectDetails />} />
        <Route path="project/:id/team" element={<Team />} />
        <Route path="admin" element={<AdminRoute><AdminPanel /></AdminRoute>} />
        <Route path="languages" element={<AdminOrManagerRoute><LanguagesPage /></AdminOrManagerRoute>} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <ThemeProvider theme={darkTechTheme}>
      <CssBaseline />
      <AuthProvider>
        <Router>
          <AppRoutes />
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
}