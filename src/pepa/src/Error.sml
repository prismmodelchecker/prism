(* 
 File: Error.sml

 Structure which prints PEPA semantic error messages. 
*)

structure Error 
       :> Error 
= struct
  exception Fatal_error of string
  (* Generic error routine *)
  fun error classification message = 
      let val diagnostic = classification ^ " error: " ^ message
      in 
         (* Write errors to file for MLj version *)
         if !Debugging.on 
         then let val errorlog = TextIO.openOut("pepacompiler.err") 
   	       in 
	          TextIO.output (errorlog, ">> " ^ diagnostic ^ "\n"); 
	          TextIO.flushOut errorlog;
	          TextIO.closeOut errorlog
   	       end
         else ();
	 raise Fatal_error (diagnostic)
       end

  (* Fatal error messages *)
  fun fatal_error s = error "Fatal" s

  (* Internal error messages *)
  fun internal_error s = error "PEPA Workbench internal" s

  (* Syntax and parser error messages *)
  fun lexical_error s = error "Lexical" s
  fun parse_error s   = error "Parsing" s

  (* Warning messages are not printed more than once, except
     after a forced reset *)
  local
    val warningsSoFar = ref [] : string list ref
    fun lookAndAdd (s, []) = (warningsSoFar := s :: !warningsSoFar; false)
      | lookAndAdd (s, h::t) = s=h orelse lookAndAdd (s, t)
  in
    fun alreadyWarned s = lookAndAdd (s, !warningsSoFar)
    fun reset () = warningsSoFar := []
  end

  fun warning s = 
     if alreadyWarned s then () else 
        (TextIO.output (TextIO.stdErr, 
           ">> Warning: " ^ s ^ "\n");
         TextIO.flushOut TextIO.stdErr)

end;
