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

# Release to gh-pages
# Preparation:
#   $ git co -b gh-pages
#   $ git clean -fd
#   $ git rm EVERYTHING
#   $ rm -rf EVERYTHING
#   $ git com -m "Remove all files"
# Usage:
#   $ make prod
#   $ make rel
rel:
	echo "Releasing the product to GitHub..."
	scripts/prerelease.sh
	git co gh-pages
	git co master -- resources/public/
	git rm -r --cached resources
	cp -r resources/public/* .
	git add index.html img css js
	git com -m "Deploy app"
	git ll
	git clean -fd
	git co master
	echo "Ready to release with: git push github gh-pages"

%:
	@:
