(* 
 File: Error.sig

 Signature for the structure which prints PEPA semantic error messages. 
*)

signature Error =
sig
  (* Thrown to stop the program *)
  exception Fatal_error of string

  (* These functions never return *)
  val fatal_error : string -> 'a
  val internal_error : string -> 'a
  val lexical_error : string -> 'a
  val parse_error : string -> 'a

  (* Warning messages are printed only once *)
  val warning : string -> unit
  (* Reset this for testing purposes *)
  val reset : unit -> unit
end;
