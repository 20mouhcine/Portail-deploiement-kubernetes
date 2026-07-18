# Frontend KubePortal

Frontend Angular 21 et Tailwind CSS du portail interne de déploiement Kubernetes.

## Démarrage local

Le backend Spring Boot doit répondre sur `http://localhost:8080`.

```powershell
npm install
npm start
```

Ouvrir ensuite `http://localhost:4200`. Le proxy défini dans `proxy.conf.json`
transmet automatiquement les appels `/api` au backend.

## Organisation

```text
src/app/
├── core/auth/
│   ├── guards/          Protection des routes
│   ├── models/          Contrats TypeScript de l'API
│   └── services/        Session, login, logout et CSRF
├── features/
│   ├── auth/            Formulaire et page de connexion
│   └── dashboard/       Première page protégée
├── layout/              En-tête de l'application
└── shared/components/   Composants visuels réutilisables
```

La session est conservée par Spring Boot dans un cookie `JSESSIONID` HttpOnly.
Angular ne stocke aucun JWT. Le cookie `XSRF-TOKEN` et l'en-tête
`X-XSRF-TOKEN` protègent les requêtes qui modifient l'état.

## Commandes de vérification

```powershell
npm test -- --watch=false
npm run build
```
