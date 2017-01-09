# arboreal

This repo is very much a thought experiment, and as such is neither ready
for use, nor a fully formed concept.

Arboreal is a way of structuring pure functions without littering your code with
side effects. When developing API handlers, one problem I repeatedly noticed
was not realizing where Database reads/writes or other side effects were
occurring. Since Clojure is a functional language, it's nice to not have functions 
with deeply nested side effects. Doing this makes the vast majority of the 
function impure, and thus untestable.
Arboreal lets you build an expressive handler out of pure functions, and takes
care of side-effects behind the scenes.

# Example
```
(defn create-organization!
  [{:keys [name] :as request}]
  (let [org-id (persist/create-organization! {:name name})
        group-id (persist/create-group! {:name "Admin"
                                         :type :admin
                                         :organization-id org-id})
        admin-user-id (get-session-user request)]
    (provision-org org-id)
    (persist/add-member group-id admin-user-id)
    (grant-permissions group-id org-id :admin)
    (grant-permissions group-id group-id :admin)))
```
Instead of dealing with this as a monolithic function, this can be considered
as a call tree, with side-effects at the nodes. The tree is constructed with
child nodes depending on the outcome of their parents. Side effects are then
separated as discrete functions, and a handler is constructed by concatenating
pure  functions outlining intent to call a side effect, rather than just doing
it.
```
(defsideeffect persist-organization!
  "Actualy performs the side effect"
  [org]
  (persist org))

...

(defintent build-organization persist-organization!
  "Registers intent to do a side effect, returning args to
  pass to the side-effect function"
  [org-name]
  {:org-name org-name
   :created-by :system
   :created-at 0})

...

(deftwig create-organization :org build-organization :org-id)

...

(defn create-organization-handler
  "Handler constructed of pure functions"
  [request]
  (arborize {:org "Name of the org"}
    (branch create-organization
            provision-org)
    (branch create-group
            grant-group-permissions)))
```

Side effects should be recognized for what they are, and kept distinct from
pure functions which are otherwise lovely little testable things. In the above
example, we can now test that the create-organization function creates the
right shaped object and returns an intent to persist it, without worrying
about whether the thing was persisted. Data persistence, message passing
or other side-effect riddled operations can be integration tested on their
own merit.

The shape of the call tree is also more obvious.
