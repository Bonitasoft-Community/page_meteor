'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('meteormonitor', ['googlechart', 'ui.bootstrap', 'ngSanitize']);






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
	function ( $http, $scope,  $sce ) {

	this.processes= [  ];
	this.status="";
	this.statusrobot=" status robot";
	this.statusexecution ="t";
	this.statuslistrobots = [];
	this.wait=false;
	this.startwait=false;
	
	// --------------------- getListProcess
	this.getlistprocesses = function()
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
		var json= angular.toJson(postMsg, true);
		
		$http.get( '?page=custompage_meteor&action=getlistprocesses&jsonparam='+json )
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.processes 					= jsonResult.processes;
						self.listeventslistprocesses 	= jsonResult.listevents;
						self.showonlyactive=false;
						self.wait=false;
				})
				.error( function() {
					self.wait=false;
					});
	}
	this.getlistprocesses();
	
	// Start
	this.start = function()
	{
		var param = { "processes": this.processes};
 		var json= angular.toJson( param, false);
		var self=this;
		self.startwait=true;

		// alert("start json="+json);
 		
		$http.get( '?page=custompage_meteor&action=start&paramjson='+json )
				.success( function ( jsonResult ) {
						console.log("history", angular.toJson(jsonResult));

						self.listeventsexecution    = jsonResult.listevents;
						self.statusrobots 			= jsonResult.statusrobots;
						self.simulationid			= jsonResult.simulationid;
						self.statusexecution		= jsonResult.statusexecution;
						self.startwait=false;
						
						// alert('statusrobot='+self.statusrobot);
				})
				.error( function() {
					self.startwait=false;
					
					alert('an error occure');
					});
	};
	
	
	// Refresh
	this.execution={};
	this.refresh = function()
	{
		
		var param = { "simulationid": this.simulationid};
 		var json= angular.toJson( param, false);
		var self=this;
		self.startwait=true;

		$http.get( '?page=custompage_meteor&action=refresh&paramjson='+json )
		.success( function ( jsonResult ) {
			self.listeventsexecution    = jsonResult.listevents;
			self.execution 				= jsonResult;
			self.startwait=false;
			
		})
		.error( function() {
			self.startwait=false;
			});
	};

	<!-- Manage the event -->
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents );
	}

	
	<!-- manage configuration -->
	this.config= { 'list':[] };
	// Save the current config
	this.saveConfig= function( name)
	{
		var param = { "processes": this.processes, "confname": this.config.newname};
 		var json= angular.toJson( param, false);
		var self=this;
		self.startwait=true;
		$http.get( '?page=custompage_meteor&action=saveconfig&paramjson='+json )
				.success( function ( jsonResult ) {
					self.startwait=false;
				})
				.error( function() {
					self.startwait=false;
					alert('an error occure');
					});
	};
	
	// load the config
	this.loadConfig = function() 
	{
		// load the config in the current process
		var param = { "confname": this.config.currentname};
 		var json= angular.toJson( param, false);
		var self=this;
		self.startwait=true;
		
		$http.get( '?page=custompage_meteor&action=loadconfig&paramjson='+json )
		.success( function ( jsonResult ) {
			self.startwait=false;
		})
		.error( function() {
			self.startwait=false;
			});
	}
	
	// get the list of configuration
	this.initConfig=function()
	{
		$http.get( '?page=custompage_meteor&action=initconfig' )
		.success( function ( jsonResult ) {
			self.config.list = jsonResult.list;
			self.startwait=false;
		})
		.error( function() {
			self.startwait=false;
			});
	}
	this.initConfig();
	
});



})();