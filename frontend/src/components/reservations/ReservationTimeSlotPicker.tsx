"use client";

type Props = {
  id?: string;
  /** Chỉ các khung còn chọn được (đã lọc booked + quá khứ). */
  times: string[];
  loading: boolean;
  error: string | null;
  selectedTime: string | null;
  onSelectTime: (time: string | null) => void;
  disabled?: boolean;
};

export function ReservationTimeSlotPicker({
  id = "reservation-time-slot",
  times,
  loading,
  error,
  selectedTime,
  onSelectTime,
  disabled = false,
}: Props) {
  const selectDisabled = disabled || loading || times.length === 0;

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-stone-700">
        Khung giờ
      </label>
      <select
        id={id}
        value={selectedTime ?? ""}
        disabled={selectDisabled}
        onChange={(e) => {
          const v = e.target.value;
          onSelectTime(v === "" ? null : v);
        }}
        className="mt-1 w-full appearance-none rounded-lg border border-stone-300 bg-white bg-[length:1rem] bg-[position:right_0.75rem_center] bg-no-repeat px-3 py-2.5 pr-10 text-sm text-stone-900 outline-none transition focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20 disabled:cursor-not-allowed disabled:bg-stone-50 disabled:text-stone-400"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2378716c'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'/%3E%3C/svg%3E")`,
        }}
      >
        <option value="" disabled>
          {loading ? "Đang tải khung giờ…" : times.length === 0 ? "Không còn khung trống" : "Chọn khung giờ"}
        </option>
        {times.map((time) => (
          <option key={time} value={time}>
            {time}
          </option>
        ))}
      </select>
      {error ? <p className="mt-1.5 text-sm text-red-700">{error}</p> : null}
      {!error && !loading && times.length === 0 ? (
        <p className="mt-1.5 text-xs text-muted">
          Không còn khung giờ cho bàn và ngày này. Chọn ngày khác hoặc bàn khác.
        </p>
      ) : null}
    </div>
  );
}
