package org.pentaho.commons.metadata.mqleditor.editor.service.impl;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.pentaho.commons.connection.IPentahoConnection;
import org.pentaho.commons.metadata.mqleditor.IConnection;
import org.pentaho.commons.metadata.mqleditor.IDatasource;
import org.pentaho.commons.metadata.mqleditor.beans.BusinessData;
import org.pentaho.commons.metadata.mqleditor.editor.service.ConnectionServiceException;
import org.pentaho.commons.metadata.mqleditor.editor.service.DatasourceServiceException;
import org.pentaho.commons.metadata.mqleditor.utils.ResultSetConverter;
import org.pentaho.commons.metadata.mqleditor.utils.SerializedResultSet;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.repository.DomainAlreadyExistsException;
import org.pentaho.metadata.repository.DomainIdNullException;
import org.pentaho.metadata.repository.DomainStorageException;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.metadata.util.InlineEtlModelGenerator;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginResourceLoader;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.engine.services.connection.PentahoConnectionFactory;
import org.pentaho.platform.plugin.services.connections.sql.SQLConnection;
import org.pentaho.platform.plugin.services.webservices.SessionHandler;
import org.pentaho.pms.schema.v3.physical.IDataSource;
import org.pentaho.pms.schema.v3.physical.SQLDataSource;
import org.pentaho.pms.service.IModelManagementService;
import org.pentaho.pms.service.IModelQueryService;
import org.pentaho.pms.service.JDBCModelManagementService;
import org.pentaho.pms.service.ModelManagementServiceException;

public class DatasourceServiceDelegate {

  private IDataAccessPermissionHandler dataAccessPermHandler;
  private List<IDatasource> datasources = new ArrayList<IDatasource>();
  private IModelManagementService modelManagementService;
  private IModelQueryService modelQueryService;
  private IMetadataDomainRepository metadataDomainRepository;
  private IPentahoSession session = null;
  public DatasourceServiceDelegate(IPentahoSession session) {
    this.session = session;
    modelManagementService =  new JDBCModelManagementService();
    metadataDomainRepository = PentahoSystem.get(IMetadataDomainRepository.class, null);
  }
  
  protected boolean hasDataAccessPermission() {
    if (dataAccessPermHandler == null) {
      String dataAccessClassName = null;
      try {
        IPluginResourceLoader resLoader = PentahoSystem.get(IPluginResourceLoader.class, null);
        dataAccessClassName = resLoader.getPluginSetting(getClass(), "settings/data-access-permission-handler", "org.pentaho.dataaccess.datasource.wizard.service.impl.SimpleDataAccessPermissionHandler" );  //$NON-NLS-1$ //$NON-NLS-2$
        Class<?> clazz = Class.forName(dataAccessClassName);
        Constructor<?> defaultConstructor = clazz.getConstructor(new Class[]{});
        dataAccessPermHandler = (IDataAccessPermissionHandler)defaultConstructor.newInstance(new Object[]{});
      } catch (Exception e) {
        // TODO: error(Messages.getErrorString("DashboardRenderer.ERROR_0024_SQL_PERMISSIONS_INIT_ERROR", sqlExecClassName), e); //$NON-NLS-1$
        e.printStackTrace();
      }
      
    }
    return dataAccessPermHandler != null && dataAccessPermHandler.hasDataAccessPermission(SessionHandler.getSession());
  }
  
  
  public List<IDatasource> getDatasources() {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    return datasources;
  }
  
  public IDatasource getDatasourceByName(String name) {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    for(IDatasource datasource:datasources) {
      if(datasource.getDatasourceName().equals(name)) {
        return datasource;
      }
    }
    return null;
  }
  
  public Boolean addDatasource(IDatasource datasource) {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    datasources.add(datasource);
    return true;
  }
  
  public Boolean updateDatasource(IDatasource datasource) {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    for(IDatasource datasrc:datasources) {
      if(datasrc.getDatasourceName().equals(datasource.getDatasourceName())) {
        datasources.remove(datasrc);
        datasources.add(datasource);
      }
    }
    return true;
  }
  public Boolean deleteDatasource(IDatasource datasource) {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    datasources.remove(datasources.indexOf(datasource));
    return true;
  }
  public Boolean deleteDatasource(String name) {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    for(IDatasource datasource:datasources) {
      if(datasource.getDatasourceName().equals(name)) {
        return deleteDatasource(datasource);
      }
    }
    return false;
  }

  
  public SerializedResultSet doPreview(IConnection connection, String query, String previewLimit) throws DatasourceServiceException {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    SerializedResultSet serializedResultSet = null;
    int limit = (previewLimit != null && previewLimit.length() > 0) ? Integer.parseInt(previewLimit): -1;
    try {
      conn = getDataSourceConnection(connection);

      if (!StringUtils.isEmpty(query)) {
        stmt = conn.createStatement();
        if(limit >=0) {
          stmt.setMaxRows(limit);
        }        
        ResultSetConverter rsc = new ResultSetConverter(stmt.executeQuery(query));
        serializedResultSet =  new SerializedResultSet(rsc.getColumnTypeNames(), rsc.getMetaData(), rsc.getResultSet());
  
      } else {
        throw new DatasourceServiceException("Query not valid"); //$NON-NLS-1$
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DatasourceServiceException("Query validation failed", e); //$NON-NLS-1$
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
        if (conn != null) {
          conn.close();
        }
      } catch (SQLException e) {
        throw new DatasourceServiceException(e);
      }
    }
    return serializedResultSet;

  }
  
