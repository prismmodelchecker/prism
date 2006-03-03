(* 
 File: Parser.sig

 Signature for the structure which contains the PEPA parser
*)

signature Parser =
sig
  val parse : Lexer.token list -> Pepa.Component
end;
