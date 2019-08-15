(in-ns 'user)

;;; Start nREPL server
(require '[cider.nrepl :as cider]
         '[nrepl.server :as nrepl])
(defonce server
  (nrepl/start-server
    :handler cider/cider-nrepl-handler))
;; Spit port number to the file (for Vim+fireplace)
(let [port (:port server)]
  (spit ".nrepl-port" port)
  (println "nREPL server is running on port" port))

;;; Start figwheel server
(require 'figwheel.main.api)
(figwheel.main.api/start {:mode :serve} "dev")

;;; Start rebel-readline (a REPL by bhauman)
(require 'rebel-readline.main)
(rebel-readline.main/-main)   ;; this will block till exit from REPL

;;; Remove the port file.
(require 'clojure.java.io)
(clojure.java.io/delete-file ".nrepl-port")

;;; We need to exit explicitly because nREPL server is running
;;; on a different thread.
;;; `(shutdown-agents)` will do too.
(println "Bye!")
(System/exit 0)
