\ LP format - for model browsing. Use MPS format to capture full model detail.
\ Signature: 0x14645777317348bd
Minimize
  - eta-0-0 - eta-0-1 - eta-1-0 - eta-2-0 - eta-3-0 + 5 Constant
Subject To
 v-target: mu[3] = 1
 v-no: mu[2] = 0
 lamU[0-0]: - 1.01 eta-0-0 + lam-0-0 <= 0
 lamD[0-0]: 1.01 eta-0-0 + lam-0-0 >= 0
 uPlusLow-0-0-1: uplus[0-0->1] >= 0
 uMinLow-0-0-1: umin[0-0->1] >= 0
 uPlusHigh-0-0-1: - 1.01 eta-0-0 + uplus[0-0->1] <= 0
 uMinHigh-0-0-1: - 1.01 eta-0-0 + umin[0-0->1] <= 0
 dualFeas_0-0_1: - mu[1] + lam-0-0 - uplus[0-0->1] + umin[0-0->1] <= 0
 uPlusLow-0-0-2: uplus[0-0->2] >= 0
 uMinLow-0-0-2: umin[0-0->2] >= 0
 uPlusHigh-0-0-2: - 1.01 eta-0-0 + uplus[0-0->2] <= 0
 uMinHigh-0-0-2: - 1.01 eta-0-0 + umin[0-0->2] <= 0
 dualFeas_0-0_2: - mu[2] + lam-0-0 - uplus[0-0->2] + umin[0-0->2] <= 0
 lamU[0-1]: - 1.01 eta-0-1 + lam-0-1 <= 0
 lamD[0-1]: 1.01 eta-0-1 + lam-0-1 >= 0
 uPlusLow-0-1-2: uplus[0-1->2] >= 0
 uMinLow-0-1-2: umin[0-1->2] >= 0
 uPlusHigh-0-1-2: - 1.01 eta-0-1 + uplus[0-1->2] <= 0
 uMinHigh-0-1-2: - 1.01 eta-0-1 + umin[0-1->2] <= 0
 dualFeas_0-1_2: - mu[2] + lam-0-1 - uplus[0-1->2] + umin[0-1->2] <= 0
 uPlusLow-0-1-3: uplus[0-1->3] >= 0
 uMinLow-0-1-3: umin[0-1->3] >= 0
 uPlusHigh-0-1-3: - 1.01 eta-0-1 + uplus[0-1->3] <= 0
 uMinHigh-0-1-3: - 1.01 eta-0-1 + umin[0-1->3] <= 0
 dualFeas_0-1_3: - mu[3] + lam-0-1 - uplus[0-1->3] + umin[0-1->3] <= 0
 lamU[1-0]: - 1.01 eta-1-0 + lam-1-0 <= 0
 lamD[1-0]: 1.01 eta-1-0 + lam-1-0 >= 0
 uPlusLow-1-0-2: uplus[1-0->2] >= 0
 uMinLow-1-0-2: umin[1-0->2] >= 0
 uPlusHigh-1-0-2: - 1.01 eta-1-0 + uplus[1-0->2] <= 0
 uMinHigh-1-0-2: - 1.01 eta-1-0 + umin[1-0->2] <= 0
 dualFeas_1-0_2: - mu[2] + lam-1-0 - uplus[1-0->2] + umin[1-0->2] <= 0
 uPlusLow-1-0-3: uplus[1-0->3] >= 0
 uMinLow-1-0-3: umin[1-0->3] >= 0
 uPlusHigh-1-0-3: - 1.01 eta-1-0 + uplus[1-0->3] <= 0
 uMinHigh-1-0-3: - 1.01 eta-1-0 + umin[1-0->3] <= 0
 dualFeas_1-0_3: - mu[3] + lam-1-0 - uplus[1-0->3] + umin[1-0->3] <= 0
 lamU[2-0]: - 1.01 eta-2-0 + lam-2-0 <= 0
 lamD[2-0]: 1.01 eta-2-0 + lam-2-0 >= 0
 uPlusLow-2-0-2: uplus[2-0->2] >= 0
 uMinLow-2-0-2: umin[2-0->2] >= 0
 uPlusHigh-2-0-2: - 1.01 eta-2-0 + uplus[2-0->2] <= 0
 uMinHigh-2-0-2: - 1.01 eta-2-0 + umin[2-0->2] <= 0
 dualFeas_2-0_2: - mu[2] + lam-2-0 - uplus[2-0->2] + umin[2-0->2] <= 0
 lamU[3-0]: - 1.01 eta-3-0 + lam-3-0 <= 0
 lamD[3-0]: 1.01 eta-3-0 + lam-3-0 >= 0
 uPlusLow-3-0-3: uplus[3-0->3] >= 0
 uMinLow-3-0-3: umin[3-0->3] >= 0
 uPlusHigh-3-0-3: - 1.01 eta-3-0 + uplus[3-0->3] <= 0
 uMinHigh-3-0-3: - 1.01 eta-3-0 + umin[3-0->3] <= 0
 dualFeas_3-0_3: - mu[3] + lam-3-0 - uplus[3-0->3] + umin[3-0->3] <= 0
 actSel-0: eta-0-0 + eta-0-1 >= 1
 actSel-1: eta-1-0 >= 1
 actSel-2: eta-2-0 >= 1
 actSel-3: eta-3-0 >= 1
 mc1-0-0: eta-0-0 + phi-0-0 >= 0
 mc2-0-0: - eta-0-0 + phi-0-0 <= 0
 mc3-0-0: - eta-0-0 - lam-0-0 + phi-0-0 >= -1
 mc4-0-0: eta-0-0 - lam-0-0 + phi-0-0 <= 1
 mc1-0-1: eta-0-1 + phi-0-1 >= 0
 mc2-0-1: - eta-0-1 + phi-0-1 <= 0
 mc3-0-1: - eta-0-1 - lam-0-1 + phi-0-1 >= -1
 mc4-0-1: eta-0-1 - lam-0-1 + phi-0-1 <= 1
 mc1-1-0: eta-1-0 + phi-1-0 >= 0
 mc2-1-0: - eta-1-0 + phi-1-0 <= 0
 mc3-1-0: - eta-1-0 - lam-1-0 + phi-1-0 >= -1
 mc4-1-0: eta-1-0 - lam-1-0 + phi-1-0 <= 1
 mc1-2-0: eta-2-0 + phi-2-0 >= 0
 mc2-2-0: - eta-2-0 + phi-2-0 <= 0
 mc3-2-0: - eta-2-0 - lam-2-0 + phi-2-0 >= -1
 mc4-2-0: eta-2-0 - lam-2-0 + phi-2-0 <= 1
 mc1-3-0: eta-3-0 + phi-3-0 >= 0
 mc2-3-0: - eta-3-0 + phi-3-0 <= 0
 mc3-3-0: - eta-3-0 - lam-3-0 + phi-3-0 >= -1
 mc4-3-0: eta-3-0 - lam-3-0 + phi-3-0 <= 1
 bell-0-0: mu[0] + 1.01 eta-0-0 + 0.95 uplus[0-0->1] - 0.85 umin[0-0->1]
   + 0.15 uplus[0-0->2] - 0.05 umin[0-0->2] - phi-0-0 <= 1.01
 bell-0-1: mu[0] + 1.01 eta-0-1 + 0.27 uplus[0-1->2] - 0.17 umin[0-1->2]
   + 0.83 uplus[0-1->3] - 0.73 umin[0-1->3] - phi-0-1 <= 1.01
 bell-1-0: mu[1] + 1.01 eta-1-0 + 0.15 uplus[1-0->2] - 0.05 umin[1-0->2]
   + 0.95 uplus[1-0->3] - 0.85 umin[1-0->3] - phi-1-0 <= 1.01
 bell-2-0: mu[2] + 1.01 eta-2-0 + uplus[2-0->2] - umin[2-0->2] - phi-2-0
   <= 1.01
 bell-3-0: mu[3] + 1.01 eta-3-0 + uplus[3-0->3] - umin[3-0->3] - phi-3-0
   <= 1.01
 multi-bound: mu[0] >= 0.65
