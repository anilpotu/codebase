#!/bin/bash
# Creates the four databases needed by the gRPC-origin services.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE grpcdb;
    CREATE DATABASE financialdb;
    CREATE DATABASE healthdb;
    CREATE DATABASE socialdb;
EOSQL
