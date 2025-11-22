# Build
FROM ghcr.io/graalvm/native-image-community:25-muslib AS build-stage
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean nativeCompile \
    -PnativeImageArgs="--libc=musl" \
    -x test

# Release
FROM alpine:3.22 AS release-stage
WORKDIR /app
COPY --from=build-stage /app/build/native/nativeCompile/wol-bot-native /app/wol-bot-native

EXPOSE 8080

ENTRYPOINT ["/app/wol-bot-native"]
