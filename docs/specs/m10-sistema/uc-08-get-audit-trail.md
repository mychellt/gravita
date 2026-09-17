# UC-M10-08 — Get Audit Trail (`GetAuditTrailUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 8 — Robustez (doc §14)

## Functional requirement

§11.2 — "Trilha completa: Toda ação registrada: usuário, data, hora, tela, campo alterado, valor anterior e novo." / "Consulta histórico: Qualquer registro do sistema tem aba 'Histórico de alterações' acessível ao admin."

## Description

An administrator opens the "change history" tab for any record and sees every prior change (who, when, screen, field, old value, new value). Precondition: requester holds admin-level access; the target entity/record exists (or has existed — cancelled/deleted-in-spirit records still have history, per §11.2's fiscal immutability rule). Postcondition: none — read-only.

## Port signature

```java
public interface GetAuditTrailUseCase {
    Page<AuditTrailEntry> execute(GetAuditTrailQuery query);
}
```

`GetAuditTrailQuery`: `entity`, `entityId`, plus pagination.

## Outbound ports required

- `AuditTrailRepositoryPort`

## REST endpoint

`GET /api/system/audit-trail?entity=&id=`

## Domain entities touched

- `AuditTrailEntry` (read-only)

## Acceptance criteria

- [ ] Returns every recorded change for the given `(entity, entityId)`, ordered chronologically.
- [ ] Accessible only to admin (`system → audit-trail → view`).
- [ ] Includes entries for cancelled fiscal documents — cancellation never removes prior history.
- [ ] Works for any entity across any module, since the write path is a schema-wide DB trigger, not a per-module integration.

## Dependencies

- **Depends on:** the DB trigger that writes `AuditTrailEntry` (see Notes) — every module's schema must have it in place before this query is meaningful.
- **Blocks:** None.

## Notes

Per doc §13 ("auditoria por trigger... nunca dependente do código da aplicação"), the write path — the DB trigger populating `AuditTrailEntry` — is built into every module's schema starting in Phase 1 (Fundação), alongside each table's own migration. This ticket covers only the admin-facing query/UI, which ships as hardening in Phase 8 (Robustez), once every module's schema (and therefore every trigger) exists to query against.
