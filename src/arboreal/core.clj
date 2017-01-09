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

(defmacro deftwig
  [n extract function tag]
  `(defn ~n
     [bolus#]
     (assoc bolus# ~tag (execute ~function (~extract bolus#)))))

 (defmacro branch
   [bolus & fs]
   (let [$bolus (gensym "$bolus_")]
     `(let [~$bolus ~bolus]
        (future (-> ~$bolus ~@fs))
        ~$bolus)))

 (defmacro arborize
   [bolus & forms]
   `(do
     (branch ~bolus ~@forms)
     nil))
