(* 
 File: Semantic.sig

 Signature for the structure which performs semantic analysis of
 PEPA models

*)

signature Semantic =
sig
  val analyse : Pepa.Component -> Pepa.Component
end;
