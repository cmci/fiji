
import Jama.Matrix;

public class AssignmentModel {

	static double covaPosition         = 10.0;
	static double covaSize             = 1000.0;
	static double covaNeighborPosition = 5.0;

	static double[][] covaApp =
	    {{covaPosition, 0.0, 0.0},
	     {0.0, covaPosition, 0.0},
	     {0.0, 0.0, covaSize}};
	static double[][] covaNeighOff =
	    {{covaNeighborPosition, 0.0},
	     {0.0, covaNeighborPosition}};

	static Matrix covaAppearance             = new Matrix(covaApp);
	static Matrix invCovaAppearance          = covaAppearance.inverse();
	static double normAppearance             = 1.0/(Math.sqrt(covaAppearance.times(2.0*Math.PI).det()));
	static double negLogNormAppearance       = -Math.log(normAppearance);

	static Matrix covaNeighborOffset         = new Matrix(covaNeighOff);
	static Matrix invCovaNeighborOffset      = covaNeighborOffset.inverse();
	static double normNeighborOffset         = 1.0/(Math.sqrt(covaNeighborOffset.times(2.0*Math.PI).det()));
	static double negLogNormNeighborOffset   = -Math.log(normNeighborOffset);

	static final double negLogPAppearance(SingleAssignment assignment) {

		return negLogPAppearance(assignment.getSource(), assignment.getTarget());
	}

	static final double negLogPAppearance(Candidate source, Candidate target) {

		Matrix diff = new Matrix(3, 1);

		diff.set(0, 0, target.getCenter()[0] - source.getCenter()[0]);
		diff.set(1, 0, target.getCenter()[1] - source.getCenter()[1]);
		diff.set(2, 0, target.getSize()      - source.getSize());

		return negLogNormAppearance + 0.5*(diff.transpose().times(invCovaAppearance).times(diff)).get(0, 0);
	}

	static final double negLogPNeighbor(double[] originalOffset, double[] offset) {

		Matrix diff = new Matrix(2, 1);
		
		diff.set(0, 0, originalOffset[0] - offset[0]);
		diff.set(1, 0, originalOffset[1] - offset[1]);

		return negLogNormNeighborOffset + 0.5*(diff.transpose().times(invCovaNeighborOffset).times(diff)).get(0, 0);
	}
}
