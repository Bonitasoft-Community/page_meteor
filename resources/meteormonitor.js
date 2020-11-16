/**
 * 
 */

(function() {

<!-- 'ng-file-upload' -->
var appCommand = angular.module('meteormonitor', ['googlechart', 'ui.bootstrap', 'ngSanitize','ngModal','angularFileUpload','ngCookies']);






// --------------------------------------------------------------------------
//
// Controler TitleControler
//
// --------------------------------------------------------------------------
	
appCommand.controller('TitleController', ['$cookies',
	function ($cookies) {
	this.isshowhistory = false;
	this.showhistory = function( show )
	{
	   this.isshowhistory = show;
	}		
}]);
// --------------------------------------------------------------------------
//
// Controler Meteor
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('MeteorControler',
	function ( $http, $scope,  $sce, $interval, $timeout, $upload, $cookies ) {

	this.processes 	= { "enable": false};
	this.experience	= { "enable": true};
	this.scenarii  	= { "enable": false};
	
	this.status="";
	this.statusrobot=" status robot";
	this.statusexecution ="";
	this.statuslistrobots = [];
	this.wait=false;
	this.isshowExportDialog=false;
	
	this.showmainscenarii=true;
	this.navbaractiv = 'experiencescenarii';

	this.name="MeteorControler";
	
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}

	this.getNavStyle = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'border: 1px solid #c2c2c2;border-bottom-color: transparent;';
		return 'background-color:#cbcbcb';
	}
	
	this.testChart = {
			'type': 'ColumnChart',
			'displayed': true,
			'data': {
				'cols': ['cover','happy'],
				'rows' : [ 12,43]
			}
	};
	
	this.getHttpConfig = function () {
		var additionalHeaders = {};
		var csrfToken = $cookies['X-Bonita-API-Token'];
		if (csrfToken) {
			additionalHeaders ['X-Bonita-API-Token'] = csrfToken;
		}
		var config= {"headers": additionalHeaders};
		console.log("GetHttpConfig : "+angular.toJson( config));
		return config;
	}
	
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
	this.collectProcesses = function()
	{
		this.wait=true;
		this.processes.enable=true;
		
		var self=this;
		var postMsg = {
					showcreatecases: true,
					showactivities: true,
					showusers:  true,
					showvariables:  true,
					showdocuments:  true,
			};
		var json= encodeURI( angular.toJson(postMsg, true));
		
			
		$http.get( '?page=custompage_meteor&action=collectProcesses&jsonparam='+json+'&t='+Date.now(), this.getHttpConfig() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
					
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
			
				console.log("listArtifacts",jsonResult);
				self.processes.scenarii 		= jsonResult.processes;
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
	this.experience = { "listCasesId" : "", "scenarii":[], "enable": true };
	this.collectExperience = function( action )
	{
		this.wait=true;
		this.experience.enable=true;
		this.experience.action= action;
		
		console.log("collectExperience action["+action+"]");
		this.sendAllOnServer(this, "experienceAction",action);
	}
	this.postCollectExperience = function( jsonResult ) {
		console.log("postCollectExperience : listevents="+jsonResult.listevents);
		this.listeventsexperience  		= jsonResult.listevents;
		
		this.experience.scenarii 		= jsonResult.experience.scenarii;
	}
	
	
	// ------------------------------------------------------------------------------------------------------
	// Start
	// ------------------------------------------------------------------------------------------------------
	this.listUrlIndex=0;
	this.listUrlCall = [];
	this.listUrlPercent=0;
	this.mode="CLASSIC";
	
	this.start = function( mode )
	{
		this.wait=true;
		this.operation="Start";
		this.mode=mode;
		this.sendAllOnServer( this, "start", "start");
		// this.starttimer();
	
	};
	this.postStart = function( jsonResult ) {
		this.execution.listeventsexecution   = jsonResult.listevents;
		console.log("postStart listEventExecution="+jsonResult.listevents);
		this.simulationid					= jsonResult.simulationid;
		this.execution={};
		this.execution.robots 				= jsonResult.robots;
		this.execution.status				= jsonResult.Status;
		this.execution.statusExecution		= jsonResult.statusexecution;
		this.execution.timeStarted			= jsonResult.TimeStarted;
		this.execution.timeinms 			= jsonResult.timeinms;
		this.execution.percentunittest		= jsonResult.percentunittest;
		this.execution.listevents			= jsonResult.listevents;
		
		if (jsonResult.configList)
			this.config.list=jsonResult.configList;
		if (jsonResult.armtimer)
			this.timer.armAtEnd =jsonResult.armtimer;
		
		
		// timer ?
		// alert("simuilationuid="+self.simulationid);
		if (this.timer.armAtEnd && this.simulationid)
		{
			// now arm the refresh timer
			console.log("Arm the timer for["+this.name+"] object");
			console.log( this );
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

		$http.get( '?page=custompage_meteor&action=start&paramjson='+json+'&t='+Date.now(), this.getHttpConfig() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
			
					
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
		$http.get( '?page=custompage_meteor&action=initpage&jsonparam='+json+'&t='+Date.now(), this.getHttpConfig() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
			
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				
				self.config.list 		= jsonResult.configList;
				self.listeventsconfig 	= jsonResult.listeventsconfig;
				self.deploimentsuc  	= jsonResult.deploimentsuc;
				self.deploimenterr  	= jsonResult.deploimenterr;
				console.log("deploymebntsuc ="+self.deploimentsuc);
				self.wait				= false;
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

		$http.get( '?page=custompage_meteor&action=status&paramjson='+json+'&t='+Date.now(), this.getHttpConfig() )
		.success( function ( jsonResult, statusHttp, headers, config ) {
					
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
			
				
				self.execution 							= jsonResult;
				self.execution.listeventsexecution    	= jsonResult.listevents;
				self.refreshinprogress					= false;
				
				// $scope.chartTimeline = JSON.parse(jsonResult.chartTimeline);
				
				console.log(" Refresh done, status=["+self.execution.Status+"]");
				console.log(self.timer.timer);
				// ream the timeout
				if (self.execution.status !="DONE")
				{
					console.log(" Rearm the timer now");
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
		this.execution.listeventsexecution="";
		
		// prepare the string
		var param = {};
		if (this.processes.enable)
			param.processes = this.processes;
		if (this.experience.enable)
			param.experience = this.experience;
		if (this.scenarii.enable)
			param.scenarii = this.scenarii;
		param.mode = this.mode;
		param.scenarioname= this.config.name;
		
		var json = angular.toJson( param, false);

		console.log("action["+action+"] Json="+json);
		
		// split the string by packet of 5000
		while (json.length>0)
		{
			var jsonFirst = encodeURIComponent( json.substring(0,2500));
			this.listUrlCall.push( "action=collect_add&paramjson="+jsonFirst);
			json =json.substring(2500);
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
		
		$http.get( '?page=custompage_meteor&'+self.listUrlCall[ self.listUrlIndex ]+'&t='+Date.now(), this.getHttpConfig() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
					
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
			
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
	this.config= { 'list':[], 'name':'', 'newname' :'' };
	
	// Save the current config
	this.saveConfig= function()
	{
		var param = { "scenarioname": this.config.newname, "confdescription" : this.config.newdescription };
		var json = encodeURI( angular.toJson( param, false));

 		this.listeventsconfig="";
		this.wait=true;

		this.sendAllOnServer( this, "saveConfig", "saveconfig&paramjson="+json);
	};
	
	this.postSaveConfig = function ( jsonResult ) {
		console.log("postSaveConfig");
		this.listeventsconfig 		= jsonResult.listeventsconfig;
		this.config.list 			= jsonResult.configList;
	}
	// -----------------------
	// export the configuration
	this.getExportConfiguration=function()
	{
		var list=[];
		for (var i in this.config.list)
		{
			// console.log("getExportConfiguration:conf="+angular.toJson(this.config.list[i]));
			if (this.config.list[i].selected)
				list.push( this.config.list[i].name );
		}

		var param = { "listscenarioname": list};
		var json = encodeURI( angular.toJson( param, false));
		return json;
	}
	
	// import the configuratin
	
	this.fileIsDropped = function( testfileimported ) {
		var self=this;
		self.listeventsconfig ='';
		self.wait=true;
		$http.get( '?page=custompage_meteor&action=importconfs&filename='+testfileimported+'&t='+Date.now(), this.getHttpConfig() )
		.success( function ( jsonResult, statusHttp, headers, config ) {
				
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
		
			self.config.list 			= jsonResult.configList;
			self.listeventsconfig 		= jsonResult.listeventsconfig;
			
			self.config.newname			= jsonResult.name; 
			self.config.name			= jsonResult.name;
			self.config.newdescription	= jsonResult.description; 
			
			self.experience={};
			if ( jsonResult.config &&  jsonResult.config.experience)
				self.experience = jsonResult.config.experience;
			if (!self.experience)
				self.experience={};

			self.processes={};
			if ( jsonResult.config &&  jsonResult.config.processes)
				self.processes = jsonResult.config.processes;
			if (!self.processes)
				self.processes={};

			self.scenarii=[];
			if ( jsonResult.config &&  jsonResult.config.scenarii)
				self.scenarii  = jsonResult.config.scenarii;
			if (!self.scenarii)
				self.scenarii=[];
			
			self.wait=false;
		})
		.error( function ( jsonResult ) {
			self.wait=false});
		
	}
	// load the config
	this.loadConfig = function( action ) 
	{
		// load the config in the current process
		var param = { "scenarioname": this.config.currentname};
		var json = encodeURI( angular.toJson( param, false));

		var self =this;
		self.listeventsconfig="";
		self.listeventslistprocesses="";
		self.listUrlPercent=0;
		self.wait=true;
		
		$http.get( '?page=custompage_meteor&action='+action+'&paramjson='+json+'&t='+Date.now(), this.getHttpConfig() )
		.success( function ( jsonResult, statusHttp, headers, config ) {
				
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			console.log("-- Result action load");

			self.wait=false;
			// ready to save it
			self.config.newname        	= self.config.currentname; 
			self.config.name			= self.config.currentname;
			self.config.newdescription 	= jsonResult.description; 
			
			self.processes = jsonResult.config.processes;
			if (! self.processes)
				self.processes=[];
			self.scenarii  = jsonResult.config.scenarii;
			if (! self.scenarii)
				self.scenarii=[];
			self.experience = jsonResult.config.experience;
			console.log("Load experience="+self.experience);
			if (! self.experience)
				self.experience={};
			
			self.listeventsconfig 	= jsonResult.listeventsconfig;
			self.showonlyactive=true;
			
			self.simulationid					= jsonResult.simulationid;			
			if (self.simulationid)
			{
				// now arm the refresh timer
				console.log("Arm the timer ");
				console.log(self);
				$scope.timer = $timeout(function() { self.refresh() }, 30000);
			}

			console.log("-- End Result action load");

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
			var param = { "scenarioname": this.config.currentname};

			var json = encodeURI( angular.toJson( param, false));

			var self = this;
			self.listeventsconfig="";
			self.listeventslistprocesses="";

			self.listUrlPercent=0;
			self.wait=true;

			$http.get( '?page=custompage_meteor&action=deleteconfig&paramjson='+json+'&t='+Date.now(), this.getHttpConfig() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
		
				self.wait=false;
				self.config.newname=""; // ready to save it
				self.config.name ="";
				self.config.newdescription=""; // ready to save it
				
				self.config.currentname="";
				self.listeventsconfig = jsonResult.listeventsconfig;
				self.config.list = jsonResult.configList;

			})
			.error( function() {
				self.wait=false;
				// alert("Can't contact the server");
				});
		}
	}
	
	
	// -----------------------------------------------------------------------------------------
	// tool
	// -----------------------------------------------------------------------------------------
	this.getHtml = function(listevents, sourceContext) {
		// console.log("getHtml:Start (r/o) source="+sourceContext);
		return $sce.trustAsHtml(listevents);
	}

	
	var me = this;
	$scope.$watch('importfiles', function() {
		
		console.log("Watch import file");
		if (! $scope.importfiles) {
			return;
		}
		console.log("Watch import file.lenght="+ $scope.importfiles.length);
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
	
	

});



})();