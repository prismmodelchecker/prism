//Project 3, Question 3

dtmc

	const double a = 0.01;
	const double b = 0.05;

module Generator
	x:[0..1] init 0;

	[]true -> 0.5:(x'=0) + 0.5:(x'=1);
endmodule

module Line1
	y:[0..1];
	error1prev:bool init false;
	error1:bool init false;
	assigned_y:bool init false;
	
	[]x=0 -> a:(y'=1) & (error1prev'=error1) & (error1'=true) & (assigned_y'=true) + 1-a:(y'=0) & (error1prev'=error1) & (error1'=false) & (assigned_y'=true);
	[]x=1 -> b:(y'=0) & (error1prev'=error1) & (error1'=true) & (assigned_y'=true) + 1-b:(y'=1) & (error1prev'=error1) & (error1'=false) & (assigned_y'=true);

endmodule

module Line2
	z:[0..1];
	error2prev:bool init false;
	error2:bool init false;
	
	[]assigned_y & y=0 -> a:(z'=1) & (error2prev'=error2) & (error2'=true) + 1-a:(z'=0) & (error2prev'=error2) & (error2'=false);
	[]assigned_y & y=1 -> b:(z'=0) & (error2prev'=error2) & (error2'=true) + 1-b:(z'=1) & (error2prev'=error2) & (error2'=false);

endmodule
