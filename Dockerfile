FROM node:20-alpine AS frontend-build
WORKDIR /frontend

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY web web
RUN npm run build:web

FROM maven:3.9-eclipse-temurin-17 AS backend-deps
WORKDIR /build/backend

COPY backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

FROM backend-deps AS backend-build
WORKDIR /build/backend

COPY backend/src src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM docker.1ms.run/library/eclipse-temurin:17-jre
WORKDIR /app

COPY --from=backend-build /build/backend/target/chuanzi-restaurant-assistant.jar /app/app.jar
COPY --from=frontend-build /frontend/dist/web /app/web

ENV APP_PORT=8080
ENV WEB_ROOT=/app/web
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8" \
    LANG=C.UTF-8

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
