(*
  File: Derivatives.sml

*)
structure Derivatives :> Derivatives = 
struct

   type value = string list ref

   datatype tree = empty | 
      node of tree ref * (string * value) * tree ref

   fun look (id, tree as ref empty) = NONE
     | look (id, tree as ref (node (left, (id', value), right))) =
         if id = id'
	 then SOME (!value)
	 else if id < id' then look (id, left)
              else look (id, right)

   (* The data structure *)
   val tree = ref empty 

   (* This function is exported from this structure *)
   fun lookup id = look (id, tree)

   fun find (id, f, tree as ref empty) = 
	 let val newValue = ref []
	  in 
             f newValue;
             tree := node (ref empty, (id, newValue), ref empty)
	 end
     | find (id, f, tree as ref (node (left, (id', value), right))) =
         if id = id'
	 then f (value)
	 else if id < id' then find (id, f, left)
              else find (id, f, right)

   fun findAndReplace (id, f) = find (id, f, tree)

   fun addDerivative derivativeSet r = 
       r := derivativeSet

   (* This function is exported from this structure *)
   fun recordDerivatives (id, derivativeSet) = 
       findAndReplace (id, addDerivative derivativeSet)

end;
