# Toll Management System (FASTag)

A dependency-free Java in-memory domain service for toll plazas, lanes,
vehicle and FASTag accounts, toll processing, passes, payments, disputes, and reports.

## Source layout

```text
src/
└── com/
    └── toll/
        ├── Main.java
        ├── TollManagementSystem.java
        └── models/
            ├── Admin.java
            ├── CrossingResult.java
            ├── Dispute.java
            ├── FASTagAccount.java
            ├── Lane.java
            ├── MonthlyPass.java
            ├── Payment.java
            ├── TollPlaza.java
            ├── TollRate.java
            ├── TollTransaction.java
            └── Vehicle.java
```

## Run the demo

From the repository root:

```powershell
javac -d out src\com\toll\*.java src\com\toll\models\*.java
java -cp out com.toll.Main
```

The demo creates a plaza, configures effective-dated rates, registers a vehicle
and tag, and processes a FASTag crossing. Data is held in memory and is lost
when the process exits; persistence, payment-provider integration, and identity
verification are outside this starter implementation.

Run the dependency-free behavior checks:

```powershell
javac -d out src\com\toll\*.java src\com\toll\models\*.java test\com\toll\*.java
java -cp out com.toll.TollManagementSystemTest
```
