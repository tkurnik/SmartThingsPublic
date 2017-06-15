/**
 *  MyNeurioServiceMgr
 *
 *  Copyright 2015 Yves Racine
 *  linkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat
 
definition(
	name: "MyNeurioServiceMgr",
	namespace: "yracine",
	author: "Yves Racine",
	description: "This smartapp is the Service Manager for My Neurio Device: it instantiates the Neurio device(s) & appliances and polls them on a regular basis",
	category: "My Apps",
	iconUrl: "https://s3-us-west-2.amazonaws.com/neurio/community/NeurioAppLogo60x60.png",
	iconX2Url: "https://s3-us-west-2.amazonaws.com/neurio/community/NeurioAppLogo72x72.png",
	iconX3Url: "https://s3-us-west-2.amazonaws.com/neurio/community/NeurioAppLogo72x72.png"
)


preferences {
	page(name: "about", title: "About", nextPage: "auth")
	page(name: "auth", title: "Neurio", content:"authPage", nextPage:"deviceList")
	page(name: "deviceList", title: "Neurio Sensors", content:"NeurioDeviceList")
/*  
	Removed the second page of appliance selection as this is not an efficient method to avoid ST max execution time constraint
    
	page(name: "applianceList", title: "Neurio Appliances", content:"NeurioApplianceList", nextPage:"applianceList2")
	page(name: "applianceList2", title: "Neurio Appliances", content:"NeurioApplianceList2", install:true)
*/
	page(name: "applianceList", title: "Neurio Appliances", content:"NeurioApplianceList",nextPage:"otherSettings")
	page(name: "otherSettings", title: "Other Settings", content:"otherSettings", install:true)
}

mappings {

	path("/auth") {
		action: [
		  GET: "auth"
		]
	}

	path("/swapToken") {
		action: [
			GET: "swapToken"
		]
	}
}

def auth() {
	redirect location: oauthInitUrl()
}


def about() {
 	dynamicPage(name: "about", install: false, uninstall: true) {
 		section("About") {	
			paragraph "MyNeurioServiceMgr, the smartapp that connects your Neurio Sensor(s) to SmartThings via cloud-to-cloud integration" +
				" and polls your Neurio appliance data on a regular interval"
			paragraph "Version 0.8.3\n\n" +
			"If you like this app, please support the developer via PayPal:\n\nyracine@yahoo.com\n\n" +
			"Copyright©2015 Yves Racine"
			href url:"http://github.com/yracine/device-type.myneurio", style:"embedded", required:false, title:"More information...", 
			description: "http://github.com/yracine/device-type.myneurio"
		} 
	}        
}

def otherSettings() {
	dynamicPage(name: "otherSettings", title: "Other Settings", install: true, uninstall: false) {
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:
				false
			input "phoneNumber", "phone", title: "Send a text message?", required: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
	}
}


def authPage() {
	log.debug "authPage()"

	if(!atomicState.accessToken) {
		log.debug "about to create access token"
		createAccessToken()
		atomicState.accessToken = state.accessToken
	}


	def description = "Required"
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if(atomicState.authToken) {
		log.debug "atomicState.authToken in authPage= ${atomicState.authToken}"

		// TODO: Check if it's valid
		if(true) {
			description = "You are connected."
			uninstallAllowed = true
			oauthTokenProvided = true
		} else {
			description = "Required" // Worth differentiating here vs. not having atomicState.authToken? 
			oauthTokenProvided = false
		}
	}

	def redirectUrl = buildRedirectUrl("auth")

	log.debug "RedirectUrl = ${redirectUrl}"

	// get rid of next button until the user is actually auth'd

	if (!oauthTokenProvided) {

		return dynamicPage(name: "auth", title: "Login", nextPage:null, uninstall:uninstallAllowed,submitOnChange: true) {
			section(){
				paragraph "Tap below to log in to the Neurio portal and authorize SmartThings access. Be sure to scroll down on page 2 and press the 'Allow' button."
				href url:redirectUrl, style:"embedded", required:true, title:"My Neurio", description:description
			}
		}

	} else {

		return dynamicPage(name: "auth", title: "Log In", nextPage:"deviceList", uninstall:uninstallAllowed,submitOnChange: true) {
			section(){
				paragraph "Tap Next to continue to setup your Neurio Sensors."
				href url:redirectUrl, style:"embedded", state:"complete", title:"My Neurio", description:description
			}
		}

	}


}