  public SerializedResultSet doPreview(IConnection connection, String query) throws DatasourceServiceException {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    SerializedResultSet serializedResultSet = null;
    try {
      conn = getDataSourceConnection(connection);

      if (!StringUtils.isEmpty(query)) {
        stmt = conn.createStatement();
        ResultSetConverter rsc = new ResultSetConverter(stmt.executeQuery(query));
        serializedResultSet =  new SerializedResultSet(rsc.getColumnTypeNames(), rsc.getMetaData(), rsc.getResultSet());
      } else {
        throw new DatasourceServiceException("Query is not valid"); //$NON-NLS-1$
      }
    } catch (SQLException e) {
      throw new DatasourceServiceException("Query validation failed", e); //$NON-NLS-1$
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
        if (conn != null) {
          conn.close();
        }
      } catch (SQLException e) {
        throw new DatasourceServiceException(e);
      }
    }
    return serializedResultSet;

  }
  public SerializedResultSet doPreview(IDatasource datasource) throws DatasourceServiceException {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    String limit = datasource.getPreviewLimit();
    if(limit != null && limit.length() > 0) {
      return doPreview(datasource.getSelectedConnection(), datasource.getQuery(), limit);
    } else {
      return doPreview(datasource.getSelectedConnection(), datasource.getQuery());  
    }
    
  }
  
  /**
   * NOTE: caller is responsible for closing connection
   * 
   * @param ds
   * @return
   * @throws DatasourceServiceException
   */
  private static Connection getDataSourceConnection(IConnection connection) throws DatasourceServiceException {
    Connection conn = null;

    String driverClass = connection.getDriverClass();
    if (StringUtils.isEmpty(driverClass)) {
      throw new DatasourceServiceException("Connection attempt failed"); //$NON-NLS-1$  
    }
    Class<?> driverC = null;

    try {
      driverC = Class.forName(driverClass);
    } catch (ClassNotFoundException e) {
      throw new DatasourceServiceException("Driver not found in the class path. Driver was " + driverClass, e); //$NON-NLS-1$
    }
    if (!Driver.class.isAssignableFrom(driverC)) {
      throw new DatasourceServiceException("Driver not found in the class path. Driver was " + driverClass); //$NON-NLS-1$    }
    }
    Driver driver = null;
    
    try {
      driver = driverC.asSubclass(Driver.class).newInstance();
    } catch (InstantiationException e) {
      throw new DatasourceServiceException("Unable to instance the driver", e); //$NON-NLS-1$
    } catch (IllegalAccessException e) {
      throw new DatasourceServiceException("Unable to instance the driver", e); //$NON-NLS-1$    }
    }
    try {
      DriverManager.registerDriver(driver);
      conn = DriverManager.getConnection(connection.getUrl(), connection.getUsername(), connection.getPassword());
      return conn;
    } catch (SQLException e) {
      throw new DatasourceServiceException("Unable to connect", e); //$NON-NLS-1$
    }
  }

  public boolean testDataSourceConnection(IConnection connection) throws DatasourceServiceException {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return false;
    }
    Connection conn = null;
    try {
      conn = getDataSourceConnection(connection);
    } catch (DatasourceServiceException dme) {
      throw new DatasourceServiceException(dme.getMessage(), dme);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
      } catch (SQLException e) {
        throw new DatasourceServiceException(e);
      }
    }
    return true;
  }

  /**
   * Construct the IDataSource from IConnection and a SQL query
   * This is a temporary fix. We need to figure out a better way of doing. Will be gone once we implement the thin version of common database dialog
   * @param IConnection connection, String query
   * @return IDataSource
   * @throws DatasourceServiceException
   */
  private IDataSource constructIDataSource(IConnection connection, String query) throws DatasourceServiceException{
    final String SLASH = "/"; //$NON-NLS-1$
    final String DOUBLE_SLASH = "//";//$NON-NLS-1$
    final String COLON = ":";//$NON-NLS-1$
    String databaseType = null;
    String databaseName = null;
    String hostname = null;
    String port = null;
    String url = connection.getUrl();
    try {
    int lastIndexOfSlash = url.lastIndexOf(SLASH); 
    if((lastIndexOfSlash >= 0) &&( lastIndexOfSlash +SLASH.length() <=url.length())) {
      databaseName = url.substring(lastIndexOfSlash+SLASH.length() ,url.length());
    }
    int lastIndexOfDoubleSlash =  url.lastIndexOf(DOUBLE_SLASH);
    int indexOfColonFromDoubleSlash = url.indexOf(COLON,lastIndexOfDoubleSlash);
    if(lastIndexOfDoubleSlash >=  0 && lastIndexOfDoubleSlash+DOUBLE_SLASH.length() <= url.length()) {
      hostname = url.substring(lastIndexOfDoubleSlash+DOUBLE_SLASH.length(), indexOfColonFromDoubleSlash);
    }
    if(indexOfColonFromDoubleSlash >=0 && indexOfColonFromDoubleSlash + SLASH.length() <= url.length() &&  lastIndexOfSlash >=0 && lastIndexOfSlash <= url.length()) {
      port = url.substring(indexOfColonFromDoubleSlash + SLASH.length(), lastIndexOfSlash);
    }
    if(connection.getDriverClass().equals("org.hsqldb.jdbcDriver")) {//$NON-NLS-1$
      databaseType = "Hypersonic";//$NON-NLS-1$
    } else if(connection.getDriverClass().equals("com.mysql.jdbc.Driver") || connection.getDriverClass().equals("org.git.mm.mysql.Driver")){ //$NON-NLS-1$ //$NON-NLS-2$ 
      databaseType="MySql"; //$NON-NLS-1$
    }
    DatabaseMeta dbMeta = new DatabaseMeta(databaseName, databaseType, "JDBC", hostname, databaseName, port, connection.getUsername(), connection.getPassword()); //$NON-NLS-1$
    return new SQLDataSource(dbMeta, query);
    } catch(Exception e) {
      throw new DatasourceServiceException(e);
    }
  }

  /**
   * This method gets the business data which are the business columns, columns types and sample preview data
   * 
   * @param modelName, connection, query, previewLimit
   * @return BusinessData
   * @throws DatasourceServiceException
   */
  
  public BusinessData generateModel(String modelName, IConnection connection, String query, String previewLimit) throws DatasourceServiceException {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    try {
      IDataSource dataSource = constructIDataSource(connection, query);
      SQLConnection sqlConnection= (SQLConnection) PentahoConnectionFactory.getConnection(IPentahoConnection.SQL_DATASOURCE, connection.getDriverClass(),
          connection.getUrl(), connection.getUsername(), connection.getPassword(), null, null);
      
      Domain domain = getModelManagementService().generateModel(modelName, connection.getName(), sqlConnection.getNativeConnection(), query);
      List<List<String>> data = getModelManagementService().getDataSample(dataSource, Integer.parseInt(previewLimit));
      
      return new BusinessData(domain, data);
    } catch(ModelManagementServiceException mmse) {
      throw new DatasourceServiceException(mmse.getLocalizedMessage(), mmse);
    }
  }

  /**
   * This method generates the business mode from the query and save it
   * 
   * @param modelName, connection, query
   * @return BusinessData
   * @throws DatasourceServiceException
   */  
  public BusinessData saveModel(String modelName, IConnection connection, String query, Boolean overwrite, String previewLimit)  throws DatasourceServiceException {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    Domain domain = null;
    try {
      IDataSource dataSource = constructIDataSource(connection, query);
      SQLConnection sqlConnection= (SQLConnection) PentahoConnectionFactory.getConnection(IPentahoConnection.SQL_DATASOURCE, connection.getDriverClass(),
          connection.getUrl(), connection.getUsername(), connection.getPassword(), null, null);
      domain = getModelManagementService().generateModel(modelName, connection.getName(),
          sqlConnection.getNativeConnection(), query);
      List<List<String>> data = getModelManagementService().getDataSample(dataSource, Integer.parseInt(previewLimit));
      getMetadataDomainRepository().storeDomain(domain, overwrite);
      return new BusinessData(domain, data);
    } catch(ModelManagementServiceException mmse) {
      throw new DatasourceServiceException(mmse.getLocalizedMessage(), mmse);
    } catch(DomainStorageException dse) {
      throw new DatasourceServiceException("Unable to store domain" + domain.getName(), dse); //$NON-NLS-1$
    } catch(DomainAlreadyExistsException dae) {
      throw new DatasourceServiceException("Domain already exist" + domain.getName(), dae); //$NON-NLS-1$
    } catch(DomainIdNullException dne) {
      throw new DatasourceServiceException("Domain ID is null", dne); //$NON-NLS-1$
    }
  }
  /**
   * This method save the model
   * 
   * @param businessData, overwrite
   * @return Boolean
   * @throws DataSourceManagementException
   */  
  public Boolean saveModel(BusinessData businessData, Boolean overwrite)throws DatasourceServiceException {
    if (!hasDataAccessPermission()) {
      // TODO: log
      System.out.println("NO PERMISSION");
      return null;
    }
    Boolean returnValue = false;
    try {
    getMetadataDomainRepository().storeDomain(businessData.getDomain(), overwrite);
    returnValue = true;
    } catch(DomainStorageException dse) {
      throw new DatasourceServiceException("Unable to store domain" + businessData.getDomain().getName(), dse); //$NON-NLS-1$
    } catch(DomainAlreadyExistsException dae) {
      throw new DatasourceServiceException("Domain already exist" + businessData.getDomain().getName(), dae); //$NON-NLS-1$
    } catch(DomainIdNullException dne) {
      throw new DatasourceServiceException("Domain ID is null", dne); //$NON-NLS-1$
    }
    return returnValue;
  }
  public void setModelManagementService(IModelManagementService modelManagementService) {
    this.modelManagementService = modelManagementService;
  }

  public IModelManagementService getModelManagementService() {
    return modelManagementService;
  }

  public void setModelQueryService(IModelQueryService modelQueryService) {
    this.modelQueryService = modelQueryService;
  }

  public IModelQueryService getModelQueryService() {
    return modelQueryService;
  }
  
  public IMetadataDomainRepository getMetadataDomainRepository() {
    return metadataDomainRepository;
  }

  public void setMetadataDomainRepository(IMetadataDomainRepository metadataDomainRepository) {
    this.metadataDomainRepository = metadataDomainRepository;
  }

  /**
   * NOTE: caller is responsible for closing connection
   * 
   * @param ds
   * @return
   * @throws DataSourceManagementException
   */
  private static Connection getConnection(IConnection connection) throws ConnectionServiceException {
    Connection conn = null;

    String driverClass = connection.getDriverClass();
    if (StringUtils.isEmpty(driverClass)) {
      throw new ConnectionServiceException("Connection attempt failed"); //$NON-NLS-1$  
    }
    Class<?> driverC = null;

    try {
      driverC = Class.forName(driverClass);
    } catch (ClassNotFoundException e) {
      throw new ConnectionServiceException("Driver not found in the class path. Driver was " + driverClass, e); //$NON-NLS-1$
    }
    if (!Driver.class.isAssignableFrom(driverC)) {
      throw new ConnectionServiceException("Driver not found in the class path. Driver was " + driverClass); //$NON-NLS-1$    }
    }
    Driver driver = null;
    
    try {
      driver = driverC.asSubclass(Driver.class).newInstance();
    } catch (InstantiationException e) {
      throw new ConnectionServiceException("Unable to instance the driver", e); //$NON-NLS-1$
    } catch (IllegalAccessException e) {
      throw new ConnectionServiceException("Unable to instance the driver", e); //$NON-NLS-1$    }
    }
    try {
      DriverManager.registerDriver(driver);
      conn = DriverManager.getConnection(connection.getUrl(), connection.getUsername(), connection.getPassword());
      return conn;
    } catch (SQLException e) {
      throw new ConnectionServiceException("Unable to connect", e); //$NON-NLS-1$
    }
  }

  public boolean isAdministrator() {
    return SecurityHelper.isPentahoAdministrator(this.session);
  }
  
  public Domain generateInlineEtlModel(String modelName, String relativeFilePath, boolean headersPresent, String delimeter, String enclosure) throws DatasourceServiceException {
    try  {
    InlineEtlModelGenerator generator = new InlineEtlModelGenerator(modelName, relativeFilePath, headersPresent, delimeter, enclosure);
    return generator.generate();
    } catch(Exception e) {
      throw new DatasourceServiceException("Unable to generate the model" + e.getLocalizedMessage());
    }
  }

  public Boolean saveInlineEtlModel(Domain modelName, boolean overwrite) throws DatasourceServiceException  {
    try {
      getMetadataDomainRepository().storeDomain(modelName, overwrite);
      return true;
    } catch(DomainStorageException dse) {
      throw new DatasourceServiceException("Unable to store domain" + modelName.getName(), dse); //$NON-NLS-1$
    } catch(DomainAlreadyExistsException dae) {
      throw new DatasourceServiceException("Domain already exist" + modelName.getName(), dae); //$NON-NLS-1$
    } catch(DomainIdNullException dne) {
      throw new DatasourceServiceException("Domain ID is null", dne); //$NON-NLS-1$
    }
  }
}