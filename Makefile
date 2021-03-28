.PHONY: test

test:
	clojure -M:test -m kaocha.runner --reporter kaocha.report/dots "$@"
