.PHONY: download deploy

deploy:	download
	site put-asset --file target/react.js --path /graphiql/react.js
	site put-asset --file target/react-dom.js --path /graphiql/react-dom.js
	site put-asset --file target/graphiql.css --path /graphiql/graphiql.css
	site put-asset --file target/graphiql.js --path /graphiql/graphiql.js
	site put-asset --file index.html --path /graphiql/index.html

download: target/react.js target/react-dom.js target/graphiql.css target/graphiql.js

target/react.js:
	mkdir -p target
	curl -L -o $@ https://unpkg.com/react@16.14.0/umd/react.development.js

target/react-dom.js:
	mkdir -p target
	curl -L -o $@ https://unpkg.com/react-dom@16/umd/react-dom.development.js

target/graphiql.css:
	curl -L -o $@ https://unpkg.com/graphiql/graphiql.min.css

target/graphiql.js:
	curl -L -o $@ https://unpkg.com/graphiql/graphiql.min.js
