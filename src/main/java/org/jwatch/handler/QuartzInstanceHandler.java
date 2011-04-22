/**
 * JWatch - Quartz Monitor: http://code.google.com/p/jwatch/
 * Copyright (C) 2011 Roy Russo and the original author or authors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301 USA
 **/
package org.jwatch.handler;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jwatch.domain.adapter.QuartzJMXAdapter;
import org.jwatch.domain.adapter.QuartzJMXAdapterFactory;
import org.jwatch.domain.connection.QuartzConnectService;
import org.jwatch.domain.connection.QuartzConnectServiceImpl;
import org.jwatch.domain.instance.QuartzInstanceConnection;
import org.jwatch.domain.instance.QuartzInstanceConnectionService;
import org.jwatch.listener.settings.QuartzConfig;
import org.jwatch.util.GlobalConstants;
import org.jwatch.util.JSONUtil;
import org.jwatch.util.SettingsUtil;
import org.jwatch.util.Tools;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * All interactions with QuartzInstance objects are handled through this class.
 *
 * @author <a href="mailto:royrusso@gmail.com">Roy Russo</a>
 *         Date: Apr 8, 2011 4:31:24 PM
 */
public class QuartzInstanceHandler
{
   static Logger log = Logger.getLogger(QuartzInstanceHandler.class);

   /**
    * Returns instances found. For now, it pulls from the config file every time.
    *
    * @return
    * @see org.jwatch.domain.instance.QuartzInstanceConnectionService
    */
   public static JSONObject loadInstances()
   {
      JSONObject jsonObject = new JSONObject();
      try
      {
/*         Map qMap = QuartzInstanceConnectionService.getQuartzInstanceMap();
         if (qMap == null)// if the map is empty, try the config file.
         {*/
         QuartzInstanceConnectionService.initQuartzInstanceMap();
         Map qMap = QuartzInstanceConnectionService.getQuartzInstanceMap();
/*         }*/

         if (qMap != null)
         {
            JSONArray jsonArray = new JSONArray();
            for (Iterator it = qMap.entrySet().iterator(); it.hasNext();)
            {
               Map.Entry entry = (Map.Entry) it.next();
               String k = (String) entry.getKey();
               QuartzInstanceConnection quartzInstanceConnection = (QuartzInstanceConnection) qMap.get(k);
               QuartzConfig quartzConfig = new QuartzConfig(quartzInstanceConnection);
               JSONObject jo = JSONObject.fromObject(quartzConfig);
               jsonArray.add(jo);
            }
            jsonObject.put(GlobalConstants.JSON_DATA_ROOT_KEY, jsonArray);
            jsonObject.put(GlobalConstants.JSON_TOTAL_COUNT, qMap.size());
         }
         jsonObject.put(GlobalConstants.JSON_SUCCESS_KEY, true);
      }
      catch (Throwable t)
      {
         jsonObject.put(GlobalConstants.JSON_SUCCESS_KEY, false);
      }
      return jsonObject;
   }


   /**
    * Given JMX connection settings: this will connect to the instance, and if successful
    * persist the new instance in the settings file and memory map.
    *
    * @param map
    * @return success/failure
    */
   public static JSONObject createInstance(Map map)
   {
      JSONObject jsonObject = new JSONObject();

      try
      {
         String host = StringUtils.trimToNull((String) map.get("host"));
         int port = Integer.valueOf(StringUtils.trimToNull((String) map.get("port")));
         String username = StringUtils.trimToNull((String) map.get("userName"));
         String password = StringUtils.trimToNull((String) map.get("password"));

         if (StringUtils.trimToNull(host) != null)
         {
            QuartzConfig quartzConfig = new QuartzConfig(Tools.generateUUID(), host, port, username, password);
            QuartzConnectService quartzConnectService = new QuartzConnectServiceImpl();
            List<QuartzInstanceConnection> quartzInstanceConnection = quartzConnectService.initInstance(quartzConfig);
            if (quartzInstanceConnection == null)
            {
               log.error(GlobalConstants.MESSAGE_FAILED_CONNECT + " " + quartzConfig);
               jsonObject = JSONUtil.buildError(GlobalConstants.MESSAGE_FAILED_CONNECT + " " + quartzConfig);
               return jsonObject;
            }

            // persist
            if (quartzInstanceConnection != null && quartzInstanceConnection.size() > 0)
            {
               for (int i = 0; i < quartzInstanceConnection.size(); i++)
               {
                  QuartzInstanceConnection instanceConnection = quartzInstanceConnection.get(i);
                  QuartzInstanceConnectionService.putQuartzInstance(instanceConnection);
               }
               SettingsUtil.saveConfig(quartzConfig);
            }
            jsonObject.put(GlobalConstants.JSON_DATA_ROOT_KEY, quartzConfig);
            jsonObject.put(GlobalConstants.JSON_SUCCESS_KEY, true);
         }
         else
         {
            jsonObject.put(GlobalConstants.JSON_MESSAGE, GlobalConstants.MESSAGE_CONFIG_EMPTY);
            jsonObject.put(GlobalConstants.JSON_SUCCESS_KEY, false);
         }
      }
      catch (UnknownHostException e)
      {
         log.error(e);
         jsonObject = JSONUtil.buildError("Unknown Host. " + GlobalConstants.MESSAGE_ERR_CHECK_LOG);
      }
      catch (Throwable t)
      {
         log.error(t);
         jsonObject = JSONUtil.buildError(GlobalConstants.MESSAGE_ERR_CHECK_LOG);
      }
      return jsonObject;
   }

   public static JSONObject getInstanceDetails(Map map)
   {
      JSONObject jsonObject = new JSONObject();
      String qiid = StringUtils.trimToNull((String) map.get("uuid"));
      try
      {
         QuartzInstanceConnection quartzInstanceConnection = QuartzInstanceConnectionService.getQuartzInstanceByID(qiid);
         if (quartzInstanceConnection != null)
         {
            QuartzJMXAdapter jmxAdapter = QuartzJMXAdapterFactory.initQuartzJMXAdapter(quartzInstanceConnection.getObjectName(), quartzInstanceConnection.getMBeanServerConnection());
            //jmxAdapter.
         }
      }
      catch (Throwable t)
      {
         log.error(t);
         jsonObject = JSONUtil.buildError(GlobalConstants.MESSAGE_ERR_CHECK_LOG);
      }
      return jsonObject;
   }
}
