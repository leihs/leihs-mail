(ns leihs.mail.send.result)

(defn failure-message
  [label detail]
  (str (if (keyword? label) (name label) label) ": " detail))

(defn success-result
  []
  {:success true})

(defn failure-result
  ([message]
   {:success false :message message})
  ([label detail]
   (failure-result (failure-message label detail))))
