package msteamsnotifications.teamcity.extension;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import msteamsnotifications.MsTeamsNotification;
import msteamsnotifications.teamcity.BuildState;
import msteamsnotifications.teamcity.BuildStateEnum;
import msteamsnotifications.teamcity.TeamCityIdResolver;
import msteamsnotifications.teamcity.extension.bean.ProjectMsTeamsNotificationsBean;
import msteamsnotifications.teamcity.extension.bean.ProjectMsTeamsNotificationsBeanJsonSerialiser;
import msteamsnotifications.teamcity.payload.MsTeamsNotificationPayloadManager;
import msteamsnotifications.teamcity.settings.MsTeamsNotificationContentConfig;
import msteamsnotifications.teamcity.settings.MsTeamsNotificationMainSettings;
import msteamsnotifications.teamcity.settings.MsTeamsNotificationProjectSettings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class MsTeamsNotificationAjaxEditPageController extends BaseController {

	    private static final String PROJECT_ID = "projectId";
	    private static final String SLACK_NOTIFICATION = "msteamsNotifications";
	    private static final String SUBMIT_ACTION = "submitAction";
	    private static final String ERRORS_TAG = "<errors />";
	    private static final String MESSAGES = "messages";
	    private static final String SLACK_NOTIFICATION_ID = "msTeamsNotificationId";
	    private static final String MAX_COMMITS_DISPLAY = "maxCommitsToDisplay";
	    private static final String BOT_NAME = "botName";
	    private static final String ICON_URL = "iconUrl";
	    private static final String FALSE = "false";
		protected static final String BEFORE_FINISHED = "BeforeFinished";
		protected static final String BUILD_INTERRUPTED = "BuildInterrupted";
		protected static final String BUILD_STARTED = "BuildStarted";
		protected static final String BUILD_BROKEN = "BuildBroken";
		protected static final String BUILD_FIXED = "BuildFixed";
		protected static final String BUILD_FAILED = "BuildFailed";
		protected static final String BUILD_SUCCESSFUL = "BuildSuccessful";
		
		
		private final WebControllerManager myWebManager;
    private final MsTeamsNotificationMainSettings myMainSettings;
    private SBuildServer myServer;
	    private ProjectSettingsManager mySettings;
	    private final String myPluginPath;
	    private final MsTeamsNotificationPayloadManager myManager;

	public MsTeamsNotificationAjaxEditPageController(SBuildServer server, WebControllerManager webManager,
                                                       ProjectSettingsManager settings, MsTeamsNotificationProjectSettings whSettings, MsTeamsNotificationPayloadManager manager,
                                                       PluginDescriptor pluginDescriptor, MsTeamsNotificationMainSettings mainSettings) {
	        super(server);
	        myWebManager = webManager;
	        myServer = server;
	        mySettings = settings;
	        myPluginPath = pluginDescriptor.getPluginResourcesPath();
	        myManager = manager;
            myMainSettings = mainSettings;
	    }

	    public void register(){
	      myWebManager.registerController("/msteamsnotifications/ajaxEdit.html", this);
	    }
	    
	    protected static void checkAndAddBuildState(HttpServletRequest r, BuildState state, BuildStateEnum myBuildState, String varName){
	    	if ((r.getParameter(varName) != null)
	    		&& ("on".equalsIgnoreCase(r.getParameter(varName)))){
	    		state.enable(myBuildState);
	    	} else {
	    		state.disable(myBuildState);
	    	}
	    }
	    
	    protected static void checkAndAddBuildStateIfEitherSet(HttpServletRequest r, BuildState state, BuildStateEnum myBuildState, String varName, String otherVarName){
	    	if ((r.getParameter(varName) != null)
	    			&& ("on".equalsIgnoreCase(r.getParameter(varName)))){
	    		state.enable(myBuildState);
	    	} else if ((r.getParameter(otherVarName) != null)
	    			&& ("on".equalsIgnoreCase(r.getParameter(otherVarName)))){
		    	state.enable(myBuildState);
	    	} else {
	    		state.disable(myBuildState);;
	    	}
	    }

	    @Nullable
		@Override
	    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
	    	
	        HashMap<String,Object> params = new HashMap<String,Object>();
	        
	        SUser myUser = SessionUser.getUser(request);
	        SProject myProject;
	        MsTeamsNotificationProjectSettings projSettings = null;

	    	if ("post".equalsIgnoreCase(request.getMethod())){
	    		if ((request.getParameter(PROJECT_ID) != null)
	    			&& request.getParameter(PROJECT_ID).startsWith("project")){
	    		    	projSettings = (MsTeamsNotificationProjectSettings) mySettings.getSettings(request.getParameter(PROJECT_ID), SLACK_NOTIFICATION);
	    		    	myProject = this.myServer.getProjectManager().findProjectById(request.getParameter(PROJECT_ID));

			    		if ((projSettings != null) && (myProject != null)
			    				&& (myUser.isPermissionGrantedForProject(myProject.getProjectId(), Permission.EDIT_PROJECT))){
			    			if ((request.getParameter(SUBMIT_ACTION) != null ) 
			    				&& ("removeMsTeamsNotification".equals(request.getParameter(SUBMIT_ACTION)))
			    				&& (request.getParameter("removedMsTeamsNotificationId") != null)){
			    					projSettings.deleteMsTeamsNotification(request.getParameter("removedMsTeamsNotificationId"), myProject.getProjectId());
			    					if(projSettings.updateSuccessful()){
			    						myProject.persist();
			    						params.put(MESSAGES, ERRORS_TAG);
			    					} else {
			    						params.put(MESSAGES, "<errors><error id=\"messageArea\">The msteamsnotifications was not found. Have the MsTeamsNotifications been edited on disk or by another user?</error></errors>");
			    					}
			    					
			    			} else if ((request.getParameter(SUBMIT_ACTION) != null ) 
				    				&& ("updateMsTeamsNotification".equals(request.getParameter(SUBMIT_ACTION)))){
                                if (request.getParameter(SLACK_NOTIFICATION_ID) != null){
                                    Boolean enabled = false;
                                    Boolean mentionChannelEnabled = false;
                                    Boolean mentionMsTeamsUserEnabled = false;
                                    Boolean buildTypeAll = false;
                                    Boolean buildTypeSubProjects = false;
                                    MsTeamsNotificationContentConfig content = new MsTeamsNotificationContentConfig();
                                    Set<String> buildTypes = new HashSet<String>();
                                    if ((request.getParameter("msTeamsNotificationsEnabled") != null )
                                            && ("on".equalsIgnoreCase(request.getParameter("msTeamsNotificationsEnabled")))){
                                        enabled = true;
                                    }
                                    if ((request.getParameter("mentionChannelEnabled") != null )
                                            && ("on".equalsIgnoreCase(request.getParameter("mentionChannelEnabled")))){

                                        mentionChannelEnabled = true;
                                    }
                                    if ((request.getParameter("mentionMsTeamsUserEnabled") != null )
                                            && ("on".equalsIgnoreCase(request.getParameter("mentionMsTeamsUserEnabled")))){
                                        mentionMsTeamsUserEnabled = true;
                                    }

                                    content.setEnabled((request.getParameter("customContentEnabled") != null )
                                            && ("on".equalsIgnoreCase(request.getParameter("customContentEnabled"))));

                                    if (content.isEnabled()){

                                        if ((request.getParameter(MAX_COMMITS_DISPLAY) != null )
                                                && (request.getParameter(MAX_COMMITS_DISPLAY).length() > 0)){
                                            content.setMaxCommitsToDisplay(convertToInt(request.getParameter(MAX_COMMITS_DISPLAY), MsTeamsNotificationContentConfig.DEFAULT_MAX_COMMITS));
                                        }

                                        content.setShowBuildAgent((request.getParameter("showBuildAgent") != null )
                                                && ("on".equals(request.getParameter("showBuildAgent"))));

                                        content.setShowCommits((request.getParameter("showCommits") != null )
                                                && ("on".equalsIgnoreCase(request.getParameter("showCommits"))));

                                        content.setShowCommitters((request.getParameter("showCommitters") != null)
                                                && ("on".equalsIgnoreCase(request.getParameter("showCommitters"))));

                                        content.setShowElapsedBuildTime((request.getParameter("showElapsedBuildTime") != null)
                                                && ("on".equalsIgnoreCase(request.getParameter("showElapsedBuildTime"))));

                                        content.setShowFailureReason((request.getParameter("showFailureReason") != null)
                                                && ("on".equalsIgnoreCase(request.getParameter("showFailureReason"))));

                                        if ((request.getParameter(BOT_NAME) != null )
                                                && (request.getParameter(BOT_NAME).length() > 0)){
                                            content.setBotName(request.getParameter(BOT_NAME));
                                        }

                                        if ((request.getParameter(ICON_URL) != null )
                                                && (request.getParameter(ICON_URL).length() > 0)){
                                            content.setIconUrl(request.getParameter(ICON_URL));
                                        }
                                    }

                                    BuildState states = new BuildState();

                                    checkAndAddBuildState(request, states, BuildStateEnum.BUILD_SUCCESSFUL, BUILD_SUCCESSFUL);
                                    checkAndAddBuildState(request, states, BuildStateEnum.BUILD_FAILED, BUILD_FAILED);
                                    checkAndAddBuildState(request, states, BuildStateEnum.BUILD_FIXED, BUILD_FIXED);
                                    checkAndAddBuildState(request, states, BuildStateEnum.BUILD_BROKEN, BUILD_BROKEN);
                                    checkAndAddBuildState(request, states, BuildStateEnum.BUILD_STARTED, BUILD_STARTED);
                                    checkAndAddBuildState(request, states, BuildStateEnum.BUILD_INTERRUPTED, BUILD_INTERRUPTED);
                                    checkAndAddBuildState(request, states, BuildStateEnum.BEFORE_BUILD_FINISHED, BEFORE_FINISHED);
                                    checkAndAddBuildStateIfEitherSet(request, states, BuildStateEnum.BUILD_FINISHED, BUILD_SUCCESSFUL, BUILD_FAILED);
                                    checkAndAddBuildState(request, states, BuildStateEnum.RESPONSIBILITY_CHANGED, "ResponsibilityChanged");

                                    if ((request.getParameter("buildTypeSubProjects") != null ) && ("on".equalsIgnoreCase(request.getParameter("buildTypeSubProjects")))){
                                        buildTypeSubProjects = true;
                                    }
                                    if ((request.getParameter("buildTypeAll") != null ) && ("on".equalsIgnoreCase(request.getParameter("buildTypeAll")))){
                                        buildTypeAll = true;
                                    } else {
                                        if (request.getParameterValues("buildTypeId") != null){
                                            String[] types = request.getParameterValues("buildTypeId");
                                            for (String string : types) {
                                                buildTypes.add(string);
                                            }
                                        }
                                    }

                                    if ("new".equals(request.getParameter(SLACK_NOTIFICATION_ID))){
                                        projSettings.addNewMsTeamsNotification(myProject.getProjectId(), request.getParameter("token"), enabled,
                                                states, buildTypeAll, buildTypeSubProjects, buildTypes, mentionChannelEnabled, mentionMsTeamsUserEnabled);
                                        if(projSettings.updateSuccessful()){
                                            myProject.persist();
                                            params.put(MESSAGES, ERRORS_TAG);
                                        } else {
                                            params.put("message", "<errors><error id=\"\">" + projSettings.getUpdateMessage() + "</error>");
                                        }
                                    } else {
                                        projSettings.updateMsTeamsNotification(myProject.getProjectId(), request.getParameter("token"),
                                                request.getParameter(SLACK_NOTIFICATION_ID), enabled,
                                                states, buildTypeAll, buildTypeSubProjects, buildTypes, mentionChannelEnabled,
                                                mentionMsTeamsUserEnabled, content);
                                        if(projSettings.updateSuccessful()){
                                            myProject.persist();
                                            params.put(MESSAGES, ERRORS_TAG);
                                        } else {
                                            params.put("message", "<errors><error id=\"\">" + projSettings.getUpdateMessage() + "</error>");
                                        }
                                    }
                                } // TODO Need to handle msTeamsNotificationId being null
			    			}
			    		} else {
			    			params.put(MESSAGES, "<errors><error id=\"messageArea\">You do not appear to have permission to edit MsTeamsNotifications.</error></errors>");
			    		}
	    		}
	    	}

	    	if ("get".equalsIgnoreCase(request.getMethod())
	        		&& request.getParameter(PROJECT_ID) != null 
	        		&& request.getParameter(PROJECT_ID).startsWith("project")){
	        	
		    	MsTeamsNotificationProjectSettings projSettings1 = (MsTeamsNotificationProjectSettings) mySettings.getSettings(request.getParameter(PROJECT_ID), SLACK_NOTIFICATION);
		    	SProject project = this.myServer.getProjectManager().findProjectById(request.getParameter(PROJECT_ID));
		    	
		    	String message = projSettings1.getMsTeamsNotificationsAsString();
		    	
		    	params.put("haveProject", "true");
		    	params.put(MESSAGES, message);
		    	params.put(PROJECT_ID, project.getProjectId());
		    	params.put("projectExternalId", TeamCityIdResolver.getExternalProjectId(project));
		    	params.put("projectName", project.getName());
		    	
		    	params.put("msteamsNotificationCount", projSettings1.getMsTeamsNotificationsCount());
		    	if (projSettings1.getMsTeamsNotificationsCount() == 0){
		    		params.put("noMsTeamsNotifications", "true");
		    		params.put(SLACK_NOTIFICATION, FALSE);
		    	} else {
		    		params.put("noMsTeamsNotifications", FALSE);
		    		params.put(SLACK_NOTIFICATION, "true");
		    		params.put("msTeamsNotificationList", projSettings.getMsTeamsNotificationsAsList());
		    		params.put("msteamsNotificationsDisabled", !projSettings.isEnabled());
		    		params.put("msteamsNotificationsEnabledAsChecked", projSettings.isEnabledAsChecked());
		    		params.put("projectMsTeamsNotificationsAsJson", ProjectMsTeamsNotificationsBeanJsonSerialiser.serialise(ProjectMsTeamsNotificationsBean.build(projSettings, project, myMainSettings)));
		    	}
	        } else {
	        	params.put("haveProject", FALSE);
	        }
	        
	        return new ModelAndView(myPluginPath + "MsTeamsNotification/ajaxEdit.jsp", params);
	    }


    private int convertToInt(String s, int defaultValue){
        try{
            int myInt = Integer.parseInt(s);
            return myInt;
        } catch (NumberFormatException e){
            return defaultValue;
        }
    }
}