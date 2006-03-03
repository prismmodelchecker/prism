(* 
 File: Lexer.sml

 Structure which performs lexical analysis of PEPA models.
*)

structure Lexer :> Lexer 
= struct
  datatype token = 
      Ident of Pepa.Identifier
    | Float of string
    | Symbol of char

  fun isIdChar c = Char.isAlphaNum c orelse Char.contains "'_*-" c

  fun isPepaSym c = Char.contains "/{}<>().,=+#;" c

  fun replace_T_by_infty "T" = "infty"
    | replace_T_by_infty "top" = "infty"
    | replace_T_by_infty s = s 

  fun analyse []  = []
    | analyse (a::x) =
        if Char.isSpace a then analyse x else 
	if isPepaSym a then Symbol a :: analyse x else 
	if Char.isDigit a then getnumber [a] x else 
        if Char.isAlpha a then getword [a] x else
	   Error.lexical_error ("Unrecognised token "
				      ^ implode (a :: x))
  and getword l [] = [ident l]
    | getword l (a::x) = 
        if isIdChar a
	then getword (a::l) x
	else ident l :: analyse (a::x)
  and getnumber l [] = [float l]
    | getnumber l (a::x) = 
        if Char.isDigit a orelse a = #"."
	then getnumber (a::l) x
	else float l :: analyse (a::x)

  and ident l = 
        Ident (replace_T_by_infty (implode (rev l)))
  and float l = 
        Float (implode (rev l))

end;