def NeurioDeviceList() {
	log.debug "NeurioDeviceList()"

	def neurioSensors = getNeurioSensors()

	log.debug "device list: $neurioSensors"

	def p = dynamicPage(name: "deviceList", title: "Select Your Sensor(s)",nextPage:"applianceList") {
		section(""){
			paragraph "Tap below to see the list of Neurio Sensors available in your Neurio account and select the ones you want to connect to SmartThings."
			input(name: "NeurioSensors", title:"", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:neurioSensors])
		}
	}

	log.debug "list p: $p"
	return p
}


def NeurioApplianceList() {
	log.debug "NeurioApplianceList()"

    
	def neurioAppliances = getNeurioAppliances(state.locationId)

	log.debug "device list: $neurioAppliances"
/*
	Removed page 2 as this is not an efficient method to avoid ST max execution time constraint
    
	def p = dynamicPage(name: "applianceList", title: "Select Your Appliance(s)", nextPage:"applianceList2") {
*/
	def p = dynamicPage(name: "applianceList", title: "Select Your Appliance(s)",  nextPage: "otherSettings") {
		section(""){
			paragraph "Tap below to see the list of Neurio Appliances available in your Neurio account,and select the ones you want to connect to SmartThings (max=6."
			input(name: "NeurioAppliances", title:"", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:neurioAppliances])
		}
	}

	log.debug "list p: $p"
	return p
}

def NeurioApplianceList2() {
	log.debug "NeurioApplianceList2()"

    
	def neurioAppliances = getNeurioAppliances(state.locationId)

	log.debug "device list: $neurioAppliances"

	def p = dynamicPage(name: "applianceList2", title: "Select Your Appliance(s)", nextPage:"otherSettings" ) {
		section(""){
			paragraph "page 2: select the ones you want to connect to SmartThings (max=6 per page)."
			input(name: "NeurioAppliances", title:"", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:neurioAppliances])
		}
	}

	log.debug "list p: $p"
	return p
}


def getNeurioSensors() {
	def NEURIO_SUCCESS=200
    
	log.debug "getting Neurio devices list"
	def deviceListParams = [
		uri: "${get_URI_ROOT()}/users/current",
		headers: ["Authorization": "Bearer ${atomicState.authToken}"],
		Accept: "application/json",
		charset: "UTF-8"
	]

	log.debug "_______AUTH______ ${atomicState.authToken}"
	log.debug "device list params: $deviceListParams"

	def sensors = [:]
	try {
		httpGet(deviceListParams) { resp ->

			if (resp.status == NEURIO_SUCCESS) {
/*        
				int i=0    // Used to simulate many sensors
*/
				log.debug "getNeurioSensors>resp data = ${resp.data}" 
				def jsonMap =resp.data
				def userid = jsonMap.id
				def username = jsonMap.name
				def email = jsonMap.email
				def status = jsonMap.status  
				if (status != 'active') {
					log.error "getNeurioSensors>userId=${userid},name=${username},email=${email} not active, exiting..."
					return
				}
                
				log.debug "getNeurioSensors>userId=${userId},name=${username},email=${email},active=${active}"
				jsonMap.locations.each {
					def locationId = it.id
					def locationName = it.name
					def timezone = it.timezone
					log.debug "getNeurioSensors>found locationId=${locationId},name=${locationName},timezone=${timezone}"

					// save the locationId and locationName for dni reference
					state.locationId= locationId
					state.locationName=locationName                    

					it.sensors.each {
						def sensorId = it.id
						def sensorType = it.sensorType
						def dni = [ app.id, locationName, sensorId, ].join('.')
						sensors[dni] = sensorId
						log.debug "getNeurioSensors>sensorId=${sensorId},type=${sensorType}"
					} /* end each sensor */                        
				} /* end each location */                        
			} else {
				state?.msg= "trying to get list of Sensors, http error status: ${resp.status}"
				log.error state.msg        
				runIn(30, "sendMsgWithDelay")
			}
        
		}        
	} catch (java.net.UnknownHostException e) {
		state?.msg= "trying to get list of Sensors, Unknown host - check the URL " + deviceListParams.uri
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
		        
	} catch (java.net.NoRouteToHostException t) {
		state?.msg= "trying to get list of Sensors, No route to host - check the URL " + deviceListParams.uri
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
	} catch (e) {
		state?.msg= "exception $e while getting list of Sensors" 
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
    }
    

	log.debug "sensors: $sensors"

	return sensors
}


