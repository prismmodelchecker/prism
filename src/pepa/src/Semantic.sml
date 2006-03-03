(* 
 File: Semantic.sml

 The structure which performs semantic analysis of PEPA models.
 Errors include component definitions which are missing or duplicated.
 Warnings include components which are defined but never used and
 occurrences of taus or wholly unused activity identifiers in any set 
 (hiding or cooperation).

*)


structure Semantic 
       :> Semantic
= struct

  local
    fun act (Pepa.PREFIX ((a, _), C)) = a :: act C
      | act (Pepa.VAR _) = []
      | act (Pepa.RATE _) = []
      | act (Pepa.CHOICE (P, Q)) = act P @ act Q
      | act (Pepa.COOP (P, Q, _)) = act P @ act Q
      | act (Pepa.HIDING (P, _)) = act P
      | act (Pepa.CONSTS (_, P, C)) = act P @ act C

    fun rates (Pepa.PREFIX ((_, r), C)) = r :: rates C
      | rates (Pepa.VAR _) = []
      | rates (Pepa.RATE r) = [] (* this is a definition, not a use *)
      | rates (Pepa.CHOICE (P, Q)) = rates P @ rates Q
      | rates (Pepa.COOP (P, Q, _)) = rates P @ rates Q
      | rates (Pepa.HIDING (P, _)) = rates P
      | rates (Pepa.CONSTS (_, P, C)) = rates P @ rates C

    fun used (Pepa.VAR I) = [I]
      | used (Pepa.RATE _) = []
      | used (Pepa.PREFIX (_, C)) = used C
      | used (Pepa.CHOICE (P, Q)) = used P @ used Q
      | used (Pepa.COOP (P, Q, _)) = used P @ used Q
      | used (Pepa.HIDING (P, _)) = used P
      | used (Pepa.CONSTS (_, P, C)) = used P @ used C

    fun defined (Pepa.CONSTS (I, Pepa.RATE _, C)) = defined C
      | defined (Pepa.CONSTS (I, _, C)) = I :: defined C
      | defined _ = []

    fun ratesDefined (Pepa.CONSTS (I, Pepa.RATE _, C)) = I :: ratesDefined C
      | ratesDefined _ = []
  in
    val activities = Sort.quicksort o act
    val used = Sort.quicksort o used
    val ratesUsed = Sort.quicksort o rates
    val defined = Sort.quicksort o defined
    val ratesDefined = Sort.quicksort o ratesDefined
  end

  fun rmDup [] = []
    | rmDup [x] = ([x] : string list)
    | rmDup (x1 :: (t as (x2 :: _))) =
        let val tail = rmDup t
         in if x1 = x2 then tail else x1 :: tail
        end

  local 
    fun fstNotSnd ([], _) = []
      | fstNotSnd (fst, []) = (fst : string list)
      | fstNotSnd (h1::fst, h2::snd) =
	if h1 < h2 
	then h1 :: fstNotSnd (fst, h2::snd)
	else if h1 = h2 then fstNotSnd (fst, h2::snd)
	     else fstNotSnd (h1::fst, snd)
  in
    fun firstNotSecond (fst, snd) = fstNotSnd (rmDup fst, rmDup snd)
  end

  fun checkDup s [] = ()
    | checkDup s [_] = ()
    | checkDup s (x1 :: (t as (x2 :: _))) =
      if x1 = x2 
      then 
         Error.fatal_error (s ^ " multiply defined: " ^ x1)
      else checkDup s t

  fun reportNotUsed s C = 
      Error.warning (s ^ " definition unused: " ^ C)

  fun reportNotDefined s C = 
      Error.fatal_error (s ^ " not defined: " ^ C)

  (* Seems not to be the bug *)
  (* This version of member works on sorted string lists *)
  fun member (s : string) =
    let
      fun mem [] = false
        | mem (h::t) =
          not (h > s) andalso (h = s orelse mem t)          
    in mem end

  fun purifylist act class =
    let
      fun pL [] = []
        | pL (h :: t) = 
           case h of
             "tau" =>
               (Error.warning ("tau found in " ^ class ^ " set, ignoring");
                pL t)
           | id  => 
               if member id act
               then id :: pL t
               else (Error.warning ("unused activity name `" ^
                  id ^ "' found in " ^ class ^ " set, ignoring");
                pL t)

    in pL end

  fun purify act =
    let 
      fun p (Pepa.COOP (P, Q, L)) = 
          Pepa.COOP (p P, p Q, purifylist act "cooperation" L)
        | p (Pepa.HIDING (P, L)) =
          Pepa.HIDING (p P, purifylist act "hiding" L)
        | p (Pepa.CONSTS (I, P, C)) =
          Pepa.CONSTS (I, p P, p C)
        | p C = C
    in p end

  fun analyse C = 
    let
      val usedActivities = rmDup (activities C)
      val usedNames = used C
      val definedNames = defined C
      val defNotUsed = firstNotSecond (definedNames, usedNames)
      val _ = map (reportNotUsed "Component") defNotUsed

      val usedRates = ratesUsed C
      val definedRates = ratesDefined C
      val defNotUsedRates = firstNotSecond (definedRates, usedRates)
      val _ = map (reportNotUsed "Rate") defNotUsedRates

      val usedNotDefined = firstNotSecond (usedNames, definedNames)
      val _ = map (reportNotDefined "Component") usedNotDefined
      val _ = checkDup "Component" definedNames

      val usedNotDefinedRates = 
          firstNotSecond (usedRates, Sort.quicksort ("infty" :: definedRates))
		       (* infty is predefined *)

      val _ = map (reportNotDefined "Rate") usedNotDefinedRates
      val _ = checkDup "Rate" definedRates

    in
      purify usedActivities C
    end

end;
