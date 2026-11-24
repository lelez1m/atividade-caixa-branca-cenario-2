#!/bin/sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mkdir -p "$ROOT/testes/build"
javac -encoding UTF-8 -d "$ROOT/testes/build" "$ROOT/revisado/EstoqueArquivo.java" "$ROOT/testes/TestesEstoque.java"
java -cp "$ROOT/testes/build" TestesEstoque
