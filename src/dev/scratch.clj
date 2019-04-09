(ns scratch
  (:require [postal.core :as postal]))

(comment
  (postal/send-message {:from "me@nitaai.com"
                        :to ["matus.kmit@zhdk.ch"]
                        :subject "Hi!"
                        :body "Test."
                        :X-Tra "Something else"})
  )
