package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.schemabuilder.DropKeyspace;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import eu.modernmt.persistence.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by andrearossi on 08/03/17.
 * A CassandraDatabase object represents the access point
 * to a Cassandra DB instance.
 * It offers methods for establishing connections with the DB
 * and to get DAO objects for the entities it stores.
 */
public class CassandraDatabase extends Database {
    public static final String DOMAINS_TABLE = "domains";
    public static final String IMPORT_JOBS_TABLE = "import_jobs";
    public static final String COUNTERS_TABLE = "table_counters";

    public static final int DOMAINS_TABLE_ID = 1;
    public static final int IMPORT_JOBS_TABLE_ID = 2;
    public static final int[] TABLE_IDS = {DOMAINS_TABLE_ID, IMPORT_JOBS_TABLE_ID};

    private String keyspace;
    private Cluster cluster;

    /**
     * This method figures the suitable default keyspace name
     * for the current engine, given its name and configuration
     *
     * @param engineName the name of the current translation engine
     * @param sourceLang the language from which to translate
     * @param targetLang the languate to which to translate
     */

    public static String getDefaultKeyspace(String engineName, Locale sourceLang, Locale targetLang) {
        return "mmt_" + engineName + "_" + sourceLang.getDisplayName() + "_" + targetLang.getDisplayName();
    }

    /**
     * This constructor builds an access point to a Cassandra DB
     * and, in particular, to one of its keyspaces
     *
     * @param host     the hostname of the machine that is running Cassandra
     * @param port     the port on which the Cassandra machine is listening to
     * @param keyspace the keyspace in which the target entities are stored in the Cassandra DB
     */
    public CassandraDatabase(String host, int port, String keyspace) {
        this.keyspace = keyspace;
        this.cluster = Cluster.builder().withPort(port).addContactPoint(host).build();
    }

    /**
     * This method provides a connection to a Cassandra DB
     *
     * @param cached
     * @return A CassandraConnection object, that
     * can be used to establish a communication Session with the DB
     * @throws PersistenceException
     */
    @Override
    public Connection getConnection(boolean cached) throws PersistenceException {
        return new CassandraConnection(this.cluster, this.keyspace);
    }

    /**
     * This method creates and returns a DAO for Domain objects
     *
     * @param connection a currently active connection to the DB
     * @return A DomainDao that can perform CRUD operations for Domain objects
     * @throws PersistenceException
     */
    @Override
    public DomainDAO getDomainDAO(Connection connection) {
        return new CassandraDomainDAO((CassandraConnection) connection);
    }

    /**
     * This method creates and returns a DAO for ImportJob objects
     *
     * @param connection a currently active connection to the DB
     * @return An ImportJobDao that can perform CRUD operations for ImportJob objects
     * @throws PersistenceException
     */
    @Override
    public ImportJobDAO getImportJobDAO(Connection connection) {
        return new CassandraImportJobDAO((CassandraConnection) connection);
    }

    /**
     * This method establishes a new connection to the DB
     * and drops the current keyspace (with all its tables).
     * If the keyspace has already been dropped, the method does nothing.
     *
     * @throws PersistenceException
     */
    public void drop() throws PersistenceException {
        CassandraConnection connection = null;

        try {
            connection = new CassandraConnection(this.cluster, null);
            Session session = connection.session;

            // DropKeyspace dropKeyspace = SchemaBuilder.dropKeyspace("\"" + this.keyspace + "\"").ifExists();
            DropKeyspace dropKeyspace = SchemaBuilder.dropKeyspace(this.keyspace).ifExists();
            CassandraUtils.checkedExecute(session, dropKeyspace);

        } catch (KeyspaceNotFoundException e) {
            /*ignore*/
        } finally {
            IOUtils.closeQuietly(connection);
        }

    }

    /**
     * This method establishes a new connection to the DB
     * and uses it to create
     * - a new keyspace
     * - a new domains table
     * - a new importjobs table
     * - a new table_counters table,
     * with an entry for domain and another one for importjobs
     *
     * @throws PersistenceException
     */
    @Override
    public void create() throws PersistenceException {
        CassandraConnection connection = null;

        try {
            connection = new CassandraConnection(this.cluster, null);
            Session session = connection.session;


            String createKeyspace =
                    "CREATE KEYSPACE " + this.keyspace + " WITH replication = " +
                            "{'class':'SimpleStrategy', 'replication_factor':1};";


            String useKeySpace = "USE " + this.keyspace + ";";

            String createCountersTable =
                    "CREATE TABLE IF NOT EXISTS " + this.keyspace + "." + COUNTERS_TABLE +
                            " (table_id int PRIMARY KEY, table_counter bigint );";

            String createDomainsTable =
                    "CREATE TABLE " + this.keyspace + "." + DOMAINS_TABLE +
                            " (id int PRIMARY KEY, name varchar);";

            String createImportJobsTable =
                    "CREATE TABLE " + this.keyspace + "." + IMPORT_JOBS_TABLE +
                            " (id bigint PRIMARY KEY, domain int, size int, \"begin\" bigint, end bigint, data_channel smallint);";


            CassandraUtils.checkedExecute(session, createKeyspace);
            CassandraUtils.checkedExecute(session, useKeySpace);

            CassandraUtils.checkedExecute(session, createCountersTable);
            CassandraUtils.checkedExecute(session, createDomainsTable);
            CassandraUtils.checkedExecute(session, createImportJobsTable);
            CassandraIdGenerator.initializeTableCounter(session, TABLE_IDS);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    /**
     * This method states if the current keyspace exists or not
     *
     * @return True if the current keyspace exists in the DB; else, false
     * @throws PersistenceException
     */
    @Override
    public boolean exists() throws PersistenceException {
        return (cluster.getMetadata().getKeyspace(this.keyspace) != null);
    }

    /**
     * This method closes the Cluster,
     * which is the current access point to the DB
     * (it DOES NOT kill the DB process!)
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        this.cluster.close();
    }
}