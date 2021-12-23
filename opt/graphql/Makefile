.PHONY: all init install introspect

all:	init install

init:
	site post-resources --file resources.edn

install:
	@site put-asset --file ../../src/juxt/site/alpha/site-schema.graphql --type application/graphql --path /_site/graphql

introspect:
	@site -s post --file <(./json) --path /_site/graphql --type application/json
