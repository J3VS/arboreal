(ns circus.example.side-effects)

(def entity-store (atom {}))

(defn persist-user!
  [user]
  (let [uuid (str (gensym))]
    (swap! entity-store assoc-in [:users uuid] user)))

(defn persist-group!
  [group]
  (let [uuid (str (gensym))]
    (swap! entity-store assoc-in [:groups uuid] group)))
