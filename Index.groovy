import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import org.apache.commons.lang3.StringEscapeUtils

import org.bonitasoft.engine.identity.User;

import org.bonitasoft.web.extension.page.PageContext;
import org.bonitasoft.web.extension.page.PageController;
import org.bonitasoft.web.extension.page.PageResourceProvider;


import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProfileAPI;

import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;

import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
	
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

import org.bonitasoft.store.BonitaStoreAccessor;

import org.bonitasoft.forklift.ForkliftAPI;
import org.bonitasoft.forklift.ForkliftAPI.ConfigurationSet;
import org.bonitasoft.forklift.ForkliftAPI.ResultSynchronization;


public class Index implements PageController {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft");
		long timeBegin= System.currentTimeMillis();
		
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			
			String action=request.getParameter("action");
			logger.info("###################################### action is["+action+"] !");
			if (action==null || action.length()==0 )
			{
				logger.severe("###################################### RUN Default !");
				
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			
			String paramJsonEncode= request.getParameter("paramjson");
            String paramJsonSt = (paramJsonEncode==null ? null : java.net.URLDecoder.decode(paramJsonEncode, "UTF-8"));

            
			APISession apiSession = pageContext.getApiSession()
			BonitaStoreAccessor bonitaAccessor= new BonitaStoreAccessor( apiSession );
            
			ForkliftAPI forkliftAPI = ForkliftAPI.getInstance();
             
			List<BEvent> listEvents=new ArrayList<BEvent>();
            
			HashMap<String,Object> answer = null;

			
			 if ("synchronisationdetect".equals(action))
	        {
	          	answer = new HashMap<String,Object>()
	          	ConfigurationSet configurationSet= forkliftAPI.loadConfiguration("default", pageResourceProvider.getPageName(), apiSession.getTenantId());
	          	
	          	ResultSynchronization resultSynchronization = forkliftAPI.detect( configurationSet , bonitaAccessor );
               	listEvents.addAll(resultSynchronization.listEvents);
               	answer.put("detection", resultSynchronization.toJsonObject());
	        }
			else if ("synchronisationexecute".equals(action))
	        {
	          	answer = new HashMap<String,Object>()
	          	ConfigurationSet configurationSet= forkliftAPI.loadConfiguration("default", pageResourceProvider.getPageName(), apiSession.getTenantId());
	            	
	          	configurationSet.setActions( paramJsonSt );
	          	
	          	ResultSynchronization resultSynchronization = forkliftAPI.synchronize( configurationSet , bonitaAccessor );
               	listEvents.addAll(resultSynchronization.listEvents);
               	answer.put("report", resultSynchronization.report);
               	answer.put("detection", resultSynchronization.toJsonObject());
	        }
			else if ("synchronisation".equals(action))
	        {
	          	answer = new HashMap<String,Object>()
	          	ConfigurationSet configurationSet= forkliftAPI.loadConfiguration("default", pageResourceProvider.getPageName(), apiSession.getTenantId());

	          	ResultSynchronization resultSynchronization = forkliftAPI.synchronize( configurationSet , bonitaAccessor );
               	listEvents.addAll(resultSynchronization.listEvents);
               	answer.put("report", resultSynchronization.report);
	        }
			// ---------- config
            else if ("loadConfiguration".equals(action))
            {
            	answer = new HashMap<String,Object>()
        	    // final Object jsonObject = JSONValue.parse(paramJsonSt);
            	
            	ConfigurationSet configurationSet= forkliftAPI.loadConfiguration("default", pageResourceProvider.getPageName(), apiSession.getTenantId());
            	answer.putAll(configurationSet.toJsonObject() );
            	
               	listEvents.addAll(configurationSet.listEvents);
            }
            else if ("saveConfiguration".equals(action))
            {
            	answer = new HashMap<String,Object>();
				
                // get the name
                final Object jsonObject = JSONValue.parse(paramJsonSt);
                Map jsonObjectMap = (Map) jsonObject;
                ConfigurationSet configurationSet =new ConfigurationSet();
                configurationSet.fromPage( jsonObjectMap );
                listEvents= forkliftAPI.saveConfiguration( "default", configurationSet,  pageResourceProvider.getPageName(), apiSession.getTenantId());
            }
			
			
			// ------------------ end
			if (answer==null)
			{
				answer = new HashMap<String,Object> ();
				answer.put("status", "Unknow command");
			}
			if (! answer.containsKey("listevents"))
				answer.put("listevents",BEventFactory.getHtml(listEvents));
            
			String jsonDetailsSt = JSONValue.toJSONString( answer );
			long timeEnd= System.currentTimeMillis();
			logger.info("###################################### EndMeteor ["+action+"] Return["+jsonDetailsSt+"] in "+(timeEnd-timeBegin)+" ms");
			
			PrintWriter out = response.getWriter()

			out.write( jsonDetailsSt );
			out.flush();
			out.close();				
			return;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
		}
	}

	
	/** -------------------------------------------------------------------------
	 *
	 *runTheBonitaIndexDoGet
	 * 
	 */
	private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
				try {
						def String indexContent;
						pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
								indexContent = s.getText()
						}
						
						def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
					  File pageDirectory = pageResourceProvider.getPageDirectory();
            
		        // def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
		        // indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
		        // indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
		        indexContent= indexContent.replace("@_CURRENTTIMEMILIS_@", String.valueOf(System.currentTimeMillis()));
		        indexContent= indexContent.replace("@_PAGEDIRECTORY_@", pageDirectory.getAbsolutePath()) ;
		       
		              
						response.setCharacterEncoding("UTF-8");
						PrintWriter out = response.getWriter();
						out.print(indexContent);
						out.flush();
						out.close();
				} catch (Exception e) {
						e.printStackTrace();
				}
		}
		
		/**
		to create a simple chart
		*/
		public static class ActivityTimeLine
		{
				public String activityName;
				public Date dateBegin;
				public Date dateEnd;
				
				public static ActivityTimeLine getActivityTimeLine(String activityName, int timeBegin, int timeEnd)
				{
					Calendar calBegin = Calendar.getInstance();
					calBegin.set(Calendar.HOUR_OF_DAY , timeBegin);
					Calendar calEnd = Calendar.getInstance();
					calEnd.set(Calendar.HOUR_OF_DAY , timeEnd);
					
						ActivityTimeLine oneSample = new ActivityTimeLine();
						oneSample.activityName = activityName;
						oneSample.dateBegin		= calBegin.getTime();
						oneSample.dateEnd 		= calEnd.getTime();
						
						return oneSample;
				}
				public long getDateLong()
				{ return dateBegin == null ? 0 : dateBegin.getTime(); }
		}
		
		
		/** create a simple chart 
		*/
		public static String getChartTimeLine(String title, List<ActivityTimeLine> listSamples){
				Logger logger = Logger.getLogger("org.bonitasoft");
				
				/** structure 
				 * "rows": [
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },
           {
        		 c: [
        		      { "v": "January" },"
                  { "v": 19,"f": "42 items" },
                  { "v": 12,"f": "Ony 12 items" },
                ]
           },

				 */
				String resultValue="";
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");
				
				for (int i=0;i<listSamples.size();i++)
				{
					logger.info("sample [i] : "+listSamples.get( i ).activityName+"] dateBegin["+simpleDateFormat.format( listSamples.get( i ).dateBegin)+"] dateEnd["+simpleDateFormat.format( listSamples.get( i ).dateEnd) +"]");
						if (listSamples.get( i ).dateBegin!=null &&  listSamples.get( i ).dateEnd != null)
								resultValue+= "{ \"c\": [ { \"v\": \""+listSamples.get( i ).activityName+"\" }," ;
								resultValue+= " { \"v\": \""+listSamples.get( i ).activityName +"\" }, " ;
								resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateBegin) +")\" }, " ;
								resultValue+= " { \"v\": \"Date("+ simpleDateFormat.format( listSamples.get( i ).dateEnd) +")\" } " ;
								resultValue+= "] },";
				}
				if (resultValue.length()>0)
						resultValue = resultValue.substring(0,resultValue.length()-1);
				
				String resultLabel = "{ \"type\": \"string\", \"id\": \"Role\" },{ \"type\": \"string\", \"id\": \"Name\"},{ \"type\": \"datetime\", \"id\": \"Start\"},{ \"type\": \"datetime\", \"id\": \"End\"}";
				
				String valueChart = "	{"
					   valueChart += "\"type\": \"Timeline\", ";
					  valueChart += "\"displayed\": true, ";
					  valueChart += "\"data\": {";
					  valueChart +=   "\"cols\": ["+resultLabel+"], ";
					  valueChart +=   "\"rows\": ["+resultValue+"] ";
					  /*
					  +   "\"options\": { "
					  +         "\"bars\": \"horizontal\","
					  +         "\"title\": \""+title+"\", \"fill\": 20, \"displayExactValues\": true,"
					  +         "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
					  */
					  valueChart +=  "}";
					  valueChart +="}";
// 				+"\"isStacked\": \"true\","
 	          
//		    +"\"displayExactValues\": true,"
//		    
//		    +"\"hAxis\": { \"title\": \"Date\" }"
//		    +"},"
				logger.info("Value1 >"+valueChart+"<");

				
				return valueChart;		
		}	
}
