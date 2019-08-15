CURPATH := $(shell pwd)

CMD_LIST := clean dev prod

all:
	@echo Usage: make COMMAND
	@echo -e COMMAND:\
		\\n'  clean:    Clean up output files.'\
		\\n'  dev:      Start development.'\
		\\n'  prod:     Build for deployment.'

.PHONY: $(CMD_LIST)
.SILENT: $(CMD_LIST)

clean:
	rm -rf target
	rm -f resources/public/js/clobanner.js

dev:
	clojure -A:dev

prod:
	clojure -A:prod

%:
	@:
