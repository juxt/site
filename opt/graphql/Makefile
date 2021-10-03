.PHONY: all init build-graphiql deploy-graphiql deploy-schema introspect
.DEFAULT: all

all:	init deploy-schema

deploy-graphiql:	build-graphiql
	site put-asset --file build/index.html --path /_site/graphiql/index.html

init:
	site post-resources --file resources.edn

deploy-schema:
	site put-asset --file schema.graphql --type application/graphql --path /_site/graphql

build-graphiql:
	yarn
	yarn build
	npx gulp

introspect:
	@site -s post --file graphiql-introspection-query.graphql --path /graphql --type application/graphql
