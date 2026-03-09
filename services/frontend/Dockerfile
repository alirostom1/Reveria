FROM node:22-alpine AS build

WORKDIR /app

COPY services/frontend/package.json services/frontend/package-lock.json* ./

RUN npm ci

COPY services/frontend/ .

RUN npm run build -- --configuration=production

FROM nginx:alpine

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

COPY services/frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

