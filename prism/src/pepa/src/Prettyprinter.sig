(* 
 File: Prettyprinter.sig

 Signature for the structure which prettyprints PEPA models
*)

signature Prettyprinter =
sig
  datatype printmode = compressed | uncompressed | verbose | asPRISM
  val print : printmode -> Pepa.Component -> string
  val printtransition : printmode -> Pepa.Transition -> string
end;
