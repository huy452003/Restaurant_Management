import { apiFetch } from "@/lib/api/client";
import type {
  ReservationAdminRequestModel,
  ReservationModel,
} from "@/lib/api/types";

export async function cancelReservationAsStaff(id: number): Promise<void> {
  await apiFetch<ReservationModel>(`/reservations/cancel/${id}`, { method: "PATCH" });
}

export async function updateReservationByAdmin(
  id: number,
  update: ReservationAdminRequestModel,
): Promise<void> {
  await apiFetch<ReservationModel[]>("/reservations/admin", {
    method: "PUT",
    body: JSON.stringify({ ids: [id], updates: [update] }),
  });
}
