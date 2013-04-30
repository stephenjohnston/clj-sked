(ns clj-sked.core)

(defn create-scheduler []
  { :next-fire-map (ref (sorted-map))
    :name-map (ref (sorted-map))
    :background-future (ref nil)
  } )

(defn- get-fire-map-first-entry [fire-map-first-entry]
  (if (nil? fire-map-first-entry)
    nil
    (second fire-map-first-entry)))

(defn get-next-fire-time [scheduler]
  (let [next-fire-map (:next-fire-map scheduler)]
    (if (empty? @next-fire-map)
      nil
      (get-fire-map-first-entry (first @next-fire-map)))))

(defn remove-job [scheduler item-name]
  (let [next-fire-map (:next-fire-map scheduler)
        name-map (:name-map scheduler)
        target-item (name-map item-name)]
    (dosync
     (alter next-fire-map dissoc (:fire-time target-item))
     (alter name-map dissoc item-name))
    target-item))

(defn- sleep-until-fire-time [next-fire-time]
  (let [current-time (System/currentTimeMillis)
        sleep-time (- next-fire-time current-time)]
    (when ( > sleep-time 0)
      (Thread/sleep sleep-time))))

(defn- service-next-job [scheduler next-items-lst]
  (sleep-until-fire-time (:fire-time (first next-items-lst)))
  (doseq [item next-items-lst]
    ;;(prn "*** firing and removing job for " (:name item))
    (remove-job scheduler (:name item))))

(defn service-all-scheduled-jobs [scheduler]
  (loop []
    (if (.isInterrupted (Thread/currentThread)) ;; if this thread has been interrupted, return nil (and stop looping)
      nil
      (let [next-items-lst (get-next-fire-time scheduler)]
        (if (nil? next-items-lst) ;; if there is no task to fire, return nil (and stop looping)
          nil
          (do ;; process the next job
            (service-next-job scheduler next-items-lst)
            (recur)))))))

(defn start-scheduler [scheduler]
  (let [future-ref (:background-future scheduler)
        new-future (future (service-all-scheduled-jobs scheduler))]
    (dosync (ref-set future-ref new-future))))

(defn cancel-future? [scheduler item]
  (let [fire-time (:fire-time item)
        next-item-lst (get-next-fire-time scheduler)
        bg-future (:background-future scheduler)]
    (if (and (not (nil? next-item-lst)) (< fire-time (:fire-time (first next-item-lst))) (not (nil? bg-future)))
      (future-cancel @bg-future)
      false)))

(defn insert-job-into-maps [scheduler item]
  (let [next-fire-map (:next-fire-map scheduler)
        name-map (:name-map scheduler)]
    (dosync
     (let []
       (alter next-fire-map (fn [map itm] (let [lst (get map (:fire-time itm) (list))]
                                           (if (empty? lst)
                                             (assoc map (:fire-time itm) (list itm))
                                             (assoc map (:fire-time itm) (conj lst item))
                                             ))) item)
       (alter name-map assoc (:name item) item)))))

;; TODO: Need some validation on the item to ensure it contains
;;   :fire-time
;;   :name
;;   :cron-expression (or :trigger?)
;;   :job-fn
(defn insert-job [scheduler item]

  (let [cancelled? (cancel-future? scheduler item)
        bg-future (:background-future scheduler)]
    (insert-job-into-maps scheduler item)
    (when (or cancelled? (nil? @bg-future) (future-done? @bg-future))
      (prn "starting scheduler")
       (start-scheduler scheduler))))

(defn get-job [scheduler item-name]
  (get (:name-map scheduler) item-name))