Bounds
 mu[0] <= 1
 mu[1] <= 1
 mu[2] <= 1
 mu[3] <= 1
 -1 <= lam-0-0 <= 1
 uplus[0-0->1] <= 1.01
 umin[0-0->1] <= 1.01
 uplus[0-0->2] <= 1.01
 umin[0-0->2] <= 1.01
 -1 <= lam-0-1 <= 1
 uplus[0-1->2] <= 1.01
 umin[0-1->2] <= 1.01
 uplus[0-1->3] <= 1.01
 umin[0-1->3] <= 1.01
 -1 <= lam-1-0 <= 1
 uplus[1-0->2] <= 1.01
 umin[1-0->2] <= 1.01
 uplus[1-0->3] <= 1.01
 umin[1-0->3] <= 1.01
 -1 <= lam-2-0 <= 1
 uplus[2-0->2] <= 1.01
 umin[2-0->2] <= 1.01
 -1 <= lam-3-0 <= 1
 uplus[3-0->3] <= 1.01
 umin[3-0->3] <= 1.01
 -1 <= phi-0-0 <= 1
 -1 <= phi-0-1 <= 1
 -1 <= phi-1-0 <= 1
 -1 <= phi-2-0 <= 1
 -1 <= phi-3-0 <= 1
 Constant = 1
Binaries
 eta-0-0 eta-0-1 eta-1-0 eta-2-0 eta-3-0
End
