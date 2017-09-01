'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('forkliftmonitor', ['googlechart', 'ui.bootstrap','ngSanitize']);






// --------------------------------------------------------------------------
//
// Controler Ping
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('ForkLiftControler',
	function ( $http, $scope, $sce ) {

	this.isshowhistory=false;
	this.showsources=false;
	this.config = { 'sources':[],
					'content': {},
					'options' : [ {'name':'Directory', 'type':'DIR'}, {'name':'Bonita server', 'type':'BONITA'}],
					'toadd': 'DIR',
					
	}
	
	this.showhistory = function( showHistory ) {
		this.isshowhistory = showHistory;
		};
	

	// -------------------------------------
	// Source management
	// -------------------------------------
	this.addOneSource = function( typeSource)
	{
		var newsource= { 'type': typeSource };
		this.config.sources.push( newsource);
	}
	this.removeOneSource = function ( source )
	{
		var index = this.config.sources.indexOf( source);
		this.config.sources.splice(index, 1); 
	}
	this.testConnection = function( source)
	{
		alert("Not yet implemented");
	}
		
	// load sources
	this.loadConfiguration = function () 
	{
		this.config.listevents='';
		this.config.wait=true;
		var self=this;
		$http.get( '?page=custompage_forklift&action=loadConfiguration' )
		.success( function ( jsonResult ) {
			self.config.wait=false;
			self.config.sources = jsonResult.sources;
			if (! self.config.sources)
				self.config.sources=[];
			if (self.config.sources.length==0)
				self.showsources=true;
			
			self.config.content = jsonResult.content;
			if (!self.config.content)
				self.config.content={};
			self.config.listevents= jsonResult.listevents; 
		})
		.error( function ( jsonResult ) {
			self.config.wait=false;
		});
	
	}
	this.loadConfiguration();
	
	// save sources
	this.saveConfiguration= function()
	{
		// remove the event in each list
		for (var i in this.config.list)
		{
			this.config.list[ i ].listevents='';
		}
		var param = { "sources": this.config.sources, 'content': this.config.content };
		var json = encodeURI( angular.toJson( param, false));
		this.config.listevents='';
		this.config.wait=true;
		var self=this;
		$http.get( '?page=custompage_forklift&action=saveConfiguration&paramjson='+json )
			.success( function ( jsonResult ) {
				self.config.wait=false;
				self.config.listevents= jsonResult.listevents; 
			})
			.error( function ( jsonResult ) {
				self.config.wait=false;
			});
	}
		
	this.checkContent = function( checkPlease)
	{
		//this.config.content.organization=checkPlease;
		//this.config.content.layout=checkPlease;
		//this.config.content.theme=checkPlease;
		//this.config.content.pages=checkPlease;
		//this.config.content.restapi=checkPlease;
		this.config.content.profile=checkPlease;
		//this.config.content.livingapp=checkPlease;
		//this.config.content.bdm=checkPlease;
		this.config.content.process=checkPlease;
	}
		
	

	// -------------------------------------
	// Operation
	// -------------------------------------
	this.synchronisation={ 'wait': false, 'listevents':'', 'report':''};
	this.synchronisation.actions = [
		{ 'type': 'DEPLOY', 'name':'Deploy'},
		{ 'type': 'IGNORE', 'name':'Ignore'},
		{ 'type': 'DELETE', 'name':'Delete'}
		];
	
	this.synchronisationDetect = function()
	{
		var self=this;
		self.synchronisation.wait=true;
		self.synchronisation.listevents='';
		self.synchronisation.report='';
		$http.get( '?page=custompage_forklift&action=synchronisationdetect' )
			.success( function ( jsonResult ) {
				self.synchronisation.wait=false;
				self.synchronisation.listevents= jsonResult.listevents;
				self.synchronisation.detection= jsonResult.detection;
				self.synchronisation.report= jsonResult.detection.report;
			})
			.error( function ( jsonResult ) {
				self.synchronisation.wait=false;
			});
		
		
	}
	this.synchronisationExecute = function()
	{
		var self=this;
		self.synchronisation.wait=true;
		self.synchronisation.listevents='';
		self.synchronisation.report='';
		// create a short list
		var listDeploy=[];
		for (var i in self.synchronisation.detection.items)
		{
			var item=self.synchronisation.detection.items[ i ];
			if (item.action=='IGNORE')
				continue;
			var actionitem={};
			actionitem.type=item.type;
			actionitem.name=item.name;
			actionitem.version=item.version;
			actionitem.action=item.action;
			listDeploy.push(actionitem);
		}
		
		var json = encodeURI( angular.toJson( listDeploy, false));
		
		$http.get( '?page=custompage_forklift&action=synchronisationexecute&paramjson='+json )
			.success( function ( jsonResult ) {
				self.synchronisation.wait=false;
				self.synchronisation.listevents= jsonResult.listevents;
				self.synchronisation.report= jsonResult.detection.report;

				// ok, search the result in the list to update the status
				console.log("~~~~~~~~~~~~~~~~~~~~~~~ Search result.detection.items ")
				for (var i in jsonResult.detection.items)
				{
					var actionresult= jsonResult.detection.items[ i ];
					console.log("result i="+i+" type=["+actionresult.type+"] name=["+ actionresult.name+"] version=["+actionresult.version+"]")
					for (var j in self.synchronisation.detection.items)
					{
						var currentitem=self.synchronisation.detection.items[ j ];
						console.log(" Local j="+j+" type=["+currentitem.type+"] name=["+ currentitem.name+"] version=["+ currentitem.version+"]")

						if (( currentitem.type === actionresult.type) 
								&& ( currentitem.name === actionresult.name) 
								&& ( (! currentitem.version) || currentitem.version && (currentitem.version === actionresult.version)))
						{
							console.log(" MATCHLocal status=["+actionresult.status+"]");

							currentitem.deploystatus= actionresult.deploystatus;
							currentitem.listevents= actionresult.listevents;
							if (currentitem.deploystatus == "DEPLOYED" || currentitem.deploystatus =="DELETED")
							{
								currentitem.invaliddecision= true;
								currentitem.action="IGNORE";
							}
						}
								
					}
				}
				
			})
			.error( function ( jsonResult ) {
				self.synchronisation.wait=false;
			});
		
		
	}
	this.ping = function()
	{
		$('#collectbtn').hide();
		$('#collectwait').show();
		this.pinginfo="Hello";

		var self=this;

		$http.get( '?page=custompage_forklift&action=ping' )
				.success( function ( jsonResult ) {
						console.log("history",jsonResult);
						self.pingdate 		= jsonResult.pingcurrentdate;
						self.pinginfo 		= jsonResult.pingserverinfo;
						self.listprocesses	= jsonResult.listprocesses;
						$scope.chartObject		 	= JSON.parse(jsonResult.chartObject);

						$('#collectbtn').show();
						$('#collectwait').hide();
				})
				.error( function() {
					alert('an error occure');
						$('#collectbtn').show();
						$('#collectwait').hide();
					});

	}

	// ------------------------------------------------------------------------------------------------------
	// TOOLBOX
	// Manage the event
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents );
	}


});



})();