package gov.nasa.jpl.view_repo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresHelper {

	private Connection conn;
	private String host;
	private String dbName;
	private String user;
	private String pass;
	private String workspaceName;

	public static enum DbEdgeTypes {
		REGULAR(1), DOCUMENT(2);

		private final int id;

		DbEdgeTypes(int id) {
			this.id = id;
		}

		public int getValue() {
			return id;
		}
	}

	public PostgresHelper(String workspaceName) {
		this.host = DbContract.HOST;
		this.dbName = DbContract.DB_NAME;
		this.user = DbContract.USERNAME;
		this.pass = DbContract.PASSWORD;
		this.workspaceName = workspaceName.replace("-", "_");
	}

	public void close() throws SQLException {
		conn.close();
	}

	public boolean connect() throws SQLException, ClassNotFoundException {
		if (host.isEmpty() || dbName.isEmpty() || user.isEmpty()
				|| pass.isEmpty()) {
			throw new SQLException("Database credentials missing");
		}

		Class.forName("org.postgresql.Driver");
		this.conn = DriverManager.getConnection(this.host + this.dbName,
				this.user, this.pass);
		return true;
	}

	public void execUpdate(String query) throws SQLException {
		System.out.println("Query: " + query);
		this.conn.createStatement().executeUpdate(query);
	}

	public ResultSet execQuery(String query) throws SQLException {
		System.out.println("Query: " + query);
		return this.conn.createStatement().executeQuery(query);
	}

	public int insert(String table, Map<String, String> values)
			throws SQLException {

		StringBuilder columns = new StringBuilder();
		StringBuilder vals = new StringBuilder();

		try {
			for (String col : values.keySet()) {
				columns.append(col).append(",");

				if (values.get(col) instanceof String) {
					vals.append("'").append(values.get(col)).append("',");
				} else
					vals.append(values.get(col)).append(",");
			}

			columns.setLength(columns.length() - 1);
			vals.setLength(vals.length() - 1);

			String query = String.format("INSERT INTO %s (%s) VALUES (%s)",
					table, columns.toString(), vals.toString());

			return this.conn.createStatement().executeUpdate(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public List<EdgeTypes> getEdgeTypes() {
		List<EdgeTypes> result = new ArrayList<EdgeTypes>();
		try {
			ResultSet rs = execQuery("SELECT * FROM edgeTypes");

			while (rs.next()) {
				result.add(new EdgeTypes(rs.getInt(1), rs.getString(2)));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public Node getNodeFromNodeRefId(String nodeRefId) {
		try {
			ResultSet rs = execQuery("SELECT * FROM nodes" + workspaceName
					+ " where nodeRefId = '" + nodeRefId + "'");

			if (rs.first()) {
				return new Node(rs.getInt(1), rs.getString(2), rs.getString(3),
						rs.getInt(4), rs.getString(5));
			} else
				return null;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Node getNode(int id) {
		try {
			ResultSet rs = execQuery("SELECT * FROM nodes " + workspaceName
					+ " where id = " + id);
			if (rs.next()) {
				return new Node(rs.getInt(1), rs.getString(2), rs.getString(3),
						rs.getInt(4), rs.getString(5));
			} else
				return null;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Node getNodeFromSysmlId(String sysmlId) {
		try {
			ResultSet rs = execQuery("SELECT * FROM nodes" + workspaceName
					+ " where sysmlid = '" + sysmlId + "'");

			if (rs.next()) {
				return new Node(rs.getInt(1), rs.getString(2), rs.getString(3),
						rs.getInt(4), rs.getString(5));
			} else
				return null;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void insertNode(String nodeRefId, String versionedRefId,
			String sysmlId) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("nodeRefId", nodeRefId);
			map.put("versionedRefId", versionedRefId);
			map.put("sysmlId", sysmlId);
			map.put("nodeType", "1");
			insert("nodes" + workspaceName, map);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateNodeVersionedRefId(String sysmlId, String versionedRefId) {
		if(sysmlId == null || versionedRefId == null) return;
		try {
			execUpdate("update nodes" + workspaceName
					+ " set versionedRefId = '" + versionedRefId
					+ "' where sysmlid='" + sysmlId + "'");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteNode(String sysmlId) {
		try {
			execUpdate("delete from nodes" + workspaceName
					+ " where sysmlid = '" + sysmlId + "'");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteNodeBySysmlId(String sysmlId) {
		try {
			execUpdate("delete from nodes" + workspaceName
					+ " where sysmlId = " + sysmlId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertEdge(String parentSysmlId, String childSysmlId,
			DbEdgeTypes edgeType) {

		if (parentSysmlId.isEmpty() || childSysmlId.isEmpty())
			return;
		try {
			execUpdate("insert into edges" + workspaceName
					+ " values((select id from nodes" + workspaceName
					+ " where sysmlId = '" + parentSysmlId + "'),"
					+ "(select id from nodes" + workspaceName
					+ " where sysmlId = '" + childSysmlId + "'), "
					+ DbEdgeTypes.REGULAR.getValue() + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// returns list of nodeRefIds
	public List<String> getChildrenNodeRefIds(String sysmlId, DbEdgeTypes et) {
		List<String> result = new ArrayList<String>();
		try {
			Node n = getNodeFromSysmlId(sysmlId);

			if (n == null)
				return result;

			ResultSet rs = execQuery("select nodeRefId from nodes"
					+ workspaceName
					+ " where id in (select * from get_children(" + n.getId()
					+ ", " + et.getValue() + ", '" + workspaceName + "'))");

			while (rs.next()) {
				result.add(rs.getString(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<String> getImmediateChildren(String sysmlId,
			DbEdgeTypes edgeType) {
		List<String> result = new ArrayList<String>();
		try {
			Node n = getNodeFromSysmlId(sysmlId);
			if (n == null)
				return result;

			ResultSet rs = execQuery("select noderefid from nodes"
					+ workspaceName
					+ " where id in (select child from edges where parent = "
					+ n.getId() + " and edgeType = " + edgeType.getValue()
					+ ")");

			while (rs.next()) {
				result.add(rs.getString(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<Node> getChildren(String nodeRefId, int edgeType) {
		List<Node> result = new ArrayList<Node>();
		try {
			Node n = getNodeFromNodeRefId(nodeRefId);
			if (n == null)
				return result;

			ResultSet rs = execQuery("select * from get_children(" + n.getId()
					+ ", " + edgeType + ", " + workspaceName + ")");

			while (rs.next()) {
				result.add(getNode(rs.getInt(1)));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public void deleteEdgesForNode(String sysmlId) {
		try {
			Node n = getNodeFromSysmlId(sysmlId);

			if (n == null)
				return;

			execUpdate("delete from edges" + workspaceName + " where parent = "
					+ n.getId() + " or child = " + n.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteEdgesForChildNode(String sysmlId, DbEdgeTypes edgeType) {
		try {
			Node n = getNodeFromSysmlId(sysmlId);

			if (n == null)
				return;

			execUpdate("delete from edges" + workspaceName + " where child = "
					+ n.getId() + " and edgeType = " + edgeType.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteEdges(String parentSysmlId, String childSysmlId) {
		try {
			Node pn = getNodeFromSysmlId(parentSysmlId);
			Node cn = getNodeFromSysmlId(childSysmlId);

			if (pn == null || cn == null)
				return;

			execUpdate("delete from edges where parent = " + pn.getId()
					+ " and child = " + cn.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteEdges(String parentSysmlId, String childSysmlId,
			int edgeType) {
		try {
			Node pn = getNodeFromSysmlId(parentSysmlId);
			Node cn = getNodeFromSysmlId(childSysmlId);

			if (pn == null || cn == null)
				return;

			execUpdate("delete from edges" + workspaceName + " where parent = "
					+ pn.getId() + " and child = " + cn.getId()
					+ " and edgeType = " + edgeType);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createBranchFromWorkspace(String childWorkspaceName) {
		try {
			execUpdate("create table nodes" + childWorkspaceName.replace("-", "_")
					+ " as select * from nodes" + workspaceName);

			execUpdate("create table edges" + childWorkspaceName.replace("-", "_")
					+ " as select * from edges" + workspaceName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
