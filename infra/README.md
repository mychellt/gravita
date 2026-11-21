# Gravita infrastructure

Infrastructure as code for Gravita's environments. Today only **homologation** exists:
one **AWS EC2 Graviton instance** (t4g.small, Ubuntu 24.04) running Docker Compose, plus
SQS queues and an S3 bucket for backups. Design rationale: the *Gravita Homologation
Environment — System Design* doc.

```
infra/
├── terraform/
│   ├── modules/
│   │   ├── aws-vm/        # VPC, public subnet, security group, EC2 + Elastic IP, IAM role, cloud-init
│   │   └── sqs-queues/    # queues + dead-letter queues + least-privilege policy
│   └── envs/
│       └── homolog/       # wires the modules, backup bucket, budget; remote state in S3
├── compose/               # docker-compose.homolog.yaml + Caddyfile (deployed to /opt/gravita)
├── scripts/               # deploy, backup, restore, reset (run on the VM)
└── secrets/               # SOPS-encrypted env files only (+ the .example template)
```

## Design choices

- **No access keys anywhere.** The app (SQS) and the backup script (S3) use the EC2 instance
  role through IMDSv2 (hop limit 2, so containers can reach it).
- **No NAT Gateway, no load balancer.** Public subnet + security group; Caddy terminates TLS.
- **SSH closed by default.** Shell access goes through **SSM Session Manager**; `rsync`/`ssh`
  run over an SSM tunnel (see *Deploy*). Open port 22 only by setting `ssh_allowed_cidrs`.
- **us-east-1 by default** (~50% cheaper than São Paulo). Set `aws_region = "sa-east-1"` for lower latency.
- **Costs:** ~US$18/month (t4g.small, 30 GB gp3, Elastic IP); covered by new-account credits
  for the first months. A US$30 budget e-mails `budget_alert_emails` at 50% and 100% forecast.

## Rules

- **Never commit Terraform state, `terraform.tfvars` or `backend.hcl`** (see `.gitignore`).
- **Never commit plain-text secrets.** `secrets/*.env` must be SOPS-encrypted; CI fails otherwise.
- Changes under `infra/**` run `.github/workflows/infra-ci.yml` (fmt, validate, shellcheck, compose config).

## Prerequisites (one time)

1. A **separate AWS account** for homologation (AWS Organizations), with an AWS CLI profile
   `gravita-homolog` (`aws configure sso` or `aws configure --profile gravita-homolog`).
