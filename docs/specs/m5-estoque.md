# M5 — Estoque (Inventory)

Source: ERP MVP doc §6. Context package: `br.gravita.inventory`.

## Purpose

Real-time stock control with optional lot/serial traceability. The user can see the current balance of any product from any screen, without opening the inventory module.

## Functional requirements

### 6.1 Movimentações

- Movement types: Entry (purchase, customer return), Exit (sale, supplier return), Adjustment, Transfer.
- Inventory adjustment: positive or negative, with a mandatory justification; generates an automatic accounting entry.
- Transfer: between warehouses or branches; generates in-transit stock until the destination confirms.
- Reservation: quantity reserved for confirmed orders, unavailable for a new sale.
- Average cost (CMV): updated on every entry; cost history by date available for reports.
- Traceability: lot with expiry date, and serial number — bidirectional tracking (sale ↔ purchase).
- History: immutable log of every movement per product, with date, time and user.

### 6.2 Alertas e Controle

- Min stock: alert on the dashboard and product screen when balance ≤ configured minimum.
- Validity: alert for lots expiring within X days (configurable); the system never sells an expired lot.
- Reorder point: automatic purchase-order suggestion when balance hits the reorder point.
- Inventory count: partial (by group) or total physical count; automatic adjustment after approval.
- Negative stock: configurable block — allow or block an exit with no available balance.

## Domain model

- **StockBalance** (aggregate root, one per `product × warehouse/branch [× lot]`) — `onHand`, `reserved`, `inTransit`, `averageCost: Money` (CMV), `available` (derived: `onHand - reserved`). Invariant: `available` can't go negative unless negative stock is explicitly allowed for the product/warehouse.
- **StockMovement** (immutable, append-only) — `type: {ENTRY, EXIT, ADJUSTMENT, TRANSFER}`, `product`, `quantity`, `unitCost`, `sourceWarehouse`/`destinationWarehouse` (transfers), `lot`/`serial` (optional), `justification` (mandatory for adjustments), `originReference` (sale, purchase, return...), `user`, `timestamp`.
- **Lot** — `code`, `expiryDate`, `product`, quantities by warehouse. Invariant: never allocatable to a sale exit once `expiryDate` has passed.
- **SerialUnit** — `serialNumber`, `product`, current status/location; tracked individually rather than by quantity.
- **StockReservation** — `orderRef`, `product`, `quantity`, released on order cancellation, consumed on fulfillment.
- **PhysicalCount (Inventário)** — `scope: {PARTIAL_BY_GROUP, TOTAL}`, counted quantities vs. system quantities, `status: {IN_PROGRESS, PENDING_APPROVAL, APPROVED}`. Approval generates `ADJUSTMENT` movements for every divergence.

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m5-estoque/`](m5-estoque/README.md), for granular phase planning.

| Use case | Responsibility |
|---|---|
| [`GetStockBalanceUseCase`](m5-estoque/uc-01-get-stock-balance.md) | Read current balance for a product (any warehouse or a specific one) — the query other modules embed in their own screens. |
| [`RegisterStockEntryUseCase`](m5-estoque/uc-02-register-stock-entry.md) / [`RegisterStockExitUseCase`](m5-estoque/uc-03-register-stock-exit.md) | Driven by purchase receipt / sale confirmation. |
| [`AdjustInventoryUseCase`](m5-estoque/uc-04-adjust-inventory.md) | Manual positive/negative adjustment with justification. |
| [`TransferStockUseCase`](m5-estoque/uc-05-transfer-stock.md) | Between warehouses/branches, with in-transit tracking and destination confirmation. |
| [`ReserveStockUseCase`](m5-estoque/uc-06-reserve-stock.md) / [`ReleaseStockReservationUseCase`](m5-estoque/uc-07-release-stock-reservation.md) | Tied to order approval/cancellation. |
| [`StartPhysicalCountUseCase`](m5-estoque/uc-08-start-physical-count.md) / [`ApprovePhysicalCountUseCase`](m5-estoque/uc-09-approve-physical-count.md) | Count workflow, auto-generating adjustments on approval. |
| [`CheckExpiringLotsUseCase`](m5-estoque/uc-10-check-expiring-lots.md) | Feeds the expiry alert. |
| [`SuggestReorderUseCase`](m5-estoque/uc-11-suggest-reorder.md) | Feeds M6's purchase suggestion when balance hits the reorder point. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `StockBalanceRepositoryPort`, `StockMovementRepositoryPort`, `LotRepositoryPort`, `SerialUnitRepositoryPort`, `PhysicalCountRepositoryPort` | Persistence. |
| `NotifyLowStockPort`, `NotifyExpiringLotPort` | Feed the M9 dashboard and M10 alerting. |
| `PostAdjustmentAccountingEntryPort` | Automatic accounting entry on adjustment (into `finance`). |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `GET /api/inventory/products/{id}/balance` | `GetStockBalanceUseCase` |
| `POST /api/inventory/movements/entry` / `.../exit` | Entry/exit registration |
| `POST /api/inventory/adjustments` | `AdjustInventoryUseCase` |
| `POST /api/inventory/transfers` / `POST /api/inventory/transfers/{id}/confirm` | `TransferStockUseCase` |
| `POST /api/inventory/reservations` / `DELETE /api/inventory/reservations/{id}` | Reserve/release |
| `POST /api/inventory/counts` / `POST /api/inventory/counts/{id}/approve` | Physical count workflow |
| `GET /api/inventory/alerts/low-stock`, `GET /api/inventory/alerts/expiring-lots` | Alert queries |

### Outbound (`adapter.out.persistence`)

`StockBalanceJpaEntity`, `StockMovementJpaEntity` (append-only table, no update/delete), `LotJpaEntity`, `SerialUnitJpaEntity`, `PhysicalCountJpaEntity`.

## Cross-module dependencies

- **Consumes from `masterdata`**: product stock parameters (min/max/reorder point, lot/serial control flags).
- **Consumes from `tax` (M2/M3)**: confirmed inbound receipt → `RegisterStockEntryUseCase`; confirmed sale → `RegisterStockExitUseCase`.
- **Consumes from `sales` (M7)**: order approval/cancellation → reserve/release.
- **Provides to `purchasing` (M6)**: reorder suggestions.
- **Provides to `reporting` (M9)**: balances, CMV history, turnover, critical-stock list.
- **Provides to `finance` (M8)**: adjustment accounting entries.

## Notes

- `StockBalance` is modeled per lot when lot control is active on the product; when it isn't, the lot dimension collapses and balance is tracked per `product × warehouse` only. The doc doesn't spell out the exact aggregation key, so this is inferred from "rastreabilidade... ativável por produto" (doc §2.4/§6.1).
