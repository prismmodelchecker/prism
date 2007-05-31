#const N#
label "one_trying" = #| i=1:N#p#i#=1#end#;label "one_critical" = #| i=1:N#p#i#=2#end#;

// Mutual exclusion
#+ i=1:N#(p#i#=2?1:0)#end# <= 1

// Liveness: If a process is trying, then eventually a process enters the critical section
"one_trying" => P>=1 [ true U "one_critical" ]
