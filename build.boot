(set-env!
  :source-paths #{"src/all"
                  ; "leihs-clj-shared/src"
                  }
  :resource-paths #{"resources"}
  :project 'leihs-mail
  :version "0.1.0-SNAPSHOT"
  :dependencies '[]) 

(task-options!
  target {:dir #{"target"}}
  aot {:all true}
  repl {:init-ns 'leihs.mail.main}
  sift {:include #{#"leihs-mail.jar"}}
  jar {:file "leihs-mail.jar"
       :main 'leihs.mail.main})

(deftask uberjar
  "Build an uberjar of the application."
  []
  (comp (aot) (uber) (jar) (sift) (target))) 

(deftask run
  "Run the application with given opts."
  []
  (require 'leihs.mail.main)
  (->> *args*
       (cons "run")
       (apply (resolve 'leihs.mail.main/-main)))
  (wait))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; DEV ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask dev
  []
  (set-env! :source-paths #(conj % "boot" "src/dev"))
  (require 'app
           '[clojure.tools.namespace.repl :as ctnr])
  identity)

(deftask reset
  "Reload all changed namespaces on the classpath
  and reset the application state continuously."
  []
  ; use `resolve` because of dynamic `require` (not top-level):
  ; https://github.com/boot-clj/boot/wiki/Boot-Troubleshooting#why-isnt-require-working-in-my-pod
  (with-pass-thru _
    (apply (resolve 'ctnr/set-refresh-dirs)
           (get-env :directories))
    (with-bindings {#'*ns* *ns*}
      ((resolve 'app/reset)))))

(deftask focus
  []
  (comp (dev)
        (repl "-s")
        (watch)
        (reset)))
