
import java.util.HashMap;

import ij.IJ;
import ij.ImagePlus;

import ij.plugin.Duplicator;

public class Visualiser {

	public void drawSequence(ImagePlus blockImage, Sequence sequence, boolean drawConfidence) {

		// visualize result
		ImagePlus blockCopy = (new Duplicator()).run(blockImage);
		blockCopy.show();

		IJ.setForegroundColor(255, 255, 255);
		IJ.selectWindow(blockCopy.getTitle());

		int slice = sequence.size();

		HashMap<Candidate, Integer>  ids             = new HashMap<Candidate, Integer>();
		HashMap<Candidate, double[]> previousCenters = new HashMap<Candidate, double[]>();

		for (SequenceNode node: sequence) {

			Assignment assignment = node.getAssignment();

			if (slice == sequence.size()) {

				int id = 1;
				for (SingleAssignment singleAssignment : assignment) {

					Candidate target = singleAssignment.getTarget();

					ids.put(target, id);
					previousCenters.put(target, target.getCenter());

					int x = (int)target.getCenter()[0];
					int y = (int)target.getCenter()[1];

					drawCandidate(x, y, slice + 1, id);
					id++;
				}
			}

			for (SingleAssignment singleAssignment : assignment) {

				Candidate source = singleAssignment.getSource();
				Candidate target = singleAssignment.getTarget();

				int id = ids.get(target);
				double[] previousCenter = previousCenters.get(target);
				ids.put(source, id);
				previousCenters.put(source, source.getCenter());
				int px = (int)previousCenter[0];
				int py = (int)previousCenter[1];
				int x  = (int)source.getCenter()[0];
				int y  = (int)source.getCenter()[1];
				double confidence = singleAssignment.getNegLogP();

				drawCandidate(x, y, slice, id);
				drawConnection(px, py, x, y, slice, confidence);
			}
			slice--;
		}

		blockCopy.updateAndDraw();
	}

	private void drawCandidate(int x, int y, int slice, int id) {

		String annotation = "" + id;
		IJ.setSlice(slice);
		IJ.runMacro("drawString(\"" + annotation + "\", " + x + ", " + y + ")");
	}

	private void drawConnection(int x1, int y1, int x2, int y2, int slice, double confidence) {

		IJ.setSlice(slice);
		IJ.makeLine(x1, y1, x2, y2);
		IJ.run("Draw", "slice");
	}

}
