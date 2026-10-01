export interface DashboardSummary {
  totalSales: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
  lowStockCount: number;
  pendingOrdersCount: number;
  recentOrders: { orderNumber: string; customerEmail: string; status: string; grandTotal: number }[];
  bestSellers: { productName: string; reviewCount: number; averageRating: number }[];
}

export interface SalesReport {
  points: { periodLabel: string; revenue: number; orderCount: number }[];
  totalRevenue: number;
  totalOrders: number;
}

export interface OrderStatusReport {
  countsByStatus: Record<string, number>;
}

export interface InventoryRow {
  id: number;
  productVariantId: number;
  variantSku: string;
  productName: string;
  quantityAvailable: number;
  quantityReserved: number;
  sellable: number;
  lowStockThreshold: number;
  isLowStock: boolean;
}

export interface Coupon {
  id: number;
  code: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  minimumOrderAmount: number;
  maxRedemptions: number | null;
  maxRedemptionsPerUser: number;
  redemptionsCount: number;
  validFrom: string;
  validUntil: string;
  isActive: boolean;
}

export interface CustomerSummary {
  id: number;
  email: string;
  fullName: string;
  status: string;
  createdAt: string;
  orderCount: number;
}
