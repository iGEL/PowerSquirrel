# syntax=docker/dockerfile:1.6

###########################
# Builder (ARM64, Alpine) #
###########################
FROM --platform=linux/arm64/v8 rust:1-alpine3.20 AS builder

# Install build tools
RUN apk add --no-cache build-base

WORKDIR /app

# Pre-build step to cache dependencies
COPY Cargo.toml Cargo.lock ./
RUN mkdir src && echo 'fn main() {}' > src/main.rs \
    && cargo build --release \
    && rm -rf src

# Now copy real sources
COPY src ./src

# Build release binary
# On this image/arch, the host triple is aarch64-unknown-linux-musl
# which produces a (fully) static musl binary by default.
RUN cargo build --release

#########################
# Final runtime image   #
#########################
FROM --platform=linux/arm64/v8 alpine:3.20

WORKDIR /app

# Copy the static binary
COPY --from=builder /app/target/release/sungrow_inverter /usr/local/bin/sungrow_inverter

# Optional: app user instead of root
# RUN adduser -D appuser
# USER appuser

CMD ["sungrow_inverter"]
