;; logistics domain Typed version.
;;

(define (domain logistics)
  (:requirements :strips :typing :durative-actions) 
  (:types truck
          airplane - vehicle
          package
          vehicle - physobj
          airport
          location - place
          city
          place 
          physobj - object)
  
  (:predicates 	(in-city ?loc - place ?city - city)
		(at ?obj - physobj ?loc - place)
		(in ?pkg - package ?veh - vehicle))
  
(:durative-action LOAD-TRUCK
   :parameters    (?pkg - package ?truck - truck ?loc - place)
   :duration (= ?duration 10)
   :condition  (and (over all (at ?truck ?loc))
   	       	    (at start(at ?pkg ?loc)))
   :effect     (and (at start (not (at ?pkg ?loc)))
   	       	    (at end (in ?pkg ?truck))))

(:durative-action LOAD-AIRPLANE
  :parameters   (?pkg - package ?airplane - airplane ?loc - place)
   :duration (= ?duration 15)
  :condition    (and (at start (at ?pkg ?loc))
  		     (over all (at ?airplane ?loc)))
  :effect       (and (at start (not (at ?pkg ?loc)))
  		     (at end (in ?pkg ?airplane))))

(:durative-action UNLOAD-TRUCK
  :parameters   (?pkg - package ?truck - truck ?loc - place)
   :duration (= ?duration 9)
  :condition    (and (over all (at ?truck ?loc))
  		     (at start (in ?pkg ?truck)))
  :effect       (and (at start (not (in ?pkg ?truck)))
  		     (at end (at ?pkg ?loc))))

(:durative-action UNLOAD-AIRPLANE
  :parameters    (?pkg - package ?airplane - airplane ?loc - place)
   :duration (= ?duration 12)
  :condition     (and (at start (in ?pkg ?airplane))
  		      (over all (at ?airplane ?loc)))
  :effect        (and (at start (not (in ?pkg ?airplane)))
  		      (at end (at ?pkg ?loc))))

(:durative-action DRIVE-TRUCK
  :parameters 	 (?truck - truck ?loc-from - place ?loc-to - place ?city - city)
   :duration (= ?duration 20)
  :condition     (and (at start (at ?truck ?loc-from))
  		      (over all (in-city ?loc-from ?city))
		      (over all (in-city ?loc-to ?city)))
  :effect        (and (at start (not (at ?truck ?loc-from)))
  		      (at end (at ?truck ?loc-to))))

(:durative-action FLY-AIRPLANE
  :parameters	 (?airplane - airplane ?loc-from - airport ?loc-to - airport)
  :duration (= ?duration 40)
  :condition     (at start (at ?airplane ?loc-from))
  :effect        (and (at start (not (at ?airplane ?loc-from)))
  		      (at end (at ?airplane ?loc-to))))
)
