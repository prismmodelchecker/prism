(* 
 File: Parser.sml

 The structure which contains the PEPA parser
*)

structure Parser 
       :> Parser 
= struct

(* Parser tools.  Taken from Chris Reade's
   book ``Elements of Functional Programming''.
*)

(* Utility functions *) 

fun pair a b = (a,b)
fun fst(x,y) = x
fun snd(x,y) = y

fun consonto x a = a :: x
fun fold f [] b     = b
  | fold f (h::t) b = f (h, fold f t b)
fun link llist = fold (op @) llist []

datatype 'a possible = 
    Ok of 'a 
  | Fail

type 'a parser = Lexer.token list -> ('a * Lexer.token list) possible

infixr 4 <&>
infixr 3 <|>
infix  0 modify
 
fun (parser1 <|> parser2) s =
    let fun parser2_if_fail Fail = parser2 s
          | parser2_if_fail x    = x
    in
        parser2_if_fail (parser1 s)
    end

fun (parser modify f) s =
    let fun modresult Fail        = Fail
	  | modresult (Ok (x, y)) = Ok (f x, y)
    in
        modresult (parser s)
    end

fun (parser1 <&> parser2) s =
    let fun parser2_after Fail          = Fail
	  | parser2_after (Ok (x1, s1)) = (parser2 modify (pair x1)) s1
    in
        parser2_after (parser1 s)
    end
 
fun emptyseq s = Ok ([], s)

fun optional pr = (pr modify (consonto []))
                  <|> emptyseq

fun sequence pr =
    let fun seqpr s = ((pr <&> seqpr modify (op ::))
                       <|> emptyseq) s
    in
        seqpr
    end

fun seqwith (front, sep, back) pr =
    let val sep_pr = sep <&> pr               modify snd
        val items  = pr  <&> sequence sep_pr  modify (op ::)	
    in
	front <&> optional items <&> back modify (link o fst o snd)
    end

fun parserList []           = emptyseq
  | parserList (pr :: rest) = pr <&> (parserList rest) modify (op ::)

fun alternatives []           = (fn x => Fail)
  | alternatives (pr :: rest) = pr <|> alternatives rest;
    

 
(* Basic parsers *)

fun variable (Lexer.Ident x :: s)   = Ok (x, s)
  | variable other                  = Fail 

fun float (Lexer.Float x :: s)   = Ok (x, s)
  | float other                  = Fail 

fun literal a (Lexer.Symbol x :: s) = if a = x then Ok (x, s) else Fail
  | literal a other                 = Fail;
    


(* A parser for PEPA.
*)


fun unparenth (bra, (e, ket)) = e;
    
fun bracedseq s = seqwith (literal #"{", literal #",", literal #"}") s;

fun angledseq s = seqwith (literal #"<", literal #",", literal #">") s;

val actlist : Pepa.Identifier list parser    = bracedseq variable;

val cooplist: Pepa.Identifier list parser    = angledseq variable;

fun def  s   = ((adef <&> optional (literal #";" <&> def) modify opt_consts)
               <|> agenta                                                  ) s
 
and adef s   = (optional (literal #"#")  <&> variable  <&>
                literal #"="     <&> agentd                 modify mk_const) s

and agenta s = (agentb <&> optional (literal #"/" <&> actlist) 
                                                           modify mk_hiding) s

and agentb s = (agentc <&> optional (literal #"+" <&> agentb) 
                                                             modify mk_plus) s

and agentc s = (agent <&> optional (cooplist <&> agentc) 
                                                             modify mk_coop) s

and agentd s = (agent <&> optional (literal #"+" <&> agentd) 
                                                             modify mk_plus) s

and agent s  = ((literal #"("    <&> variable    <&>
                 literal #","    <&> variable    <&>
                 literal #")"    <&> literal #"." <&> 
                 agent                                    modify mk_prefix)
               <|> (literal #"(" <&> agenta      <&> literal #")" 
                                                          modify unparenth)
               <|> (variable                               modify mk_agent)
               <|> (float                                  modify mk_rate)) s

and mk_agent s                               = Pepa.VAR s

and mk_rate s                                = Pepa.RATE s

and mk_const (hash, (i, (eq, e)))            = (i, e)

and mk_hiding (P, [])                        = P
  | mk_hiding (P, [(slash, L)])              = Pepa.HIDING (P, L)
  | mk_hiding other                          = error "hiding"

and mk_plus (P, [])                          = P
  | mk_plus (P, [(plus, Q)])                 = Pepa.CHOICE (P, Q)
  | mk_plus other                            = error "plus"

and mk_coop (P, [])                          = P
  | mk_coop (P, [(L, Q)])                    = Pepa.COOP (P, Q, L)
  | mk_coop other                            = error "cooperation"

and mk_prefix (bra, (alpha, (comma, (rate, (ket, (dot, P))))))
                                             = Pepa.PREFIX ((alpha, rate), P)

and opt_consts ((i, e1), [(oper, e2)])       = Pepa.CONSTS (i, e1, e2)
  | opt_consts ((i, e1), _)                  = error ("constants: at or near " ^ i)

and error s                                  = Error.parse_error s
     
local
  fun lit (Lexer.Symbol #";") = ";\n"
    | lit (Lexer.Symbol #"=") = " = "
    | lit (Lexer.Symbol #"+") = " + "
    | lit (Lexer.Symbol #",") = ", "
    | lit (Lexer.Symbol #"<") = " <"
    | lit (Lexer.Symbol #">") = "> "
    | lit (Lexer.Symbol c) = str c
    | lit (Lexer.Float s)  = s
    | lit (Lexer.Ident s)  = s
in
  fun report Fail         = error "ill-formed PEPA model definition"
    | report (Ok (c, [])) = c
    | report (Ok (c, x))  = error (String.concat 
                                  ("Unparsed :-\n" ::  (map lit x)))
end

val parse = report o def

end;
