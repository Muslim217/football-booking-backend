package com.football.booking.enums;

/**
 * Уровни верификации владельца площадки.
 *
 * UNVERIFIED      — только зарегистрировался
 * PHONE_VERIFIED  — подтвердил телефон
 * TRUSTED         — 5+ успешных бронирований, нет жалоб (выставляется автоматически)
 * ADMIN_APPROVED  — администратор вручную подтвердил площадку
 */
public enum VerificationStatus {
    UNVERIFIED,
    PHONE_VERIFIED,
    TRUSTED,
    ADMIN_APPROVED
}
