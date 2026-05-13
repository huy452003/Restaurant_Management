type JwtExpiredHandler = () => void;

let handler: JwtExpiredHandler | null = null;

export function setJwtExpiredHandler(next: JwtExpiredHandler | null) {
  handler = next;
}

/** Gọi khi request có xác thực trả về 401 (JWT hết hạn / không hợp lệ). */
export function notifyJwtExpired() {
  handler?.();
}
