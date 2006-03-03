(*
   File: Debugging.sml
*)

structure Debugging :> Debugging =
struct
  (* Flag set by the checkRequested function *)
   val on = ref false 

  (* Check if the user has requested debugging information
     by creating a file ".pepa_compiler_debug_true" in the
     current working directory  *)
   fun checkRequested () =
       let val is = TextIO.openIn ".pepa_compiler_debug_true"
        in on := true; 
           TextIO.closeIn is
       end handle _ => on := false;

   fun say s = 
       if !on then 
	   (TextIO.output(TextIO.stdErr, s);
            TextIO.flushOut(TextIO.stdErr))
       else ()

   fun log s = say ("DEBUGGING>> " ^ s ^ "\n")

   fun logList [] = say "\t*empty*\n"
     | logList l  = (say "\t[[["; logList' l ; say "]]]\n")
   and logList' [] = ()
     | logList' [x] = say x
     | logList' (x::xs) = (say x; say ", "; logList' xs)

   fun logListList [] = say "\t*empty*\n"
     | logListList l  = (say "    [[[[[\n"; logListList' l ; say "    ]]]]]\n")
   and logListList' [] = ()
     | logListList' [x] = logList x
     | logListList' (x::xs) = (logList x; say ",\n"; logListList' xs)

end;
