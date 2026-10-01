export interface ProductImage {
  id: number;
  url: string;
  altText: string | null;
  displayOrder: number;
}

export interface ProductVariant {
  id: number;
  sku: string;
  size: string | null;
  color: string | null;
  colorHex: string | null;
  price: number;
  inStock: boolean;
  availableQuantity: number;
}

export interface ProductSummary {
  id: number;
  sku: string;
  slug: string;
  name: string;
  brandName: string | null;
  categoryName: string;
  price: number;
  discountPrice: number | null;
  currency: string;
  primaryImageUrl: string | null;
  averageRating: number;
  reviewCount: number;
  isBestSeller: boolean;
  isNewArrival: boolean;
  inStock: boolean;
}

export interface ProductDetail {
  id: number;
  sku: string;
  slug: string;
  name: string;
  description: string;
  price: number;
  discountPrice: number | null;
  currency: string;
  brandName: string | null;
  categoryName: string;
  categorySlug: string;
  status: string;
  averageRating: number;
  reviewCount: number;
  isBestSeller: boolean;
  isNewArrival: boolean;
  images: ProductImage[];
  variants: ProductVariant[];
  availableSizes: string[];
  availableColors: string[];
}

export type ProductSortOption = 'NEWEST' | 'PRICE_ASC' | 'PRICE_DESC' | 'RATING' | 'POPULARITY';

export interface ProductFilter {
  categorySlug?: string;
  brand?: string[];
  size?: string[];
  color?: string[];
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  inStockOnly?: boolean;
  search?: string;
  sortBy?: ProductSortOption;
  page?: number;
  pageSize?: number;
}
