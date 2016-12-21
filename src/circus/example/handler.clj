(ns circus.handler
  (:require [circus.core :refer [circus then]]]))

(defn create-user
  [{{{:keys [username]} :params} :request}]
  {:side-effect side-effects/persist-user!
   :args [{:username username}]})

(defn create-group
  [{{:keys [user-id]} :create-user}]
  {:side-effect side-effects/persist-group!
   :args [{:name "group"
           :members #{user-id}}]})

(defn create-user-handler
  []
  (circus {}
    (then create-user
      (then create-group))))
