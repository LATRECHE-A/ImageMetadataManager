#!/usr/bin/env bash
set -Eeuo pipefail

readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
readonly PROJECT_ROOT
readonly BUILD_DIR="${PROJECT_ROOT}/build"
readonly TEST_REPORTS="${BUILD_DIR}/test-reports"

QUICK=false
COVERAGE=false
CI=false
VERBOSE=false

log_info() { echo -e "${BLUE}ℹ${NC} $*"; }
log_ok() { echo -e "${GREEN}✓${NC} $*"; }
log_warn() { echo -e "${YELLOW}⚠${NC} $*"; }
log_err() { echo -e "${RED}✗${NC} $*" >&2; }

usage() {
    cat <<EOF
Usage: $(basename "$0") [OPTIONS]

OPTIONS:
    --quick        Skip integration tests
    --coverage     Generate coverage
    --ci           CI mode
    --verbose      Detailed output
    -h, --help     Show help
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --quick) QUICK=true; shift ;;
        --coverage) COVERAGE=true; shift ;;
        --ci) CI=true; shift ;;
        --verbose) VERBOSE=true; shift ;;
        -h|--help) usage; exit 0 ;;
        *) log_err "Unknown: $1"; usage; exit 1 ;;
    esac
done

log_info "Checking dependencies..."
command -v java >/dev/null || { log_err "java not found"; exit 1; }
command -v javac >/dev/null || { log_err "javac not found"; exit 1; }
log_ok "Dependencies OK"

log_info "Building..."
cd "$PROJECT_ROOT"
make clean build || { log_err "Build failed"; exit 1; }
log_ok "Build complete"

log_info "Running tests..."
make test || { log_err "Tests failed"; exit 1; }
log_ok "Tests passed"

if [[ $COVERAGE == true ]]; then
    log_info "Generating coverage..."
    log_warn "Coverage requires JaCoCo setup"
fi

log_ok "All tests complete"
