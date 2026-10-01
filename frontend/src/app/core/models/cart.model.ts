export interface CartItem {
  id: number;
  productVariantId: number;
  productName: string;
  productSlug: string;
  imageUrl: string | null;
  size: string | null;
  color: string | null;
  unitPrice: number;
  priceChanged: boolean;
  quantity: number;
  lineTotal: number;
  inStock: boolean;
  availableQuantity: number;
}

export interface Cart {
  cartId: number;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
}
