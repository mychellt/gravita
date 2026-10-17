/** Chave de acesso da NF-e: exatamente 44 dígitos (BR1 — obrigatória inclusive no lançamento manual). */
export const ACCESS_KEY_PATTERN = /^\d{44}$/;

export const onlyDigits = (value: string): string => value.replace(/\D/g, '');

const unitValueFormat = new Intl.NumberFormat('pt-BR', {
  style: 'currency', currency: 'BRL', minimumFractionDigits: 2, maximumFractionDigits: 4,
});

/** Valor unitário em reais; preserva até 4 casas, como a nota traz (ex.: R$ 1,5000 → R$ 1,50, R$ 1,2345 → R$ 1,2345). */
export const formatUnitValue = (value: number): string => unitValueFormat.format(value);

/** Variação com sinal explícito (+R$ 0,30 / −R$ 1,00), para o aviso de mudança de preço. */
export const formatDelta = (delta: number): string => `${delta > 0 ? '+' : '−'}${formatUnitValue(Math.abs(delta))}`;
