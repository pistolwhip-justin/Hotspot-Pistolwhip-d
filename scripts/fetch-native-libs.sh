#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="2.20.0"
BASE="https://github.com/heiher/hev-socks5-tunnel/releases/download/${VERSION}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fetch() {
  local asset="$1"
  local expected="$2"
  local destination="$3"
  curl --fail --location --silent --show-error "${BASE}/${asset}" --output "${TMP}/${asset}"
  printf '%s  %s\n' "$expected" "${TMP}/${asset}" | sha256sum --check -
  install -D -m 0644 "${TMP}/${asset}" "${ROOT}/app/src/main/jniLibs/${destination}/libhev-socks5-tunnel.so"
}

fetch "hev-socks5-tunnel-android-arm64-v8a" "88b67182d6db79e880fa9a5aee850cf9ebf094efa56cb42d195ee0308085dd42" "arm64-v8a"
fetch "hev-socks5-tunnel-android-armeabi-v7a" "230da9afae932982c394489e414baa3a7c24d61d47b80c4834f0c9e02d960b28" "armeabi-v7a"
fetch "hev-socks5-tunnel-android-x86" "052600549b1c557b058e68222ff9a6115c96f7588715b02e3214c75b8af1e8a6" "x86"
fetch "hev-socks5-tunnel-android-x86_64" "14de34d62890929077224a76f222cd1e6f92f6d2f31a01b79996c96c79110339" "x86_64"

echo "HevSocks5Tunnel ${VERSION}: all Android ABI libraries downloaded and SHA-256 verified."
