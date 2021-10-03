.PHONY: build deploy

build:
	yarn
	yarn build
	npx gulp

deploy:
	site put-asset --file build/index.html --path /_site/graphiql/index.html
