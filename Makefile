CURPATH := $(shell pwd)

CMD_LIST := clean dev prod rel

all:
	@echo Usage: make COMMAND
	@echo -e COMMAND:\
		\\n'  clean:    Clean up output files.'\
		\\n'  dev:      Start development.'\
		\\n'  prod:     Build for deployment.'\
		\\n'  rel:      Release at GitHub.'

.PHONY: $(CMD_LIST)
.SILENT: $(CMD_LIST)

clean:
	rm -rf target
	rm -f resources/public/js/clobanner.js

dev:
	clojure -A:dev

prod:
	clojure -A:prod

rel:
	echo "Releasing the product to GitHub..."
	scripts/prerelease.sh
	git co gh-pages
	git co master -- resources/public/
	git rm -r --cached resources
	rm resources/public/index-dev.html
	cp -ru resources/public/* .
	git com -am "Deploy app"
	git clean -fd
	git co master
	echo "Ready to release with: git push github gh-pages:gh-pages"

%:
	@:
