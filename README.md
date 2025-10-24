# Cash Desk Module

A Spring Boot application for managing cash operations (deposits, withdrawals, and balance checks) for multiple bank cashiers handling BGN and EUR currencies with denomination tracking.

**Interview Assignment** for Senior Software Engineer position at First Investment Bank (Fibank).

---

## Features

- **Cash Operations**: Deposit and withdrawal operations with denomination tracking
- **Balance Queries**: Query balances with optional date range and cashier filters
- **Multi-Currency**: BGN and EUR support
- **Multi-Cashier**: MARTINA, PETER, LINDA
- **Transaction History**: Complete audit trail of all operations
- **Backup & Recovery**: Automated backups with manual backup/restore API
- **Idempotency Support**: Prevent duplicate transactions
- **Authentication**: API key-based security
- **Health Checks & Monitoring**: Spring Actuator with custom file system health indicators

---

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Postman (for testing)

---

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd CashDeskModule
mvn clean install
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

Data files are stored in `~/.cashdesk/data/` by default.

---

## API Overview

### Authentication

All requests require an authentication header:

### Main Endpoints

**1. Cash Operation** - `POST /api/v1/cash-operation`

Processes deposits and withdrawals.

```json
{
  "operationType": "DEPOSIT",
  "cashier": "MARTINA",
  "currency": "BGN",
  "amount": 600.00,
  "denominations": {
    "10": 10,
    "50": 10
  }
}
```

**2. Balance Query** - `GET /api/v1/cash-balance`

Returns balances with optional filters: `?cashier=MARTINA&dateFrom=2024-10-20T00:00:00Z&dateTo=2024-10-24T23:59:59Z`

**3. Backup Management** - `/api/v1/admin/backup`
- `POST /backup` - Create manual backup
- `GET /backup` - List all backups
- `POST /backup/restore/{backupName}` - Restore from backup
- `GET /backup/verify/{backupName}` - Verify backup integrity

**4. Health & Monitoring** - `/actuator`
- `GET /actuator/health` - Overall application health status
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics

---

## Testing with Postman

### Import Collection

The repository includes Postman files in the `postman/` directory:

1. Open Postman
2. Click **Import**
3. Select both files:
   - `CashDeskModule.postman_collection.json`
   - `CashDeskModule.postman_environment.json`
4. Select the **Cash Desk Module - Local** environment

### Test Scenarios Included

**Basic Operations:**
- Deposits and withdrawals for all cashiers
- Balance queries (all cashiers, specific cashier, date range filters)

**Advanced Features:**
- Idempotency tests (duplicate request prevention)
- Backup/restore operations

**Error Cases:**
- Invalid authentication
- Insufficient funds
- Invalid denominations

### Initial State

Each cashier starts with:
- **BGN**: 1000 (50×10 + 10×50)
- **EUR**: 2000 (100×10 + 20×50)

---

## Technical Details

### Architecture

- **Layered Design**: Controller → Service → Repository → Domain
- **Design Patterns**: Strategy (operation handlers), Repository, DTO
- **Concurrency**: Per-cashier read-write locks for thread safety
- **Storage**: File-based (TXT format) in `~/.cashdesk/data/`

### Key Features

**Data Storage:**
- `transactions.txt` - Append-only audit trail (pipe-delimited)
- `balances.txt` - Current state snapshot (atomic writes)
- Automatic backups to `~/.cashdesk/backups/` (daily at 2 AM)

**Idempotency:**
- `Idempotency-Key` header prevents duplicate transactions
- 24-hour cache (configurable)
- Thread-safe concurrent request handling

**Technologies:**
- Java 17, Spring Boot 3.5.6, Maven
- SLF4J logging, Jakarta Validation
- File-based persistence (no database)

**Health Checks & Monitoring:**
- Spring Boot Actuator for operational monitoring
- Custom health indicators for file system checks:
  - **Balance File Health**: Monitors balance file accessibility, disk space, and file integrity
  - **Transaction File Health**: Monitors transaction file accessibility, transaction count, and file size
  - **Disk Space Health**: Built-in check for available disk space (10MB minimum threshold)
- Health endpoint returns detailed status including:
  - File readability and writability
  - Available disk space
  - File sizes and transaction counts
  - Warnings for unusually large files

---

