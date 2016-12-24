/**
 *
 *
 *
 *
 */

preferences {
	input("destMac", "text", title: "mac:", description: "The device mac address")
    //input("secureCode", "text", title: "secureCode:", description: "The secure code")
}
 

metadata {
	definition (name: "WoL", namespace: "Co1dSUN", 
    	author: "coldfuture@gmail.com") {
        capability "Actuator"
        capability "Polling"
        
        command "myWOLCommand"
        
      	}

	simulator {
		// TODO-: define status and reply messages here
	}

	tiles {
		standardTile("poll", "device.poll", width: 3, height: 3, canChangeIcon: true, canChangeBackground: false) {
            state "Wake UP", label: '${name}', action:"myWOLCommand", backgroundColor: "#ffffff", icon:"st.Electronics.electronics18"
        }

        
		main "poll"
		details "poll"
	
	}
}


def myWOLCommand() {
    sendEvent(name: "magic_packet", value: "sent")
    sendHubCommand(new physicalgraph.device.HubAction(
        "wake on lan $destMac",
        physicalgraph.device.Protocol.LAN,
        null,
        null
    ))
    log.debug "Magic packet is sent to '${destMac}'"
}