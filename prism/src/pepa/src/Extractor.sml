(* 
   File: Extractor.sml 

*)
structure Extractor :> Extractor =
struct
  val log = ref NONE : TextIO.outstream option ref

  (* Check if the user has requested logging information
     by creating a file ".pepa_compiler_log_true" in the
     current working directory  *)
  fun initialise jobname = 
       let val is = TextIO.openIn ".pepa_compiler_log_true"
        in log := SOME (TextIO.openOut (jobname ^ ".log"));
	   TextIO.closeIn is
       end handle _ => log := NONE;

  fun finalise() = case !log of NONE => () 
                      | SOME os => TextIO.closeOut(os)

  fun print s = case !log of NONE => () 
                      | SOME os => TextIO.output(os, s)

  fun listToString sep [] = ""
    | listToString sep [x] = x
    | listToString sep (x::xs) = x ^ sep ^ (listToString sep xs)

  fun recordLocals (component, localStates) = 
      print (component ^ " = {" ^ listToString ", " localStates ^ "}\n")

  fun recordSystem (system) = 
      print ("System = (" ^ system ^ ")\n")

end;