private def getNeurioAppliances(locationId) {
	def NEURIO_SUCCESS=200
    
	def args = "locationId=" + locationId 

	log.debug "getting Neurio Appliances list"
	def deviceListParams = [
		uri: "${get_URI_ROOT()}/appliances?${args}",
		headers: ["Authorization": "Bearer ${atomicState.authToken}"],
		Accept: "application/json",
		charset: "UTF-8"
	]

	log.debug "_______AUTH______ ${atomicState.authToken}"
	log.debug "device list params: $deviceListParams"

	def appliances = [:]
	try {
		httpGet(deviceListParams) { resp ->

			if (resp.status == NEURIO_SUCCESS) {
				log.debug "getNeurioAppliances>resp data = ${resp.data}" 
				def jsonMap =resp.data
				jsonMap.each {
                
					def applianceId= it.id
					def applianceName= it?.name
					def applianceLabel= it?.label
					def applianceCreated= it?.createdAt
					def applianceUpdated= it?.updatedAt

					def dni = [app.id, state.locationName ,applianceLabel, applianceId].join('.')
					appliances[dni] = applianceLabel
				}				                
			} else {
				state?.msg= "trying to get list of Appliances, http status: ${resp.status}"
				log.error state.msg        
				runIn(30, "sendMsgWithDelay")
			}
            
		}        
	} catch (java.net.UnknownHostException e) {
		state?.msg= "trying to get list of Appliances, Unknown host - check the URL " + deviceListParams.uri
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
		        
	} catch (java.net.NoRouteToHostException t) {
		state?.msg= "trying to get list of Appliances, No route to host - check the URL " + deviceListParams.uri
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
	} catch (e) {
		state?.msg= "exception $e while getting list of Appliances" 
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
    }
    

	log.debug "appliances: $appliances"

	return appliances
}


def setParentAuthTokens(auth_data) {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	sendPush("setParentAuthTokens>begin auth_data: $auth_data")
*/

	atomicState.refreshToken = auth_data?.refresh_token
	atomicState.authToken = auth_data?.access_token
	atomicState.expiresIn=auth_data?.expires_in
	atomicState.tokenType = auth_data?.token_type
	atomicState.authexptime= auth_data?.authexptime
	refreshAllChildAuthTokens()
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	sendPush("setParentAuthTokens>New atomicState: $atomicState")
*/
}

def refreshAllChildAuthTokens() {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	sendPush("refreshAllChildAuthTokens>begin updating children with $atomicState")
*/

	def children= getChildDevices()
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	sendPush("refreshAllChildAuthtokens> refreshing ${children.size()} thermostats")
*/

	children.each { 
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
		sendPush("refreshAllChildAuthTokens>begin updating $it.deviceNetworkId with $atomicState")
*/
		it.refreshChildTokens(atomicState) 
	}
}

def refreshThisChildAuthTokens(child) {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	sendPush("refreshThisChildAuthTokens>begin child id: ${child.device.deviceNetworkId}, updating it with ${atomicState}")
*/
	child.refreshChildTokens(atomicState)

/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	sendPush("refreshThisChildAuthTokens>end")
*/
}



def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()    
	initialize()
    
/* 	Create appliance objects
*/
	create_child_appliances()  

}


private def delete_child_devices() {
	def delete
	def deleteAppliances
    
	// Delete any that are no longer in settings
	if (!NeurioSensors) {
		log.debug "delete_child_devices>deleting all Neurio Sensors"
		delete = getAllChildDevices()
	} else {
		delete = getChildDevices().findAll { !NeurioSensors.contains(it.deviceNetworkId) }
		log.debug "delete_child_devices>deleting ${delete.size()} Neurio Sensors"
		deleteAppliances = getChildDevices().findAll { !NeurioAppliances.contains(it.deviceNetworkId) }
		log.debug "delete_child_devices>deleting ${deleteAppliances.size()} Neurio Appliances"
	}

	try { 
		delete.each { deleteChildDevice(it.deviceNetworkId) }
	} catch (e) {
		log.debug "delete_child_devices>exception $e while deleting ${delete.size()} Neurio Sensors"
	}
	try { 

		deleteAppliances.each { deleteChildDevice(it.deviceNetworkId) }
	} catch (e) {
		log.debug "delete_child_devices>exception $e while deleting ${deleteAppliances.size()} Neurio Appliances"
	}
}




private def create_child_devices() {

	def devices = NeurioSensors.collect { dni ->

		def d = getChildDevice(dni)
		log.debug "create_child_devices>looping thru Neurio Sensors, found id $dni"

		if(!d) {
			def neurio_info  = dni.tokenize('.')
			def sensorId = neurio_info.last()
 			def locationName = neurio_info[1]
			def labelName = 'My Neurio ' + "${locationName}:${sensorId}"
			log.debug "create_child_devices>about to create child device with id $dni, sensorId = $sensorId, locationName=  ${locationName}"
			d = addChildDevice(getChildNamespace(), getChildName(), dni, null,
				[label: "${labelName}"]) 
			d.initialSetup( getSmartThingsClientId(), atomicState, sensorId ) 	// initial setup of the Child Device
			log.debug "create_child_devices>created ${d.displayName} with id $dni"
            
		} else {
			log.debug "create_child_devices>found ${d.displayName} with id $dni already exists"
		}


	}

	log.debug "create_child_devices>created ${devices.size()} Neurio sensors"
}



private def create_child_appliances() {

	    
	def devices = NeurioAppliances.collect { dni ->

		log.debug "create_child_appliances>Looping thru Neurio Appliances, found id $dni"

		def neurio_info  = dni.tokenize('.')
		def applianceId = neurio_info.last()
 		def locationName = neurio_info[1]
 		def applianceLabel = neurio_info[2]
		def d = getChildDevice(dni)
		if(!d) {
            
			def labelName = 'My Appliance ' + "${locationName}:${applianceLabel}"
			log.debug "About to create child device with id $dni, locationName = $locationName, applianceLabel=  ${applianceLabel}"
			d = addChildDevice(getChildNamespace(), getNeurioApplianceChildName(), dni, null,
				[label: "${labelName}"]) 
			d.initialSetup(atomicState, applianceId ) 	// initial setup of the Child Device
			log.debug "create_child_appliances>created ${d.displayName} with id $dni"
		} else {
        
			log.debug "create_child_appliances>found ${d.displayName} with id $dni already exists"
		}
	}
	log.debug "create_child_appliances>created ${devices.size()} Neurio appliances"

}

def initialize() {
    
	log.debug "initialize"
	state?.exceptionCount=0
	state?.msg=null    
	delete_child_devices()	
	create_child_devices()
    
    
	// set up internal poll timer
	def pollTimer = 20

	log.trace "setting poll to ${pollTimer}"
	schedule("0 0/${pollTimer.toInteger()} * * * ?", takeAction)
}

def takeAction() {
	log.trace "takeAction>begin"
	def MAX_EXCEPTION_COUNT=5    
	def msg
    
	def devices = NeurioSensors.collect { dni ->
    
		Boolean pollSuccessful=false    
		def d = getChildDevice(dni)
		log.debug "takeAction>looping thru Neurio Sensors, found id $dni, about to poll"
		try {
			d.poll()
			pollSuccessful=true
			// reset exception counter            
			state?.exceptionCount=0       
		} catch (e) {
			state?.exceptionCount=state?.exceptionCount+1        
			log.error "MyNeurioServiceMgr>exception $e while trying to poll the device $d, exceptionCount= ${state?.exceptionCount}" 
		}
		if (pollSuccessful) {
			log.debug "takeAction>about to get Neurio Appliance data and update Appliance objects for device $d"
			get_neurio_appliances_data(d)
		}    	
        
		if (state?.exceptionCount == MAX_EXCEPTION_COUNT) {
			// need to re-authenticate again    
			atomicState.authToken= null                    
			msg = "MyNeurioServiceMgr>too many exceptions ($MAX_EXCEPTION_COUNT), need to re-authenticate at Neurio..." 
			log.error msg
			send msg
			        
		}        
	}
	log.trace "takeAction>end"
}


def oauthInitUrl() {
	log.debug "oauthInitUrl"
	def stcid = getSmartThingsClientId();

	atomicState.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
		response_type: "code",
		client_id: stcid,
		state: atomicState.oauthInitState,
		redirect_uri: buildRedirectUrl()
	]

	return "${get_URI_ROOT()}/oauth2/authorize?" + toQueryString(oauthParams)
}


def buildRedirectUrl(action = "swapToken") {
	log.debug "buildRedirectUrl, atomicState.accessToken=${atomicState.accessToken}," +
		serverUrl + "/api/token/${atomicState.accessToken}/smartapps/installations/${app.id}/${action}"
	return serverUrl + "/api/token/${atomicState.accessToken}/smartapps/installations/${app.id}/${action}"
}

