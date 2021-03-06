/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Single database connection implementation.
 * 
 * Created by The eXo Platform SAS. </br>
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady
 *         Azarenkov</a>
 * @version $Id: MultiDbJDBCConnection.java 20950 2008-10-06 14:23:07Z
 *          pnedonosko $
 */

public class MultiDbJDBCConnection extends JDBCStorageConnection
{

   /**
    * Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public MultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {

      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getIdentifier(final String internalId)
   {
      return internalId;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getInternalId(final String identifier)
   {
      return identifier;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {

      FIND_ITEM_BY_ID = "select * from " + JCR_ITEM + " where ID=?";

      FIND_ITEM_BY_NAME =
         "select * from " + JCR_ITEM + " where PARENT_ID=? and NAME=? and I_INDEX=? order by I_CLASS, VERSION DESC";

      FIND_PROPERTY_BY_NAME =
         "select V.DATA" + " from " + JCR_ITEM + " I, " + JCR_VALUE + " V"
            + " where I.I_CLASS=2 and I.PARENT_ID=? and I.NAME=? and I.ID=V.PROPERTY_ID order by V.ORDER_NUM";

      FIND_REFERENCES =
         "select P.ID, P.PARENT_ID, P.VERSION, P.P_TYPE, P.P_MULTIVALUED, P.NAME" + " from " + JCR_REF + " R, "
            + JCR_ITEM + " P" + " where R.NODE_ID=? and P.ID=R.PROPERTY_ID and P.I_CLASS=2";

      FIND_VALUES_BY_PROPERTYID =
         "select PROPERTY_ID, ORDER_NUM, DATA, STORAGE_DESC from " + JCR_VALUE
            + " where PROPERTY_ID=? order by ORDER_NUM";

      FIND_NODES_BY_PARENTID =
         "select * from " + JCR_ITEM + " where I_CLASS=1 and PARENT_ID=?" + " order by N_ORDER_NUM";

      FIND_LAST_ORDER_NUMBER_BY_PARENTID =
         "select count(*), max(N_ORDER_NUM) from " + JCR_ITEM + " where I_CLASS=1 and PARENT_ID=?";

      FIND_NODES_COUNT_BY_PARENTID = "select count(ID) from " + JCR_ITEM + " where I_CLASS=1 and PARENT_ID=?";

      FIND_PROPERTIES_BY_PARENTID = "select * from " + JCR_ITEM + " where I_CLASS=2 and PARENT_ID=?" + " order by ID";

      FIND_MAX_PROPERTY_VERSIONS =
         "select max(VERSION) FROM " + JCR_ITEM + " WHERE PARENT_ID=? and NAME=? and I_INDEX=? and I_CLASS=2";

      INSERT_NODE =
         "insert into " + JCR_ITEM + "(ID, PARENT_ID, NAME, VERSION, I_CLASS, I_INDEX, N_ORDER_NUM) VALUES(?,?,?,?,"
            + I_CLASS_NODE + ",?,?)";
      INSERT_PROPERTY =
         "insert into " + JCR_ITEM
            + "(ID, PARENT_ID, NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,"
            + I_CLASS_PROPERTY + ",?,?,?)";

      INSERT_VALUE = "insert into " + JCR_VALUE + "(DATA, ORDER_NUM, PROPERTY_ID, STORAGE_DESC) VALUES(?,?,?,?)";
      INSERT_REF = "insert into " + JCR_REF + "(NODE_ID, PROPERTY_ID, ORDER_NUM) VALUES(?,?,?)";

      RENAME_NODE =
         "update " + JCR_ITEM + " set PARENT_ID=?, NAME =?, VERSION=?, I_INDEX =?, N_ORDER_NUM =? where ID=?";

      UPDATE_NODE = "update " + JCR_ITEM + " set VERSION=?, I_INDEX=?, N_ORDER_NUM=? where ID=?";
      UPDATE_PROPERTY = "update " + JCR_ITEM + " set VERSION=?, P_TYPE=? where ID=?";
      //UPDATE_VALUE = "update "+JCR_VALUE+" set DATA=?, STORAGE_DESC=? where PROPERTY_ID=?, ORDER_NUM=?";

      DELETE_ITEM = "delete from " + JCR_ITEM + " where ID=?";
      DELETE_VALUE = "delete from " + JCR_VALUE + " where PROPERTY_ID=?";
      DELETE_REF = "delete from " + JCR_REF + " where PROPERTY_ID=?";

      FIND_NODES_COUNT = "select count(*) from " + JCR_ITEM + " I where I.I_CLASS=1";

      FIND_WORKSPACE_DATA_SIZE = "select sum(length(DATA)) from " + JCR_VALUE;

      FIND_WORKSPACE_PROPERTIES_ON_VALUE_STORAGE =
         "select PROPERTY_ID, STORAGE_DESC, ORDER_NUM from " + JCR_VALUE + " where STORAGE_DESC is not null";

      FIND_NODE_DATA_SIZE =
         "select sum(length(DATA)) from " + JCR_ITEM + " I, " + JCR_VALUE
            + " V  where I.PARENT_ID=? and I.I_CLASS=2 and I.ID=V.PROPERTY_ID";

      FIND_NODE_PROPERTIES_ON_VALUE_STORAGE =
         "select V.PROPERTY_ID, V.STORAGE_DESC, V.ORDER_NUM from " + JCR_ITEM + " I, " + JCR_VALUE + " V  "
            + "where I.PARENT_ID=? and I.I_CLASS=2 and I.ID=V.PROPERTY_ID and V.STORAGE_DESC is not null";

      FIND_VALUE_STORAGE_DESC_AND_SIZE = "select length(DATA), STORAGE_DESC from " + JCR_VALUE + " where PROPERTY_ID=?";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(NodeData data) throws SQLException
   {
      if (insertNode == null)
      {
         insertNode = dbConnection.prepareStatement(INSERT_NODE);
      }
      else
      {
         insertNode.clearParameters();
      }

      insertNode.setString(1, data.getIdentifier());
      insertNode.setString(2,
         data.getParentIdentifier() == null ? Constants.ROOT_PARENT_UUID : data.getParentIdentifier());
      insertNode.setString(3, data.getQPath().getName().getAsString());
      insertNode.setInt(4, data.getPersistedVersion());
      insertNode.setInt(5, data.getQPath().getIndex());
      insertNode.setInt(6, data.getOrderNumber());
      return insertNode.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addPropertyRecord(PropertyData data) throws SQLException
   {
      if (insertProperty == null)
      {
         insertProperty = dbConnection.prepareStatement(INSERT_PROPERTY);
      }
      else
      {
         insertProperty.clearParameters();
      }

      insertProperty.setString(1, data.getIdentifier());
      insertProperty.setString(2, data.getParentIdentifier());
      insertProperty.setString(3, data.getQPath().getName().getAsString());
      insertProperty.setInt(4, data.getPersistedVersion());
      insertProperty.setInt(5, data.getQPath().getIndex());
      insertProperty.setInt(6, data.getType());
      insertProperty.setBoolean(7, data.isMultiValued());

      return insertProperty.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addReference(PropertyData data) throws SQLException, IOException
   {
      if (insertReference == null)
      {
         insertReference = dbConnection.prepareStatement(INSERT_REF);
      }
      else
      {
         insertReference.clearParameters();
      }

      if (data.getQPath().getAsString().indexOf("versionableUuid") > 0)
      {
         LOG.info("add ref versionableUuid " + data.getQPath().getAsString());
      }

      List<ValueData> values = data.getValues();
      int added = 0;
      for (int i = 0; i < values.size(); i++)
      {
         ValueData vdata = values.get(i);
         String refNodeIdentifier;
         try
         {
            refNodeIdentifier = ValueDataUtil.getString(vdata);
         }
         catch (RepositoryException e)
         {
            throw new IOException(e.getMessage(), e);
         }

         insertReference.setString(1, refNodeIdentifier);
         insertReference.setString(2, data.getIdentifier());
         insertReference.setInt(3, i);
         added += insertReference.executeUpdate();
      }

      return added;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteReference(String propertyIdentifier) throws SQLException
   {
      if (deleteReference == null)
      {
         deleteReference = dbConnection.prepareStatement(DELETE_REF);
      }
      else
      {
         deleteReference.clearParameters();
      }

      deleteReference.setString(1, propertyIdentifier);
      return deleteReference.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteItemByIdentifier(String identifier) throws SQLException
   {
      if (deleteItem == null)
      {
         deleteItem = dbConnection.prepareStatement(DELETE_ITEM);
      }
      else
      {
         deleteItem.clearParameters();
      }

      deleteItem.setString(1, identifier);
      return deleteItem.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int updateNodeByIdentifier(int version, int index, int orderNumb, String identifier) throws SQLException
   {
      if (updateNode == null)
      {
         updateNode = dbConnection.prepareStatement(UPDATE_NODE);
      }
      else
      {
         updateNode.clearParameters();
      }

      updateNode.setInt(1, version);
      updateNode.setInt(2, index);
      updateNode.setInt(3, orderNumb);
      updateNode.setString(4, identifier);
      return updateNode.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int updatePropertyByIdentifier(int version, int type, String identifier) throws SQLException
   {
      if (updateProperty == null)
      {
         updateProperty = dbConnection.prepareStatement(UPDATE_PROPERTY);
      }
      else
      {
         updateProperty.clearParameters();
      }

      updateProperty.setInt(1, version);
      updateProperty.setInt(2, type);
      updateProperty.setString(3, identifier);
      return updateProperty.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findItemByName(String parentId, String name, int index) throws SQLException
   {
      if (findItemByName == null)
      {
         findItemByName = dbConnection.prepareStatement(FIND_ITEM_BY_NAME);
      }
      else
      {
         findItemByName.clearParameters();
      }

      findItemByName.setString(1, parentId);
      findItemByName.setString(2, name);
      findItemByName.setInt(3, index);
      return findItemByName.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findPropertyByName(String parentId, String name) throws SQLException
   {
      if (findPropertyByName == null)
      {
         findPropertyByName = dbConnection.prepareStatement(FIND_PROPERTY_BY_NAME);
      }
      else
      {
         findPropertyByName.clearParameters();
      }

      findPropertyByName.setString(1, parentId);
      findPropertyByName.setString(2, name);
      return findPropertyByName.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findItemByIdentifier(String identifier) throws SQLException
   {
      if (findItemById == null)
      {
         findItemById = dbConnection.prepareStatement(FIND_ITEM_BY_ID);
      }
      else
      {
         findItemById.clearParameters();
      }

      findItemById.setString(1, identifier);
      return findItemById.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findReferences(String nodeIdentifier) throws SQLException
   {
      if (findReferences == null)
      {
         findReferences = dbConnection.prepareStatement(FIND_REFERENCES);
      }
      else
      {
         findReferences.clearParameters();
      }

      findReferences.setString(1, nodeIdentifier);
      return findReferences.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifier(String parentIdentifier) throws SQLException
   {
      if (findNodesByParentId == null)
      {
         findNodesByParentId = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID);
      }
      else
      {
         findNodesByParentId.clearParameters();
      }

      findNodesByParentId.setString(1, parentIdentifier);
      return findNodesByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findLastOrderNumberByParentIdentifier(String parentIdentifier) throws SQLException
   {
      if (findLastOrderNumberByParentId == null)
      {
         findLastOrderNumberByParentId = dbConnection.prepareStatement(FIND_LAST_ORDER_NUMBER_BY_PARENTID);
      }
      else
      {
         findLastOrderNumberByParentId.clearParameters();
      }

      findLastOrderNumberByParentId.setString(1, parentIdentifier);
      return findLastOrderNumberByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesCountByParentIdentifier(String parentIdentifier) throws SQLException
   {
      if (findNodesCountByParentId == null)
      {
         findNodesCountByParentId = dbConnection.prepareStatement(FIND_NODES_COUNT_BY_PARENTID);
      }
      else
      {
         findNodesCountByParentId.clearParameters();
      }

      findNodesCountByParentId.setString(1, parentIdentifier);
      return findNodesCountByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildPropertiesByParentIdentifier(String parentIdentifier) throws SQLException
   {
      if (findPropertiesByParentId == null)
      {
         findPropertiesByParentId = dbConnection.prepareStatement(FIND_PROPERTIES_BY_PARENTID);
      }
      else
      {
         findPropertiesByParentId.clearParameters();
      }

      findPropertiesByParentId.setString(1, parentIdentifier);
      return findPropertiesByParentId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findChildNodesByParentIdentifier(String parentCid,int fromOrderNum, int offset, int limit)
      throws SQLException
   {
      throw new UnsupportedOperationException("findChildNodesByParentIdentifier is not supported for old queries");
   }

   // -------- values processing ------------

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addValueData(String cid, int orderNumber, InputStream stream, int streamLength, String storageDesc)
      throws SQLException
   {

      if (insertValue == null)
      {
         insertValue = dbConnection.prepareStatement(INSERT_VALUE);
      }
      else
      {
         insertValue.clearParameters();
      }

      if (stream == null)
      {
         // [PN] store vd reference to external storage etc.
         insertValue.setNull(1, Types.BINARY);
         insertValue.setString(4, storageDesc);
      }
      else
      {
         insertValue.setBinaryStream(1, stream, streamLength);
         insertValue.setNull(4, Types.VARCHAR);
      }

      insertValue.setInt(2, orderNumber);
      insertValue.setString(3, cid);
      return insertValue.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int deleteValueData(String cid) throws SQLException
   {
      if (deleteValue == null)
      {
         deleteValue = dbConnection.prepareStatement(DELETE_VALUE);
      }
      else
      {
         deleteValue.clearParameters();
      }

      deleteValue.setString(1, cid);
      return deleteValue.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findValuesByPropertyId(String cid) throws SQLException
   {
      if (findValuesByPropertyId == null)
      {
         findValuesByPropertyId = dbConnection.prepareStatement(FIND_VALUES_BY_PROPERTYID);
      }
      else
      {
         findValuesByPropertyId.clearParameters();
      }

      findValuesByPropertyId.setString(1, cid);
      return findValuesByPropertyId.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int renameNode(NodeData data) throws SQLException
   {
      if (renameNode == null)
      {
         renameNode = dbConnection.prepareStatement(RENAME_NODE);
      }
      else
      {
         renameNode.clearParameters();
      }

      renameNode.setString(1,
         data.getParentIdentifier() == null ? Constants.ROOT_PARENT_UUID : data.getParentIdentifier());
      renameNode.setString(2, data.getQPath().getName().getAsString());
      renameNode.setInt(3, data.getPersistedVersion());
      renameNode.setInt(4, data.getQPath().getIndex());
      renameNode.setInt(5, data.getOrderNumber());
      renameNode.setString(6, data.getIdentifier());
      return renameNode.executeUpdate();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      throw new UnsupportedOperationException(
         "The method findNodesAndProperties is not supported for this type of connection use the complex queries instead");
   }

   /**
    * {@inheritDoc}
    */
   protected void deleteLockProperties() throws SQLException
   {
      PreparedStatement removeValuesStatement = null;
      PreparedStatement removeItemsStatement = null;

      try
      {
         removeValuesStatement =
            dbConnection.prepareStatement("DELETE FROM " + JCR_VALUE + " WHERE PROPERTY_ID IN " + "(SELECT ID FROM "
               + JCR_ITEM + " WHERE NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep' OR"
               + " NAME = '[http://www.jcp.org/jcr/1.0]lockOwner')");

         removeItemsStatement =
            dbConnection.prepareStatement("DELETE FROM " + JCR_ITEM
               + " WHERE NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep'"
               + " OR NAME = '[http://www.jcp.org/jcr/1.0]lockOwner'");

         removeValuesStatement.executeUpdate();
         removeItemsStatement.executeUpdate();
      }
      finally
      {
         if (removeValuesStatement != null)
         {
            try
            {
               removeValuesStatement.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close statement", e);
            }
         }

         if (removeItemsStatement != null)
         {
            try
            {
               removeItemsStatement.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close statement", e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesCount() throws SQLException
   {
      if (findNodesCount == null)
      {
         findNodesCount = dbConnection.prepareStatement(FIND_NODES_COUNT);
      }
      return findNodesCount.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findMaxPropertyVersion(String parentId, String name, int index) throws SQLException
   {
      if (findMaxPropertyVersions == null)
      {
         findMaxPropertyVersions = dbConnection.prepareStatement(FIND_MAX_PROPERTY_VERSIONS);
      }

      findMaxPropertyVersions.setString(1, getInternalId(parentId));
      findMaxPropertyVersions.setString(2, name);
      findMaxPropertyVersions.setInt(3, index);

      return findMaxPropertyVersions.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findWorkspaceDataSize() throws SQLException
   {
      if (findWorkspaceDataSize == null)
      {
         findWorkspaceDataSize = dbConnection.prepareStatement(FIND_WORKSPACE_DATA_SIZE);
      }

      return findWorkspaceDataSize.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findWorkspacePropertiesOnValueStorage() throws SQLException
   {
      if (findWorkspacePropertiesOnValueStorage == null)
      {
         findWorkspacePropertiesOnValueStorage =
            dbConnection.prepareStatement(FIND_WORKSPACE_PROPERTIES_ON_VALUE_STORAGE);
      }

      return findWorkspacePropertiesOnValueStorage.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findNodeDataSize(String parentId) throws SQLException
   {
      if (findNodeDataSize == null)
      {
         findNodeDataSize = dbConnection.prepareStatement(FIND_NODE_DATA_SIZE);
      }

      findNodeDataSize.setString(1, parentId);

      return findNodeDataSize.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findNodePropertiesOnValueStorage(String parentId) throws SQLException
   {
      if (findNodePropertiesOnValueStorage == null)
      {
         findNodePropertiesOnValueStorage = dbConnection.prepareStatement(FIND_NODE_PROPERTIES_ON_VALUE_STORAGE);
      }

      findNodePropertiesOnValueStorage.setString(1, parentId);

      return findNodePropertiesOnValueStorage.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findValueStorageDescAndSize(String cid) throws SQLException
   {
      if (findValueStorageDescAndSize == null)
      {
         findValueStorageDescAndSize = dbConnection.prepareStatement(FIND_VALUE_STORAGE_DESC_AND_SIZE);
      }

      findValueStorageDescAndSize.setString(1, cid);

      return findValueStorageDescAndSize.executeQuery();
   }
}
