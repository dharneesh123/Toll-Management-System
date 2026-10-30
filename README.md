# Toll Management System (FASTag)

Java source structure for toll plaza, vehicle, FASTag, payment, pass, and
dispute domain models and repositories.

## Source layout

```text
src/
└── com/
    └── toll/
        ├── models/
        │   ├── Admin.java
        │   ├── CrossingResult.java
        │   ├── Dispute.java
        │   ├── FASTagAccount.java
        │   ├── Lane.java
        │   ├── MonthlyPass.java
        │   ├── Payment.java
        │   ├── TollPlaza.java
        │   ├── TollRate.java
        │   ├── TollTransaction.java
        │   └── Vehicle.java
        └── repository/
            ├── AdminRepository.java
            ├── DisputeRepository.java
            ├── FASTagAccountRepository.java
            ├── MonthlyPassRepository.java
            ├── PaymentRepository.java
            ├── TollPlazaRepository.java
            ├── TollRateRepository.java
            ├── TollTransactionRepository.java
            └── VehicleRepository.java
```
