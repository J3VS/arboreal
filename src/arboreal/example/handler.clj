(ns arboreal.example.handler
  (:require [arboreal.core :as arb :refer [defpure defintent deftwig
                                           branch arborize]]
            [arboreal.example.side-effects :as se]))

(defintent build-user se/persist-user!
  [{:keys [username password]}]
  {:username username
   :created-by :sytstem
   :created-at 0})

(defintent build-group se/persist-group!
  [{:keys [group-name]}]
  {:group-name group-name
   :created-by :system
   :created-at 0})

(deftwig create-user :user build-user :user-id)
(deftwig create-group :group build-group :group-id)

(defn create-user-handler
  []
  (arborize {:user {:username "username"
                    :password "password"}
             :group {:group-name "New Group"}}
    (branch
      create-user)
    (branch
      create-group)))
