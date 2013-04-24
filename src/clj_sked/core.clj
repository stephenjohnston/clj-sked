(ns clj-sked.core)

;; scheduler needs a next-fire-map
;; and it also needs a name-map
;;
(defn create-scheduler []
  { :next-fire-map (ref (sorted-map))
    :name-map (ref (sorted-map))  })

;; TODO: Need some validation on the item to ensure it contains
;;   :fire-time
;;   :name
;;   :cron-expression (or :trigger?)
;;   :job-fn
(defn insert-job [scheduler item]
  (let [next-fire-map (:next-fire-map scheduler)
        name-map (:name-map scheduler)]
    (dosync
     (alter next-fire-map assoc (:fire-time item) item)
     (alter name-map assoc (:name item) item))))

(defn remove-job [scheduler item-name]
  (let [next-fire-map (:next-fire-map scheduler)
        name-map (:name-map scheduler)
        target-item (name-map item-name)]
    (dosync
     (alter next-fire-map dissoc (:fire-time target-item))
     (alter name-map dissoc item-name))
    target-item))

;; TODO: Is this correct?
;; This may not be quite right depending if we remove the job from the maps after we wake
;; or if we remove the job from the maps before we sleep.
(defn get-next-fire-time [scheduler]
  (let [next-fire-map (:next-fire-map scheduler)]
    (first @next-fire-map)))

(defn get-job [scheduler item-name]
  (get (:name-map scheduler) item-name))
