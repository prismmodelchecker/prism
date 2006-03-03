(* 
 File: Prettyprinter.sml

 The structure which prettyprints PEPA models
*)

structure Prettyprinter 
       :> Prettyprinter
= struct

  datatype printmode = compressed | uncompressed | verbose | asPRISM

  fun print mode = 
  let 
     fun pp (Pepa.PREFIX ((alpha, rate), P)) 
	     = "("^printid alpha^", "^printid rate^")."^(pp P)
       | pp (Pepa.CHOICE (P, Q)) 
	     = (pp P) ^ " + " ^ (pp Q)
       | pp (Pepa.COOP (P, Q, L)) 
	     = (pp P) 
	     ^ (if mode = verbose then " <" ^ (printlist L) ^ "> " else "|")
	     ^ (pp Q)
       | pp (Pepa.HIDING (P, [])) 
	     = (pp P)
       | pp (Pepa.HIDING (P, L)) 
	     = (pp P) ^ "/{" ^ (printlist L) ^ "}"
       | pp (Pepa.VAR I) 
	     = printid I
       | pp (Pepa.RATE r) = r
       | pp (Pepa.CONSTS (I, P, L)) 
	     = printid I ^ " = " ^ (pp P) ^ ";\n" ^ (pp L)
     and printid s = s
     and printlist nil        = ""
       | printlist [x]        = printid x
       | printlist (x::y::z)  = printid x ^ ", " ^ (printlist (y::z))
     local 
        val l : string list ref = ref []
        fun countList I [] = 0
          | countList I (h::t) = (if I = h then 1 else 0) + countList I t
     in
        fun count I = (l := I :: !l; countList I (!l))
     end
     fun p2p (Pepa.COOP (P, Q, [])) 
	     = "("
             ^ (p2p P) 
	     ^ (" ||| ")
	     ^ (p2p Q)
             ^ ")"
       | p2p (Pepa.COOP (P, Q, L)) 
	     = "("
             ^ (p2p P) 
	     ^ (" |[" ^ (printlist L) ^ "]| ")
	     ^ (p2p Q)
             ^ ")"
       | p2p (Pepa.HIDING (P, [])) 
	     = "("
             ^ (p2p P)
             ^ ")"
       | p2p (Pepa.HIDING (P, L)) 
	     = "("
             ^ (p2p P) ^ "/{" ^ (printlist L) ^ "}"
             ^ ")"
       | p2p (Pepa.VAR I) 
	     = (case count I of
                 1 => printid I
               | n => printid I ^ "_" ^ Int.toString n)
       | p2p (Pepa.CONSTS (I, P, L)) 
	     = p2p L
       | p2p _ = ""
     and printid s = s
     and printlist nil        = ""
       | printlist [x]        = printid x
       | printlist (x::y::z)  = printid x ^ ", " ^ (printlist (y::z))
  in if mode = asPRISM then p2p else pp
  end

  fun printtransition mode (P, NONE ,Q) =
        (* Note: phantom transition introduced ?? *)
	    (print mode P) ^ 
	    "~~~>" ^ 
	    (print mode Q) ^ "\n"
    | printtransition mode (P, SOME ((a, r), n), Q) =
	    (print mode P) ^ 
	    "~~" ^ a ^ "," ^
                   r ^ "," ^Int.toString n ^ "~>" ^ 
	    (print mode Q) ^ "\n"

end;
