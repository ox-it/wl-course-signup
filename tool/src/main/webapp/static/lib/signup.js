var Signup = function(){
    return {
        "util": {
            /**
             * Formats a durations to display to the user.
             * @param {Object} duration Duration in milliseconds.
             */
            "formatDuration": function(remaining){
                if (remaining < 1000) {
                    return "< 1 second";
                }
                else 
                    if (remaining < 60000) {
                        return Math.floor(remaining / 1000) + " seconds";
                    }
                    else 
                        if (remaining < 3600000) {
                            return Math.floor(remaining / 60000) + " minutes";
                        }
                        else 
                            if (remaining < 86400000) {
                                return Math.floor(remaining / 3600000) + " hours";
                            }
                            else {
                                return Math.floor(remaining / 86400000) + " days";
                            }
            }
        },
		"signup": {
			"getActions": function(status, id, admin) {
				if (admin) {
					switch (status) {
						case "PENDING":
							return [{
								"name": "Accept",
								"url": "/course-signup/rest/signup/" + id + "/accept"
							}, {
								"name": "Reject",
								"url": "/course-signup/rest/signup/" + id + "/reject"
							}];
						case "ACCEPTED":
							return [{
								"name": "Approve",
								"url": "/course-signup/rest/signup/" + id + "/approve"
							}, {
								"name": "Reject",
								"url": "/course-signup/rest/signup/" + id + "/reject"
							}];
						case "APPROVED":
							return [];
						case "REJECTED":
							return [];
						case "WITHDRAWN":
							return [];
					}
				} else {
					switch (status) {
						case "PENDING": 
							return [{
								"name": "Withdraw",
								"url": "/course-signup/rest/signup/"+id+"/withdraw"
							}];
					}
				}
				return [];
			},
			"formatActions": function(actions) {
				return $.map(actions, function(action) {
					return '<a class="action" href="'+ action.url+ '">'+ action.name+ '</a>';
				}).join(" / ");
			},
			/**
			 * Formats a notes string so we only display the first bit of it and then display a tooltip for the rest.
			 * @param {Object} notes
			 */
			"formatNotes": function(notes) {
				if (notes && notes.length> 50) {
					return '<span class="signup-notes">'+ Text.toHtml(notes.substr(0, 45))+'... <span class="more">[more]<span class="full">'+ Text.toHtml(notes)+ '</span></span></span>'
				} else {
					return Text.toHtml(notes);
				}
			}
		},
		"user": {
			"render": function(user){
				var details = "";
				if (user) {
					details += '<a href="mailto:' + user.email + '">' + user.name + '</a>';
					if (user.units && user.units.length > 0) {
						details += '<br>' + user.units.join(" / ");
					}
				}
				return details;
			}
		}
    };
}();

/**
 * jQuery plugin to make a signup table.
 */
(function($){
    	
    $.fn.signupTable = function(url, isAdmin){
		var element = this;
		var table = this.dataTable( {
					"bJQueryUI": true,
					"sPaginationType": "full_numbers",
					"bProcessing": true,
					"sAjaxSource": url,
					"bAutoWidth": false,
					"aaSorting": [[1, "desc"]],
					"aoColumns": [
						{
							"sTitle": "",
							"bSortable": false,
							"fnRender": function(aObj) {
								return '<input type="checkbox" value="'+ aObj.aData[0]+'">';
							}
						},
						{
							"sTitle": "Created",
							"fnRender": function(aObj){
								if (aObj.aData[1]) {
									return Signup.util.formatDuration($.serverDate() - aObj.aData[1]) + " ago";
								} else {
									return "unknown";
								} 
							},
							"bUseRendered": false				
						},
						{"sTitle": "Student"},
						{"sTitle": "Component"},
						{"sTitle": "Supervisor"},
						{"sTitle": "Notes", "sWidth": "20%", "sClass": "signup-notes"},
						{"sTitle": "Status"},
						{"sTitle": "Actions"}
					],
					"fnServerData": function( sSource, aoData, fnCallback ) {
						jQuery.ajax( {
							dataType: "json",
							type: "GET",
							url: sSource,
							success: function(result) {
								var data = [];
								$.each(result, function(){
									var course = ['<span class="course-group">'+this.group.title+"</span>"].concat(
										$.map(this.components.concat(), function(component){
											return '<span class="course-component">'+ component.title+ " "+ component.slot + " in "+ component.when+ ' ('+ component.places+ '&nbsp;places)</span>';
										})).join("<br>");
									var actions = Signup.signup.formatActions(Signup.signup.getActions(this.status, this.id, isAdmin)); 
									data.push([
										this.id,
										(this.created)?this.created:"",
										Signup.user.render(this.user),
										course,
										Signup.user.render(this.supervisor),
										Signup.signup.formatNotes(this.notes),
										this.status,
										actions						
									]);
								});
								fnCallback({
									"aaData": data 
								});
							}
						})
					}
				} );
			$("a.action", this).live("click", function(e) {
					var url = $(this).attr("href");
					$.ajax({
						"url": url,
						"type": "POST",
						"success": function(data) {
							console.log(data);
							element.dataTable().fnReloadAjax();
						}
					});
					return false;
				});
		return table;
        
    };
})(jQuery);

// Stop browsers without console.log from crashing.
var console = console || {"log":function(){}};


