(* 
 File: PEPA2PRISM.sig

*)

signature PEPA2PRISM =
sig
  (* The compile function is given a PEPA file name and
     returns the equivalent PRISM model as a string *)
  val compile : string -> string

  (* This is the main method which can be used if running
     the compiler as a command-line application *)
  val main : string list -> unit
end;
