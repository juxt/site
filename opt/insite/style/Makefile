.PHONY: build upload clean

build: 	dist/styles.css dist/styles.css.gz dist/styles.css.br

dist/styles.css:	main.css
	node_modules/.bin/tailwindcss -i $< -o $@

dist/styles.css.gz:	dist/styles.css
	gzip -fk $<

dist/styles.css.br:	dist/styles.css
	brotli -f $<

upload:
	site put-asset --file dist/styles.css --type 'text/css' --path /_insite/css/styles.css -c PUBLIC
	site put-asset --file dist/styles.css.gz --type 'text/css' --path /_insite/css/styles.css.gz -c PUBLIC -e gzip
	site put-asset --file dist/styles.css.br --type 'text/css' --path /_insite/css/styles.css.br -c PUBLIC -e br

clean:
	rm -rf dist
