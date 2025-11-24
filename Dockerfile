# Build
FROM container-registry.oracle.com/graalvm/native-image:25 AS build-stage
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean nativeCompile \
    -PnativeImageArgs="-march=armv8-a --gc=G1" \
    -x test

# Release
FROM debian:12-slim AS release-stage
WORKDIR /app
COPY --from=build-stage /app/build/native/nativeCompile/wol-bot-native /app/wol-bot-native

EXPOSE 8080

ENTRYPOINT ["/app/wol-bot-native"]
