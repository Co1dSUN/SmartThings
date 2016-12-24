/**
 *  	Denon Network Receiver 
 *    	Based on Denon/Marantz receiver by Kristopher Kubicki
 *    	SmartThings driver to connect your Denon Network Receiver to SmartThings
 *		Special for Denon Ceol RCD-N8
 */

preferences {
    input("destIp", "text", title: "IP", description: "The device IP")
    input("destPort", "number", title: "Port", description: "The port you wish to connect", defaultValue: 80)
}

metadata {
	definition (name: "Ceol", namespace: "Co1dSUN", 
    	author: "coldfuture@gmail.com") {
        capability "Actuator"
        capability "Switch" 
        capability "Polling"
        capability "Switch Level"
        capability "Music Player" 
        
        attribute "mute", "string"
        attribute "input", "string"
		attribute "inputChan", "enum"     
        
        command "mute"
        command "unmute"
        command "toggleMute"
        command "inputSelect", ["string"]
        command "inputNext"
        command "networkSelect", ["string"]
        command "pc"
        command "spotify"
        command "volUp"
        command "volDown"
        }

    simulator {
        // TODO-: define status and reply messages here
    }

    //tiles {
	tiles(scale: 2) {
		multiAttributeTile(name:"multiAVR", type: "generic", width: 6, height: 4) {
           tileAttribute("device.status", key: "PRIMARY_CONTROL") { 	            
            	attributeState ("off", label: 'off', backgroundColor: "#53a7c0", action:"switch.on", defaultState: true)
				attributeState ("on", label: 'on', backgroundColor: "#79b821", action:"switch.off")
        	}
           tileAttribute("device.level", key: "VALUE_CONTROL") {
        		attributeState ("level", action: "volUp")
        		attributeState ("level", action: "volDown")
    		}
            /*tileAttribute("device.status", key: "MEDIA_STATUS") { 	            
            	attributeState "on", label: '${name}', action:"switch.off"
                attributeState "off", label: '${name}', action:"switch.on"
			}*/
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
           		attributeState ("level", action:"setLevel")
                }       
            /*tileAttribute ("device.mute", key: "MEDIA_MUTED") {
            	attributeState("unmuted", action:"mute", nextState: "muted")
            	attributeState("muted", action:"unmute", nextState: "unmuted")
        	}*/
            tileAttribute("device.input", key: "SECONDARY_CONTROL") {
            	attributeState ("default", label:'${currentValue}', defaultState: true)
        	}
        } 
        /*standardTile("power", "device.status", width: 2, height: 2, canChangeIcon: false, canChangeBackground: true) {
        	state "on", label: '${currentValue}', action:"switch.off", backgroundColor: "#79b821", icon:"st.Electronics.electronics16"
            state "off", label: '${currentValue}', action:"switch.on", backgroundColor: "#ffffff", icon:"st.Electronics.electronics16"
        }*/
		standardTile("poll", "device.poll", width: 2, height: 2, decoration: "flat") {
            state "poll", label: "", action: "polling.poll", icon: "st.secondary.refresh", backgroundColor: "#FFFFFF"
        }
        standardTile("pc", "device.input", width: 2, height: 2, decoration: "flat"){
            state "pc", label: '${currentValue}', action: "pc", icon:"st.Electronics.electronics3" , backgroundColor: "#53a7c0"
            //state "PC", label: 'PC', action: "pc", icon:"st.Electronics.electronics3" , backgroundColor: "#79b821"         
        	
        }
        standardTile("mute", "device.mute", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "muted", label: '${name}', action:"unmute", backgroundColor: "#79b821", icon:"st.Electronics.electronics13"
            state "unmuted", label: '${name}', action:"mute", backgroundColor: "#ffffff", icon:"st.Electronics.electronics13"
		}
		standardTile("sInput", "device.input", width: 4, height: 2, decoration: "flat"){
        	state "Digital In", label: '${currentValue}', action:"sRadio", icon:"st.Office.office12"
        	state "Tuner", label: '${currentValue}', action:"sPC", icon:"st.Office.office12"
        }
        standardTile("net", "device.network", width: 2, height: 2, decoration: "flat"){
            state "SPOTIFY", label: '${currentValue}', action: "spotify", icon:"st.Electronics.electronics3" , backgroundColor: "#53a7c0"
            //state "PC", label: 'PC', action: "pc", icon:"st.Electronics.electronics3" , backgroundColor: "#79b821"         
        	
        }

main "multiAVR"
        details(["multiAVR", "mute", "pc", "net", "sInput","poll"])
    }
}
def parse(String description) {
	//log.debug "Parsing '${description}'"
    
 	def map = stringToMap(description)

    
    if(!map.body || map.body == "DQo=") { return }
        //log.debug "${map.body} "
	def body = new String(map.body.decodeBase64())
    
	def statusrsp = new XmlSlurper().parseText(body)
	def power = statusrsp.Power.value.text()

	if(power == "ON") { 
    	sendEvent(name: "status", value: 'on') 
        //sendEvent(name: "power", value: 'on') 
    }
    if(power != "" && power != "ON") { 
    	sendEvent(name: "status", value: 'off')
        //sendEvent(name: "power", value: 'off')
	}
    
    def muteLevel = statusrsp.Mute.value.text()
    if(muteLevel == "on") { 
    	sendEvent(name: "mute", value: 'muted')
	}
    if(muteLevel != "" && muteLevel != "on") {
	    sendEvent(name: "mute", value: 'unmuted')
    }
    
    def inputCanonical = statusrsp.InputFuncSelect.value.text()
            sendEvent(name: "input", value: inputCanonical)
            sendEvent(name: "switch", value: inputCanonical)
	        log.debug "Current Input is: ${inputCanonical}"
    
    def inputNet = statusrsp.NetFuncSelect.value.text()
    		sendEvent(name: "network", value: inputNet)
            log.debug "Current Network service is: ${inputNet}"
    
    def inputSurr = statusrsp.selectSurround.value.text() //Not used
	        log.debug "Current Surround is: ${inputSurr}"

    if(statusrsp.MasterVolume.value.text()) { 
    	def int volLevel = (int) statusrsp.MasterVolume.value.toFloat() ?: -40.0
        volLevel = (volLevel + 80) * 0.8
       //volLevel = (volLevel + 80)
        	log.debug "Adjusted volume is ${volLevel}"
   		
        def int curLevel = 36
        try {
        	curLevel = device.currentValue("level")
        	log.debug "Current volume is ${curLevel}"
        } catch(NumberFormatException nfe) { 
        	curLevel = 36
        }
	
        if(curLevel != volLevel) {
    		sendEvent(name: "level", value: volLevel)
        }
    } 
}


