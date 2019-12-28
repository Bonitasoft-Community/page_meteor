/**
 * 
 */

(function() {


var appCommand = angular.module('meteormonitor', ['googlechart', 'ui.bootstrap', 'ngSanitize','ngModal','angularFileUpload']);






// --------------------------------------------------------------------------
//
// Controler TitleControler
//
// --------------------------------------------------------------------------
	
appCommand.controller('TitleController', 
	function () {
	this.isshowhistory = false;
	this.showhistory = function( show )
	{
	   this.isshowhistory = show;
	}		
});
// --------------------------------------------------------------------------
//
// Controler Meteor
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('MeteorControler',
	function ( $http, $scope,  $sce, $interval, $timeout, $upload ) {

	this.processes= [  ];
	this.status="";
	this.statusrobot=" status robot";
	this.statusexecution ="t";
	this.statuslistrobots = [];
	this.wait=false;
	this.wait=false;
	this.showprocess=true;
	this.showscenarii=true;
	this.showexperience = true;
	this.isshowExportDialog=false;
	
	this.showmainscenarii=true;
	
	this.testChart = {
			'type': 'ColumnChart',
			'displayed': true,
			'data': {
				'cols': ['cover','happy'],
				'rows' : [ 12,43]
			}
	};
	
	
	// ------------------------------------------------------------------------------------------------------
	// Style configuration

	this.getRobotStyle = function ( robotinfo ) {
		if (robotinfo.nbErrors > 0 )
			return "background-color:#f2dede;";
		if (robotinfo.status ==="DONE")
			return "background-color:##dff0d8;";
		return "";
	}

	// ------------------------------------------------------------------------------------------------------
	// Process
	// ------------------------------------------------------------------------------------------------------
	this.getListArtefacts = function()
	{
		this.wait=true;		
		var self=this;
		var postMsg = {
					showcreatecases: true,
					showactivities: true,
					showusers:  true,
					showvariables:  true,
					showdocuments:  true,
			};
		var json= encodeURI( angular.toJson(postMsg, true));

		$http.get( '?page=custompage_meteor&action=getListArtefacts&jsonparam='+json+'&t='+Date.now() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
					
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
			
				console.log("listArtifacts",jsonResult);
				self.processes 					= jsonResult.processes;
				self.listeventslistprocesses 	= jsonResult.listevents;
				self.showonlyactive=false;
				self.wait=false;
			})
			.error( function() {
				self.wait=false;
			});
	}
	// this.getListArtefacts();
	
	this.addInput = function( activityinfo ) {
		if (! activityinfo.inputs)
			activityinfo.inputs=[];
		
		console.log("add in variable");
		activityinfo.inputs.push(  {'percent':'100'} );
	}
	this.proposeInput = function( processinfo, input)
	{
		var jsonValue = JSON.parse(processinfo.proposecontent);
		input.content=JSON.stringify( jsonValue, null, 4);
	}
	this.formatInput = function( processinfo, input)
	{
		input.content=JSON.stringify( JSON.parse(input.content), null, 4);
	}
			
	this.removeInput = function (activityinfo, input)
	{
		var i= activityinfo.inputs.indexOf(input);
		if (i!= -1) {
			activityinfo.inputs.splice(i, 1);
		}
	}
	
	
	// ------------------------------------------------------------------------------------------------------
	// Scenarii
	// ------------------------------------------------------------------------------------------------------
	
	
	this.configscenarii = {'cmd':  { 'sentences': [ 
								"createCase( \"processName\": \"myProcess\", \"processVersion\": \"1.0\", \"input\": {\"firstname\":\"walter\", \"lastname\":\"bates\"});", 
								"executeTask( \"processName\": \"myProcess\", \"processVersion\": \"1.0\", \"taskName\": \"validate\", \"input\": {\"status\":\"accepted\"});", 
								"executeTask( \"taskName\": \"validate\", \"input\": {\"status\":\"accepted\"});",
								"sleep( \"sleepInMs\" : 4000)",
								"assert();",
								"[{\"verb\":\"createCase\",\"processName\":\"MeteorStep\",\"processVersion\":\"1.0\"},{\"verb\":\"executeTask\",\"taskName\":\"Meteor_1\"},{\"verb\":\"executeTask\",\"taskName\":\"Meteor_2\"}]"
								]
							},
							'grv':{'sentences': [
								"Long processDefinitionId = accessor.processAPI.getLatestProcessDefinitionId(\"ProcessName\");",
								"ProcessDefinition processDefinition = accessor.processAPI.getProcessDefinition( processDefinitionId);",
								"ProcessInstance processInstance = processDefinition.start();",
								"TaskInstance taskInstance = wait { case.isHumanTaskAvailable process_instance:processInstance, task_name:\"Step1\"};",
								"Long assignee = taskInstance.assign assignee_name:defaultUser;",
								"Long assignee = taskInstance.execute assignee_name:defaultUser;",
								
								]}
	};
	
	 this.scenarii= [];
	 // this.scenarii = [ { 'name':'Chigago', 'type':'GRV', 'nbrobots': 1,
		// 'scenario':'Hello Chigago' } ];
	 
	this.removeScenarii = function ( scenarioinfo ) {
		var index = this.scenarii.indexOf(scenarioinfo);
		this.scenarii.splice(index, 1); 
	}
	this.addScenarii = function ()
	{
		this.scenarii.push( {} );
	}
	
	this.appendsentence= function( scenarioinfo, sentence) {
		if (scenarioinfo.scenario)
			scenarioinfo.scenario+= '\n'+sentence;
		else
			scenarioinfo.scenario = sentence;
	}
	// ------------------------------------------------------------------------------------------------------
	// Experience
	// ------------------------------------------------------------------------------------------------------
	this.experience = { "listCasesId" : "1003", "scenarii":[] };
	this.collectExperience = function( action )
	{
		this.wait=true;
		this.experience.action= action;
		
		console.log("collectExperience action["+action+"]");
		this.sendAllOnServer(this, "experienceAction",action);
	}
	this.postCollectExperience = function( jsonResult ) {
		console.log("postCollectExperience");
		this.listeventsexperience  		= jsonResult.listevents;
		
		this.experience.scenarii 		= jsonResult.experience.scenarii;
	}
	
	
	// ------------------------------------------------------------------------------------------------------
	// Start
	// ------------------------------------------------------------------------------------------------------
	this.listUrlIndex=0;
	this.listUrlCall = [];
	this.listUrlPercent=0;
	
	this.start = function()
	{
		this.wait=true;
		this.operation="Start";
		this.sendAllOnServer( this, "start", "start");
		// this.starttimer();
	
	};
	this.postStart = function( jsonResult ) {
		this.listeventsexecution    		= jsonResult.listevents;
		this.simulationid					= jsonResult.simulationid;
		this.execution={};
		this.execution.robots 				=  jsonResult.robots;
		this.execution.status				=  jsonResult.Status;
		this.execution.statusExecution		=  jsonResult.statusexecution;
		this.execution.timeStarted			=  jsonResult.TimeStarted;
		if (jsonResult.configList)
			this.config.list=jsonResult.configList;
		
		// timer ?
		// alert("simuilationuid="+self.simulationid);
		if (this.timer.armAtEnd && this.simulationid)
		{
			// now arm the refresh timer
			console.log("Arm the timer ");
			console.log(self);
			$scope.timer = $timeout(function() { this.refresh() }, 30000);
			// $scope.timer = $interval(function() { self.refresh()
			// }, 30000);
			// self.starttimer();
		}
	}


	this.startAllInOne = function() 
	{
		var param = { "processes": this.processes, "scenarii": this.scenarii};
		var json = encodeURI( angular.toJson( param, false));

 		console.log("Start : "+json);

		$http.get( '?page=custompage_meteor&action=start&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("history", angular.toJson(jsonResult));

						self.listeventsexecution    = jsonResult.listevents;
						self.statusrobots 			= jsonResult.statusrobots;
						self.simulationid			= jsonResult.simulationid;
						self.statusexecution		= jsonResult.statusexecution;
						self.wait=false;
						
						// alert('statusrobot='+self.statusrobot);
				})
				.error( function() {
					self.wait=false;
					
					// alert('an error occure');
					});
	};


	
	
	
	// -----------------------
	// get the list of configuration
	this.initPage=function()
	{
		var self=this;
		var postMsg = {
				showcreatecases: true,
				showactivities: true,
				showusers:  true,
				showvariables:  true,
				showdocuments:  true,
		};
		var json= encodeURI( angular.toJson(postMsg, true));


		self.wait=true;
		$http.get( '?page=custompage_meteor&action=initpage&jsonparam='+json+'&t='+Date.now() )
		.success( function ( jsonResult ) {
			self.config.list 				= jsonResult.configList;
			self.processes 					= jsonResult.processes;
			self.listeventslistprocesses 	= jsonResult.listevents;
			
			self.wait=false;
			self.listeventsconfig = jsonResult.listeventsconfig;
		})
		.error( function() {
			self.wait=false;
			});
	}
	this.initPage();
	
	
	
	// ------------------------------------------------------------------------------------------------------
	// Start
	this.timer={ 'armAtEnd': false};
	
	
	
	// ------------------------------------------------------------------------------------------------------
	// Refresh
	// ------------------------------------------------------------------------------------------------------
	this.execution={};
	/*
	 * this.refresh = function() { console.log(" Refresh
	 * ["+this.simulationid+"]"); this.refreshinternal( this ); }
	 * 
	 * this.refreshinternal = function( self )
	 */
	this.refresh = function() {
		var self=this;
		console.log(" RefreshInternal simulationId["+self.simulationid+"]");
		
		var param = { "simulationid": self.simulationid};
		var json = encodeURI( angular.toJson( param, false));

		self.operation="Refresh";
		self.refreshinprogress=true;

		$http.get( '?page=custompage_meteor&action=status&paramjson='+json+'&t='+Date.now() )
		.success( function ( jsonResult ) {
			self.listeventsexecution    = jsonResult.listevents;
			self.execution 				= jsonResult;
			self.refreshinprogress		= false;
			
			// $scope.chartTimeline = JSON.parse(jsonResult.chartTimeline);
			
			console.log(" Refresh done, status=["+self.execution.Status+"]");
			console.log(self.timer.timer);
			// ream the timeout
			if (self.execution.status !="DONE")
			{
				console.log(" Rearm the timer");
				$scope.timer = $timeout(function() { self.refresh() }, 30000);
			}
			
			if (self.execution.Status=="DONE" && (angular.isDefined($scope.timer)))              
			{
				// console.log(" Stop the timer");
				// $interval.cancel( $scope.timer );
				$scope.timer=null;
			}
			
		})
		.error( function() {
			self.refreshinprogress=false;
			});
	};
	
	// ------------------------------------------------------------------------------------------------------
	// List URL (we miss the POST)
	// ------------------------------------------------------------------------------------------------------

	this.sendAllOnServer = function( self, postAction, action ) // , listUrlCall,
													// listUrlIndex )
	{
		console.log("sendAllOnServer postAction["+postAction+"] action["+action+"]");
		// stop existing timer
		if (angular.isDefined($scope.timer))              
		{
			console.log(" Stop the current timer");
			$timeout.cancel( $scope.timer );
			// $interval.cancel( $scope.timer );
		}

		// the array maybe very big, so let's create a list of http call
		this.listUrlCall=[];
		this.listUrlCall.push( "action=collect_reset");
		this.listeventsexecution="";
		
		// prepare the string
		var param = { "processes" :  this.processes, "scenarii":  this.scenarii, "experience": this.experience};
		var json = angular.toJson( param, false);

		// split the string by packet of 5000
		while (json.length>0)
		{
			var jsonFirst = encodeURI( json.substring(0,5000));
			this.listUrlCall.push( "action=collect_add&paramjson="+jsonFirst);
			json =json.substring(5000);
		}
		var self=this;
		self.listUrlCall.push( "action="+action);
		self.postActionInProgress=postAction;
		
		self.listUrlIndex=0;
		self.timer.armAtEnd=true;
		self.executeListUrl( self ) // , self.listUrlCall, self.listUrlIndex );
		
		
	};
	
	this.executeListUrl = function( self ) // , listUrlCall, listUrlIndex )
	{
		console.log(" Call "+self.listUrlIndex+" : "+self.listUrlCall[ self.listUrlIndex ]);
		self.listUrlPercent= Math.round( (100 *  self.listUrlIndex) / self.listUrlCall.length);
		
		$http.get( '?page=custompage_meteor&'+self.listUrlCall[ self.listUrlIndex ]+'&t='+Date.now() )
			.success( function ( jsonResult ) {
				// console.log("Correct, advance one more",
				// angular.toJson(jsonResult));
				self.listUrlIndex = self.listUrlIndex+1;
				if (self.listUrlIndex  < self.listUrlCall.length )
					self.executeListUrl( self ) // , self.listUrlCall,
												// self.listUrlIndex);
				else
				{
					console.log("Finish", angular.toJson(jsonResult));
					self.wait=false;
					self.wait=false;
					self.listUrlPercent= 100; 
					
					// post depending of the action
					if (self.postActionInProgress=='experienceAction') {
						self.postCollectExperience( jsonResult );
					}
					if (self.postActionInProgress=='start') {
						self.postStart( jsonResult );
					}
					if (self.postActionInProgress == "saveConfig") {
						self.postSaveConfig( jsonResult );
					}
				}
			})
			.error( function() {
				self.wait=false;
				
				// alert('an error occure');
				});	
		};
	
		
		
		// ------------------------------------------------------------------------------------------------------
		// Manage configuration
		// ------------------------------------------------------------------------------------------------------
		this.config= { 'list':[] };
		
		// Save the current config
		this.saveConfig= function()
		{
			var param = { "confname": this.config.newname, "confdescription" : this.config.newdescription };
			var json = encodeURI( angular.toJson( param, false));

	 		this.listeventsconfig="";
			this.wait=true;

			this.sendAllOnServer( this, "saveConfig", "saveconfig&paramjson="+json);
		};
		this.postSaveConfig = function ( jsonResult ) {
			
			self.listeventsconfig 				= jsonResult.listeventsconfig;

		}
		// -----------------------
		// export the configuration
		this.getExportConfiguration=function()
		{
			var list=[];
			for (var i in this.config.list)
			{
				console.log("One conf="+this.config.list[i]);
				if (this.config.list[i].selected)
					list.push( this.config.list[i].name );
			}

			var param = { "listconfname": list};
			var json = encodeURI( angular.toJson( param, false));
			return json;
		}
		
		// import the configuratin
		
		this.fileIsDropped = function( testfileimported ) {
			var self=this;
			self.listeventsconfig ='';
			self.wait=true;
			$http.get( '?page=custompage_meteor&action=importconfs&filename='+testfileimported+'&t='+Date.now() )
			.success( function ( jsonResult ) {
				self.config.list 			= jsonResult.configList;
				self.listeventsconfig 		= jsonResult.listeventsconfig;
				
				self.config.newname=jsonResult.name; 
				self.config.newdescription=jsonResult.description; 
				
				self.processes = jsonResult.config.processes;
				if (!self.processes)
					self.processes=[];
				self.scenarii  = jsonResult.config.scenarii;
				if (!self.scenarii)
					self.scenarii=[];
				self.wait=false;
			})
			.error( function ( jsonResult ) {
				self.wait=false});
			
		}
		// load the config
		this.loadConfig = function() 
		{
			// load the config in the current process
			var param = { "confname": this.config.currentname};
			var json = encodeURI( angular.toJson( param, false));

			var selfconfig=this;
			selfconfig.listeventsconfig="";
			selfconfig.listeventslistprocesses="";
			selfconfig.listUrlPercent=0;
			selfconfig.wait=true;
			
			$http.get( '?page=custompage_meteor&action=loadconfig&paramjson='+json+'&t='+Date.now() )
			.success( function ( jsonResult ) {
				selfconfig.wait=false;
				// ready to save it
				selfconfig.config.newname=selfconfig.config.currentname; 
				selfconfig.config.newdescription=jsonResult.description; 
				
				selfconfig.processes = jsonResult.config.processes;
				if (!selfconfig.processes)
					selfconfig.processes=[];
				selfconfig.scenarii  = jsonResult.config.scenarii;
				if (!selfconfig.scenarii)
					selfconfig.scenarii=[];
				selfconfig.listeventsconfig = jsonResult.listeventsconfig;
				selfconfig.showonlyactive=true;

			})
			.error( function() {
				self.wait=false;
				// alert("Can't contact the server");
				});
		}
		
		this.loadAndStartConfig = function( )
		{		
			var param = { "confname": this.config.currentname};
			var json = encodeURI( angular.toJson( param, false));

			var self=this;
			self.listeventsconfig="";
			self.listeventslistprocesses="";
			self.listUrlPercent=0;
			self.listeventsconfig ='';
			self.wait=true;
			self.config.newname			= self.config.currentname;
			
			$http.get( '?page=custompage_meteor&action=loadandstart&paramjson='+json+'&t='+Date.now() )
			.success( function ( jsonResult ) {
				self.wait						= false;
				self.config.newdescription	= jsonResult.description; 
				
				self.processes 						= jsonResult.config.processes;
				if (!self.processes)
					self.processes=[];
				self.scenarii  						= jsonResult.config.scenarii;
				if (!self.scenarii)
					self.scenarii=[];
				self.listeventsconfig 				= jsonResult.listeventsconfig;
				self.showonlyactive					= true;
				self.simulationid					= jsonResult.simulationid;
				
				if (self.simulationid)
				{
					// now arm the refresh timer
					console.log("Arm the timer ");
					console.log(self);
					$scope.timer = $timeout(function() { self.refresh() }, 30000);
				}

			})
			.error( function() {
				self.wait=false;
				// alert("Can't contact the server");
				});
		}
		this.deleteConfig = function()
		{
			if (confirm("Do you want to delete the configuration "+this.config.currentname))
			{
				var param = { "confname": this.config.currentname};

				var json = encodeURI( angular.toJson( param, false));

				var selfconfig=this;
				selfconfig.listeventsconfig="";
				selfconfig.listeventslistprocesses="";

				selfconfig.listUrlPercent=0;
				selfconfig.wait=true;

				$http.get( '?page=custompage_meteor&action=deleteconfig&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
					selfconfig.wait=false;
					selfconfig.config.newname=""; // ready to save it
					selfconfig.config.newdescription=""; // ready to save it
					
					selfconfig.config.currentname="";
					selfconfig.listeventsconfig = jsonResult.listeventsconfig;
					selfconfig.config.list = jsonResult.configList;

				})
				.error( function() {
					self.wait=false;
					// alert("Can't contact the server");
					});
			}
		}
		var me = this;
		$scope.$watch('importfiles', function() {
			
			
			for (var i = 0; i < $scope.importfiles.length; i++) {
				me.wait=true;
				var file = $scope.importfiles[i];
				
				// V6 : url is fileUpload
				// V7 : /bonita/portal/fileUpload
				$scope.upload = $upload.upload({
					url: '/bonita/portal/fileUpload',
					method: 'POST',
					data: {myObj: $scope.myModelObj},
					file: file
				}).progress(function(evt) {
// console.log('progress: ' + parseInt(100.0 * evt.loaded / evt.total) + '% file
// :'+ evt.config.file.name);
				}).success(function(data, status, headers, config) {
				
					console.log('file ' + config.file.name + 'is uploaded successfully. Response: ' + data);
					me.fileIsDropped(data);
				});
			} // end $scope.importfiles
		}); 
	
		// ------------------------------------------------------------------------------------------------------
		// TOOLBOX
		<!-- Manage the event -->
		this.getListEvents = function ( listevents ) {
			return $sce.trustAsHtml(  listevents );
		}

		this.selectAll = function ( list)
		{
			for (var i in list)
			{
				list[i].selected=true;
			}
		}

});



})();