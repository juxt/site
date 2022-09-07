#!/usr/bin/env sh

read_secret() {
  SECRET_NAME=$1
  PREFIX_KEYS=$2

  if [ -n "$PREFIX_KEYS" ]; then
    PREFIX_KEYS=${PREFIX_KEYS}_
  fi

  aws --region=eu-west-1 secretsmanager get-secret-value --secret-id $SECRET_NAME | jq .SecretString -r -M | jq -r 'keys[] as $k | "'$PREFIX_KEYS'\($k|ascii_upcase)\n\(.[$k])"' | \
    while read -r key; read -r val; do
     echo "if [ -z "\$${key}" ]; then export ${key}='${val}'; echo \"** Setting secret: ${key}\"; else echo \"Already set: ${key}\"; fi;"
    done
}

read_secret ${1} ${2}
