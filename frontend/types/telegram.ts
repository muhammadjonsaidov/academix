// academix_tz.md §2.7 "barcha rollar uchun umumiy" — Telegram sub-resource shapes.
export interface TelegramLinkToken {
  linkUrl: string;
  expiresInSeconds: number;
}

export interface TelegramConnectionStatus {
  connected: boolean;
  telegramUsername: string | null;
}
