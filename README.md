# Clobanner

# Development

Start development.

    $ clojure -Adev

The index page for dev. is `index-dev.html`.

Run the command once on a cljs buffer in Vim:

    :Piggieback (figwheel.main.api/repl-env "dev")

# Packaging

    $ clojure -Aprod

Deploy files in `resources/public` directory.

- `index.html`
- `css/`
- `img/`
- `js/clobanner.js`
