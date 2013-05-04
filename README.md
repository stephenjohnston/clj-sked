# clj-sked
Our goal is to create a high performance scheduler, written entirely in clojure.
The project is actively being developed.  As of right now, the scheduler is functional, but it still needs some cleanup. 
Example usage:
```clojure
    user> (defn compute-answer [] 42) ; the job I want to schedule
    #'user/compute-answer
    user> (def sked (create-scheduler)) ; create the scheduler
    #'user/sked
    user> (def mypromise (insert-job sked {:name "foo"  ; schedule the job to fire in 3 seconds
                                           :fire-time (+ 3000 (System/currentTimeMillis)) 
                                           :job-fn compute-answer}))
    #'user/mypromise
    user> @mypromise ; wait for the job to fire and display the result
    42
```
## License

Copyright © 2013 

Distributed under the Eclipse Public License, the same as Clojure.
