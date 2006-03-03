(* 
  File: Extractor.sig

*)
signature Extractor =
  sig
    val finalise : unit -> unit
    val initialise : string -> unit
    val recordLocals : string * string list -> unit
    val recordSystem : string -> unit
  end;
