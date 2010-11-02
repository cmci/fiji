
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.Collection;

import ij.ImagePlus;

import ij.io.FileSaver;

import mpicbg.imglib.type.numeric.RealType;

public class Visualiser {

	final double texWidth = 13.0;

	private ImagePlus regionImp;
	private String    outputDir;

	public Visualiser(ImagePlus regionImp, String outputDir) {

		this.regionImp = regionImp;
		this.outputDir = outputDir;
	}

	public <T extends RealType<T>> void texifyMserTree(MSER<T> mser, int slice) {

		ImagePlus imp = new ImagePlus("", regionImp.getStack().getProcessor(slice + 1));
		FileSaver saver = new FileSaver(imp);
		saver.saveAsPng(outputDir + "/mser-tree" + slice + ".png");

		int width    = imp.getWidth();
		int height   = imp.getHeight();
		double scale = texWidth/width;

		File texFile = new File(outputDir + "/mser-tree" + slice + ".tikz.tex");
		try {
			FileWriter out = new FileWriter(texFile);
			out.write("\\begin{tikzpicture}\n");
			out.write("\\def\\imagewidth{" + texWidth + "cm}\n");
			out.write("\\node (image) {\\includegraphics[width=\\imagewidth]{mser-tree" + slice + ".png}};\n");
			texifyRegions(mser.getTopMsers(), scale, width, height, -1, out);
			out.write("\\end{tikzpicture}\n");
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void texifyClosestCandidates(Collection<Region> regions, int slice) {

		ImagePlus imp = new ImagePlus("", regionImp.getStack().getProcessor(slice + 1));
		FileSaver saver = new FileSaver(imp);
		saver.saveAsPng(outputDir + "/mser-closest-regions" + slice + ".png");

		int width    = imp.getWidth();
		int height   = imp.getHeight();
		double scale = texWidth/width;

		File texFile = new File(outputDir + "/mser-closest-regions" + slice + ".tikz.tex");
		try {
			FileWriter out = new FileWriter(texFile);
			out.write("\\begin{tikzpicture}\n");
			out.write("\\def\\imagewidth{" + texWidth + "cm}\n");
			out.write("\\node (image) {\\includegraphics[width=\\imagewidth]{mser-closest-regions" + slice + ".png}};\n");
			texifyClosestCandidates(regions, scale, width, height, out);
			out.write("\\end{tikzpicture}\n");
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void texifyClosestCandidates(Collection<Region> msers, double scale, int width, int height, Writer out) throws IOException {

		for (Region region : msers) {

			texifyRegion(region, Math.sqrt(region.getSize()), scale, width, height, out);

			for (Region closest : region.getClosestRegions()) {

				texifyRegion(closest, Math.sqrt(closest.getSize()), scale, width, height, out);
				out.write("\\draw[->, green!50!red] (node" + region.getId() + ") -- (node" + closest.getId() + ");\n");
			}
		}
	}

	private void texifyRegions(Collection<Region> msers, double scale, int width, int height, int parentId, Writer out) throws IOException {

		for (Region mser: msers) {
			int newParentId = texifyChild(mser, Math.sqrt(mser.getSize()), scale, width, height, parentId, out);
			texifyRegions(mser.getChildren(), scale, width, height, newParentId, out);
		}
	}

	private int texifyChild(Region mser, double radius, double scale, int width, int height, int parentId, Writer out) throws IOException {

		texifyRegion(mser, radius, scale, width, height, out);

		if (parentId >= 0)
			out.write("\\draw[->, green!50!red] (node" + mser.getId() + ") -- (node" + parentId + ");\n");

		return mser.getId();
	}

	private void texifyRegion(Region mser, double radius, double scale, int width, int height, Writer out) throws IOException {

		int[] center = new int[mser.getCenter().length];
		for (int d = 0; d < mser.getCenter().length; d++)
			center[d] = (int)mser.getCenter()[d];

		// draw a circle
		double offsetX = (center[0] - (double)width/2.0)*scale;
		double offsetY = (center[1] - (double)height/2.0)*scale;

		out.write("\\node[circle, fill=green, inner sep=" + radius*scale +
		          ", opacity=0.5] (node" + mser.getId() +
		          ") at ($(image) + (" + offsetX + ", " + -offsetY + ")$) {};\n");
	}
}
