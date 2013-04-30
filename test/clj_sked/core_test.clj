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

(deftest insert-job-into-maps-test
  (testing "Ensure that the order of the next-fire-map is correct"
    (let [sked (create-scheduler)
          ;; after inserting these jobs, the next-fire-map should have job3, job1, then job2
          dmy   (insert-job-into-maps sked {:fire-time 5000 :name "job1" })
          dmy2  (insert-job-into-maps sked {:fire-time 6500 :name "job2" })
          dmy3  (insert-job-into-maps sked {:fire-time 1000 :name "job3" })
          nfm   @(:next-fire-map sked)
          keys  (vec (keys nfm))]
      (is (= 1000 (nth keys 0)))
      (is (= 5000 (nth keys 1)))
      (is (= 6500 (nth keys 2))))))
