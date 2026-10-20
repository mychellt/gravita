/** Chave semântica do ícone do produto, como o backend a devolve (ProductIcon). */
export type ProductIcon =
  | 'PAINT' | 'FLOORING' | 'PLUMBING' | 'SANITARY' | 'ELECTRICAL' | 'TOOLS' | 'FASTENERS' | 'CEMENT'
  | 'WOOD' | 'ADHESIVE' | 'LOCKSMITH' | 'SAFETY' | 'CLEANING' | 'GARDEN' | 'DEFAULT';

const TABLER_ICON: Record<ProductIcon, string> = {
  PAINT: 'ti-paint',
  FLOORING: 'ti-layout-grid',
  PLUMBING: 'ti-droplet',
  SANITARY: 'ti-bath',
  ELECTRICAL: 'ti-bolt',
  TOOLS: 'ti-tool',
  FASTENERS: 'ti-nut',
  CEMENT: 'ti-wall',
  WOOD: 'ti-trees',
  ADHESIVE: 'ti-flask',
  LOCKSMITH: 'ti-lock',
  SAFETY: 'ti-shield-check',
  CLEANING: 'ti-spray',
  GARDEN: 'ti-plant',
  DEFAULT: 'ti-package',
};

/** Classe Tabler do ícone; chave ausente ou desconhecida (backend antigo) cai no ícone genérico. */
export function productIconClass(icon: string | null | undefined): string {
  return TABLER_ICON[icon as ProductIcon] ?? TABLER_ICON.DEFAULT;
}
