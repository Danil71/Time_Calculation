/* eslint-disable react-refresh/only-export-components */
import type { ReactNode } from 'react'; // Исправлена ошибка verbatimModuleSyntax
import { createContext, useContext, useState } from 'react';

interface AuthUser {
  username: string; // Изменено с email на username
  fullName: string;
  role: string;
}

interface AuthContextType {
  user: AuthUser | null;
  login: (token: string, username: string, fullName: string, role: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  
  // Ленивая инициализация: читаем localStorage один раз при создании компонента.
  // Это исправляет ошибку вызова setState внутри useEffect!
  const [user, setUser] = useState<AuthUser | null>(() => {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('userLogin');
    const role = localStorage.getItem('userRole');
    const fullName = localStorage.getItem('userFullName');

    if (token && username && role) {
      return { username, fullName: fullName || '', role };
    }
    return null;
  });

  const login = (token: string, username: string, fullName: string, role: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('userLogin', username);
    localStorage.setItem('userRole', role);
    localStorage.setItem('userFullName', fullName);
    setUser({ username, fullName, role });
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth должен использоваться внутри AuthProvider');
  return context;
};