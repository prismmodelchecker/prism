#A script with programmable completion for BASH
#In order to apply it, put a line "source path/to/this/script" somewhere where it gets executed every time you start bash, e.g. in ~/.profile 

#Features
#* completion of switches. Just type hyphen and hit TAB twice
#* completion of constants. If a model name was specified, hit TAB twice when you are specifying a parameter to -const switch
#* completion for -{pta,scc,sim}method switch parameters

_prismcomplete() {
	local cur prev MODELFILE PROPFILE curconst preconst CONSTANTS PARS SWITCHES
	COMPREPLY=()
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"

	if [ "$prev" = -const ]; then

		#get the name of the model
		MODELFILE=`echo $COMP_LINE | grep -E -o "[^ ]*[.]([psn]m|smg) " | sed "s/ ^//"`
		#expand tilde
		MODELFILE=`eval echo $MODELFILE`
		#check if model has been specified and is valid
		test -n "$MODELFILE"|| return 0;
		test -e $MODELFILE || return 0; 

		#get the parts after/before the last comma
		curconst=`echo $cur | sed "s/.*,\([^,]*\)$/\\1/"`
		preconst=`echo $cur | sed "s/[^,]*$//" | sed "s/.*[=]//"` 

		#check if we are after = sign, which means that we don't autocomplete
		(echo $curconst | grep -q "=") && return 1;

		#get a list of constants from the model file
		CONSTANTS=`grep "^[ \t]*const " $MODELFILE | sed "s/.*const[ ]*\([^=;]*\)[\=;].*/\\1/" | sed "s/[^ ]* \([^ ]*\)[ ]*/\\1=/"`

		COMPREPLY=( `compgen -W "$CONSTANTS" -- $curconst` )
		
		COMPREPLY=("${COMPREPLY[@]/#/$preconst}")
		return 0;
	fi;

	if [ "$prev" = -property ]; then
	
		PROPFILE=`echo $COMP_LINE | grep -E -o "[^ ]*[.](pctl|props)" | sed "s/ ^//"`
		PROPFILE=`eval echo $PROPFILE`
		test -n "$PROPFILE"|| return 0;
		test -e $PROPFILE || return 0; 
	
		#get the number of properties
		SEQUENCE=`sed "s/\/\/.*//" $PROPFILE | grep "[a-z]" | awk '{print NR, " ", $0}' | sed "s/^[0-9]*[^\\"]*\\"\(.*\)\\":.*/\\1/" | sed "s/^\([0-9][0-9]*\).*$/\\1/"`
	
		COMPREPLY=( `compgen -W "$SEQUENCE" -- $cur` )
		COMPREPLY=("${COMPREPLY[@]/%/ }")
		return 0;
	fi;

	if [ "$prev" = -simmethod ]; then
		PARS="ci aci apmc sprt"
		COMPREPLY=( `compgen -W "$PARS" -- $cur` )
		COMPREPLY=("${COMPREPLY[@]/%/ }")
		return 0;
	fi;

	if [ "$prev" = -ptamethod ]; then
		PARS="games digital"
		COMPREPLY=( `compgen -W "$PARS" -- $cur` )
		COMPREPLY=("${COMPREPLY[@]/%/ }")
		return 0;
	fi;

	if [ "$prev" = -sccmethod ]; then
		PARS="xiebeerel lockstep sccfind"
		COMPREPLY=( `compgen -W "$PARS" -- $cur` )
		COMPREPLY=("${COMPREPLY[@]/%/ }")
		return 0;
	fi;
	if [[ $cur == -* ]] ; then
		SWITCHES="-noprob0 -noprob1 -help -version -pctl -property -const -steadystate -transient -simpath -nobuild -test -testall -importpepa -importtrans -importstates -importlabels -importinitdist -dtmc -ctmc -mdp -exportresults -exporttrans -exportstaterewards -exporttransrewards -exportrewards -exportstates -exportlabels -exportmatlab -exportmrmc -exportrows -exportordered -exportunordered -exporttransdot -exporttransdotstates -exportdot -exportbsccs -exportsteadystate -exporttransient -exportprism -exportprismconst -mtbdd -m -sparse -s -hybrid -h -ptamethod -power -jacobi -gaussseidel -bgaussseidel -pgaussseidel -bpgaussseidel -jor -sor -bsor -psor -bpsor -omega -relative -absolute -epsilon -maxiters -nopre -fair -nofair -fixdl -noprobchecks -zerorewardcheck -nossdetect -sccmethod -symm -aroptions -exportadv -exportadvmdp -verbose -extraddinfo -extrareachinfo -nocompact -sbl -sbmax -gsl -gsmax -cuddmaxmem -cuddepsilon -sim -simmethod -simsamples -simconf -simwidth -simapprox -simmanual -simvar -simmaxrwd -simpathlen"
		COMPREPLY=( `compgen -W "$SWITCHES" -- $cur` )
		COMPREPLY=("${COMPREPLY[@]/%/ }")
		return 0;
	fi;
	#COMPREPLY=( `compgen -A file -- $cur` )
	return 0;
}

complete -o nospace -F _prismcomplete -o default prism

