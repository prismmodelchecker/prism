(*
   File: Debugging.sig

*)
signature Debugging =
sig
    (* Check if the user has requested debugging information
       by creating a file ".pepa_compiler_debug_true" in the
       current working directory  *)
    val checkRequested : unit -> unit

    (* Flag set by the checkRequested function *)
    val on : bool ref

    val log : string -> unit
    val logList : string list -> unit
    val logListList : string list list -> unit
end;
