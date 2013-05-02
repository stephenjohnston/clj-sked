# clj-sked
Our goal is to create a high performance scheduler, written entirely in clojure.
The project is actively being developed.  As of right now, the scheduler is not yet functioning or complete.

Example usage:
```clojure
    user> (defn compute-answer [] 42) ;; the job I want to schedule
    #'user/compute-answer
    user> (def sked (create-scheduler)) ;; create the scheduler
    #'user/sked
    user> (def mypromise (insert-job sked {:name "foo" :fire-time (+ 3000 (System/currentTimeMillis)) :job-fn compute-answer}))
    "starting scheduler"
    #'user/mypromise
    user> @mypromise ;; wait for the job to fire and display the result
    "*** firing and removing job for" "foo"
    42
```
## License

Copyright © 2013 

Distributed under the Eclipse Public License, the same as Clojure.
