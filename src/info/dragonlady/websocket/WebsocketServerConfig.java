package info.dragonlady.websocket;

import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

public class WebsocketServerConfig implements ServerApplicationConfig {

	@Override
	public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> arg0) {
		// TODO Auto-generated method stub
//	       Set<Class<?>> results = new HashSet<>();
//	       for (Class<?> classes : arg0) {
//	    	   if (classes.getPackage().getName().indexOf(".websocket.") > 0) {
//	    		   results.add(classes);
//	    	   }
//	       }
		return arg0;
	}

	@Override
	public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> arg0) {
		// TODO Auto-generated method stub
		Set<ServerEndpointConfig> result = new HashSet<>();
//		if (arg0.contains(EchoEndpoint.class)) {
//			result.add(ServerEndpointConfig.Builder.create(
//					EchoEndpoint.class,
//					"/websocket/echoProgrammatic").build());
//		}

		return result;	
	}

}
