# Build:  docker build -t campusevents-api backend
# Run:    docker run -p 8080:8080 --env-file backend/.env -e DB_URL=... campusevents-api
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package \
 && cp "$(find target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' ! -name '*-sources.jar' | head -1)" app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home app && mkdir -p /data/uploads /data/certificates && chown -R app /data /app
COPY --from=build /app/app.jar app.jar
USER app
# JVM tuned for a small instance (Render free: a fraction of a CPU, 512 MB): the quick C1 compiler only and the
# single-threaded GC halve startup and the first requests after a restart (measured: 74s -> 34s start at 0.5 CPU).
# Uploaded event images and certificate templates; mount a persistent disk at /data or they are lost on redeploy
ENV APP_IMAGES_FOLDER=/data/uploads/ \
    APP_CERTIFICATES_FOLDER=/data/certificates \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss512k -XX:ReservedCodeCacheSize=64m"
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
