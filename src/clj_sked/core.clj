(ns clj-sked.core
  (:import (java.util.concurrent Executors)))

(defn create-scheduler []
  { :next-fire-map (ref (sorted-map))
    :name-map (ref (sorted-map))
    :background-future (ref nil)
    :pool (Executors/newFixedThreadPool 10)
  } )

(defn- get-fire-map-first-entry [fire-map-first-entry]
  (when fire-map-first-entry
    (second fire-map-first-entry)))

(defn get-next-fire-time [scheduler]
  (let [next-fire-map (:next-fire-map scheduler)]
    (when-not (empty? @next-fire-map)
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
   (let [tasks (map (fn [item]
                     (prn "*** firing and removing job for" (:name item))
                     (deliver (:promise item) ((:job-fn item)))
                     (remove-job scheduler (:name item)))  next-items-lst)]
     (.invokeAll (:pool scheduler) tasks)))

(defn service-all-scheduled-jobs [scheduler]
  (loop []
    (when-not (.isInterrupted (Thread/currentThread)) ;; if this thread has been interrupted, return nil (and stop looping)
      (let [next-items-lst (get-next-fire-time scheduler)]
        (when-not (nil? next-items-lst) ;; if there is no task to fire, return nil (and stop looping)
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
                                           (assoc map (:fire-time itm) (conj lst item)))) item)
       (alter name-map assoc (:name item) item)))))

;; TODO: Need some validation on the item to ensure it contains
;;   :fire-time
;;   :name
;;   :cron-expression (or :trigger?)
;;   :job-fn
(defn insert-job [scheduler item]

  (let [ret-val (promise)
        new-item (assoc item :promise ret-val)
        cancelled? (cancel-future? scheduler new-item)
        bg-future (:background-future scheduler)]
    (insert-job-into-maps scheduler new-item)
    (when (or cancelled? (nil? @bg-future) (future-done? @bg-future))
      (prn "starting scheduler")
      (start-scheduler scheduler))
    ret-val))

(defn get-job [scheduler item-name]
  (get @(:name-map scheduler) item-name))
