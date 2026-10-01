# Gravita — Angular 17

Protótipo funcional completo do ERP para varejo brasileiro.

## Stack

- **Angular 17** (standalone components, signals, lazy routing)
- **TypeScript 5.4**
- **SCSS** com design system via CSS custom properties
- **Tabler Icons** via webfont CDN
- **Google Fonts** — DM Sans + DM Mono

## Pré-requisitos

```bash
node >= 18
npm >= 9
```

## Instalação e execução

```bash
# 1. Instalar dependências
npm install

# 2. Rodar em modo desenvolvimento
npm start
# Acesse: http://localhost:4200

# 3. Build de produção
npm run build:prod
```

## Estrutura de pastas

```
src/
├── app/
│   ├── core/
│   │   ├── models/         # Interfaces e tipos de domínio
│   │   └── services/
│   │       ├── data.service.ts    # Mock de dados com signals
│   │       ├── toast.service.ts   # Notificações globais
│   │       └── title.service.ts   # Título dinâmico da topbar
│   │
│   ├── layout/
│   │   ├── admin-shell/    # Layout da área administrativa (/admin)
│   │   ├── shell/          # Layout pai (sidebar + topbar + router-outlet)
│   │   ├── sidebar/        # Navegação lateral
│   │   └── topbar/         # Barra superior
│   │
│   ├── modules/
│   │   ├── admin/          # Área administrativa — configurações da plataforma
│   │   ├── dashboard/      # M9 — Dashboard executivo
│   │   ├── pdv/            # M3 — PDV / NFC-e com modal de pagamento
│   │   ├── nfe/            # M2 — Listagem de NF-e
│   │   │   └── nfe-form/   # M2 — Emissão de NF-e (formulário)
│   │   ├── nfse/           # M4 — NFS-e multi-município
│   │   ├── estoque/        # M5 — Controle de estoque
│   │   ├── compras/        # M6 — Pedidos de compra
│   │   ├── crm/            # M7 — Vendas & CRM (funil Kanban)
│   │   ├── financeiro/     # M8 — Financeiro (CR/CP/Fluxo)
│   │   ├── relatorios/     # M9 — Relatórios & BI
│   │   └── settings/       # M10 — Configurações do sistema
│   │
│   └── shared/
│       ├── components/
│       │   ├── badge/          # Badge de status reutilizável
│       │   ├── page-header/    # Cabeçalho de página reutilizável
│       │   └── toast/          # Container de notificações
│       └── pipes/
│           └── brl.pipe.ts     # Formatação de moeda BRL
│
├── styles.scss             # Design tokens globais + utilitários
├── index.html
└── main.ts
```

## Módulos implementados

| Código | Módulo           | Rota           | Funcionalidades |
|--------|------------------|----------------|-----------------|
| M2     | NF-e             | `/nfe`         | Listagem, filtros, DANFE, cancelamento |
| M2     | NF-e Form        | `/nfe/nova`    | Emissão completa com steps, itens, tributos |
| M3     | PDV / NFC-e      | `/pdv`         | Carrinho, modal de pagamento, troco, NFC-e |
| M4     | NFS-e            | `/nfse`        | Listagem RPS/NFS-e, padrões ABRASF/Nacional |
| M5     | Estoque          | `/inventory`   | Movimentações, alertas críticos, filtros |
| M6     | Compras          | `/purchasing`  | Pedidos, aprovação, recebimento |
| M7     | Vendas & CRM     | `/crm`         | Funil Kanban, clientes, metas |
| M8     | Financeiro       | `/finance`     | CR/CP, fluxo de caixa, conciliação |
| M9     | Relatórios & BI  | `/reports`     | Curva ABC, DRE gerencial, cards de relatório |
| M10    | Configurações    | `/settings`    | Empresa, usuários, integrações, toggles |
| —      | Dashboard        | `/dashboard`   | KPIs, gráfico, atividade recente |
| Admin  | Configurações gerais | `/admin/settings/:section` | Área administrativa da plataforma: planos e preços, assinatura/teste, suporte & SLA, catálogo de integrações, segurança, dados institucionais e conteúdo do site — comum a todos os clientes |

## Arquitetura

- **Standalone components** — sem NgModule, tree-shaking otimizado
- **Angular Signals** — estado reativo sem RxJS overhead
- **Lazy loading** — cada módulo carregado sob demanda via `loadComponent`
- **Design tokens** — todas as cores e tamanhos via CSS custom properties
- **Mock data** — `DataService` com signals simulando backend real

## Próximos passos (pós-MVP)

1. Integrar com API REST real (substituir `DataService`)
2. Adicionar `HttpClient` com interceptors de autenticação
3. Implementar guards de rota por perfil de permissão
4. Adicionar testes unitários (Jest / Karma)
5. PWA + service worker para PDV offline

## Deploy AWS (S3 apenas)

Para este projeto Angular (frontend estatico), voce pode publicar somente com S3 Website Hosting.

- S3 (armazenamento + website endpoint)
- GitHub Actions com OIDC (sem chave AWS fixa em secret)

### 1) Build de producao

```bash
npm ci
npm run build:prod
```

### 2) Setup inicial da infraestrutura (uma vez)

Requisitos:

- AWS CLI configurado (`aws configure`)
- Permissoes para S3 e IAM/OIDC (ou conta admin para primeira execucao)

Executar:

```bash
REGION=us-east-1 APP_NAME=gravita-web npm run aws:setup
```

O script vai criar:

- bucket S3
- website hosting no bucket
- fallback SPA (`index.html` para erro)

Ao final, ele imprime:

- `S3_BUCKET`
- `Website URL`

### 3) Configurar GitHub Actions (deploy automatico)

No repositorio GitHub, configure:

- Repository Variable `AWS_REGION`
- Repository Variable `S3_BUCKET`
- Repository Secret `AWS_ROLE_TO_ASSUME`

O workflow esta em `.github/workflows/deploy-aws.yml` e faz deploy no push para `main`.

### 4) Deploy manual (opcional)

Depois de configurar variaveis de ambiente localmente:

```bash
export AWS_REGION=us-east-1
export S3_BUCKET=seu-bucket

npm run build:prod
npm run deploy:aws
```

### 5) Custo mensal estimado (baixo trafego)

- S3: centavos a poucos dolares

Observacao: em S3-only o endpoint e HTTP (sem HTTPS). Para HTTPS e dominio customizado, use CloudFront.

Em geral fica bem mais barato que manter EC2 ligado 24/7.
