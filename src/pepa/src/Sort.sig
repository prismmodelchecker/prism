(* 
 File: Sort.sig

 Signature for the structure which contains utility functions 
 for sorting string lists.
*)

signature Sort =
sig
  val quicksort : string list -> string list
end;
