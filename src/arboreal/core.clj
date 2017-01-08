(ns arboreal.core
  (:require [clojure.spec :as s]))

(defn defn-with-meta
  [n k syms]
  `(defn ~(with-meta n (assoc (meta n) ::fn-type k)) ~@syms))

(defmacro defpure
  [n & syms]
  (defn-with-meta n :pure syms))

(defmacro defeffect
  [n & syms]
  (defn-with-meta n :side-effect syms))

(defmacro defintent
  [n side-effect args & fms]
  `(defn ~(with-meta n (assoc (meta n) ::fn-type :intent))
     ~args [~side-effect ~@fms]))

(defn inject-arg
  [arg form]
  `(~(first form) ~arg ~@(next form)))

(defn function-type
  [f]
  (::fn-type (meta f)))

(defn -execute
  [f arg]
  `(case (function-type (var ~f))
     :pure   (~f ~arg)
     :intent (let [[x# & ys#] (~f ~arg)]
               (apply x# ys#))))

(defmacro execute
  [f arg]
  (-execute f arg))

(defmacro execute-with-tag
  [f tag arg]
  (assoc arg tag (-execute f arg)))

(defn inject-result
  [f1 arg form]
  `(~(first form) (execute ~f1 ~arg) ~@(next form)))

(defn inject-arg-on-thread
  [troop f]
  `(future ~(inject-arg troop f)))

;; Basic `circus` macro, to inject the trigger into the `then` call
(defmacro circus
  [troop then]
  (inject-arg troop then))

(defmacro with-tag
  [f arg tag]
  `(exeute-with-tag ~f ~arg tag)

(defmacro execute-f
  [f arg]
  `(cond
    (and
      (list? ~f)
      (= (var with-tag) ~(first f)))
        (execute-with-tag ~@(rest f) ~arg)))

;; Basic `then` macro to thread calls into successive `after` calls
(defmacro then
  [troop f & afters]
  (if afters
    `(do
      ~@(map #(inject-result f troop %) afters))
    `(execute ~f ~troop)))

;; I don't like this...I need to find a better, less clunky way
;; to tag the response of each pure/intent function
(defmacro then-tagged
  [troop f tag & afters]
  (if afters
    `(do
      ~@(map #(inject-result f troop %) afters))
    `(execute-with-tag ~f ~troop ~tag)))
