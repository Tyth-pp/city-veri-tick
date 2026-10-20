package com.city.result;

import com.city.constant.AccountMessageConstant;
import com.city.constant.ActivityMessageConstant;
import com.city.constant.AuthMessageConstant;
import com.city.constant.CommonMessageConstant;
import com.city.constant.CouponMessageConstant;
import com.city.constant.OrderMessageConstant;
import com.city.constant.TicketMessageConstant;

public enum SystemResultCode implements ResultCode {
    SUCCESS(200, CommonMessageConstant.SUCCESS),
    BAD_REQUEST(400, CommonMessageConstant.BAD_REQUEST),
    SESSION_TIME_INVALID(400, ActivityMessageConstant.SESSION_TIME_INVALID),
    SESSION_TIME_REQUIRED(400, ActivityMessageConstant.SESSION_TIME_REQUIRED),
    SESSION_CAPACITY_INVALID(400, ActivityMessageConstant.SESSION_CAPACITY_INVALID),
    SALE_TIME_REQUIRED(400, ActivityMessageConstant.SALE_TIME_REQUIRED),
    SALE_TIME_INVALID(400, ActivityMessageConstant.SALE_TIME_INVALID),
    SALE_TIME_AFTER_SESSION(400, ActivityMessageConstant.SALE_TIME_AFTER_SESSION),
    TICKET_FIELDS_INVALID(400, TicketMessageConstant.TICKET_FIELDS_INVALID),
    UNAUTHORIZED(401, CommonMessageConstant.UNAUTHORIZED),
    ACCOUNT_DISABLED(40102, AccountMessageConstant.ACCOUNT_DISABLED),
    SMS_CHANNEL_UNAVAILABLE(503, AuthMessageConstant.SMS_CHANNEL_UNAVAILABLE),
    SMS_SEND_TOO_FREQUENT(429, AuthMessageConstant.SMS_SEND_TOO_FREQUENT),
    SMS_ATTEMPTS_EXCEEDED(429, AuthMessageConstant.SMS_ATTEMPTS_EXCEEDED),
    SMS_VERIFICATION_BUSY(429, AuthMessageConstant.SMS_VERIFICATION_BUSY),
    SMS_CODE_INVALID(401, AuthMessageConstant.SMS_CODE_INVALID),
    LOGIN_CREDENTIALS_INVALID(401, AuthMessageConstant.LOGIN_CREDENTIALS_INVALID),
    REAL_NAME_REQUIRED(409, AccountMessageConstant.REAL_NAME_REQUIRED),
    ID_CARD_MISMATCH(409, AccountMessageConstant.ID_CARD_MISMATCH),
    FORBIDDEN(403, CommonMessageConstant.FORBIDDEN),
    NOT_FOUND(404, CommonMessageConstant.NOT_FOUND),
    CONFLICT(409, CommonMessageConstant.CONFLICT),
    ACCOUNT_DUPLICATE(409, AccountMessageConstant.ACCOUNT_DUPLICATE),
    PASSWORD_INVALID(400, AccountMessageConstant.PASSWORD_INVALID),
    PASSWORD_UNCHANGED(400, AccountMessageConstant.PASSWORD_UNCHANGED),
    ACTIVITY_SESSION_REQUIRED(409, ActivityMessageConstant.ACTIVITY_SESSION_REQUIRED),
    TICKET_TYPE_REQUIRED(409, TicketMessageConstant.TICKET_TYPE_REQUIRED),
    TICKET_TYPE_NAME_DUPLICATE(409, TicketMessageConstant.TICKET_TYPE_NAME_DUPLICATE),
    SESSION_CAPACITY_EXCEEDED(409, ActivityMessageConstant.SESSION_CAPACITY_EXCEEDED),
    TICKET_TYPE_HAS_ORDERS(409, TicketMessageConstant.TICKET_TYPE_HAS_ORDERS),
    TICKET_TYPE_NOT_ON_SALE(409, TicketMessageConstant.TICKET_TYPE_NOT_ON_SALE),
    SESSION_NOT_ON_SALE(409, ActivityMessageConstant.SESSION_NOT_ON_SALE),
    ACTIVITY_NOT_PUBLISHED(409, ActivityMessageConstant.ACTIVITY_NOT_PUBLISHED),
    TICKET_STOCK_NOT_ENOUGH(409, TicketMessageConstant.TICKET_STOCK_NOT_ENOUGH),
    TICKET_ALREADY_CHECKED_IN(409, TicketMessageConstant.TICKET_ALREADY_CHECKED_IN),
    COUPON_UNAVAILABLE(409, CouponMessageConstant.COUPON_UNAVAILABLE),
    COUPON_ALREADY_CLAIMED(409, CouponMessageConstant.COUPON_ALREADY_CLAIMED),
    COUPON_CLAIM_LIMIT(409, CouponMessageConstant.COUPON_CLAIM_LIMIT),
    COUPON_CLAIM_NOT_STARTED(409, CouponMessageConstant.COUPON_CLAIM_NOT_STARTED),
    COUPON_CLAIM_ENDED(409, CouponMessageConstant.COUPON_CLAIM_ENDED),
    COUPON_SOLD_OUT(409, CouponMessageConstant.COUPON_SOLD_OUT),
    COUPON_CLAIM_BUSY(503, CouponMessageConstant.COUPON_CLAIM_BUSY),
    ORDER_PAYMENT_EXPIRED(409, OrderMessageConstant.ORDER_PAYMENT_EXPIRED),
    ORDER_REQUEST_BUSY(409, OrderMessageConstant.ORDER_REQUEST_BUSY),
    ACTIVITY_QUERY_BUSY(503, ActivityMessageConstant.ACTIVITY_QUERY_BUSY),
    INTERNAL_ERROR(500, CommonMessageConstant.INTERNAL_ERROR);

    private final int code;
    private final String message;

    SystemResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
