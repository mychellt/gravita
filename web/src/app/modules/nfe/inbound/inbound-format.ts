/** Chave de acesso da NF-e: exatamente 44 dígitos (BR1 — obrigatória inclusive no lançamento manual). */
export const ACCESS_KEY_PATTERN = /^\d{44}$/;

export const onlyDigits = (value: string): string => value.replace(/\D/g, '');
