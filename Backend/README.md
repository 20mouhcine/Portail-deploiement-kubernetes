# Backend d'authentification

Le backend utilise Spring Security avec une session serveur. Le navigateur reçoit un
cookie `JSESSIONID` `HttpOnly`; aucun JWT ou mot de passe n'est exposé au frontend.

## Variables d'environnement locales

La variable suivante est obligatoire pour PostgreSQL :

```text
DB_PASSWORD=<mot de passe PostgreSQL>
```

Ces variables facultatives créent le premier administrateur au démarrage. Elles doivent
être fournies ensemble et le mot de passe doit contenir au moins 12 caractères :

```text
INITIAL_ADMIN_USERNAME=admin
INITIAL_ADMIN_EMAIL=admin@example.com
INITIAL_ADMIN_PASSWORD=<mot de passe initial>
```

Après la première création, retirer `INITIAL_ADMIN_PASSWORD` de la configuration de
lancement. Le mot de passe n'est enregistré en base que sous forme de hash BCrypt.

Variables utiles au déploiement :

```text
CORS_ALLOWED_ORIGINS=https://portail.example.com
SESSION_COOKIE_SECURE=true
```

`SESSION_COOKIE_SECURE` reste à `false` uniquement pour le développement HTTP local.

## Limitation des tentatives de connexion

Par défaut, un compte est bloqué temporairement après 5 échecs de connexion dans
une fenêtre de 15 minutes. Une même adresse IP peut produire au maximum 20 échecs
sur cette fenêtre. Le blocage dure 15 minutes et l'API répond avec le statut HTTP
`429 Too Many Requests` et l'en-tête `Retry-After`.

Ces valeurs peuvent être adaptées avec des variables d'environnement :

```text
LOGIN_MAX_ATTEMPTS=5
LOGIN_IP_MAX_ATTEMPTS=20
LOGIN_ATTEMPT_WINDOW=15m
LOGIN_BLOCK_DURATION=15m
LOGIN_MAX_TRACKED_KEYS=10000
```

Les compteurs sont conservés en mémoire. Avec plusieurs replicas backend, utiliser
un stockage partagé comme Redis pour appliquer une limite globale.

## HTTPS en production

Le profil `prod` active le cookie de session `Secure`, la prise en charge des en-têtes
transmis par un proxy et HTTPS sur le port `8443`. Il faut fournir un certificat
PKCS12 avec les variables suivantes :

```text
SPRING_PROFILES_ACTIVE=prod
SSL_KEY_STORE=file:C:/certificats/kubeportal.p12
SSL_KEY_STORE_PASSWORD=<secret>
SSL_KEY_STORE_TYPE=PKCS12
CORS_ALLOWED_ORIGINS=https://portail.example.com
```

Le backend répond alors sur `https://localhost:8443` ou sur le nom DNS couvert par
le certificat. Le frontend doit lui aussi être servi en HTTPS, sinon le navigateur
n'enverra pas le cookie `Secure`.

Dans Kubernetes, il est habituel que l'Ingress termine TLS. Dans ce cas, le backend
peut rester en HTTP à l'intérieur du cluster tout en conservant le profil `prod` :

```text
SPRING_PROFILES_ACTIVE=prod
SSL_ENABLED=false
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=https://portail.example.com
```

Le service backend ne doit alors être accessible que via l'Ingress de confiance,
qui doit transmettre correctement `X-Forwarded-Proto: https`. Ne jamais versionner
un certificat privé ou son mot de passe ; les formats de certificats sensibles sont
ignorés par Git dans ce projet.

## API

| Méthode | Route | Accès |
|---|---|---|
| `GET` | `/api/auth/csrf` | Public |
| `POST` | `/api/auth/login` | Public avec CSRF |
| `GET` | `/api/auth/me` | Utilisateur connecté |
| `POST` | `/api/auth/logout` | Utilisateur connecté avec CSRF |
| `GET` | `/api/admin/users` | `ADMIN` |
| `POST` | `/api/admin/users` | `ADMIN` |
| `PATCH` | `/api/admin/users/{id}/enabled` | `ADMIN` |
| `PUT` | `/api/admin/users/{id}/roles` | `ADMIN` |

Le login attend un corps `application/x-www-form-urlencoded` avec `username` et
`password`. Angular doit d'abord appeler `/api/auth/csrf`, puis envoyer la valeur du
cookie `XSRF-TOKEN` dans l'en-tête `X-XSRF-TOKEN` pour les requêtes qui modifient l'état.

## Vérification

```powershell
mvn test
```

Les tests utilisent H2 en mémoire et ne modifient pas la base PostgreSQL locale.

## Production

Le projet utilise actuellement `spring.jpa.hibernate.ddl-auto=update` pour faciliter le
développement. Avant un déploiement réel, remplacer cette stratégie par des migrations
versionnées et partager les sessions avec Spring Session/Redis si le backend possède
plusieurs replicas Kubernetes.
