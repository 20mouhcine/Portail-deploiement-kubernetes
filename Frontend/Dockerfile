# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/dist/Frontend/browser/ /usr/share/nginx/html/

ENV BACKEND_HOST=kubeportal-backend:8080
ENV NGINX_ENVSUBST_FILTER=BACKEND_HOST

EXPOSE 8080
