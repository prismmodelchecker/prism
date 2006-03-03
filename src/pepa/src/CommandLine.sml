(* 
  File: CommandLine.sml

  Used to compile the MLJ version of the Workbench only
*)
structure CommandLine :> CommandLine =
struct

   val args = ref [] : string list ref

   fun setArguments s = args := s

   fun arguments () = !args

end;
