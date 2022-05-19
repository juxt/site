.PHONY: test

test:
	clojure -M:test -m kaocha.runner --reporter kaocha.report/dots test

watch:
	clojure -M:test -m kaocha.runner --watch --reporter kaocha.report/dots test
