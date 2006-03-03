(* This is the PEPA-to-PRISM compiler *)

structure PEPA2PRISM :> PEPA2PRISM =
struct 

  val PEPA_version = "0.03.3 \"Inverleith Row\"";
  val PEPA_compiled = "07-02-2003";
  val p2p as this_file = "PEPA2PRISM.sml";

  val runningInteractively = ref false;

  fun println s = 
      if !runningInteractively 
      then (TextIO.output (TextIO.stdOut, s);
            TextIO.flushOut TextIO.stdOut)	    
      else ()

  exception UndeclaredIdentifier of string

  fun lookup I []             = raise UndeclaredIdentifier (I)
    | lookup I ((Id, P) :: t) = if I = Id then P else (lookup I t);

  fun member x [] =  false
    | member x (h::t) = (x=h) orelse (member x t);

  local
    fun remove x [] = []
      | remove x (h::t) = if x=h then remove x t else h :: remove x t
  in
    infix minus
    fun l minus [] = l
      | l minus (h::t) = (remove h l) minus t;
  end;

  exception Act of Pepa.Component and Pass of Pepa.Component;

  local
      fun active E D (Pepa.PREFIX ((a, "infty"), P))  = active E D P
        | active E D (Pepa.PREFIX ((a, r), P))        = Lists.insertu (a, active E D P)
        | active E D (Pepa.CHOICE (P, Q))             = Lists.merge(active E D P, active E D Q)
        | active E D (Pepa.HIDING (P, L))             = (active E D P) minus L 
        | active E D (Pepa.COOP (P, Q, L))            = Lists.merge (active E D P, active E D Q)
        | active E D (Pepa.VAR I)                     = 
             if member I D then [] 
             else (* Look up a previously memoised result if possible *)
                  (case Alphabets.lookup I of
                     SOME (a as _::_, _) => a
                   | _ => let val activeSet = active E (I :: D) (lookup I E)
                           in Alphabets.recordActive(I, activeSet);
                              activeSet
                          end)
        | active E D P                                = raise Act P
      and passive E D (Pepa.PREFIX ((a, "infty"), P)) = Lists.insertu (a, passive E D P)
        | passive E D (Pepa.PREFIX ((a, r), P))       = passive E D P
        | passive E D (Pepa.CHOICE (P, Q))            = Lists.merge(passive E D P, passive E D Q)
        | passive E D (Pepa.HIDING (P, L))            = (passive E D P) minus L 
        | passive E D (Pepa.COOP (P, Q, L))           = Lists.merge(passive E D P, passive E D Q) 
                                                        minus L
        | passive E D (Pepa.VAR I)                    = 
             if member I D then [] 
             else (* Look up a previously memoised result if possible *)
                  (case Alphabets.lookup I of
                     SOME (_, p as _::_) => p
                   | _ => let val passiveSet = passive E (I :: D) (lookup I E)
                           in Alphabets.recordPassive(I, passiveSet);
                              passiveSet
                          end)
        | passive E D P                          = raise Pass P
      and alphabet E D P = Lists.merge(active E D P, passive E D P)
    in
      fun act E D S  = 
          let 
             val _ = Debugging.log "computing active set ... (act.1/2)"
             val result = active E D S 
             val _ = Debugging.log "finished computing active set ... (act.2/2)"
          in result end
      fun pass E D S  = 
          let 
             val _ = Debugging.log "computing passive set ... (pass.1/2)"
             val result = passive E D S 
             val _ = Debugging.log "finished computing passive set ... (pass.2/2)"
          in result end
      fun alph E D S  = 
          let 
             val _ = Debugging.log "computing alphabet ... (alph.1/2)"
             val result = alphabet E D S 
             val _ = Debugging.log "finished computing alphabet ... (alph.2/2)"
          in result end
    end

    exception Modules of Pepa.Component
    fun modules E D (Pepa.PREFIX (_, P))           = []
      | modules E D (Pepa.CHOICE (P, Q))           = []
      | modules E D (Pepa.HIDING (P, L))           = (modules E D P) 
      | modules E D (Pepa.COOP (Pepa.VAR P, Pepa.VAR Q, L))  = 
	     (P :: 
	      (if member P D then [] else modules E (P :: Q :: D) (lookup P E))) @
	     (Q :: 
	      (if member Q D then [] else modules E (P :: Q :: D) (lookup Q E)))
      | modules E D (Pepa.COOP (Pepa.VAR P, Q, L))  = 
	     (P :: 
	      (if member P D then [] else modules E (P :: D) (lookup P E))) @
	     modules E (P :: D) Q
      | modules E D (Pepa.COOP (P, Pepa.VAR Q, L))  = 
	     modules E (Q :: D) P @
	     (if member Q D then [] else modules E (Q :: D) (lookup Q E)) @
	     [Q]
      | modules E D (Pepa.COOP (P, Q, L))           = 
	     modules E D P @ modules E D Q
      | modules E D (Pepa.VAR I)                    = [I]
      | modules E D P                               = raise Modules P;

    local 
      fun addifmissing (x, []) = [x]
        | addifmissing (x, h::t) = if x=h then h::t else h :: addifmissing (x, t)
      fun duplicates [] = []
	| duplicates (h :: t) = 
	     if member h t
	     then addifmissing (h, duplicates t)
	     else duplicates t
    in fun moduletypes E _ P = duplicates (modules E [] P)
    end

  (* module specifications --- find the module and work out *)

  infix intersect
  fun [] intersect l = []
    | (h::t) intersect l = 
      if member h l then h :: (t intersect l) else t intersect l

  fun spec E System Module =
  let
    val A = act E [] (Pepa.VAR Module)

    (* Cool function definition.  Read the @ symbol as `union'. *)
    fun sp (Pepa.COOP (P, Q, L)) coopset =
	(if P = Pepa.VAR Module orelse Q = Pepa.VAR Module
	 then (A intersect L) @ (A intersect coopset)
	 else []) @ 
	(sp P (coopset @ L)) @ 
	(sp Q (coopset @ L))

      | sp (Pepa.HIDING (P, L)) coopset = 
	(if P = Pepa.VAR Module 
	 then ((coopset minus L) intersect A)
	 else []) @ 
	sp P (coopset minus L)

      | sp _ _ = []

  in Lists.sortu (sp System [])
  end

  local
    fun derivs E D (Pepa.PREFIX (_, P)) = derivs E D P
      | derivs E D (Pepa.CHOICE (P, Q)) = 
		Lists.merge (derivs E D P, derivs E D Q)
      | derivs E D (Pepa.HIDING (P, L)) = derivs E D P
      | derivs E D (Pepa.VAR P) = 
	if member P D then D 
        else derivs E (Lists.insertu (P, D)) (lookup P E)
(*
(* Lookup a previously memoised result if possible *)
                  (case Derivatives.lookup P of
                     SOME ds => ds
                   | _ => let val derivativeSet = derivs E (Lists.insertu (P, D)) (lookup P E)
                           in Derivatives.recordDerivatives(P, derivativeSet);
                              derivativeSet
                          end)
*)
      | derivs _ D _ = D
  in
    fun derivatives E D s = derivs E D (lookup s E)
  end;
(* *)
  type SystemSpecification = 
         { activity_names : string list, 
	       derivative_names : string list,
	       entries : string list, 
	       name : Pepa.Identifier, 
	       rpc_names : string list }
  type Analysis = 
	 { active : string list, 
	   concurrency : Pepa.Identifier list, 
	   passive : string list,
	   system : SystemSpecification list, 
	   modules : string list, 
	   moduletypes : Pepa.Identifier list }
(* *)
  local
    fun analyse E (Pepa.CONSTS (I, P, L)) = analyse ((I, P) :: E) L
      | analyse E P = 
	  let 
	      val _ = Debugging.log "inside analysis, computing modules ... (analyse.1/7)"
	      val modules_P  = modules E [] P
	      val _ = Debugging.log "inside analysis, sorting module results ... (analyse.2/7)"
	      val modules_P' = Lists.sortu modules_P 
	      val _ = Debugging.log "inside analysis, computing module types ... (analyse.3/7)"
	      val moduletypes_P = moduletypes E [] P
	      val _ = Debugging.log "inside analysis, finished computing module types ... (analyse.4/7)"
	      fun module_analyse s = 
		let 
		    val _ = Debugging.log "inside module analysis ... (module_analyse.1/3)"
		    val own_entries = spec E P s
		    val _ = Debugging.log "inside module analysis ... (module_analyse.2/3)"
	            val derivs = derivatives E [] s
		    val _ = Debugging.log "inside module analysis ... (module_analyse.3/3)"
		in 
		   { name = s,
		     entries = own_entries,
		     derivative_names = derivs,
		     activity_names = (act E [] (Pepa.VAR s)) 
                                       minus ("tau" :: own_entries),
		     rpc_names = (pass E [] (Pepa.VAR s))
		   }
		end
	      val _ = Debugging.log "inside analysis, starting computing found modules ... (analyse.5/7)"
	      val found_modules = modules_P' minus moduletypes_P
	      val _ = Debugging.log "inside analysis, finished computing found modules ... (analyse.6/7)"
	      val _ = Debugging.log "modules_P' is: ... "
	      val _ = Debugging.logList modules_P'
	      val _ = Debugging.log "... end modules_P'"
	      val found_system = map module_analyse modules_P'
	      val _ = Debugging.log "derivative names are: ... "
	      val _ = Debugging.logListList (map #derivative_names found_system)
	      val _ = Debugging.log "... end derivative names"
	      val _ = Debugging.log "inside analysis, finished computing found modules ... (analyse.7/7)"
	   in { active = (act E [] P) minus ["tau"],
		passive = pass E [] P,
		concurrency = modules_P,
		modules = found_modules,
		moduletypes = moduletypes_P,
		system = found_system
	      }
	  end
  in
     val analyse : Pepa.Component -> Analysis  = analyse []
  end;
(* *)
  fun named [] = true
    | named ((Pepa.PREFIX (_, Pepa.VAR _)) :: t) = named t
    | named _ = false;

  exception Find
  local 
    fun duplicates [] = []
      | duplicates (h::t) = if member h t then h :: duplicates t else duplicates t
    fun get_name (Pepa.PREFIX ((a, _), _)) = a 
      | get_name _ = ""
    fun choices E D (Pepa.PREFIX ((a, "infty"), _)) = []
      | choices E D (P as (Pepa.PREFIX ((a, _), _))) = [P]
      | choices E D (Pepa.CHOICE (P, Q)) = 
	(choices E D P) @ (choices E D Q)
      | choices E D (Pepa.VAR P) = 
	if member P D then [] else choices E (P :: D) (lookup P E)
      | choices _ _ _ = []
    val filter = Lists.sortu o duplicates o (map get_name)
    fun poss_choices E D P = filter (choices E D P)
    fun choosy E P = 
	let val _ = Debugging.log "entered choosy function ... (choosy.1/4)"
	    val C = choices E [] P 
            val _ = Debugging.log "evaluated choices ... (choosy.2/4)"
            val C' = filter C
            val _ = Debugging.log "filtered component list ... (choosy.3/4)"
            val activities = act E [] P
            val _ = Debugging.log "computed activities.... (choosy.4/4)"
	 in (C' intersect activities) <> [] andalso (not (named C))
	end
    fun any [] = false
      | any (h :: t) = h orelse any t

    fun find_in_system [] P = raise Find
      | find_in_system ((h : SystemSpecification) :: t) P =
	if #name(h) = P then h else find_in_system t P

    fun module_is_choosy E (A : Analysis) P = 
        let val _ = Debugging.log "finding choosy modules ... (module_is_choosy.1/4)"
            val derivativeNames = #derivative_names (find_in_system (#system A) P)
            val _ = Debugging.log "found derivative names ... (module_is_choosy.2/4)"
            val derivativeIdentifiers = map Pepa.VAR derivativeNames
            val _ = Debugging.log "constructed derivative identifiers ... (module_is_choosy.3/4)"
            val anyTrue = any (map (choosy E) derivativeIdentifiers)
            val _ = Debugging.log "finding any choosy module ... (module_is_choosy.4/4)"
        in
   	    anyTrue
        end

    fun env (Pepa.CONSTS (I, P, L)) = (I, P) :: env L
      | env _ = []
  in
    fun find_choosy_modules A S = 
       let 
         val _ = Debugging.log "finding choosy modules ... (find_choosy_modules.1/1)"
	 val E = env S
	 fun f [] = []
	   | f (h::t) = if module_is_choosy E A h then h :: f t else f t
       in f end

    fun single_choice [] = true
      | single_choice [_] = true
      | single_choice ((Pepa.PREFIX ((a, _), _)) :: (t as ((Pepa.PREFIX ((b, _), _)) :: _))) = 
	a=b andalso single_choice t
      | single_choice _ = false
  end;
(* *)
  exception Lookup;
  exception Markup;
  exception Comms
(* *)
    fun spandex name =
	let fun dropnum [] = [] 
	      | dropnum (name as h::t) = 
		if Char.isDigit h then dropnum t else name
	 in (implode o rev o tl o dropnum o rev o explode) name
	end

    fun compute_id name =
	let fun getnum [] = [] 
	      | getnum (h::t) = 
		if Char.isDigit h then h :: getnum t else []
	 in (implode o rev o getnum o rev o explode) name
	end

    fun lookup2 [] P = raise Lookup
      | lookup2 ({activity_names, derivative_names, entries, name, rpc_names} :: t) P =
	  if name = P orelse name = spandex P
	  then {entries = entries, name = P, rpc_names = rpc_names} 
	  else lookup2 t P
(* *)
    (* The markup function only processes parallel components *) 
    fun markup moduletypes P =
	let fun skull (Pepa.CONSTS (I, P, L)) = skull L
              | skull P = P
            fun nextindex P [] = 0
	      | nextindex P ((P', n) :: t) =
		if P = P' then n else nextindex P t
	    fun mk E (Pepa.HIDING (P, L)) = 
		let val (E', P') = mk E P in (E', Pepa.HIDING (P', L)) end
	      | mk E (Pepa.COOP (P, Q, L)) = 
		let val (E', P') = mk E P 
		    val (E'', Q') = mk E' Q
		in (E'', Pepa.COOP (P', Q', L)) 
		end
	      | mk E (Pepa.VAR P) = 
		if member P moduletypes
		then let val m = nextindex P E
		      in ((P, m+1) :: E, Pepa.VAR (P ^ "_" ^ Int.toString m))
		     end
		else (E, Pepa.VAR P)
	      | mk _ _ = raise Markup
	 in #2 (mk [] (skull P))
	end
(*   *)
    local
      fun partners P a [] = []
	| partners P a ({ active, activity, passive } :: t) =
	  if passive = P andalso activity = a then
	     active :: partners P a t
	  else 
	     partners P a t
    in
      val partners = fn comms => fn P => fn a => partners P a comms
    end
(* *)
    local
      fun partners P a [] = []
	| partners P a ({ active, activity, passive } :: t) =
	  if spandex passive = P andalso activity = a then
	     (active, compute_id passive) :: partners P a t
	  else 
	     partners P a t
    in
      val partners_in_modules = fn comms => fn P => fn a => partners P a comms
    end
(* *)

    fun internalise L s =
	let fun int {entries, name, rpc_names} = 
		       { entries   = entries minus L, 
		         name      = name, 
			 rpc_names = rpc_names minus L }
	 in map int s
	end


    fun postanalyse 
	({ active, passive, concurrency, modules, moduletypes, system } : Analysis) P =
    let val markP = markup moduletypes P

	(* The comms function only processes parallel components *) 
	fun comms (Pepa.VAR P) = ([lookup2 system P], [])
	  | comms (Pepa.HIDING (P, L)) = 
	    let val (table, results) = comms P 
	     in (internalise L table, results)
	    end
	  | comms (Pepa.COOP (P, Q, L)) = 
	    let val (tableP, resultsP) = comms P
		val (tableQ, resultsQ) = comms Q
	     in (tableP @ tableQ, 
		 resultsP @ resultsQ @ connect (tableP, tableQ, L))
	    end
	  | comms _ = raise Comms
	and connect ( _,  _, []) = []
	  | connect ([],  _,  _) = []
	  | connect ( _, [],  _) = []
	  | connect ({ entries = eP, name = P, rpc_names = rP } :: tableP, tableQ, L) =
	    let fun cp { entries = eQ, name = Q, rpc_names = rQ } =
		let fun PtoQ name = { passive = P, activity = name, active = Q }
		    fun QtoP name = { passive = Q, activity = name, active = P }
		 in map PtoQ ((rP intersect eQ) intersect L) @
		    map QtoP ((rQ intersect eP) intersect L)
		end
	     in map cp tableQ @ connect (tableP, tableQ, L)
	    end

	fun flatten [] = []
	  | flatten (h::t) = h @ flatten t

     in (flatten o #2 o comms) markP
     end


   (* The activeActive function only processes parallel components *) 
      fun activeActive system (Pepa.CONSTS (_, _, L)) = activeActive system L
	| activeActive system (Pepa.VAR P) = ([lookup2 system P], [])
	| activeActive system (Pepa.HIDING (P, L)) = 
	    let val (table, results) = activeActive system P 
	     in (internalise L table, results)
	    end
        | activeActive system (Pepa.COOP (P, Q, L)) = 
	    let val (tableP, resultsP) = activeActive system P
		val (tableQ, resultsQ) = activeActive system Q
	     in (tableP @ tableQ, 
		 resultsP @ resultsQ @ connect (tableP, tableQ, L))
	    end
	| activeActive system _ = Error.internal_error ("Active/active function code")
      and connect ( _,  _, []) = []
	| connect ([],  _,  _) = []
	| connect ( _, [],  _) = []
	| connect ({ entries = eP, name = P, rpc_names = rP } :: tableP, tableQ, L) =
	    let fun cp { entries = eQ, name = Q, rpc_names = rQ } =
		   eP intersect eQ intersect L
	     in map cp tableQ @ connect (tableP, tableQ, L)
	    end



    infix ^^
    fun s1 ^^ s2 = s1 ^ "\n" ^ s2
    infix &
    fun s & "" = s
      | s & t = s ^^ t
    infix &&
    fun "" && "" = ""
      | s  && "" = s
      | "" && t  = t
      | s  && t  = s ^^ "" ^^ t

    fun translate P jobname =
      let 
        val _ = Debugging.log "inside translation function (translate.1)"
	val analysis as { active, passive, concurrency, modules, moduletypes, system } = 
	    analyse P
        val _ = Debugging.logList active;
        val _ = Debugging.logList passive;
        val _ = Debugging.logList modules;
        val _ = Debugging.logList moduletypes;
        val _ = Debugging.log "inside translation function (translate.2)"

        fun strList [] = ""
          | strList [x] = quote x
          | strList (h::t) = quote h ^ ", " ^ strList t
        and quote s = "``" ^ s ^ "''"
        val _ = Debugging.log "inside translation function (translate.3)"
        val _ = case List.concat (#2 (activeActive system P)) of
                  [] => ()
                | aa => Error.warning
                              ("Active/active synchronisation found on " ^ 
                                    strList (Lists.sortu aa))

        val _ = Debugging.log "inside translation function (translate.4)"

	val choosy_modules : Pepa.Identifier list = find_choosy_modules analysis P modules
        val _ = Debugging.logList choosy_modules;

        val _ = Debugging.log "inside translation function (translate.5)"
	val comms = postanalyse analysis P
        val _ = Debugging.log "inside translation function (translate.6)"
	val partners : string -> string -> string list = partners comms
        val _ = Debugging.log "inside translation function (translate.7)"
	val partners_in_modules = partners_in_modules comms

        val _ = Debugging.log "inside translation function (translate.8)"

	fun select _ P [] = []
	  | select f P ((h as { name, entries, 
			      derivative_names, 
			      activity_names,
			      rpc_names }) :: t) =
	    if name = P then f h else select f P t
	fun interface P l = select #entries P l
	fun entries [] = ""
	  | entries (h::t) = "//      activity " ^ h ^^ entries t
	fun modulespecs qualifier [] = ""
	  | modulespecs qualifier (h::t) = 
	      qualifier ^ h ^ 
	      (case interface h system of
		 [] => " empty: only individual or passive activities" | 
		  l => "" ^^ 
		  entries l ^ "//    endinterface ") ^^
	      modulespecs qualifier t

	local
	  fun body (myModuleName as T, myModuleNumber as 1) P =
	    let 
	      val names = select #derivative_names T system
	      val procs = select #activity_names   T system
	      val singular = length names = 1
	      val state = T ^ "_STATE"
	      fun define module [] = ""
		| define module [x] = 
                   "// This module has only one local state even" ^^
                   "// though we write the variable range as [0..1]" ^^
                   "const " ^ module ^ " = 0;" ^^
                   "module " ^ module ^^
                   "" ^^
                   "        " ^ state ^ " : [0..1] init 0;" ^^
                   ""
		| define module l = 
		  let 
		    fun csep n [] = ""
		      | csep n [x] = fmt n x
		      | csep n (h :: t) = fmt n h ^ ";\nconst " ^ csep (n + 1) t
                    and fmt n s = s ^ " = " ^ Int.toString n
		  in
                    "// Descriptive names for the local states of" ^^
                    "// this module, taken from the PEPA input model" ^^
		    "const " ^ csep 0 l ^ ";" ^^
                    "module " ^ module ^^
                    "" ^^
		    "        " ^ state ^ " : [0.." ^ Int.toString(length l - 1) ^ "] init " ^ T ^ ";"  ^^
                    ""
		  end

	      fun sortagent E passives =
		 let 
		   fun sort (Pepa.VAR I) = sort (lookup I E)
		     | sort (P as (Pepa.PREFIX ((a, r), _))) = 
		       if member a ("tau" :: passives) 
		       then ([], [], [P]) 
		       else if r = "infty" 
			    then ([], [P], [])
			    else ([P], [], [])
		     | sort (Pepa.CHOICE (P, Q)) = 
		       let val ((accept1, call1, pcall1), 
				(accept2, call2, pcall2)) = (sort P, sort Q)
			in (accept1 @ accept2, call1 @ call2, pcall1 @ pcall2)
		       end
		     | sort _ = ([], [], [])
		 in
		   sort
		 end

	      fun trsing sep fmt = 
		let 
		  fun tr E (Pepa.CONSTS (I, P, L)) T = tr ((I, P) :: E) L T
		    | tr E _ T = 
		      let 
			  fun trsys sep (Pepa.CHOICE (P,Q)) = 
			        trsys sep P ^^ trsys sep Q
			    | trsys sep (Pepa.PREFIX (("tau", "infty"), Pepa.VAR I)) =
                               ((* A passive action cannot be synchronised upon. *)
                                Error.warning ("Cannot synchronise on a tau action: should not be passive");
                                fmt "" "1" I)
			    | trsys sep (Pepa.PREFIX (("tau", r), Pepa.VAR I)) =
                               (* It's OK to have a tau move if the rate is not infty *)
                                fmt "" r I
			    | trsys sep (P as Pepa.PREFIX ((a, "infty"), Pepa.VAR I)) =
                               ((* All passive actions should be matched.  We generate
                                  a warning for this, but we could classify it as an error 
                                  Note that we check the term, T, not the derivative I. *)
                                if !Debugging.on andalso 
                                   partners T a = [] andalso partners_in_modules T a = [] 
                                then Error.warning ("unmatched passive action: " ^ a)
                                else ();
                                fmt a "1" I)
			    | trsys sep (Pepa.PREFIX ((a, r), Pepa.VAR I)) =
                               ((* PEPA and PRISM do different things with active/active
                                   synchronisation but we have previously trapped this *)
                                fmt a r I)
                            | trsys sep _ =
                                Error.fatal_error "Composition/hiding found in component defn"
		      in trsys sep (lookup T E)
		      end
		in tr
		end

	      fun trcase sep names E P T = 
		let 
		  fun trclause [] = ""
		    | trclause (h::t) = 
                        let
                          fun fmt stateID state action rate newstate =
                              "        " ^
                              "[" ^ action ^ "] (" ^ stateID ^ "=" ^ state ^ ") -> " ^ 
                                    rate ^ " : (" ^ stateID ^ "'=" ^ newstate ^ ");"
                        in 
			  trsing (sep ^ "    ") (fmt state h) E P h ^^
			  trclause t
                        end
		in  trclause names
		end

                val _ = Extractor.recordLocals (T, names)
	    in 
		define T names ^^
		trcase "      " names [] P T ^^
                "endmodule" ^^ ""
	    end
	  | body (myModuleName as T, myModuleNumber as n) P =
            let 
               val thisCopy = T ^ "_" ^ Int.toString n
            in 
               "// We make another copy of module " ^ T ^^
               "module " ^ thisCopy ^ " = " ^ 
                       T ^ "[" ^ T ^ "_STATE=" ^ thisCopy ^ "_STATE]\nendmodule\n"
            end
	in
	  fun modulebodies [] _ = ""
	    | modulebodies [h] P = body h P 
	    | modulebodies (h::t) P = 
	      body h P ^^ modulebodies t P
	end
        fun systemEquation P =
            let 
               fun root (Pepa.CONSTS (I, P, L)) = root L
                 | root P = P
               val extractorString = Prettyprinter.print Prettyprinter.uncompressed (root P)
               val _ = Extractor.recordSystem extractorString
            in
              "// The system equation" ^^
              "system" ^^
                 "        " ^
                 Prettyprinter.print Prettyprinter.asPRISM P ^^
	      "endsystem"    
            end

        fun rates (Pepa.CONSTS (I, Pepa.RATE(r), P)) = 
            "rate " ^ I ^ " = " ^ r ^ ";" ^^ rates P
          | rates (Pepa.CONSTS (I, _, P)) = 
            rates P
          | rates _ = ""

	local 
	    val l : string list ref = ref []
	    fun countList I [] = 0
	      | countList I (h::t) = (if I = h then 1 else 0) + countList I t
	in
	    fun count I = (l := I :: !l; countList I (!l))
	end

        fun flatten (Pepa.CONSTS (I, P, L)) = flatten L
          | flatten (Pepa.HIDING (P, L)) = flatten P
          | flatten (Pepa.COOP (P, Q, L)) = flatten P @ flatten Q
          | flatten (Pepa.VAR (I)) = [(I, count I)]
          | flatten _ = Error.internal_error ("Prefix/choice found in PEPA system equation")

      val _ = Debugging.log "inside translation function (4)"

      in 
	"// Output from the PEPA-to-PRISM compiler" ^^
        "// Version "^ PEPA_version ^^
        "// Released: " ^ PEPA_compiled ^^
	"// " ^^ 
	"// Model file: " ^ jobname ^^ 
(*
        The Date structure is not available under Moscow ML 1.40
	"// Compiled: " ^ Date.fmt "%c" (Date.fromTimeLocal (Time.now())) ^^ 
*)

	"" ^^
	"// All PEPA models define CTMCs so mark this as a stochastic model " ^^
	"stochastic" &&
	"// The rates used in the model " ^^
        rates P && 

	"// Information about components inferred by the compiler" ^^
        "// during static analysis:" ^^
	"//" ^^
	modulespecs "//    interface " modules ^^
	modulespecs "//    interface " moduletypes &&

	modulebodies (flatten P) P &&
        systemEquation P &&
	"// End of output from the PEPA-to-PRISM compiler\n"
      end 


  local
    fun rmext (#"\n" :: t) = rmext t
      | rmext (#"a" :: #"p" :: #"e" :: #"p" :: #"." :: t) = implode (rev t)
      | rmext other = implode (rev other)
  in
    fun removeExtension s = rmext (rev (explode s))
  end

  fun startsWithHyphen [] = false
    | startsWithHyphen (#"-" :: _) = true
    | startsWithHyphen _ = false

  fun isNotFlag s = not (startsWithHyphen (explode s))

  val nonFlagArgs = List.filter isNotFlag

  fun getJobName args =
    case nonFlagArgs args of
    [x] => removeExtension x
    | _ => (TextIO.output (TextIO.stdOut, "Filename: ");
            TextIO.flushOut TextIO.stdOut;
            removeExtension (TextIO.inputLine TextIO.stdIn))
 

  fun compile jobname =
    let
      val theFile = Files.readFile jobname
      val _ = println("Translating the model\n");
      val _ = Debugging.log "Starting semantic analysis"
      val M = Semantic.analyse 
                    (Parser.parse 
                          (Lexer.analyse theFile)) 
      val _ = Debugging.log "Finished semantic analysis"
      val _ = Debugging.log "Starting translation"
      val prism_output = translate M jobname
      val _ = Debugging.log "Finished translation"
    in
      prism_output
    end
  

  fun run jobname =
    let
      val prism_output = compile jobname 
      val prism_out = TextIO.openOut (jobname ^ "_pepa.sm")
    in 
      println("Writing PRISM output to \"" ^ 
	   		        jobname ^ "_pepa.sm\"\n");
      TextIO.output (prism_out, prism_output);
      TextIO.closeOut prism_out
    end

  
  fun main args =
    let
      val _ = runningInteractively := args = []
      val otherArgs = CommandLine.arguments ()
    in
      println("PEPA to PRISM compiler [version "^
			  PEPA_version ^ ", " ^
			  PEPA_compiled ^ "]\n");
      let val jobname = getJobName args
      in  
          (* Check if debugging requested first *)
	  Debugging.checkRequested();
          Extractor.initialise jobname;
          run jobname; 
          Extractor.finalise ();
          println("Exiting PEPA to PRISM compiler.\n")
      end
    end

end;
