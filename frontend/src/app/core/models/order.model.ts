export interface OrderItem {
  id: number;
  productName: string;
  sku: string;
  variantLabel: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface OrderTimelineEntry {
  status: string;
  label: string;
  timestamp: string | null;
  completed: boolean;
}

export type OrderStatus =
  | 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'REFUNDED';

export interface Order {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  items: OrderItem[];
  subtotal: number;
  discountTotal: number;
  shippingFee: number;
  taxTotal: number;
  grandTotal: number;
  currency: string;
  placedAt: string;
  cancellable: boolean;
  timeline: OrderTimelineEntry[];
}

export interface CheckoutRequest {
  shippingAddressId: number;
  billingAddressId: number;
  couponCode?: string;
  idempotencyKey: string;
}
