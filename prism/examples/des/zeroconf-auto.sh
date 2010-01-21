# simple model

# min/max probability using fresh/used ip address
../../bin/ptamc zeroconf/simple4.des 'fresh_use' 'true' -min
../../bin/ptamc zeroconf/simple4.des 'used_use' 'true' -max

# time bounded
../../bin/ptamc zeroconf/simple4_deadline.des 'fresh_before' 'true' -min
../../bin/ptamc zeroconf/simple4_deadline.des 'fresh_after' 'true' -max

####################################################################

# min/max probability using fresh/used ip address
../../bin/ptamc zeroconf/sender4.des zeroconf/environment.des 'fresh_use:*' 'true' -min
../../bin/ptamc zeroconf/sender4.des zeroconf/environment.des 'used_use:*' 'true' -max

# time bounded
../../bin/ptamc zeroconf/sender4.des zeroconf/environment.des zeroconf/deadline25.des 'used_use:*:before' 'true' -max
../../bin/ptamc zeroconf/sender4.des zeroconf/environment.des zeroconf/deadline50.des 'used_use:*:before' 'true' -max
../../bin/ptamc zeroconf/sender4.des zeroconf/environment.des zeroconf/deadline100.des 'used_use:*:before' 'true' -max

