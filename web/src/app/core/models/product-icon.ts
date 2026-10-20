/** Chave semântica do ícone do produto, como o backend a devolve (ProductIcon). */
export type ProductIcon =
  | 'PAINT' | 'FLOORING' | 'PLUMBING' | 'SANITARY' | 'ELECTRICAL' | 'TOOLS' | 'FASTENERS' | 'CEMENT'
  | 'WOOD' | 'ADHESIVE' | 'LOCKSMITH' | 'SAFETY' | 'CLEANING' | 'GARDEN' | 'DEFAULT';

const EMOJI: Record<ProductIcon, string> = {
  PAINT: '🪣',
  FLOORING: '🔲',
  PLUMBING: '🚰',
  SANITARY: '🚽',
  ELECTRICAL: '💡',
  TOOLS: '🔨',
  FASTENERS: '🔩',
  CEMENT: '🧱',
  WOOD: '🪵',
  ADHESIVE: '🧴',
  LOCKSMITH: '🔑',
  SAFETY: '🦺',
  CLEANING: '🧹',
  GARDEN: '🌱',
  DEFAULT: '📦',
};

/** Emoji do produto; chave ausente ou desconhecida (backend antigo) cai no emoji genérico. */
export function productIconEmoji(icon: string | null | undefined): string {
  return EMOJI[icon as ProductIcon] ?? EMOJI.DEFAULT;
}
