package gov.nasa.jpl.view_repo.db;

public class Node {

	private int id;
	private String nodeRefId;
	private String versionedRefId;
	private int nodeType;
	private String sysmlId;
	
	
	public String getSysmlId() {
		return sysmlId;
	}
	public void setSysmlId(String sysmlId) {
		this.sysmlId = sysmlId;
	}
	public Node(int id, String nodeRefId, String versionedRefId, int nodeType, String sysmlId) {
		super();
		this.id = id;
		this.nodeRefId = nodeRefId;
		this.versionedRefId = versionedRefId;
		this.nodeType = nodeType;
		this.sysmlId = sysmlId;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNodeRefId() {
		return nodeRefId;
	}
	public void setNodeRefId(String nodeRefId) {
		this.nodeRefId = nodeRefId;
	}
	public String getVersionedRefId() {
		return versionedRefId;
	}
	public void setVersionedRefId(String versionedRefId) {
		this.versionedRefId = versionedRefId;
	}
	public int getNodeType() {
		return nodeType;
	}
	public void setNodeType(int nodeType) {
		this.nodeType = nodeType;
	}
	
}
