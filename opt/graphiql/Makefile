.PHONY: build deploy token

build:
	yarn
	yarn build
	npx gulp

token:
	site -s check-token || site get-token

deploy:	token
	site put-asset --file build/index.html --path /_site/graphiql/index.html
