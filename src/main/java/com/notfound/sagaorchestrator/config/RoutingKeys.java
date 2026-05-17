package com.notfound.sagaorchestrator.config;

public final class RoutingKeys {

    private RoutingKeys() {
    }

    public static final String ORDER_CREATE_COMMAND = "order.create.command";
    public static final String ORDER_CONFIRM_COMMAND = "order.confirm.command";
    public static final String ORDER_CANCEL_COMMAND = "order.cancel.command";
    public static final String BOOK_STOCK_RESERVE_COMMAND = "book.stock.reserve.command";
    public static final String BOOK_STOCK_CONFIRM_COMMAND = "book.stock.confirm.command";
    public static final String BOOK_STOCK_RELEASE_COMMAND = "book.stock.release.command";
    public static final String PROMOTION_RESERVE_COMMAND = "promotion.reserve.command";
    public static final String PROMOTION_CONFIRM_COMMAND = "promotion.confirm.command";
    public static final String PROMOTION_RELEASE_COMMAND = "promotion.release.command";
    public static final String PAYMENT_CREATE_COMMAND = "payment.create.command";
    public static final String PAYMENT_REFUND_COMMAND = "payment.refund.command";
    public static final String SHIPPING_CREATE_COMMAND = "shipping.create.command";
    public static final String SHIPPING_CANCEL_COMMAND = "shipping.cancel.command";
    public static final String CART_CLEAR_COMMAND = "cart.clear.command";

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_FAILED = "order.failed";
    public static final String BOOK_STOCK_RESERVED = "book.stock.reserved";
    public static final String BOOK_STOCK_CONFIRMED = "book.stock.confirmed";
    public static final String BOOK_STOCK_RELEASED = "book.stock.released";
    public static final String BOOK_STOCK_FAILED = "book.stock.failed";
    public static final String PROMOTION_RESERVED = "promotion.reserved";
    public static final String PROMOTION_CONFIRMED = "promotion.confirmed";
    public static final String PROMOTION_RELEASED = "promotion.released";
    public static final String PROMOTION_FAILED = "promotion.failed";
    public static final String PAYMENT_CREATED = "payment.created";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
    public static final String SHIPPING_CREATED = "shipping.created";
    public static final String SHIPPING_CANCELLED = "shipping.cancelled";
    public static final String SHIPPING_FAILED = "shipping.failed";
    public static final String CART_CLEARED = "cart.cleared";
    public static final String CHECKOUT_COMPLETED = "checkout.completed";
    public static final String CHECKOUT_FAILED = "checkout.failed";
}

