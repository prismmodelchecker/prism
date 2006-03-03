(*
  File: Alphabets.sig

*)
signature Alphabets = 
sig

   val lookup : string -> (string list * string list) option

   val recordActive  : string * string list -> unit
   val recordPassive : string * string list -> unit

end;
