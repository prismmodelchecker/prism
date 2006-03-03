(* 
  File: CommandLine.sig

  Used to compile the MLJ version of the Workbench only
*)
signature CommandLine =
sig
   val setArguments : string list -> unit
   val arguments : unit -> string list
end;
