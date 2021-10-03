.PHONY: all init deploy introspect

all:	init deploy

init:
	site post-resources --file resources.edn

deploy:
	site put-asset --file schema.graphql --type application/graphql --path /_site/graphql

introspect:
	@site -s post --file <(./json) --path /_site/graphql --type application/json
