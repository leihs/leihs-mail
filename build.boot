(set-env!
  :source-paths #{"src/all" "leihs-clj-shared/src"}
  :resource-paths #{"resources"}
  :project 'leihs-mail
  :version "0.1.0-SNAPSHOT"
  :dependencies '[
                  [aleph "0.4.6"]
                  [bidi "2.1.3"]
                  [cheshire "5.8.1"]
                  [org.clojure/clojure "1.9.0"]
                  [com.github.mfornos/humanize-slim "1.2.2"]
                  [compojure "1.6.1"]
                  [hiccup "1.0.5"]
                  [boot-fmt "0.1.8"]
                  [camel-snake-kebab "0.4.0"]
                  [clj-pid "0.1.2"]
                  [com.draines/postal "2.0.3"]
                  [hikari-cp "2.7.1"]
                  [honeysql "0.9.4"]
                  [environ "1.1.0"]
                  [io.dropwizard.metrics/metrics-core "4.0.3"]
                  [io.dropwizard.metrics/metrics-healthchecks "4.0.3"]
                  [io.forward/yaml "1.0.9"]
                  [log4j/log4j "1.2.17" :exclusions
                   [javax.mail/mail
                    javax.jms/jms
                    com.sun.jdmk/jmxtools
                    com.sun.jmx/jmxri]]
                  [logbug "4.2.2"]
                  [nilenso/honeysql-postgres "0.2.4"]
                  [org.bouncycastle/bcprov-jdk15on "1.54"]
                  [org.clojure/data.generators "0.1.2"]
                  [org.clojure/java.jdbc "0.7.8"]
                  [org.clojure/test.check "0.9.0" :scope "test"]
                  [org.clojure/tools.cli "0.3.7"]
                  [org.clojure/tools.logging "0.4.1"]
                  [org.clojure/tools.namespace "0.3.0-alpha4"]
                  [org.slf4j/slf4j-log4j12 "1.7.25"]
                  [pandect "0.6.1"]
                  [pg-types "2.4.0-PRE.1"]
                  [ring "1.6.3"]
                  [ring-middleware-accept "2.0.3"]
                  [ring/ring-json "0.4.0"]
                  [ring/ring-jetty-adapter "1.7.1"]
                  ])

(require '[boot-fmt.core :refer [fmt]])

(task-options!
  target {:dir #{"target"}}
  aot {:all true}
  repl {:init-ns 'user}
  sift {:include #{#"leihs-mail.jar"}}
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

(deftask uberjar
  "Build an uberjar of the application."
  []
  (comp (aot)
        (uber)
        (jar)
        (sift)
        (target)))

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
  "Development profile to be used in combination with other tasks."
  []
  (set-env! :source-paths #(conj % "src/dev"))
  (require 'reset '[clojure.tools.namespace.repl :as ctnr])
  identity)

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
