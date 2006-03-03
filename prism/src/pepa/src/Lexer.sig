(* 
 File: Lexer.sig

 Functions which perform lexical analysis of PEPA models.
*)

signature Lexer =
sig

  datatype token = 
      Ident of Pepa.Identifier
    | Float of string
    | Symbol of char

  val analyse : char list -> token list

end;

