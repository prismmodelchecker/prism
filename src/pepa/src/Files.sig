(* 
 File: Files.sig

 File handling for the PEPA Workbench
*)

signature Files =
sig

  (* Read in a file, returning its contents, removing
     comments while also processing any options *)
  val readFile : string -> char list

  (* Get the job name *)
  val jobName : unit -> string

end;
