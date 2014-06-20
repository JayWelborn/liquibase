package liquibase.actiongenerator.core;

import liquibase.CatalogAndSchema;
import liquibase.action.Action;
import liquibase.action.core.ColumnsMetaDataQueryAction;
import liquibase.actiongenerator.AbstractActionGenerator;
import liquibase.actiongenerator.ActionGeneratorChain;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.OfflineConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.executor.ExecutionOptions;
import liquibase.statement.core.FetchObjectsStatement;
import liquibase.structure.core.Column;

public class FetchColumnsSnapshotGenerator extends AbstractActionGenerator<FetchObjectsStatement> {

    @Override
    public boolean supports(FetchObjectsStatement statement, ExecutionOptions options) {
        DatabaseConnection connection = options.getRuntimeEnvironment().getTargetDatabase().getConnection();
        if (connection == null || connection instanceof JdbcConnection || connection instanceof OfflineConnection) {
            return statement.getExample() instanceof Column;
        } else {
            return false;
        }

    }

    @Override
    public ValidationErrors validate(FetchObjectsStatement statement, ExecutionOptions options, ActionGeneratorChain chain) {
        Database database = options.getRuntimeEnvironment().getTargetDatabase();
        ValidationErrors errors = super.validate(statement, options, chain);
        Column example = (Column) statement.getExample();
        if (example.getSchema() != null && example.getSchema().getCatalogName() != null && example.getSchema().getName() != null) {
            if (!example.getSchema().getCatalogName().equals(example.getSchema().getName()) && !database.supportsSchemas()) {
                errors.addError("Database "+ database.getShortName()+" does not support separate catalogs and schemas");
            }
        }
        return errors;
    }

    @Override
    public boolean generateStatementsIsVolatile(ExecutionOptions options) {
        return false;
    }

    @Override
    public boolean generateRollbackStatementsIsVolatile(ExecutionOptions options) {
        return false;
    }

    @Override
    public Action[] generateActions(final FetchObjectsStatement statement, ExecutionOptions options, ActionGeneratorChain chain) {
        Database database = options.getRuntimeEnvironment().getTargetDatabase();

        if (database.getConnection() == null || database.getConnection() instanceof OfflineConnection) {
            throw new UnexpectedLiquibaseException("Cannot read table metadata for an offline database");
        }
        Column example = (Column) statement.getExample();
        String tableName = null;
        if (example.getRelation() != null) {
            if (database instanceof PostgresDatabase) {
                tableName = example.getRelation().getName().toLowerCase();
            } else {
                tableName = example.getRelation().getName().toUpperCase();
            }
        }

        String columnName = null;
        if (example.getName() != null) {
            if (database instanceof PostgresDatabase) {
                columnName = example.getName().toLowerCase();
            } else {
                columnName = example.getName().toUpperCase();
            }
        }
        String catalogName = null;
        String schemaName = null;
        if (example.getSchema() != null) {
            catalogName = example.getSchema().getCatalogName();
            schemaName = example.getSchema().getName();
        }
        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(catalogName, schemaName).customize(database);
        catalogName = ((AbstractJdbcDatabase) database).getJdbcCatalogName(catalogAndSchema);
        schemaName = ((AbstractJdbcDatabase) database).getJdbcSchemaName(catalogAndSchema);

        return new Action[] {
                new ColumnsMetaDataQueryAction(catalogName, schemaName, tableName, columnName)
        };
    }
}
