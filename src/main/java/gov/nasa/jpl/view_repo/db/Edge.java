package gov.nasa.jpl.view_repo.db;

public class Edge {

	private int parent;
	private int child;
	private int edgeType;
	
	public Edge(int parent, int child, int edgeType) {
		super();
		this.parent = parent;
		this.child = child;
		this.edgeType = edgeType;
	}
	public int getParent() {
		return parent;
	}
	public void setParent(int parent) {
		this.parent = parent;
	}
	public int getChild() {
		return child;
	}
	public void setChild(int child) {
		this.child = child;
	}
	public int getEdgeType() {
		return edgeType;
	}
	public void setEdgeType(int edgeType) {
		this.edgeType = edgeType;
	}
	
	
	
}
