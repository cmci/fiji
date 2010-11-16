
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ij.IJ;

public class SequenceSearch extends AStarSearch<Sequence, SequenceNode> {

	public static final int NumAssignments = 3;

	private Set<Candidate>         startCandidates;
	private Vector<Set<Candidate>> sliceCandidates;

	public SequenceSearch(Set<Candidate> startCandidates, Vector<Set<Candidate>> sliceCandidates) {

		this.startCandidates = startCandidates;
		this.sliceCandidates = sliceCandidates;
	}

	protected Set<SequenceNode> expand(Sequence path) {

		Set<SequenceNode> nodes = new HashSet<SequenceNode>();

		Set<Candidate>    activeCandidates = (path.size() == 0 ? startCandidates : path.getActiveNodes());
		IJ.log("active nodes: " + activeCandidates);
		Set<Candidate>    nextCandidates   = sliceCandidates.get(path.size());

		AssignmentSearch  assignmentSearch = new AssignmentSearch(activeCandidates, nextCandidates);

		double sumProbs = 0.0;

		// enumerate the k most likely assignments
		Assignment bestAssignment = assignmentSearch.findBestPath(new Assignment());
		nodes.add(new SequenceNode(bestAssignment));

		sumProbs += Math.exp(-bestAssignment.getNegLogP());

		for (int i = 0; i < NumAssignments - 1; i++) {

			Assignment nextBestAssignment = assignmentSearch.findNextBestPath();
			nodes.add(new SequenceNode(nextBestAssignment));

			sumProbs += Math.exp(-nextBestAssignment.getNegLogP());
		}

		// estimate normalisation constant Z
		// TODO (for now, let's assume there are no more assignments...
		double normaliser = Math.log(sumProbs);

		// compute probability of each assignment and set the path that led to
		// it
		for (SequenceNode node : nodes) {

			Sequence bestPath = new Sequence();
			bestPath.addAll(path);
			bestPath.push(node);
			node.setBestPath(bestPath);

			// TODO: these are neg log probs!
			node.setNegLogP(node.getAssignment().getNegLogP() + normaliser);
		}

		return nodes;
	}


	protected double g(Sequence path, SequenceNode node) {

		return (path.peek() != null ? path.peek().getDistanceFromStart() : 0.0) + node.getNegLogP();
	}

	protected double h(SequenceNode node) {

		// TODO (for now, explore all nodes)
		return 0.0;
	}

	protected boolean reachedTarget(Sequence path) {

		IJ.log("Current slice: " + path.size());
		return (path.size() == sliceCandidates.size());
	}
}
