\ LP format - for model browsing. Use MPS format to capture full model detail.
\ Signature: 0x24c8caa659d02ef
Minimize
  0 mu[2] - eta-0-0 - eta-0-1 - eta-1-0 - eta-2-0 - eta-3-0 + 5 Constant
Subject To
 v-target: mu[3] = 1
 v-no: mu[2] = 0
 state-0-choice-0-combination-0: mu[0] - mu[1] + 1.01 eta-0-0 <= 1.01
 state-0-choice-0-combination-1: mu[0] - 0.8 mu[1] - 0.2 mu[2]
   + 1.01 eta-0-0 <= 1.01
 state-0-choice-1-combination-0: mu[0] - 0.68 mu[3] - 0.32 mu[2]
   + 1.01 eta-0-1 <= 1.01
 state-0-choice-1-combination-1: mu[0] - 0.88 mu[3] - 0.12 mu[2]
   + 1.01 eta-0-1 <= 1.01
 state-1-choice-0-combination-0: mu[1] - 0.8 mu[3] - 0.2 mu[2]
   + 1.01 eta-1-0 <= 1.01
 state-1-choice-0-combination-1: mu[1] - mu[3] + 1.01 eta-1-0 <= 1.01
 state-2-choice-0-combination-0: 1.01 eta-2-0 <= 1.01
 state-3-choice-0-combination-0: 1.01 eta-3-0 <= 1.01
 actSel-0: eta-0-0 + eta-0-1 >= 1
 actSel-1: eta-1-0 >= 1
 actSel-2: eta-2-0 >= 1
 actSel-3: eta-3-0 >= 1
 multi-bound: mu[0] >= 0.65
Bounds
 mu[0] <= 1
 mu[1] <= 1
 mu[2] <= 1
 mu[3] <= 1
 mu[2] <= 1
 Constant = 1
Binaries
 eta-0-0 eta-0-1 eta-1-0 eta-2-0 eta-3-0
End
