
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import ij.process.ImageProcessor;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.type.numeric.RealType;

public class Sipnet_<T extends RealType<T>> implements PlugIn {

	// the stack to process
	private ImagePlus imp;
	private ImagePlus reg;
	private int numSlices;

	private Image<T> sliceImage;
	private Image<T> sliceRegion;

	private Visualisation visualiser;
	private IO            io;

	private MSER<T>  mser;
	private Sipnet   sipnet;

	private Vector<Set<Region>> sliceCandidates;

	public void run(String args) {

		IJ.log("Starting plugin Sipnet");

		int delta = 10;
		int minArea = 10;
		int maxArea = 100000;
		double maxVariation = 10.0;
		double minDiversity = 0.5;

		// ask for parameters
		GenericDialog gd = new GenericDialog("Settings");
		gd.addNumericField("delta:", delta, 0);
		gd.addNumericField("min area:", minArea, 0);
		gd.addNumericField("max area:", maxArea, 0);
		gd.addNumericField("max variation:", maxVariation, 2);
		gd.addNumericField("min diversity:", minDiversity, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
	
		delta = (int)gd.getNextNumber();
		minArea = (int)gd.getNextNumber();
		maxArea = (int)gd.getNextNumber();
		maxVariation = gd.getNextNumber();
		minDiversity = gd.getNextNumber();

		// read image
		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open an image first.");
			return;
		}

		// setup visualisation and file IO
		visualiser = new Visualisation();
		io         = new IO();

		// setup image stack
		ImageStack stack = imp.getStack();
		numSlices = stack.getSize();
		sliceCandidates = new Vector<Set<Region>>(numSlices);
		sliceCandidates.setSize(numSlices - 1);

		// prepare segmentation image
		reg = imp.createImagePlus();
		ImageStack regStack = new ImageStack(imp.getWidth(), imp.getHeight());
		for (int s = 1; s <= numSlices; s++) {
			ImageProcessor duplProcessor = imp.getStack().getProcessor(s).duplicate();
			regStack.addSlice("", duplProcessor);
		}
		reg.setStack(regStack);
		reg.setDimensions(1, numSlices, 1);
		if (numSlices > 1)
			reg.setOpenAsHyperStack(true);
	
		reg.setTitle("msers of " + imp.getTitle());
	
		// create set of start points
		Set<Region> startCandidates = new HashSet<Region>();

		for (int s = 0; s < numSlices; s++) {

			IJ.log("Processing slice " + s + "...");

			String mserFilename = "./top-msers-" + s + ".sip";

			HashSet<Region> topMsers = null;
			HashSet<Region> msers    = null;
			
			if (io.exists(mserFilename)) {

				IJ.log("Reading Msers from " + mserFilename);
				topMsers = io.readMsers(mserFilename);
				msers    = flatten(topMsers);

			} else {

				// create slice image
				ImagePlus sliceImp = new ImagePlus("slice " + s+1, stack.getProcessor(s+1));
				sliceImage  = ImagePlusAdapter.wrap(sliceImp);
				ImagePlus sliceReg = new ImagePlus("slice " + s+1, regStack.getProcessor(s+1));
				sliceRegion = ImagePlusAdapter.wrap(sliceReg);
				LocalizableByDimCursor<T> regionsCursor = sliceRegion.createLocalizableByDimCursor();
				while (regionsCursor.hasNext()) {
					regionsCursor.fwd();
					regionsCursor.getType().setReal(0.0);
				}
	
				// set up algorithm
				if (mser == null)
					mser = new MSER<T>(sliceImage.getDimensions(), delta, minArea, maxArea, maxVariation, minDiversity);
	
				mser.process(sliceImage, true, false, sliceRegion);

				topMsers = mser.getTopMsers();
				msers    = mser.getMsers();
	
				IJ.log("Found " + topMsers.size() + " parent candidates in slice " + s);
	
				// visualise result
				IJ.run(sliceReg, "Fire", "");
				visualiser.texifyMserTree(mser, sliceReg, "./sipnet-tex/", "slice" + s);
	
				// write msers to file
				io.writeMsers(topMsers, "./top-msers-" + s + ".sip");
			}

			// TODO; pick startCandidates via GUI
			if (s == 0) {
				Vector<Region> vmsers = new Vector<Region>();
				vmsers.addAll(msers);
				// randomly select some start candidates
				for (int i = 0; i < 50; i++) {
					int rand = (int)(Math.random()*vmsers.size());
					startCandidates.add(vmsers.get(rand));
				}
			} else
				sliceCandidates.set(s - 1, msers);
		}

		// perform greedy search
		IJ.log("Searching for the best path greedily");
		sipnet = new Sipnet(1.0, 0.5);
		Sequence greedySeequence = sipnet.greedySearch(startCandidates, sliceCandidates);

		if (greedySeequence == null)
			return;

		// visualize result
		IJ.setForegroundColor(255, 255, 255);
		int slice = 1;
		for (Assignment assignment : greedySeequence) {
			int id = 0;
			for (SingleAssignment singleAssignment : assignment) {

				Region source = singleAssignment.getSource();
				Region target = singleAssignment.getTarget();

				int x = (int)source.getCenter()[0];
				int y = (int)source.getCenter()[1];
				IJ.setSlice(slice);
				IJ.runMacro("drawString(\"" + id + "\", " + x + ", " + y + ")");

				x = (int)target.getCenter()[0];
				y = (int)target.getCenter()[1];
				IJ.setSlice(slice+1);
				IJ.runMacro("drawString(\"" + id + "\", " + x + ", " + y + ")");

				id++;
			}
			slice++;
		}

		imp.updateAndDraw();
	}

	private HashSet<Region> flatten(Collection<Region> parents) {

		HashSet<Region> allRegions = new HashSet<Region>();

		allRegions.addAll(parents);
		for (Region parent : parents)
			allRegions.addAll(flatten(parent.getChildren()));

		return allRegions;
	}
}
