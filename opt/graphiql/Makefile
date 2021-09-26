.PHONY: build deploy introspect

deploy:	build
	site put-asset --file build/index.html --path /graphiql/index.html

build:
	yarn
	yarn build
	npx gulp

introspect:
	@site -s post --file graphiql-introspection-query.graphql --path /graphql --type application/graphql
