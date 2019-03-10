for tr in 8 10 12; do
  for cs in 10; do
	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -jacobi -epsilon 1e-8 > ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_jacobi.log

	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -gs -epsilon 1e-8 > ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_gs.log

	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -improvedgs -epsilon 1e-8 -nocompact> ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_improvedgs.log

	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -ii -epsilon 1e-10 -nocompact> ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_intervaliteration.log
  done
done

for tr in 8 9; do
  for cs in 15; do
	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -jacobi -epsilon 1e-8 > ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_jacobi.log

	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -gs -epsilon 1e-8 > ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_gs.log

	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -improvedgs -epsilon 1e-8 -nocompact> ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_improvedgs.log

	../../bin/prism ../models/crowds/crowds.pm ../models/crowds/positive.pctl -const TotalRuns=$tr,CrowdSize=$cs -s -ii -epsilon 1e-10 -nocompact> ../logs/crowds/crowds_TotalRuns_$tr\_CrowdSize_$cs\_intervaliteration.log
  done
done

