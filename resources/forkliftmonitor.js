'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('forkliftmonitor', ['ngSanitize']);






// --------------------------------------------------------------------------
//
// Controler Forklift
//
// --------------------------------------------------------------------------

appCommand.controller('ForkLiftControler',
	function ( $http, $scope, $sce ) {

	this.isshowhistory=false;
	this.showsources=false;
	// , {'name':'Bonita server', 'type':'BONITA'}
	this.config = { 'sources':[],
					'content': {},
					'options' : [ 
						{'name':'Directory ', 
							'type':'Dir',
							'title' : "Directory",
							'explanation':'All files behind this directory are detected'
						}, 
						{'name':'BCD', 
							'type':'BCD',
							'title':'Bonita Continous Delivery (BCD)',
							'explanation': 'Specify the Target directory of BCD'}], 
					'toadd': 'Dir',
					
	}
		
	this.navbaractiv = 'Artifacts';
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
	this.showhistory = function( showHistory ) {
		this.isshowhistory = showHistory;
		};
	

	// -------------------------------------
	// Source management
	// -------------------------------------
	this.addOneSource = function( typeSource)
	{
		console.log("addOneSource type="+typeSource);
		
		var newsource= { 'type': typeSource };
		
		this.config.sources.push( newsource );
		console.log("List="+angular.fromJson(this.config.sources));
		
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
	this.init = function () 
	{
		this.config.listevents='';
		this.config.wait=true;
		var self=this;
		$http.get( '?page=custompage_forklift&action=init&t='+Date.now()  )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page ! statusHttp="+statusHttp+" jsonResult="+jsonResult);
					window.location.reload();
				}
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
	this.init();
	
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
		$http.get( '?page=custompage_forklift&action=saveConfiguration&paramjson='+json+'&t='+Date.now()  )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page ! statusHttp="+statusHttp+" jsonResult="+jsonResult);

					window.location.reload();
				}
				self.config.wait=false;
				self.config.listevents= jsonResult.listevents; 
				console.log("Save list events="+jsonResult.listevents);
			})
			.error( function ( jsonResult ) {
				self.config.wait=false;
			});
	}
		
	this.checkContent = function( checkPlease )
	{
		this.config.content.organization=checkPlease;
		this.config.content.layout=checkPlease;
		this.config.content.theme=checkPlease;
		this.config.content.custompage=checkPlease;
		this.config.content.restapi=checkPlease;
		this.config.content.profile=checkPlease;
		this.config.content.livingapp=checkPlease;
		// this.config.content.bdm=checkPlease;
		this.config.content.process=checkPlease;
	}
	// default : check all
	this.checkContent( true );
	

	// -------------------------------------
	// Operation
	// -------------------------------------
	this.synchronisation={ 'wait': false, 'listevents':'', 'report':''};
	this.synchronisation.actions = [
		{ 'type': 'DEPLOY', 'name':'Deploy'},
		{ 'type': 'IGNORE', 'name':'Ignore'},
		{ 'type': 'DELETE', 'name':'Archive'}
		];
	
	this.synchronisationDetect = function()
	{
		var self=this;
		self.synchronisation.wait=true;
		self.synchronisation.listevents='';
		self.synchronisation.report='';
		$http.get( '?page=custompage_forklift&action=synchronisationdetect'+'&t='+Date.now()  )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page ! statusHttp="+statusHttp+" jsonResult="+jsonResult);

					window.location.reload();
				}
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
			if (item.action=='IGNORE' || item.display==false)
				continue;
			var actionitem={};
			actionitem.type=item.type;
			actionitem.name=item.name;
			actionitem.version=item.version;
			actionitem.action=item.action;
			listDeploy.push(actionitem);
		}
		
		var json = encodeURI( angular.toJson( listDeploy, false));
		
		self.sendPost('synchronisationexecute', json );
	
		
	}
	/**
	 * 
	 */
	this.afterSynchronisationexecute = function ( jsonResult ) {
		this.synchronisation.wait=false;
		this.synchronisation.listevents= jsonResult.listevents;
		this.synchronisation.report= jsonResult.detection.report;

		// ok, search the result in the list to update the status
		console.log("~~~~~~~~~~~~~~~~~~~~~~~ afterSynchronisationexecute result.detection.items ")
		for (var i in jsonResult.detection.items)
		{
			var actionresult= jsonResult.detection.items[ i ];
			// console.log("result i="+i+" type=["+actionresult.type+"] name=["+ actionresult.name+"] version=["+actionresult.version+"]")
			for (var j in this.synchronisation.detection.items)
			{
				var currentitem=this.synchronisation.detection.items[ j ];
				// console.log(" Local j="+j+" type=["+currentitem.type+"] name=["+ currentitem.name+"] version=["+ currentitem.version+"]")

				if (( currentitem.type === actionresult.type) 
						&& ( currentitem.name === actionresult.name) 
						&& ( (! currentitem.version) || currentitem.version && (currentitem.version === actionresult.version)))
				{
					// console.log(" MATCHLocal status=["+actionresult.status+"]");

					currentitem.deploystatus= actionresult.deploystatus;
					currentitem.listevents= actionresult.listevents;
					if (currentitem.deploystatus == "DEPLOYED" || currentitem.deploystatus =="DELETED" || currentitem.deploystatus =="LOADED")
					{
						currentitem.invaliddecision= true;
						currentitem.action="IGNORE";
					}
				}
						
			}
		}
		console.log("~~~~~~~~~~~~~~~~~~~~~~~ afterSynchronisationexecute end ")

	}
	

	// -------------------------------------
	// Toolbox
	// -------------------------------------

	this.showartefact={ 'layout':true, 
			'theme':true,
			'lookandfeel':true,
			'bdm':true,
			'organization':true,
			'restapi':true,
			'pages':true,
			'process':true,
			'profile':true,
			'livingapp':true
	}

	this.getItems=function()
	{

		var listdisplay=[];
		if (this.synchronisation.detection && this.synchronisation.detection.items)
		{
			for (var i in this.synchronisation.detection.items)
			{		
				var item= this.synchronisation.detection.items[ i ];
				console.log("item="+item.type+" / "+item.name);
				
				if (item.type === 'layout')
					item.display = this.showartefact.layout;
				else if (item.type === 'theme')
					item.display = this.showartefact.theme;
				else if (item.type === 'lookandfeel')
					item.display = this.showartefact.lookandfeel;
				else if (item.type === 'bdm')
					item.display = this.showartefact.bdm;
				else if (item.type === 'organization')
					item.display = this.showartefact.organization;
				else if (item.type === 'restapi')
					item.display = this.showartefact.restapi;
				else if (item.type === 'pages')
					item.display = this.showartefact.pages;
				else if (item.type === 'process')
					item.display = this.showartefact.process;
				else if (item.type === 'profile')
					item.display = this.showartefact.profile;
				else if (item.type === 'livingapp')
					item.display = this.showartefact.livingapp;
				
				else
					listdisplay.push( item )
			}
			
			for (var i in this.synchronisation.detection.items)
			{		
				var item= this.synchronisation.detection.items[ i ];
				if (item.display==true)
					listdisplay.push( item )
			}
		}
		
		return listdisplay;
	}
	
	
	// -----------------------------------------------------------------------------------------
	// Thanks to Bonita to not implement the POST : we have to split the URL
	// -----------------------------------------------------------------------------------------
	var postParams=
	{
		"listUrlCall" : [],
		"action":"",
		"advPercent":0
		
	}
	this.sendPost = function(finalaction, json )
	{
		var self=this;
		self.inprogress=true;
		console.log("sendPost inProgress<=true action="+finalaction+" Json="+ angular.toJson( json ));
		
		self.postParams={};
		self.postParams.listUrlCall=[];
		self.postParams.action= finalaction;
		var action = "collect_reset";
		// split the string by packet of 1800 (URL cut at 2800, and we have
		// to encode the string)
		while (json.length>0)
		{
			var jsonSplit= json.substring(0,1500);
			var jsonEncodeSplit = encodeURI( jsonSplit );
			
			// Attention, the char # is not encoded !!
			jsonEncodeSplit = jsonEncodeSplit.replace(new RegExp('#', 'g'), '%23');
			
			// console.log("collect_add JsonPartial="+jsonSplit);
			// console.log("collect_add JsonEncode ="+jsonEncodeSplit);
		
			
			self.postParams.listUrlCall.push( "action="+action+"&paramjsonpartial="+jsonEncodeSplit);
			action = "collect_add";
			json = json.substring(1500);
		}
		self.postParams.listUrlCall.push( "action="+self.postParams.action);
		
		
		self.postParams.listUrlIndex=0;
		self.executeListUrl( self ) // , self.listUrlCall, self.listUrlIndex
									// );
		// this.operationTour('updateJob', plugtour, plugtour, true);
		// console.log("sendPost.END")
		
	}
	
	this.executeListUrl = function( self ) // , listUrlCall, listUrlIndex )
	{
		// console.log(" CallList "+self.postParams.listUrlIndex+"/"+ self.postParams.listUrlCall.length+" : "+self.postParams.listUrlCall[ self.postParams.listUrlIndex ]);
		self.postParams.advPercent= Math.round( (100 *  self.postParams.listUrlIndex) / self.postParams.listUrlCall.length);
		
		// console.log("executeListUrl call HTTP");

		$http.get( '?page=custompage_forklift&t='+Date.now()+'&'+self.postParams.listUrlCall[ self.postParams.listUrlIndex ] )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page ! statusHttp="+statusHttp+" jsonResult="+jsonResult);

					window.location.reload();
				}
				// console.log("executeListUrl receive data HTTP");
				// console.log("Correct, advance one more",
				// angular.toJson(jsonResult));
				self.postParams.listUrlIndex = self.postParams.listUrlIndex+1;
				if (self.postParams.listUrlIndex  < self.postParams.listUrlCall.length )
					self.executeListUrl( self ) // , self.listUrlCall,
												// self.listUrlIndex);
				else
				{
					self.inprogress = false;
					// console.log("sendPost finish inProgress<=false jsonResult="+ angular.toJson(jsonResult));
					self.postParams.advPercent= 100; 
					if (self.postParams.action=="synchronisationexecute") {
						self.afterSynchronisationexecute( jsonResult );
					}
				}
			})
			.error( function(jsonResult, statusHttp, headers, config) {
				console.log("executeListUrl.error HTTP statusHttp="+statusHttp);
				// connection is lost ?
				if (statusHttp==401) {
					console.log("error, Redirected to the login page ! statusHttp="+statusHttp+" jsonResult="+jsonResult);

					window.location.reload();
				}
				self.inprogress = false;				
				});	
	};
	
	// ------------------------------------------------------------------------------------------------------
	// TOOLBOX
	// Manage the event
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents );
	}


});



})();