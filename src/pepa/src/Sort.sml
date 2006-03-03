(* 
 File: Sort.sml

 Utility structure for sorting string lists.
*)

structure Sort 
       :> Sort 
= struct

 fun quicksort ([] : string list) = []
   | quicksort [x] = [x]
   | quicksort (a::rest) = (* the head "a" is the pivot *)
       let 
         fun split(left,right,[]) = quicksort left @ (a::quicksort right)
           | split(left,right,x::l) =
               if x <= a
               then split(x::left,right,l)
               else split(left,x::right,l)
       in 
         split([],[],rest)
     end

end;
