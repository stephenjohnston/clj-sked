(ns clj-sked.core)

(defn create-scheduler []
  { :next-fire-map (ref (sorted-map))
   :name-map (ref (sorted-map))
   :background-future (ref nil)
   })

(defn get-next-fire-time [scheduler]
  (let [next-fire-map (:next-fire-map scheduler)]
    (if (or ( nil? @next-fire-map) (empty? @next-fire-map)) nil
        (let [lst (first @next-fire-map)]
          (if (nil? lst) nil
              (second lst))))))

(defn remove-job [scheduler item-name]
  (let [next-fire-map (:next-fire-map scheduler)
        name-map (:name-map scheduler)
        target-item (name-map item-name)]
    (dosync
     (alter next-fire-map dissoc (:fire-time target-item))
     (alter name-map dissoc item-name))
    target-item))


(defn loop-forever [scheduler]
  (loop [done false]
    ;; if we are done or if this thread has been interrupted, stop looping

    (if (or done (.isInterrupted (Thread/currentThread))) (do (prn "scheduler thread is exiting") nil)
        (let [next-items-lst (get-next-fire-time scheduler)]

          (if (nil? next-items-lst)
            (recur true)
            (let [current-time (System/currentTimeMillis)
                  next-fire-time (:fire-time (first next-items-lst))
                  sleep-time (- next-fire-time current-time)]
              (if (> sleep-time 0)
                (do
                  (prn "sleeping for " sleep-time)
                  (Thread/sleep sleep-time))
                (prn "missed the fire time by: " (- current-time next-fire-time) "ms"))

              (doseq [item next-items-lst]
                ;;(prn "*** firing and removing job for " (:name item))
                (remove-job scheduler (:name item)))
              (recur false)))))))

(defn start-scheduler [scheduler]
  (let [future-ref (:background-future scheduler)
        new-future (future (loop-forever scheduler))]
    (dosync (ref-set future-ref new-future))))

(defn cancel-future? [scheduler item]
  (let [fire-time (:fire-time item)
        next-item-lst (get-next-fire-time scheduler)
        bg-future (:background-future scheduler)]
    (if (and (not (nil? next-item-lst)) (< fire-time (:fire-time (first next-item-lst))) (not (nil? bg-future)))
      (future-cancel @bg-future)
      false)))

;; TODO: Need some validation on the item to ensure it contains
;;   :fire-time
;;   :name
;;   :cron-expression (or :trigger?)
;;   :job-fn
(defn insert-job [scheduler item]
  (let [next-fire-map (:next-fire-map scheduler)
        name-map (:name-map scheduler)
        cancelled? (cancel-future? scheduler item)
        bg-future (:background-future scheduler)]
    (dosync
     (let []
       (alter next-fire-map (fn [map itm] (let [lst (get map (:fire-time itm) (list))]
                                           (if (empty? lst)
                                             (assoc map (:fire-time itm) (list itm))
                                             (assoc map (:fire-time itm) (conj lst item))
                                             ))) item)
       (alter name-map assoc (:name item) item)))
    (when (or cancelled? (nil? @bg-future) (future-done? @bg-future))
      (prn "starting scheduler")
       (start-scheduler scheduler))))

(defn get-job [scheduler item-name]
  (get (:name-map scheduler) item-name))
