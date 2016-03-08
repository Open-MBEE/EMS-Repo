package gov.nasa.jpl.view_repo.db;

public interface DbContract {
    // localhost sometimes is mapped to, so the SQL driver needs 127.0.0.1 in HOST
	public static final String HOST = "jdbc:postgresql://127.0.0.1/";
	public static final String DB_NAME = "mms";
	public static final String USERNAME = "postgres";
	public static final String PASSWORD = "test123";
}
