package gov.nasa.jpl.view_repo.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class DoorsPostgresHelper {

    private PostgresHelper dpgh = null;

    public DoorsPostgresHelper() {

        dpgh = new PostgresHelper( "null" );

    }

    public HashMap< String, HashMap< String, ArrayList< String > > >
           getArtifactMappings() {

        HashMap< String, HashMap< String, ArrayList< String > > > artifactConfiguration =
                new HashMap< String, HashMap< String, ArrayList< String > > >();

        try {

            dpgh.connect();

            ResultSet rs =
                    dpgh.execQuery( "SELECT * FROM doorsartifactmappings" );

            while ( rs.next() ) {

                String project = rs.getString( 1 );

                String artifacttype = rs.getString( 2 );

                String appliedMetatype = rs.getString( 3 );

                if ( !artifactConfiguration.keySet().contains( project ) ) {

                    artifactConfiguration.put( project,
                                               new HashMap< String, ArrayList< String > >() );

                    artifactConfiguration.get( project )
                                         .put( artifacttype,
                                               new ArrayList< String >() );

                    artifactConfiguration.get( project ).get( artifacttype )
                                         .add( appliedMetatype );

                }

                // unique project has already been added , now we want to add
                // the current applied metatype to the project
                else {

                    if ( !artifactConfiguration.get( project ).keySet()
                                               .contains( artifacttype ) ) {

                        artifactConfiguration.get( project )
                                             .put( artifacttype,
                                                   new ArrayList< String >() );

                        artifactConfiguration.get( project ).get( artifacttype )
                                             .add( appliedMetatype );

                    }

                    else {

                        artifactConfiguration.get( project ).get( artifacttype )
                                             .add( appliedMetatype );

                    }

                }

            }

        } catch ( SQLException e ) {
            e.printStackTrace();
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            dpgh.close();
        }

        return artifactConfiguration;

    }

}
