package com.toll.models;

public final class TollTypes {
    private TollTypes() { }

    public enum VehicleType { CAR, VAN, BUS, TRUCK, MULTI_AXLE }
    public enum LaneType { FASTAG, CASH, EMERGENCY }
    public enum LaneStatus { OPEN, CLOSED }
    public enum AccountStatus { ACTIVE, BLOCKED, CLOSED }
    public enum TripType { SINGLE, RETURN }
    public enum TransactionStatus { SUCCESS, FAILED, PENDING }
    public enum PaymentStatus { PAID, FAILED, REFUNDED }
    public enum PaymentMethod { UPI, CARD, NET_BANKING, CASH, FASTAG }
    public enum PaymentType { RECHARGE, CASH_TOLL, PASS, REFUND }
}
