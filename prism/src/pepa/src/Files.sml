(* 
 File: Files.sml

 File handling for the PEPA Workbench
*)

structure Files 
       :> Files 
= struct

  val job = ref ""

  fun jobName () = !job

  fun readFile s =
    let 
      val reverse = implode o rev o explode
      fun hasext s = String.isPrefix "apep." (reverse s)
      val f = TextIO.openIn s handle _ => 
	TextIO.openIn (s^".pepa") handle _ =>
	  if hasext s
	  then Error.fatal_error ("Cannot open \"" ^ s ^ "\"")
	  else Error.fatal_error ("Cannot open \"" ^ s ^ "\" or \"" ^ 
					   s ^ ".pepa\"")

      val c = ref (SOME #" ")
      val r = ref [] : char list ref 

      fun prepend (ref NONE) = ()
        | prepend (ref (SOME c)) = r := c :: !r
    in
      job := s;
      while (c := TextIO.input1 f; !c <> NONE) do
	if !c = SOME #"%" (* Added comment syntax *)
	then (TextIO.inputLine f; ())
	else prepend c;
      TextIO.closeIn f;
      rev (!r)
    end

end;
