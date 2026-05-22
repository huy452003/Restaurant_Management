"use client";

type Props = {
  id?: string;
  value: number;
  onChange: (guests: number) => void;
  capacity: number | null;
  disabled?: boolean;
};

export function ReservationGuestsInput({
  id = "reservation-guests",
  value,
  onChange,
  capacity,
  disabled = false,
}: Props) {
  const max = capacity ?? undefined;
  const invalid = capacity != null && value > capacity;

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-stone-700">
        Số khách
      </label>
      <input
        id={id}
        type="number"
        min={1}
        max={max}
        value={value}
        disabled={disabled || capacity == null}
        onChange={(e) => {
          const raw = Number(e.target.value);
          if (!Number.isFinite(raw)) return;
          onChange(Math.max(1, Math.floor(raw)));
        }}
        className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25 disabled:cursor-not-allowed disabled:bg-stone-50"
      />
      {capacity == null ? (
        <p className="mt-1 text-xs text-muted">Chọn bàn trước để nhập số khách.</p>
      ) : (
        <p className={`mt-1 text-xs ${invalid ? "text-red-600" : "text-muted"}`}>
          Tối đa {capacity} khách
        </p>
      )}
    </div>
  );
}
