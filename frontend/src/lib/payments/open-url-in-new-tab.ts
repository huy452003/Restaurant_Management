/**
 * Mở URL sang tab mới mà không điều hướng tab hiện tại.
 * Không dùng window.open(..., "noopener") rồi kiểm tra null — trình duyệt vẫn mở tab
 * nhưng trả về null, dễ gây fallback location.assign trùng tab.
 */
export function openUrlInNewTab(url: string): boolean {
  if (typeof window === "undefined" || !url) return false;

  const popup = window.open(url, "_blank");
  if (popup) {
    popup.opener = null;
    return true;
  }

  const form = document.createElement("form");
  form.method = "GET";
  form.action = url;
  form.target = "_blank";
  form.rel = "noopener noreferrer";
  form.style.display = "none";
  document.body.appendChild(form);
  form.submit();
  form.remove();
  return true;
}
