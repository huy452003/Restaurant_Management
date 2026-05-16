export type UserRole =
  | "ADMIN"
  | "CUSTOMER"
  | "MANAGER"
  | "CHEF"
  | "CASHIER";

export type Gender = "MALE" | "FEMALE";

export type OrderType = "DINE_IN" | "DELIVERY";

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PREPARING"
  | "COMPLETED"
  | "CANCELLED";

export type MenuItemStatus = "AVAILABLE" | "OUT_OF_STOCK" | "DISCONTINUED";

export type UserStatus = string;

export interface ApiResponse<T> {
  statusCode: number;
  message: string;
  modelName: string | null;
  errors: Record<string, unknown> | null;
  data: T;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface UserLoginModel {
  id?: number;
  username: string;
  fullname: string;
  email: string;
  phone: string;
  gender: Gender;
  birth?: string;
  age?: number;
  address?: string;
  role: UserRole;
  userStatus: UserStatus;
  accessToken: string;
  expires: string;
  refreshToken: string;
  refreshExpires: string;
}

export interface MenuItemModel {
  id: number;
  name: string;
  description?: string;
  price: string;
  image: string;
  categoryName: string;
  menuItemStatus: MenuItemStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface CategoryModel {
  id: number;
  name: string;
  description?: string;
  image: string;
  categoryStatus: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface OrderModel {
  id: number;
  orderNumber: string;
  customerName?: string;
  customerPhone?: string;
  customerEmail?: string;
  tableNumber?: number | null;
  waiterId?: number;
  orderStatus: OrderStatus;
  orderType: OrderType;
  subTotal?: string;
  tax?: string;
  totalAmount?: string;
  totalOrderItem?: number;
  /** Backend: còn có thể tạo thanh toán mới cho đơn này */
  canAcceptPayment?: boolean;
  /** Backend: trạng thái được phép chọn tiếp (pipeline + thanh toán). */
  allowedOrderStatuses?: OrderStatus[];
  notes?: string;
  completedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReservationModel {
  id: number;
  customerName?: string;
  customerPhone?: string;
  customerEmail?: string;
  tableNumber: number;
  reservationTs: string;
  numberOfGuests: number;
  reservationStatus: string;
  specialRequest?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserModel {
  id: number;
  username: string;
  fullname: string;
  email: string;
  phone: string;
  gender: Gender;
  birth?: string;
  age?: number;
  address?: string;
  role: UserRole;
  userStatus: UserStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface ShiftModel {
  id: number;
  employeeId: number;
  shiftDate: string;
  startTime: string;
  endTime: string;
  totalWorkingHours?: number;
  shiftStatus: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TableModel {
  id: number;
  tableNumber: number;
  capacity: number;
  tableStatus: string;
  location?: string;
}

export interface PaymentModel {
  id: number;
  orderNumber?: string;
  cashierFullname?: string;
  paymentMethod: string;
  amount: string;
  paymentStatus: string;
  transactionId?: string;
  paidAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface VnpayCheckoutResponse {
  paymentUrl: string;
  payment: PaymentModel;
}

export interface OrderItemModel {
  id: number;
  orderNumber: string;
  menuItemName: string;
  quantity: number;
  unitPrice: string;
  subTotal: string;
  specialInstructions?: string;
  orderItemStatus: string;
}
