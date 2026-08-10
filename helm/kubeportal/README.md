# Chart Helm KubePortal

Ce chart déploie KubePortal avec :

- un Deployment et un Service pour le backend ;
- un Deployment et un Service Nginx pour le frontend ;
- PostgreSQL sous forme de StatefulSet avec stockage persistant ;
- un Secret applicatif, un ServiceAccount et les règles RBAC ;
- un Ingress optionnel.

## Installation minimale

```powershell
helm upgrade --install kubeportal ./helm/kubeportal `
  --namespace kubeportal `
  --create-namespace `
  --set fullnameOverride=kubeportal `
  --set backend.image.tag=local `
  --set frontend.image.tag=local `
  --set-string postgresql.auth.password="mot-de-passe-db" `
  --set-string initialAdmin.password="mot-de-passe-admin"
```

Les deux mots de passe sont obligatoires lorsqu'aucun Secret existant n'est fourni. Pour éviter leur présence dans l'historique du terminal, créer un fichier de valeurs privé non versionné ou un Secret Kubernetes contenant les clés `DB_PASSWORD` et `INITIAL_ADMIN_PASSWORD`, puis définir `secrets.existingSecret`.

## Base de données externe

Pour utiliser un PostgreSQL existant :

```yaml
postgresql:
  enabled: false

externalDatabase:
  host: postgresql.example.internal
  port: 5432
  database: kube_portal
  username: kubeportal

secrets:
  existingSecret: kubeportal-secrets
```

## Paramètres importants

| Paramètre | Valeur par défaut | Description |
|---|---:|---|
| `backend.image.repository` | `kubeportal-backend` | Image du backend |
| `frontend.image.repository` | `kubeportal-frontend` | Image du frontend |
| `postgresql.enabled` | `true` | Installe PostgreSQL dans le cluster |
| `postgresql.persistence.enabled` | `true` | Conserve les données sur un PVC |
| `rbac.clusterWide` | `true` | Autorise la gestion de plusieurs namespaces |
| `ingress.enabled` | `false` | Expose le frontend par un Ingress |
| `sessionCookieSecure` | `false` | À activer lorsque l'accès utilise HTTPS |

Le frontend relaie `/api` vers le Service backend. L'Ingress doit donc envoyer toutes les routes vers le Service frontend.
