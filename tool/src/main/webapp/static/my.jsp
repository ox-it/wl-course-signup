<%@ page import="org.sakaiproject.component.cover.ServerConfigurationService" %>
<%@ page import="org.sakaiproject.user.cover.UserDirectoryService" %>
<%@ page session="false" %>
<%
if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
	String redirectURL = "login.jsp";
    response.sendRedirect(redirectURL);
}
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<title>Module Signup</title>

	<link href="<%= ServerConfigurationService.getString("skin.repo", "/library/skin") %>/tool_base.css" type="text/css" rel="stylesheet" media="all" />
	<link href="<%= ServerConfigurationService.getString("skin.repo", "/library/skin") %>/<%= ServerConfigurationService.getString("skin.default", "default") %>/tool.css" type="text/css" rel="stylesheet" media="all" />

	<link rel="stylesheet" type="text/css" href="lib/jqmodal-r14/jqModal.css" />
	<link rel="stylesheet" type="text/css" href="lib/dataTables-1.7/css/demo_table_jui.css"/>
	<link rel="stylesheet" type="text/css" href="lib/jquery-ui-1.8.4.custom/css/smoothness/jquery-ui-1.8.4.custom.css"/>
	<link rel="stylesheet" type="text/css" href="lib/tool.css" />
	<link rel="stylesheet" type="text/css" href="lib/jquery.tooltip.css" />
	
	<script type="text/javascript" src="lib/jquery/jquery-1.4.2.min.js"></script>
	<script type="text/javascript" src="lib/jstree-1.0rc/_lib/jquery.cookie.js"></script>
	<script type="text/javascript" src="lib/jstree-1.0rc/jquery.jstree.js"></script>
	<script type="text/javascript" src="lib/jqmodal-r14/jqModal.js"></script>
	<script type="text/javascript" src="lib/trimpath-template-1.0.38/trimpath-template.js"></script>
	<script type="text/javascript" src="lib/dataTables-1.7/js/jquery.dataTables.js"></script>
	<script type="text/javascript" src="lib/dataTables.reloadAjax.js"></script>
	<script type="text/javascript" src="lib/signup.js"></script>
	<script type="text/javascript" src="lib/Text.js"></script>
	<script type="text/javascript" src="lib/serverDate.js"></script>
	<script type="text/javascript" src="lib/jquery.tooltip.js"></script>

	<script type="text/javascript">
	$(function() {
		
		// The site to load the static files from.
		var signupSiteId = "/access/content/group/<%= ServerConfigurationService.getString("course-signup.site-id", "course-signup") %>";
		
		/**
		* This loads details about a node in the tree.
	 	* This basically loads a HTML files and shows the user.
	 	* @param {Object} id
	 	*/
		$.ajax( {
			"url": signupSiteId + "/my_modules.html",
			"cache": false,
			"success": function(data){
				// This is because we now top and tail files in Sakai.
				data = data.replace(/^(.|\n)*<body[^>]*>/im, "");
				data = data.replace(/<\/body[^>]*>(.|\n)*$/im, "");
				$("#notes").html(data);
			}
		});
	
				
		$("#signups").html('<table border="0" class="display" id="signups-table"></table>');
		var signups = $("#signups-table").signupTable("../rest/signup/my", false);
		Signup.util.autoresize();
	});
	</script>

</head>
<body>
<div id="toolbar">
<ul class="navIntraTool actionToolBar">
	<li><span><a href="home.jsp">Home</a></span></li>
	<li><span><a href="search.jsp">Search Modules</a></span></li>
	<li><span><a href="index.jsp">Browse by Department</a></span></li>  
	<li><span><a href="calendar.jsp">Browse by Calendar</a></span></li>
	<li><span>My Modules</span></li>
	<li><span><a href="pending.jsp">Pending Acceptances</a></span></li>
	<li><span><a href="approve.jsp">Pending Confirmations</a></span></li>
	<li><span><a href="admin.jsp">Module Administration</a></span></li>
</ul>
</div>

<div id="notes">
            <!-- Show the contents of my_modules.html -->
</div>
		
<div id="signups"><!-- Browse the areas which there are courses -->
</div>

</body>
</html>