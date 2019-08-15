## rebel

    $ clojure -Arebel
    user=> (load "dev")
           (start-nrepl-server!)
           (require 'figwheel.main.api)
           (figwheel.main.api/start {:mode :serve} "dev")

from a cljs buffer in vim:

    :Piggieback (figwheel.main.api/repl-env "dev")