2. The **state bucket** (Terraform can't create its own backend):
   ```bash
   ACCOUNT=$(aws sts get-caller-identity --profile gravita-homolog --query Account --output text)
   aws s3api create-bucket --bucket "gravita-tfstate-$ACCOUNT" --region us-east-1 --profile gravita-homolog
   aws s3api put-bucket-versioning --bucket "gravita-tfstate-$ACCOUNT" \
     --versioning-configuration Status=Enabled --profile gravita-homolog
   aws s3api put-public-access-block --bucket "gravita-tfstate-$ACCOUNT" --profile gravita-homolog \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```
3. Activate the **`Environment` cost allocation tag** (Billing → Cost allocation tags) so the budget counts homolog resources.
4. Tools: Terraform ≥ 1.10, AWS CLI v2 + [Session Manager plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html),
   [SOPS](https://github.com/getsops/sops), [age](https://github.com/FiloSottile/age).
5. Put your age public key in `infra/.sops.yaml`.

## Provision

```bash
cd infra/terraform/envs/homolog
cp backend.hcl.example backend.hcl            # fill in the account id
cp terraform.tfvars.example terraform.tfvars  # budget e-mails, your SSH public key
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
terraform output                              # IP, instance id, queue URLs, backup bucket
```

Commit the generated `.terraform.lock.hcl`. Then create DNS **A records** for
`homolog.<domain>` and `mail.homolog.<domain>` pointing at `vm_public_ip`.

## Configure secrets

```bash
cd infra
cp secrets/homolog.env.example secrets/homolog.env
# fill in: terraform outputs, new encryption keys (openssl rand -base64 32), passwords, domains
sops --encrypt --in-place secrets/homolog.env
git add secrets/homolog.env                   # encrypted, safe to commit
```

Edit later with `sops secrets/homolog.env`.

## Continuous deployment

`.github/workflows/deploy-homolog.yml` deploys on every push to the **`homolog` branch**
(or manually, on that branch). It runs the tests, builds the `linux/arm64` image to
`ghcr.io/<owner>/gravita:<sha>`, builds the Angular app, then — with no SSH and no stored
AWS keys — assumes `github_deploy_role_arn` through OIDC, uploads a release bundle
(stack files, scripts, web build, decrypted `.env`) to `s3://<backup bucket>/deploy/`,
runs `scripts/install-release.sh` on the VM over **SSM Run Command**, and deletes the bundle.

One-time setup in GitHub → Settings → Environments → **`homolog`** (add required reviewers if you want an approval gate):

| Kind | Name | Value |
| --- | --- | --- |
| Variable | `AWS_REGION` | `us-east-1` |
| Variable | `AWS_HOMOLOG_ROLE_ARN` | `terraform output -raw github_deploy_role_arn` |
| Variable | `HOMOLOG_INSTANCE_ID` | `terraform output -raw vm_instance_id` |
| Variable | `HOMOLOG_BUCKET` | `terraform output -raw backup_bucket` |
| Variable | `HOMOLOG_URL` | `https://homolog.<domain>` (enables the smoke check) |
| Secret | `SOPS_AGE_KEY` | an age **private** key listed as a recipient in `.sops.yaml` (use a dedicated CI key) |

If the GHCR package is private, log the VM in once (as `deploy`):
`docker login ghcr.io -u <user>` with a token that has only `read:packages`.

## Manual deploy

SSH over SSM (no open port 22) — add to `~/.ssh/config`:

```
Host gravita-homolog
  HostName <vm_instance_id>
  User deploy
  ProxyCommand aws ssm start-session --target %h --document-name AWS-StartSSHSession --parameters portNumber=%p --profile gravita-homolog --region us-east-1
```

Then, from a machine that can decrypt the secrets (CI will do this later):

```bash
VM=gravita-homolog
(cd web && npm ci && npm run build -- --configuration production)
sops --decrypt infra/secrets/homolog.env > /tmp/homolog.env
rsync -a infra/compose/Caddyfile "$VM:/opt/gravita/"
rsync -a infra/compose/docker-compose.homolog.yaml "$VM:/opt/gravita/docker-compose.yaml"
rsync -a infra/scripts/ "$VM:/opt/gravita/scripts/"
rsync -a --delete web/dist/gravita-web/browser/ "$VM:/opt/gravita/web/"
rsync -a /tmp/homolog.env "$VM:/opt/gravita/.env" && rm /tmp/homolog.env
ssh "$VM" 'chmod 600 /opt/gravita/.env && /opt/gravita/scripts/deploy.sh ghcr.io/<org>/gravita:<sha>'
```

The app image must be built for `linux/arm64` (`docker buildx build --platform linux/arm64,linux/amd64`).
If the GHCR package is private, run `docker login ghcr.io` on the VM once with a read-only token.

## Operate (on the VM, as `deploy`)

| Task | Command |
| --- | --- |
| Shell | `aws ssm start-session --target <vm_instance_id>` or `ssh gravita-homolog` |
| Logs | `cd /opt/gravita && docker compose logs -f app` |
| Backup now | `/opt/gravita/scripts/backup.sh` (also runs nightly at 03:00 UTC) |
| Restore latest | `/opt/gravita/scripts/restore.sh` |
| Restore a given dump | `/opt/gravita/scripts/restore.sh gravita-<stamp>.dump` |
| Reset to empty database | `/opt/gravita/scripts/homolog-reset.sh` |
| Read test e-mails | `https://mail.homolog.<domain>` (basic auth) |

**Test a restore once a month.** A backup that has never been restored doesn't count.
