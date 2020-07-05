(load-file "shared-clj/deps.clj")

(set-env!
  :source-paths #{"src/all" "shared-clj/src"}
  :project 'leihs-mail
  :version "0.1.0-SNAPSHOT"
  :dependencies (extend-shared-deps '[[boot-fmt "0.1.8"]
                                      [clj-pid "0.1.2"]
                                      [com.draines/postal "2.0.3"]
                                      [spootnik/signal "0.2.1"]]))

(require '[boot-fmt.core :refer [fmt]])

(task-options!
  target {:dir #{"target"}}
  aot {:all true}
  repl {:init-ns 'user}
  jar {:file "leihs-mail.jar", :main 'leihs.mail.main}
  fmt {:options {:width 80,
                 :old? false,
                 :map {:lift-ns? false},
                 :fn-map {"comment" :flow, "while" :arg1},
                 :comment
                 {:wrap? false, :inline? false, :count? false},
                 :vector {:respect-nl? true}},
       :files #{"src/all"},
       :mode :overwrite,
       :really true})

(deftask prod
  "Production profile to be used in combination with other tasks."
  []
  (with-pass-thru _
    (set-env! :resource-paths #{"resources/prod"})))

(deftask uberjar
  "Build an uberjar of the application."
  []
  (comp (prod)
        (aot)
        (uber)
        (sift :add-resource #{"resources/prod"})
        (jar)
        (target)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; DEV ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftask dev
  "Development profile to be used in combination with other tasks."
  []
  (set-env! :source-paths #(conj % "src/dev")
            :resource-paths #{"resources/dev"})
  (require 'reset '[clojure.tools.namespace.repl :as ctnr])
  identity)

(deftask run
  "Run the application with given opts."
  []
  (comp (dev)
        (with-pass-thru _
          (require 'leihs.mail.main)
          (->> *args*
               (cons "run")
               (apply (resolve 'leihs.mail.main/-main))))
        (wait)))

(ns-unmap *ns* 'repl)
(deftask repl
  "Overriding built-in repl with dev profile."
  []
  (comp (dev)
        (boot.task.built-in/repl)))

(deftask reset
  "Reset changed namespaces using clojure.tools.namespace."
  []
  ; use `resolve` because of dynamic `require` (not top-level):
  ; https://github.com/boot-clj/boot/wiki/Boot-Troubleshooting#why-isnt-require-working-in-my-pod
  (with-pass-thru _
    (apply (resolve 'ctnr/set-refresh-dirs)
           (get-env :directories))
    (with-bindings {#'*ns* *ns*}
      ((resolve 'reset/reset)))))

(deftask
  focus
  "Watch for changed files, reload namespaces and reset application state."
  []
  (comp (dev)
        (boot.task.built-in/repl "-s")
        (watch)
        (reset)))
