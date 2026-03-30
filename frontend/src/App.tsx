import { CssBaseline, ThemeProvider, Typography } from '@mui/material';
import { Navigate, Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import type { JSX } from 'react/jsx-runtime';
import Layout from './components/Layout';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import { darkTechTheme } from './theme'; // Файл темы, который мы делали ранее

// Защита маршрутов
const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
};

// Заглушка для Дашборда (напишем на Этапе 2)
const DashboardStub = () => <Typography variant="h4">Главная страница: Дашборд</Typography>;

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      
      {/* Все защищенные страницы оборачиваем в Layout */}
      <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<DashboardStub />} />
        {/* Здесь появятся другие роуты: /add, /project/:id и т.д. */}
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