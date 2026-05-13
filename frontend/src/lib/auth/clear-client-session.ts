const STORAGE_KEYS = ["accessToken", "refreshToken", "userProfile"] as const;

export function clearClientSessionStorage() {
  if (typeof window === "undefined") return;
  for (const k of STORAGE_KEYS) {
    window.localStorage.removeItem(k);
  }
}
