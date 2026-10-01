export interface WishlistItem {
  id: number;
  productId: number;
  productName: string;
  productSlug: string;
  imageUrl: string | null;
  price: number;
  discountPrice: number | null;
  inStock: boolean;
}

export interface Wishlist {
  id: number;
  items: WishlistItem[];
}

export interface Review {
  id: number;
  productId: number;
  reviewerName: string;
  rating: number;
  title: string | null;
  body: string | null;
  createdAt: string;
}

export interface ReviewRequest {
  orderItemId: number;
  rating: number;
  title?: string;
  body?: string;
}

export interface AppNotification {
  id: number;
  type: string;
  title: string;
  body: string | null;
  isRead: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
}
