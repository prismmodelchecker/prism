(*
  File: Derivatives.sig

*)
signature Derivatives = 
sig

   val lookup : string -> string list option

   val recordDerivatives  : string * string list -> unit

end;
