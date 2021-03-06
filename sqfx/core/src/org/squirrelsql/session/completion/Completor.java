package org.squirrelsql.session.completion;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.squirrelsql.session.ColumnInfo;
import org.squirrelsql.session.ProcedureInfo;
import org.squirrelsql.session.TableInfo;
import org.squirrelsql.session.parser.kernel.TableAliasInfo;
import org.squirrelsql.session.schemainfo.SchemaCacheProperty;
import org.squirrelsql.session.schemainfo.StructItemCatalog;
import org.squirrelsql.session.schemainfo.StructItemSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Completor
{
   private SchemaCacheProperty _schemaCacheValue;
   private TableAliasInfo[] _currentAliasInfos;
   private List<TableCompletionCandidate> _currentTableCandidatesNextToCursors = new ArrayList<>();

   public Completor(SchemaCacheProperty schemaCacheValue, List<TableInfo> currentTableInfosNextToCursor, TableAliasInfo[] currentAliasInfos)
   {
      _schemaCacheValue = schemaCacheValue;
      _currentAliasInfos = currentAliasInfos;

      for (TableInfo tableInfo : currentTableInfosNextToCursor)
      {
         _currentTableCandidatesNextToCursors.add(new TableCompletionCandidate(tableInfo, tableInfo.getStructItemSchema()));
      }
   }

   public ObservableList<CompletionCandidate> getCompletions(TokenParser tokenParser)
   {
      List<CompletionCandidate> ret = new ArrayList<>();


      if(0 == tokenParser.completedSplitsCount()) // everything
      {

         for (TableCompletionCandidate tableCompletionCandidate : _currentTableCandidatesNextToCursors)
         {
            for (ColumnInfo columnInfo : _schemaCacheValue.get().getColumns(tableCompletionCandidate.getTableInfo()))
            {
               if(tokenParser.uncompletedSplitMatches(columnInfo.getColName()))
               {
                  ret.add(new ColumnCompletionCandidate(columnInfo, tableCompletionCandidate));
               }
            }
         }

         for (TableAliasInfo currentAliasInfo : _currentAliasInfos)
         {
            if(tokenParser.uncompletedSplitMatches(currentAliasInfo.aliasName))
            {
               /////////////////////////////////////////////////////////////////////
               // For now we check duplicates for tables only.
               AliasCompletionCandidate aliasCompletionCandidate = new AliasCompletionCandidate(currentAliasInfo);
               //
               //////////////////////////////////////////////////////////////////////

               ret.add(aliasCompletionCandidate);
            }
         }


         for (String keyword : _schemaCacheValue.get().getDefaultKeywords())
         {
            if(tokenParser.uncompletedSplitMatches(keyword))
            {
               ret.add(new KeywordCompletionCandidate(keyword));
            }
         }

         for (String keyword : _schemaCacheValue.get().getKeywords().getCellsAsString(0))
         {
            if(tokenParser.uncompletedSplitMatches(keyword))
            {
               ret.add(new KeywordCompletionCandidate(keyword));
            }
         }

         List<StructItemCatalog> catalogs = _schemaCacheValue.get().getCatalogs();

         for (StructItemCatalog catalog : catalogs)
         {
            if(tokenParser.uncompletedSplitMatches(catalog.getCatalog()))
            {
               ret.add(new CatalogCompletionCandidate(catalog));
            }
         }

         List<StructItemSchema> schemas = _schemaCacheValue.get().getSchemas();

         for (StructItemSchema schema : schemas)
         {
            if(tokenParser.uncompletedSplitMatches(schema.getSchema()))
            {
               ret.add(new SchemaCompletionCandidate(schema));
            }
         }

         List<String> functions = _schemaCacheValue.get().getAllFunctions();

         for (String function : functions)
         {
            if(tokenParser.uncompletedSplitMatches(function))
            {
               ret.add(new FunctionCompletionCandidate(function));
            }
         }

         // ???????
//         TableLoader<DataBaseType> types = _schemaCache.getTypes();
//
//         for (String function : functions)
//         {
//            if(tokenParser.uncompletedSplitMatches(function))
//            {
//               ret.add(new DataBaseTypeCompletionCandidate(function));
//            }
//         }

         fillTopLevelObjectsForSchemas(ret, tokenParser, createFakeSchemaArrayForCatalog(null));

      }
      else if(1 == tokenParser.completedSplitsCount()) // MyCatalog.xxx or MySchema.xxx or MyTable.xxx
      {
         StructItemCatalog catalog = _schemaCacheValue.get().getCatalogByName(tokenParser.getCompletedSplitAt(0));

         if(null != catalog) // MyCatalog.xxx
         {
            fillTopLevelObjectsForSchemas(ret, tokenParser, createFakeSchemaArrayForCatalog(catalog));
         }

         ///////////////////////////////////////////
         // MySchema.xxx
         List<StructItemSchema> schemas = _schemaCacheValue.get().getSchemasByName(tokenParser.getCompletedSplitAt(0));

         fillTopLevelObjectsForSchemas(ret, tokenParser, schemas);
         //
         ////////////////////////////////////////////

         ////////////////////////////////////////////////
         // MyTable.xxx
         fillColumnsForTable(ret, createFakeSchemaArrayForCatalog(null), tokenParser.getCompletedSplitAt(0), tokenParser);
         //
         //////////////////////////////////////////////////

         ////////////////////////////////////////////////
         // ALIAS.xxx
         for (TableAliasInfo currentAliasInfo : _currentAliasInfos)
         {
            if(tokenParser.getCompletedSplitAt(0).equalsIgnoreCase(currentAliasInfo.aliasName))
            {
               List<TableInfo> tablesBySimpleName = _schemaCacheValue.get().getTablesBySimpleName(currentAliasInfo.tableName);
               for (TableInfo tableInfo : tablesBySimpleName)
               {
                  fillColumnsForTable(ret, Arrays.asList(tableInfo.getStructItemSchema()), tableInfo.getName(), tokenParser);
               }
            }
         }
         //
         ///////////////////////////////////////////////////////


      }
      else if(2 == tokenParser.completedSplitsCount()) // MyCatalog.MySchema,xxx or MyCatalog.MyTable.xxx or MySchema.MyTable.xxx
      {
         StructItemCatalog catalog = _schemaCacheValue.get().getCatalogByName(tokenParser.getCompletedSplitAt(0));


         if (null != catalog) // MyCatalog.MySchema,xxx or MyCatalog.MyTable.xxx
         {
            List<StructItemSchema> schemas = _schemaCacheValue.get().getSchemaByNameAsArray(catalog.getCatalog(), tokenParser.getCompletedSplitAt(1));

            fillTopLevelObjectsForSchemas(ret, tokenParser, schemas);

            if(0 == schemas.size())
            {
               fillColumnsForTable(ret, createFakeSchemaArrayForCatalog(catalog), tokenParser.getCompletedSplitAt(1), tokenParser);
            }
         }
         else // MySchema.MyTable.xxx
         {
            List<StructItemSchema> schemas = _schemaCacheValue.get().getSchemasByName(tokenParser.getCompletedSplitAt(0));
            fillColumnsForTable(ret, schemas, tokenParser.getCompletedSplitAt(1), tokenParser);
         }
      }
      else if(3 == tokenParser.completedSplitsCount()) // MyCatalog.MySchema,MyTable.xxx
      {
         StructItemCatalog catalog = _schemaCacheValue.get().getCatalogByName(tokenParser.getCompletedSplitAt(0));

         if(null != catalog)
         {
            List<StructItemSchema> schemas = _schemaCacheValue.get().getSchemaByNameAsArray(catalog.getCatalog(), tokenParser.getCompletedSplitAt(1));

            fillColumnsForTable(ret, schemas, tokenParser.getCompletedSplitAt(2), tokenParser);
         }
      }

      return FXCollections.observableArrayList(ret);
   }

   private List<StructItemSchema> createFakeSchemaArrayForCatalog(StructItemCatalog catalog)
   {
      List<StructItemSchema> fakeSchemaArray = new ArrayList<>();

      StructItemSchema fakeSchema;

      if(null == catalog)
      {
         fakeSchema = new StructItemSchema(null, null);
      }
      else
      {
         fakeSchema = new StructItemSchema(null, catalog.getCatalog());
      }

      fakeSchemaArray.add(fakeSchema);
      return fakeSchemaArray;
   }

   private void fillColumnsForTable(List<CompletionCandidate> toFill, List<StructItemSchema> schemas, String tableName, TokenParser tokenParser)
   {
      for (StructItemSchema schema : schemas)
      {
         List<TableInfo> tables;

         tables = _schemaCacheValue.get().getTablesByFullyQualifiedName(schema.getCatalog(), schema.getSchema(), tableName);

         fillMatchingCols(toFill, tokenParser, tables, schema);

         if(tables.size() > 0)
         {
            return;
         }

         tables = _schemaCacheValue.get().getTablesBySchemaQualifiedName(schema.getSchema(), tableName);

         fillMatchingCols(toFill, tokenParser, tables, schema);

         if(tables.size() > 0)
         {
            return;
         }

         tables = _schemaCacheValue.get().getTablesBySimpleName(tableName);

         fillMatchingCols(toFill, tokenParser, tables, schema);
      }
   }

   private void fillMatchingCols(List<CompletionCandidate> ret, TokenParser tokenParser, List<TableInfo> tables, StructItemSchema schema)
   {
      for (TableInfo table : tables)
      {

         for (ColumnInfo columnInfo : _schemaCacheValue.get().getColumns(table))
         {
            if(tokenParser.uncompletedSplitMatches(columnInfo.getColName()))
            {
               ret.add(new ColumnCompletionCandidate(columnInfo, new TableCompletionCandidate(table, schema)));
            }
         }
      }
   }

   private void fillTopLevelObjectsForSchemas(List<CompletionCandidate> ret, TokenParser tokenParser, List<StructItemSchema> schemas)
   {
      for (StructItemSchema schema : schemas)
      {
         List<TableInfo> tableInfos = _schemaCacheValue.get().getTableInfosMatching(schema.getCatalog(), schema.getSchema(), TableTypes.getTableAndView());

         DuplicateSimpleNamesCheck duplicateSimpleNamesCheck = new DuplicateSimpleNamesCheck();

         for (TableInfo tableInfo : tableInfos)
         {
            if(tokenParser.uncompletedSplitMatches(tableInfo.getName()))
            {
               /////////////////////////////////////////////////////////////////////
               // For now we check duplicates for tables only.
               TableCompletionCandidate tableCompletionCandidate = new TableCompletionCandidate(tableInfo, schema);
               duplicateSimpleNamesCheck.check(tableCompletionCandidate);
               //
               //////////////////////////////////////////////////////////////////////

               ret.add(tableCompletionCandidate);
            }
         }

         List<ProcedureInfo> procedureInfos = _schemaCacheValue.get().getProcedureInfosMatching(schema.getCatalog(), schema.getSchema());

         for (ProcedureInfo procedureInfo : procedureInfos)
         {
            if(tokenParser.uncompletedSplitMatches(procedureInfo.getName()))
            {
               ret.add(new ProcedureCompletionCandidate(procedureInfo, schema));
            }
         }

//         Looks bad for Postgres should be made configurable
//         ArrayList<UDTInfo> udtInfos = _schemaCache.getUDTInfosMatching(schema.getCatalog(), schema.getSchema());
//
//         for (UDTInfo udtInfo : udtInfos)
//         {
//            if(tokenParser.uncompletedSplitMatches(udtInfo.getName()))
//            {
//               ret.add(new UDTCompletionCandidate(udtInfo, schema));
//            }
//         }
      }
   }
}
