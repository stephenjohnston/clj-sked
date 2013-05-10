(ns clj-sked.core-test
  (:use clojure.test
        clj-sked.core))

(deftest creating-scheduler-test
  (testing "Test creating an empty scheduler"
    (let [sked (create-scheduler)]
      (is (= {}  @(:next-fire-map sked)))
      (is (= {} @(:name-map sked)))
      (is (= nil @(:background-future sked))))))

(deftest insert-job-test
  (testing "Test creating an empty scheduler"
    (let [sked (create-scheduler)
          testjob {:fire-time (+ 5000 (System/currentTimeMillis)),
                   :name "testjob" }
          dmy   (insert-job sked testjob)]
      (is (= 1 (count @(:next-fire-map sked))))
      (is (= 1 (count @(:name-map sked))))
      (is (= (future? @(:background-future sked))))
;;      (Thread/sleep 5500)
;;      (is (= 0 (count @(:next-fire-map sked))))
;;      (is (= 0 (count @(:name-map sked))))
;;      (is (= (future-done? @(:background-future sked))))
      )))

(defn insert-job-into-maps-helper [joblist]
  (let [sked (create-scheduler)]
    (doseq [job joblist]
      (insert-job-into-maps sked job))
    (keys @(:next-fire-map sked))))


(deftest insert-job-into-maps-test
  (testing "Ensure that the order of the next-fire-map is correct"
    ;; after inserting these jobs, the next-fire-map should have job3, job1, then job2
    ;; test inserting these in reverse order
    (let [joblist '( {:fire-time 6500 :name "job1" }
                     {:fire-time 5000 :name "job2" }
                     {:fire-time 1000 :name "job3" } )
          keys  (insert-job-into-maps-helper joblist)]
      (is (= 1000 (nth keys 0)))
      (is (= 5000 (nth keys 1)))
      (is (= 6500 (nth keys 2))))))

(deftest insert-job-into-maps-test2
  (testing "Ensure that the order of the next-fire-map is correct"
    ;; after inserting these jobs, the next-fire-map should have job3, job1, then job2
    ;; test inserting these in order
    (let [joblist '( {:fire-time 1000 :name "job1" }
                     {:fire-time 5000 :name "job2" }
                     {:fire-time 6500 :name "job3" } )
          keys  (insert-job-into-maps-helper joblist)]
      (is (= 1000 (nth keys 0)))
      (is (= 5000 (nth keys 1)))
      (is (= 6500 (nth keys 2))))))

(deftest insert-job-into-maps-test3
  (testing "Ensure that the order of the next-fire-map is correct"
    ;; after inserting these jobs, the next-fire-map should have job3, job1, then job2
    (let [joblist '( {:fire-time 6500 :name "job1" }
                     {:fire-time 1000 :name "job2" }
                     {:fire-time 5000 :name "job3" } )
          keys  (insert-job-into-maps-helper joblist)]
      (is (= 1000 (nth keys 0)))
      (is (= 5000 (nth keys 1)))
      (is (= 6500 (nth keys 2))))))

(deftest get-job-test1
  (testing "Check to make sure we can retreive inserted jobs by name"
    (let [jobvec [ {:fire-time 6500 :name "job3" }
                   {:fire-time 1000 :name "job1" }
                   {:fire-time 5000 :name "job2" }]
          sked (create-scheduler)]
      (doseq [job jobvec]
        (insert-job-into-maps sked job))
      (is (= (get-job sked "job1") (nth jobvec 1)))
      (is (= (get-job sked "job2") (nth jobvec 2)))
      (is (= (get-job sked "job3") (nth jobvec 0))))))

(def sleep-until-fire-time
  (ns-resolve 'clj-sked.core
              'sleep-until-fire-time))

(deftest sleep-until-fire-time-test
  "Test to make sure sleeping until a time of one second from now works correctly"
  (let [start-time (System/currentTimeMillis)
        slp (sleep-until-fire-time (+ 1000 start-time))
        end-time (System/currentTimeMillis)]
    (is (>= end-time (+ 1000 start-time)))))
