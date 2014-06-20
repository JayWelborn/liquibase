package liquibase.sqlgenerator.core;

import liquibase.CatalogAndSchema;
import liquibase.action.Action;
import liquibase.action.core.UnparsedSql;
import liquibase.actiongenerator.ActionGeneratorChain;
import liquibase.database.Database;
import liquibase.database.core.SybaseDatabase;
import liquibase.executor.ExecutionOptions;
import liquibase.statement.core.GetViewDefinitionStatement;

public class GetViewDefinitionGeneratorSybase extends GetViewDefinitionGenerator {
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(GetViewDefinitionStatement statement, ExecutionOptions options) {
        return options.getRuntimeEnvironment().getTargetDatabase() instanceof SybaseDatabase;
    }

    @Override
    public Action[] generateActions(GetViewDefinitionStatement statement, ExecutionOptions options, ActionGeneratorChain chain) {
        Database database = options.getRuntimeEnvironment().getTargetDatabase();

        CatalogAndSchema schema = new CatalogAndSchema(statement.getCatalogName(), statement.getSchemaName()).customize(database);

        String schemaName = schema.getSchemaName();
        if (schemaName == null) {
            schemaName = database.getDefaultSchemaName();
        }
        if (schemaName == null) {
            schemaName = "dbo";
        }

        String sql = "select text from syscomments where id = object_id('" +
                schemaName + "." +
                statement.getViewName() + "') order by colid";

        return new Action[]{
                new UnparsedSql(sql)
        };
    }
}