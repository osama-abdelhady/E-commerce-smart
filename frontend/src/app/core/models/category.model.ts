export interface Category {
  id: number;
  slug: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  parentId: number | null;
  children: Category[];
}

export interface Brand {
  id: number;
  slug: string;
  name: string;
  logoUrl: string | null;
}
