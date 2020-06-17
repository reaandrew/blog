---
author: "Andy Rea"
title: "Local PGSQL Docker Utility"
date: 2020-06-17
draft: false
---

This is a small set of bash scripts to make it really simple to start, stop, connect and load scripts into a local postgres database.

This uses docker, it sets up trust with the host so it can connect using the psql client. It also sets up a local volume in the directory which you launch the utility from which allows the container instances themselves to be transient but still giving you control to destroy the state when required.

## Here is how to use it

1. Start the instance

```shell
spd start
```

2. Connect to the DB instance

```shell
spd connect
```

3. Execute an SQL file against the running database

```shell
spd load fubar.sql
```

4. Stop the DB instance

```shell
spd stop
```

5. Stop the DB instance and destroy the DB state

```shell
spd destroy
```

## Here is the script

```shell
export DEFAULT_PG_PASSWORD=mysecretpassword

run(){
  mkdir -p pg_data
  docker run -d -p 5432:5432 \
    --name my-postgres \
    -e POSTGRES_PASSWORD="$DEFAULT_PG_PASSWORD" \
    -e POSTGRES_HOST_AUTH_METHOD=trust \
    -e PGDATA=/var/lib/postgresql/data/pgdata \
    -v `pwd`/pg_data:/var/lib/postgresql/data \
    postgres > /dev/null
}

teardown(){
  docker kill my-postgres 2> /dev/null || :
  docker rm my-postgres 2> /dev/null || :
}

setup_db_instance(){
  {
    teardown > /dev/null
  }
  run
}

spd(){
  case "$1" in
    start)
      setup_db_instance
      ;;
    stop)
      teardown > /dev/null
      ;;
    destroy)
      teardown > /dev/null
      sudo rm -rf ./pg_data
      ;;
    connect)
      {
      PGPASSWORD="$DEFAULT_PG_PASSWORD" psql -h $(docker inspect my-postgres 2> /dev/null \
        | jq -r '.[] | .NetworkSettings.IPAddress') -U postgres 2> /dev/null
      } || {
        echo "DB is not running. run 'spd start'"
      }
      ;;
    load)
      {
        PGPASSWORD="$DEFAULT_PG_PASSWORD" psql -h $(docker inspect my-postgres  2> /dev/null\
          | jq -r '.[] | .NetworkSettings.IPAddress') -U postgres -f "$2" 2> /dev/null
      } || {
        echo "DB is not running. run 'spd start'"
      }
      ;;
  esac
}
```
