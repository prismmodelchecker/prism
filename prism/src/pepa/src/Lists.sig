(*
   File:  Lists.sig

   Operations on string lists
*)

signature Lists =
  sig
    val addifmissing : string * string list -> string list
    val insertu : string * string list -> string list
    val merge : string list * string list -> string list
    val sortu : string list -> string list
  end;

