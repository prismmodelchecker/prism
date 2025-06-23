package explicit;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBQuadExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRBConstr;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import com.gurobi.gurobi.GRBCallback;


public class trigger extends GRBCallback {
    private double lastImproveTime;
    private double timeout;
    private GRBModel model;
    private double bestObj = Double.POSITIVE_INFINITY;

    public trigger(GRBModel model, double timeoutSeconds) {
        this.model = model;
        this.timeout = timeoutSeconds;
        // initialize lastImproveTime to zero so first incumbent counts as improvement
        this.lastImproveTime = 0.0;
    }

    @Override
    protected void callback() {
        try {
            // 1) Whenever a new integer solution is found, record it:
            if (where == GRB.CB_MIPSOL) {
                double runtime = getDoubleInfo(GRB.Callback.RUNTIME);
                double obj     = getDoubleInfo(GRB.Callback.MIPSOL_OBJ);
                // For minimization, lower is better
                if (obj < bestObj) {
                    bestObj = obj;
                    lastImproveTime = runtime;
                    System.out.println("New incumbent: " + obj + " at t=" + runtime);
                }
            }

            // 2) At nodes / periodically, check for stall
            if (where == GRB.CB_MIPNODE || where == GRB.CB_MIP) {
                double runtime = getDoubleInfo(GRB.Callback.RUNTIME);
                // if we've seen at least one solution, and it's been > timeout s
                if (lastImproveTime > 0 && runtime - lastImproveTime > timeout) {
                    System.out.printf(
                        "No improvement for %.1f seconds (last at %.1f)—aborting%n",
                        timeout, lastImproveTime
                    );
                    abort();  // stop the solve and keep the current incumbent
                }
            }
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }
}