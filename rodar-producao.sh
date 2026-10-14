#!/usr/bin/env bash
# Roda o KODIE no perfil de producao, lendo as credenciais do arquivo .env.
# Uso: ./rodar-producao.sh
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
    echo "Arquivo .env nao encontrado. Copie .env.exemplo para .env e preencha." >&2
    exit 1
fi

set -a; source .env; set +a

faltando=()
for variavel in DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD KODIE_ADMIN_SENHA; do
    [[ -z "${!variavel:-}" ]] && faltando+=("$variavel")
done
if (( ${#faltando[@]} > 0 )); then
    echo "Preencha no .env: ${faltando[*]}" >&2
    exit 1
fi
if [[ "$DATABASE_URL" != jdbc:postgresql://* ]]; then
    echo "DATABASE_URL precisa comecar com jdbc:postgresql:// (veja as instrucoes no .env.exemplo)" >&2
    exit 1
fi

./mvnw -q -B -DskipTests package
exec java -jar target/kodie-0.0.1-SNAPSHOT.jar
