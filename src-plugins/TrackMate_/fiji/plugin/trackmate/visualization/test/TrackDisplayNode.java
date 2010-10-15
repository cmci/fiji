package fiji.plugin.trackmate.visualization.test;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import ij3d.ContentNode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.media.j3d.Switch;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Tuple3d;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import customnode.CustomTriangleMesh;
import customnode.MeshMaker;

public class TrackDisplayNode extends ContentNode {

	private static final int DEFAULT_PARALLEL_NUMBER = 12;

	/** The graph containing the connectivity. */
	protected SimpleGraph<Spot, DefaultEdge> graph;
	/** The spots indexed by frame. */
	protected TreeMap<Integer, List<Spot>> spots;
	/** The list of tracks. */
	protected List<Set<Spot>> tracks;
	/** Hold the color and transparency of all spots for a given track. */
	protected Map<Set<Spot>, Color4f> colors;
	/** The track tube radius. */
	protected double radius;
	
	/** Switch used for display. Is the only child of this {@link ContentNode}.	 */
	protected Switch trackSwitch;
	/** Boolean set that controls the visibility of each mesh.	 */
	protected BitSet switchMask;
	
	/** Hold an index of the bit masks for each track. */
	protected HashMap<Set<Spot>, Collection<Integer>> trackIndices;
	/** Hold an index of the bit masks for each frame. */
	protected HashMap<Integer, Collection<Integer>> frameIndices;
	/** Hold a index of all the meshes created, indexed as the other maps. */
	protected HashMap<Integer, CustomTriangleMesh> meshes;
	
	
	 
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackDisplayNode(
			SimpleGraph<Spot, DefaultEdge> graph, 
			TreeMap<Integer, List<Spot>> spots, 
			List<Set<Spot>> tracks, 
			Map<Set<Spot>, Color4f> colors, 
			double radius) {
		this.graph = graph;
		this.spots = spots;
		this.tracks = tracks;
		this.colors = colors;
		this.radius = radius;
		this.trackSwitch = new Switch(Switch.CHILD_MASK);
		trackSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		makeMeshes();
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void makeMeshes() {
		
		// Loop over tracks
		Set<DefaultEdge> allEdges;
		Spot target;
		CustomTriangleMesh mesh;
		
		trackIndices 	= new HashMap<Set<Spot>, Collection<Integer>>(tracks.size());
		for(Set<Spot> track : tracks)  
			trackIndices.put(track, new ArrayList<Integer>());
		
		frameIndices = new HashMap<Integer, Collection<Integer>>(spots.size());
		for(int frame : spots.keySet()) 
			frameIndices.put(frame, new ArrayList<Integer>());
		
		
		int index = 0;
		int frame;
		
		for (Set<Spot> track : tracks) {
			
			for (Spot source : track) {

				// Find the frame to which it belongs
				frame = -1;
				for(int key : spots.keySet())
					if (spots.get(key).contains(source)) {
						frame = key;
						break;
					}
				
				// Create a tube from this spot to its targets
				allEdges = graph.outgoingEdgesOf(source);
				for(DefaultEdge edge : allEdges) {
					target = graph.getEdgeTarget(edge);
					mesh = makeMesh(source, target, colors.get(track));
					// Add the tube to the content
					trackSwitch.addChild(mesh);
					// Store indices
					trackIndices.get(track).add(index);
					frameIndices.get(frame).add(index);
					meshes.put(index, mesh);
					// Next!
					index++;
					
				}
			}
			
		}
		
		switchMask = new BitSet(index);
		switchMask.set(0, index-1, true); // all visible
		trackSwitch.setChildMask(switchMask);
		removeAllChildren();
		addChild(trackSwitch);
	}
	
	
	@SuppressWarnings("unchecked")
	private CustomTriangleMesh makeMesh(final Spot source, final Spot target, final Color4f color) {
		double[] x = new double[] { source.getFeature(Feature.POSITION_X), target.getFeature(Feature.POSITION_X) };
		double[] y = new double[] { source.getFeature(Feature.POSITION_Y), target.getFeature(Feature.POSITION_Y) };
		double[] z = new double[] { source.getFeature(Feature.POSITION_Z), target.getFeature(Feature.POSITION_Z) };
		double[] r = new double[] { radius, radius };
		List points = MeshMaker.createTube(x, y, z, r, DEFAULT_PARALLEL_NUMBER, false);
		CustomTriangleMesh node = new CustomTriangleMesh(points, new Color3f(color.x, color.y, color.z), color.w);
		return node;
	}
	
	
	/*
	 * CONTENTNODE METHODS
	 */
	
	
	@Override
	public void channelsUpdated(boolean[] channels) {}

	@Override
	public void colorUpdated(Color3f color) {
		for(CustomTriangleMesh mesh : meshes.values())
			mesh.setColor(color);
	}

	@Override
	public void eyePtChanged(View view) {}

	@Override
	public void getCenter(Tuple3d center) {
		double x = 0, y = 0, z = 0;
		for (Spot spot : graph.vertexSet()) {
			x += spot.getFeature(Feature.POSITION_X);
			y += spot.getFeature(Feature.POSITION_Y);
			z += spot.getFeature(Feature.POSITION_Z);
		}
		int nspot = graph.vertexSet().size();
		x /= nspot;
		y /= nspot;
		z /= nspot;
	}

	@Override
	public void getMax(Tuple3d max) {
		double xmax = Float.NEGATIVE_INFINITY;
		double ymax = Float.NEGATIVE_INFINITY;
		double zmax = Float.NEGATIVE_INFINITY;
		for (Spot spot : graph.vertexSet()) {
			if (xmax < spot.getFeature(Feature.POSITION_X) + radius)
				xmax = spot.getFeature(Feature.POSITION_X) + radius;
			if (ymax < spot.getFeature(Feature.POSITION_Y) + radius)
				ymax = spot.getFeature(Feature.POSITION_Y) + radius;
			if (zmax < spot.getFeature(Feature.POSITION_Z) + radius)
				zmax = spot.getFeature(Feature.POSITION_Z) + radius;
		}
		max.x = xmax;
		max.y = ymax;
		max.z = zmax;

	}

	@Override
	public void getMin(Tuple3d min) {
		double xmin = Float.POSITIVE_INFINITY;
		double ymin = Float.POSITIVE_INFINITY;
		double zmin = Float.POSITIVE_INFINITY;
		for (Spot spot : graph.vertexSet()) {
			if (xmin > spot.getFeature(Feature.POSITION_X) - radius)
				xmin = spot.getFeature(Feature.POSITION_X) - radius;
			if (ymin > spot.getFeature(Feature.POSITION_Y) - radius)
				ymin = spot.getFeature(Feature.POSITION_Y) - radius;
			if (zmin > spot.getFeature(Feature.POSITION_Z) - radius)
				zmin = spot.getFeature(Feature.POSITION_Z) - radius;
		}
		min.x = xmin;
		min.y = ymin;
		min.z = zmin;
	}

	@Override
	public float getVolume() {
		float volume = 0;
		for (CustomTriangleMesh mesh : meshes.values()) 
			volume += mesh.getVolume();
		return volume;
	}

	@Override
	public void shadeUpdated(boolean shaded) {
		for (CustomTriangleMesh mesh : meshes.values())
			 mesh.setShaded(shaded);
	}

	@Override
	public void thresholdUpdated(int threshold) {}

	@Override
	public void transparencyUpdated(float transparency) {
		for(CustomTriangleMesh mesh : meshes.values()) 
			mesh.setTransparency(transparency);
	}

}
