(*
   File: Lists.sml

   Operations on string lists
*)

structure Lists :> Lists =
struct

  fun sortu [] = []
    | sortu (h::t) = insertu (h, sortu t)
  and insertu (x : string, []) = [x]
    | insertu (x, h::t) = 
      if x < h then x :: h :: t else 
      if x = h then h :: t else h :: insertu (x, t);

  fun addifmissing (x : string, []) = [x]
    | addifmissing (x, h::t) = if x=h then h::t else h :: addifmissing (x, t)

  fun merge ([], []) = [] : string list
    | merge ([], l) = l
    | merge (l, []) = l
    | merge (l1 as (h1::t1), l2 as (h2::t2)) =
      if h1 = h2 then merge (t1, l2)
      else if h1 < h2 then h1 :: merge (t1, l2)
      else h2 :: merge(l1, t2)
	
end;