def swapToken() {
	log.debug "swapping token: $params"
	debugEvent ("swapping token: $params", true)

	def code = params.code
	def oauthState = params.state

	def stcid = getSmartThingsClientId()

	def tokenParams = [
		grant_type: "authorization_code",
		code: params.code,
		client_id: stcid,
		client_secret: getSmartThingsPrivateKey(),        
		redirect_uri: buildRedirectUrl()
	]
	def tokenMethod = [
		uri:"${get_URI_ROOT()}/oauth2/token?",
		body: toQueryString(tokenParams)
	]

	log.debug "Swapping token $params"

	def jsonMap
	try {	
		httpPost(tokenMethod) { resp ->
			jsonMap = resp.data
		}
	} catch ( e) {
		
		log.error ("exception ${e}, error swapping token: $resp.status")		
	}
	log.debug "Swapped token for $jsonMap"
	debugEvent ("swapped token for $jsonMap", true)

	atomicState.refreshToken = jsonMap.refresh_token
	atomicState.authToken = jsonMap.access_token
	atomicState.expiresIn=jsonMap.expires_in
	atomicState.tokenType = jsonMap.token_type
	def authexptime = new Date((now() + (atomicState.expiresIn * 60 * 1000))).getTime()
	atomicState.authexptime = authexptime


	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Withings Connection</title>
<style type="text/css">
	@font-face {
		font-family: 'Swiss 721 W01 Thin';
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
		font-weight: normal;
		font-style: normal;
	}
	@font-face {
		font-family: 'Swiss 721 W01 Light';
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
		src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
			 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
		font-weight: normal;
		font-style: normal;
	}
	.container {
		width: 560px;
		padding: 40px;
		/*background: #eee;*/
		text-align: center;
	}
	img {
		vertical-align: middle;
	}
	img:nth-child(2) {
		margin: 0 30px;
	}
	p {
		font-size: 2.2em;
		font-family: 'Swiss 721 W01 Thin';
		text-align: center;
		color: #666666;
		padding: 0 40px;
		margin-bottom: 0;
	}
/*
	p:last-child {
		margin-top: 0px;
	}
*/
	span {
		font-family: 'Swiss 721 W01 Light';
	}
</style>
</head>
<body>
	<div class="container">
		<img src="https://s3-us-west-2.amazonaws.com/neurio/community/NeurioAppLogo72x72.png" width="216" height="216" alt="neurio icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
		<p>Your Neurio Account is now connected to SmartThings!</p>
		<p>Click 'Done' to finish setup.</p>
	</div>
</body>
</html>
"""

	render contentType: 'text/html', data: html
}

def getChildDeviceIdsString() {
	return NeurioSensors.collect { it.split(/\./).last() }.join(',')
}

def toJson(Map m) {
	return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}


def debugEvent(message, displayEvent) {

	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	log.debug "Generating AppDebug Event: ${results}"
	sendEvent (results)
}

    
private void get_neurio_appliances_data(neurio) {
	Boolean foundAppliance=false		
	def applianceList
	String applianceName
	def applianceLabel
	def applianceTags
	def applianceCreated
	def applianceUpdated

	try {
		neurio.getApplianceList("")
		applianceList = neurio.currentAppliancesList.toString().minus('[').minus(']').tokenize(',')
		foundAppliance=true        
	} catch (e) {
		log.error("Not able to get the list of appliances from Neurio, exception $e")    	
	}    
	if (foundAppliance) {

		log.debug("list of appliances = $applianceList")    	
    
		for (applianceId in applianceList) {
			log.debug("applianceId=${applianceId}")    	
			neurio.getApplianceData(applianceId)
			String applianceData=neurio.currentApplianceData.toString()
			def applianceFields=null            
			if (applianceData) {    
				applianceFields = new JsonSlurper().parseText(applianceData)
			} else {
				log.error("get_neurio_appliances_data>applianceData is empty, exiting")
				continue        
			}    
			log.debug "get_neurio_appliances_data>applianceFields = $applianceFields"

			if (applianceFields) {
				applianceName=applianceFields?.name.toString()
				applianceLabel=applianceFields?.label
				applianceCreated=formatDateInLocalTime(applianceFields?.createdAt)
				applianceUpdated=formatDateInLocalTime(applianceFields?.updatedAt)
				log.debug "get_neurio_appliances_data>applianceId= ${applianceId}, applianceName=${applianceName}" +
					",applianceLabel=${applianceLabel},created=${applianceCreated}, updated=${applianceUpdated}"
				
				def locationName = neurio.currentLocationName                
				def dni = [app.id, locationName,applianceLabel, applianceId].join('.')
				def d = getChildDevice(dni)
				if (d) {
					log.debug "get_neurio_appliances_data>found ${d.displayName} with id $dni already exists, polling the object"
					d.poll()					
				} else {
					log.debug "get_neurio_appliances_data>didn't find dni ${dni}, probably not instantiated originally"
            
				}            
			}

		} /* end for */            
        
	}    
}

private String formatDateInLocalTime(dateInString) {
	if ((dateInString==null) || (dateInString.trim()=="")) {
		return ""    
	}    
	SimpleDateFormat ISODateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	Date ISODate = ISODateFormat.parse(dateInString)
	String dateInLocalTime = ISODate.format("yyyy-MM-dd HH:mm", location.timeZone)
	return dateInLocalTime
}


private def get_URI_ROOT() {
	return "https://api.neur.io/v1"
}

private void sendMsgWithDelay() {

	if (state?.msg) {
		send "MyNeurioServiceMgr> ${state.msg}"
	}
}

private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)

	}

	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, msg)
	}

	log.debug msg
}


def getNeurioApplianceChildName() {"My Neurio Appliance"}

def getChildNamespace() { "yracine" }

def getChildName() { "My Neurio Device" }

def getServerUrl() { return "https://graph.api.smartthings.com" }

def getSmartThingsClientId() { "kjPlS3AAQtaUGlmB30IU9g" }

def getSmartThingsPrivateKey() { "6Qg0niXeQDSk-dkfU475og" }