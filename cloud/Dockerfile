FROM rust:1.85-bookworm AS builder
WORKDIR /build

COPY Cargo.toml Cargo.lock* ./
COPY brain/ brain/
COPY carapace/ carapace/
COPY pincers/ pincers/
COPY protocol/ protocol/
COPY server/ server/

RUN cargo build --release -p crab-server

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/release/crab-server /usr/local/bin/crab-server

EXPOSE 3000
ENV RUST_LOG=crab=info
CMD ["crab-server"]
