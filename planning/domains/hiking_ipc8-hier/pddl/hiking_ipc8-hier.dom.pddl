;  (c) 2001 Copyright (c) University of Huddersfield
;  Automatically produced from GIPO from the domain hiking
;  All rights reserved. Use of this software is permitted for non-commercial
;  research purposes, and it may be copied only for that use.  All copies must
;  include this copyright message.  This software is made available AS IS, and
;  neither the GIPO team nor the University of Huddersfield make any warranty about
;  the software or its performance.

(define (domain hiking)
  (:requirements :strips :equality :typing :durative-actions)
  (:types car tent person couple place )
  (:predicates 
              (at_tent ?x1 - tent ?x2 - place)
              (at_person ?x1 - person ?x2 - place)
              (at_car ?x1 - car ?x2 - place)
              (partners ?x1 - couple ?x2 - person ?x3 - person)
              (up ?x1 - tent)
              (down ?x1 - tent)
              (walked ?x1 - couple ?x2 - place)
              (next ?x1 - place ?x2 - place)
)

  (:durative-action put_down
         :parameters ( ?x1 - person ?x2 - place ?x3 - tent)
	 :duration (= ?duration 1)
         :condition (and (over all (at_person ?x1 ?x2))
	 	    	 (over all (at_tent ?x3 ?x2))
			 (at start (up ?x3)))
         :effect (and (at end (down ?x3))
	 	      (at start (not (up ?x3))))
)
  (:durative-action put_up
         :parameters ( ?x1 - person ?x2 - place ?x3 - tent)
 	 :duration (= ?duration 1)
         :condition (and (over all (at_person ?x1 ?x2))
	 	    	 (over all (at_tent ?x3 ?x2))
			 (at start (down ?x3)))
         :effect (and (at end (up ?x3))
	 	      (at start (not (down ?x3))))
)

  (:durative-action drive_passenger
         :parameters ( ?x1 - person ?x2 - place ?x3 - place ?x4 - car ?x5 - person)
 	 :duration (= ?duration 1)
         :condition (and (at start (at_person ?x1 ?x2))
	 	       	    (at start (at_car ?x4 ?x2))
			    (at start (at_person ?x5 ?x2))
			    (over all (not (= ?x1 ?x5))))
         :effect (and (at end (at_person ?x1 ?x3))
	 	      (at start (not (at_person ?x1 ?x2)))
		      (at end (at_car ?x4 ?x3))
		      (at start (not (at_car ?x4 ?x2)))
		      (at end (at_person ?x5 ?x3))
		      (at start (not (at_person ?x5 ?x2))))
)

  (:durative-action drive
         :parameters ( ?x1 - person ?x2 - place ?x3 - place ?x4 - car)
 	 :duration (= ?duration 1)
         :condition (and (at start (at_person ?x1 ?x2))
	 	    	 (at start (at_car ?x4 ?x2)))
         :effect (and (at end (at_person ?x1 ?x3))
	 	      (at start (not (at_person ?x1 ?x2)))
		      (at end (at_car ?x4 ?x3))
		      (at start (not (at_car ?x4 ?x2))))
)

  (:durative-action drive_tent
         :parameters ( ?x1 - person ?x2 - place ?x3 - place ?x4 - car ?x5 - tent)
 	 :duration (= ?duration 1)
         :condition (and (at start (at_person ?x1 ?x2))
	 	    	 (at start (at_car ?x4 ?x2))
			 (at start (at_tent ?x5 ?x2))
 			 (at start (down ?x5))
			 (over all (down ?x5)))
         :effect (and (at end (at_person ?x1 ?x3))
	 	      (at start (not (at_person ?x1 ?x2)))
		      (at end (at_car ?x4 ?x3))
		      (at start (not (at_car ?x4 ?x2)))
		      (at end (at_tent ?x5 ?x3))
		      (at start (not (at_tent ?x5 ?x2))))
)

  (:durative-action drive_tent_passenger
         :parameters ( ?x1 - person ?x2 - place ?x3 - place ?x4 - car ?x5 - tent ?x6 - person)
	 :duration (= ?duration 1)
	 :condition (and (at start (at_person ?x1 ?x2))
	 	    	 (at start (at_car ?x4 ?x2))
			 (at start (at_tent ?x5 ?x2))
			 (at start (down ?x5))
			 (at start (at_person ?x6 ?x2))
			 (over all (not (= ?x1 ?x6))))
         :effect (and (at end (at_person ?x1 ?x3))
	 	      (at start (not (at_person ?x1 ?x2)))
		      (at end (at_car ?x4 ?x3))
		      (at start (not (at_car ?x4 ?x2)))
		      (at end (at_tent ?x5 ?x3))
		      (at start (not (at_tent ?x5 ?x2)))
		      (at end (at_person ?x6 ?x3))
		      (at start (not (at_person ?x6 ?x2))))
)

  (:durative-action walk_together
         :parameters ( ?x1 - tent ?x2 - place ?x3 - person ?x4 - place ?x5 - person ?x6 - couple)
	 :duration (= ?duration 1)
         :condition (and (at end (at_tent ?x1 ?x2))
	 	    	 (at end (up ?x1))
			 (at start (at_person ?x3 ?x4))
			 (over all (next ?x4 ?x2))
			 (at start (at_person ?x5 ?x4))
			 (over all (not (= ?x3 ?x5)))
			 (at start (walked ?x6 ?x4))
			 (over all (partners ?x6 ?x3 ?x5)))
         :effect (and (at end (at_person ?x3 ?x2))
	 	      (at start (not (at_person ?x3 ?x4)))
		      (at end (at_person ?x5 ?x2))
		      (at start (not (at_person ?x5 ?x4)))
		      (at end (walked ?x6 ?x2))
		      (at start (not (walked ?x6 ?x4))))
)

)