def setLevel(val) {
	sendEvent(name: "mute", value: "unmuted")     
    sendEvent(name: "level", value: val)
	def int scaledVal = val *0.8 - 80
    request("cmd0=PutMasterVolumeSet%2F$scaledVal")
}

def volUp() {
	//sendEvent(name: "level", value: val)
	sendget('Direct.xml?MVUP')
}

def volDown() {
	//sendEvent(name: "level", value: val)
	sendget('Direct.xml?MVDOWN')
}

def on() {
	sendEvent(name: "status", value: 'on')
	request('cmd0=PutSystem_OnStandby%2FON')
}

def off() { 
	sendEvent(name: "status", value: 'off')
	request('cmd0=PutSystem_OnStandby%2FSTANDBY')
}

def mute() { 
	sendEvent(name: "mute", value: "muted")
	request('cmd0=PutVolumeMute%2FON')
}

def unmute() { 
	sendEvent(name: "mute", value: "unmuted")
	request('cmd0=PutVolumeMute%2FOFF')
}

def toggleMute(){
    if(device.currentValue("mute") == "muted") { unmute() }
	else { mute() }
}

def spotify() { 
	sendEvent(name: "network", value: "SPOTIFY")
	request('cmd0=PutVolumeMute%2FOFF')
}

def pc() {
    log.debug "Setting input to AUX D"
    request("cmd0=PutZone_InputFunction%2FSIAUX3")
    sendEvent(name: "input", value: "Digital In")
    }



def sPC() {
    log.debug "Setting input to AUX D"
    sendEvent(name: "input", value: "Digital In")
    request("cmd0=PutZone_InputFunction%2FSIAUX3")
    }

def sRadio() { 
    log.debug "Setting input to FM Radio"
    sendEvent(name: "input", value: "TUNER")
    request("cmd0=PutZone_InputFunction%2FSITUNER")
}

def poll() { 
	//log.debug "Polling requested"
    refresh()
}

def refresh() {

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
   	 		'method': 'GET',
    		'path': "/goform/formMainZone_MainZoneXml.xml",
            'headers': [ HOST: "$destIp:$destPort" ] 
		)   
     
   
    hubAction
}

def sendget(query) {
	def path = "http://${destIp}/goform/formiPhoneApp${query}"
    sendHubCommand(new physicalgraph.device.HubAction(
   	 		method: "GET",
    		path: path,
        	headers: [ HOST: "${destIp}:${destPort}" ]
        ))
	log.debug "Executing ${query}"
}

def request(body) { 

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
   	 		'method': 'POST',
    		'path': "/MainZone/index.put.asp",
        	'body': body,
        	'headers': [ HOST: "$destIp:$destPort" ]
		) 
              
    hubAction
}


private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}