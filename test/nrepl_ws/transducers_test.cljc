(ns nrepl-ws.transducers-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [nrepl-ws.transducers :refer :all]))

(s/def ::reply-msgs (s/cat :not-done-msgs (s/* (s/and string? #(re-matches #"[a-zA-Z0-9]+" %) #(not= "done" %)))
                           :done-msg #{"done"}))

(s/def ::replies (s/+ ::reply-msgs))

(defspec counts-match
  10
  (prop/for-all [replies (s/gen ::replies)]
                (=
                 (count (s/conform ::replies replies))
                 (count (sequence (partition-when #(= "done" %)) replies)))))

(comment
  (gen/generate (s/gen ::reply-msgs)))

(comment
  (gen/generate (s/gen ::replies)))

(comment
  (s/conform ::reply-msgs ["hello" "world" "done"]))

(comment
  (s/conform ::replies ["hello" "world" "done" "done" "more" "done"]))

(comment
  (defn partition-by-and-remove
    [pred]
    (comp (partition-by pred)
          (remove #(every? pred %)))))

;; with transduce

(comment
  (transduce (remove #(= "done" %)) conj ["hello" "world" "done" "done" "more" "done"]))

(comment
  (transduce (partition-by #(= "done" %)) conj ["hello" "world" "done" "done" "more" "done"]))

(comment
  (transduce (partition-when #(= "done" %)) conj ["hello" "world" "done" "done" "more" "done"]))

;; with sequence

(comment
  (sequence (remove #(= "done" %)) ["hello" "world" "done" "done" "more" "done"]))

(comment
  (sequence (partition-by #(= "done" %)) ["hello" "world" "done" "done" "more" "done"]))

(comment
  (sequence (partition-when #(= "done" %)) ["hello" "world" "done" "done" "more" "done"]))

(comment
  (def ch (async/chan 10
                      (partition-when #(= "done" %)))))

(comment
  (async/>!! ch "hello"))

(comment
  (async/>!! ch "world"))

(comment
  (async/>!! ch "done"))

(comment
  (let [[v c] (async/alts!! [ch
                             (async/timeout 1000)])]
    (println "got val!" v "from" c)))
