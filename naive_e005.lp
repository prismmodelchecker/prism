\ LP format - for model browsing. Use MPS format to capture full model detail.
\ Signature: 0x4dfdf1b6b4a61f7c
Minimize
  0 mu[2] - eta-0-0 - eta-0-1 - eta-1-0 - eta-2-0 - eta-3-0 + 5 Constant
Subject To
 v-target: mu[3] = 1
 v-no: mu[2] = 0
 state-0-choice-0-combination-0: mu[0] - 0.95 mu[1] - 0.05 mu[2]
   + 1.01 eta-0-0 <= 1.01
 state-0-choice-0-combination-1: mu[0] - 0.85 mu[1] - 0.15 mu[2]
   + 1.01 eta-0-0 <= 1.01
 state-0-choice-1-combination-0: mu[0] - 0.73 mu[3] - 0.27 mu[2]
   + 1.01 eta-0-1 <= 1.01
 state-0-choice-1-combination-1: mu[0] - 0.83 mu[3] - 0.17 mu[2]
   + 1.01 eta-0-1 <= 1.01
 state-1-choice-0-combination-0: mu[1] - 0.85 mu[3] - 0.15 mu[2]
   + 1.01 eta-1-0 <= 1.01
 state-1-choice-0-combination-1: mu[1] - 0.95 mu[3] - 0.05 mu[2]
   + 1.01 eta-1-0 <= 1.01
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
