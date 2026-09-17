# UC-M1-02 — Upload Digital Certificate (`UploadDigitalCertificateUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.1 Empresa / Filiais: upload and manage an A1 (`.pfx`) certificate per company; A3 (token) support is future work.

## Description

An administrator uploads (or replaces) a company's A1 certificate file and its password. The certificate is encrypted before storage (doc §11.4) and its expiry date is recorded. Precondition: the company exists (UC-01). Postcondition: the company has an active certificate usable for automatic fiscal-document signing (M2/M3/M4).

## Port signature

```java
public interface UploadDigitalCertificateUseCase {
    void execute(UploadDigitalCertificateCommand command);
}
```

`UploadDigitalCertificateCommand`: `companyId`, `pfxFile: byte[]`, `password`. No return value; raises `BusinessRuleException` if the file isn't a valid `.pfx` or the password is wrong.

## Outbound ports required

- `CertificateStoragePort` (encrypted at-rest storage)
- `CompanyRepositoryPort` (to attach the certificate reference to the company)

## REST endpoint

`POST /api/companies/{id}/certificate`

## Domain entities touched

- `Company`
- `DigitalCertificate`

## Acceptance criteria

- [ ] Only `A1` type is accepted in the MVP; `A3` is rejected with a clear message (doc §2.1 explicitly defers A3).
- [ ] The `.pfx` payload is encrypted before it reaches storage — never persisted in plaintext (doc §11.4).
- [ ] Expiry date is extracted from the certificate and stored on `DigitalCertificate`.
- [ ] Uploading a new certificate replaces the previous one; the company never holds two active certificates.
- [ ] A company with an expired or absent certificate cannot execute any fiscal-issuance use case (M2/M3/M4) — this invariant is enforced at issuance time, not here, but this ticket must ensure expiry is queryable.

## Dependencies

- **Depends on:** UC-01 (Register Company).
- **Blocks:** All fiscal issuance use cases in M2, M3, M4 (automatic signing requires an active certificate).
