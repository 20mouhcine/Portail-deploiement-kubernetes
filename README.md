# Portail de déploiement Kubernetes

Application Angular + Spring Boot permettant de gérer des projets et leurs déploiements Kubernetes.

## Prérequis

- Java 21
- Node.js et npm
- PostgreSQL avec une base `kube_portal`
- Un cluster Kubernetes accessible par le kubeconfig courant (Minikube convient en local)

## Configuration locale

Copier `.env.example` vers `.env`, remplacer les mots de passe, puis charger ces variables dans le terminal. `DB_PASSWORD` est obligatoire : aucun mot de passe par défaut n'est utilisé.

Les variables `INITIAL_ADMIN_*` créent le premier administrateur uniquement lorsque les trois sont renseignées. En production, activer `SESSION_COOKIE_SECURE=true` et limiter `CORS_ALLOWED_ORIGINS` à l'origine exacte du frontend.

## Démarrage

Backend :

```powershell
cd Backend
$env:DB_PASSWORD = "votre-mot-de-passe-postgres"
./mvnw.cmd spring-boot:run
```

Frontend, dans un second terminal :

```powershell
cd Frontend
npm install
npm start
```

Ouvrir `http://localhost:4200`. Le proxy Vite transmet `/api` vers `http://localhost:8080`.

## Autorisations

- Un administrateur voit et gère tous les projets.
- Le propriétaire gère son projet; l'identité du propriétaire vient de la session et non du corps HTTP.
- Les utilisateurs autorisés voient le projet.
- Seuls `ADMIN` et `DEVOPS` peuvent lancer des opérations Kubernetes, dans les namespaces autorisés du projet.
- Si aucun namespace n'est configuré, seul `default` est autorisé. Les namespaces système Kubernetes sont refusés.
- Les secrets sont des références `nom-du-secret/clé`; leur valeur n'est jamais stockée par le portail.

## Vérification

```powershell
cd Backend
./mvnw.cmd test

cd ../Frontend
npm test -- --watch=false
npm run build
```

Les jobs Kubernetes sont réessayés avec temporisation exponentielle. Les jobs abandonnés et les rollouts trop longs sont marqués en échec, et les flux SSE envoient des heartbeats avec reconnexion progressive côté frontend.
