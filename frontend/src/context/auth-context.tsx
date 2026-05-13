"use client";

import {
  createContext,
  useCallback,
  useContext,
  useLayoutEffect,
  useMemo,
  useState,
} from "react";
import type { UserLoginModel, UserRole } from "@/lib/api/types";
import { apiFetch } from "@/lib/api/client";
import { clearClientSessionStorage } from "@/lib/auth/clear-client-session";
import { setJwtExpiredHandler } from "@/lib/auth/jwt-expired";

type AuthState = {
  user: UserLoginModel | null;
  loading: boolean;
};

type AuthContextValue = AuthState & {
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setUserFromStorage: () => void;
  hasRole: (...roles: UserRole[]) => boolean;
};

const AuthContext = createContext<AuthContextValue | null>(null);

const STORAGE_USER = "userProfile";

function readUser(): UserLoginModel | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(STORAGE_USER);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserLoginModel;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserLoginModel | null>(null);
  const [loading, setLoading] = useState(true);

  const setUserFromStorage = useCallback(() => {
    const u = readUser();
    const token = typeof window !== "undefined" ? window.localStorage.getItem("accessToken") : null;
    if (u && token) setUser(u);
    else setUser(null);
  }, []);

  useLayoutEffect(() => {
    queueMicrotask(() => {
      setUserFromStorage();
      setLoading(false);
    });
  }, [setUserFromStorage]);

  useLayoutEffect(() => {
    setJwtExpiredHandler(() => {
      clearClientSessionStorage();
      setUser(null);
      const path = `${window.location.pathname}${window.location.search}`;
      const onLogin = path === "/login" || path.startsWith("/login?");
      const next = !onLogin ? encodeURIComponent(path) : "";
      window.location.replace(next ? `/login?next=${next}` : "/login");
    });
    return () => setJwtExpiredHandler(null);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const res = await apiFetch<UserLoginModel>("/users/login", {
      auth: false,
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
    const data = res.data;
    window.localStorage.setItem("accessToken", data.accessToken);
    window.localStorage.setItem("refreshToken", data.refreshToken);
    window.localStorage.setItem(STORAGE_USER, JSON.stringify(data));
    setUser(data);
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiFetch<string>("/users/logout", { method: "POST" });
    } catch {
      /* ignore */
    } finally {
      clearClientSessionStorage();
      setUser(null);
    }
  }, []);

  const hasRole = useCallback(
    (...roles: UserRole[]) => {
      if (!user) return false;
      return roles.includes(user.role);
    },
    [user],
  );

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      logout,
      setUserFromStorage,
      hasRole,
    }),
    [user, loading, login, logout, setUserFromStorage, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
