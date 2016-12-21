(ns circus.core)

;; The following is proof of concept; thinking aloud

;; Making a handler request, what are we actually doing?
;; The API Handler is an input point to the "system"
;; The three things we really do are
;; 1) Mutate a persistent store
;; 2) Forward requests to other services
;; 3) Subscribe to events/spawn functions within the API
;; the first two of these are really just the same thing...the former
;; is simply forwarding a request to a database, the third can be decoupled

;; Questions we need to ask when a request comes
;; 1) Is it a valid request?
;;    - does it make sense
;;    - is the user authorized to make the request
;     - these can be handled separately
;; 2) Where do we go from here?
;;    - Where should subsequent requests/calls be sent

;; How do we abstract point 2?
;; Flows take the form of a tree. When one (batch?) of requests
;; is completed, others can continue


;; TODO: the following functions need to be updated to handle the shape
;; of the triggers, as well as a config that outlines how the messages
;; are passed

(defprotocol Effect
  (realize [self]))

(defrecord SideEffect [handler args tag]
  Effect
  (realize [_]
    {:tag (apply handler args)}))

(defn apply-side-effects
  [troop side-effect]
  (-> (realize side-effect
      (merge troop))))

(defn inject-arg
  [arg form]
  `(~(first form) ~arg ~@(next form)))

(defn inject-result
  [f1 arg form]
  `(~(first form) (apply-side-effects ~arg (~f1 ~arg)) ~@(next form)))

(defn inject-arg-on-thread
  [troop f]
  `(future ~(inject-arg troop f)))

;; Basic `circus` macro, to inject the trigger into the `then` call
(defmacro circus
  [troop then]
  (inject-arg troop then))

;; Basic `then` macro to thread calls into successive `after` calls
(defmacro then
  [troop f & afters]
  (if afters
    `(do
      ~@(map (inject-result f troop %) afters))
    `(apply-side-effects ~troop (~f ~troop))))

;;TODO: consider running tree branches on different threads
(defmacro then-async
  [troop & thens]
  (when thens
    `(do
      ~@(map (fn [then]
               (inject-arg-on-thread troop then)) thens))))

;;TODO: think about how to batch and yet still tag each response
(defmacro then-batch
  [troop thens & [after]]
  (when thens
    ))
