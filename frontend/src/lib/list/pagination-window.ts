/** Trả về tối đa `size` chỉ số trang (0-based) quanh trang hiện tại. */
export function getVisiblePageIndices(current: number, totalPages: number, size = 5): number[] {
  if (totalPages <= 0) return [];
  if (totalPages <= size) {
    return Array.from({ length: totalPages }, (_, i) => i);
  }

  const half = Math.floor(size / 2);
  let start = current - half;
  if (start < 0) start = 0;
  if (start + size > totalPages) start = totalPages - size;

  return Array.from({ length: size }, (_, i) => start + i);
}
